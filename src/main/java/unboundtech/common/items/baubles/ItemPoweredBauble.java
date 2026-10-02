package unboundtech.common.items.baubles;

import baubles.api.IBauble;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import thaumcraft.api.IRunicArmor;

/**
 * Общее у защитной бижутерии (`protective_baubles.md`): рунный щит за
 * ЭЛЕКТРИЧЕСТВО вместо вис. Ниша — не «мы бесплатны» (щит ТК тоже
 * платный: 0.5 Aer + 0.5 Terra за очко), а ЧЕМ платишь: у ТК жезл, у
 * нас розетка (§1, закон 3).
 *
 * §4.3 п.1: {@code getRunicCharge} отдаёт свои очки ТОЛЬКО пока в
 * буфере есть EU; пустой буфер → 0, и движок на ближайшем пересчёте
 * (раз в 40 тиков) срежет максимум щита. Предмет при этом цел.
 * Списание за РОСТ щита живёт в {@code UTRunicBillingHandler} (§4.3 п.2).
 */
public abstract class ItemPoweredBauble extends Item
        implements IBauble, IRunicArmor, IElectricItem {

    /** §5: 1 000 EU за очко щита — одинаково у обоих предметов. */
    public static final int EU_PER_SHIELD_POINT = 1_000;

    private final int runicCharge;
    private final double maxCharge;
    private final int tier;
    private final double transferLimit;
    private final String tooltipKey;

    protected ItemPoweredBauble(int runicCharge, double maxCharge, int tier,
                                double transferLimit, String tooltipKey) {
        this.runicCharge = runicCharge;
        this.maxCharge = maxCharge;
        this.tier = tier;
        this.transferLimit = transferLimit;
        this.tooltipKey = tooltipKey;
        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        this.setNoRepair();
    }

    /**
     * Есть ли в буфере хотя бы столько EU.
     *
     * Читаем через {@code getCharge} — он же симулированный разряд,
     * но без аргументов, которые роняли фичу. Побочный эффект у него
     * остаётся (создаёт пустой NBT на бестеговом стеке), он безвреден:
     * тег пустой и ни инфузия, ни крафт на него не смотрят.
     */
    public static boolean hasEnergy(ItemStack stack, int amount) {
        return ElectricItem.manager != null
                && ElectricItem.manager.getCharge(stack) >= amount;
    }

    /**
     * Снять EU; true — списано полностью.
     *
     * ⚠️ externally = FALSE (критикал скептика): при
     * {@code canProvideEnergy() == false} менеджер IC2 на внешнем
     * разряде возвращает 0 всегда — фича была мертва целиком.
     */
    public static boolean drawEnergy(ItemStack stack, int amount) {
        if (!hasEnergy(stack, amount)) {
            return false;
        }
        return ElectricItem.manager.discharge(stack, amount, Integer.MAX_VALUE,
                true, false, false) >= amount;
    }

    // ================= IRunicArmor =================

    /** §4.3 п.1: разряженная вещь в щит не считается вовсе. */
    @Override
    public int getRunicCharge(ItemStack stack) {
        return hasEnergy(stack, EU_PER_SHIELD_POINT) ? this.runicCharge : 0;
    }

    /** Паспортное значение — для тултипа (без оглядки на заряд). */
    public int ratedRunicCharge() {
        return this.runicCharge;
    }

    // ================= IElectricItem =================

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return this.maxCharge;
    }

    @Override
    public int getTier(ItemStack stack) {
        return this.tier;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return this.transferLimit;
    }

    /** §8: заряд виден и на себе, и на чужом игроке. */
    @Override
    public boolean willAutoSync(ItemStack stack,
                                net.minecraft.entity.EntityLivingBase wearer) {
        return true;
    }

    // ================= тултип (§9) =================

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        boolean live = hasEnergy(stack, EU_PER_SHIELD_POINT);
        lines.add((live ? "§b" : "§8") + I18n.translateToLocal(
                "unboundtech.tooltip.runic_charge") + " " + this.runicCharge
                + (live ? "" : " §c(" + I18n.translateToLocal(
                        "unboundtech.tooltip.bauble_dead") + ")"));
        lines.add("§7" + I18n.translateToLocal(this.tooltipKey));
        lines.add("§8" + I18n.translateToLocal(
                "unboundtech.tooltip.shield_price"));
    }
}
