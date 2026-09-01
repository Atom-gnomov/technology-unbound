package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.api.TileThaumcraft;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.energy.EnergyCanon;

/**
 * Сингулятор (`singulator.md`): жезл заряжается от розетки — воткнул,
 * заплатил электричеством, вышел с полным. Курс {@link EnergyCanon#EU_PER_VIS}
 * (10 000 EU за 1 вис — ОДНА строка канона §3.1 на Сингулятор и иридиевый
 * стержень), скорость 1 вис / 5 тиков, приём EV. Намеренно НЕ дешевле
 * маршрута «Эфирный Двигатель → узел» — Сингулятор покупает время.
 *
 * §4.2: внутри ТК вис хранится в САНТИВИС (getMaxVis = ёмкость × 100);
 * {@code addVis} принимает целые вис и сам умножает на 100 — курс канона
 * задан за 1 вис, дробных остатков не бывает (§10).
 *
 * Режимы (§4.1): авто — доливает самый пустой примал (при равенстве —
 * канонический порядок ТК Aer→Terra→Ignis→Aqua→Ordo→Perditio);
 * выбранный — льёт один примал. Кнопки GUI едут ванильным каналом
 * enchantItem ({@link unboundtech.common.gui.IMachineButtons}).
 */
public class TileSingulator extends TileThaumcraft implements ITickable,
        IMachineStatus, unboundtech.common.gui.ISyncedMachine,
        unboundtech.common.gui.IEnergyGauge,
        unboundtech.common.gui.IMachineButtons {

    /** §5: буфер 1 000 000 EU, приём EV (2 048 EU/t). */
    public static final double CAPACITY = 1_000_000.0;
    private static final int TIER = 4;
    /** §5: 1 вис за 5 тиков при полном питании. */
    public static final int TICKS_PER_VIS = 5;

    /** Канонический порядок прималов ТК (§4.1). */
    public static final Aspect[] PRIMALS = {
            Aspect.AIR, Aspect.EARTH, Aspect.FIRE,
            Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY,
    };

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);

    private ItemStack wand = ItemStack.EMPTY;
    /** 0 — авто; 1..6 — примал из {@link #PRIMALS} (индекс−1). */
    private int mode;
    private int counter;
    private boolean active;
    /** Сколько вис долито за «сессию» (с последней вставки жезла, §9). */
    private int sessionVis;

    // клиентские копии полей GUI (ХФ-7)
    private int guiEnergy;
    private int guiMode;
    private int guiState;
    private int guiMaxVis;
    private int guiSession;
    private final int[] guiVis = new int[6];

    // ================= тик =================

    @Override
    public void update() {
        this.sink.update();
        if (this.world == null || this.world.isRemote) {
            return;
        }
        if (!this.hasWand()) {
            this.setActive(false);
            return;
        }
        if (++this.counter < TICKS_PER_VIS) {
            return;
        }
        this.counter = 0;
        Aspect target = this.chooseAspect();
        if (target == null) {
            this.setActive(false);
            return;
        }
        ItemWandCasting item = (ItemWandCasting) this.wand.getItem();
        // честный долив (вердикт скептика): остаток места может быть
        // меньше 1 вис — льём сантивисами и платим ровно за влитое,
        // EU_PER_VIS/100 за сантивис (10 000/100 = 100 EU, без дробей)
        int room = item.getMaxVis(this.wand) - item.getVis(this.wand, target);
        int pour = Math.min(100, room);
        double cost = EnergyCanon.EU_PER_VIS * pour / 100.0;
        if (pour <= 0 || !this.sink.canUseEnergy(cost)) {
            this.setActive(false);
            return;
        }
        item.addRealVis(this.wand, target, pour, true);
        this.sink.useEnergy(cost);
        this.sessionVis += pour;
        this.setActive(true);
        this.markDirty();
    }

    /** Авто: самый пустой незаполненный примал; выбранный: только он. */
    private Aspect chooseAspect() {
        ItemWandCasting item = (ItemWandCasting) this.wand.getItem();
        int max = item.getMaxVis(this.wand);
        if (this.mode >= 1 && this.mode <= 6) {
            Aspect chosen = PRIMALS[this.mode - 1];
            return item.getVis(this.wand, chosen) < max ? chosen : null;
        }
        Aspect best = null;
        int bestVis = Integer.MAX_VALUE;
        for (Aspect primal : PRIMALS) {
            int vis = item.getVis(this.wand, primal);
            if (vis < max && vis < bestVis) {
                best = primal;
                bestVis = vis;
            }
        }
        return best;
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    // ================= жезл =================

    public boolean hasWand() {
        return !this.wand.isEmpty()
                && this.wand.getItem() instanceof ItemWandCasting;
    }

    public ItemStack getWand() {
        return this.wand;
    }

    /** ПКМ жезлом (§9): вставить; занятая вилка не принимает. */
    public boolean insertWand(EntityPlayer player, ItemStack held) {
        if (this.hasWand() || held.isEmpty()
                || !(held.getItem() instanceof ItemWandCasting)) {
            return false;
        }
        this.wand = held.splitStack(1);
        this.sessionVis = 0;
        this.counter = 0;
        this.markDirty();
        this.syncTile();
        return true;
    }

    /** Shift-ПКМ пустой рукой (§9): забрать; жезл — личная вещь (§10). */
    public boolean extractWand(EntityPlayer player) {
        if (!this.hasWand() && this.wand.isEmpty()) {
            return false;
        }
        if (!player.inventory.addItemStackToInventory(this.wand)) {
            player.dropItem(this.wand, false);
        }
        this.wand = ItemStack.EMPTY;
        this.setActive(false);
        this.markDirty();
        this.syncTile();
        return true;
    }

    /** Пуш NBT тайла клиентам — жезл в вилке видит TESR. */
    private void syncTile() {
        this.markDirty();
        if (this.world != null && !this.world.isRemote) {
            net.minecraft.block.state.IBlockState state =
                    this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    /** Слом блока: жезл НИКОГДА не удаляется (§10). */
    public ItemStack takeWandForDrop() {
        ItemStack out = this.wand;
        this.wand = ItemStack.EMPTY;
        return out;
    }

    // ================= кнопки GUI (канал enchantItem) =================

    @Override
    public boolean onButton(EntityPlayer player, int id) {
        if (id < 0 || id > 6) {
            return false;
        }
        this.mode = id;
        this.markDirty();
        return true;
    }

    // ================= синк GUI (ХФ-7) =================

    private int stateCode() {
        if (!this.hasWand()) {
            return 1;
        }
        if (this.chooseAspect() == null) {
            return 2;
        }
        if (!this.sink.canUseEnergy(EnergyCanon.EU_PER_VIS / 100.0)) {
            return 3;
        }
        return 0;
    }

    @Override
    public int[] syncFields() {
        int[] fields = new int[11];
        fields[0] = (int) this.sink.getEnergyStored();
        fields[1] = this.mode;
        fields[2] = this.stateCode();
        fields[3] = this.sessionVis;
        boolean wandIn = this.hasWand();
        ItemWandCasting item = wandIn
                ? (ItemWandCasting) this.wand.getItem() : null;
        fields[4] = wandIn ? item.getMaxVis(this.wand) : 0;
        for (int i = 0; i < 6; i++) {
            fields[5 + i] = wandIn ? item.getVis(this.wand, PRIMALS[i]) : 0;
        }
        return fields;
    }

    @Override
    public void applySyncField(int index, int value) {
        switch (index) {
            case 0: this.guiEnergy = value; break;
            case 1: this.guiMode = value; break;
            case 2: this.guiState = value; break;
            case 3: this.guiSession = value; break;
            case 4: this.guiMaxVis = value; break;
            default:
                if (index >= 5 && index < 11) {
                    this.guiVis[index - 5] = value;
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

    public int guiMode() {
        return this.guiMode;
    }

    /** Сантивис примала i (клиент). */
    public int guiVis(int i) {
        return this.guiVis[i];
    }

    /** Потолок стержня в сантивис (клиент); 0 — жезла нет. */
    public int guiMaxVis() {
        return this.guiMaxVis;
    }

    public boolean guiHasWand() {
        return this.guiMaxVis > 0;
    }

    // ================= статус =================

    @Override
    public String getStatusLine() {
        if (this.world != null && this.world.isRemote) {
            return this.clientStatusLine();
        }
        int eu = (int) this.sink.getEnergyStored();
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU";
        if (!this.hasWand()) {
            return "§bСингулятор: вилка пуста — ткните жезлом" + tail;
        }
        if (this.chooseAspect() == null) {
            return "§bСингулятор: жезл заполнен (" + this.modeName()
                    + "), долито " + this.sessionVis / 100 + " вис" + tail;
        }
        if (!this.sink.canUseEnergy(EnergyCanon.EU_PER_VIS / 100.0)) {
            return "§eСингулятор: копит " + EnergyCanon.EU_PER_VIS
                    + " EU на вис (" + this.modeName() + ")" + tail;
        }
        return "§aСингулятор: заряжает (" + this.modeName() + "), долито "
                + this.sessionVis / 100 + " вис" + tail;
    }

    private String clientStatusLine() {
        String tail = ". Буфер " + this.guiEnergy + " / " + (int) CAPACITY + " EU";
        String mode = modeName(this.guiMode);
        switch (this.guiState) {
            case 1: return "§bСингулятор: вилка пуста — ткните жезлом" + tail;
            case 2: return "§bСингулятор: жезл заполнен (" + mode
                    + "), долито " + this.guiSession / 100 + " вис" + tail;
            case 3: return "§eСингулятор: копит " + EnergyCanon.EU_PER_VIS
                    + " EU на вис (" + mode + ")" + tail;
            default: return "§aСингулятор: заряжает (" + mode + "), долито "
                    + this.guiSession / 100 + " вис" + tail;
        }
    }

    private String modeName() {
        return modeName(this.mode);
    }

    /** Имя режима: «авто» или имя примала. */
    public static String modeName(int mode) {
        return mode >= 1 && mode <= 6 ? PRIMALS[mode - 1].getName() : "авто";
    }

    // ================= ключ (IMachineStatus) =================

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        this.sink.writeToNBT(tag);
        tag.setInteger("UTMode", this.mode);
        // жезл в ключ НЕ прячется (§9): при разборке выпадает отдельно
    }

    @Override
    public void readWrenchNBT(NBTTagCompound tag) {
        this.sink.readFromNBT(tag);
        this.mode = tag.getInteger("UTMode");
    }

    // ================= NBT =================
    // База порта зовёт custom-методы из ОБОИХ путей — диска и
    // update-пакета (вердикт скептика: getUpdatePacket возит только
    // writeCustomNBT; свои read/writeToNBT ломали доставку жезла TESR).

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.sink.readFromNBT(tag);
        this.wand = tag.hasKey("UTWand")
                ? new ItemStack(tag.getCompoundTag("UTWand")) : ItemStack.EMPTY;
        this.mode = tag.getInteger("UTMode");
        this.sessionVis = tag.getInteger("UTSession");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.sink.writeToNBT(tag);
        if (!this.wand.isEmpty()) {
            tag.setTag("UTWand", this.wand.writeToNBT(new NBTTagCompound()));
        }
        tag.setInteger("UTMode", this.mode);
        tag.setInteger("UTSession", this.sessionVis);
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
