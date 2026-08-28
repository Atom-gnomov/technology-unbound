package unboundtech.common.items;

import ic2.api.energy.tile.IEnergySink;
import ic2.api.item.ElectricItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.ItemWandCasting;
import unboundtech.energy.EnergyCanon;

/**
 * Фокус Заряда (`05_objects/techno_foci.md` §4.1): «переносная розетка
 * сомнительного происхождения» — единственный способ зарядить машину или
 * инструмент IC2 вдали от сети.
 *
 *  - курс: 1 000 EU за 1 вис ({@link EnergyCanon#EU_PER_WAND_VIS_BACK} —
 *    вдвое хуже Таум-Генератора: походный инструмент, не электростанция);
 *  - тратит Ordo и Ignis поровну — по полвиса за операцию;
 *  - кулдаун 10 тиков (§5: до 2 000 EU/сек, если цель принимает);
 *  - вис списывается ТОЛЬКО когда цель приняла EU: сперва целимся,
 *    потом платим;
 *  - фокус отдаёт EU и никогда не берёт обратно (§4.1 ⛔: без петель).
 *
 * Цели: блок-приёмник EU под прицелом или заряжаемый предмет в другой
 * руке. Не принимает — «эта штука не берёт заряд».
 */
public class ItemFocusCharge extends ItemFocusBasic {

    /** 1 вис = 100 сентивис; §5: Ordo + Ignis поровну. */
    private static final AspectList COST = new AspectList()
            .add(Aspect.ORDER, 50).add(Aspect.FIRE, 50);

    public ItemFocusCharge() {
        this.setMaxStackSize(1);
    }

    @Override
    public int getFocusColor(ItemStack stack) {
        return 0x7EDCFF;   // §5: голубой
    }

    @Override
    public AspectList getVisCost(ItemStack stack) {
        return COST;
    }

    @Override
    public int getActivationCooldown(ItemStack stack) {
        return 10;   // §5: 1 вис / 10 тиков удержания
    }

    @Override
    public String getSortingHelper(ItemStack stack) {
        return "UTCH" + super.getSortingHelper(stack);
    }

    @Override
    public ItemStack onFocusRightClick(ItemStack wandstack, World world,
                                       EntityPlayer player, RayTraceResult mop) {
        if (!(wandstack.getItem() instanceof ItemWandCasting) || world.isRemote) {
            return wandstack;
        }
        ItemWandCasting wand = (ItemWandCasting) wandstack.getItem();
        if (!wand.consumeAllVis(wandstack, player, COST, false, false)) {
            return wandstack;   // §4.1: нет нужного примала — фокус молчит
        }
        double packet = EnergyCanon.EU_PER_WAND_VIS_BACK;
        double accepted = this.chargeTarget(world, player, mop, packet);
        if (accepted <= 0) {
            player.sendStatusMessage(new TextComponentString(
                    "§eЭта штука не берёт заряд"), true);
            return wandstack;
        }
        // Цель приняла — теперь платим весь вис операции (§5: цена за
        // операцию, а не за принятый остаток: розетка, не весы).
        wand.consumeAllVis(wandstack, player, COST, true, false);
        player.swingArm(ItemWandCasting.getHandHoldingWand(player, wandstack));
        return wandstack;
    }

    /** Сначала блок под прицелом, затем предмет в другой руке. */
    private double chargeTarget(World world, EntityPlayer player,
                                RayTraceResult mop, double packet) {
        if (mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK) {
            TileEntity te = world.getTileEntity(mop.getBlockPos());
            if (te instanceof IEnergySink) {
                IEnergySink sink = (IEnergySink) te;
                double demanded = sink.getDemandedEnergy();
                if (demanded > 0) {
                    double offer = Math.min(packet, demanded);
                    double left = sink.injectEnergy(
                            mop.sideHit == null ? EnumFacing.UP : mop.sideHit,
                            offer, 32.0);
                    return offer - left;
                }
                return 0;
            }
        }
        ItemStack other = player.getHeldItem(EnumHand.OFF_HAND);
        if (!other.isEmpty() && ElectricItem.manager != null) {
            return ElectricItem.manager.charge(other, packet,
                    Integer.MAX_VALUE, true, false);
        }
        return 0;
    }
}
