package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.common.config.ConfigItems;
import unboundtech.common.UTItems;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.common.entities.EntityFluxBullet;

/**
 * Патронная Линия (`cartridge_line.md`): боеприпас ПАЧКАМИ — «чтобы
 * перед каждым боем не варить тонны вещей». Чисто электрическая машина
 * (решение владельца: вис снят, симметрия закона 1 живёт в материалах):
 * цикл 100 тиков, 8 патронов, 4 000 EU (равномерно 40 EU/т прогресса —
 * пропало питание, цикл замирает и ничего не теряется). MV, буфер 40k.
 *
 * Тип патрона определяет СЫРЬЁ, не настройка (§4.1): алюментум →
 * зажигательные, нитор → осветительные, фиал → вис, Флюкс-Заряд →
 * флюкс, железный самородок → обычные.
 *
 * Режим ленты (§4.1): 60 гильз + 60 сырья → 1 лента за 800 тиков
 * (8 полных циклов — хвост в 4 патрона идёт полным циклом; канон
 * писал 750, реализация честно округляет вверх — заметка в каноне);
 * входы съедаются по ходу (прервали — потраченное не возвращается,
 * §10). Ленты — корм Пулемёта (T5); искровая лента придёт с ним.
 */
public class TileCartridgeLine extends TileThaumcraft implements ITickable,
        IMachineStatus, unboundtech.common.gui.ISyncedMachine,
        unboundtech.common.gui.IEnergyGauge {

    public static final double CAPACITY = 40_000.0;
    private static final int TIER = 2;
    /** §5: 100 тиков цикл, 8 патронов, 4 000 EU → 40 EU за тик прогресса. */
    public static final int CYCLE_TICKS = 100;
    public static final int PER_CYCLE = 8;
    public static final int EU_PER_TICK = 40;
    /** §4.1: лента = 60 патронов одного типа. */
    public static final int BELT_SIZE = 60;

    public static final int SLOT_CASING = 0;
    public static final int SLOT_RAW = 1;
    public static final int SLOT_OUT = 2;

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);

    public final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_CASING) {
                return stack.getItem() == UTItems.casing;
            }
            if (slot == SLOT_RAW) {
                return rawType(stack) >= 0;
            }
            return false;   // выход — только наружу
        }

        @Override
        protected void onContentsChanged(int slot) {
            TileCartridgeLine.this.markDirty();
        }
    };

    /** false — патроны, true — лента (ключ, §9). */
    private boolean beltMode;
    private int progress;
    /** Набрано патронов в собираемую ленту (0..60). */
    private int beltFilled;
    /** Тип собираемой ленты ({@link EntityFluxBullet}), -1 — не начата. */
    private int beltType = -1;
    private boolean active;

    // клиентские копии полей GUI (ХФ-7)
    private int guiEnergy;
    private int guiState;
    private int guiProgress;
    private int guiBeltFilled;
    private int guiMode;

    /** Тип патрона по сырью (§4.1); -1 — не сырьё. */
    public static int rawType(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        Item item = stack.getItem();
        if (item == net.minecraft.init.Items.IRON_NUGGET) {
            return EntityFluxBullet.TYPE_BALL;
        }
        if (item == UTItems.fluxCharge) {
            return EntityFluxBullet.TYPE_FLUX;
        }
        if (item == ConfigItems.itemResource) {
            if (stack.getMetadata() == 0) {
                return EntityFluxBullet.TYPE_INCENDIARY;
            }
            if (stack.getMetadata() == 1) {
                return EntityFluxBullet.TYPE_ILLUMINATING;
            }
            return -1;
        }
        if (item == ConfigItems.itemEssence && stack.getMetadata() == 1) {
            return EntityFluxBullet.TYPE_VIS;
        }
        return -1;
    }

    // ================= тик =================

    @Override
    public void update() {
        this.sink.update();
        if (this.world == null || this.world.isRemote) {
            return;
        }
        // К-2 скептика: готовая лента при забитом выходе ждёт здесь —
        // иначе batchSize()=0 клинил машину навсегда
        if (this.beltMode && this.beltFilled >= BELT_SIZE && this.beltType >= 0) {
            ItemStack ready = this.inventory.getStackInSlot(SLOT_OUT);
            if (ready.isEmpty()) {
                this.inventory.setStackInSlot(SLOT_OUT,
                        unboundtech.common.items.ItemCartridgeBelt
                                .of(this.beltType));
                this.beltFilled = 0;
                this.beltType = -1;
                this.markDirty();
            } else {
                this.setActive(false);
                return;
            }
        }
        if (!this.canWork()) {
            this.setActive(false);
            return;
        }
        if (!this.sink.canUseEnergy(EU_PER_TICK)) {
            this.setActive(false);   // 🟡: цикл замирает, ничего не горит
            return;
        }
        this.sink.useEnergy(EU_PER_TICK);
        this.setActive(true);
        if (++this.progress < CYCLE_TICKS) {
            return;
        }
        this.progress = 0;
        this.finishCycle();
    }

    /** Гильзы + сырьё на месте, выход не забит, лента не готова. */
    private boolean canWork() {
        int batch = this.batchSize();
        if (batch <= 0) {
            return false;
        }
        ItemStack casings = this.inventory.getStackInSlot(SLOT_CASING);
        ItemStack raw = this.inventory.getStackInSlot(SLOT_RAW);
        int type = rawType(raw);
        if (casings.getCount() < batch || raw.getCount() < batch || type < 0) {
            return false;
        }
        if (this.beltMode) {
            // тип ленты фиксируется первым циклом
            return this.beltType < 0 || this.beltType == type;
        }
        ItemStack out = this.inventory.getStackInSlot(SLOT_OUT);
        if (out.isEmpty()) {
            return true;
        }
        return out.getItem() == UTItems.cartridgeFor(type)
                && out.getCount() + batch <= out.getMaxStackSize();
    }

    /** Сколько патронов делает текущий цикл (хвост ленты короче). */
    private int batchSize() {
        if (!this.beltMode) {
            return PER_CYCLE;
        }
        return Math.min(PER_CYCLE, BELT_SIZE - this.beltFilled);
    }

    private void finishCycle() {
        int batch = this.batchSize();
        ItemStack raw = this.inventory.getStackInSlot(SLOT_RAW);
        int type = rawType(raw);
        if (type < 0 || batch <= 0) {
            return;
        }
        this.inventory.getStackInSlot(SLOT_CASING).shrink(batch);
        raw.shrink(batch);
        if (this.beltMode) {
            this.beltType = type;
            this.beltFilled += batch;
            if (this.beltFilled >= BELT_SIZE) {
                ItemStack belt = unboundtech.common.items.ItemCartridgeBelt
                        .of(this.beltType);
                ItemStack out = this.inventory.getStackInSlot(SLOT_OUT);
                if (out.isEmpty()) {
                    this.inventory.setStackInSlot(SLOT_OUT, belt);
                } else {
                    // выход забит — лента ждёт в станке (не теряется)
                    this.beltFilled = BELT_SIZE;
                    return;
                }
                this.beltFilled = 0;
                this.beltType = -1;
            }
        } else {
            ItemStack out = this.inventory.getStackInSlot(SLOT_OUT);
            if (out.isEmpty()) {
                this.inventory.setStackInSlot(SLOT_OUT,
                        new ItemStack(UTItems.cartridgeFor(type), batch));
            } else {
                out.grow(batch);
            }
        }
        this.markDirty();
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    /** Ключ (§9): смена режима; прерывание ленты сбрасывает прогресс. */
    public void toggleMode() {
        // остаток ревью #24: ГОТОВУЮ ленту ключ не уничтожает — если
        // выход занят, роняем её в мир
        if (this.beltFilled >= BELT_SIZE && this.beltType >= 0) {
            ItemStack belt = unboundtech.common.items.ItemCartridgeBelt
                    .of(this.beltType);
            ItemStack out = this.inventory.getStackInSlot(SLOT_OUT);
            if (out.isEmpty()) {
                this.inventory.setStackInSlot(SLOT_OUT, belt);
            } else if (this.world != null && !this.world.isRemote) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(
                        this.world, this.pos.getX(), this.pos.getY() + 1,
                        this.pos.getZ(), belt);
            }
        }
        this.beltMode = !this.beltMode;
        this.progress = 0;
        // §10: НЕЗАВЕРШЁННЫЙ прогресс ленты сбрасывается, потраченное
        // не возвращается
        this.beltFilled = 0;
        this.beltType = -1;
        this.markDirty();
    }

    public boolean isBeltMode() {
        return this.beltMode;
    }

    // ================= капабилити предмето-труб =================

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                    .cast(this.inventory);
        }
        return super.getCapability(capability, facing);
    }

    // ================= синк GUI (ХФ-7) =================

    private int stateCode() {
        ItemStack raw = this.inventory.getStackInSlot(SLOT_RAW);
        int batch = this.batchSize();
        if (rawType(raw) < 0
                || this.inventory.getStackInSlot(SLOT_CASING).getCount() < batch
                || raw.getCount() < batch) {
            return 1;   // 🔵 нет сырья
        }
        if (!this.canWork()) {
            return 2;   // 🔵 выход забит / тип ленты не тот
        }
        if (!this.sink.canUseEnergy(EU_PER_TICK)) {
            return 3;   // 🟡 нет EU
        }
        return 0;
    }

    @Override
    public int[] syncFields() {
        return new int[]{(int) this.sink.getEnergyStored(), this.stateCode(),
                this.progress, this.beltFilled, this.beltMode ? 1 : 0};
    }

    @Override
    public void applySyncField(int index, int value) {
        switch (index) {
            case 0: this.guiEnergy = value; break;
            case 1: this.guiState = value; break;
            case 2: this.guiProgress = value; break;
            case 3: this.guiBeltFilled = value; break;
            case 4: this.guiMode = value; break;
            default: break;
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

    public int guiProgress() {
        return this.guiProgress;
    }

    public int guiBeltFilled() {
        return this.guiBeltFilled;
    }

    public boolean guiBeltMode() {
        return this.guiMode != 0;
    }

    // ================= статус =================

    @Override
    public String getStatusLine() {
        if (this.world != null && this.world.isRemote) {
            return this.clientStatusLine();
        }
        int eu = (int) this.sink.getEnergyStored();
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU";
        String head = this.beltMode
                ? "Линия [лента " + this.beltFilled + " / " + BELT_SIZE + "]"
                : "Линия [патроны]";
        switch (this.stateCode()) {
            case 1: return "§b" + head + ": ждёт гильзы и сырьё" + tail;
            case 2: return "§b" + head + ": выход забит" + tail;
            case 3: return "§e" + head + ": нет энергии — цикл замер" + tail;
            default: return "§a" + head + ": работает, цикл "
                    + this.progress + " / " + CYCLE_TICKS + tail;
        }
    }

    private String clientStatusLine() {
        String tail = ". Буфер " + this.guiEnergy + " / " + (int) CAPACITY + " EU";
        String head = this.guiBeltMode()
                ? "Линия [лента " + this.guiBeltFilled + " / " + BELT_SIZE + "]"
                : "Линия [патроны]";
        switch (this.guiState) {
            case 1: return "§b" + head + ": ждёт гильзы и сырьё" + tail;
            case 2: return "§b" + head + ": выход забит" + tail;
            case 3: return "§e" + head + ": нет энергии — цикл замер" + tail;
            default: return "§a" + head + ": работает, цикл "
                    + this.guiProgress + " / " + CYCLE_TICKS + tail;
        }
    }

    // ================= NBT =================

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.sink.readFromNBT(tag);
        this.inventory.deserializeNBT(tag.getCompoundTag("UTInv"));
        this.beltMode = tag.getBoolean("UTBeltMode");
        this.progress = tag.getInteger("UTProgress");
        this.beltFilled = tag.getInteger("UTBeltFilled");
        this.beltType = tag.hasKey("UTBeltType")
                ? tag.getInteger("UTBeltType") : -1;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.sink.writeToNBT(tag);
        tag.setTag("UTInv", this.inventory.serializeNBT());
        tag.setBoolean("UTBeltMode", this.beltMode);
        tag.setInteger("UTProgress", this.progress);
        tag.setInteger("UTBeltFilled", this.beltFilled);
        tag.setInteger("UTBeltType", this.beltType);
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        // Shift-ПКМ ключом: разборка с СОХРАНЕНИЕМ буферов (§9)
        this.writeCustomNBT(tag);
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.readCustomNBT(tag);
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
