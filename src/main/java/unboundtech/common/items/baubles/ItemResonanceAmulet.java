package unboundtech.common.items.baubles;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * Резонансный Амулет (`protective_baubles.md` §4.1): рунный заряд 14 —
 * против 8 у лучшего амулета ТК, но платит электричеством.
 *
 * Сверх щита умеет две вещи:
 *  - АВАРИЙНОЕ ЛЕЧЕНИЕ (приём METS HeavyQuantumSuit): ниже 20 % HP
 *    лечит 1 HP/сек за 5 000 EU, пока не вернёт к 20 %; после
 *    срабатывания кулдаун 30 секунд;
 *  - ЗАРЯЖАЕТ КОЛЬЦА СХЕМЫ в соседних слотах баблсов (приём ASP-шлема):
 *    приоритет — сначала себе, потом кольцам.
 */
public class ItemResonanceAmulet extends ItemPoweredBauble {

    /** §5. */
    public static final int RUNIC_CHARGE = 14;
    public static final double MAX_CHARGE = 1_000_000.0;
    private static final int TIER = 3;                  // HV
    private static final double TRANSFER = 512.0;
    /** §4.1: лечение. */
    private static final float HEAL_THRESHOLD = 0.2F;
    private static final int HEAL_EU = 5_000;
    private static final int HEAL_PERIOD = 20;          // 1 HP/сек
    private static final int HEAL_COOLDOWN = 600;       // 30 секунд
    /** §4.1: раздача кольцам — пачкой, чтобы не дёргать каждый тик. */
    private static final int RING_SHARE_PERIOD = 20;
    private static final int RING_SHARE_EU = 400;
    /** Себе оставляем запас на очко щита (приоритет §4.1). */
    private static final int SELF_RESERVE = EU_PER_SHIELD_POINT * 4;

    private static final String TAG_COOLDOWN = "UTHealCooldown";

    public ItemResonanceAmulet() {
        super(RUNIC_CHARGE, MAX_CHARGE, TIER, TRANSFER,
                "unboundtech.tooltip.resonance_amulet");
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.AMULET;
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (wearer.world.isRemote || !(wearer instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) wearer;
        this.tickHealing(stack, player);
        if (player.ticksExisted % RING_SHARE_PERIOD == 0) {
            this.shareWithRings(stack, player);
        }
    }

    /** §4.1: подъём до 20 % HP по одному сердцу в секунду. */
    private void tickHealing(ItemStack stack, EntityPlayer player) {
        long now = player.world.getTotalWorldTime();
        long ready = readyAt(stack);
        // абсолютный тик + верхняя граница: срок из чужого мира не
        // блокирует лечение навсегда (вердикт решателя)
        if (ready > now && ready <= now + HEAL_COOLDOWN) {
            return;
        }
        float floor = player.getMaxHealth() * HEAL_THRESHOLD;
        if (player.getHealth() >= floor || player.getHealth() <= 0.0F) {
            return;
        }
        if (player.ticksExisted % HEAL_PERIOD != 0) {
            return;
        }
        if (!drawEnergy(stack, HEAL_EU)) {
            return;
        }
        player.heal(1.0F);
        if (player.getHealth() >= floor) {
            // порог взят — уходим на кулдаун (§4.1)
            startCooldown(stack, player.world);
        }
        if (player.world instanceof net.minecraft.world.WorldServer) {
            ((net.minecraft.world.WorldServer) player.world).spawnParticle(
                    net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                    player.posX, player.posY + player.height * 0.7, player.posZ,
                    6, 0.3, 0.4, 0.3, 0.0);
        }
    }

    /**
     * §4.1: одна зарядка на комплект — амулет доливает Кольца Схемы в
     * слотах баблсов, оставляя себе запас на несколько очков щита.
     */
    private void shareWithRings(ItemStack stack, EntityPlayer player) {
        if (ElectricItem.manager == null
                || !hasEnergy(stack, SELF_RESERVE + RING_SHARE_EU)) {
            return;
        }
        IItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) {
            return;
        }
        for (int slot = 0; slot < baubles.getSlots(); slot++) {
            ItemStack worn = baubles.getStackInSlot(slot);
            if (worn.isEmpty() || worn == stack
                    || !(worn.getItem()
                            instanceof unboundtech.common.items.ItemSchemaRing)) {
                continue;
            }
            double room = ElectricItem.manager.charge(worn, RING_SHARE_EU,
                    Integer.MAX_VALUE, true, true);
            if (room <= 0.0) {
                continue;
            }
            int give = (int) Math.min(room, RING_SHARE_EU);
            if (drawEnergy(stack, give)) {
                ElectricItem.manager.charge(worn, give, Integer.MAX_VALUE,
                        true, false);
            }
            if (!hasEnergy(stack, SELF_RESERVE + RING_SHARE_EU)) {
                return;
            }
        }
    }

    private static long readyAt(ItemStack stack) {
        return stack.hasTagCompound()
                ? stack.getTagCompound().getLong(TAG_COOLDOWN) : 0L;
    }

    private static void startCooldown(ItemStack stack,
                                      net.minecraft.world.World world) {
        stack.setTagInfo(TAG_COOLDOWN, new net.minecraft.nbt.NBTTagLong(
                world.getTotalWorldTime() + HEAL_COOLDOWN));
    }

    /** Разряженный амулет гаснет — видно и на чужом игроке (§8). */
    public static boolean isLit(ItemStack stack) {
        return stack.getItem() instanceof IElectricItem
                && hasEnergy(stack, EU_PER_SHIELD_POINT);
    }
}
