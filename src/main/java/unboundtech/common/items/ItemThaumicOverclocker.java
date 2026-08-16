package unboundtech.common.items;

import ic2.api.upgrade.IProcessingUpgrade;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileJarFillable;
import unboundtech.energy.OverclockRules;

/**
 * Таум-Оверклокер (`05_objects/thaumic_overclocker.md`): апгрейд машины IC2,
 * который продаёт скорость за эссенцию, а не за электричество. ×0.6 времени
 * при ×1.2 потребления — выгоднее родного (0.7/1.6) по обеим осям; вся цена
 * вынесена в заряд (Machina + Motus) и перегрев.
 *
 * Контракт IC2: {@link IProcessingUpgrade}. Машина сама опрашивает множители
 * и зовёт {@code onTick} каждый свой тик ({@code InvSlotUpgrade.tickNoMark}).
 * {@code maxStackSize = 1} ОБЯЗАТЕЛЕН: IC2 возводит множитель в степень
 * размера стопки.
 *
 * <h3>Активность машины</h3>
 * {@link IUpgradableBlock} не сообщает, работает ли машина. Единственный
 * честный сигнал — {@code ic2.core.block.TileEntityBlock.getActive()}:
 * осознанное отступление от правила «только ic2.api.*» (читаем одно
 * публичное поле состояния, ничего не вызываем на запись). Для машин чужих
 * модов, не наследующих этот класс, фолбэк — «активна всегда»: перегрев
 * гарантирован, вечного бесплатного ускорения нет.
 *
 * Отступления от карточки (§8): слои модели «пусто/перегрев» не сделаны —
 * одна текстура, состояние видно в тултипе; полоса нагрева в гогглах — 🔜.
 */
public class ItemThaumicOverclocker extends Item implements IProcessingUpgrade {

    private static final String NBT_MACHINA = "UTMachina";
    private static final String NBT_MOTUS = "UTMotus";
    private static final String NBT_HEAT = "UTHeat";
    private static final String NBT_INERT = "UTInert";
    private static final String NBT_WORK = "UTWork";

    public ItemThaumicOverclocker() {
        this.setMaxStackSize(1);   // Math.pow(mult, stackCount) — стопка запрещена
    }

    // ================= IProcessingUpgrade =================

    @Override
    public boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> properties) {
        return properties.contains(UpgradableProperty.Processing);
    }

    @Override
    public double getProcessTimeMultiplier(ItemStack stack, IUpgradableBlock parent) {
        return this.isBoosting(stack) ? OverclockRules.PROCESS_TIME_MULTIPLIER : 1.0D;
    }

    @Override
    public double getEnergyDemandMultiplier(ItemStack stack, IUpgradableBlock parent) {
        return this.isBoosting(stack) ? OverclockRules.ENERGY_DEMAND_MULTIPLIER : 1.0D;
    }

    @Override
    public int getExtraProcessTime(ItemStack stack, IUpgradableBlock parent) {
        return 0;
    }

    @Override
    public int getExtraEnergyDemand(ItemStack stack, IUpgradableBlock parent) {
        return 0;
    }

    @Override
    public Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock parent,
                                              Collection<ItemStack> output) {
        return output;   // выход не трогаем
    }

    /**
     * Сердце апгрейда. Возвращает {@code true} ТОЛЬКО при смене состояния
     * «ускоряет ↔ не ускоряет»: это заставляет машину сделать
     * {@code markDirty()} → {@code InvSlotUpgrade.onChanged()} →
     * {@code resetRates()}, и множители перечитываются (§4.2 карточки).
     * Возвращать true каждый тик — постоянный markDirty впустую.
     */
    @Override
    public boolean onTick(ItemStack stack, IUpgradableBlock parent) {
        if (!(parent instanceof TileEntity)) {
            return false;
        }
        TileEntity machine = (TileEntity) parent;
        World world = machine.getWorld();
        if (world == null || world.isRemote) {
            return false;
        }
        NBTTagCompound tag = tagOf(stack);
        boolean wasBoosting = this.isBoosting(stack);

        int inert = tag.getInteger(NBT_INERT);
        if (inert > 0) {
            tag.setInteger(NBT_INERT, inert - 1);
            return this.isBoosting(stack) != wasBoosting;
        }

        boolean active = machineActive(machine);
        int heat = tag.getInteger(NBT_HEAT);
        if (active && wasBoosting) {
            // Греет и тратит только пока реально ускоряет: нагрев — плата
            // за выигрыш, а не за сам факт присутствия в слоте.
            heat += OverclockRules.HEAT_PER_ACTIVE_TICK;
            int work = tag.getInteger(NBT_WORK) + 1;
            if (work >= OverclockRules.TICKS_PER_ESSENTIA_UNIT) {
                work = 0;
                tag.setInteger(NBT_MACHINA, Math.max(0, tag.getInteger(NBT_MACHINA) - 1));
                tag.setInteger(NBT_MOTUS, Math.max(0, tag.getInteger(NBT_MOTUS) - 1));
            }
            tag.setInteger(NBT_WORK, work);
            if (heat >= OverclockRules.HEAT_THRESHOLD) {
                heat = 0;
                tag.setInteger(NBT_INERT, OverclockRules.INERT_TICKS);
                ventFluxGas(world, machine.getPos());
            }
        } else if (!active) {
            heat = Math.max(0, heat - OverclockRules.COOLING_PER_IDLE_TICK);
        }
        tag.setInteger(NBT_HEAT, heat);
        return this.isBoosting(stack) != wasBoosting;
    }

    /**
     * §4.2: при перегреве над машиной встаёт блок флюкс-газа. Некуда — выброс
     * пропускается (не наказываем дважды), но перегрев всё равно случился.
     */
    private static void ventFluxGas(World world, BlockPos machine) {
        for (int i = 0; i < OverclockRules.FLUX_PER_OVERHEAT; i++) {
            BlockPos at = machine.up(1 + i);
            if (world.isAirBlock(at)) {
                world.setBlockState(at, ConfigBlocks.blockFluxGas.getDefaultState(), 3);
            }
        }
    }

    /** См. javadoc класса: core-класс IC2 читается, фолбэк — «активна». */
    private static boolean machineActive(TileEntity machine) {
        if (machine instanceof ic2.core.block.TileEntityBlock) {
            return ((ic2.core.block.TileEntityBlock) machine).getActive();
        }
        return true;
    }

    // ================= заряд =================

    /** Ускоряет = есть заряд обоих пулов и не в перегреве. */
    public boolean isBoosting(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null
                && tag.getInteger(NBT_INERT) <= 0
                && tag.getInteger(NBT_MACHINA) > 0
                && tag.getInteger(NBT_MOTUS) > 0;
    }

    private static NBTTagCompound tagOf(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    /**
     * Заправка — ПКМ по банке эссенции (§4.1): из банки с {@code Machina}
     * пополняется пул Machina, с {@code Motus} — Motus, по единице за
     * единицу. Чужой аспект не берётся.
     */
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos,
                                           EnumFacing side, float hitX, float hitY, float hitZ,
                                           EnumHand hand) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileJarFillable)) {
            return EnumActionResult.PASS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        TileJarFillable jar = (TileJarFillable) te;
        String pool = jar.aspect == Aspect.MECHANISM ? NBT_MACHINA
                : jar.aspect == Aspect.MOTION ? NBT_MOTUS : null;
        if (pool == null) {
            return EnumActionResult.PASS;   // банка с чужим аспектом — не наш жест
        }
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = tagOf(stack);
        Aspect aspect = jar.aspect;
        int have = tag.getInteger(pool);
        int taken = 0;
        while (have + taken < OverclockRules.CHARGE_CAPACITY
                && jar.takeFromContainer(aspect, 1)) {
            taken++;
        }
        if (taken > 0) {
            tag.setInteger(pool, have + taken);
            player.sendStatusMessage(new TextComponentString("§5" + aspect.getName()
                    + ": " + (have + taken) + " / " + OverclockRules.CHARGE_CAPACITY), true);
        }
        return EnumActionResult.SUCCESS;
    }

    // ================= показ игроку =================

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        NBTTagCompound tag = stack.getTagCompound();
        int machina = tag == null ? 0 : tag.getInteger(NBT_MACHINA);
        int motus = tag == null ? 0 : tag.getInteger(NBT_MOTUS);
        int inert = tag == null ? 0 : tag.getInteger(NBT_INERT);
        lines.add("§5Machina " + machina + "/" + OverclockRules.CHARGE_CAPACITY
                + " · Motus " + motus + "/" + OverclockRules.CHARGE_CAPACITY);
        lines.add("§7×" + OverclockRules.PROCESS_TIME_MULTIPLIER + " "
                + I18n.translateToLocal("unboundtech.tooltip.overclocker.time")
                + ", ×" + OverclockRules.ENERGY_DEMAND_MULTIPLIER + " "
                + I18n.translateToLocal("unboundtech.tooltip.overclocker.power"));
        if (inert > 0) {
            lines.add("§c" + I18n.translateToLocal("unboundtech.tooltip.overclocker.hot")
                    + ": " + inert);
        } else if (machina <= 0 || motus <= 0) {
            lines.add("§c" + I18n.translateToLocal("unboundtech.tooltip.overclocker.empty"));
        }
    }
}
