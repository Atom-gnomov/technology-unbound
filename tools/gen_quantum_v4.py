# -*- coding: utf-8 -*-
"""Квант-броня v4 — редизайн по директиве владельца: два пути на РАЗНЫХ
базах с разными силуэтами.

Путь Б (Ихор, ModelQuantIchorArmour): подкладка — настоящий квант IC2
(перекраска v3, жилы сохранены), поверх — робы/шаровары/шапка из ткани
Тинкерера (палитра сэмплируется из НАСТОЯЩИХ ichor1/ichor2.png ТК),
корона-антенна золотом, панельки-композиты, самоцветы. По ткани
прорезаны русла — ихор ТЕЧЁТ по ним бегущими дэшами glow-кадров.

Путь А (Пустота, ModelQuantVoidArmour): база — НЕТРОНУТАЯ развёртка
void_robe_armor.png ТК (роба уже нарисована художником ТК), в её
свободных зонах — развёртки квантовых частей языком v3 (ячеистый
композит, каналы, гекс-узлы). Glow — узлы с затухающими затравками.

Арт-правила (texture-style-rules): без монотонных градиентов,
пофейсовая отрисовка, материал деталями. Языковые функции (каналы,
композит, самоцветы) переиспользуются из gen_quantum_hybrid (v3).
"""
import io
import math
import os
import sys
import zipfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_quantum_hybrid import (  # noqa: E402
    S, faces, load_ic2, lum, Channels, dash_runes, carve_channel,
    hex_node, face_cellplate, face_suitweave, gem_face, recolour_suit,
    ICHOR_SUIT, LILAC, BLUE, GOLD_GLOW, GOLD_CORE, DARK_HOUSING,
    BRASS, STEEL)

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")
OUT = os.path.join(ROOT, "textures", "models", "armor")
TC_JAR = (r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\mods"
          r"\Thaumcraft-1.2.8.2-universal.jar")


def load_tc(path):
    with zipfile.ZipFile(TC_JAR) as z:
        img = Image.open(io.BytesIO(z.read(path)))
        img.load()
    return img.convert("RGBA")


# ==================== Ихор: раскладка (плоскость 128x64) ====================
# зона 0..64 x 0..32 — стандартная развёртка брони (квант-подкладка)

UV_ICHOR = {
    "hat":         (64, 0, 10, 2, 10, "hatcloth"),
    "spire":       (104, 0, 1, 4, 1, "gild"),
    "spire_gem":   (110, 0, 1, 1, 1, "gem"),
    "prong":       (116, 0, 1, 2, 1, "gild"),
    "front_robe":  (64, 14, 3, 9, 1, "cloth"),
    "back_robe":   (74, 14, 8, 9, 1, "cloth"),
    "chest_panel": (94, 14, 3, 2, 1, "plate"),
    "gem_chest":   (104, 14, 2, 2, 1, "gem"),
    "gem_belt":    (112, 14, 2, 2, 1, "gem"),
    "belt_sash":   (64, 26, 8, 2, 4, "cloth"),
    "cuff_arm":    (88, 26, 5, 3, 5, "cloth"),
    "roll":        (64, 34, 5, 2, 5, "cloth"),
    "bloom":       (0, 34, 6, 6, 6, "cloth"),
    "knee_cuff":   (26, 34, 5, 2, 5, "cloth"),
    "toe":         (26, 44, 5, 2, 5, "plate"),
    "back_panel":  (48, 44, 2, 2, 1, "plate"),
}

# ==================== Пустота: раскладка в дырах робы ====================
# зоны свободны по ОБЪЕДИНЁННОЙ альфе base∪overlay (скептик №2):
# роба рисуется двумя слоями, ткань живёт в overlay

UV_VOID = {
    "pauldron":     (76, 0, 7, 4, 9, "plate"),
    "pauldron_top": (44, 0, 9, 2, 6, "plate"),
    "hood_ring":    (68, 52, 10, 2, 9, "plate"),
    "chest_core":   (0, 32, 8, 5, 2, "plate"),
    "gem_brow":     (8, 10, 2, 2, 1, "gem"),
    "lamp":         (16, 10, 1, 1, 1, "lamp"),
    "skirt_slab":   (44, 8, 3, 4, 1, "slab"),
    "sab_cap":      (24, 32, 6, 3, 4, "plate"),
    "knee_guard":   (36, 28, 5, 3, 1, "plate"),
    "back_cell":    (106, 52, 6, 6, 1, "plate"),
    "belt_node":    (8, 0, 3, 2, 1, "node"),
    "heel":         (8, 4, 5, 2, 3, "slab"),
}

GEM_NAMES = {"spire_gem", "gem_chest", "gem_belt", "gem_brow"}


def ichor_palette():
    """Палитра ткани — сэмпл из НАСТОЯЩИХ ichor1/ichor2.png ТК
    (правило: ассеты только с реальных образцов): 4 тона по квартилям
    яркости непрозрачных пикселей."""
    pixels = []
    for name in ("ichor1", "ichor2"):
        img = load_tc("assets/thaumcraft/textures/models/%s.png" % name)
        px = img.load()
        for y in range(img.size[1]):
            for x in range(img.size[0]):
                c = px[x, y]
                # только ТЁПЛЫЕ тона: серый металл слоёв давал соль-шум
                if c[3] > 128 and c[0] > c[2] + 16 and c[1] >= c[2]:
                    pixels.append(c[:3])
    pixels.sort(key=lum)
    n = len(pixels)
    # опорный тон: медиана образца, осветлённая к золоту (критик v2:
    # квантили ткани почти равны — контраст задаём принудительно
    # масштабом от опоры, шаг >= 8-12 ступеней RGB)
    base = tuple(int(v * 0.45 + t * 0.55) for v, t in
                 zip(pixels[n // 2], (0xE8, 0xC0, 0x60)))
    return [tuple(min(255, int(v * k)) for v in base)
            for k in (0.74, 0.92, 1.08, 1.26)]


def cloth_face(px, rect, pal, channels, channel=True, hem=True):
    """Ткань ихора: пофейсовое плетение 4 тонов, золотая кайма подола;
    по крупным граням — прорезанное русло с гекс-узлом (ихор течёт)."""
    x0, y0, w, h = rect
    if w <= 0 or h <= 0:
        return
    seed = (x0 * 7 + y0 * 13) % 23   # сбив фазы на каждой грани
    narrow = w <= 5 or h <= 5
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if narrow:
                # торцы и узкие грани — продольные нити, без шахматки
                i = 2 if (y + seed) % 3 else 1
            else:
                i = (1, 2, 2, 1)[(x + 2 * y + seed) % 4]
                if (x * 3 + y * 5 + seed) % 19 == 0:
                    i = 0            # редкая тёмная нить
                elif (x * 7 + y * 3 + seed) % 23 == 0:
                    i = 3            # редкий тёплый блик по гребню
            px[x, y] = pal[i] + (255,)
    if hem:
        for x in range(x0, x0 + w):
            px[x, y0 + h - 1] = BRASS[2] + (255,)
            if (x - x0) % 3 == 0:
                px[x, y0 + h - 1] = BRASS[4] + (255,)
    if channel and w >= 6 and h >= 10:
        # русло: сверху вниз с коленом, узел в нижней трети
        ex = x0 + w // 2 + ((x0 // 3) % 3 - 1)
        ex = max(x0 + 2, min(x0 + w - 3, ex))
        nx = max(x0 + 2, min(x0 + w - 3, x0 + w // 2 - 1))
        ny = y0 + 2 * h // 3
        points = [(ex, y0 + 1), (ex, y0 + h // 3), (nx, y0 + h // 3), (nx, ny)]
        channels.add(points)
        carve_channel(px, points, False)
        hex_node(px, nx, ny, False)


def gild_face(px, rect):
    """Золото короны: тёплая база, тёмный кант, точечные блики."""
    x0, y0, w, h = rect
    if w <= 0 or h <= 0:
        return
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                c = BRASS[1]
            else:
                c = BRASS[3] if (x * 3 + y * 7) % 5 else BRASS[2]
                if (x * 5 + y * 3) % 13 == 0:
                    c = BRASS[4]
            px[x, y] = c + (255,)


def gem_v4(px, rect, core, deep):
    """Самоцвет с огранкой (критик): тёмная оправа по контуру, ядро
    локального цвета, 1px белый блик сверху-слева."""
    x0, y0, w, h = rect
    if w <= 0 or h <= 0:
        return
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                c = DARK_HOUSING[0]
            elif x - x0 <= 1 or y - y0 <= 1:
                c = core
            else:
                c = deep
            px[x, y] = c + (255,)
    if w >= 3 and h >= 3:
        px[x0 + 1, y0 + 1] = (0xFF, 0xFF, 0xF0, 255)


def cellplate_void(px, rect, channels=None, entry=None, node_at=None):
    """Композит Пустоты v4 (критик: не «сыпь в горошек»): кант холодным
    VOID_RIM светлее ткани, ~2/3 ячеек пригашены, яркие — группами;
    узел — ромб 3x3 лиловым, читается на модели."""
    from gen_quantum_hybrid import VOID_CELL, VOID_RIM, COMPOSITE
    x0, y0, w, h = rect
    if w <= 0 or h <= 0:
        return
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                c = VOID_RIM
            elif (x - x0) % 3 == 0 or (y - y0) % 3 == 0:
                c = COMPOSITE[True]
            else:
                cx, cy = (x - x0) // 3, (y - y0) // 3
                if (cx // 2 + cy * 3) % 4 == 0:
                    c = VOID_CELL[2] if (cx + cy) % 2 else VOID_CELL[1]
                else:
                    c = VOID_CELL[0] if (cx * 3 + cy * 5) % 3 else COMPOSITE[True]
            px[x, y] = c + (255,)
    if channels is not None and entry is not None and node_at is not None:
        ex, ey = entry
        nx, ny = node_at
        points = [(ex, ey), (nx, ey), (nx, ny)] if ex != nx else [(ex, ey), (nx, ny)]
        channels.add(points)
        carve_channel(px, points, True)
        for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, yy = nx + dx, ny + dy
            if x0 < xx < x0 + w - 1 and y0 < yy < y0 + h - 1:
                px[xx, yy] = (0x8A, 0x64, 0xB4, 255)
        px[nx, ny] = LILAC + (255,)


def slab_face(px, rect):
    """Утяжелённый подол Пустоты: тёмная ткань с холодным кантом."""
    face_suitweave(px, rect, True)
    x0, y0, w, h = rect
    if w <= 0 or h <= 0:
        return
    for x in range(x0, x0 + w):
        px[x, y0 + h - 1] = STEEL[1] + (255,)


def paint_parts(px, uv, void, pal, channels):
    for name, (u, v, w, h, d, role) in uv.items():
        fs = faces(u, v, w, h, d)
        for f, rect in fs.items():
            if rect[2] <= 0 or rect[3] <= 0:
                continue
            if role == "plate":
                entry = node_at = None
                x0, y0, fw, fh = rect
                if f == "front" and fw >= 10 and fh >= 6:
                    ex = max(x0 + 2, min(x0 + fw - 3, x0 + fw // 2 + (u % 3) - 1))
                    nx = max(x0 + 2, min(x0 + fw - 3, x0 + fw // 2 - (u % 2)))
                    entry = (ex, y0 + fh - 1)
                    node_at = (nx, y0 + 2 + fh // 4)
                if void:
                    cellplate_void(px, rect, channels, entry, node_at)
                else:
                    face_cellplate(px, rect, void, channels, entry, node_at)
            elif role == "cloth":
                cloth_face(px, rect, pal, channels,
                           channel=(f in ("front", "back")), hem=True)
            elif role == "hatcloth":
                cloth_face(px, rect, pal, channels, channel=False,
                           hem=(f != "top" and f != "bottom"))
            elif role == "gild":
                gild_face(px, rect)
            elif role == "slab":
                slab_face(px, rect)
            elif role == "node":
                if void:
                    cellplate_void(px, rect)
                else:
                    face_cellplate(px, rect, void)
                x0, y0, fw, fh = rect
                if f == "front" and fw >= 4 and fh >= 4:
                    hex_node(px, x0 + fw // 2 - 1, y0 + fh // 2 - 1, void)
                    channels.add([(x0 + fw // 2 - 1, y0 + 1),
                                  (x0 + fw // 2 - 1, y0 + fh // 2 - 1)])
            elif role == "lamp":
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        px[x, y] = DARK_HOUSING[1] + (255,)
                px[x0, y0] = DARK_HOUSING[2] + (255,)
            elif role == "gem":
                if void:
                    gem_v4(px, rect, LILAC, (0x5A, 0x3E, 0x86))
                else:
                    gem_v4(px, rect, GOLD_CORE, (0xB4, 0x7A, 0x1E))


def base_ichor(channels):
    """Подкладка — перекрашенный квант IC2 в стандартной зоне брони,
    полнотелость, поверх — развёртки роб из ткани ихора."""
    canvas = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))
    cp = canvas.load()
    q1 = recolour_suit(load_ic2("assets/ic2/textures/armor/quantum_1.png"), False)
    q2 = recolour_suit(load_ic2("assets/ic2/textures/armor/quantum_2.png"), False)
    for src in (q1, q2):
        big = src.resize((src.size[0] * 2, src.size[1] * 2), Image.NEAREST)
        bp = big.load()
        for y in range(min(big.size[1], canvas.size[1])):
            for x in range(min(big.size[0], canvas.size[0])):
                c = bp[x, y]
                if c[3] > 8:
                    cp[x, y] = c
    for y in range(0, 32 * S):
        for x in range(0, 64 * S):
            if cp[x, y][3] <= 128:
                i = 1 if (x // 2 + y // 2) % 3 else 0
                cp[x, y] = ICHOR_SUIT[i] + (255,)
    pal = ichor_palette()
    paint_parts(cp, UV_ICHOR, False, pal, channels)
    return canvas


def base_void(channels):
    """База — нетронутая роба ТК; квант-части в её свободных зонах."""
    base = load_tc("assets/thaumcraft/textures/models/void_robe_armor.png")
    over = load_tc(
        "assets/thaumcraft/textures/models/void_robe_armor_overlay.png")
    # ТК рендерит робу двумя проходами: overlay = вся ткань (тинтуется
    # цветом покраски), base = металл поверх. Наш предмет не красится и
    # рендерится одним проходом — запекаем тинтованный дефолтным
    # DEFAULT_ROBE_COLOR (6961280) overlay ПОД базу (скептик №1).
    tint = (6961280 >> 16 & 0xFF, 6961280 >> 8 & 0xFF, 6961280 & 0xFF)
    for img in (base, over):
        assert img.size == (128 * S, 64 * S), img.size
    op = over.load()
    for y in range(over.size[1]):
        for x in range(over.size[0]):
            r, g, b, a = op[x, y]
            op[x, y] = (r * tint[0] // 255, g * tint[1] // 255,
                        b * tint[2] // 255, a)
    canvas = over
    canvas.alpha_composite(base)
    cp = canvas.load()
    # оригинал ТК хранит чисто-RGB дебаг-маркеры вне UV робы — в
    # прозрачность (критик: «светофор», если UV какой-то грани заденет)
    for y in range(canvas.size[1]):
        for x in range(canvas.size[0]):
            r, g, b, a = cp[x, y]
            if a > 0 and ((r >= 90 and g < 50 and b < 50)
                          or (g >= 90 and r < 50 and b < 50)
                          or (b >= 90 and r < 50 and g < 50)):
                cp[x, y] = (0, 0, 0, 0)
    paint_parts(cp, UV_VOID, True, None, channels)
    return canvas


def glow_ichor(channels):
    """Ихор ТЕЧЁТ: бегущие дэши по руслам, дыхание самоцветов."""
    frames = []
    for f in range(8):
        img = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))
        px = img.load()
        breath = 0.55 + 0.45 * math.sin(math.pi * 2 * f / 8)
        for (points, end) in channels.lines:
            dash_runes(px, points, GOLD_GLOW, phase=f, broken=False)
        for name, (u, v, w, h, d, role) in UV_ICHOR.items():
            if name not in GEM_NAMES:
                continue
            x0, y0, fw, fh = faces(u, v, w, h, d)["front"]
            k = breath if name != "spire_gem" else 1.0
            c = tuple(int(vv * k) for vv in GOLD_CORE)
            for y in range(y0, y0 + fh):
                for x in range(x0, x0 + fw):
                    px[x, y] = c + (255,)
        # кончик шпиля короны мигает маяком
        u, v, w, h, d, _ = UV_ICHOR["spire"]
        x0, y0, fw, fh = faces(u, v, w, h, d)["top"]
        if f % 4 < 2:
            for y in range(y0, y0 + fh):
                for x in range(x0, x0 + fw):
                    px[x, y] = GOLD_GLOW + (255,)
        frames.append(img)
    return frames


def glow_void(channels):
    """Пустота: узлы статичны, затравки затухают, лампы Э2, самоцвет."""
    frames = []
    for f in range(8):
        img = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))
        px = img.load()
        breath = 0.55 + 0.45 * math.sin(math.pi * 2 * f / 8)
        from gen_quantum_hybrid import polyline_pixels
        for (points, end) in channels.lines:
            tail = polyline_pixels(points)[-8:]
            tail.reverse()
            for i, (x, y) in enumerate(tail):
                fade = (1.0, 0.55, 0.25)[min(2, i // 3)]
                if (i % 4) < 3:
                    c = tuple(int(v * fade) for v in LILAC)
                    px[x, y] = c + (255,)
        for (x, y, t) in channels.nodes:
            px[x, y] = LILAC + (255,)
        for name, (u, v, w, h, d, role) in UV_VOID.items():
            if role == "lamp":
                x0, y0, fw, fh = faces(u, v, w, h, d)["front"]
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        px[x, y] = BLUE + (255,)
            elif name in GEM_NAMES:
                x0, y0, fw, fh = faces(u, v, w, h, d)["front"]
                c = tuple(int(vv * breath) for vv in LILAC)
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        px[x, y] = c + (255,)
        frames.append(img)
    return frames


def main():
    ch_i = Channels()
    base_i = base_ichor(ch_i)
    base_i.save(os.path.join(OUT, "quant_ichor.png"))
    for i, frame in enumerate(glow_ichor(ch_i)):
        frame.save(os.path.join(OUT, "quant_ichor_glow_%d.png" % i))
    ch_v = Channels()
    base_v = base_void(ch_v)
    base_v.save(os.path.join(OUT, "quant_void.png"))
    for i, frame in enumerate(glow_void(ch_v)):
        frame.save(os.path.join(OUT, "quant_void_glow_%d.png" % i))
    print("v4: ихор=%d русел, пустота=%d узлов" %
          (len(ch_i.lines), len(ch_v.nodes)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
