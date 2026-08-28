package unboundtech.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * Корпус Эссентиального Накопителя (`essentia_vault.md` §8): глухая плита
 * закалённого таумия с заклёпками. Логики нет — стена библиотеки.
 */
public class BlockVaultCasing extends Block {

    public BlockVaultCasing() {
        super(Material.IRON);
        this.setHardness(3.0f);
        this.setResistance(15.0f);
        this.setSoundType(net.minecraft.block.SoundType.METAL);
        this.setHarvestLevel("pickaxe", 1);
    }
}
