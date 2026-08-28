package unboundtech.common.entities;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import unboundtech.common.UTBlocks;

/**
 * Снаряд Флюкс-Револьвера (`flux_revolver.md` §4.1): СУЩНОСТЬ, а не
 * рейтрейс — он летит, его видно, его можно опередить; этим наша стрельба
 * отличается от хитсканов METS. Время жизни ограничено (§12.3: нагрузка
 * на сервер), дальность ~32 блока задаётся скоростью и потолком тиков.
 */
public class EntityFluxBullet extends EntityThrowable {

    /** Типы T3 (`cartridges.md` §4.2); вис/флюкс/искра придут с T4–T5. */
    public static final int TYPE_INCENDIARY = 0;
    public static final int TYPE_ILLUMINATING = 1;

    /** §5 револьвера: базовый урон задаёт ствол, патрон умножает. */
    public static final float BASE_DAMAGE = 6.0F;
    private static final int LIFE_TICKS = 40;
    /** §4.2 карточки патронов: свет на 60 секунд. */
    public static final int LIGHT_TICKS = 1200;

    private int type = TYPE_INCENDIARY;

    public EntityFluxBullet(World world) {
        super(world);
    }

    public EntityFluxBullet(World world, EntityLivingBase thrower, int type) {
        super(world, thrower);
        this.type = type;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && this.ticksExisted > LIFE_TICKS) {
            this.setDead();
        }
        if (this.world.isRemote) {
            this.world.spawnParticle(this.type == TYPE_INCENDIARY
                            ? EnumParticleTypes.FLAME : EnumParticleTypes.END_ROD,
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
        if (result.entityHit != null) {
            result.entityHit.attackEntityFrom(
                    DamageSource.causeThrownDamage(this, this.getThrower()),
                    BASE_DAMAGE * mult);
            if (this.type == TYPE_INCENDIARY) {
                result.entityHit.setFire(5);   // §4.2: поджиг 5 сек
            }
        }
        if (this.type == TYPE_ILLUMINATING) {
            // §4.2: блок света на 60 сек в точке попадания
            BlockPos at = result.entityHit != null
                    ? result.entityHit.getPosition()
                    : result.getBlockPos().offset(result.sideHit);
            if (this.world.isAirBlock(at)) {
                this.world.setBlockState(at,
                        UTBlocks.photonLight.getDefaultState(), 3);
                this.world.scheduleUpdate(at, UTBlocks.photonLight, LIGHT_TICKS);
            }
        }
        this.setDead();
    }

    @Override
    public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("UTType", this.type);
    }

    @Override
    public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.type = tag.getInteger("UTType");
    }
}
