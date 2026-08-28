package unboundtech.common.blocks;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.world.World;
import unboundtech.common.tiles.TileVaultGolemPort;

/**
 * Голем-порт Накопителя (`essentia_vault.md` §4.1): ниша с полкой, к
 * которой подходят големы. Сам ничего не хранит — прокси к контроллеру.
 */
public class BlockVaultGolemPort extends BlockContainer {

    public BlockVaultGolemPort() {
        super(Material.IRON);
        this.setHardness(3.0f);
        this.setResistance(15.0f);
        this.setSoundType(net.minecraft.block.SoundType.METAL);
        this.setHarvestLevel("pickaxe", 1);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileVaultGolemPort();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
}
