package unboundtech.common.blocks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import unboundtech.common.items.ItemCartridge;
import unboundtech.common.tiles.IMachineStatus;
import unboundtech.common.tiles.TileMechanistMortar;

/**
 * Мортира Механистов (`mechanist_mortar.md`): орудие, не машина —
 * прочность 6/40, кирка >= 2, рендер целиком TESR (блок невидим).
 *
 * ПКМ патронами — доложить короб; ПКМ пустой рукой в РУЧНОМ режиме —
 * выстрел по взгляду; иначе и Shift-ПКМ — статус. Ключ (IC2/таумиевый)
 * циклит режимы. Одна мортира на чанк (§4.1) — проверка при установке.
 */
public class BlockMechanistMortar extends BlockContainer
        implements ic2.api.tile.IWrenchable {

    public BlockMechanistMortar() {
        super(Material.IRON);
        this.setHardness(6.0f);
        this.setResistance(40.0f);
        this.setSoundType(net.minecraft.block.SoundType.METAL);
        this.setHarvestLevel("pickaxe", 2);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileMechanistMortar();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;   // рисует TESR
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** §4.1: одна мортира на чанк — батарея превращает авто в мясорубку. */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote) {
            return;
        }
        // карта тайлов ЧАНКА, не весь мир (ревью №19a)
        for (TileEntity te : world.getChunk(pos).getTileEntityMap().values()) {
            if (te instanceof TileMechanistMortar && !te.getPos().equals(pos)) {
                world.setBlockToAir(pos);
                boolean creative = placer instanceof EntityPlayer
                        && ((EntityPlayer) placer).capabilities.isCreativeMode;
                if (!creative) {   // в креативе предмет не тратился (№19c)
                    net.minecraft.inventory.InventoryHelper.spawnItemStack(world,
                            pos.getX(), pos.getY(), pos.getZ(), new ItemStack(this));
                }
                if (placer instanceof EntityPlayer) {
                    ((EntityPlayer) placer).sendStatusMessage(
                            new TextComponentString(
                                    "§cОдна мортира на чанк — вторая не встала"),
                            true);
                }
                return;
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileMechanistMortar)) {
            return false;
        }
        TileMechanistMortar mortar = (TileMechanistMortar) te;
        ItemStack held = player.getHeldItem(hand);
        // патроны — в короб
        if (held.getItem() instanceof ItemCartridge) {
            if (!world.isRemote) {
                int taken = mortar.loadAmmo(
                        ((ItemCartridge) held.getItem()).bulletType, held.getCount());
                if (taken > 0) {
                    held.shrink(taken);
                    player.sendStatusMessage(new TextComponentString(
                            "§aВ коробе: +" + taken), true);
                } else {
                    player.sendStatusMessage(new TextComponentString(
                            "§cКороб занят другим типом или полон"), true);
                }
            }
            return true;
        }
        if (!held.isEmpty()) {
            return false;
        }
        // пустая рука: в ручном режиме без шифта — ОГОНЬ, иначе статус
        if (!world.isRemote) {
            if (mortar.isManual() && !player.isSneaking()) {
                if (!mortar.manualFire(player)) {
                    player.sendStatusMessage(new TextComponentString(
                            ((IMachineStatus) mortar).getStatusLine()), true);
                }
            } else {
                player.sendStatusMessage(new TextComponentString(
                        ((IMachineStatus) mortar).getStatusLine()), true);
            }
        }
        return true;
    }

    // ================= IWrenchable: ключ циклит режимы =================

    @Override
    public EnumFacing getFacing(World world, BlockPos pos) {
        return EnumFacing.NORTH;
    }

    @Override
    public boolean setFacing(World world, BlockPos pos, EnumFacing newDirection,
                             EntityPlayer player) {
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileMechanistMortar)) {
            return false;
        }
        int mode = ((TileMechanistMortar) te).cycleMode();
        if (player != null) {
            String name = mode == TileMechanistMortar.MODE_MANUAL
                    ? "ручной — ПКМ пустой рукой стреляет по взгляду"
                    : mode == TileMechanistMortar.MODE_AUTO
                            ? "авто — бьёт по чудовищам в 32 блоках"
                            : "авто+игроки — PvP-предохранитель снят";
            player.sendStatusMessage(new TextComponentString("§5Режим: " + name), true);
        }
        return true;
    }

    @Override
    public boolean wrenchCanRemove(World world, BlockPos pos, EntityPlayer player) {
        return false;   // орудие снимается киркой, ключ только для режимов
    }

    @Override
    public List<ItemStack> getWrenchDrops(World world, BlockPos pos, IBlockState state,
                                          TileEntity te, EntityPlayer player, int fortune) {
        return new ArrayList<>();
    }
}
