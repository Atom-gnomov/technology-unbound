package unboundtech.common.items;

import com.google.common.collect.Multimap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import unboundtech.common.UTItems;
import unboundtech.common.entities.EntityFluxBullet;

/**
 * Флюкс-Револьвер (`05_objects/flux_revolver.md`): первый ствол мода —
 * не мощный, а ПРЕДСКАЗУЕМЫЙ. Работает на патронах, без EU (§5: суть —
 * патрон; электрические стволы в паке уже есть).
 *
 *  - барабан 6 патронов ОДНОГО типа, смешивать нельзя (§4);
 *  - темп 1 выстрел / 10 тиков — держится серверным кулдауном, клиент
 *    не решает (§10); удержание ПКМ ведёт огонь очередью;
 *  - Shift-ПКМ — перезарядка из инвентаря (первый найденный тип), при
 *    чужом типе в барабане — сперва разрядка (§9);
 *  - гильза возвращается под ноги с шансом 50 % (§4.1);
 *  - прочность 512 выстрелов, чинится закалённым таумием;
 *  - ЛКМ — вежливые 2 урона рукоятью, это не оружие ближнего боя.
 */
public class ItemFluxRevolver extends Item {

    /** §5. */
    public static final float BASE_DAMAGE = EntityFluxBullet.BASE_DAMAGE;
    public static final int DRUM_SIZE = 6;
    public static final int SHOT_COOLDOWN_TICKS = 10;
    public static final int RELOAD_TICKS_PER_ROUND = 15;
    public static final int DURABILITY_SHOTS = 512;

    private static final String TAG_TYPE = "UTAmmo";
    private static final String TAG_COUNT = "UTAmmoCount";

    public ItemFluxRevolver() {
        this.setMaxStackSize(1);
        this.setMaxDamage(DURABILITY_SHOTS);
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == UTItems.temperedIngot;
    }

    // ================= стрельба =================

    /**
     * ⚠️ Урок примерки: NBT-изменения предмета ТОЛЬКО на сервере в
     * КРЕАТИВЕ затираются авторитетным клиентским инвентарём — барабан
     * «не заряжался». Поэтому вся логика барабана выполняется на ОБЕИХ
     * сторонах симметрично (детерминированно), а мир-эффекты — снаряд,
     * звук, гильза — только на сервере. Темп держит ванильный кулдаун
     * предмета: пока он тикает, повторный клик не проходит.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
                                                    EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            this.reload(stack, player);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        this.fire(stack, player);
        // Удержание ПКМ — очередь по барабану через onUsingTick.
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase living, int count) {
        if (living instanceof EntityPlayer) {
            this.fire(stack, (EntityPlayer) living);
        }
    }

    private void fire(ItemStack stack, EntityPlayer player) {
        if (player.getCooldownTracker().hasCooldown(this)) {
            return;
        }
        World world = player.world;
        int count = ammoCount(stack);
        if (count <= 0) {
            // §4: щелчок бойка, выстрела нет
            if (!world.isRemote) {
                world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.4f, 1.6f);
                player.sendStatusMessage(new TextComponentString(
                        "§7Барабан пуст — Shift-ПКМ с патронами в инвентаре"), true);
            }
            player.getCooldownTracker().setCooldown(this, SHOT_COOLDOWN_TICKS);
            return;
        }
        int type = ammoType(stack);
        // барабан — на обеих сторонах (креатив!), мир — только сервер
        setAmmo(stack, type, count - 1);
        stack.damageItem(1, player);
        player.getCooldownTracker().setCooldown(this, SHOT_COOLDOWN_TICKS);
        if (world.isRemote) {
            return;
        }
        EntityFluxBullet bullet = new EntityFluxBullet(world, player, type);
        // §4: дальность ~32 блока, разброс 2°
        bullet.shoot(player, player.rotationPitch, player.rotationYaw,
                0.0f, 2.4f, 2.0f);
        world.spawnEntity(bullet);
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.5f, 1.5f);
        // §4.1: с шансом 50 % гильза выпадает под ноги
        if (world.rand.nextBoolean()) {
            world.spawnEntity(new EntityItem(world,
                    player.posX, player.posY, player.posZ,
                    new ItemStack(UTItems.casing)));
        }
    }

    // ================= перезарядка =================

    /**
     * Тип берётся у первого патрона по порядку слотов (§10); чужой тип в
     * барабане — сперва разрядка обратно в инвентарь. Время перезарядки
     * оплачивается кулдауном: 15 тиков за патрон (§5).
     */
    private void reload(ItemStack stack, EntityPlayer player) {
        boolean server = !player.world.isRemote;
        int have = ammoCount(stack);
        int type = ammoType(stack);

        ItemStack found = ItemStack.EMPTY;
        for (ItemStack slot : player.inventory.mainInventory) {
            if (slot.getItem() instanceof ItemCartridge) {
                found = slot;
                break;
            }
        }
        if (found.isEmpty()) {
            if (have > 0) {
                this.unload(stack, player, type, have);
                return;
            }
            player.sendStatusMessage(new TextComponentString(
                    "§cВ инвентаре нет патронов"), true);
            return;
        }
        int foundType = ((ItemCartridge) found.getItem()).bulletType;
        if (have > 0 && foundType != type) {
            this.unload(stack, player, type, have);
            return;
        }
        int loaded = 0;
        for (ItemStack slot : player.inventory.mainInventory) {
            if (have + loaded >= DRUM_SIZE) {
                break;
            }
            if (slot.getItem() instanceof ItemCartridge
                    && ((ItemCartridge) slot.getItem()).bulletType == foundType) {
                int take = Math.min(slot.getCount(), DRUM_SIZE - have - loaded);
                slot.shrink(take);
                loaded += take;
            }
        }
        if (loaded > 0) {
            setAmmo(stack, foundType, have + loaded);
            player.getCooldownTracker().setCooldown(this,
                    RELOAD_TICKS_PER_ROUND * loaded);
            if (server) {
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
                        SoundCategory.PLAYERS, 0.5f, 1.4f);
            }
        }
    }

    /** Разрядка барабана обратно в патроны (§12.2 — пока разрешена). */
    private void unload(ItemStack stack, EntityPlayer player, int type, int have) {
        Item cartridge = type == EntityFluxBullet.TYPE_ILLUMINATING
                ? UTItems.cartridgeIlluminating : UTItems.cartridgeIncendiary;
        ItemStack back = new ItemStack(cartridge, have);
        if (!player.inventory.addItemStackToInventory(back)) {
            player.dropItem(back, false);
        }
        setAmmo(stack, type, 0);
        player.getCooldownTracker().setCooldown(this,
                RELOAD_TICKS_PER_ROUND * have);
        player.sendStatusMessage(new TextComponentString("§eБарабан разряжен"), true);
    }

    // ================= NBT барабана =================

    public static int ammoType(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getInteger(TAG_TYPE);
    }

    public static int ammoCount(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getInteger(TAG_COUNT);
    }

    private static void setAmmo(ItemStack stack, int type, int count) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(TAG_TYPE, type);
        tag.setInteger(TAG_COUNT, count);
    }

    // ================= мелочи =================

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;   // без позы лука: короткий ствол
    }

    /** ЛКМ — вежливые 2 урона рукоятью (§9). */
    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(
            EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> map =
                super.getAttributeModifiers(slot, stack);
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER,
                            "Weapon modifier", 1.0, 0));
        }
        return map;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> lines, ITooltipFlag flag) {
        int count = ammoCount(stack);
        if (count <= 0) {
            lines.add("§7" + I18n.translateToLocal("unboundtech.tooltip.revolver_empty"));
        } else {
            String name = ammoType(stack) == EntityFluxBullet.TYPE_ILLUMINATING
                    ? I18n.translateToLocal("item.unboundtech.cartridge_illuminating.name")
                    : I18n.translateToLocal("item.unboundtech.cartridge_incendiary.name");
            lines.add("§7" + name + ": " + count + " / " + DRUM_SIZE);
        }
        lines.add("§7" + I18n.translateToLocal("unboundtech.tooltip.revolver_damage")
                + " " + (int) BASE_DAMAGE);
    }
}
