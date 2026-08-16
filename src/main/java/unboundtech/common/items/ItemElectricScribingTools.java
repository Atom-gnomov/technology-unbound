package unboundtech.common.items;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import thaumcraft.api.IScribeTools;

/**
 * Электрическая чернильница (`05_objects/electric_scribing_tools.md`).
 *
 * Не кончается, а разряжается: ставится в любой зарядник IC2 и снова пишет.
 *
 * <h3>Почему трансляция живёт именно в getDamage/setDamage</h3>
 * Движок ТК списывает чернила ДВУМЯ разными путями (§4, проверено по
 * {@code ResearchManager} порта): из инвентаря — через
 * {@code damageItem(1, player)}, из слота стола — через
 * {@code setItemDamage(dmg + 1)} напрямую, мимо {@code damageItem}.
 * Оба в 1.12.2 приходят в {@link Item#setDamage}/{@link Item#getDamage}, потому
 * что Forge пропатчил {@code ItemStack.getItemDamage()} и
 * {@code setItemDamage(int)} на делегирование предмету. Значит одна пара
 * переопределений закрывает оба пути, и обработчик стола писать не нужно.
 *
 * Заряд хранит сам IC2 (NBT-ключ {@code charge}); его менеджер прочность НЕ
 * трогает (сверено по байткоду {@code ic2.core.item.ElectricItemManager}),
 * так что рекурсии между двумя учётами нет.
 *
 * {@link IScribeTools} — пустой маркерный интерфейс, реализуется ради того,
 * чтобы стол и записки предмет увидели.
 */
public class ItemElectricScribingTools extends Item implements IScribeTools, IElectricItem {

    /** §5: ёмкость 4 000 EU. */
    public static final double CAPACITY = 4_000.0;
    /** §5: 50 EU за очко исследования. */
    public static final double EU_PER_POINT = 50.0;
    /** §5: 80 очков на полный заряд — это же и «прочность» в терминах ТК. */
    public static final int RESEARCH_POINTS = (int) (CAPACITY / EU_PER_POINT);
    /** §5: тир LV — заряжается от чего угодно. */
    private static final int TIER = 1;
    /** Предел передачи LV. Канон числа не задаёт — берём тировый потолок. */
    private static final double TRANSFER_LIMIT = 32.0;

    private static final String TOOLTIP_KEY = "unboundtech.tooltip.electric_scribing";

    public ItemElectricScribingTools() {
        this.setMaxStackSize(1);
        this.setMaxDamage(RESEARCH_POINTS);
        this.setNoRepair();
    }

    // ================= прочность ↔ заряд =================

    /**
     * ⚠️ Чтение заряда обязано быть БЕЗ побочных эффектов: {@link #getDamage}
     * зовут на каждом кадре отрисовки и при каждом сравнении стеков.
     *
     * А {@code ElectricItem.manager.getCharge} побочный эффект имеет: он
     * реализован как {@code discharge(..., simulate)}, и внутри стоит
     * {@code StackUtil.getOrCreateNbtData(stack)} — то есть чтение НАВЕШИВАЕТ
     * на стек NBT-тег (проверено по байткоду
     * {@code ic2.core.item.ElectricItemManager}). Поэтому у стека без тега
     * заряд возвращаем нулём, не спрашивая менеджер: тега нет — значит IC2
     * ничего в него и не клал.
     */
    private static double charge(ItemStack stack) {
        if (!stack.hasTagCompound() || ElectricItem.manager == null) {
            return 0.0;
        }
        return ElectricItem.manager.getCharge(stack);
    }

    /** Полный заряд = 0 «урона», пустой = 80. */
    @Override
    public int getDamage(ItemStack stack) {
        int points = (int) Math.floor(charge(stack) / EU_PER_POINT);
        return Math.max(0, RESEARCH_POINTS - Math.min(RESEARCH_POINTS, points));
    }

    /**
     * Считаем ДЕЛЬТУ в очках и снимаем ровно её цену, а не выставляем заряд
     * по формуле: иначе остаток от неполного очка (зарядник даёт не кратно
     * 50 EU) сгорал бы при каждой записи.
     */
    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (ElectricItem.manager == null) {
            return;
        }
        int delta = damage - this.getDamage(stack);
        if (delta > 0) {
            ElectricItem.manager.discharge(stack, delta * EU_PER_POINT, TIER, true, false, false);
        } else if (delta < 0) {
            ElectricItem.manager.charge(stack, -delta * EU_PER_POINT, TIER, true, false);
        }
        // Разряженная чернильница НЕ ломается (§10): стол просто перестаёт её
        // видеть — consumeInkFromPlayer берёт лишь стеки с damage < maxDamage.
    }

    // ================= IElectricItem =================

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;   // потребитель, а не батарейка
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return CAPACITY;
    }

    @Override
    public int getTier(ItemStack stack) {
        return TIER;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return TRANSFER_LIMIT;
    }

    // ================= показ игроку =================

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        int stored = (int) charge(stack);
        lines.add("§b" + stored + " / " + (int) CAPACITY + " EU");
        lines.add("§7" + I18n.translateToLocal(TOOLTIP_KEY)
                + ": " + (RESEARCH_POINTS - this.getDamage(stack)));
    }

    /** В креативе — пустая и заряженная, как у электроинструментов IC2. */
    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!this.isInCreativeTab(tab)) {
            return;
        }
        items.add(new ItemStack(this));
        ItemStack full = new ItemStack(this);
        if (ElectricItem.manager != null) {
            ElectricItem.manager.charge(full, CAPACITY, TIER, true, false);
        }
        items.add(full);
    }
}
