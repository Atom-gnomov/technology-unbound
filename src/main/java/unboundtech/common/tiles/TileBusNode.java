package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import unboundtech.common.blocks.BlockEssentiaConduit;

/**
 * Шинный Узел (`05_objects/bus_node.md`): держит по глотку каждого смысла
 * и отправляет их по одной нити, не давая перепутаться, — за плату током.
 *
 * Устройство (§4):
 *  - до 5 сторон под трубы, один канал на трубу, буфер 8 единиц на канал;
 *  - направление читается по ТЯГЕ соседей, как у труб ТК (правильный
 *    прецедент §4.1): рядом источник — тянем, рядом потребитель — отдаём;
 *  - линия — кабели ({@link BlockEssentiaConduit}); пропускная способность
 *    линии = минимальный тир кабеля на пути; каналов меньше, чем труб —
 *    лишние стороны не обслуживаются (🔴 в статусе);
 *  - перекачка — 20 EU за доставленную единицу, платит отправитель;
 *  - порядок обслуживания строго алфавитный по тегу аспекта (§4.2).
 */
public class TileBusNode extends TileThaumcraft
        implements ITickable, IMachineStatus, IEssentiaTransport,
        unboundtech.common.gui.ISyncedMachine, unboundtech.common.gui.IEnergyGauge {

    /** Клиентские копии полей GUI (ХФ-7). */
    private int guiEnergy;
    private int guiState;
    private int guiPipes;
    private int guiChannels;
    private final int[] guiBuffers = new int[12];

    private int stateCode() {
        if (this.lineChannels <= 0) {
            return 1;
        }
        if (this.pipeCount > Math.min(MAX_PIPE_SIDES, this.lineChannels)) {
            return 2;
        }
        if (!this.sink.canUseEnergy(EU_PER_UNIT)) {
            return 3;
        }
        return 0;
    }

    @Override
    public int[] syncFields() {
        int[] fields = new int[16];
        fields[0] = (int) this.sink.getEnergyStored();
        fields[1] = this.stateCode();
        fields[2] = this.pipeCount;
        fields[3] = Math.min(MAX_PIPE_SIDES, this.lineChannels);
        // 6 боковых буферов: цвет аспекта (или -1) + количество
        for (int i = 0; i < 6; i++) {
            fields[4 + 2 * i] = this.bufAspect[i] == null
                    ? -1 : this.bufAspect[i].getColor();
            fields[5 + 2 * i] = this.bufAmount[i];
        }
        return fields;
    }

    @Override
    public void applySyncField(int index, int value) {
        switch (index) {
            case 0: this.guiEnergy = value; break;
            case 1: this.guiState = value; break;
            case 2: this.guiPipes = value; break;
            case 3: this.guiChannels = value; break;
            default:
                if (index >= 4 && index < 16) {
                    this.guiBuffers[index - 4] = value;
                }
                break;
        }
    }

    @Override
    public double gaugeEnergy() {
        return this.world != null && this.world.isRemote
                ? this.guiEnergy : this.sink.getEnergyStored();
    }

    @Override
    public double gaugeCapacity() {
        return CAPACITY;
    }

    public int guiBufferColor(int side) {
        return this.guiBuffers[2 * side];
    }

    public int guiBufferAmount(int side) {
        return this.guiBuffers[2 * side + 1];
    }

    /** Клиентская строка — только из синкнутых полей (ХФ-7). */
    private String clientStatusLine() {
        String tail = ". Буфер " + this.guiEnergy + " / " + (int) CAPACITY + " EU";
        switch (this.guiState) {
            case 1: return "§cУзел: нет кабеля линии" + tail;
            case 2: return "§cУзел: каналов не хватает — " + this.guiPipes
                    + " труб, " + this.guiChannels + " каналов" + tail;
            case 3: return "§eУзел: копит EU" + tail;
            default: break;
        }
        int held = 0;
        for (int i = 0; i < 6; i++) {
            held += this.guiBuffers[2 * i + 1];
        }
        return "§aУзел: " + this.guiPipes + " труб, линия на " + this.guiChannels
                + " каналов, в буферах " + held + tail;
    }

    /** §5: буфер 4 000 EU, вход LV. */
    public static final double CAPACITY = 4_000.0;
    private static final int TIER = 1;
    /** §5: 20 EU за перекачанную единицу. */
    public static final int EU_PER_UNIT = 20;
    /** §5: буфер 8 единиц на канал. */
    public static final int CHANNEL_BUFFER = 8;
    /** §4: обслуживание раз в 10 тиков. */
    public static final int SERVICE_TICKS = 10;
    /** §5: до 5 сторон под трубы. */
    public static final int MAX_PIPE_SIDES = 5;

    private static final int SUCTION = 128;
    private static final int NETWORK_REFRESH_TICKS = 100;
    private static final int NETWORK_LIMIT = 512;

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);

    /** Поканальные буферы: индекс — сторона света. */
    private final Aspect[] bufAspect = new Aspect[6];
    private final int[] bufAmount = new int[6];

    private int counter;
    private int pipeCount;
    private int lineChannels;
    private List<BlockPos> endpoints = new ArrayList<>();
    private int networkAge = NETWORK_REFRESH_TICKS;

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
        this.networkAge++;
        if (this.counter % SERVICE_TICKS != 0) {
            return;
        }
        if (this.networkAge >= NETWORK_REFRESH_TICKS) {
            this.refreshNetwork();
        }
        EnumFacing[] served = this.servedSides();
        for (EnumFacing side : served) {
            this.pullFrom(side);
        }
        for (EnumFacing side : served) {
            this.deliverFrom(side);
        }
    }

    /**
     * Стороны с трубами, по алфавиту имён сторон — детерминизм §4.2;
     * обслуживаются первые {@code lineChannels} (и не больше пяти).
     */
    private EnumFacing[] servedSides() {
        List<EnumFacing> pipes = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            if (this.isPipe(side)) {
                pipes.add(side);
            }
        }
        pipes.sort(java.util.Comparator.comparing(EnumFacing::getName));
        this.pipeCount = pipes.size();
        int cap = Math.min(MAX_PIPE_SIDES, this.lineChannels);
        if (pipes.size() > cap) {
            pipes = pipes.subList(0, Math.max(0, cap));
        }
        return pipes.toArray(new EnumFacing[0]);
    }

    /** Труба — сосед-транспорт, не принадлежащий самой шине. */
    private boolean isPipe(EnumFacing side) {
        BlockPos at = this.pos.offset(side);
        if (BlockEssentiaConduit.joins(this.world, at)) {
            return false;
        }
        TileEntity te = this.world.getTileEntity(at);
        return te instanceof IEssentiaTransport;
    }

    /** ВХОД: забираем из трубы в буфер стороны — если шине это нужно. */
    private void pullFrom(EnumFacing side) {
        if (this.bufAmount[side.getIndex()] >= CHANNEL_BUFFER) {
            return;
        }
        TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.world,
                this.pos.getX(), this.pos.getY(), this.pos.getZ(), side);
        if (!(te instanceof IEssentiaTransport)) {
            return;
        }
        IEssentiaTransport transport = (IEssentiaTransport) te;
        EnumFacing remote = side.getOpposite();
        if (!transport.canOutputTo(remote) || transport.getEssentiaAmount(remote) <= 0) {
            return;
        }
        Aspect offered = transport.getEssentiaType(remote);
        if (offered == null) {
            return;
        }
        int idx = side.getIndex();
        if (this.bufAspect[idx] != null && this.bufAspect[idx] != offered
                && this.bufAmount[idx] > 0) {
            return;   // канал занят другим аспектом
        }
        if (this.getSuctionAmount(side) <= transport.getSuctionAmount(remote)
                || this.getSuctionAmount(side) < transport.getMinimumSuction()) {
            return;
        }
        // Тянем только то, что шине есть куда девать — иначе узел
        // превращается в пылесос без выхлопа.
        if (!this.hasDemand(offered, side)) {
            return;
        }
        int taken = transport.takeEssentia(offered, 1, remote);
        if (taken > 0) {
            this.bufAspect[idx] = offered;
            this.bufAmount[idx] += taken;
            this.markDirty();
        }
    }

    /** ВЫХОД: буфер стороны → потребитель (локальный или через линию). */
    private void deliverFrom(EnumFacing side) {
        int idx = side.getIndex();
        Aspect aspect = this.bufAspect[idx];
        if (aspect == null || this.bufAmount[idx] <= 0) {
            return;
        }
        if (!this.sink.canUseEnergy(EU_PER_UNIT)) {
            return;   // §4: нет EU — перекачка встаёт, буферы сохраняются
        }
        if (this.pushLocal(aspect, side) || this.pushRemote(aspect)) {
            this.sink.useEnergy(EU_PER_UNIT);
            this.bufAmount[idx]--;
            if (this.bufAmount[idx] <= 0) {
                this.bufAspect[idx] = null;
            }
            this.markDirty();
        }
    }

    /** Потребитель на другой стороне ЭТОГО узла. */
    private boolean pushLocal(Aspect aspect, EnumFacing except) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (side == except || !this.isPipe(side)) {
                continue;
            }
            if (this.pushInto(aspect, side)) {
                return true;
            }
        }
        return false;
    }

    /** Потребитель на дальнем конце линии: чужой узел или накопитель. */
    private boolean pushRemote(Aspect aspect) {
        for (BlockPos at : this.endpoints) {
            TileEntity te = this.world.getTileEntity(at);
            if (te instanceof TileEssentiaVaultController) {
                if (((TileEssentiaVaultController) te).busInsert(aspect)) {
                    return true;
                }
            } else if (te instanceof TileBusNode && te != this) {
                if (((TileBusNode) te).acceptFromLine(aspect)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Линия просит пристроить единицу: отдаём своей трубе с тягой. */
    boolean acceptFromLine(Aspect aspect) {
        for (EnumFacing side : this.servedSides()) {
            if (this.pushInto(aspect, side)) {
                return true;
            }
        }
        return false;
    }

    private boolean pushInto(Aspect aspect, EnumFacing side) {
        TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.world,
                this.pos.getX(), this.pos.getY(), this.pos.getZ(), side);
        if (!(te instanceof IEssentiaTransport)) {
            return false;
        }
        IEssentiaTransport transport = (IEssentiaTransport) te;
        EnumFacing remote = side.getOpposite();
        if (!transport.canInputFrom(remote)
                || transport.getSuctionAmount(remote) <= 0) {
            return false;   // направление решает тяга соседа (§4.1)
        }
        Aspect wanted = transport.getSuctionType(remote);
        if (wanted != null && wanted != aspect) {
            return false;
        }
        return transport.addEssentia(aspect, 1, remote) > 0;
    }

    /** Есть ли на шине спрос на аспект (не считая стороны-источника). */
    private boolean hasDemand(Aspect aspect, EnumFacing origin) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (side != origin && this.isPipe(side)) {
                TileEntity te = this.world.getTileEntity(this.pos.offset(side));
                if (te instanceof IEssentiaTransport) {
                    IEssentiaTransport t = (IEssentiaTransport) te;
                    EnumFacing remote = side.getOpposite();
                    Aspect wanted = t.getSuctionType(remote);
                    if (t.canInputFrom(remote) && t.getSuctionAmount(remote) > 0
                            && (wanted == null || wanted == aspect)) {
                        return true;
                    }
                }
            }
        }
        for (BlockPos at : this.endpoints) {
            TileEntity te = this.world.getTileEntity(at);
            if (te instanceof TileEssentiaVaultController) {
                if (((TileEssentiaVaultController) te).busHasRoom(aspect)) {
                    return true;
                }
            } else if (te instanceof TileBusNode && te != this) {
                if (((TileBusNode) te).wantsFromLine(aspect)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Тянет ли какая-то из наших труб этот аспект. */
    boolean wantsFromLine(Aspect aspect) {
        for (EnumFacing side : this.servedSides()) {
            TileEntity te = this.world.getTileEntity(this.pos.offset(side));
            if (te instanceof IEssentiaTransport) {
                IEssentiaTransport t = (IEssentiaTransport) te;
                EnumFacing remote = side.getOpposite();
                Aspect wanted = t.getSuctionType(remote);
                if (t.canInputFrom(remote) && t.getSuctionAmount(remote) > 0
                        && (wanted == null || wanted == aspect)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Обход линии: BFS по кабелям от узла; конечные точки — чужие узлы и
     * контроллеры накопителей, пропускная способность — минимальный тир
     * кабеля на линии (§4 кабеля).
     */
    private void refreshNetwork() {
        this.networkAge = 0;
        this.endpoints = new ArrayList<>();
        this.lineChannels = 0;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        int minChannels = Integer.MAX_VALUE;
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos at = this.pos.offset(side);
            if (this.world.getBlockState(at).getBlock() instanceof BlockEssentiaConduit) {
                queue.add(at);
            }
        }
        while (!queue.isEmpty() && seen.size() < NETWORK_LIMIT) {
            BlockPos at = queue.poll();
            if (!seen.add(at)) {
                continue;
            }
            net.minecraft.block.Block block = this.world.getBlockState(at).getBlock();
            if (block instanceof BlockEssentiaConduit) {
                minChannels = Math.min(minChannels,
                        ((BlockEssentiaConduit) block).channels);
                for (EnumFacing side : EnumFacing.VALUES) {
                    BlockPos next = at.offset(side);
                    if (!seen.contains(next)
                            && BlockEssentiaConduit.joins(this.world, next)) {
                        queue.add(next);
                    }
                }
            } else if (!at.equals(this.pos)) {
                TileEntity te = this.world.getTileEntity(at);
                if (te instanceof TileBusNode
                        || te instanceof TileEssentiaVaultController) {
                    this.endpoints.add(at);
                }
            }
        }
        this.lineChannels = minChannels == Integer.MAX_VALUE ? 0 : minChannels;
    }

    /** Сосед-кабель поставлен/сломан — линию надо перечитать. */
    public void invalidateNetwork() {
        this.networkAge = NETWORK_REFRESH_TICKS;
    }

    // ================= IEssentiaTransport: сторона труб =================

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
        return face != null && this.bufAmount[face.getIndex()] < CHANNEL_BUFFER
                ? SUCTION : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (face == null) {
            return 0;
        }
        int idx = face.getIndex();
        if (this.bufAspect[idx] != aspect || this.bufAmount[idx] <= 0) {
            return 0;
        }
        int taken = Math.min(amount, this.bufAmount[idx]);
        this.bufAmount[idx] -= taken;
        if (this.bufAmount[idx] <= 0) {
            this.bufAspect[idx] = null;
        }
        this.markDirty();
        return taken;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (face == null || aspect == null || amount <= 0) {
            return 0;
        }
        int idx = face.getIndex();
        if (this.bufAspect[idx] != null && this.bufAspect[idx] != aspect
                && this.bufAmount[idx] > 0) {
            return 0;
        }
        int accepted = Math.min(amount, CHANNEL_BUFFER - this.bufAmount[idx]);
        if (accepted <= 0) {
            return 0;
        }
        this.bufAspect[idx] = aspect;
        this.bufAmount[idx] += accepted;
        this.markDirty();
        return accepted;
    }

    @Override
    public Aspect getEssentiaType(EnumFacing face) {
        return face == null ? null : this.bufAspect[face.getIndex()];
    }

    @Override
    public int getEssentiaAmount(EnumFacing face) {
        return face == null ? 0 : this.bufAmount[face.getIndex()];
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // ================= IMachineStatus =================

    @Override
    public String getStatusLine() {
        if (this.world != null && this.world.isRemote) {
            return this.clientStatusLine();
        }
        int eu = (int) this.sink.getEnergyStored();
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU";
        if (this.lineChannels <= 0) {
            return "§cУзел: нет кабеля линии" + tail;
        }
        if (this.pipeCount > Math.min(MAX_PIPE_SIDES, this.lineChannels)) {
            return "§cУзел: каналов не хватает — " + this.pipeCount + " труб, "
                    + Math.min(MAX_PIPE_SIDES, this.lineChannels) + " каналов" + tail;
        }
        if (!this.sink.canUseEnergy(EU_PER_UNIT)) {
            return "§eУзел: копит EU" + tail;
        }
        int held = 0;
        for (int amount : this.bufAmount) {
            held += amount;
        }
        return "§aУзел: " + this.pipeCount + " труб, линия на " + this.lineChannels
                + " каналов, в буферах " + held + tail;
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        this.sink.writeToNBT(tag);
        this.writeState(tag);
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.sink.readFromNBT(tag);
        this.readState(tag);
    }

    // ================= NBT =================

    private void writeState(NBTTagCompound tag) {
        for (int i = 0; i < 6; i++) {
            tag.setString("UTBufA" + i,
                    this.bufAspect[i] == null ? "" : this.bufAspect[i].getTag());
            tag.setInteger("UTBufN" + i, this.bufAmount[i]);
        }
    }

    private void readState(NBTTagCompound tag) {
        for (int i = 0; i < 6; i++) {
            String key = tag.getString("UTBufA" + i);
            this.bufAspect[i] = key.isEmpty() ? null : Aspect.getAspect(key);
            this.bufAmount[i] = this.bufAspect[i] == null ? 0
                    : tag.getInteger("UTBufN" + i);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.sink.readFromNBT(tag);
        this.readState(tag);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.sink.writeToNBT(tag);
        this.writeState(tag);
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
