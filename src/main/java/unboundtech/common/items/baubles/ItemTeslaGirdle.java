package unboundtech.common.items.baubles;

import baubles.api.BaubleType;
import net.minecraft.item.ItemStack;

/**
 * Тесла-Пояс (`protective_baubles.md` §4.2): рунный заряд 12 — против
 * 10 у пояса ТК, и бьёт в ответ. Когда ЩИТ поглотил удар в ближнем
 * бою, пояс разряжается в нападавшего: 6 урона электричеством + цепь
 * на 2 цели в 4 блоках, 5 000 EU, кулдаун 3 секунды.
 *
 * Сам разряд живёт в {@code UTRunicBillingHandler}: только там видно,
 * что щит ТК действительно съел удар (сравнение заряда до и после
 * {@code EventHandlerRunic}).
 *
 * Кинетический пояс ТК (взрыв при пробое) остаётся лучше в толпе: он
 * бьёт всех, мы — троих и без разрушения (§4.2, разные роли).
 */
public class ItemTeslaGirdle extends ItemPoweredBauble {

    /** §5. */
    public static final int RUNIC_CHARGE = 12;
    public static final double MAX_CHARGE = 500_000.0;
    private static final int TIER = 2;                  // MV
    private static final double TRANSFER = 128.0;
    /** §4.2: разряд. */
    public static final int ZAP_EU = 5_000;
    public static final float ZAP_DAMAGE = 6.0F;
    public static final double CHAIN_RANGE = 4.0;
    public static final int CHAIN_TARGETS = 2;
    /** §4.2: только живой источник в 5 блоках — иначе бьём в пустоту. */
    public static final double SOURCE_RANGE = 5.0;
    public static final int ZAP_COOLDOWN = 60;          // 3 секунды

    private static final String TAG_COOLDOWN = "UTZapCooldown";

    public ItemTeslaGirdle() {
        super(RUNIC_CHARGE, MAX_CHARGE, TIER, TRANSFER,
                "unboundtech.tooltip.tesla_girdle");
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.BELT;
    }

    /**
     * Кулдаун хранится АБСОЛЮТНЫМ тиком мира (вердикт скептика): запись
     * NBT каждый тик при willAutoSync=true давала бы пакет в тик.
     */
    public static boolean onCooldown(ItemStack stack,
                                     net.minecraft.world.World world) {
        long now = world.getTotalWorldTime();
        long ready = readyAt(stack);
        // верхняя граница: срок из чужого/старшего мира (бэкап, перенос)
        // не должен блокировать предмет навсегда (вердикт решателя)
        return ready > now && ready <= now + ZAP_COOLDOWN;
    }

    private static long readyAt(ItemStack stack) {
        return stack.hasTagCompound()
                ? stack.getTagCompound().getLong(TAG_COOLDOWN) : 0L;
    }

    public static void startCooldown(ItemStack stack,
                                     net.minecraft.world.World world) {
        stack.setTagInfo(TAG_COOLDOWN, new net.minecraft.nbt.NBTTagLong(
                world.getTotalWorldTime() + ZAP_COOLDOWN));
    }
}
