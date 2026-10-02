# -*- coding: utf-8 -*-
"""Приведение иконок к ванильному стилю Minecraft.

Просьба владельца: «можно майнкрафт стиль, чтобы ИИшка выдержала».
Уговаривать модель ненадёжно — стиль навязываем механически, по
НАСТОЯЩИМ ванильным текстурам из клиента:

1. ПАЛИТРА. Собирается из ТРЁХ паков, рядом с которыми наши вещи
   лежат в инвентаре: ванильный Minecraft, Thaumcraft и IndustrialCraft
   (требование владельца: «чтобы вписывались в майнкрафт/таумкрафт/
   индастриал»). Благодаря ТК в эталоне есть фиолетовые и зелёные
   арканные тона, благодаря IC2 — холодные машинные серо-синие; наши
   сигнальные цвета больше не выпадают из палитры.
2. ЗЕРНО. У ванильных текстур шумное, «крупчатое» затенение, а не
   гладкие заливки: добавляем лёгкий детерминированный дизеринг по
   светлоте, как в оригинале.
3. ПЛОСКОСТЬ. Срезаем блики-пересветы: в ваниле нет глянца.

Палитра кэшируется в tools/materials/mc_palette.json, чтобы не лезть в
jar при каждом прогоне.
"""
import json
import os
import random
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, "materials", "mc_palette.json")
MC_DIRS = [
    os.path.expandvars(r"%APPDATA%\.minecraft\versions"),
]


def _find_jar():
    for root in MC_DIRS:
        if not os.path.isdir(root):
            continue
        for dirpath, _dirs, files in os.walk(root):
            for name in files:
                if not name.endswith(".jar"):
                    continue
                path = os.path.join(dirpath, name)
                try:
                    with zipfile.ZipFile(path) as z:
                        if any(n.startswith(
                                "assets/minecraft/textures/items/")
                                for n in z.namelist()):
                            return path
                except Exception:          # noqa: BLE001
                    continue
    return None


MODS_DIR = os.path.expandvars(r"%APPDATA%\.minecraft\mods")

# префикс ассетов -> сколько цветов берём от пака
SOURCES = (
    ("assets/minecraft/textures/items/", 64),
    ("assets/thaumcraft/textures/items/", 48),
    ("assets/ic2/textures/items/", 48),
)


def _jars():
    """Ванильный клиент + джары ТК и IC2 из папки модов."""
    found = []
    jar = _find_jar()
    if jar:
        found.append(jar)
    if os.path.isdir(MODS_DIR):
        for name in os.listdir(MODS_DIR):
            low = name.lower()
            if name.endswith(".jar") and ("thaumcraft" in low
                                          or "industrialcraft" in low):
                found.append(os.path.join(MODS_DIR, name))
    return found


def _collect(jar, prefix):
    counts = {}
    try:
        z = zipfile.ZipFile(jar)
    except Exception:              # noqa: BLE001
        return counts
    with z:
        for name in z.namelist():
            if not (name.startswith(prefix) and name.endswith(".png")):
                continue
            try:
                with z.open(name) as fh:
                    img = Image.open(fh).convert("RGBA")
                    img.load()
            except Exception:      # noqa: BLE001
                continue
            if img.height > img.width:      # анимированные ленты
                img = img.crop((0, 0, img.width, img.width))
            for r, g, b, a in img.getdata():
                if a < 128:
                    continue
                key = (r // 8 * 8, g // 8 * 8, b // 8 * 8)
                counts[key] = counts.get(key, 0) + 1
    return counts


def palette(limit=160):
    """Цвета трёх паков (ваниль + ТК + IC2), кэшируется на диск."""
    if os.path.exists(CACHE):
        return [tuple(c) for c in json.load(open(CACHE))]
    best = []
    jars = _jars()
    for prefix, take in SOURCES:
        counts = {}
        for jar in jars:
            for key, n in _collect(jar, prefix).items():
                counts[key] = counts.get(key, 0) + n
        ranked = [c for c, _n in sorted(counts.items(), key=lambda kv: -kv[1])]
        for colour in ranked[:take]:
            if colour not in best:
                best.append(colour)
    best = best[:limit]
    if not best:
        return []
    os.makedirs(os.path.dirname(CACHE), exist_ok=True)
    json.dump([list(c) for c in best], open(CACHE, "w"))
    return best


def _nearest(colour, pal):
    r, g, b = colour
    best, bestd = colour, None
    for pr, pg, pb in pal:
        # взвешиваем по восприятию: зелёный важнее синего
        d = 3 * (r - pr) ** 2 + 6 * (g - pg) ** 2 + (b - pb) ** 2
        if bestd is None or d < bestd:
            best, bestd = (pr, pg, pb), d
    return best


def apply(img, grain=0.10, seed=0, max_dist=52):
    """Ванильное зерно + УСЛОВНЫЙ снап палитры.

    Жёсткий снап к ванили убивает опознавательные цвета мода (ванильная
    палитра почти без фиолетового: флюкс превращался в красно-синий).
    Поэтому тянем к ванили только то, что и так рядом с ней — металл,
    дерево, камень; наши сигнальные цвета (флюкс, ихор, нано-зелень)
    остаются свои, им достаётся только зерно и огрубление тонов.
    """
    pal = palette()
    img = img.convert("RGBA")
    px = img.load()
    rnd = random.Random(seed or (img.width * 7919 + img.height))
    cache = {}
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a < 128:
                px[x, y] = (0, 0, 0, 0)
                continue
            if grain:
                # зерно ванили: ±N% светлоты, детерминированно
                k = 1.0 + (rnd.random() - 0.5) * 2 * grain
                r = max(0, min(255, int(r * k)))
                g = max(0, min(255, int(g * k)))
                b = max(0, min(255, int(b * k)))
            key = (r // 4, g // 4, b // 4)
            if key not in cache:
                if pal:
                    nearest = _nearest((r, g, b), pal)
                    dist = sum(abs(a - b2) for a, b2 in
                               zip((r, g, b), nearest)) / 3.0
                    cache[key] = nearest if dist <= max_dist else (
                        r // 16 * 16, g // 16 * 16, b // 16 * 16)
                else:
                    cache[key] = (r, g, b)
            nr, ng, nb = cache[key]
            px[x, y] = (nr, ng, nb, 255)
    return img


def main():
    import sys
    pal = palette()
    print("ванильных цветов в палитре:", len(pal))
    for path in sys.argv[1:]:
        out = apply(Image.open(path))
        out.save(path)
        print("  приведён", os.path.basename(path))


if __name__ == "__main__":
    main()
