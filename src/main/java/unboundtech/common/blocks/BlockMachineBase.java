package unboundtech.common.blocks;

import ic2.api.tile.IWrenchable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import unboundtech.common.tiles.IMachineStatus;

/**
 * Общий каркас блока-машины Unbound Technology (шаблон из
 * phase3_converters_spec.md §0.6): горизонтальный FACING + собственный
 * ACTIVE, мета = {@code facing | (active ? 8 : 0)}.
 *
 * Свечение при работе задаётся {@link #activeLightLevel()} — по канону
 * спеки генератор светится слабо (7), пока перерабатывает вис.
 */
public abstract class BlockMachineBase extends BlockContainer implements IWrenchable {

    public static final PropertyDirection FACING = PropertyDirection.create(
            "facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    protected BlockMachineBase() {
        super(Material.IRON);
        this.setHardness(3.0f);
        this.setResistance(15.0f);
        this.setSoundType(net.minecraft.block.SoundType.METAL);
        // Канон карточек: «кирка >= 1». Без этого Material.IRON ломается
        // деревянной киркой с дропом (ForgeHooks.canHarvestBlock уходит в
        // ItemPickaxe.canHarvestBlock, который для IRON верен на любом уровне).
        this.setHarvestLevel("pickaxe", 1);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(ACTIVE, Boolean.FALSE));
    }

    /** Уровень света работающей машины (0 — не светится). */
    protected int activeLightLevel() {
        return 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ACTIVE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(ACTIVE, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex()
                | (state.getValue(ACTIVE) ? 8 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        // Морда машины смотрит на игрока, как у машин IC2.
        return this.getDefaultState()
                .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
                .withProperty(ACTIVE, Boolean.FALSE);
    }

    @Override
    public IBlockState withRotation(IBlockState state, net.minecraft.util.Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, net.minecraft.util.Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(ACTIVE) ? this.activeLightLevel() : 0;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    /**
     * Переключает ACTIVE. Тайл переживает смену состояния сам: {@code Chunk
     * .setBlockState} зовёт {@code breakBlock} и пересоздаёт TileEntity только
     * когда МЕНЯЕТСЯ САМ БЛОК, а у нас блок тот же — меняется лишь свойство.
     *
     * Поэтому здесь НЕТ пляски ванильной печи (сохранить тайл → setBlockState →
     * validate + setTileEntity): печи она нужна лишь потому, что горящая и
     * потухшая печь — два разных блока. Нам она была бы вредна: setTileEntity
     * во время тика тайлов кладёт тайл в {@code addedTileEntityList} повторно,
     * и машина начинает тикать дважды за тик.
     */
    public static void setActive(World world, BlockPos pos, boolean active) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockMachineBase)
                || state.getValue(ACTIVE) == active) {
            return;
        }
        world.setBlockState(pos, state.withProperty(ACTIVE, active), 3);
        // Пометить чанк изменённым обязан кто-то: setBlockState этого не делает
        // для тайла, а машина копит EU и флаги в NBT. Без markDirty автосейв
        // (Chunk.needsSaving -> isModified) может не сохранить буфер вовсе.
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        if (tile != null) {
            tile.markDirty();
        }
    }

    @Override
    @Nullable
    public abstract net.minecraft.tileentity.TileEntity createNewTileEntity(World world, int meta);

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this);
    }

    // ================= Обратная связь: строка статуса =================

    /**
     * ПКМ пустой рукой — строка статуса (канон `machine_feedback.md` §4).
     * GUI у машин нет намеренно, поэтому без этого игрок не отличит
     * «интерференция» от «нет узла» и от «буфер полон».
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote || !player.getHeldItem(hand).isEmpty()) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof IMachineStatus)) {
            return false;
        }
        // Строка в hotbar, а не в чат: статус смотрят часто, засорять чат нельзя.
        player.sendStatusMessage(
                new TextComponentString(((IMachineStatus) tile).getStatusLine()), true);
        return true;
    }

    // ================= IWrenchable: разборка ключом =================
    //
    // Интерфейс реализует именно БЛОК (все методы принимают World+BlockPos) —
    // проверено по ic2.api.tile.IWrenchable в 2.8.195. Бронзовый ключ IC2 и
    // наш таумиевый работают через него одинаково.

    @Override
    public EnumFacing getFacing(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockMachineBase
                ? state.getValue(FACING) : EnumFacing.NORTH;
    }

    @Override
    public boolean setFacing(World world, BlockPos pos, EnumFacing newDirection,
                             EntityPlayer player) {
        if (newDirection.getAxis() == EnumFacing.Axis.Y) {
            return false;   // машины горизонтальные
        }
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockMachineBase)) {
            return false;
        }
        world.setBlockState(pos, state.withProperty(FACING, newDirection), 3);
        return true;
    }

    @Override
    public boolean wrenchCanRemove(World world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    /**
     * Дроп при разборке ключом — с сохранённым содержимым в NBT.
     * Кирка, в отличие от ключа, роняет машину пустой (канон §10 карточек).
     */
    @Override
    public List<ItemStack> getWrenchDrops(World world, BlockPos pos, IBlockState state,
                                          TileEntity te, EntityPlayer player, int fortune) {
        ItemStack drop = new ItemStack(this);
        if (te instanceof IMachineStatus) {
            NBTTagCompound saved = new NBTTagCompound();
            ((IMachineStatus) te).writeWrenchNBT(saved);
            // stable_39: func_82582_d = isEmpty(); hasNoTags() — имя из
            // snapshot-маппингов, на нашей сборке его нет.
            if (!saved.isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag(WRENCH_TAG, saved);
                drop.setTagCompound(tag);
            }
        }
        List<ItemStack> drops = new ArrayList<>();
        drops.add(drop);
        return drops;
    }

    /** Ключ имени NBT, под которым живёт сохранённое содержимое машины. */
    public static final String WRENCH_TAG = "UTWrench";

    /** Восстанавливает содержимое из «ключевого» дропа при установке. */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote || stack.getTagCompound() == null
                || !stack.getTagCompound().hasKey(WRENCH_TAG)) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IMachineStatus) {
            ((IMachineStatus) tile).readWrenchNBT(stack.getTagCompound().getCompoundTag(WRENCH_TAG));
            tile.markDirty();
        }
    }
}
