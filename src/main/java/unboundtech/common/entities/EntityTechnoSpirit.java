package unboundtech.common.entities;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import unboundtech.common.UTItems;
import unboundtech.common.UTSpiritSpawner;
import unboundtech.common.blocks.BlockMachineBase;
import unboundtech.common.items.ItemNanoThaumArmor;

/**
 * Техно-дух (`05_objects/techno_spirit.md`): живая реакция мира на то,
 * что игрок скрещивает магию с электричеством. «Сначала это просто искра
 * не в такт, потом искра, которая держится в воздухе дольше положенного,
 * а потом она уже смотрит на тебя».
 *
 *  - НЕ агрессивен (§4.1): подлетает к работающим машинам и кормится;
 *    ударили — огрызается разрядом (2 урона + поджиг 2 сек) и улетает,
 *    удар сбрасывает эскалацию всей группы ({@link UTSpiritSpawner});
 *  - исключение §4.1а: игрок в ПОЛНОМ Нано-Тауме для духа — большая
 *    ходячая машина у узла, его дух атакует;
 *  - дроп: Заряженная Искра всегда (гейт арены T5 — падает и от
 *    не-игрока, §10), редстоун 0–2 с шансом 60 %;
 *  - формы-обличья §4.2 (8 штук) — поле формы заведено, столько моделей
 *    прототип пока не рисует: одна форма «Механизм», коробка на ножках;
 *  - в мирном режиме исчезает; не персистентен.
 *
 * TODO по карточке: замедление машины −10 % за духа (§4.1) — придёт
 * вместе с общим регулятором скорости машин; полоса «сытости» под
 * гогглами (§9).
 */
public class EntityTechnoSpirit extends EntityFlying {

    private static final DataParameter<Integer> FORM = EntityDataManager
            .createKey(EntityTechnoSpirit.class, DataSerializers.VARINT);

    private BlockPos machineTarget;
    private int retarget;
    private int fleeTicks;

    public EntityTechnoSpirit(World world) {
        super(world);
        this.setSize(0.6f, 0.6f);
        this.experienceValue = 5;   // §5
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(FORM, this.rand.nextInt(8));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MAX_HEALTH)
                .setBaseValue(10.0);   // §5: 5 сердец
    }

    public int getForm() {
        return this.dataManager.get(FORM);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote
                && this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            this.setDead();   // §10: мирный режим — духов нет
            return;
        }
        if (this.world.isRemote) {
            if (this.rand.nextInt(4) == 0) {
                this.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.SPELL_MOB,
                        this.posX, this.posY + 0.3, this.posZ, 0.7, 0.5, 1.0);
            }
            return;
        }
        this.steer();
    }

    /** Полёт: к цели-машине (или к игроку в Нано-Тауме), иначе дрейф. */
    private void steer() {
        if (this.fleeTicks > 0) {
            this.fleeTicks--;
            return;   // улетает с той скоростью, что задана при ударе
        }
        if (--this.retarget <= 0) {
            this.retarget = 40;
            this.pickTarget();
        }
        double tx;
        double ty;
        double tz;
        EntityPlayer prey = this.nanoThaumPrey();
        if (prey != null) {
            tx = prey.posX;
            ty = prey.posY + 1.0;
            tz = prey.posZ;
            if (this.getDistance(prey) < 1.5 && this.ticksExisted % 20 == 0) {
                prey.attackEntityFrom(DamageSource.causeMobDamage(this), 2.0f);
                prey.setFire(2);
            }
        } else if (this.machineTarget != null) {
            tx = this.machineTarget.getX() + 0.5;
            ty = this.machineTarget.getY() + 1.6;
            tz = this.machineTarget.getZ() + 0.5;
        } else {
            if (this.rand.nextInt(60) == 0) {
                this.motionX += (this.rand.nextFloat() - 0.5) * 0.1;
                this.motionY += (this.rand.nextFloat() - 0.5) * 0.05;
                this.motionZ += (this.rand.nextFloat() - 0.5) * 0.1;
            }
            return;
        }
        double dx = tx - this.posX;
        double dy = ty - this.posY;
        double dz = tz - this.posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 1.0) {
            this.motionX += dx / dist * 0.01;
            this.motionY += dy / dist * 0.01;
            this.motionZ += dz / dist * 0.01;
        }
    }

    /** Ближайшая РАБОТАЮЩАЯ наша машина в 16 блоках (§4). */
    private void pickTarget() {
        this.machineTarget = null;
        double best = 16 * 16;
        for (net.minecraft.tileentity.TileEntity te
                : this.world.loadedTileEntityList) {
            if (!(te.getBlockType() instanceof BlockMachineBase)) {
                continue;
            }
            double d = te.getPos().distanceSq(this.posX, this.posY, this.posZ);
            if (d < best) {
                IBlockState state = this.world.getBlockState(te.getPos());
                if (state.getBlock() instanceof BlockMachineBase
                        && state.getValue(BlockMachineBase.ACTIVE)) {
                    best = d;
                    this.machineTarget = te.getPos();
                }
            }
        }
    }

    /** §4.1а: полный комплект Нано-Таума делает игрока добычей. */
    private EntityPlayer nanoThaumPrey() {
        for (EntityPlayer player : this.world.playerEntities) {
            if (player.isDead || player.capabilities.isCreativeMode
                    || this.getDistance(player) > 12) {
                continue;
            }
            boolean full = true;
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) {
                    continue;
                }
                ItemStack piece = player.getItemStackFromSlot(slot);
                if (!(piece.getItem() instanceof ItemNanoThaumArmor)) {
                    full = false;
                    break;
                }
            }
            if (full) {
                return player;
            }
        }
        return null;
    }

    /** §4.1: огрызнуться разрядом и улететь; удар сбрасывает эскалацию. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.world.isRemote || this.isDead) {
            return super.attackEntityFrom(source, amount);
        }
        Entity attacker = source.getTrueSource();
        if (attacker instanceof EntityLivingBase && this.getDistance(attacker) < 4) {
            attacker.attackEntityFrom(DamageSource.causeMobDamage(this), 2.0f);
            attacker.setFire(2);
            double dx = this.posX - attacker.posX;
            double dz = this.posZ - attacker.posZ;
            double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
            this.motionX = dx / len * 0.8;
            this.motionY = 0.4;
            this.motionZ = dz / len * 0.8;
            this.fleeTicks = 60;
        }
        UTSpiritSpawner.onSpiritHurt(this.world, this.getPosition());
        return super.attackEntityFrom(source, amount);
    }

    /** §4.3: Искра — всегда, даже от не-игрока; редстоун 0–2 при 60 %. */
    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        this.entityDropItem(new ItemStack(UTItems.chargedSpark), 0.0f);
        if (this.rand.nextInt(100) < 60) {
            int dust = this.rand.nextInt(3);
            if (dust > 0) {
                this.entityDropItem(new ItemStack(
                        net.minecraft.init.Items.REDSTONE, dust), 0.0f);
            }
        }
    }

    @Override
    protected boolean canDespawn() {
        return true;   // §10: не персистентен
    }

    @Override
    public boolean isBurning() {
        return false;   // §10: не материален, среда его не жжёт
    }

    @Override
    public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("UTForm", this.getForm());
    }

    @Override
    public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.dataManager.set(FORM, tag.getInteger("UTForm"));
    }
}
