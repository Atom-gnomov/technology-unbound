package unboundtech.common.items;

import ic2.api.tile.IWrenchable;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import unboundtech.common.UTItems;

/**
 * Таумиевый ключ (`05_objects/thaumium_wrench.md`).
 *
 * Функциональный близнец бронзового ключа IC2 — но из таумия, чтобы маг мог
 * разбирать машины, не строя бронзовую металлургию (§1).
 *
 * Работает через публичный {@link IWrenchable}: интерфейс блочный, поэтому
 * ключ одинаково берёт и наши машины, и машины IC2 (§10, §12). Шанс потери
 * при разборке НЕ переопределяем (§5) — что вернёт {@code getWrenchDrops}
 * самого блока, то игрок и получит.
 *
 * Внутренности IC2 ({@code ic2.core.item.tool.ItemToolWrench.wrenchBlock})
 * намеренно не зовём: {@code docs/IC2_API_ASSUMPTIONS.md} держит мод на
 * публичном {@code ic2.api.*}, а возвращаемый там тип вообще
 * пакетно-приватный.
 */
public class ItemThaumiumWrench extends Item {

    /** §5: 200 использований. */
    private static final int USES = 200;

    private static final String TOOLTIP_KEY = "unboundtech.tooltip.thaumium_wrench";

    public ItemThaumiumWrench() {
        this.setMaxStackSize(1);
        this.setMaxDamage(USES);
    }

    /**
     * {@code onItemUseFirst}, а не {@code onItemUse}: иначе ПКМ сперва
     * «активирует» блок — машина IC2 открыла бы GUI, и до разборки дело бы
     * не дошло. Ровно по этой же причине хук выбран у ключа самого IC2.
     */
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos,
                                           EnumFacing side, float hitX, float hitY, float hitZ,
                                           EnumHand hand) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof IWrenchable)) {
            return EnumActionResult.PASS;   // чужой блок — ключ его не касается
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        IWrenchable wrenchable = (IWrenchable) state.getBlock();
        return player.isSneaking()
                ? this.remove(wrenchable, player, world, pos, state, hand)
                : this.rotate(wrenchable, player, world, pos, side, hand);
    }

    private EnumActionResult rotate(IWrenchable wrenchable, EntityPlayer player, World world,
                                    BlockPos pos, EnumFacing side, EnumHand hand) {
        if (!wrenchable.canSetFacing(world, pos, side, player)
                || !wrenchable.setFacing(world, pos, side, player)) {
            return EnumActionResult.FAIL;
        }
        this.wear(player, hand);
        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult remove(IWrenchable wrenchable, EntityPlayer player, World world,
                                    BlockPos pos, IBlockState state, EnumHand hand) {
        if (!wrenchable.wrenchCanRemove(world, pos, player)) {
            return EnumActionResult.FAIL;
        }
        // Тайл нужен ДО сноса блока: именно из него блок собирает дроп с
        // сохранённым содержимым (буфер EU и настройки).
        TileEntity tile = world.getTileEntity(pos);
        List<ItemStack> drops = wrenchable.getWrenchDrops(world, pos, state, tile, player, 0);
        world.setBlockToAir(pos);
        if (drops != null) {
            for (ItemStack drop : drops) {
                if (drop != null && !drop.isEmpty()) {
                    Block.spawnAsEntity(world, pos, drop);
                }
            }
        }
        this.wear(player, hand);
        return EnumActionResult.SUCCESS;
    }

    private void wear(EntityPlayer player, EnumHand hand) {
        player.getHeldItem(hand).damageItem(1, player);
    }

    /** §5: чинится обычным таумием — оредикт заводит сам порт. */
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        for (int id : OreDictionary.getOreIDs(repair)) {
            if (UTItems.ORE_THAUMIUM.equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        // §9: игрок не должен выяснять методом тыка, берёт ли ключ машины IC2.
        lines.add("§7" + I18n.translateToLocal(TOOLTIP_KEY));
    }
}
