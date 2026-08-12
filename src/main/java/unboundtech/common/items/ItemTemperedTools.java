package unboundtech.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Инструменты и оружие из закалённого таумия
 * (`05_objects/tempered_thaumium_tools.md`).
 *
 * Своей логики нет намеренно (§4): спецэффекты — территория родных
 * инструментов ТК, наша ниша только прочность. Подклассы существуют по двум
 * причинам: у {@link ItemPickaxe} и {@link ItemAxe} конструкторы
 * {@code protected}, и всем пяти нужен общий тултип §9.
 *
 * ⚠️ Топор обязан создаваться трёхаргументным конструктором. Одноаргументный
 * читает урон и скорость из массивов на пять элементов по
 * {@code material.ordinal()}, а у материала из {@code EnumHelper} ordinal = 5:
 * одноаргументный вариант уронил бы игру на регистрации предметов.
 * Значения взяты алмазные — материал по канону «алмаз с прочностью ×1.2».
 */
public final class ItemTemperedTools {

    /** Урон алмазного топора (`ItemAxe.ATTACK_DAMAGES[3]`). */
    private static final float AXE_DAMAGE = 8.0F;
    /** Скорость алмазного топора (`ItemAxe.ATTACK_SPEEDS[3]`). */
    private static final float AXE_SPEED = -3.0F;

    /** Ключ тултипа «Прочность 1873 — на 20 % выше алмаза» (§9). */
    public static final String TOOLTIP_KEY = "unboundtech.tooltip.tempered_tool";

    private ItemTemperedTools() {
    }

    private static void tooltip(List<String> lines) {
        lines.add("§7" + I18n.translateToLocal(TOOLTIP_KEY));
    }

    public static class Sword extends ItemSword {
        public Sword(Item.ToolMaterial material) {
            super(material);
        }

        @Override
        public void addInformation(ItemStack stack, @Nullable World world,
                                   List<String> lines, ITooltipFlag flag) {
            tooltip(lines);
        }
    }

    public static class Pickaxe extends ItemPickaxe {
        public Pickaxe(Item.ToolMaterial material) {
            super(material);
        }

        @Override
        public void addInformation(ItemStack stack, @Nullable World world,
                                   List<String> lines, ITooltipFlag flag) {
            tooltip(lines);
        }
    }

    public static class Axe extends ItemAxe {
        public Axe(Item.ToolMaterial material) {
            super(material, AXE_DAMAGE, AXE_SPEED);
        }

        @Override
        public void addInformation(ItemStack stack, @Nullable World world,
                                   List<String> lines, ITooltipFlag flag) {
            tooltip(lines);
        }
    }

    public static class Spade extends ItemSpade {
        public Spade(Item.ToolMaterial material) {
            super(material);
        }

        @Override
        public void addInformation(ItemStack stack, @Nullable World world,
                                   List<String> lines, ITooltipFlag flag) {
            tooltip(lines);
        }
    }

    public static class Hoe extends ItemHoe {
        public Hoe(Item.ToolMaterial material) {
            super(material);
        }

        @Override
        public void addInformation(ItemStack stack, @Nullable World world,
                                   List<String> lines, ITooltipFlag flag) {
            tooltip(lines);
        }
    }
}
