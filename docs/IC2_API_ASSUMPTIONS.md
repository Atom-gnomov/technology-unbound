# IC2 API: что подтверждено и что осталось допущением

Jar IC2 в среде разработки недоступен (CurseMaven и CDN закрыты прокси),
поэтому API сверялся по исходникам `ic2/api/**` из **2.8.220-ex112-dev**
(вендоренная копия в открытом проекте-туторе, побайтово сверенная со второй
независимой копией и с версионными патчами Su5eD/IC2-Patcher для 2.8.164
и 2.8.221+), а также по конфигам, которые генерирует сам IC2.

**Статус: сигнатуры энергетики, рецептов и имена предметов — ПОДТВЕРЖДЕНЫ.**
Остаточный риск — расхождение конкретной сборки IC2 у игрока; всё, что
резолвится через `IC2Handles`, деградирует в WARN, а не в краш.

## Подтверждённые находки, повлиявшие на код

| Факт | Следствие |
|---|---|
| `ingot/refined_iron` — **только IC2 Classic**; в Experimental есть `ingot/steel` | таум-сталь плавится из `ingot/steel`, фолбэк — ванильное железо |
| `crafting/iridium` — это Иридиевая **пластина**, сырой иридий — `misc_resource/iridium_ore` / `iridium_shard` | аспекты разведены на три записи |
| Броня: `nano_chestplate`/`quantum_chestplate` (не `*_bodyarmor`), у хазмата ботинки — `rubber_boots` | имена в коде верны |
| `misc_resource/resin` (не `crafting/resin`) | рецепт экстрактора верен |
| `dust/silver` существует | рецепт дробления серебряной руды верен |
| `Recipes.macerator` и т.п. имеют тип `IBasicMachineRecipeManager`, метод `addRecipe(IRecipeInput, NBTTagCompound, boolean, ItemStack...)`; 3-аргументной формы 1.7.10 нет | вызовы в коде верны |
| `forStack(stack)` берёт **размер стека** как требуемое количество | рецепты «6 осколков → кластер» верны |
| Топлива через IC2 API нет — генератор читает ванильный burn time | алюментум работает без нашего кода |
| modid аддонов: `advanced_solar_panels`, `mets` | CompatIds верны |

## Энергетический слой (фаза 3а)

| Использование | Ожидаемая сигнатура |
|---|---|
| `ic2.api.energy.tile.IEnergySource` | `double getOfferedEnergy()`, `void drawEnergy(double)`, `int getSourceTier()` |
| `ic2.api.energy.tile.IEnergyEmitter` | `boolean emitsEnergyTo(IEnergyAcceptor, EnumFacing)` |
| `ic2.api.energy.tile.IEnergySink` | `double getDemandedEnergy()`, `int getSinkTier()`, `double injectEnergy(EnumFacing, double, double)` |
| `ic2.api.energy.tile.IEnergyAcceptor` | `boolean acceptsEnergyFrom(IEnergyEmitter, EnumFacing)` |
| `ic2.api.energy.event.EnergyTileLoadEvent(IEnergyTile)` | постится в `MinecraftForge.EVENT_BUS` в `TileEntity.onLoad()` |
| `ic2.api.energy.event.EnergyTileUnloadEvent(IEnergyTile)` | постится в `invalidate()` и `onChunkUnload()` |

Выбраны «сырые» интерфейсы, а не `prefab.BasicSource/BasicSink`: у префабов
сигнатура конструктора менялась между сборками IC2, а интерфейсы стабильны.

Подтверждённые нюансы, учтённые в коде:
- `emitsEnergyTo` объявлен на `IEnergyEmitter`, `acceptsEnergyFrom` — на
  `IEnergyAcceptor`; `IEnergyTile` — пустой маркер;
- конструктор события принимает `IEnergyTile` и **внутри** зовёт
  `EnergyNet.instance.getWorld(tile)` — бросает NPE, если тайл ещё не в мире.
  Поэтому load постится строго в `onLoad()`, а не в `validate()`;
- unload обязателен и в `invalidate()`, и в `onChunkUnload()`, причём **до**
  вызова `super` (тайл должен быть ещё связан с миром);
- `getSinkTier()` может вернуть `Integer.MAX_VALUE`, чтобы не взрываться от
  повышенного напряжения. Мы сознательно оставляем MV (2): взрыв от HV —
  честное поведение машины IC2, игрок ставит трансформатор.

## Классы и сигнатуры

| Использование | Сигнатура (подтверждена по 2.8.220) |
|---|---|
| `ic2.api.item.IC2Items.getItem(String name, String variant)` | `static ItemStack`; `getItem(name)` == `getItem(name, null)` |
| `ic2.api.recipe.Recipes.macerator/extractor/compressor` | `static IBasicMachineRecipeManager` |
| `IBasicMachineRecipeManager.addRecipe(IRecipeInput, NBTTagCompound, boolean replace, ItemStack... outputs)` | `boolean` |
| `ic2.api.recipe.Recipes.inputFactory` | `static IRecipeInputFactory` |
| `IRecipeInputFactory.forOreDict(String)` / `.forStack(ItemStack)` / `.forStack(ItemStack, int)` | `IRecipeInput` |

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

Решено: IC2 подключён `deobfCompile 'curse.maven:industrial-craft-242638:3838713'`
(файл выбран владельцем) — FG 2.3 деобфусцирует jar, работает и build,
и runClient. Пары name/variant из этого файла — эталон для сверки таблицы выше.

Рантайм-страховка уже встроена: `IC2Handles.item()` на отсутствующий предмет
даёт WARN и пустой стек, зависимый контент пропускается без краша.

## modid аддонов (CompatIds)

- METS (`mets`) и Advanced Solar Panels (`advanced_solar_panels`) — сверить
  по `mcmod.info` реальных jar-ов перед фазой 10.

## Формула генератора

Генератор IC2 принимает предметы с furnace burn time. Ожидание: алюментум
(6400 тиков, задаётся портом TC4U ≥1.2.8.0) → ~16,000 EU. Замерить в игре —
приёмочный пункт 5 из `phase1_core_spec.md` §8.
