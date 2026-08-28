package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.common.blocks.BlockVaultCasing;
import unboundtech.common.blocks.BlockVaultGolemPort;

/**
 * Контроллер Эссентиального Накопителя (`05_objects/essentia_vault.md`):
 * библиотека «многого понемногу» на конце шины — 27 плит, внутри тишина,
 * разгороженная на полки.
 *
 * Структура 3×3×3 (§4.1): 25 корпусов + контроллер в центре одной грани
 * (лицом наружу) + РОВНО один голем-порт на любой грани. Пределы §5:
 * 384 суммарно, но не более 96 одного аспекта. Питание — 1 EU/t за каждый
 * хранимый индекс (вид аспекта) плюс 20 EU за единицу на входе и выходе.
 * Без энергии встаёт целиком — «библиотека гаснет, стоит выдернуть
 * провод». Порядок выдачи — алфавитный (§4.3).
 */
public class TileEssentiaVaultController extends TileThaumcraft
        implements ITickable, IMachineStatus, IAspectContainer, IEssentiaTransport {

    /** §5. */
    public static final double CAPACITY = 20_000.0;
    private static final int TIER = 1;
    public static final int TOTAL_CAP = 384;
    public static final int PER_ASPECT_CAP = 96;
    public static final int EU_PER_UNIT = 20;
    private static final int STRUCTURE_CHECK_TICKS = 20;
    private static final int SUCTION = 96;

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);

    private AspectList aspects = new AspectList();
    private boolean formed;
    private boolean powered;
    private int counter;

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos,
            net.minecraft.block.state.IBlockState oldState,
            net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        this.sink.update();
        if (this.world == null || this.world.isRemote) {
            return;
        }
        this.counter++;
        if (this.counter % STRUCTURE_CHECK_TICKS == 0) {
            this.checkStructure();
        }
        // §5: 1 EU/t на каждый хранимый индекс. Не хватает — гаснет целиком.
        int upkeep = this.aspects.size();
        this.powered = upkeep == 0 || this.sink.canUseEnergy(upkeep);
        if (this.powered && upkeep > 0) {
            this.sink.useEnergy(upkeep);
        }
        boolean active = this.formed && this.powered;
        net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof BlockMachineBase
                && state.getValue(BlockMachineBase.ACTIVE) != active) {
            BlockMachineBase.setActive(this.world, this.pos, active);
        }
    }

    /** Принимает и отдаёт только собранный и запитанный накопитель. */
    public boolean operational() {
        return this.formed && this.powered;
    }

    public boolean isFormed() {
        return this.formed;
    }

    /**
     * §4.1: куб 3×3×3 за спиной контроллера — 25 корпусов + ровно один
     * голем-порт; сам контроллер — центр ближней грани.
     */
    private void checkStructure() {
        EnumFacing facing = EnumFacing.NORTH;
        net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof BlockMachineBase) {
            facing = state.getValue(BlockMachineBase.FACING);
        }
        BlockPos center = this.pos.offset(facing.getOpposite());
        int ports = 0;
        boolean ok = true;
        for (int dx = -1; dx <= 1 && ok; dx++) {
            for (int dy = -1; dy <= 1 && ok; dy++) {
                for (int dz = -1; dz <= 1 && ok; dz++) {
                    BlockPos at = center.add(dx, dy, dz);
                    if (at.equals(this.pos)) {
                        continue;
                    }
                    Block block = this.world.getBlockState(at).getBlock();
                    if (block instanceof BlockVaultGolemPort) {
                        ports++;
                    } else if (!(block instanceof BlockVaultCasing)) {
                        ok = false;
                    }
                }
            }
        }
        boolean now = ok && ports == 1;
        if (now != this.formed) {
            this.formed = now;
            this.markDirty();
        }
    }

    public int tagAmount() {
        int total = 0;
        for (Aspect tag : this.aspects.getAspects()) {
            total += this.aspects.getAmount(tag);
        }
        return total;
    }

    /** Аспекты по алфавиту тегов (§4.3). */
    public Aspect[] sortedAspects() {
        Aspect[] list = this.aspects.getAspects();
        Arrays.sort(list, Comparator.comparing(Aspect::getTag));
        return list;
    }

    public Aspect firstAspect(int atLeast) {
        for (Aspect aspect : this.sortedAspects()) {
            if (this.aspects.getAmount(aspect) >= atLeast) {
                return aspect;
            }
        }
        return null;
    }

    // ================= шина =================

    /** §10: пробка по одному аспекту не вешает остальные. */
    public boolean busHasRoom(Aspect aspect) {
        return this.operational()
                && this.tagAmount() < TOTAL_CAP
                && this.aspects.getAmount(aspect) < PER_ASPECT_CAP
                && this.sink.canUseEnergy(EU_PER_UNIT);
    }

    /** Узел кладёт единицу; накопитель платит за приём (§5). */
    public boolean busInsert(Aspect aspect) {
        if (!this.busHasRoom(aspect)) {
            return false;
        }
        this.sink.useEnergy(EU_PER_UNIT);
        this.aspects.add(aspect, 1);
        this.markDirty();
        return true;
    }

    // ================= IEssentiaTransport =================

    @Override
    public boolean isConnectable(EnumFacing face) {
        return face != null;
    }

    @Override
    public boolean canInputFrom(EnumFacing face) {
        return true;
    }

    @Override
    public boolean canOutputTo(EnumFacing face) {
        return true;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(EnumFacing face) {
        return null;
    }

    @Override
    public int getSuctionAmount(EnumFacing face) {
        return this.operational() && this.tagAmount() < TOTAL_CAP ? SUCTION : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (!this.operational() || aspect == null || amount <= 0) {
            return 0;
        }
        int taken = Math.min(amount, this.aspects.getAmount(aspect));
        while (taken > 0 && !this.sink.canUseEnergy(EU_PER_UNIT * taken)) {
            taken--;   // §5: 20 EU за единицу и на выходе
        }
        if (taken <= 0) {
            return 0;
        }
        this.sink.useEnergy(EU_PER_UNIT * taken);
        this.aspects.remove(aspect, taken);
        this.markDirty();
        return taken;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (!this.operational() || aspect == null || amount <= 0) {
            return 0;
        }
        int room = Math.min(TOTAL_CAP - this.tagAmount(),
                PER_ASPECT_CAP - this.aspects.getAmount(aspect));
        int accepted = Math.min(amount, Math.max(0, room));
        while (accepted > 0 && !this.sink.canUseEnergy(EU_PER_UNIT * accepted)) {
            accepted--;
        }
        if (accepted <= 0) {
            return 0;
        }
        this.sink.useEnergy(EU_PER_UNIT * accepted);
        this.aspects.add(aspect, accepted);
        this.markDirty();
        return accepted;
    }

    @Override
    public Aspect getEssentiaType(EnumFacing face) {
        return this.firstAspect(1);
    }

    @Override
    public int getEssentiaAmount(EnumFacing face) {
        Aspect first = this.firstAspect(1);
        return first == null ? 0 : this.aspects.getAmount(first);
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // ================= IAspectContainer (голем — §4.2) =================

    @Override
    public AspectList getAspects() {
        return this.aspects;
    }

    @Override
    public void setAspects(AspectList list) {
        this.aspects = list == null ? new AspectList() : list.copy();
        this.markDirty();
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return this.busHasRoom(aspect);
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        int accepted = this.addEssentia(aspect, amount, null);
        return amount - accepted;   // контракт: возвращает ОСТАТОК
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0
                || this.aspects.getAmount(aspect) < amount
                || !this.operational()
                || !this.sink.canUseEnergy(EU_PER_UNIT * amount)) {
            return false;
        }
        this.sink.useEnergy(EU_PER_UNIT * amount);
        this.aspects.remove(aspect, amount);
        this.markDirty();
        return true;
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        if (!this.doesContainerContain(list)) {
            return false;
        }
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null
                    && !this.takeFromContainer(aspect, list.getAmount(aspect))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return this.aspects.getAmount(aspect) >= amount;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null
                    && !this.doesContainerContainAmount(aspect, list.getAmount(aspect))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return this.aspects.getAmount(aspect);
    }

    // ================= IMachineStatus =================

    @Override
    public String getStatusLine() {
        int eu = (int) this.sink.getEnergyStored();
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU";
        if (!this.formed) {
            return "§cНакопитель: структура не собрана — 3x3x3, 25 корпусов,"
                    + " один голем-порт, контроллер в центре грани" + tail;
        }
        if (!this.powered) {
            return "§cНакопитель: нет энергии — библиотека погашена" + tail;
        }
        int total = this.tagAmount();
        StringBuilder top = new StringBuilder();
        int shown = 0;
        for (Aspect aspect : this.sortedAspects()) {
            if (shown >= 5) {
                break;
            }
            if (top.length() > 0) {
                top.append(", ");
            }
            top.append(aspect.getName()).append(" ")
                    .append(this.aspects.getAmount(aspect));
            shown++;
        }
        String state = total >= TOTAL_CAP ? "§cНакопитель: забит — "
                : "§aНакопитель: занято ";
        return state + total + " / " + TOTAL_CAP
                + (top.length() == 0 ? ", пуст" : " — " + top) + tail;
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        // §9: разборка контроллера ключом сохраняет ВСЁ содержимое.
        this.sink.writeToNBT(tag);
        this.aspects.writeToNBT(tag);
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.sink.readFromNBT(tag);
        this.aspects.readFromNBT(tag);
    }

    // ================= NBT =================

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.sink.readFromNBT(tag);
        this.aspects.readFromNBT(tag);
        this.formed = tag.getBoolean("UTFormed");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.sink.writeToNBT(tag);
        this.aspects.writeToNBT(tag);
        tag.setBoolean("UTFormed", this.formed);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.sink.onLoad();
    }

    @Override
    public void invalidate() {
        this.sink.invalidate();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.sink.onChunkUnload();
        super.onChunkUnload();
    }
}
