package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import unboundtech.common.UTItems;
import unboundtech.common.entities.EntityFluxBullet;

/**
 * Флюкс-Аркебуза (`05_objects/flux_arquebus.md`): длинный ствол T4 —
 * вдвое дальше и вчетверо точнее револьвера, но ОДНОЗАРЯДНАЯ.
 *
 *  - урон 12 × модификатор патрона, ПРОБИТИЕ до 2 целей (вторая ×0.5) —
 *    того нет ни у одного ствола METS (§4.1);
 *  - дальность 64 блока, разброс 0.5°;
 *  - один патрон в стволе, перезарядка 40 тиков (Shift-ПКМ), прерывается
 *    сменой предмета и начинается заново — стрелять на бегу неудобно
 *    намеренно (§4);
 *  - отдача толкает игрока назад на ~0.4 блока (§4.1);
 *  - прочность 1024 выстрела, чинится закалённым таумием;
 *  - EU не ест — суть в патроне, как у револьвера.
 *
 * Урок револьвера учтён: NBT ствола меняется на ОБЕИХ сторонах
 * (креативный клиент авторитетен), мир-эффекты — только сервер.
 * Аркебуза Механистов (§4.2) — дроп данжа, придёт вместе с данжем.
 */
public class ItemFluxArquebus extends Item {

    /** §5. */
    public static final float BASE_DAMAGE = 12.0F;
    public static final int RELOAD_TICKS = 40;
    public static final int RECOIL_TICKS = 12;
    public static final int DURABILITY_SHOTS = 1024;
    public static final int PIERCE_TARGETS = 1;   // +1 цель сквозь первую
    /** Скорость 4.8 и жизнь ~56 тиков дают ~64 блока при точности 0.5°. */
    private static final float VELOCITY = 4.8F;
    private static final float INACCURACY = 0.5F;
    private static final int BULLET_LIFE = 56;

    private static final String TAG_TYPE = "UTAmmo";
    private static final String TAG_LOADED = "UTLoaded";

    public ItemFluxArquebus() {
        this.setMaxStackSize(1);
        this.setMaxDamage(DURABILITY_SHOTS);
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == UTItems.temperedIngot;
    }

    /** Смена NBT не должна дёргать ванильный re-equip поверх отдачи (ТЗ §4). */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack,
                                               ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
                                                    EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            this.reload(stack, player);
        } else {
            this.fire(stack, player);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void fire(ItemStack stack, EntityPlayer player) {
        if (player.getCooldownTracker().hasCooldown(this)) {
            return;
        }
        World world = player.world;
        if (!isLoaded(stack)) {
            if (!world.isRemote) {
                world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.4f, 1.2f);
                player.sendStatusMessage(new TextComponentString(
                        "§7Ствол пуст — Shift-ПКМ с патроном в инвентаре"), true);
            }
            player.getCooldownTracker().setCooldown(this, RECOIL_TICKS);
            return;
        }
        int type = ammoType(stack);
        setLoaded(stack, type, false);
        stack.damageItem(1, player);
        // §8 ТЗ: отдача-анимация едет от этой кулдаун-кривой (12 тиков)
        player.getCooldownTracker().setCooldown(this, RECOIL_TICKS);
        // §4.1: игрока толкает назад на ~0.4 блока — на обеих сторонах,
        // иначе клиент не почувствует
        float yaw = player.rotationYaw * 0.017453292F;
        player.addVelocity(MathHelper.sin(yaw) * 0.4, 0.02,
                -MathHelper.cos(yaw) * 0.4);
        player.velocityChanged = true;
        if (world.isRemote) {
            return;
        }
        EntityFluxBullet bullet = new EntityFluxBullet(world, player, type)
                .withGunProfile(BASE_DAMAGE, PIERCE_TARGETS, BULLET_LIFE);
        bullet.shoot(player, player.rotationPitch, player.rotationYaw,
                0.0f, VELOCITY, INACCURACY);
        world.spawnEntity(bullet);
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.7f);
        if (world.rand.nextBoolean()) {
            world.spawnEntity(new EntityItem(world,
                    player.posX, player.posY, player.posZ,
                    new ItemStack(UTItems.casing)));
        }
    }

    /** Один патрон в ствол; 40 тиков оплачиваются кулдауном (§4). */
    private void reload(ItemStack stack, EntityPlayer player) {
        if (player.getCooldownTracker().hasCooldown(this) || isLoaded(stack)) {
            return;
        }
        for (ItemStack slot : player.inventory.mainInventory) {
            if (slot.getItem() instanceof ItemCartridge) {
                int type = ((ItemCartridge) slot.getItem()).bulletType;
                slot.shrink(1);
                setLoaded(stack, type, true);
                player.getCooldownTracker().setCooldown(this, RELOAD_TICKS);
                if (player.world.isRemote) {
                    unboundtech.client.RenderFluxArquebus.noteReload(player);
                } else {
                    player.world.playSound(null, player.posX, player.posY,
                            player.posZ, SoundEvents.ITEM_ARMOR_EQUIP_IRON,
                            SoundCategory.PLAYERS, 0.5f, 0.8f);
                }
                return;
            }
        }
        if (!player.world.isRemote) {
            player.sendStatusMessage(new TextComponentString(
                    "§cВ инвентаре нет патронов"), true);
        }
    }

    // ================= NBT ствола =================

    public static boolean isLoaded(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(TAG_LOADED);
    }

    public static int ammoType(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getInteger(TAG_TYPE);
    }

    private static void setLoaded(ItemStack stack, int type, boolean loaded) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(TAG_TYPE, type);
        tag.setBoolean(TAG_LOADED, loaded);
    }

    /** Штык (§9 ТЗ модели): вложенный меч в NBT; установка — с данжем. */
    public static ItemStack bayonet(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("UTBayonet")) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(tag.getCompoundTag("UTBayonet"));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        if (isLoaded(stack)) {
            String name = I18n.translateToLocal("item.unboundtech.cartridge_"
                    + ItemCartridge.key(ammoType(stack)) + ".name");
            lines.add("§7" + I18n.translateToLocal("unboundtech.tooltip.arquebus_loaded")
                    + ": " + name);
        } else {
            lines.add("§7" + I18n.translateToLocal("unboundtech.tooltip.arquebus_empty"));
        }
        lines.add("§7" + I18n.translateToLocal("unboundtech.tooltip.revolver_damage")
                + " " + (int) BASE_DAMAGE);
    }
}
