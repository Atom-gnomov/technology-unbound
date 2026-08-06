# libs/

Сюда кладутся jar-зависимости, которых нет в публичных мавенах:

| Файл | Откуда |
|---|---|
| `Thaumcraft-1.2.8.1-dev.jar` | репо порта TC4U: `gradlew devJar` → `mod/build/libs/` (1.2.8.1 пока только на ветке `claude/ic2-thaumcraft-integration-agwnn4`) |

IC2 больше НЕ нужен в libs/ — он подключён через CurseMaven
(`deobfCompile 'curse.maven:industrial-craft-242638:3838713'` в build.gradle,
файл выбран владельцем мода) и скачивается Gradle-ом автоматически.

Имена файлов должны совпадать со строками `compile name: '...'` в `build.gradle`
(без расширения `.jar`). Версии можно поднять — синхронно в обоих местах.

Jar-ы намеренно в .gitignore — в репозиторий не коммитятся.
