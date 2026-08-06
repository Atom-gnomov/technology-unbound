# Допущения по IC2 API — ПРОВЕРЕНЫ первой сборкой (2026-08-06)

Фаза 1 писалась без доступа к jar IC2: все обращения записаны по знанию API
IC2 Experimental 2.8.x (1.12.2). Первая сборка состоялась 2026-08-06 против
**IC2 2.8.222-ex112** (CurseMaven файл 3838713, деобфусцирован FG 2.3) и
dev-jar порта **TC4U 1.2.8.0** (ветка `claude/ic2-thaumcraft-integration-agwnn4`).

Итог: **компиляция прошла с первого раза, все допущения подтверждены** —
и сигнатуры (компилятором), и строки name/variant (по enum-ам реального jar),
и семантика входов (декомпиляцией CFR). Ниже — таблицы со статусами; при
смене версии IC2 сверять заново по этому же методу.

## Классы и сигнатуры — ✅ подтверждено компиляцией

| Использование | Сигнатура | Статус |
|---|---|---|
| `ic2.api.item.IC2Items.getItem(String name, String variant)` | `static ItemStack` | ✅ |
| `ic2.api.recipe.Recipes.macerator` | `static IMachineRecipeManager` | ✅ |
| `ic2.api.recipe.Recipes.extractor` | `static IMachineRecipeManager` | ✅ |
| `ic2.api.recipe.Recipes.compressor` | `static IMachineRecipeManager` | ✅ |
| `IMachineRecipeManager.addRecipe(IRecipeInput, NBTTagCompound, boolean replace, ItemStack... outputs)` | `boolean` | ✅ |
| `ic2.api.recipe.Recipes.inputFactory` | `static IRecipeInputFactory` | ✅ |
| `IRecipeInputFactory.forOreDict(String)` / `.forStack(ItemStack)` | `IRecipeInput` | ✅ |

## Пары name/variant — ✅ все найдены в enum-ах 2.8.222

Сверено javap-ом по `ic2.core.ref.ItemName`, `ic2.core.ref.BlockName`,
`ic2.core.ref.TeBlock` (NB: TeBlock живёт в `ref`, не в `block`) и
enum-ам вариантов `ic2.core.item.type.*`:

- `cable` / `type:copper,insulation:0` — синтаксис варианта подтверждён
  дословно рецепт-конфигами самого IC2 (`assets/ic2/config/*.ini`:
  `ic2:cable#type:copper,insulation:0`);
- `crafting` / `rubber`, `circuit`, `advanced_circuit`, `carbon_fibre`,
  `scrap`, `iridium` — все в `CraftingItemType`;
- `ingot` / `refined_iron` — **существует** в `IngotResourceType`
  (опасение «refined iron = steel» не подтвердилось: в 2.8.x есть оба);
- `misc_resource` / `matter`, `resin`, `iridium_ore` — в `MiscResourceType`;
- `nuclear` / `uranium_238` — в `NuclearResourceType`;
- `dust` / `silver` — в `DustResourceType`;
- `re_battery`, `energy_crystal`, `lapotron_crystal`, `jetpack`, `drill`,
  `chainsaw`, `nano_saber` — плоские ItemName без вариантов (variant=null OK,
  см. семантику ниже);
- `hazmat_helmet`, `hazmat_chestplate`, `hazmat_leggings`, `rubber_boots`,
  `nano_helmet/chestplate/leggings/boots`,
  `quantum_helmet/chestplate/leggings/boots` — все в ItemName;
- `te` / `generator`, `solar_generator`, `macerator`, `iron_furnace`,
  `electric_furnace`, `nuclear_reactor`, `teleporter` — BlockName `te`
  + все семь констант в `TeBlock`.

## Семантика входов рецептов — ✅ подтверждено декомпиляцией

`RecipeInputFactory.forStack(stack)` → `new RecipeInputItemStack(stack)` →
`this(input, StackUtil.getSize(input))`: **размер стека = требуемое
количество входа**. Рецепты «6 осколков → кластер» и «4 янтаря → блок»
корректны как написаны. Перегрузка с явным amount тоже существует
(`forStack(stack, n)`) — запасной вариант не понадобился.

`IC2Items.getItem(name, null)`: `ItemAPI.getItemStack` при variant=null
дополнительно понимает слитную запись `name#variant`, затем зовёт
`ItemName.getItemStack(variant)` — для предметов без вариантов null
корректен. Плюс рантайм-страховка `IC2Handles.item()`: WARN и пустой стек
вместо краша, зависимый контент пропускается.

## Энергетические префабы (фаза 3а) — ✅ сверено 2026-08-06

- `ic2.api.energy.prefab.BasicSource(TileEntity, double capacity, int tier)`
  и `BasicSink(...)` — конструкторы есть; TileEntity-версия резолвит
  мир/позицию ЛЕНИВО (`initLocation()` при первом обращении), поэтому
  префаб можно создавать полем тайла.
- Жизненный цикл: `update()` (зовётся каждый тик, сам регистрирует тайл
  в энергосети при первом вызове), `onLoad()`, `invalidate()`,
  `onChunkUnload()`, `readFromNBT(tag)` / `writeToNBT(tag)`.
- Энергия: `addEnergy(double)`, `useEnergy(double)`, `canUseEnergy(double)`,
  `getEnergyStored()`, `getCapacity()`. Все no-op на клиенте
  (`getWorldObj().isRemote` внутри).
- `ic2.api.energy.tile.IHeatSource`: `maxrequestHeatTick(EnumFacing)` +
  `requestHeat(EnumFacing, int)` (оба @Deprecated, но именно они — точка
  расширения; `drawHeat` по умолчанию делегирует в них). Реализован
  портом в `TileNitor` за `@Optional` (TC4U 1.2.8.1).

## modid аддонов (CompatIds) — ✅ сверено 2026-08-06

- `MoreElectricTools.v1.662.jar` → modid **`mets`**;
  `Advanced Solar Panels-4.3.0.jar` (Chocohead) → modid
  **`advanced_solar_panels`** — оба совпали с константами `CompatIds`.
- Реестровые имена предметов (сверены декомпиляцией CFR + модели/ланг):
  ASP: `advanced_solar_panels:machines` (ItemBlock, меты = `TEs.getId()`
  0–5), `:crafting` (ItemMulti, меты = `CraftingTypes` 0–13), шлемы
  `advanced_solar_helmet`/`hybrid_solar_helmet`/`ultimate_solar_helmet`.
  METS: обычные предметы, registry name = имя файла модели
  (`mets:titanium_ingot` и т.д.), мета 0; блоки — через их ItemBlock.
- Доступ — `ModItems.item(modid, path, meta)` через ForgeRegistries
  (WARN + пустой стек на промах, контент пропускается без краша).

## Формула генератора — ⏳ замерить в игре

Генератор IC2 принимает предметы с furnace burn time. Ожидание: алюментум
(6400 тиков, задаётся портом TC4U ≥1.2.8.0) → ~16,000 EU. Приёмка: пункт 5
чек-листа `phase1_core_spec.md` §8 (16,000 ± 5%). Требует запуска игры с
TC4U 1.2.8.0+ (ветка ещё не влита в main порта) + IC2 + сабмодом.
