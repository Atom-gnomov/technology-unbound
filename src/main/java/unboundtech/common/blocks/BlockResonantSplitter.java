package unboundtech.common.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigItems;
import unboundtech.common.tiles.TileResonantSplitter;

/** Резонансный Расщепитель: составной → оба компонента (`resonant_splitter.md`). */
public class BlockResonantSplitter extends BlockMachineBase {

    /** Мета фиала: 0 — пустой, 1 — полный (см. {@code ItemEssence}). */
    private static final int PHIAL_EMPTY = 0;
    private static final int PHIAL_FULL = 1;
    private static final int PHIAL_AMOUNT = 8;

    @Override
    protected int activeLightLevel() {
        return 7;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileResonantSplitter();
    }

    /**
     * ПКМ пустым фиалом (§9): забрать 8 единиц компонента, которого больше.
     * Всё остальное — базовому классу (пустая рука → строка статуса).
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || held.getItem() != ConfigItems.itemEssence
                || held.getItemDamage() != PHIAL_EMPTY) {
            return super.onBlockActivated(world, pos, state, player, hand,
                    facing, hitX, hitY, hitZ);
        }
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileResonantSplitter)) {
            return false;
        }
        TileResonantSplitter splitter = (TileResonantSplitter) tile;
        Aspect aspect = splitter.richestOutput();
        if (aspect == null || !splitter.doesContainerContainAmount(aspect, PHIAL_AMOUNT)) {
            player.sendStatusMessage(new TextComponentString(
                    "§cВ выходе нет полных восьми единиц одного компонента"), true);
            return true;
        }
        splitter.takeFromContainer(aspect, PHIAL_AMOUNT);
        ItemStack phial = new ItemStack(ConfigItems.itemEssence, 1, PHIAL_FULL);
        ConfigItems.itemEssence.setAspects(phial,
                new AspectList().add(aspect, PHIAL_AMOUNT));
        held.shrink(1);
        if (!player.inventory.addItemStackToInventory(phial)) {
            player.dropItem(phial, false);
        }
        return true;
    }
}
