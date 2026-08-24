# -*- coding: utf-8 -*-
"""Ассеты прототипа Нано-Таум брони — композит из TC4 и IC2.

Решение владельца: мод приватный, ассеты обоих модов используются как
материал. Файлы генерируются ИЗ УСТАНОВЛЕННЫХ jar-ов при сборке и стоят в
.gitignore — в jar мода они попадают, в публичный репозиторий нет.

Состав:
  * текстура модели на игроке — fortress_armor.png порта (развёртка той
    самой объёмной ModelFortressArmor на ~40 деталей), перекрашенная в два
    материала карточки §8.2: тёмные зоны → нано-подложка (матовый карбон),
    светлые → закалённый таумий (фиолетовый металл); самые яркие пиксели
    (руны, самоцвет) → фиолетовое свечение;
  * иконки предметов — нано-броня IC2 с врезанной таум-пластиной и
    голубой лампой (§8.1).
"""
import io
import os
import zipfile

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")

TC_JARS = [
    r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\mods\Thaumcraft-1.2.8.2-universal.jar",
    os.path.join(REPO, "libs", "Thaumcraft-1.2.8.1-dev.jar"),
]
IC2_JARS = [
    r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\mods\industrialcraft-2-2.8.222-ex112.jar",
]

# Нано-подложка: тёмный матовый карбон (§8.2 — «гладкая, без бликов»).
NANO = [(0x0E, 0x0F, 0x12), (0x16, 0x18, 0x1C), (0x1F, 0x21, 0x26),
        (0x28, 0x2B, 0x31), (0x32, 0x35, 0x3C), (0x3C, 0x40, 0x48)]
# Закалённый таумий: тёмный металл с фиолетовым отливом, кованая фактура.
TEMPER = [(0x2A, 0x22, 0x3A), (0x3C, 0x32, 0x52), (0x50, 0x44, 0x6A),
          (0x66, 0x58, 0x84), (0x7E, 0x6F, 0x9E), (0x97, 0x88, 0xB8)]
GLOW = (0xB4, 0x7A, 0xE8)     # руны/самоцвет
CYAN = (0x3F, 0xAE, 0xE8)     # лампы/провода


def find(paths):
    for p in paths:
        if os.path.isfile(p):
            return p
    return None


def load(jar, path):
    with zipfile.ZipFile(jar) as zf:
        img = Image.open(io.BytesIO(zf.read(path)))
        img.load()
    return img.convert("RGBA")


def luminance(c):
    return (c[0] * 299 + c[1] * 587 + c[2] * 114) // 1000


def dual_recolour(img, low_ramp, high_ramp, split=0.45, glow_top=0.92):
    """Тёмная часть образца → одна рампа, светлая → другая, пик → свечение.

    Порог — доля фактического диапазона яркости, а не абсолют: развёртка
    фортресс-брони тёмная сама по себе.
    """
    w, h = img.size
    sp = img.load()
    lums = [luminance(sp[x, y]) for y in range(h) for x in range(w) if sp[x, y][3] > 8]
    lo, hi = min(lums), max(lums)
    span = max(1, hi - lo)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            c = sp[x, y]
            if c[3] <= 8:
                continue
            t = (luminance(c) - lo) / span
            if t >= glow_top:
                op[x, y] = GLOW + (c[3],)
            elif t < split:
                ramp = low_ramp
                lv = int(t / split * (len(ramp) - 1) + 0.5)
                op[x, y] = ramp[lv] + (c[3],)
            else:
                ramp = high_ramp
                lv = int((t - split) / (1.0 - split) * (len(ramp) - 1) + 0.5)
                op[x, y] = ramp[lv] + (c[3],)
    return out


def put(img, x, y, colour):
    if 0 <= x < img.size[0] and 0 <= y < img.size[1] and img.load()[x, y][3] > 0:
        img.load()[x, y] = colour + (255,)


def put_any(img, x, y, colour):
    if 0 <= x < img.size[0] and 0 <= y < img.size[1]:
        img.load()[x, y] = colour + (255,)


def icon(base, plate_box, lamp):
    """Иконка: нано-база IC2, таум-пластина в указанной зоне, лампа."""
    out = dual_recolour(base, NANO, NANO, split=0.99)   # вся база — карбон
    if plate_box:
        x0, y0, x1, y1 = plate_box
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                edge = x in (x0, x1) or y in (y0, y1)
                put(out, x, y, TEMPER[1] if edge else TEMPER[3 if (x + y) % 3 else 2])
        put(out, (x0 + x1) // 2, (y0 + y1) // 2, GLOW)
    if lamp:
        put_any(out, lamp[0], lamp[1], CYAN)
    return out


def main():
    tc = find(TC_JARS)
    ic2 = find(IC2_JARS)
    if tc is None or ic2 is None:
        print("Нет jar-ов TC4/IC2 — ассеты Нано-Таум не сгенерированы.")
        return 1

    fortress = load(tc, "assets/thaumcraft/textures/models/fortress_armor.png")
    model = dual_recolour(fortress, NANO, TEMPER)
    armor_dir = os.path.join(ROOT, "textures", "models", "armor")
    os.makedirs(armor_dir, exist_ok=True)
    model.save(os.path.join(armor_dir, "nano_thaum_armor.png"))

    items_dir = os.path.join(ROOT, "textures", "items")
    parts = {
        "nano_thaum_helmet": ("nano_helmet", (4, 6, 11, 8), (12, 5)),
        "nano_thaum_chestplate": ("nano_chestplate", (5, 6, 10, 11), (12, 3)),
        "nano_thaum_leggings": ("nano_leggings", (4, 4, 11, 6), None),
        "nano_thaum_boots": ("nano_boots", (3, 9, 6, 11), (12, 10)),
    }
    for ours, (theirs, box, lamp) in parts.items():
        base = load(ic2, "assets/ic2/textures/items/armor/%s.png" % theirs)
        icon(base, box, lamp).save(os.path.join(items_dir, ours + ".png"))

    import json
    for ours in parts:
        with open(os.path.join(ROOT, "models", "item", ours + ".json"),
                  "w", encoding="utf-8") as f:
            json.dump({"parent": "item/generated",
                       "textures": {"layer0": "unboundtech:items/" + ours}}, f, indent=2)
            f.write("\n")
    print("nano-thaum: модельная текстура (fortress x nano) + 4 иконки + модели")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
