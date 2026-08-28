package unboundtech.common.items;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Кольца Схемы (`05_objects/schema_rings.md`): единственная вещь мода,
 * меняющая САМОГО игрока — ванильные атрибуты, работают с любой бронёй.
 * «Кольцо не колдует. Кольцо напоминает телу, каким оно могло бы быть,
 * если бы его собирали по чертежу» (§3).
 *
 *  - 2 EU/t пока надето и заряжено; списание пачкой 40 EU раз в 20 тиков
 *    (§4.3); буфер 100 000 EU ≈ 42 минуты;
 *  - кончился заряд — атрибуты СНЯТЫ, кольцо цело; вернулся — вернулись;
 *  - постоянные UUID модификаторов: второе такое же кольцо не даёт
 *    ничего (§4: не складываются);
 *  - снятие maxHealth зажимает текущее здоровье (§4.2: игрок на 20/24
 *    после снятия должен оказаться на 20/20, а не умереть);
 *  - рунного заряда не дают вовсе — это плата за атрибуты (§4).
 */
public class ItemSchemaRing extends Item implements IBauble, IElectricItem {

    /** §5. */
    public static final double MAX_CHARGE = 100_000.0;
    public static final int EU_PER_TICK = 2;
    private static final int BILL_TICKS = 20;
    private static final int TIER = 1;

    /** Четыре кольца (§4.1); у каждого свои атрибуты и свои UUID. */
    public enum Variant {
        FRAME("frame"), DRIVE("drive"), STRIDE("stride"), BRACE("brace");

        final String key;

        Variant(String key) {
            this.key = key;
        }
    }

    private static final UUID[] UUIDS = {
            UUID.fromString("7c1a4bd0-2b6e-4c7e-9a35-8f4e10c30001"),
            UUID.fromString("7c1a4bd0-2b6e-4c7e-9a35-8f4e10c30002"),
            UUID.fromString("7c1a4bd0-2b6e-4c7e-9a35-8f4e10c30003"),
            UUID.fromString("7c1a4bd0-2b6e-4c7e-9a35-8f4e10c30004"),
            UUID.fromString("7c1a4bd0-2b6e-4c7e-9a35-8f4e10c30005"),
            UUID.fromString("7c1a4bd0-2b6e-4c7e-9a35-8f4e10c30006"),
            UUID.fromString("7c1a4bd0-2b6e-4c7e-9a35-8f4e10c30007"),
            UUID.fromString("7c1a4bd0-2b6e-4c7e-9a35-8f4e10c30008"),
    };

    public final Variant variant;

    public ItemSchemaRing(Variant variant) {
        this.variant = variant;
        this.setMaxStackSize(1);
    }

    /** Пары (атрибут, UUID, значение, операция) по §4.1. */
    private Object[][] modifiers() {
        switch (this.variant) {
            case FRAME:
                return new Object[][]{
                        {SharedMonsterAttributes.MAX_HEALTH, UUIDS[0], 4.0, 0},
                        {SharedMonsterAttributes.ARMOR_TOUGHNESS, UUIDS[1], 1.0, 0}};
            case DRIVE:
                return new Object[][]{
                        {SharedMonsterAttributes.ATTACK_SPEED, UUIDS[2], 0.4, 0},
                        {SharedMonsterAttributes.ATTACK_DAMAGE, UUIDS[3], 1.0, 0}};
            case STRIDE:
                // §4.1: операция 2 — множитель от итога
                return new Object[][]{
                        {SharedMonsterAttributes.MOVEMENT_SPEED, UUIDS[4], 0.12, 2}};
            default:
                return new Object[][]{
                        {SharedMonsterAttributes.KNOCKBACK_RESISTANCE, UUIDS[5], 0.5, 0},
                        {SharedMonsterAttributes.ARMOR, UUIDS[6], 2.0, 0}};
        }
    }

    // ================= IBauble =================

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (wearer.world.isRemote || wearer.ticksExisted % BILL_TICKS != 0) {
            return;
        }
        // §4.3: списание пачкой раз в 20 тиков — дешевле, чем каждый тик
        int bill = EU_PER_TICK * BILL_TICKS;
        boolean powered = ElectricItem.manager != null
                && ElectricItem.manager.discharge(stack, bill, TIER,
                        true, true, true) >= bill;
        if (powered) {
            ElectricItem.manager.discharge(stack, bill, TIER, true, true, false);
            this.apply(wearer);
        } else {
            this.remove(wearer);
        }
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (!wearer.world.isRemote) {
            this.remove(wearer);
        }
    }

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    private void apply(EntityLivingBase wearer) {
        for (Object[] mod : this.modifiers()) {
            IAttributeInstance inst = wearer.getEntityAttribute((IAttribute) mod[0]);
            UUID id = (UUID) mod[1];
            if (inst != null && inst.getModifier(id) == null) {
                // постоянный UUID: второе такое же кольцо не даст ничего
                inst.applyModifier(new AttributeModifier(id,
                        "unboundtech.ring." + this.variant.key,
                        (Double) mod[2], (Integer) mod[3]));
            }
        }
    }

    private void remove(EntityLivingBase wearer) {
        for (Object[] mod : this.modifiers()) {
            IAttributeInstance inst = wearer.getEntityAttribute((IAttribute) mod[0]);
            UUID id = (UUID) mod[1];
            if (inst != null && inst.getModifier(id) != null) {
                inst.removeModifier(id);
            }
        }
        // §4.2: снятие maxHealth обязано зажать текущее здоровье — иначе
        // игрок на 20/24 умирает «на ровном месте»
        if (wearer.getHealth() > wearer.getMaxHealth()) {
            wearer.setHealth(wearer.getMaxHealth());
        }
    }

    // ================= IElectricItem =================

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return MAX_CHARGE;
    }

    @Override
    public int getTier(ItemStack stack) {
        return TIER;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return 32.0;   // §5: зарядка LV
    }

    // ================= тултип =================

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        lines.add("§7" + I18n.translateToLocal(
                "unboundtech.tooltip.ring_" + this.variant.key));
        lines.add("§7" + I18n.translateToLocal("unboundtech.tooltip.ring_upkeep"));
    }
}
