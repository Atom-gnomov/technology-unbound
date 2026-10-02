package unboundtech.common;

import baubles.api.BaublesApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;
import thaumcraft.common.Thaumcraft;
import unboundtech.common.items.baubles.ItemPoweredBauble;
import unboundtech.common.items.baubles.ItemResonanceAmulet;
import unboundtech.common.items.baubles.ItemTeslaGirdle;

/**
 * Правило EU-щита (`protective_baubles.md` §4.3) и разряд Тесла-Пояса.
 *
 * §4.3 п.2: пока наша вещь надета, КАЖДОЕ очко, добавленное движком в
 * общий пул рунного щита, списывает 1 000 EU — независимо от того, чей
 * слот его «дал». Без этого сборка «наш амулет + наш пояс + два рунных
 * кольца ТК» давала бы 36 очков, оплаченных одним вис, и наша ветка
 * оказалась бы бесплатной прибавкой к чужой.
 *
 * ⚠️ Списание повешено на РОСТ значения {@code runicCharge} движка, а не
 * на свой таймер (прямое требование §4.3) — иначе два счётчика разъедутся.
 * Источник правды — публичная карта {@code EventHandlerRunic.runicCharge}.
 *
 * §4.2: разряд пояса ловится сравнением заряда щита ДО и ПОСЛЕ
 * обработчика ТК на одном событии урона (HIGHEST → LOWEST).
 */
public final class UTRunicBillingHandler {

    /** Последнее увиденное значение щита: игрок → очки. */
    private final Map<EntityPlayer, Integer> lastCharge = new WeakHashMap<>();
    /** Заряд щита на входе в текущее событие урона. */
    private final Map<EntityPlayer, Integer> chargeBeforeHit = new WeakHashMap<>();
    /** Урон на входе — щит мог поглотить и при аварийном скачке заряда. */
    private final Map<EntityPlayer, Float> amountBeforeHit = new WeakHashMap<>();

    private UTRunicBillingHandler() {
    }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
                .register(new UTRunicBillingHandler());
    }

    /** Текущий рунный заряд игрока по данным движка ТК. */
    private static int engineCharge(EntityPlayer player) {
        if (Thaumcraft.instance == null
                || Thaumcraft.instance.runicEventHandler == null) {
            return 0;
        }
        Integer value = Thaumcraft.instance.runicEventHandler.runicCharge
                .get(player.getEntityId());
        return value == null ? 0 : value;
    }

    /** Наши надетые питаемые баблсы (амулет, пояс). */
    private static List<ItemStack> wornPowered(EntityPlayer player) {
        List<ItemStack> found = new ArrayList<>(2);
        IItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) {
            return found;
        }
        for (int slot = 0; slot < baubles.getSlots(); slot++) {
            ItemStack stack = baubles.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemPoweredBauble) {
                found.add(stack);
            }
        }
        return found;
    }

    private static ItemStack wornOf(EntityPlayer player, Class<?> type) {
        for (ItemStack stack : wornPowered(player)) {
            if (type.isInstance(stack.getItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // ================= §4.3: плата за рост щита =================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving().world.isRemote
                || !(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        int now = engineCharge(player);
        Integer previous = this.lastCharge.get(player);
        this.lastCharge.put(player, now);
        if (previous == null || now <= previous) {
            return;   // щит не рос — платить не за что
        }
        List<ItemStack> worn = wornPowered(player);
        if (worn.isEmpty()) {
            return;   // наших вещей нет — это чужой, вис-оплаченный щит
        }
        int points = now - previous;
        int bill = points * ItemPoweredBauble.EU_PER_SHIELD_POINT;
        for (ItemStack stack : worn) {
            if (bill <= 0) {
                break;
            }
            // платим сколько можем с каждой вещи по очереди
            while (bill > 0 && ItemPoweredBauble.drawEnergy(stack,
                    ItemPoweredBauble.EU_PER_SHIELD_POINT)) {
                bill -= ItemPoweredBauble.EU_PER_SHIELD_POINT;
            }
        }
        // не хватило EU — очки остаются, но на следующем пересчёте
        // движка getRunicCharge вернёт 0 и максимум сам просядет (§4.3 п.1)
    }

    // ================= §4.2: разряд пояса =================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurtPre(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote
                || !(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        this.chargeBeforeHit.put(player, engineCharge(player));
        this.amountBeforeHit.put(player, event.getAmount());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurtPost(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote
                || !(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        Integer before = this.chargeBeforeHit.remove(player);
        Float amountBefore = this.amountBeforeHit.remove(player);
        if (before == null) {
            return;
        }
        boolean chargeDropped = engineCharge(player) < before;
        // аварийный амулет ТК может поднять заряд в том же событии —
        // тогда падения нет, но урон щит всё равно срезал (скептик §8)
        boolean damageEaten = amountBefore != null && before > 0
                && event.getAmount() < amountBefore;
        if (!chargeDropped && !damageEaten) {
            return;   // щит удар не поглощал
        }
        ItemStack girdle = wornOf(player, ItemTeslaGirdle.class);
        if (girdle.isEmpty()
                || ItemTeslaGirdle.onCooldown(girdle, player.world)) {
            return;
        }
        // §4.2: только живой источник в 5 блоках — иначе бьём в пустоту
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityLivingBase)
                || source.getDistance(player) > ItemTeslaGirdle.SOURCE_RANGE) {
            return;
        }
        if (!ItemPoweredBauble.drawEnergy(girdle, ItemTeslaGirdle.ZAP_EU)) {
            return;
        }
        ItemTeslaGirdle.startCooldown(girdle, player.world);
        this.zap(player, (EntityLivingBase) source);
    }

    /** Удар по нападавшему + цепь на две цели в 4 блоках. */
    private void zap(EntityPlayer owner, EntityLivingBase first) {
        DamageSource electric = new net.minecraft.util.EntityDamageSource(
                "unboundtech.tesla", owner).setMagicDamage();
        this.hit(owner, first, electric);
        int chained = 0;
        AxisAlignedBB zone = first.getEntityBoundingBox()
                .grow(ItemTeslaGirdle.CHAIN_RANGE);
        for (EntityLivingBase next : first.world
                .getEntitiesWithinAABB(EntityLivingBase.class, zone)) {
            if (chained >= ItemTeslaGirdle.CHAIN_TARGETS) {
                break;
            }
            if (next == first || next == owner
                    || next.getDistance(first) > ItemTeslaGirdle.CHAIN_RANGE) {
                continue;
            }
            // §4.2 по духу: дуга ищет врага, а не свой зверинец
            if (next instanceof net.minecraft.entity.passive.EntityTameable
                    && ((net.minecraft.entity.passive.EntityTameable) next)
                            .getOwner() == owner) {
                continue;
            }
            if (owner.isOnSameTeam(next)) {
                continue;
            }
            if (next instanceof EntityPlayer
                    && next.world.getMinecraftServer() != null
                    && !next.world.getMinecraftServer().isPVPEnabled()) {
                continue;
            }
            this.hit(owner, next, electric);
            chained++;
        }
    }

    private void hit(EntityPlayer owner, EntityLivingBase target,
                     DamageSource source) {
        target.hurtResistantTime = 0;
        target.attackEntityFrom(source, ItemTeslaGirdle.ZAP_DAMAGE);
        if (target.world instanceof WorldServer) {
            ((WorldServer) target.world).spawnParticle(
                    EnumParticleTypes.CRIT_MAGIC,
                    target.posX, target.posY + target.height * 0.6, target.posZ,
                    10, 0.3, 0.4, 0.3, 0.1);
        }
        target.world.playSound(null, target.posX, target.posY, target.posZ,
                unboundtech.common.UTSounds.teslaDischarge,
                net.minecraft.util.SoundCategory.PLAYERS, 0.35F, 1.8F);
    }

}
