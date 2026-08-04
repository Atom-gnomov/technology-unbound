#!/usr/bin/env bash
# Unbound Technology — развёртывание локальной среды (Linux/macOS).
# Запускать из корня клона technology-unbound. Требует JDK8_HOME.
set -euo pipefail

PORT_DIR="../Thaumcraft-4-port-to-1.12.2"
PORT_URL="https://github.com/Atom-gnomov/Thaumcraft-4-port-to-1.12.2.git"

if [[ -z "${JDK8_HOME:-}" || ! -x "$JDK8_HOME/bin/javac" ]]; then
    echo "[!] Задай JDK8_HOME на JDK 8 (не JRE): export JDK8_HOME=/path/to/jdk8" >&2
    exit 1
fi
echo "[ok] JDK 8: $JDK8_HOME"

if [[ ! -d "$PORT_DIR/.git" ]]; then
    echo "[..] Клонирую порт TC4U в $PORT_DIR ..."
    git clone "$PORT_URL" "$PORT_DIR"
else
    echo "[ok] Порт найден: $PORT_DIR"
fi

echo "[..] Собираю dev-jar порта..."
( cd "$PORT_DIR/mod" && ./gradlew devJar -x test \
    -Dorg.gradle.java.home="$JDK8_HOME" --console=plain )
DEVJAR=$(ls "$PORT_DIR"/mod/build/libs/Thaumcraft-*-dev.jar | head -1)
cp -f "$DEVJAR" libs/
echo "[ok] $DEVJAR -> libs/ (сверь имя с build.gradle)"

if ! ls libs/industrialcraft-2-*.jar >/dev/null 2>&1; then
    cat >&2 <<'EOM'
[!] В libs/ нет jar-а IC2. Скачай IndustrialCraft 2 Experimental (1.12.2):
      https://www.curseforge.com/minecraft/mc-mods/industrial-craft
    положи jar в libs/, сверь имя с build.gradle, затем:
      ./gradlew build -Dorg.gradle.java.home="$JDK8_HOME"
EOM
    exit 0
fi
echo "[ok] IC2 jar найден"

echo "[..] Собираю Unbound Technology..."
./gradlew build -Dorg.gradle.java.home="$JDK8_HOME" --console=plain
echo "[ok] Готово: build/libs/"
