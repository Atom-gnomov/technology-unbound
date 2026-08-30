package unboundtech.common.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import thaumcraft.common.config.ConfigBlocks;
import unboundtech.common.UTBlocks;

/**
 * Снаряд наших стволов (`flux_revolver.md` §4.1, `flux_arquebus.md` §4.1):
 * СУЩНОСТЬ, а не рейтрейс — он летит, его видно, его можно опередить.
 * Время жизни ограничено (§12.3), урон задаёт ствол, патрон умножает и
 * добавляет эффект (`cartridges.md` §4.2). Аркебуза даёт пробитие: до
 * двух целей насквозь, вторая получает половину урона.
 */
public class EntityFluxBullet extends EntityThrowable {

    /** Типы патронов (`cartridges.md` §4.2). */
    public static final int TYPE_INCENDIARY = 0;
    public static final int TYPE_ILLUMINATING = 1;
    public static final int TYPE_VIS = 2;
    public static final int TYPE_FLUX = 3;

    /** §5 револьвера: базовый урон задаёт ствол, патрон умножает. */
    public static final float BASE_DAMAGE = 6.0F;
    /** §4.2 карточки патронов: свет на 60 секунд. */
    public static final int LIGHT_TICKS = 1200;

    private int type = TYPE_INCENDIARY;
    private float damage = BASE_DAMAGE;
    private int pierceLeft;
    private int lifeTicks = 40;

    public EntityFluxBullet(World world) {
        super(world);
    }

    public EntityFluxBullet(World world, EntityLivingBase thrower, int type) {
        super(world, thrower);
        this.type = type;
    }

    /** Аркебуза (§4.1): свой урон, пробитие 2 целей, дальний полёт. */
    public EntityFluxBullet withGunProfile(float baseDamage, int pierce, int life) {
        this.damage = baseDamage;
        this.pierceLeft = pierce;
        this.lifeTicks = life;
        return this;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && this.ticksExisted > this.lifeTicks) {
            this.setDead();
        }
        if (this.world.isRemote) {
            this.world.spawnParticle(this.type == TYPE_INCENDIARY
                            ? EnumParticleTypes.FLAME
                            : this.type == TYPE_FLUX
                                    ? EnumParticleTypes.SPELL_MOB
                                    : EnumParticleTypes.END_ROD,
                    this.posX, this.posY, this.posZ, 0, 0, 0);
        }
    }

    @Override
    protected float getGravityVelocity() {
        return 0.001F;   // алхимический снаряд летит настильно
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (this.world.isRemote) {
            return;
        }
        float mult = this.type == TYPE_ILLUMINATING ? 0.5F : 1.0F;
        Entity hit = result.entityHit;
        if (hit != null) {
            this.strike(hit, this.damage * mult);
            // §4.1 аркебузы: пробитие — летим дальше, вторая цель ×0.5
            if (this.pierceLeft > 0) {
                this.pierceLeft--;
                this.damage *= 0.5F;
                return;   // не умираем, снаряд идёт насквозь
            }
        }
        if (this.type == TYPE_ILLUMINATING) {
            // §4.2: блок света на 60 сек в точке попадания
            BlockPos at = hit != null ? hit.getPosition()
                    : result.getBlockPos().offset(result.sideHit);
            if (this.world.isAirBlock(at)) {
                this.world.setBlockState(at,
                        UTBlocks.photonLight.getDefaultState(), 3);
                this.world.scheduleUpdate(at, UTBlocks.photonLight, LIGHT_TICKS);
            }
        }
        if (this.type == TYPE_FLUX) {
            // §4.2: облако флюкс-газа радиусом 2 — родные блоки ТК, они
            // «бьют и своих» своей же механикой и сами рассеиваются
            BlockPos center = hit != null ? hit.getPosition()
                    : result.getBlockPos().offset(result.sideHit);
            for (BlockPos at : BlockPos.getAllInBoxMutable(
                    center.add(-1, 0, -1), center.add(1, 1, 1))) {
                if (this.world.isAirBlock(at) && this.rand.nextBoolean()) {
                    this.world.setBlockState(at.toImmutable(),
                            ConfigBlocks.blockFluxGas.getStateFromMeta(0), 3);
                }
            }
        }
        this.setDead();
    }

    /** Урон с эффектом патрона; вис-патрон половину бьёт сквозь броню. */
    private void strike(Entity target, float amount) {
        if (this.type == TYPE_VIS) {
            // §4.2: игнорирует 50 % брони — половина обычным уроном,
            // половина мимо брони
            target.attackEntityFrom(DamageSource.causeThrownDamage(
                    this, this.getThrower()), amount * 0.5F);
            target.attackEntityFrom(DamageSource.causeThrownDamage(
                            this, this.getThrower()).setDamageBypassesArmor(),
                    amount * 0.5F);
        } else {
            target.attackEntityFrom(DamageSource.causeThrownDamage(
                    this, this.getThrower()), amount);
        }
        if (this.type == TYPE_INCENDIARY) {
            target.setFire(5);   // §4.2: поджиг 5 сек
        }
    }

    @Override
    public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("UTType", this.type);
        tag.setFloat("UTDamage", this.damage);
        tag.setInteger("UTPierce", this.pierceLeft);
        tag.setInteger("UTLife", this.lifeTicks);
    }

    @Override
    public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.type = tag.getInteger("UTType");
        this.damage = tag.hasKey("UTDamage") ? tag.getFloat("UTDamage") : BASE_DAMAGE;
        this.pierceLeft = tag.getInteger("UTPierce");
        this.lifeTicks = tag.hasKey("UTLife") ? tag.getInteger("UTLife") : 40;
    }
}
