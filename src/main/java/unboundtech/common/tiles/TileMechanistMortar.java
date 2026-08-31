package unboundtech.common.tiles;

import ic2.api.energy.prefab.BasicSink;
import java.util.List;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import thaumcraft.api.TileThaumcraft;
import unboundtech.common.UTSpawnerTagger;
import unboundtech.common.entities.EntityMortarShell;

/**
 * Мортира Механистов (`05_objects/mechanist_mortar.md`): чугунная тумба
 * на трёх ногах, единственный блок мода с подвижным стволом — «оборона
 * базы», видная издалека.
 *
 * Режимы (цикл ключом, §4.1): РУЧНОЙ (ПКМ пустой рукой — выстрел по
 * взгляду игрока, 60 тиков, 1 патрон) → АВТО (сама бьёт по целям в 32,
 * 120 тиков, 2 патрона) → АВТО+ИГРОКИ (targetPlayers — для PvP).
 * Каждый выстрел — 500 EU (наведение и досылатель), вход MV.
 *
 * Критерий цели — флаги OpenModularTurrets, единственное, что мы у него
 * взяли (§4.1): targetMobs всегда, targetPassive НИКОГДА (иначе авто
 * выкашивает скот), targetPlayers ключом; «враждебность» = IMob ИЛИ
 * EntityLiving с активной целью-игроком (агрессивные нейтралы). Мобов
 * от спаунера пропускает ({@link UTSpawnerTagger}). Одна мортира на
 * чанк — ставит блок. Потолок ниже 3 блоков — «нет угла возвышения».
 *
 * Прототип-упрощение (честно): «сесть в мортиру как в вагонетку» из
 * карточки отложено — требует своего сетевого канала; ручной огонь —
 * ПКМ по блоку, угол считается от взгляда стреляющего.
 */
public class TileMechanistMortar extends TileThaumcraft
        implements ITickable, IMachineStatus {

    public static final int MODE_MANUAL = 0;
    public static final int MODE_AUTO = 1;
    public static final int MODE_AUTO_PVP = 2;

    /** §5. */
    public static final double CAPACITY = 20_000.0;
    private static final int TIER = 2;   // MV
    public static final int EU_PER_SHOT = 500;
    public static final int MANUAL_COOLDOWN = 60;
    public static final int AUTO_COOLDOWN = 120;
    public static final int AUTO_RANGE = 32;
    public static final int AMMO_CAP = 64;
    private static final int CLEARANCE = 3;
    /** Скорость снаряда: ручной — дальняя дуга (~128), авто — до 32. */
    private static final float MANUAL_VELOCITY = 3.4F;

    private final BasicSink sink = new BasicSink(this, CAPACITY, TIER);

    private int mode = MODE_MANUAL;
    private int ammoType;
    private int ammoCount;
    private int cooldown;
    private int counter;

    /** Углы ствола для TESR (градусы); синкается при смене цели. */
    private float aimYaw;
    private float aimPitch = 80.0F;
    /** Клиентская интерполяция TESR. */
    public float clientYaw;
    public float clientPitch = 80.0F;
    public long lastShotTime = -100;

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
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        this.counter++;
        if (this.mode != MODE_MANUAL && this.counter % 20 == 0
                && this.cooldown <= 0 && this.canShoot()) {
            EntityLivingBase target = this.pickTarget();
            if (target != null && this.ammoCount >= 2) {
                this.aimAt(target);
                this.launch(target, null);
                this.ammoCount -= 2;   // §4.1: авто тратит 2 патрона
                this.cooldown = AUTO_COOLDOWN;
                this.afterShot();
            }
        }
        if (this.mode != MODE_MANUAL && this.counter % 100 == 0) {
            // простой: ствол возвращается к небу (§8: покой — ствол вверх)
            if (this.cooldown <= 0 && this.pickTarget() == null
                    && (this.aimPitch != 80.0F)) {
                this.aimPitch = 80.0F;
                this.syncAim();
            }
        }
    }

    /** Ручной выстрел (§4.1): по взгляду игрока, 1 патрон, 60 тиков. */
    public boolean manualFire(EntityPlayer player) {
        if (this.mode != MODE_MANUAL || this.cooldown > 0
                || !this.canShoot() || this.ammoCount < 1) {
            return false;
        }
        this.aimYaw = player.rotationYaw;
        // навесная: взгляд вниз — ближе (круче дуга), вдаль — дальше;
        // rotationPitch отрицателен ВВЕРХ (вердикт-ревью №4)
        this.aimPitch = MathHelper.clamp(45.0F + player.rotationPitch, 45.0F, 85.0F);
        this.syncAim();
        this.launch(null, player);
        this.ammoCount--;
        this.cooldown = MANUAL_COOLDOWN;
        this.afterShot();
        return true;
    }

    private void afterShot() {
        this.sink.useEnergy(EU_PER_SHOT);
        this.markDirty();
        this.syncAim();
        this.world.playSound(null, this.pos.getX() + 0.5, this.pos.getY() + 1,
                this.pos.getZ() + 0.5,
                net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
                net.minecraft.util.SoundCategory.BLOCKS, 1.5f, 0.5f);
    }

    /** Пуск: либо баллистика на цель (авто), либо по углам (ручной). */
    private void launch(EntityLivingBase target, EntityPlayer gunner) {
        EntityMortarShell shell = gunner != null
                ? new EntityMortarShell(this.world, gunner, this.ammoType)
                : new EntityMortarShell(this.world, this.ammoType);
        double muzzleY = this.pos.getY() + 1.6;
        shell.setPosition(this.pos.getX() + 0.5, muzzleY, this.pos.getZ() + 0.5);
        if (target != null) {
            // авто: дуга на цель — горизонтальная скорость из дистанции,
            // вертикальная фиксированно высокая (падение сверху, §4.2)
            double dx = target.posX - shell.posX;
            double dz = target.posZ - shell.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double vy = 1.6;
            // время полёта из vy и силы тяжести снаряда (0.05/тик)
            double flight = 2.0 * vy / 0.05;
            double vh = dist / Math.max(1.0, flight);
            // ×1.40 компенсирует сопротивление воздуха 0.99/тик
            // (вердикт-ревью №2: 1.9 давало перелёт +36% на всех дистанциях)
            shell.motionX = dx / Math.max(0.01, dist) * vh * 1.40;
            shell.motionZ = dz / Math.max(0.01, dist) * vh * 1.40;
            shell.motionY = vy;
        } else {
            float yawR = this.aimYaw * 0.017453292F;
            float pitchR = this.aimPitch * 0.017453292F;
            double vh = MANUAL_VELOCITY * MathHelper.cos(pitchR);
            shell.motionX = -MathHelper.sin(yawR) * vh;
            shell.motionZ = MathHelper.cos(yawR) * vh;
            shell.motionY = MANUAL_VELOCITY * MathHelper.sin(pitchR);
        }
        this.world.spawnEntity(shell);
        this.lastShotTime = this.world.getTotalWorldTime();
    }

    /** §4: патроны + EU + три блока неба над стволом. */
    public boolean canShoot() {
        return this.sink.canUseEnergy(EU_PER_SHOT) && this.hasClearance();
    }

    public boolean hasClearance() {
        for (int i = 1; i <= CLEARANCE; i++) {
            if (!this.world.isAirBlock(this.pos.up(i))) {
                return false;
            }
        }
        return true;
    }

    /** Критерий цели §4.1: флаги + радиус + предохранители. */
    private EntityLivingBase pickTarget() {
        AxisAlignedBB zone = new AxisAlignedBB(this.pos).grow(AUTO_RANGE);
        List<EntityLivingBase> candidates = this.world
                .getEntitiesWithinAABB(EntityLivingBase.class, zone);
        EntityLivingBase best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityLivingBase e : candidates) {
            if (e.isDead || !this.isValidTarget(e)) {
                continue;
            }
            double d = e.getDistanceSq(this.pos);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private boolean isValidTarget(EntityLivingBase e) {
        if (e.getEntityData().getBoolean(UTSpawnerTagger.TAG)) {
            return false;   // §4.1: мобы из спаунера — не цель
        }
        if (e instanceof EntityPlayer) {
            return this.mode == MODE_AUTO_PVP
                    && !((EntityPlayer) e).capabilities.isCreativeMode;
        }
        if (e instanceof IMob) {
            return true;    // targetMobs всегда true
        }
        // агрессивный нейтрал: EntityLiving с активной целью-игроком
        return e instanceof EntityLiving
                && ((EntityLiving) e).getAttackTarget() instanceof EntityPlayer;
        // targetPassive всегда false — скот не трогаем (переключателя нет)
    }

    private void aimAt(EntityLivingBase target) {
        double dx = target.posX - (this.pos.getX() + 0.5);
        double dz = target.posZ - (this.pos.getZ() + 0.5);
        this.aimYaw = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        double dist = Math.sqrt(dx * dx + dz * dz);
        this.aimPitch = (float) MathHelper.clamp(85.0 - dist, 45.0, 85.0);
        this.syncAim();
    }

    private void syncAim() {
        net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
        this.world.notifyBlockUpdate(this.pos, state, state, 3);
    }

    // ================= боезапас и режимы =================

    /** ПКМ патронами: докладываем в короб (один тип за раз). */
    public int loadAmmo(int type, int offered) {
        if (this.ammoCount > 0 && this.ammoType != type) {
            return 0;
        }
        int taken = Math.min(offered, AMMO_CAP - this.ammoCount);
        if (taken > 0) {
            this.ammoType = type;
            this.ammoCount += taken;
            this.markDirty();
            this.syncAim();   // вентщели загораются без перезахода
        }
        return taken;
    }

    /** ТЗ §Состояния: щели светятся только когда есть чем стрелять. */
    public boolean ventsLit() {
        return this.ammoCount > 0 && this.sink.canUseEnergy(EU_PER_SHOT);
    }

    public int cycleMode() {
        this.mode = (this.mode + 1) % 3;
        this.markDirty();
        this.syncAim();   // лампа и клиентская ПКМ-логика (ревью №9)
        return this.mode;
    }

    public int getMode() {
        return this.mode;
    }

    public boolean isManual() {
        return this.mode == MODE_MANUAL;
    }

    public float getAimYaw() {
        return this.aimYaw;
    }

    public float getAimPitch() {
        return this.aimPitch;
    }

    // ================= IMachineStatus =================

    @Override
    public String getStatusLine() {
        int eu = (int) this.sink.getEnergyStored();
        String modeName = this.mode == MODE_MANUAL ? "ручной"
                : this.mode == MODE_AUTO ? "авто" : "авто+игроки";
        String tail = ". Буфер " + eu + " / " + (int) CAPACITY + " EU";
        if (!this.hasClearance()) {
            return "§cМортира: нет угла возвышения — надо 3 блока неба" + tail;
        }
        if (this.ammoCount <= 0) {
            return "§bМортира (" + modeName + "): короб пуст — ПКМ патронами" + tail;
        }
        if (!this.sink.canUseEnergy(EU_PER_SHOT)) {
            return "§eМортира (" + modeName + "): копит " + EU_PER_SHOT
                    + " EU на выстрел" + tail;
        }
        return "§aМортира (" + modeName + "): "
                + this.ammoCount + " патр., готова" + tail;
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
        tag.setInteger("UTMode", this.mode);
        tag.setInteger("UTAmmoType", this.ammoType);
        tag.setInteger("UTAmmoCount", this.ammoCount);
        tag.setFloat("UTYaw", this.aimYaw);
        tag.setFloat("UTPitch", this.aimPitch);
        tag.setLong("UTShot", this.lastShotTime);
    }

    private void readState(NBTTagCompound tag) {
        this.mode = tag.getInteger("UTMode");
        this.ammoType = tag.getInteger("UTAmmoType");
        this.ammoCount = tag.getInteger("UTAmmoCount");
        this.aimYaw = tag.getFloat("UTYaw");
        this.aimPitch = tag.hasKey("UTPitch") ? tag.getFloat("UTPitch") : 80.0F;
        this.lastShotTime = tag.getLong("UTShot");
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

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(
            net.minecraftforge.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        // тренога и задранный ствол выходят за 1x1x1 (ревью №10)
        return new net.minecraft.util.math.AxisAlignedBB(
                this.pos.add(-1, 0, -1), this.pos.add(2, 2, 2));
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
