# libs/

Сюда кладутся jar-зависимости, которых нет в публичных мавенах:

| Файл | Откуда |
|---|---|
| `Thaumcraft-1.2.8.0-dev.jar` | репо порта TC4U: `gradlew devJar` → `mod/build/libs/` |
| `industrialcraft-2-2.8.222-ex112.jar` | CurseForge, страница Industrial Craft (файл для 1.12.2) |

Имена файлов должны совпадать со строками `compile name: '...'` в `build.gradle`
(без расширения `.jar`). Версии можно поднять — синхронно в обоих местах.

Jar-ы намеренно в .gitignore — в репозиторий не коммитятся.
