# Unbound Technology

Мост между **TC4 Unbound** (порт Thaumcraft 4 на 1.12.2) и **IndustrialCraft 2
Experimental**: аспекты на предметах IC2, честная конвертация энергии
(Вис ↔ EU по «законам таумодинамики»), совместные производственные цепочки,
гибридная броня и собственный контент на стыке магии и техники.

Дизайн-документы живут в репозитории порта:
[`Thaumcraft-4-port-to-1.12.2/docs/integration/`](https://github.com/Atom-gnomov/Thaumcraft-4-port-to-1.12.2/tree/main/docs/integration)
— начиная с `README.md` и `ic2_v5_decisions.md`. Спека текущей фазы:
`phase1_core_spec.md`.

## Состояние

**Фаза 1 (релиз 0.1)** — каркас: модульная система, EnergyCanon, аспекты
предметов IC2, вкладка Таумономикона, рецепты дробителя/компрессора/экстрактора,
алюментум-топливо. Дорожная карта фаз: `docs/integration/ic2_v5_impl_skeleton.md`
в репо порта (фазы 0.1 → 1.0).

## Сборка

Требования: JDK 8, интернет до maven.minecraftforge.net.

**Быстрый путь** — скрипт развёртывания среды (клонирует порт рядом, собирает
его dev-jar, кладёт в `libs/`, собирает сабмод):

```bat
:: Windows
set JDK8_HOME=C:\путь\к\jdk8
setup-local.bat
```
```bash
# Linux/macOS
export JDK8_HOME=/путь/к/jdk8
./setup-local.sh
```

IC2 качается автоматически через CurseMaven (пин файла
[3838713](https://www.curseforge.com/minecraft/mc-mods/industrial-craft/download/3838713)
в `build.gradle`) — руками ничего скачивать не нужно.

**Вручную:**
1. Положи в `libs/` dev-jar порта (см. `libs/README.md`):
   `Thaumcraft-<version>-dev.jar` — собирается в репо порта: `gradlew devJar`.
2. `./gradlew build` (Windows: `gradlew.bat build`) с JDK 8.
3. Готовый мод: `build/libs/UnboundTechnology-<version>-universal.jar`.

Первая сборка — момент сверки `docs/IC2_API_ASSUMPTIONS.md`: все обращения
к IC2 API писались без доступа к jar-у и проверяются компилятором.

## Зависимости в игре

| Мод | Обязателен |
|---|---|
| TC4 Unbound | да |
| IndustrialCraft 2 Experimental | да |
| METS, Advanced Solar Panels | нет — контент-модули включаются при наличии |

Модули можно принудительно выключить в `config/unboundtech.cfg`.
