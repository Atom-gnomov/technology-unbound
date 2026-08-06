@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
REM ============================================================
REM  Unbound Technology — развёртывание локальной среды (Windows)
REM  Запускать из корня клона technology-unbound.
REM  Что делает:
REM   1) проверяет JDK 8;
REM   2) клонирует/обновляет порт TC4U в соседнюю папку;
REM   3) собирает dev-jar порта и кладёт его в libs/;
REM   4) напоминает про jar IC2 (качается вручную с CurseForge);
REM   5) собирает сабмод.
REM ============================================================

set PORT_DIR=..\Thaumcraft-4-port-to-1.12.2
set PORT_URL=https://github.com/Atom-gnomov/Thaumcraft-4-port-to-1.12.2.git

REM --- 1. JDK 8 ---
if "%JDK8_HOME%"=="" (
    echo [!] Переменная JDK8_HOME не задана.
    echo     Укажи путь к JDK 8 ^(не JRE^), например:
    echo       set JDK8_HOME=C:\tools\jdk8u492-b09
    echo     и запусти скрипт снова.
    exit /b 1
)
if not exist "%JDK8_HOME%\bin\javac.exe" (
    echo [!] %JDK8_HOME% не похож на JDK: нет bin\javac.exe
    exit /b 1
)
echo [ok] JDK 8: %JDK8_HOME%

REM --- 2. Порт TC4U рядом ---
if not exist "%PORT_DIR%\.git" (
    echo [..] Клонирую порт TC4U в %PORT_DIR% ...
    git clone %PORT_URL% "%PORT_DIR%" || exit /b 1
) else (
    echo [ok] Порт найден: %PORT_DIR% ^(git pull делай сам при необходимости^)
)

REM --- 3. dev-jar порта ---
echo [..] Собираю dev-jar порта...
pushd "%PORT_DIR%\mod"
call gradlew.bat devJar -x test -Dorg.gradle.java.home="%JDK8_HOME%" --console=plain || (popd & exit /b 1)
popd
for %%F in ("%PORT_DIR%\mod\build\libs\Thaumcraft-*-dev.jar") do set DEVJAR=%%F
if "!DEVJAR!"=="" (
    echo [!] dev-jar не найден в %PORT_DIR%\mod\build\libs
    exit /b 1
)
copy /y "!DEVJAR!" libs\ >nul
echo [ok] !DEVJAR! -^> libs\
echo     Проверь, что имя в build.gradle ^(compile name: 'Thaumcraft-...-dev'^)
echo     совпадает с версией скопированного jar.

REM --- 4. IC2 качается автоматически (CurseMaven, см. build.gradle) ---

REM --- 5. Сборка сабмода ---
echo [..] Собираю Unbound Technology...
call gradlew.bat build -Dorg.gradle.java.home="%JDK8_HOME%" --console=plain || exit /b 1
echo [ok] Готово: build\libs\
endlocal
