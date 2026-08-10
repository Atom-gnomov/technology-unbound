package unboundtech.aspects;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.ThaumcraftApiHelper;
import unboundtech.UTLog;

/**
 * Аспектная экономика мода: аспекты предмета не назначаются на глаз, а
 * выводятся из состава его рецепта.
 *
 * Канон — {@code docs/design/04_systems/aspect_economy.md} в репо порта:
 *
 * <pre>
 * A(результат) = нормализация( порог( k × Σ A(компоненты) ) + подпись(процесс) )
 * </pre>
 *
 * Аспекты компонентов берутся <b>в рантайме</b> у самого Таумкрафта
 * ({@link ThaumcraftApiHelper#getObjectAspects}), поэтому чужие моды,
 * их изменения и правки рецептов учитываются сами собой и ничего не
 * хардкодится.
 */
public final class AspectFormula {

    /** Коэффициент передачи (канон §2.1). Допустимый диапазон — 0.5…0.75. */
    public static double TRANSFER = 0.6D;

    /** Минимально допустимый коэффициент передачи (ниже предметы «пустеют»). */
    public static final double TRANSFER_MIN = 0.5D;

    /** Максимально допустимый (выше — сборка выгоднее разборки, тигель дюпает). */
    public static final double TRANSFER_MAX = 0.75D;

    /** Порог значимости: доля от самого сильного аспекта (канон §2.2). */
    private static final double THRESHOLD_SHARE = 0.15D;

    /** Абсолютный минимум порога: аспекты слабее 2 не показываем никогда. */
    private static final int THRESHOLD_FLOOR = 2;

    /** Потолок разнообразия: не больше пяти аспектов (канон §2.3). */
    private static final int MAX_ASPECTS = 5;

    /** Нормализация: суммарный объём аспектов предмета (канон §2.5). */
    private static final int TOTAL_CAP = 40;

    /** Подписи процессов (канон §2.4): чем именно предмет сделан. */
    public enum Process {
        /** Доменная печь / плавка. */
        SMELTING(Aspect.FIRE, 3, null, 0),
        /** Тигель (алхимия). */
        CRUCIBLE(Aspect.MAGIC, 3, null, 0),
        /** Арканный верстак. */
        ARCANE_BENCH(Aspect.MAGIC, 2, Aspect.CRAFT, 2),
        /** Инфузия. */
        INFUSION(Aspect.MAGIC, 4, Aspect.AURA, 2),
        /** Дробитель / прессование. */
        MACERATION(Aspect.ENTROPY, 2, null, 0),
        /** Сборка механизма — наши машины. */
        MACHINE_ASSEMBLY(Aspect.MECHANISM, 3, null, 0);

        private final Aspect first;
        private final int firstAmount;
        private final Aspect second;
        private final int secondAmount;

        Process(Aspect first, int firstAmount, Aspect second, int secondAmount) {
            this.first = first;
            this.firstAmount = firstAmount;
            this.second = second;
            this.secondAmount = secondAmount;
        }

        /** Подпись процесса как готовый список аспектов. */
        public AspectList signature() {
            AspectList list = new AspectList();
            list.add(this.first, this.firstAmount);
            if (this.second != null) {
                list.add(this.second, this.secondAmount);
            }
            return list;
        }
    }

    private AspectFormula() {
    }

    /**
     * Выводит аспекты по компонентам рецепта.
     *
     * @param components компоненты; размер стека учитывается (4 слитка — это ×4)
     * @param process    чем изготовлено; даёт подпись
     * @param label      имя объекта для лога
     * @return готовый список аспектов; пустой, если у компонентов их нет
     */
    public static AspectList derive(List<ItemStack> components, Process process, String label) {
        AspectList sum = sumComponents(components, label);
        if (sum.size() == 0) {
            UTLog.warn("Aspect formula for {}: components have no aspects at all — "
                    + "the object would be unscannable", label);
            return process.signature();
        }

        AspectList transferred = transfer(sum);
        AspectList significant = threshold(transferred);
        AspectList trimmed = trim(significant);
        AspectList withSignature = trimmed.copy().add(process.signature());
        AspectList result = normalize(withSignature);

        UTLog.info("Aspect formula {}: Σ={} → ×{}={} → порог={} → топ-{}={} → +{} → итог {}",
                label, format(sum), TRANSFER, format(transferred), format(significant),
                MAX_ASPECTS, format(trimmed), format(process.signature()), format(result));
        return result;
    }

    /**
     * То же самое, но компоненты берутся прямо из зарегистрированного
     * арканного рецепта — так состав не может разъехаться с рецептом.
     *
     * @param recipe  рецепт; допускается {@code null} (рецепт мог не встать
     *                из-за отсутствующего предмета чужого мода)
     * @param process подпись процесса
     * @param label   имя объекта для лога
     * @return аспекты или {@code null}, если рецепта нет
     */
    public static AspectList deriveFromArcane(ShapedArcaneRecipe recipe, Process process,
                                              String label) {
        if (recipe == null || recipe.input == null) {
            UTLog.warn("Aspect formula for {} skipped: recipe is absent", label);
            return null;
        }
        return derive(inputsOf(recipe), process, label);
    }

    /** Разворачивает сетку арканного рецепта в список компонентов. */
    private static List<ItemStack> inputsOf(ShapedArcaneRecipe recipe) {
        List<ItemStack> components = new ArrayList<>();
        for (Object slot : recipe.input) {
            if (slot == null) {
                continue;
            }
            if (slot instanceof ItemStack) {
                components.add((ItemStack) slot);
                continue;
            }
            if (slot instanceof List) {
                // Оредикт-вход: берём первый вариант — аспекты у синонимов
                // одинаковы по определению оредикта.
                List<?> options = (List<?>) slot;
                for (Object option : options) {
                    if (option instanceof ItemStack) {
                        components.add((ItemStack) option);
                        break;
                    }
                }
            }
        }
        return components;
    }

    /** §2.1, первая половина: сумма аспектов компонентов с учётом количества. */
    private static AspectList sumComponents(List<ItemStack> components, String label) {
        AspectList sum = new AspectList();
        for (ItemStack stack : components) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            AspectList tags = ThaumcraftApiHelper.getObjectAspects(stack);
            if (tags == null || tags.size() == 0) {
                // Канон §7.2: молча пропускаем, но говорим об этом в лог —
                // иначе итог тихо занижается и никто не поймёт почему.
                UTLog.warn("Aspect formula for {}: component {} has no aspects, skipped",
                        label, stack.getItem().getRegistryName());
                continue;
            }
            int count = Math.max(1, stack.getCount());
            for (Aspect aspect : tags.getAspects()) {
                if (aspect != null) {
                    // Именно add: merge() в ТК берёт МАКСИМУМ, а нам нужна сумма.
                    sum.add(aspect, tags.getAmount(aspect) * count);
                }
            }
        }
        return sum;
    }

    /** §2.1, вторая половина: умножение на коэффициент передачи. */
    private static AspectList transfer(AspectList sum) {
        double k = clampTransfer();
        AspectList out = new AspectList();
        for (Aspect aspect : sum.getAspects()) {
            if (aspect == null) {
                continue;
            }
            int value = (int) Math.round(sum.getAmount(aspect) * k);
            if (value > 0) {
                out.add(aspect, value);
            }
        }
        return out;
    }

    /** §2.2: отсекаем следовые аспекты. */
    private static AspectList threshold(AspectList list) {
        int strongest = 0;
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null) {
                strongest = Math.max(strongest, list.getAmount(aspect));
            }
        }
        double cut = Math.max(THRESHOLD_FLOOR, strongest * THRESHOLD_SHARE);
        AspectList out = new AspectList();
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null && list.getAmount(aspect) >= cut) {
                out.add(aspect, list.getAmount(aspect));
            }
        }
        // Всё отсеклось (бывает у предмета из одного слабого компонента) —
        // оставляем сильнейший, иначе получим предмет вообще без аспектов.
        if (out.size() == 0 && list.size() > 0) {
            Aspect top = list.getAspectsSortedAmount()[0];
            out.add(top, list.getAmount(top));
        }
        return out;
    }

    /** §2.3: оставляем пятёрку сильнейших. */
    private static AspectList trim(AspectList list) {
        Aspect[] sorted = list.getAspectsSortedAmount();
        AspectList out = new AspectList();
        for (int i = 0; i < sorted.length && out.size() < MAX_ASPECTS; i++) {
            if (sorted[i] != null) {
                out.add(sorted[i], list.getAmount(sorted[i]));
            }
        }
        return out;
    }

    /** §2.5: сжимаем пропорционально, если суммарный объём больше потолка. */
    private static AspectList normalize(AspectList list) {
        int total = list.visSize();
        if (total <= TOTAL_CAP) {
            return list;
        }
        double scale = (double) TOTAL_CAP / (double) total;
        AspectList out = new AspectList();
        for (Aspect aspect : list.getAspects()) {
            if (aspect == null) {
                continue;
            }
            // Не ниже 1: аспект, попавший в итог, не должен исчезать от сжатия.
            out.add(aspect, Math.max(1, (int) Math.round(list.getAmount(aspect) * scale)));
        }
        return out;
    }

    /** Коэффициент передачи в допустимых каноном пределах. */
    private static double clampTransfer() {
        if (TRANSFER < TRANSFER_MIN || TRANSFER > TRANSFER_MAX) {
            UTLog.warn("Aspect transfer coefficient {} is outside the canon range {}..{} — clamped",
                    TRANSFER, TRANSFER_MIN, TRANSFER_MAX);
            return Math.min(TRANSFER_MAX, Math.max(TRANSFER_MIN, TRANSFER));
        }
        return TRANSFER;
    }

    /** Компактная запись списка для лога: «Metallum 18, Terra 5». */
    private static String format(AspectList list) {
        StringBuilder sb = new StringBuilder();
        for (Aspect aspect : list.getAspectsSortedAmount()) {
            // getAspects()/getAspectsSortedAmount() на пустом списке возвращают
            // массив из одного null — это поведение самого ТК, не опечатка.
            if (aspect == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(aspect.getName()).append(' ').append(list.getAmount(aspect));
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }
}
