package unboundtech.common.entities;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import thaumcraft.common.config.ConfigBlocks;
import unboundtech.common.UTBlocks;

/**
 * Снаряд Мортиры Механистов (`mechanist_mortar.md` §4.2, §5): летит по
 * НАВЕСНОЙ дуге и падает сверху — стена от мортиры не защищает, потолок
 * защищает полностью. Урон по площади: 24 в эпицентре × модификатор
 * патрона, линейный спад до 8 на краю радиуса 4. Эффект патрона
 * накрывает весь радиус. БЛОКИ НЕ ЛОМАЕТ — правило всего мода.
 */
public class EntityMortarShell extends EntityThrowable {

    public static final float CORE_DAMAGE = 24.0F;
    public static final float EDGE_DAMAGE = 8.0F;
    public static final double BLAST_RADIUS = 4.0;
    private static final int LIFE_TICKS = 200;

    private int type = EntityFluxBullet.TYPE_INCENDIARY;

    public EntityMortarShell(World world) {
        super(world);
    }

    public EntityMortarShell(World world, EntityLivingBase thrower, int type) {
        super(world, thrower);
        this.type = type;
    }

    /** Авто-режим: стрелка нет — EntityThrowable(world, null) даёт NPE. */
    public EntityMortarShell(World world, int type) {
        super(world);
        this.type = type;
    }

    @Override
    protected float getGravityVelocity() {
        return 0.05F;   // тяжёлая дуга — характер мортиры (§4.2)
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && this.ticksExisted > LIFE_TICKS) {
            this.setDead();
        }
        if (this.world.isRemote) {
            this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    this.posX, this.posY, this.posZ, 0, 0.02, 0);
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (this.world.isRemote) {
            return;
        }
        float mult = this.type == EntityFluxBullet.TYPE_ILLUMINATING ? 0.5F : 1.0F;
        AxisAlignedBB zone = this.getEntityBoundingBox().grow(BLAST_RADIUS);
        List<EntityLivingBase> hit = this.world
                .getEntitiesWithinAABB(EntityLivingBase.class, zone);
        for (EntityLivingBase target : hit) {
            if (target == this.getThrower()) {
                continue;   // сам наводчик у орудия не в счёт
            }
            double dist = target.getDistance(this.posX, this.posY, this.posZ);
            if (dist > BLAST_RADIUS) {
                continue;
            }
            // §4.2 канона: потолок защищает ПОЛНОСТЬЮ — цель за
            // блоками не получает ничего (вердикт-ревью №3)
            if (this.world.rayTraceBlocks(
                    new net.minecraft.util.math.Vec3d(this.posX, this.posY, this.posZ),
                    new net.minecraft.util.math.Vec3d(target.posX,
                            target.posY + target.height / 2.0, target.posZ),
                    false, true, false) != null) {
                continue;
            }
            // §5: линейный спад 24 → 8 от эпицентра к краю
            float damage = (float) (CORE_DAMAGE
                    - (CORE_DAMAGE - EDGE_DAMAGE) * dist / BLAST_RADIUS) * mult;
            target.attackEntityFrom(DamageSource.causeThrownDamage(
                    this, this.getThrower()), damage);
            if (this.type == EntityFluxBullet.TYPE_INCENDIARY) {
                target.setFire(5);
            }
        }
        // эффекты среды патрона — в точке падения
        BlockPos at = this.getPosition();
        if (this.type == EntityFluxBullet.TYPE_ILLUMINATING
                && this.world.isAirBlock(at)) {
            this.world.setBlockState(at, UTBlocks.photonLight.getDefaultState(), 3);
            this.world.scheduleUpdate(at, UTBlocks.photonLight,
                    EntityFluxBullet.LIGHT_TICKS);
        }
        if (this.type == EntityFluxBullet.TYPE_FLUX) {
            for (BlockPos p : BlockPos.getAllInBoxMutable(
                    at.add(-1, 0, -1), at.add(1, 1, 1))) {
                if (this.world.isAirBlock(p) && this.rand.nextBoolean()) {
                    this.world.setBlockState(p.toImmutable(),
                            ConfigBlocks.blockFluxGas.getStateFromMeta(0), 3);
                }
            }
        }
        // громкий низкий выстрел-разрыв + кольцо дыма (§8); блоки целы
        this.world.playEvent(2001, at, net.minecraft.block.Block
                .getStateId(net.minecraft.init.Blocks.STONE.getDefaultState()));
        this.world.playSound(null, this.posX, this.posY, this.posZ,
                net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
                net.minecraft.util.SoundCategory.BLOCKS, 1.2f, 0.6f);
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
