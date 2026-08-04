# Допущения по IC2 API — проверить при первой сборке

Среда, в которой писалась фаза 1, не имела доступа к jar IC2, поэтому все
обращения к IC2 записаны по знанию API IC2 Experimental 2.8.x (1.12.2) и
СВЕРЯЮТСЯ при первой компиляции с реальным jar-ом. Ошибки здесь = ошибки
компиляции, не рантайма — ловятся сразу.

## Классы и сигнатуры

| Использование | Ожидаемая сигнатура |
|---|---|
| `ic2.api.item.IC2Items.getItem(String name, String variant)` | `static ItemStack` |
| `ic2.api.recipe.Recipes.macerator` | `static IMachineRecipeManager` |
| `ic2.api.recipe.Recipes.extractor` | `static IMachineRecipeManager` |
| `ic2.api.recipe.Recipes.compressor` | `static IMachineRecipeManager` |
| `IMachineRecipeManager.addRecipe(IRecipeInput, NBTTagCompound, boolean replace, ItemStack... outputs)` | `boolean` |
| `ic2.api.recipe.Recipes.inputFactory` | `static IRecipeInputFactory` |
| `IRecipeInputFactory.forOreDict(String)` / `.forStack(ItemStack)` | `IRecipeInput` |

## Пары name/variant (IC2Handles / IC2Aspects / IC2Recipes)

Проверить существование каждой в `ic2.core.ref.ItemName`/`BlockName`:
`cable`/`type:copper,insulation:0`; `crafting`/`rubber`, `circuit`,
`advanced_circuit`, `carbon_fibre`, `scrap`, `iridium`; `ingot`/`refined_iron`;
`misc_resource`/`matter`; `re_battery`; `energy_crystal`; `lapotron_crystal`;
`nuclear`/`uranium_238`; `dust`/`silver`; `misc_resource`/`resin`;
`te`/`generator`, `solar_generator`, `macerator`, `iron_furnace`,
`electric_furnace`, `nuclear_reactor`, `teleporter`;
`jetpack`; `drill`; `chainsaw`; `nano_saber`;
`hazmat_helmet`, `hazmat_chestplate`, `hazmat_leggings`, `rubber_boots`;
`nano_helmet`, `nano_chestplate`, `nano_leggings`, `nano_boots`;
`quantum_helmet`, `quantum_chestplate`, `quantum_leggings`, `quantum_boots`.

## Семантика входов рецептов

Допущение: `IRecipeInputFactory.forStack(ItemStack)` использует РАЗМЕР стека
как требуемое количество входа (рецепты «6 осколков → кластер»,
«4 янтаря → блок» передают стеки размера 6/4). Если в реальном API размер
игнорируется — заменить на перегрузку с явным amount (`forStack(stack, n)`
или эквивалент) при первой сборке.

## Dev-запуск (runClient)

Релизный jar IC2 собран под SRG — в deobf-окружении ForgeGradle он не
загрузится. Для `runClient` подключать IC2 через
`deobfCompile 'curse.maven:industrial-craft-242638:<fileId>'`
(см. комментарий в build.gradle). Для обычного `build` достаточно jar в libs/.

Рантайм-страховка уже встроена: `IC2Handles.item()` на отсутствующий предмет
даёт WARN и пустой стек, зависимый контент пропускается без краша.

## modid аддонов (CompatIds)

- METS (`mets`) и Advanced Solar Panels (`advanced_solar_panels`) — сверить
  по `mcmod.info` реальных jar-ов перед фазой 10.

## Формула генератора

Генератор IC2 принимает предметы с furnace burn time. Ожидание: алюментум
(6400 тиков, задаётся портом TC4U ≥1.2.8.0) → ~16,000 EU. Замерить в игре —
приёмочный пункт 5 из `phase1_core_spec.md` §8.
