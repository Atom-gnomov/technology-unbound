# -*- coding: utf-8 -*-
"""Текстуры Квант-Гибридной брони (оба пути) — по ТЗ адверсарного
воркфлоу (docs/concepts/quantum_hybrid_armour_model.md) и пайплайну
нано-таума (память armor-pipeline-approach):

 - основа бипед-зон — РЕАЛЬНЫЕ quantum_1/quantum_2 IC2, тонированные
   в путь (А: пустотный фиолет, Б: тёплое золото); дыры глушатся
   плетением — броня полнотелая;
 - развёртки всех ~30 кастомных деталей по UV-таблице модели, свободные
   зоны своей плоскости 128x64 (x>=64 или y>=32);
 - путь А: матовые тёмно-фиолетовые пластины + ЛАТУННЫЕ акценты,
   провода в чёрной оплётке; путь Б: полированное золото + СТАЛЬНЫЕ
   акценты, провода золотые (латунь не используется вовсе);
 - эмиссив: 8 кадров на путь (quant_*_glow_0..7.png) — прозрачные
   файлы, закрашены ТОЛЬКО развёртки светящихся деталей; А — статичный
   лиловый + «дышащий» самоцвет, Б — общий золотой пульс;
 - иконки: родные quantum-иконки IC2 в тоне пути.
"""
import io
import json
import os
import zipfile

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")
IC2_JAR = r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\mods\industrialcraft-2-2.8.222-ex112.jar"

S = 2   # 2 px на юнит: файл 256x128, плоскость 128x64

STEEL = [(0x1B, 0x1A, 0x22), (0x2E, 0x2C, 0x36), (0x42, 0x3F, 0x4C),
         (0x5A, 0x58, 0x66), (0x6E, 0x6B, 0x79)]
VOID_PLATE = [(0x24, 0x13, 0x33), (0x2F, 0x1A, 0x44), (0x41, 0x22, 0x60),
              (0x4E, 0x2C, 0x70)]
BRASS = [(0x2E, 0x24, 0x0C), (0x50, 0x3E, 0x14), (0x74, 0x59, 0x1E),
         (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52)]
GOLD = [(0x6E, 0x50, 0x16), (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C),
        (0xD0, 0xAC, 0x52), (0xE8, 0xC0, 0x60), (0xF0, 0xDF, 0xA0)]
WOODCLOTH = [(0x2A, 0x1E, 0x14), (0x3E, 0x2A, 0x18), (0x5C, 0x40, 0x24)]
CABLE_BLACK = [(0x10, 0x10, 0x14), (0x1C, 0x1C, 0x22)]
LILAC = (0xC4, 0x92, 0xE8)
BLUE = (0x7F, 0xD4, 0xFF)
GOLD_GLOW = (0xFF, 0xD8, 0x73)
GOLD_CORE = (0xFF, 0xF2, 0xC8)

# UV-таблица деталей: имя -> (u, v, w, h, d, роль)
# роли: plate (пластина пути), accent (латунь А / сталь Б), steel (сталь
# обоих), cable (оплётка А / золото Б), cloth (ткань), glow (светится),
# rune (пластина + рунные пиксели), seal (пломба ТК), book (гримуар)
UV = {
    "crown_f":   (64, 0, 4, 3, 1, "plate"),
    "crown_s":   (80, 0, 1, 3, 4, "plate"),
    "gem":       (64, 4, 2, 2, 1, "glow"),
    "rune":      (72, 4, 1, 2, 5, "rune"),
    "socket":    (64, 8, 1, 2, 2, "steel"),
    "cable":     (72, 12, 1, 4, 2, "cable"),
    "collar":    (80, 12, 2, 1, 1, "steel"),
    "chest":     (64, 20, 8, 7, 1, "plate"),
    "chain":     (84, 14, 1, 3, 1, "accent"),
    "medallion": (90, 14, 2, 3, 1, "glow"),
    "lamp":      (98, 14, 1, 1, 2, "glow"),
    "seal":      (106, 14, 1, 4, 1, "seal"),
    "belt":      (64, 28, 8, 2, 4, "plate"),
    "fin":       (0, 32, 1, 6, 1, "steel"),
    "backcable": (6, 32, 1, 7, 1, "cable"),
    "pauld_top": (12, 32, 6, 3, 5, "plate"),
    "pauld_low": (12, 42, 5, 2, 4, "plate"),
    "slit":      (12, 50, 5, 1, 4, "glow"),
    "bracer":    (36, 32, 4, 4, 4, "plate"),
    "crystal":   (54, 32, 1, 1, 1, "glow"),
    "glovecable": (58, 32, 1, 2, 1, "cable"),
    "thigh":     (36, 42, 4, 4, 1, "plate"),
    "pouch":     (48, 42, 2, 3, 1, "cloth"),
    "hipchain":  (56, 42, 1, 2, 1, "accent"),
    "hipmed":    (36, 48, 2, 2, 1, "glow"),
    "sealleg":   (44, 48, 1, 4, 1, "seal"),
    "book":      (50, 48, 1, 3, 3, "book"),
    "clasp":     (60, 48, 1, 1, 1, "accent"),
    "sabaton":   (0, 40, 5, 3, 5, "plate"),
    "shin":      (0, 50, 3, 3, 1, "plate"),
    "nozzle":    (10, 56, 2, 1, 2, "steel"),
    "heel":      (20, 56, 2, 2, 1, "steel"),
    "cape_u":    (88, 28, 8, 6, 1, "cloth"),
    "cape_l":    (88, 36, 6, 5, 1, "cloth"),
}

# какие развёртки светятся (и у какого пути)
GLOW_PARTS = {
    "gem": "both", "medallion": "both", "lamp": "both", "slit": "both",
    "crystal": "both", "hipmed": "both", "nozzle": "ichor",
}


def dev_rect(u, v, w, h, d):
    return (u * S, v * S, 2 * (w + d) * S, (d + h) * S)


def load_ic2(path):
    with zipfile.ZipFile(IC2_JAR) as z:
        img = Image.open(io.BytesIO(z.read(path)))
        img.load()
    return img.convert("RGBA")


def lum(c):
    return (c[0] * 299 + c[1] * 587 + c[2] * 114) // 1000


def tint(img, ramp):
    """Перекраска по яркости в рампу пути."""
    px = img.load()
    w, h = img.size
    lo, hi = 255, 0
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] > 8:
                l = lum(c)
                lo, hi = min(lo, l), max(hi, l)
    span = max(1, hi - lo)
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] > 8:
                level = (lum(c) - lo) * (len(ramp) - 1) // span
                px[x, y] = ramp[level] + (c[3],)
    return img


def fill_rect(px, rect, colour, alpha=255):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = colour + (alpha,)


def pattern_rect(px, rect, ramp, seed=0):
    x0, y0, w, h = rect
    n = len(ramp)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            i = n // 2
            if (x * 7 + y * 13 + seed) % 11 == 0:
                i = min(n - 1, i + 1)
            if (x + y + seed) % 9 == 0:
                i = max(0, i - 1)
            if y == y0 or x == x0:
                i = min(n - 1, i + 1)
            if y == y0 + h - 1:
                i = max(0, i - 1)
            px[x, y] = ramp[i] + (255,)


def weave(px, zone_px, ramp):
    x0, y0, x1, y1 = zone_px
    for y in range(y0, y1):
        for x in range(x0, x1):
            if px[x, y][3] <= 128:
                i = 1
                if (x // 2 * 3 + y // 2 * 5) % 11 == 0:
                    i = 2
                if (x // 2 + y // 2) % 3 == 0:
                    i = 0
                px[x, y] = ramp[i] + (255,)


def base_texture(path_key):
    """Базовый файл пути: quantum-бипед + все кастомные развёртки."""
    void = path_key == "void"
    plate = VOID_PLATE if void else GOLD
    accent = BRASS if void else STEEL
    cable = CABLE_BLACK if void else GOLD[2:5]
    canvas = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))

    # бипед-зоны: реальные quantum-слои IC2, тонированные в путь
    q1 = tint(load_ic2("assets/ic2/textures/armor/quantum_1.png"),
              plate if void else GOLD)
    q2 = tint(load_ic2("assets/ic2/textures/armor/quantum_2.png"),
              plate if void else GOLD)
    for src in (q1, q2):
        big = src.resize((src.size[0] * 2, src.size[1] * 2), Image.NEAREST)
        bp = big.load()
        cp = canvas.load()
        for y in range(min(big.size[1], canvas.size[1])):
            for x in range(min(big.size[0], canvas.size[0])):
                c = bp[x, y]
                if c[3] > 8:
                    cp[x, y] = c
    px = canvas.load()
    # полнотелость: дыры бипед-зон глушим плетением пути
    for zone in ((0, 0, 64 * S, 32 * S), (0, 32 * S // 2, 0, 0)):
        pass
    weave(px, (0, 0, 128, 64), plate[:3] if void else GOLD[:3])

    # кастомные развёртки по UV-таблице
    for name, (u, v, w, h, d, role) in UV.items():
        rect = dev_rect(u, v, w, h, d)
        if role == "plate":
            pattern_rect(px, rect, plate, seed=u)
        elif role == "accent":
            pattern_rect(px, rect, accent, seed=u)
        elif role == "steel":
            pattern_rect(px, rect, STEEL, seed=u)
        elif role == "cable":
            pattern_rect(px, rect, cable, seed=u)
        elif role == "cloth":
            pattern_rect(px, rect, WOODCLOTH if not name.startswith("cape")
                         else ([(0x14, 0x10, 0x1E), (0x1E, 0x16, 0x2E),
                                (0x2A, 0x1E, 0x40)] if void else GOLD[:3]),
                         seed=u)
        elif role == "rune":
            pattern_rect(px, rect, plate, seed=u)
            x0, y0, wd, ht = rect
            for k in range(0, wd, 4):
                px[x0 + k, y0 + ht // 2] = (accent[-1] + (255,))
        elif role == "seal":
            pattern_rect(px, rect, accent, seed=u)
            x0, y0, wd, ht = rect
            for k in range(1, ht - 1, 3):
                px[x0 + wd // 2, y0 + k] = ((0x14, 0x11, 0x1A, 255))
        elif role == "book":
            pattern_rect(px, rect, [(0x3A, 0x2A, 0x4E), (0x4E, 0x38, 0x66),
                                    (0x62, 0x48, 0x80)] if void
                         else [(0x5C, 0x40, 0x24), (0x6A, 0x4B, 0x2B),
                               (0x7A, 0x57, 0x33)], seed=u)
        elif role == "glow":
            # в базе — тёмный корпус под свечение
            pattern_rect(px, rect, [(0x14, 0x11, 0x1A), (0x1E, 0x18, 0x26)],
                         seed=u)

    # рваный край плаща — альфа-зубцы по нижней кромке
    for cape in ("cape_u", "cape_l"):
        u, v, w, h, d, _ = UV[cape]
        x0, y0, wd, ht = dev_rect(u, v, w, h, d)
        for x in range(x0, x0 + wd):
            depth = (x * 7 + v) % 4
            for k in range(depth):
                px[x, y0 + ht - 1 - k] = (0, 0, 0, 0)
    return canvas


def glow_frames(path_key):
    """8 кадров эмиссива: прозрачно всё, кроме светящихся развёрток."""
    void = path_key == "void"
    frames = []
    for f in range(8):
        img = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))
        px = img.load()
        import math
        pulse = 0.7 + 0.3 * math.sin(math.pi * 2 * f / 8)
        breath = 0.55 + 0.45 * math.sin(math.pi * 2 * f / 8)
        for name, mode in GLOW_PARTS.items():
            if mode == "ichor" and void:
                continue
            u, v, w, h, d, _ = UV[name]
            rect = dev_rect(u, v, w, h, d)
            if void:
                # А: статика; «дышит» только самоцвет
                k = breath if name == "gem" else 1.0
                colour = BLUE if name == "lamp" else LILAC
                alpha = 153 if name == "crystal" else 255
            else:
                # Б: общий живой пульс; ядро самоцвета — бело-золотое
                k = pulse
                colour = GOLD_CORE if name == "gem" else GOLD_GLOW
                alpha = 153 if name == "crystal" else 255
            c = tuple(int(v0 * k) for v0 in colour)
            fill_rect(px, rect, c, alpha)
        frames.append(img)
    return frames


def icons(path_key):
    void = path_key == "void"
    ramp = VOID_PLATE if void else GOLD
    out = {}
    for slot in ("helmet", "chestplate", "leggings", "boots"):
        icon = tint(load_ic2(
            "assets/ic2/textures/items/armor/quantum_%s.png" % slot), ramp)
        px = icon.load()
        # искра пути на иконке
        c = (LILAC if void else GOLD_GLOW) + (255,)
        px[8, 4 if slot == "helmet" else 7] = c
        out["quant_%s_%s" % (path_key, slot)] = icon
    return out


def write_json(path, data):
    with io.open(path, "w", encoding="utf-8") as f:
        f.write(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def main():
    armor_dir = os.path.join(ROOT, "textures", "models", "armor")
    items_dir = os.path.join(ROOT, "textures", "items")
    models_dir = os.path.join(ROOT, "models", "item")
    os.makedirs(armor_dir, exist_ok=True)
    for path_key in ("void", "ichor"):
        base_texture(path_key).save(
            os.path.join(armor_dir, "quant_%s.png" % path_key))
        for f, frame in enumerate(glow_frames(path_key)):
            frame.save(os.path.join(
                armor_dir, "quant_%s_glow_%d.png" % (path_key, f)))
        for name, icon in icons(path_key).items():
            icon.save(os.path.join(items_dir, name + ".png"))
            write_json(os.path.join(models_dir, name + ".json"),
                       {"parent": "item/generated",
                        "textures": {"layer0": "unboundtech:items/" + name}})
    print("квант-гибрид: базы обоих путей, 16 кадров эмиссива, 8 иконок")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
