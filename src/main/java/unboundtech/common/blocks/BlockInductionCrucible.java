package unboundtech.common.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.entities.EntitySpecialItem;
import unboundtech.common.tiles.TileInductionCrucible;

/**
 * Индукционный Тигель (`induction_crucible.md`): чаша родного тигля в
 * станине машины IC2. Коллизия ниже полного куба — предмет проваливается
 * «в чашу» и попадает в {@code onEntityCollision}, как у BlockMetalDevice
 * порта (полный куб предметы не ловит: они лежат на поверхности).
 */
public class BlockInductionCrucible extends BlockMachineBase {

    private static final AxisAlignedBB BOWL_AABB =
            new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);

    /** Мета фиала: 0 — пустой, 1 — полный (см. {@code ItemEssence}). */
    private static final int PHIAL_EMPTY = 0;
    private static final int PHIAL_FULL = 1;
    private static final int PHIAL_AMOUNT = 8;

    private int delay;

    @Override
    protected int activeLightLevel() {
        return 7;   // §8: уровень света в работе — как у машин IC2
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileInductionCrucible();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOWL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** Брошенный предмет коснулся чаши — как у родного тигля. */
    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (world.isRemote) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileInductionCrucible)) {
            return;
        }
        TileInductionCrucible tile = (TileInductionCrucible) te;
        boolean boiling = tile.isHot() && tile.hasWater();
        if (entity instanceof EntityItem && !(entity instanceof EntitySpecialItem)) {
            if (boiling) {
                tile.attemptSmelt((EntityItem) entity);
            }
            return;
        }
        this.delay++;
        if (this.delay < 10) {
            return;
        }
        this.delay = 0;
        if (entity instanceof EntityLivingBase && boiling) {
            entity.attackEntityFrom(DamageSource.IN_FIRE, 1.0F);
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.BLOCKS, 0.4F, 2.0F + world.rand.nextFloat() * 0.4F);
        }
    }

    /**
     * §9: ПКМ ведром — вода; ПКМ пустым фиалом — забрать 8 единиц первого
     * по алфавиту аспекта с запасом; пустой рукой — строка статуса (база).
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty()
                && held.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            return FluidUtil.interactWithFluidHandler(player, hand, world, pos, facing);
        }
        if (!held.isEmpty() && held.getItem() == ConfigItems.itemEssence
                && held.getItemDamage() == PHIAL_EMPTY) {
            if (world.isRemote) {
                return true;
            }
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof TileInductionCrucible)) {
                return false;
            }
            TileInductionCrucible crucible = (TileInductionCrucible) tile;
            Aspect aspect = crucible.firstAspect(PHIAL_AMOUNT);
            if (aspect == null) {
                player.sendStatusMessage(new TextComponentString(
                        "§cВ тигле нет полных восьми единиц одного аспекта"), true);
                return true;
            }
            crucible.takeFromContainer(aspect, PHIAL_AMOUNT);
            ItemStack phial = new ItemStack(ConfigItems.itemEssence, 1, PHIAL_FULL);
            ConfigItems.itemEssence.setAspects(phial,
                    new AspectList().add(aspect, PHIAL_AMOUNT));
            held.shrink(1);
            if (!player.inventory.addItemStackToInventory(phial)) {
                player.dropItem(phial, false);
            }
            return true;
        }
        return super.onBlockActivated(world, pos, state, player, hand,
                facing, hitX, hitY, hitZ);
    }
}
