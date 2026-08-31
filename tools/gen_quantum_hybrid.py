# -*- coding: utf-8 -*-
"""Текстуры Квант-Гибридной брони v2 — после разноса владельца
(«лишена стиля, детали не заметны, контрастов нет»):

 - ПОФЕЙСОВАЯ отрисовка каждого бокса: рамка-кант, заливка, фаска-блик
   сверху-слева, тень снизу-справа, заклёпки на пластинах — а не плоский
   шум по всей развёртке;
 - КОНТРАСТ трёх уровней: тёмный поддоспешник (узкая нижняя рампа) ↔
   пластины (широкая рампа со светлыми фасками) ↔ яркие акценты
   (латунь у Пустотного, сталь у Ихорного);
 - спецдетали руками: фасеточный самоцвет, читаемые руны-глифы,
   пломбы ТК с строками, гримуар с корешком и застёжкой, плащ со
   складками и рваным краем;
 - бипед-зоны — реальные quantum_1/quantum_2 IC2 в ТЁМНОЙ рампе
   поддоспешника (структура тени сохраняется перекраской по яркости).
"""
import io
import json
import math
import os
import zipfile

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")
IC2_JAR = r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\mods\industrialcraft-2-2.8.222-ex112.jar"

S = 2   # 2 px на юнит: файл 256x128, плоскость 128x64

# --- рампы: поддоспешник НАМЕРЕННО тёмный, пластины — широкие ---
VOID_SUIT = [(0x0E, 0x0A, 0x16), (0x16, 0x10, 0x20), (0x20, 0x16, 0x2E),
             (0x2A, 0x1E, 0x3C)]
VOID_PLATE = [(0x1A, 0x0F, 0x28), (0x2C, 0x18, 0x44), (0x41, 0x22, 0x60),
              (0x57, 0x33, 0x7E), (0x75, 0x48, 0xA4), (0x9A, 0x6C, 0xCB)]
ICHOR_SUIT = [(0x1A, 0x13, 0x0C), (0x28, 0x1E, 0x12), (0x3A, 0x2C, 0x1A),
              (0x4C, 0x3A, 0x24)]
GOLD = [(0x6E, 0x50, 0x16), (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C),
        (0xD0, 0xAC, 0x52), (0xE8, 0xC0, 0x60), (0xF0, 0xDF, 0xA0)]
BRASS = [(0x2E, 0x24, 0x0C), (0x50, 0x3E, 0x14), (0x74, 0x59, 0x1E),
         (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52)]
STEEL = [(0x14, 0x13, 0x1A), (0x24, 0x22, 0x2C), (0x3A, 0x38, 0x46),
         (0x52, 0x50, 0x60), (0x70, 0x6D, 0x7E), (0x92, 0x8F, 0xA0)]
CLOTH_VOID = [(0x0E, 0x0A, 0x18), (0x18, 0x11, 0x28), (0x24, 0x1A, 0x3A),
              (0x30, 0x24, 0x4C)]
CLOTH_GOLD = [(0x4A, 0x36, 0x14), (0x64, 0x4A, 0x1E), (0x80, 0x60, 0x2A),
              (0x9C, 0x78, 0x38)]
BOOK_VOID = [(0x2E, 0x20, 0x42), (0x42, 0x2E, 0x5C), (0x58, 0x40, 0x78)]
BOOK_GOLD = [(0x4E, 0x36, 0x1E), (0x64, 0x46, 0x28), (0x7A, 0x57, 0x33)]
DARK_HOUSING = [(0x0C, 0x0A, 0x10), (0x16, 0x13, 0x1C), (0x22, 0x1E, 0x2C)]
LILAC = (0xC4, 0x92, 0xE8)
BLUE = (0x7F, 0xD4, 0xFF)
GOLD_GLOW = (0xFF, 0xD8, 0x73)
GOLD_CORE = (0xFF, 0xF2, 0xC8)

UV = {
    "crown_f":    (64, 0, 4, 3, 1, "plate"),
    "crown_s":    (80, 0, 1, 3, 4, "plate"),
    "gem":        (64, 4, 2, 2, 1, "gem"),
    "rune":       (72, 4, 1, 2, 5, "rune"),
    "socket":     (64, 8, 1, 2, 2, "steel"),
    "cable":      (72, 12, 1, 4, 2, "cable"),
    "collar":     (80, 12, 2, 1, 1, "steel"),
    "chest":      (64, 20, 8, 7, 1, "plate"),
    "chain":      (84, 14, 1, 3, 1, "accent"),
    "medallion":  (90, 14, 2, 3, 1, "housing"),
    "lamp":       (98, 14, 1, 1, 2, "housing"),
    "seal":       (106, 14, 1, 4, 1, "seal"),
    "belt":       (64, 28, 8, 2, 4, "plate"),
    "fin":        (0, 32, 1, 6, 1, "steel"),
    "backcable":  (6, 32, 1, 7, 1, "cable"),
    "pauld_top":  (12, 32, 6, 3, 5, "plate"),
    "pauld_low":  (12, 42, 5, 2, 4, "plate"),
    "slit":       (12, 50, 5, 1, 4, "housing"),
    "bracer":     (36, 32, 4, 4, 4, "plate"),
    "crystal":    (54, 32, 1, 1, 1, "housing"),
    "glovecable": (58, 32, 1, 2, 1, "cable"),
    "thigh":      (36, 42, 4, 4, 1, "plate"),
    "pouch":      (48, 42, 2, 3, 1, "cloth"),
    "hipchain":   (56, 42, 1, 2, 1, "accent"),
    "hipmed":     (36, 48, 2, 2, 1, "housing"),
    "sealleg":    (44, 48, 1, 4, 1, "seal"),
    "book":       (50, 48, 1, 3, 3, "book"),
    "clasp":      (60, 48, 1, 1, 1, "accent"),
    "sabaton":    (72, 52, 5, 3, 5, "plate"),
    "hood":       (96, 0, 2, 3, 9, "plate"),
    "shin":       (0, 50, 3, 3, 1, "plate"),
    "nozzle":     (10, 56, 2, 1, 2, "steel"),
    "heel":       (20, 56, 2, 2, 1, "steel"),
    "cape_u":     (88, 28, 8, 6, 1, "cape"),
    "cape_l":     (88, 36, 6, 5, 1, "cape"),
}

GLOW_PARTS = {
    "gem": "both", "medallion": "both", "lamp": "both", "slit": "both",
    "crystal": "both", "hipmed": "both", "nozzle": "ichor",
}


def faces_px(u, v, w, h, d):
    """Шесть граней развёртки бокса, в пикселях файла."""
    def r(a, b, c, e):
        return (a * S, b * S, c * S, e * S)
    return {
        "top": r(u + d, v, w, d),
        "bottom": r(u + d + w, v, w, d),
        "right": r(u, v + d, d, h),
        "front": r(u + d, v + d, w, h),
        "left": r(u + d + w, v + d, d, h),
        "back": r(u + 2 * d + w, v + d, w, h),
    }


def load_ic2(path):
    with zipfile.ZipFile(IC2_JAR) as z:
        img = Image.open(io.BytesIO(z.read(path)))
        img.load()
    return img.convert("RGBA")


def lum(c):
    return (c[0] * 299 + c[1] * 587 + c[2] * 114) // 1000


def tint(img, ramp):
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


def face_plate(px, rect, ramp, rivets=False):
    """Пластина: кант, заливка, фаска сверху-слева, тень снизу-справа."""
    x0, y0, w, h = rect
    if w <= 0 or h <= 0:
        return
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge_l = x == x0
            edge_t = y == y0
            edge_r = x == x0 + w - 1
            edge_b = y == y0 + h - 1
            if edge_l or edge_t or edge_r or edge_b:
                c = ramp[0]                      # тёмный кант
            elif x == x0 + 1 or y == y0 + 1:
                c = ramp[min(len(ramp) - 1, 4)]  # фаска-блик
            elif x == x0 + w - 2 or y == y0 + h - 2:
                c = ramp[1]                      # внутренняя тень
            else:
                c = ramp[2] if (x + y) % 5 else ramp[3]
            px[x, y] = c + (255,)
    if rivets and w >= 6 and h >= 6:
        hi = ramp[min(len(ramp) - 1, 5)]
        for (rx, ry) in ((x0 + 1, y0 + 1), (x0 + w - 2, y0 + 1),
                         (x0 + 1, y0 + h - 2), (x0 + w - 2, y0 + h - 2)):
            px[rx, ry] = hi + (255,)


def face_flat(px, rect, ramp):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            i = 1 if (x + y) % 3 else 2
            if y == y0:
                i = min(len(ramp) - 1, i + 1)
            if y == y0 + h - 1:
                i = 0
            px[x, y] = ramp[min(i, len(ramp) - 1)] + (255,)


def paint_box(px, name, u, v, w, h, d, role, void):
    plate = VOID_PLATE if void else GOLD
    accent = BRASS if void else STEEL
    fs = faces_px(u, v, w, h, d)
    if role == "plate":
        for f, rect in fs.items():
            if f in ("front", "top", "back"):
                face_plate(px, rect, plate, rivets=True)
            elif f == "bottom":
                face_flat(px, rect, plate[:3])
            else:
                face_plate(px, rect, plate)
    elif role == "accent":
        for rect in fs.values():
            face_flat(px, rect, accent[2:])
    elif role == "steel":
        for f, rect in fs.items():
            face_plate(px, rect, STEEL) if f == "front" else face_flat(px, rect, STEEL[1:5])
    elif role == "cable":
        ramp = CABLE_RAMP_VOID if void else GOLD[1:5]
        for rect in fs.values():
            face_flat(px, rect, ramp)
    elif role == "cloth":
        ramp = CLOTH_VOID if void else CLOTH_GOLD
        for rect in fs.values():
            face_flat(px, rect, ramp)
    elif role == "cape":
        ramp = CLOTH_VOID if void else CLOTH_GOLD
        for f, rect in fs.items():
            x0, y0, wd, ht = rect
            for y in range(y0, y0 + ht):
                for x in range(x0, x0 + wd):
                    i = 1 + ((x // 2) % 2)          # вертикальные складки
                    if (x + y * 3) % 11 == 0:
                        i = 0
                    px[x, y] = ramp[i] + (255,)
        # рваный низ — альфа-зубцы
        x0, y0, wd, ht = fs["front"]
        for x in range(x0, x0 + wd):
            for k in range((x * 7 + v) % 4):
                px[x, y0 + ht - 1 - k] = (0, 0, 0, 0)
    elif role == "gem":
        # фасеточный самоцвет: яркий крест, тёмные углы, оправа-кант
        core = LILAC if void else GOLD_CORE
        dark = VOID_PLATE[0] if void else GOLD[0]
        for f, rect in fs.items():
            x0, y0, wd, ht = rect
            for y in range(y0, y0 + ht):
                for x in range(x0, x0 + wd):
                    if x in (x0, x0 + wd - 1) or y in (y0, y0 + ht - 1):
                        c = (BRASS if void else STEEL)[3]
                    else:
                        cx = abs((x - x0) - wd / 2 + 0.5)
                        cy = abs((y - y0) - ht / 2 + 0.5)
                        c = core if cx + cy < max(wd, ht) / 2.5 else dark
                    px[x, y] = c + (255,)
    elif role == "rune":
        for f, rect in fs.items():
            face_plate(px, rect, plate)
        # глифы контрастным акцентом на длинных гранях
        for f in ("right", "left", "front", "back"):
            x0, y0, wd, ht = fs[f]
            for k in range(x0 + 1, x0 + wd - 1, 3):
                px[k, y0 + ht // 2] = accent[5 if void else 4] + (255,)
                if ht > 3:
                    px[k, y0 + ht // 2 - 1] = accent[3] + (255,)
    elif role == "seal":
        for f, rect in fs.items():
            face_flat(px, rect, accent[1:5])
        x0, y0, wd, ht = fs["front"]
        for k in range(y0 + 1, y0 + ht - 1, 2):
            for xx in range(x0, x0 + wd):
                px[xx, k] = ((0x14, 0x11, 0x1A, 255))
    elif role == "book":
        ramp = BOOK_VOID if void else BOOK_GOLD
        for f, rect in fs.items():
            face_plate(px, rect, [ramp[0], ramp[0], ramp[1], ramp[2],
                                  ramp[2], ramp[2]])
        # корешок и эмблема
        x0, y0, wd, ht = fs["front"]
        for y in range(y0, y0 + ht):
            px[x0, y] = accent[4] + (255,)
        if wd >= 4 and ht >= 4:
            px[x0 + wd // 2, y0 + ht // 2] = (LILAC if void else GOLD_GLOW) + (255,)
    elif role == "housing":
        for rect in fs.values():
            face_flat(px, rect, DARK_HOUSING)


CABLE_RAMP_VOID = [(0x0C, 0x0C, 0x10), (0x16, 0x16, 0x1E), (0x20, 0x20, 0x2A)]


def weave(px, ramp):
    """Полнотелость бипед-зон: глушим прозрачное плетением."""
    for y in range(0, 32 * S):
        for x in range(0, 64 * S):
            if px[x, y][3] <= 128:
                i = 1
                if (x // 2 * 3 + y // 2 * 5) % 11 == 0:
                    i = 2
                if (x // 2 + y // 2) % 3 == 0:
                    i = 0
                px[x, y] = ramp[i] + (255,)


def base_texture(path_key):
    void = path_key == "void"
    suit = VOID_SUIT if void else ICHOR_SUIT
    canvas = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))
    # бипед-зоны: quantum-слои IC2 в ТЁМНОЙ рампе поддоспешника —
    # пластины поверх обязаны читаться светлее
    q1 = tint(load_ic2("assets/ic2/textures/armor/quantum_1.png"), suit)
    q2 = tint(load_ic2("assets/ic2/textures/armor/quantum_2.png"), suit)
    cp = canvas.load()
    for src in (q1, q2):
        big = src.resize((src.size[0] * 2, src.size[1] * 2), Image.NEAREST)
        bp = big.load()
        for y in range(min(big.size[1], canvas.size[1])):
            for x in range(min(big.size[0], canvas.size[0])):
                c = bp[x, y]
                if c[3] > 8:
                    cp[x, y] = c
    weave(cp, suit[:3])
    for name, (u, v, w, h, d, role) in UV.items():
        paint_box(cp, name, u, v, w, h, d, role, void)
    return canvas


def glow_frames(path_key):
    void = path_key == "void"
    frames = []
    for f in range(8):
        img = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))
        px = img.load()
        pulse = 0.7 + 0.3 * math.sin(math.pi * 2 * f / 8)
        breath = 0.55 + 0.45 * math.sin(math.pi * 2 * f / 8)
        for name, mode in GLOW_PARTS.items():
            if mode == "ichor" and void:
                continue
            u, v, w, h, d, _ = UV[name]
            for face, rect in faces_px(u, v, w, h, d).items():
                if void:
                    k = breath if name == "gem" else 1.0
                    colour = BLUE if name == "lamp" else LILAC
                else:
                    k = pulse
                    colour = GOLD_CORE if name == "gem" else GOLD_GLOW
                alpha = 153 if name == "crystal" else 255
                c = tuple(int(v0 * k) for v0 in colour)
                x0, y0, wd, ht = rect
                for y in range(y0, y0 + ht):
                    for x in range(x0, x0 + wd):
                        # свечение с живым центром: край чуть темнее
                        edge = x in (x0, x0 + wd - 1) or y in (y0, y0 + ht - 1)
                        cc = tuple(int(ci * (0.8 if edge else 1.0)) for ci in c)
                        px[x, y] = cc + (alpha,)
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
    os.makedirs(items_dir, exist_ok=True)
    os.makedirs(models_dir, exist_ok=True)
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
    print("квант-гибрид v2: пофейсовая отрисовка, контраст, фасеточный самоцвет")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
