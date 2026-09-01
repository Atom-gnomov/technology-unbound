package unboundtech.common;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import ic2.api.item.ElectricItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import unboundtech.common.items.ItemVisEdge;

/**
 * «Резонанс» Вис-Кромки (`vis_edge.md` §4.2): кромка оставляет в ране
 * кусок незавершённого действия, и рана продолжает происходить сама.
 *
 * Удар клинком с тегом (+200 EU с клинка): через 2 сек — 20 урона
 * «доигрывания» (броня учитывается наполовину: 10 обычным + 10 мимо
 * брони, приём вис-патрона), через 4 сек — 10 догорания + поджиг 2 сек.
 * СТЕК НЕ СКЛАДЫВАЕТСЯ: повторный удар обновляет таймер (§4.2 —
 * главное ограничение). Эффект не по площади; смерть цели до
 * срабатывания — ничего не переносится. Фиолетовые частицы учащаются
 * к моменту срабатывания — таймер видно глазами.
 */
public final class UTVisEdgeHandler {

    /** §4.2/§5. */
    private static final int EU_PER_HIT = 200;
    private static final int STAGE1_TICKS = 40;
    private static final int STAGE2_TICKS = 80;
    private static final float STAGE1_DAMAGE = 20.0F;
    private static final float STAGE2_DAMAGE = 10.0F;

    private static final class Resonance {
        int age;
        boolean stage1Done;
        final java.lang.ref.WeakReference<EntityPlayer> attacker;

        Resonance(EntityPlayer player) {
            this.attacker = new java.lang.ref.WeakReference<>(player);
        }
    }

    /** Слабые ключи: выгруженная/умершая цель уходит сама. */
    private static final Map<EntityLivingBase, Resonance> TARGETS =
            new WeakHashMap<>();

    private UTVisEdgeHandler() {
    }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
                .register(new UTVisEdgeHandler());
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        // К-1 скептика: наши strike'и (indirect) снова попадали сюда с
        // trueSource=игрок и клинком в руке — двойной discharge, вечное
        // продление таймера и CME в итерации тик-хендлера. Indirect-урон
        // (стрелы, тридент, наша же магия) кромку НЕ заводит: она про
        // удар клинком.
        if (event.getSource()
                instanceof net.minecraft.util.EntityDamageSourceIndirect) {
            return;
        }
        if (event.getEntityLiving().world.isRemote
                || !(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        ItemStack held = player.getHeldItemMainhand();
        if (!ItemVisEdge.hasEdge(held)) {
            return;
        }
        // +200 EU за удар СВЕРХ обычного; разряженный клинок кромку
        // не применяет (§4.2) — сначала пробный съём
        if (ElectricItem.manager.discharge(held, EU_PER_HIT,
                Integer.MAX_VALUE, true, false, true) < EU_PER_HIT) {
            return;
        }
        ElectricItem.manager.discharge(held, EU_PER_HIT,
                Integer.MAX_VALUE, true, false, false);
        synchronized (TARGETS) {
            // повторное попадание ОБНОВЛЯЕТ таймер, а не стакается
            TARGETS.put(event.getEntityLiving(), new Resonance(player));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        synchronized (TARGETS) {
            Iterator<Map.Entry<EntityLivingBase, Resonance>> it =
                    TARGETS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<EntityLivingBase, Resonance> entry = it.next();
                EntityLivingBase target = entry.getKey();
                Resonance res = entry.getValue();
                if (target == null || target.isDead || target.world == null) {
                    it.remove();   // §4.2: умер — ничего не переносится
                    continue;
                }
                res.age++;
                this.sparkle(target, res.age);
                if (!res.stage1Done && res.age >= STAGE1_TICKS) {
                    res.stage1Done = true;
                    this.strike(target, res, STAGE1_DAMAGE);
                }
                if (res.age >= STAGE2_TICKS) {
                    it.remove();
                    this.strike(target, res, STAGE2_DAMAGE);
                    target.setFire(2);
                }
            }
        }
    }

    /** Магический тип, броня ×0.5: половина обычным, половина мимо. */
    private void strike(EntityLivingBase target, Resonance res, float amount) {
        EntityPlayer attacker = res.attacker.get();
        // В-1 скептика: causeIndirectMagicDamage САМ байпасит броню —
        // «обычную» половину собираем вручную без байпаса
        DamageSource normal = attacker != null
                ? new net.minecraft.util.EntityDamageSourceIndirect(
                        "indirectMagic", attacker, attacker).setMagicDamage()
                : new DamageSource("indirectMagic").setMagicDamage();
        DamageSource pierce = attacker != null
                ? DamageSource.causeIndirectMagicDamage(attacker, attacker)
                : DamageSource.MAGIC;
        // между стадиями есть iframes ванили — сбрасываем перед ударом
        target.hurtResistantTime = 0;
        target.attackEntityFrom(normal, amount * 0.5F);
        target.hurtResistantTime = 0;
        target.attackEntityFrom(pierce, amount * 0.5F);
    }

    /** §8: частицы учащаются к моменту срабатывания. */
    private void sparkle(EntityLivingBase target, int age) {
        if (!(target.world instanceof WorldServer) || age % Math.max(1,
                8 - age / 12) != 0) {
            return;
        }
        ((WorldServer) target.world).spawnParticle(
                EnumParticleTypes.SPELL_WITCH,
                target.posX, target.posY + target.height * 0.6, target.posZ,
                2, 0.25, 0.35, 0.25, 0.0);
    }
}
