package unboundtech.common.world;

import java.util.Random;
import net.minecraft.block.BlockPlanks;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import thaumcraft.common.config.ConfigItems;
import unboundtech.common.entities.UTVillagers;
import unboundtech.compat.ic2.IC2Handles;

/**
 * Хижина Изгнанника (`technomancer_villager.md` §4.2): сарай на отшибе —
 * не ближе 200 блоков к деревне, «видно и деревню вдали, и дикую
 * местность». Внутри техномаг, сундук с лутом-зацепкой, верстак и
 * алхимическая полка; во дворе — обе школы, обе не работают.
 *
 * Прототипные упрощения: частота — примерно ведьмина хижина (шанс на
 * чанк); дистанция до другой хижины не отслеживается (нет персистенции);
 * постройка — процедурный сарай 7×5 из досок с «жестяной» крышей из
 * железных плит.
 */
public class WorldGenExileHut implements IWorldGenerator {

    private static final int CHANCE = 900;
    private static final int VILLAGE_EXCLUSION = 200;

    @Override
    public void generate(Random rand, int chunkX, int chunkZ, World world,
                         IChunkGenerator generator, IChunkProvider provider) {
        if (world.provider.getDimension() != 0
                || rand.nextInt(CHANCE) != 0) {
            return;
        }
        BlockPos at = world.getTopSolidOrLiquidBlock(new BlockPos(
                chunkX * 16 + 8, 0, chunkZ * 16 + 8));
        Biome biome = world.getBiome(at);
        if (biome.getRegistryName() == null
                || Biome.getIdForBiome(biome) == 0
                || world.getBlockState(at.down()).getMaterial().isLiquid()) {
            return;   // §4.2: любые обитаемые, кроме океана
        }
        Village village = world.getVillageCollection()
                .getNearestVillage(at, VILLAGE_EXCLUSION);
        if (village != null) {
            return;   // §4.2: изгнание теряет смысл рядом с деревней
        }
        this.build(world, at, rand);
    }

    private void build(World world, BlockPos origin, Random rand) {
        net.minecraft.block.state.IBlockState planks = Blocks.PLANKS
                .getDefaultState().withProperty(BlockPlanks.VARIANT,
                        BlockPlanks.EnumType.SPRUCE);
        net.minecraft.block.state.IBlockState roof = Blocks.IRON_TRAPDOOR
                .getDefaultState();

        // площадка 7x5, стены высотой 3, дверной проём на юге
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 5; z++) {
                BlockPos floor = origin.add(x, 0, z);
                world.setBlockState(floor, planks, 2);
                for (int y = 1; y <= 3; y++) {
                    BlockPos wall = origin.add(x, y, z);
                    boolean edge = x == 0 || x == 6 || z == 0 || z == 4;
                    boolean door = x == 3 && z == 4 && y <= 2;
                    if (edge && !door) {
                        world.setBlockState(wall, planks, 2);
                    } else {
                        world.setBlockToAir(wall);
                    }
                }
                // жестяная крыша
                world.setBlockState(origin.add(x, 4, z), roof, 2);
            }
        }
        // окна
        world.setBlockState(origin.add(0, 2, 2), Blocks.GLASS_PANE.getDefaultState(), 2);
        world.setBlockState(origin.add(6, 2, 2), Blocks.GLASS_PANE.getDefaultState(), 2);

        // §4.2: и верстак с проводами, и алхимическая полка
        world.setBlockState(origin.add(1, 1, 1),
                Blocks.CRAFTING_TABLE.getDefaultState(), 2);
        world.setBlockState(origin.add(5, 1, 1),
                Blocks.BOOKSHELF.getDefaultState(), 2);

        // сундук с лутом-зацепкой
        BlockPos chestPos = origin.add(5, 1, 3);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState()
                .withProperty(net.minecraft.block.BlockChest.FACING,
                        EnumFacing.WEST), 2);
        TileEntity te = world.getTileEntity(chestPos);
        if (te instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) te;
            chest.setInventorySlotContents(2, new ItemStack(
                    ConfigItems.itemEssence, 1 + rand.nextInt(2), 0));
            ItemStack copper = IC2Handles.item("ingot", "copper");
            if (!copper.isEmpty()) {
                copper.setCount(2 + rand.nextInt(4));
                chest.setInventorySlotContents(4, copper);
            }
            chest.setInventorySlotContents(6, new ItemStack(Items.PAPER));
        }

        // двор: сломанная машина и погасший нитор — обе школы, обе не
        // работают (§4.2); прототип — котёл и незажжённый факел ТК нет,
        // ставим наковальню-хлам и котёл
        world.setBlockState(origin.add(-1, 1, 2),
                Blocks.CAULDRON.getDefaultState(), 2);
        world.setBlockState(origin.add(7, 1, 2),
                Blocks.ANVIL.getDefaultState(), 2);

        // сам изгнанник
        UTVillagers.spawn(world, origin.add(3, 1, 2));
    }
}
