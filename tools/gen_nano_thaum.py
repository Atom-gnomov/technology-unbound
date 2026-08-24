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
NV_GREEN = (0x4C, 0xF2, 0x6E) # линзы ПНВ
BRASS = [(0x50, 0x3E, 0x14), (0x96, 0x74, 0x2A), (0xD0, 0xAC, 0x52)]


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


# ---- патчи UV для боксов ModelNanoThaumArmor -------------------------------
# Координаты в UV-единицах модели (128x128); текстура вдвое плотнее (256x256),
# поэтому при рисовании всё умножается на 2. Раскладка бокса стандартная:
# top(u+d,v) bottom(u+d+w,v) right(u,v+d) front(u+d,v+d) left(u+d+w,v+d)
# back(u+2d+w,v+d).

def _fill(img, x, y, w, h, colour):
    px = img.load()
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px[xx, yy] = colour + (255,)


def box_patch(img, u, v, w, h, d, body, front=None, top=None):
    """Красит развёртку бокса: корпус + отдельные цвета морды и крышки."""
    S = 2   # плотность текстуры к UV
    fu, fv = (u + d) * S, (v + d) * S
    _fill(img, u * S, v * S, 2 * (d + w) * S, (d + h) * S, body)
    if top is not None:
        _fill(img, (u + d) * S, v * S, w * S, d * S, top)
    if front is not None:
        _fill(img, fu, fv, w * S, h * S, front)


def paint_addon_patches(canvas, nano_tone):
    """Свободные дыры развёртки 128x64 — зоны новых боксов модели.

    Координаты согласованы с ModelNanoThaumArmor и найдены сканом альфы
    оригинала: goggles (0,0)/(8,0), bridge (43,0), chestCable (16,0),
    armCable (120,0), pouch (22,3), pauldron (0,8), sabaton (78,12)."""
    # очки ПНВ: корпус-оправа, зелёные линзы, латунная перемычка
    box_patch(canvas, 0, 0, 2, 2, 1, NANO[1], front=NV_GREEN, top=BRASS[1])
    box_patch(canvas, 8, 0, 2, 2, 1, NANO[1], front=NV_GREEN, top=BRASS[1])
    box_patch(canvas, 43, 0, 3, 1, 1, BRASS[1], front=BRASS[2])
    # жгут груди и кабель руки: тёмный кожух, голубая жила
    box_patch(canvas, 16, 0, 1, 5, 1, NANO[1], front=CYAN)
    box_patch(canvas, 120, 0, 1, 4, 1, NANO[1], front=CYAN)
    # подсумок: карбон с латунной клипсой
    box_patch(canvas, 22, 3, 2, 2, 1, NANO[3], front=NANO[2], top=BRASS[1])
    # паулдрон: таум-пластина, латунная крышка, руна-точка на морде
    box_patch(canvas, 0, 8, 5, 3, 5, TEMPER[2], front=TEMPER[3], top=BRASS[1])
    _fill(canvas, (0 + 5 + 2) * 2, (8 + 5 + 1) * 2, 2, 2, GLOW)
    # сабатон: нано-подложка (тон снят с nano_1 IC2), таум-морда, латунный носок
    box_patch(canvas, 78, 12, 5, 3, 5, nano_tone, front=TEMPER[2], top=TEMPER[1])
    _fill(canvas, (78 + 5) * 2, (12 + 5 + 2) * 2, 5 * 2, 1 * 2, BRASS[1])


def main():
    tc = find(TC_JARS)
    ic2 = find(IC2_JARS)
    if tc is None or ic2 is None:
        print("Нет jar-ов TC4/IC2 — ассеты Нано-Таум не сгенерированы.")
        return 1

    fortress = load(tc, "assets/thaumcraft/textures/models/fortress_armor.png")
    recoloured = dual_recolour(fortress, NANO, TEMPER)
    # Файл остаётся 256x128: UV нормализованы к плоскости 128x64, менять
    # размер холста нельзя — старые боксы растянут доли на весь файл.
    # Патчи новых боксов кладутся в СВОБОДНЫЕ дыры развёртки (скан альфы).
    canvas = recoloured
    nano_layer = load(ic2, "assets/ic2/textures/armor/nano_1.png")
    nl = nano_layer.load()
    nano_tone = None
    for y in range(nano_layer.size[1]):
        for x in range(nano_layer.size[0]):
            c = nl[x, y]
            if c[3] > 200:
                nano_tone = (c[0], c[1], c[2])
                break
        if nano_tone:
            break
    if nano_tone is None:
        nano_tone = NANO[2]
    paint_addon_patches(canvas, nano_tone)
    armor_dir = os.path.join(ROOT, "textures", "models", "armor")
    os.makedirs(armor_dir, exist_ok=True)
    canvas.save(os.path.join(armor_dir, "nano_thaum_armor.png"))

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
