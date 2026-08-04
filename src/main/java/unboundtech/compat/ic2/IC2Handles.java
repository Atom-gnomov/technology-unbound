package unboundtech.compat.ic2;

import ic2.api.item.IC2Items;
import net.minecraft.item.ItemStack;
import unboundtech.UTLog;

/**
 * Безопасный доступ к предметам IC2. Строки name/variant нестабильны между
 * версиями IC2 — отсутствующий предмет даёт WARN в лог и пустой стек,
 * а вызывающий код обязан пропустить регистрацию, не роняя игру.
 *
 * Все использованные пары name/variant перечислены в docs/IC2_API_ASSUMPTIONS.md.
 */
public final class IC2Handles {

    private IC2Handles() {
    }

    /** @return стек предмета IC2 или {@link ItemStack#EMPTY} с WARN в логе. */
    public static ItemStack item(String name, String variant) {
        ItemStack stack;
        try {
            stack = IC2Items.getItem(name, variant);
        } catch (RuntimeException e) {
            stack = null;
        }
        if (stack == null || stack.isEmpty()) {
            UTLog.warn("IC2 item not found: {}:{} — related content skipped "
                    + "(check docs/IC2_API_ASSUMPTIONS.md against your IC2 version)",
                    name, variant);
            return ItemStack.EMPTY;
        }
        return stack;
    }

    /** Вариант без подварианта (у некоторых предметов IC2 variant == null). */
    public static ItemStack item(String name) {
        return item(name, null);
    }

    /** Копия стека с заданным размером; пустой вход остаётся пустым. */
    public static ItemStack withCount(ItemStack stack, int count) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }
}
