package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.container.InventoryFake;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.lib.TCSounds;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import unboundtech.common.blocks.BlockMachineBase;

/**
 * Индукционный Тигель (`05_objects/induction_crucible.md`): родной тигель,
 * у которого за 20 EU/t сняты четыре наказания (§4.1):
 *
 *  - греется сам — не нужен огонь/лава/нитор снизу;
 *  - хранимое НЕ распадается: ни {@code drain(2)}, ни {@code remove},
 *    ни случайного компонента — что положили, то и лежит;
 *  - переполнение — вежливый отказ (предмет отскакивает), флюкса нет;
 *  - остатки сливаются в трубы/банки/фиалы, а не только на пол.
 *
 * Рецепты — общий реестр ТК ({@code findMatchingCrucibleRecipe}): всё, что
 * варится в родном тигле, варится и здесь. Своих рецептов блок не вводит.
 * Механика обработки предмета списана с {@code TileCrucible} порта
 * (attemptSmelt), а не по памяти.
 */
public class TileInductionCrucible extends TileThaumcraft
        implements ITickable, IMachineStatus, IAspectContainer, IEssentiaTransport,
        unboundtech.common.gui.ISyncedMachine, unboundtech.common.gui.IEnergyGauge {

    /** Клиентские копии полей GUI — живут от контейнера (ХФ-7). */
    private int guiEnergy;
    private int guiHeat;
    private int guiWater;
    private int guiTags;
    private int guiState;
    private final int[] guiAspects = new int[6];

    /** Код причины простоя для клиентской строки (ХФ-7). */
    private int stateCode() {
        if (this.world.isBlockPowered(this.pos)) {
            return 1;
        }
        if (this.tank.getFluidAmount() <= 0) {
            return 2;
        }
        if (!this.sink.canUseEnergy(EU_PER_TICK)) {
            return 3;
        }
        if (!this.isHot()) {
            return 4;
        }
        if (this.tagAmount() >= MAX_TAGS) {
            return 5;
        }
        return 0;
    }

    @Override
    public int[] syncFields() {
        // аспекты едут ЦВЕТОМ: клиент восстанавливает имя поиском по
        // цвету в реестре (одинаков на обеих сторонах)
        Aspect[] sorted = this.sortedAspects();
        int[] fields = new int[]{(int) this.sink.getEnergyStored(), this.heat,
                this.tank.getFluidAmount(), this.tagAmount(), this.stateCode(),
                -1, 0, -1, 0, -1, 0};
        for (int i = 0; i < 3 && i < sorted.length; i++) {
            fields[5 + 2 * i] = sorted[i].getColor();
            fields[6 + 2 * i] = this.aspects.getAmount(sorted[i]);
        }
        return fields;
    }

    @Override
    public void applySyncField(int index, int value) {
        switch (index) {
            case 0: this.guiEnergy = value; break;
            case 1: this.guiHeat = value; break;
            case 2: this.guiWater = value; break;
            case 3: this.guiTags = value; break;
            case 4: this.guiState = value; break;
            default:
                if (index >= 5 && index < 11) {
                    this.guiAspects[index - 5] = value;
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

    public int guiHeat() {
        return this.guiHeat;
    }

    public int guiWater() {
        return this.guiWater;
    }

    public int guiTags() {
        return this.guiTags;
    }

    public int guiAspectColor(int i) {
        return this.guiAspects[2 * i];
    }

    public int guiAspectAmount(int i) {
        return this.guiAspects[2 * i + 1];
    }

    /** Клиентская строка — ТОЛЬКО из синкнутых полей (урок №2/№3). */
    private String clientStatusLine() {
        String tail = ". Буфер " + this.guiEnergy + " / " + (int) CAPACITY
                + " EU, вода " + this.guiWater + " / " + TANK_CAPACITY + " мБ";
        switch (this.guiState) {
            case 1: return "§cТигель: заглушен редстоуном" + tail;
            case 2: return "§cТигель: нет воды" + tail;
            case 3: return "§eТигель: копит EU" + tail;
            case 4: return "§eТигель: нагрев " + this.guiHeat + " / " + HEAT_MAX + tail;
            case 5: return "§cТигель: полон — " + this.guiTags + " / " + MAX_TAGS
                    + ", слейте остатки" + tail;
            default: break;
        }
        StringBuilder held = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (this.guiAspects[2 * i + 1] <= 0) {
                continue;
            }
            if (held.length() > 0) {
                held.append(", ");
            }
            String name = aspectNameByColor(this.guiAspects[2 * i]);
            held.append(name == null ? "?" : name).append(" ")
                    .append(this.guiAspects[2 * i + 1]);
        }
        return "§aТигель: кипит, " + this.guiTags + " / " + MAX_TAGS
                + (held.length() == 0 ? ", пуст" : " — " + held) + tail;
    }

    /** Аспект по цвету — реестр одинаков на клиенте и сервере. */
    static Aspect aspectByColor(int colour) {
        if (colour < 0) {
            return null;
        }
        for (Aspect aspect : Aspect.aspects.values()) {
            if (aspect.getColor() == colour) {
                return aspect;
            }
        }
        return null;
    }

    static String aspectNameByColor(int colour) {
        Aspect aspect = aspectByColor(colour);
        return aspect == null ? null : aspect.getName();
    }

    /** §5: буфер 10 000 EU, вход LV. */
    public static final double CAPACITY = 10_000.0;
    private static final int TIER = 1;
    /** §5: 20 EU/t пока горячий; 0 в простое и при редстоуне. */
    public static final int EU_PER_TICK = 20;
    /** §5: ёмкость 100 аспектов суммарно — как у родного тигля. */
    public static final int MAX_TAGS = 100;
    /** §5: бак 1 000 мБ, 50 мБ на рецепт — как у родного тигля. */
    public static final int TANK_CAPACITY = 1000;
    public static final int WATER_PER_CRAFT = 50;
    /** §5: до рабочей температуры за 100 тиков (+2/тик до 200). */
    public static final int HEAT_MAX = 200;

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);
    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack stack) {
            return stack != null && stack.getFluid() == FluidRegistry.WATER;
        }

        @Override
        protected void onContentsChanged() {
            syncTile();
        }
    };

    private AspectList aspects = new AspectList();
    private int heat;
    private boolean active;

    // ================= тик =================

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.state.IBlockState oldState,
            net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        this.sink.update();
        if (this.world == null) {
            return;
        }
        if (this.world.isRemote) {
            this.clientEffects();
            return;
        }
        boolean heated = !this.world.isBlockPowered(this.pos)
                && this.tank.getFluidAmount() > 0
                && this.sink.canUseEnergy(EU_PER_TICK);
        if (heated) {
            // Обод греет воду изнутри (§3): ток уходит каждый тик нагрева
            // и удержания; в простое и при редстоуне потребление — ноль.
            this.sink.useEnergy(EU_PER_TICK);
            if (this.heat < HEAT_MAX) {
                this.heat += 2;
            }
        } else if (this.heat > 0) {
            this.heat--;
        }
        this.setActive(this.isHot());
    }

    /** Рабочая температура: чаша принимает предметы. */
    public boolean isHot() {
        return this.heat >= HEAT_MAX;
    }

    private void setActive(boolean value) {
        if (this.active == value) {
            return;
        }
        this.active = value;
        BlockMachineBase.setActive(this.world, this.pos, value);
    }

    // ================= обработка предметов (списано с TileCrucible) =================

    public void attemptSmelt(EntityItem entity) {
        boolean bubble = false;
        boolean crafted = false;
        ItemStack item = entity.getItem();
        String username = entity.getThrower();
        if (username == null || username.isEmpty()) {
            NBTTagCompound data = entity.getEntityData();
            if (data.hasKey("thrower")) {
                username = data.getString("thrower");
            }
        }

        int stacksize = item.getCount();
        for (int a = 0; a < item.getCount(); ++a) {
            if (stacksize <= 0) {
                break;
            }
            CrucibleRecipe rc = ThaumcraftCraftingManager
                    .findMatchingCrucibleRecipe(username, this.aspects, item);
            if (rc != null && this.tank.getFluidAmount() >= WATER_PER_CRAFT) {
                ItemStack out = rc.getRecipeOutput().copy();
                EntityPlayer p = this.world.getPlayerEntityByName(username);
                if (p != null) {
                    FMLCommonHandler.instance().firePlayerCraftingEvent(
                            p, out, new InventoryFake(new ItemStack[]{item}));
                }
                this.aspects = rc.removeMatching(this.aspects);
                this.tank.drain(WATER_PER_CRAFT, true);
                this.ejectItem(out);
                crafted = true;
                --stacksize;
                continue;
            }

            AspectList ot = ThaumcraftCraftingManager.getObjectTags(item);
            ot = ThaumcraftCraftingManager.getBonusTags(item, ot);
            if (ot == null || ot.size() == 0) {
                // Предмет без аспектов: отскок, как у родного тигля.
                this.bounce(entity);
                return;
            }
            int incoming = 0;
            for (Aspect tag : ot.getAspects()) {
                incoming += ot.getAmount(tag);
            }
            if (this.tagAmount() + incoming > MAX_TAGS) {
                // §4.2: вежливый отказ вместо spill() — тем же звуком и
                // импульсом, каким тигель отвергает предметы без аспектов.
                // Ничего не теряется, флюкса нет.
                this.bounce(entity);
                return;
            }
            for (Aspect tag : ot.getAspects()) {
                this.aspects.add(tag, ot.getAmount(tag));
            }
            bubble = true;
            --stacksize;
        }

        if (bubble) {
            this.world.playSound(null, this.pos, TCSounds.BUBBLE,
                    SoundCategory.BLOCKS, 0.2f,
                    1.0f + this.world.rand.nextFloat() * 0.4f);
        }
        if (crafted) {
            this.world.playSound(null, this.pos, TCSounds.BUBBLE,
                    SoundCategory.BLOCKS, 0.25f, 1.2f);
        }
        if (bubble || crafted) {
            this.syncTile();
        }
        if (stacksize <= 0) {
            entity.setDead();
        } else {
            item.setCount(stacksize);
            entity.setItem(item);
        }
        this.markDirty();
    }

    /** Отскок предмета: звук и импульс родного тигля (без урона предмету). */
    private void bounce(EntityItem entity) {
        entity.motionY = 0.35f;
        entity.motionX = (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2f;
        entity.motionZ = (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2f;
        this.world.playSound(null, this.pos, SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.BLOCKS, 0.2f,
                (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.7f + 1.0f);
    }

    /** Выброс результата рецепта — EntitySpecialItem, как у порта. */
    private void ejectItem(ItemStack items) {
        boolean first = true;
        while (items.getCount() > 0) {
            ItemStack spitout = items.copy();
            if (spitout.getCount() > spitout.getMaxStackSize()) {
                spitout.setCount(spitout.getMaxStackSize());
            }
            items.shrink(spitout.getCount());
            EntitySpecialItem drop = new EntitySpecialItem(this.world,
                    this.pos.getX() + 0.5f, this.pos.getY() + 0.71f,
                    this.pos.getZ() + 0.5f, spitout);
            drop.motionY = 0.25f;
            if (!first) {
                drop.motionX = (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.01f;
                drop.motionZ = (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.01f;
            }
            this.world.spawnEntity(drop);
            first = false;
        }
    }

    public int tagAmount() {
        int total = 0;
        for (Aspect tag : this.aspects.getAspects()) {
            total += this.aspects.getAmount(tag);
        }
        return total;
    }

    /**
     * Аспекты по алфавиту тегов — §4.3: слив в алфавитном порядке, то же
     * правило, что у шины, чтобы поведение везде было предсказуемым.
     */
    public Aspect[] sortedAspects() {
        Aspect[] list = this.aspects.getAspects();
        Arrays.sort(list, Comparator.comparing(Aspect::getTag));
        return list;
    }

    /** Первый по алфавиту аспект с запасом (для слива и фиала). */
    public Aspect firstAspect(int atLeast) {
        for (Aspect aspect : this.sortedAspects()) {
            if (this.aspects.getAmount(aspect) >= atLeast) {
                return aspect;
            }
        }
        return null;
    }

    public boolean hasWater() {
        return this.tank.getFluidAmount() > 0;
    }

    public FluidTank getTank() {
        return this.tank;
    }

    // ================= частицы =================

    /** Штатные пузырьки и цветной пар ТК (§8) + редкие фиолетовые искры. */
    private void clientEffects() {
        if (!this.active || this.tank.getFluidAmount() <= 0) {
            return;
        }
        Thaumcraft.proxy.crucibleFroth(this.world,
                this.pos.getX() + 0.2f + this.world.rand.nextFloat() * 0.6f,
                this.pos.getY() + 0.75f,
                this.pos.getZ() + 0.2f + this.world.rand.nextFloat() * 0.6f);
        if (this.world.rand.nextInt(6) == 0 && this.aspects.size() > 0) {
            Aspect a = this.aspects.getAspects()[
                    this.world.rand.nextInt(this.aspects.size())];
            java.awt.Color c = new java.awt.Color(a.getColor() - 16777216);
            Thaumcraft.proxy.crucibleBubble(this.world,
                    this.pos.getX() + 0.2f + this.world.rand.nextFloat() * 0.6f,
                    this.pos.getY() + 0.78f,
                    this.pos.getZ() + 0.2f + this.world.rand.nextFloat() * 0.6f,
                    c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f);
        }
        if (this.world.rand.nextInt(20) == 0) {
            // искра вдоль индукционного обода
            Thaumcraft.proxy.sparkle(
                    this.pos.getX() + this.world.rand.nextFloat(),
                    this.pos.getY() + 0.8f,
                    this.pos.getZ() + this.world.rand.nextFloat(),
                    0xC9A9F5);
        }
    }

    private void syncTile() {
        this.markDirty();
        if (this.world != null && !this.world.isRemote) {
            net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    // ================= вода: capability =================

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.tank);
        }
        return super.getCapability(capability, facing);
    }

    // ================= IEssentiaTransport: только слив (§4.3) =================

    @Override
    public boolean isConnectable(EnumFacing face) {
        return face != EnumFacing.UP;   // сверху кидают предметы
    }

    @Override
    public boolean canInputFrom(EnumFacing face) {
        return false;   // эссенцию тигель производит, а не принимает
    }

    @Override
    public boolean canOutputTo(EnumFacing face) {
        return face != EnumFacing.UP;
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
        return 0;   // сам ничего не тянет — потребители тянут из него
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
        if (face == EnumFacing.UP || aspect == null || amount <= 0) {
            return 0;
        }
        int taken = Math.min(amount, this.aspects.getAmount(aspect));
        if (taken <= 0) {
            return 0;
        }
        this.aspects.remove(aspect, taken);
        this.syncTile();
        return taken;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
        return 0;
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

    // ================= IAspectContainer (голем видит контейнер, §4.3) =================

    @Override
    public AspectList getAspects() {
        return this.aspects;
    }

    @Override
    public void setAspects(AspectList list) {
        this.aspects = list == null ? new AspectList() : list.copy();
        this.syncTile();
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return false;   // заполняется только предметами сверху
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        return amount;  // контракт: возвращает ОСТАТОК — не принимаем ничего
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0
                || this.aspects.getAmount(aspect) < amount) {
            return false;
        }
        this.aspects.remove(aspect, amount);
        this.syncTile();
        return true;
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        if (!this.doesContainerContain(list)) {
            return false;
        }
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null) {
                this.takeFromContainer(aspect, list.getAmount(aspect));
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
        if (this.world != null && this.world.isRemote) {
            return this.clientStatusLine();
        }
        int eu = (int) this.sink.getEnergyStored();
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU, вода "
                + this.tank.getFluidAmount() + " / " + TANK_CAPACITY + " мБ";
        if (this.world.isBlockPowered(this.pos)) {
            return "§cТигель: заглушен редстоуном" + tail;
        }
        if (this.tank.getFluidAmount() <= 0) {
            return "§cТигель: нет воды" + tail;
        }
        if (!this.sink.canUseEnergy(EU_PER_TICK)) {
            return "§eТигель: копит EU" + tail;
        }
        if (!this.isHot()) {
            return "§eТигель: нагрев " + this.heat + " / " + HEAT_MAX + tail;
        }
        int amount = this.tagAmount();
        if (amount >= MAX_TAGS) {
            return "§cТигель: полон — " + amount + " / " + MAX_TAGS
                    + ", слейте остатки" + tail;
        }
        StringBuilder held = new StringBuilder();
        for (Aspect aspect : this.sortedAspects()) {
            if (held.length() > 0) {
                held.append(", ");
            }
            held.append(aspect.getName()).append(" ")
                    .append(this.aspects.getAmount(aspect));
        }
        return "§aТигель: кипит, " + amount + " / " + MAX_TAGS
                + (held.length() == 0 ? ", пуст" : " — " + held) + tail;
    }

    @Override
    public void writeWrenchNBT(NBTTagCompound tag) {
        // §10: разборка ключом сохраняет всё — аспекты, воду, EU, нагрев.
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
        tag.setInteger("UTHeat", this.heat);
        this.tank.writeToNBT(tag);
        this.aspects.writeToNBT(tag);
    }

    private void readState(NBTTagCompound tag) {
        this.heat = tag.getInteger("UTHeat");
        this.tank.readFromNBT(tag);
        this.aspects.readFromNBT(tag);
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        this.sink.readFromNBT(tag);
        this.readState(tag);
        this.active = tag.getBoolean("UTActive");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        this.sink.writeToNBT(tag);
        this.writeState(tag);
        tag.setBoolean("UTActive", this.active);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        this.writeCustomNBT(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        this.readCustomNBT(tag);
    }

    // ================= жизненный цикл IC2-синка =================

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
