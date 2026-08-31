# -*- coding: utf-8 -*-
"""Текстуры Квант-Гибридной брони v3 — «слияние 1+1=3» по вердикту
спора художников (wf_717f1cef-3a6, арбитраж):

ФОРМУЛА: единица слияния — НЕПРЕРЫВНАЯ СЕТЬ КАНАЛОВ, материал —
ЯЧЕИСТЫЙ КВАНТ-КОМПОЗИТ. Каналы стартуют в РОДНЫХ жилах quantum_1/2
(жилы сохраняются, не глушатся тинтом), переползают на пластины и
кончаются узлами (самоцвет/лампа/сопло/эмблема). Каждый дэш — рунный
микро-глиф («сломанный угол»). Пластины — решётка IC2, ячейки залиты
металлом ТК. Латунь/сталь — только 1px муфты у узлов. Пути различаются
СУДЬБОЙ одной сети: Пустота выпила каналы (тёмные борозды с призрачным
ободком, свет только в узлах + 2-3 затухающих дэша-затравки), Ихор
ТЕЧЁТ по ним (бегущие золотые дэши покадрово, капли с кромок).

Тест нано-таума: ни про одну деталь нельзя сказать «это IC2» или
«это ТК». Ровно 2 эмиссива на путь. Геометрия модели не тронута.
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

# === палитры вердикта (§2) ===
VOID_SUIT = [(0x14, 0x10, 0x1C), (0x1C, 0x16, 0x26), (0x26, 0x1D, 0x34),
             (0x2A, 0x21, 0x36)]
VOID_CELL = [(0x3A, 0x24, 0x50), (0x4E, 0x33, 0x70), (0x6B, 0x4A, 0x94),
             (0x9B, 0x7B, 0xD4)]
VOID_TRENCH = (0x0A, 0x07, 0x14)
VOID_RIM = (0x3E, 0x2E, 0x58)
CLOTH_VOID = [(0x10, 0x0C, 0x18), (0x16, 0x10, 0x1F), (0x1A, 0x14, 0x28)]
DEAD_THREAD = (0x0B, 0x08, 0x12)

ICHOR_SUIT = [(0x1E, 0x15, 0x0C), (0x2A, 0x1E, 0x10), (0x38, 0x28, 0x1A),
              (0x46, 0x33, 0x1E)]
ICHOR_CELL = [(0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52), (0xE8, 0xC0, 0x60),
              (0xF0, 0xDF, 0xA0)]
ICHOR_BED = (0x6E, 0x50, 0x16)
ICHOR_DASH = (0xD0, 0xAC, 0x52)
GILD = (0x96, 0x74, 0x2A)

BRASS = [(0x50, 0x3E, 0x14), (0x74, 0x59, 0x1E), (0x96, 0x74, 0x2A),
         (0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52)]
STEEL = [(0x24, 0x22, 0x2C), (0x3A, 0x38, 0x46), (0x52, 0x50, 0x60),
         (0x70, 0x6D, 0x7E), (0x92, 0x8F, 0xA0)]
DARK_HOUSING = [(0x0C, 0x0A, 0x10), (0x16, 0x13, 0x1C), (0x22, 0x1E, 0x2C)]
COMPOSITE = {True: (0x1C, 0x16, 0x26), False: (0x2A, 0x1E, 0x10)}

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

# узлы, светящиеся у ОБОИХ путей (маски в glow-кадрах)
NODE_GLOW = ("gem", "medallion", "hipmed", "crystal", "slit")


def faces(u, v, w, h, d):
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


# ==================== сеть каналов ====================

class Channels:
    """Реестр полилиний (§0.1): база рисует русла, glow — дэши/узлы."""

    def __init__(self):
        self.lines = []    # (points, end_type)
        self.nodes = []    # (x, y, type)
        self.drips = []    # (x, y) кончики капель (Ихор)

    def add(self, points, end="node"):
        self.lines.append((points, end))
        if end != "edge":
            ex, ey = points[-1]
            self.nodes.append((ex, ey, end))


def polyline_pixels(points):
    """Пиксели вдоль ломаной (сегменты орто/диагональ)."""
    out = []
    for i in range(len(points) - 1):
        x0, y0 = points[i]
        x1, y1 = points[i + 1]
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for s in range(steps + 1):
            x = x0 + (x1 - x0) * s // max(1, steps)
            y = y0 + (y1 - y0) * s // max(1, steps)
            if not out or out[-1] != (x, y):
                out.append((x, y))
    return out


def dash_runes(px, points, colour, phase=0, gap=1, dash=3, broken=True,
               fade=None):
    """Пунктир-руны вдоль ломаной (§0.2): дэш 2-3px, разрыв 1px, у дэша
    один пиксель смещён перпендикулярно («сломанный угол» = глиф)."""
    pix = polyline_pixels(points)
    period = dash + gap
    for i, (x, y) in enumerate(pix):
        k = (i + phase) % period
        if k >= dash:
            continue
        c = colour
        if fade is not None:
            seg = (i + phase) // period
            f = fade(seg)
            if f <= 0:
                continue
            c = tuple(int(v * f) for v in colour)
        px[x, y] = c + (255,)
        if broken and k == 1:
            # сломанный угол: перпендикулярный пиксель
            nx, ny = (x, y - 1) if (i > 0 and pix[i - 1][1] == y) else (x + 1, y)
            if 0 <= nx < 256 and 0 <= ny < 128:
                px[nx, ny] = c + (255,)


def carve_channel(px, points, void):
    """Русло в базе: Пустота — траншея с призрачным ободком, Ихор —
    русло-борозда (золото добавит glow)."""
    pix = polyline_pixels(points)
    for (x, y) in pix:
        if void:
            px[x, y] = VOID_TRENCH + (255,)
            for (nx, ny) in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if 0 <= nx < 256 and 0 <= ny < 128 and (nx, ny) not in pix[
                        max(0, pix.index((x, y)) - 2):pix.index((x, y)) + 3]:
                    cur = px[nx, ny]
                    if cur[3] == 255 and cur[:3] != VOID_TRENCH:
                        px[nx, ny] = VOID_RIM + (255,)
        else:
            px[x, y] = ICHOR_BED + (255,)
    if not void:
        dash_runes(px, points, ICHOR_DASH)


def hex_node(px, x, y, void):
    """Гекс-гнездо 2x2: тёмный металл + 2 угловых пикселя муфты (§1.1)."""
    accent = BRASS[3] if void else STEEL[3]
    for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
        px[x + dx, y + dy] = DARK_HOUSING[1] + (255,)
    px[x, y] = accent + (255,)
    px[x + 1, y + 1] = accent + (255,)


# ==================== фактуры ====================

def face_cellplate(px, rect, void, channels=None, entry=None, node_at=None):
    """Ячеистый квант-композит (§1.1): решётка тёмного композита,
    ячейки залиты металлом ТК; муфта только в точке входа канала."""
    x0, y0, w, h = rect
    if w <= 0 or h <= 0:
        return
    cell = VOID_CELL if void else ICHOR_CELL
    comp = COMPOSITE[void]
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                c = DARK_HOUSING[0]           # внешний кант — тёмный композит
            elif (x - x0) % 3 == 0 or (y - y0) % 3 == 0:
                c = comp                      # линии решётки
            else:
                cx, cy = (x - x0) // 3, (y - y0) // 3
                i = (cx * 3 + cy * 5) % 3
                c = cell[i]
                if (cx * 7 + cy * 11) % 6 == 0:
                    c = cell[3]               # редкий блик верхней ступенью
            px[x, y] = c + (255,)
    if channels is not None and entry is not None and node_at is not None:
        ex, ey = entry
        nx, ny = node_at
        points = [(ex, ey), (nx, ey), (nx, ny)] if ex != nx else [(ex, ey), (nx, ny)]
        channels.add(points)
        carve_channel(px, points, void)
        hex_node(px, nx, ny, void)
        # муфта акцент-металла в точке входа
        px[ex, ey] = (BRASS[4] if void else STEEL[4]) + (255,)


def face_suitweave(px, rect, void):
    x0, y0, w, h = rect
    suit = VOID_SUIT if void else ICHOR_SUIT
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            i = 1
            if (x // 2 + y // 2) % 3 == 0:
                i = 0
            if (x * 3 + y * 5) % 13 == 0:
                i = 2
            px[x, y] = suit[i] + (255,)


def gem_face(px, rect, void, core):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                c = DARK_HOUSING[0]           # кант композита (§1.2)
            else:
                cx = abs((x - x0) - w / 2 + 0.5)
                cy = abs((y - y0) - h / 2 + 0.5)
                c = core if cx + cy < max(w, h) / 2.5 else DARK_HOUSING[1]
            px[x, y] = c + (255,)
    # ровно 2 лапки акцент-металла
    px[x0, y0] = (BRASS[3] if void else STEEL[3]) + (255,)
    px[x0 + w - 1, y0 + h - 1] = (BRASS[3] if void else STEEL[3]) + (255,)


# ==================== генерация базы ====================

def recolour_suit(img, void):
    """§0.3: жилы кванта СОХРАНЯЮТСЯ — верхние 25% яркости остаются
    трассами (дэши цвета пути), остальное — тёмный композит."""
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
    suit = VOID_SUIT if void else ICHOR_SUIT
    # критик v3: порог 75% захватывал целые панели кванта → «диагональный
    # дождь»; жилы — только верхние 12% яркости (настоящие светящиеся
    # линии IC2), дэши длинные с пофазовым сдвигом по регионам
    vein_cut = lo + span * 0.88
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] <= 8:
                continue
            if lum(c) >= vein_cut:
                phase = (x // 8 + y // 8) % 5
                on = ((x + y // 2 + phase) % 6) < 4
                if void:
                    px[x, y] = (VOID_TRENCH if on else VOID_RIM) + (c[3],)
                else:
                    px[x, y] = (ICHOR_DASH if on else ICHOR_BED) + (c[3],)
            else:
                level = (lum(c) - lo) * (len(suit) - 1) // span
                px[x, y] = suit[min(len(suit) - 1, level)] + (c[3],)
    return img


def paint_all(px, void, channels):
    accent = BRASS if void else STEEL
    for name, (u, v, w, h, d, role) in UV.items():
        fs = faces(u, v, w, h, d)
        if role == "plate":
            # каналы: вход с кромки → колено → узел; фаза и геометрия
            # зависят от пластины (критик: не клоны), у широких — развилка
            for f, rect in fs.items():
                x0, y0, fw, fh = rect
                entry = node_at = None
                if f == "front" and fw >= 6 and fh >= 6:
                    shift = (u % 3) - 1
                    ex = x0 + fw // 2 + shift * 2
                    ex = max(x0 + 2, min(x0 + fw - 3, ex))
                    nx = x0 + fw // 2 - shift
                    nx = max(x0 + 2, min(x0 + fw - 3, nx))
                    entry = (ex, y0 + fh - 1)
                    node_at = (nx, y0 + 2 + (v % 2) + fh // 4)
                face_cellplate(px, rect, void, channels, entry, node_at)
                if f == "front" and fw >= 14 and fh >= 8:
                    # развилка: боковой отвод от середины к кромке
                    my = y0 + fh // 2
                    branch = [(x0 + fw // 2, my), (x0 + 1, my)]
                    channels.add(branch, end="edge")
                    carve_channel(px, branch, void)
                    px[x0 + 1, my] = (BRASS[4] if void else STEEL[4]) + (255,)
        elif role == "gem":
            for f, rect in fs.items():
                gem_face(px, rect, void, LILAC if void else GOLD_CORE)
        elif role == "rune":
            for f, rect in fs.items():
                face_cellplate(px, rect, void)
                x0, y0, fw, fh = rect
                if fw >= 6:
                    pts = [(x0 + 1, y0 + fh // 2), (x0 + fw - 2, y0 + fh // 2)]
                    channels.add(pts, end="edge")
                    carve_channel(px, pts, void)
        elif role == "steel":
            for f, rect in fs.items():
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        i = 2 if (x + y) % 3 else 1
                        if y == y0:
                            i = 3
                        px[x, y] = STEEL[i] + (255,)
                if name == "nozzle" and f == "front":
                    for y in range(y0 + 1, y0 + fh - 1):
                        for x in range(x0 + 1, x0 + fw - 1):
                            px[x, y] = (DARK_HOUSING[0] if void
                                        else GOLD_GLOW) + (255,)
                    px[x0, y0] = BRASS[2] + (255,)
                if name == "heel" and not void and fw >= 3:
                    for x in range(x0 + 1, x0 + fw - 1):
                        px[x, y0 + fh // 2] = ICHOR_DASH + (255,)
                if name == "heel" and void:
                    for x in range(x0, x0 + fw, 2):
                        px[x, y0 + fh // 2] = BRASS[2] + (255,)
        elif role == "cable":
            for f, rect in fs.items():
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        px[x, y] = COMPOSITE[void] + (255,)
                if fh >= fw and fh >= 3:
                    pts = [(x0 + fw // 2, y0), (x0 + fw // 2, y0 + fh - 1)]
                elif fw >= 3:
                    pts = [(x0, y0 + fh // 2), (x0 + fw - 1, y0 + fh // 2)]
                else:
                    continue
                channels.add(pts, end="edge")
                carve_channel(px, pts, void)
        elif role == "seal":
            for f, rect in fs.items():
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        i = 2 if (x + y) % 4 else 3
                        px[x, y] = accent[i] + (255,)
                if f == "front" and fh >= 6:
                    # §1.6: глифы сверху морфят в трассы снизу
                    for k in range(x0 + 1, x0 + fw - 1, 2):
                        px[k, y0 + 1] = (0x14, 0x11, 0x1A, 255)
                        if k + 1 < x0 + fw - 1:
                            px[k + 1, y0 + 2] = (0x14, 0x11, 0x1A, 255)
                    for k in range(x0 + 1, x0 + fw - 1):
                        if (k - x0) % 3 != 0:
                            px[k, y0 + fh - 2] = (0x14, 0x11, 0x1A, 255)
                    px[x0 + fw - 2, y0 + fh - 2] = DARK_HOUSING[0] + (255,)
        elif role == "book":
            for f, rect in fs.items():
                face_cellplate(px, rect, void)
                x0, y0, fw, fh = rect
                if f == "front" and fw >= 2 and fh >= 2:
                    for y in range(y0, y0 + fh):
                        px[x0, y] = (BRASS[3] if void else STEEL[3]) + (255,)
                    ex, ey = x0 + fw // 2, y0 + fh // 2
                    channels.add([(x0, ey), (ex, ey)], end="emblem")
                    px[ex, ey] = (LILAC if void else GOLD_GLOW) + (255,)
                if f == "right":
                    for y in range(y0 + 1, y0 + fh - 1):
                        if (y - y0) % 2:
                            px[x0 + fw // 2, y] = (VOID_RIM if void
                                                   else ICHOR_DASH) + (255,)
        elif role == "cape" and void:
            for f, rect in fs.items():
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        i = 1 if (x // 2 + y // 3) % 3 else 0
                        px[x, y] = CLOTH_VOID[i] + (255,)
                # мёртвые нити: колонки дэшей, рваный край по их разрывам
                for cx in range(x0 + 2, x0 + fw - 1, 4):
                    for y in range(y0 + 1, y0 + fh - 1):
                        if (y - y0) % 3 != 2:
                            px[cx, y] = DEAD_THREAD + (255,)
                    torn = 1 + (cx // 4) % 3
                    for k in range(torn):
                        for dx in (-1, 0, 1):
                            if x0 <= cx + dx < x0 + fw:
                                px[cx + dx, y0 + fh - 1 - k] = (0, 0, 0, 0)
                if f == "front" and fw >= 6:
                    pts = [(x0 + fw // 2, y0 + 1), (x0 + fw // 2, y0 + fh - 2)]
                    carve_channel(px, pts, True)
        elif role == "cape":
            continue   # у Ихора плаща нет
        elif role == "accent":
            for f, rect in fs.items():
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        link = (y - y0) % 2 == 0
                        px[x, y] = (accent[3] if link
                                    else COMPOSITE[void]) + (255,)
        elif role == "cloth":
            for f, rect in fs.items():
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        i = 1 if (x + y * 2) % 5 else 0
                        px[x, y] = CLOTH_VOID[i] + (255,)
                if f == "top":
                    px[x0 + fw // 2, y0] = accent[3] + (255,)
        elif role == "housing":
            for f, rect in fs.items():
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        edge = x in (x0, x0 + fw - 1) or y in (y0, y0 + fh - 1)
                        px[x, y] = DARK_HOUSING[0 if edge else 1] + (255,)
                if f == "front":
                    px[x0, y0] = (BRASS[3] if void else STEEL[3]) + (255,)
    # §3: подтёки Ихора из выходов каналов на нижних кромках
    if not void:
        for name in ("chest", "pauld_low", "sabaton"):
            u, v, w, h, d, _ = UV[name]
            fr = faces(u, v, w, h, d)["front"]
            x0, y0, fw, fh = fr
            dx = x0 + fw // 2
            length = 2 + (dx % 3)
            for k in range(length):
                y = y0 + fh + k
                if y < 128:
                    px[dx, y] = ICHOR_DASH + (255,)
                    if dx + 1 < 256:
                        px[dx + 1, y] = GILD + (255,)
            channels.drips.append((dx, min(127, y0 + fh + length)))


def base_texture(path_key, channels):
    void = path_key == "void"
    canvas = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))
    q1 = recolour_suit(load_ic2("assets/ic2/textures/armor/quantum_1.png"), void)
    q2 = recolour_suit(load_ic2("assets/ic2/textures/armor/quantum_2.png"), void)
    cp = canvas.load()
    for src in (q1, q2):
        big = src.resize((src.size[0] * 2, src.size[1] * 2), Image.NEAREST)
        bp = big.load()
        for y in range(min(big.size[1], canvas.size[1])):
            for x in range(min(big.size[0], canvas.size[0])):
                c = bp[x, y]
                if c[3] > 8:
                    cp[x, y] = c
    # полнотелость бипед-зон
    suit = VOID_SUIT if void else ICHOR_SUIT
    for y in range(0, 32 * S):
        for x in range(0, 64 * S):
            if cp[x, y][3] <= 128:
                i = 1 if (x // 2 + y // 2) % 3 else 0
                cp[x, y] = suit[i] + (255,)
    paint_all(cp, void, channels)
    return canvas


# ==================== glow-кадры (§4) ====================

def glow_frames(path_key, channels):
    void = path_key == "void"
    frames = []
    for f in range(8):
        img = Image.new("RGBA", (128 * S, 64 * S), (0, 0, 0, 0))
        px = img.load()
        breath = 0.55 + 0.45 * math.sin(math.pi * 2 * f / 8)
        if void:
            # узлы статичны; из каждого узла 2-3 затухающих дэша-затравки
            for (points, end) in channels.lines:
                tail = polyline_pixels(points)[-8:]
                tail.reverse()
                for i, (x, y) in enumerate(tail):
                    fade = (1.0, 0.55, 0.25)[min(2, i // 3)]
                    if (i % 4) < 3:
                        c = tuple(int(v * fade) for v in LILAC)
                        px[x, y] = c + (255,)
        else:
            # ихор ТЕЧЁТ: бегущие дэши по всем полилиниям, фаза = кадр
            for (points, end) in channels.lines:
                dash_runes(px, points, GOLD_GLOW, phase=f, broken=False)
            for (x, y) in channels.drips:
                px[x, y] = GOLD_GLOW + (255,)
        # узлы обоих путей
        for name in NODE_GLOW:
            u, v, w, h, d, _ = UV[name]
            fr = faces(u, v, w, h, d)["front"]
            x0, y0, fw, fh = fr
            k = breath if name == "gem" else 1.0
            colour = (BLUE if name == "lamp" else LILAC) if void else (
                GOLD_CORE if name == "gem" else GOLD_GLOW)
            alpha = 153 if name == "crystal" else 255
            c = tuple(int(vv * k) for vv in colour)
            for y in range(y0 + 1, y0 + fh - 1):
                for x in range(x0 + 1, x0 + fw - 1):
                    px[x, y] = c + (alpha,)
        # лампы: Э2 голубой у Пустоты, золото у Ихора
        u, v, w, h, d, _ = UV["lamp"]
        fr = faces(u, v, w, h, d)["front"]
        x0, y0, fw, fh = fr
        lamp_c = BLUE if void else GOLD_GLOW
        for (dx, dy) in ((0, 0), (1, 1)):
            if x0 + dx < 256 and y0 + dy < 128:
                px[x0 + dx, y0 + dy] = lamp_c + (255,)
        # виа-ядра Ихора в узлах
        if not void:
            for (x, y, t) in channels.nodes:
                px[x, y] = GOLD_CORE + (255,)
        frames.append(img)
    return frames


def icons(path_key):
    void = path_key == "void"
    cell = VOID_CELL if void else ICHOR_CELL
    out = {}
    for slot in ("helmet", "chestplate", "leggings", "boots"):
        icon = load_ic2("assets/ic2/textures/items/armor/quantum_%s.png" % slot)
        px = icon.load()
        w, h = icon.size
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
                    level = (lum(c) - lo) * (len(cell) - 1) // span
                    px[x, y] = cell[level] + (c[3],)
        # два дэша канала по диагонали корпуса + пиксель Э1
        bed = VOID_TRENCH if void else ICHOR_BED
        for (dx, dy) in ((6, 8), (7, 9), (9, 10), (10, 11)):
            if px[dx, dy][3] > 8:
                px[dx, dy] = bed + (255,)
        px[8, 7] = (LILAC if void else GOLD_GLOW) + (255,)
        out["quant_%s_%s" % (path_key, slot)] = icon
    return out


def write_json(path, data):
    with io.open(path, "w", encoding="utf-8") as f:
        f.write(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def main():
    armor_dir = os.path.join(ROOT, "textures", "models", "armor")
    items_dir = os.path.join(ROOT, "textures", "items")
    models_dir = os.path.join(ROOT, "models", "item")
    for d in (armor_dir, items_dir, models_dir):
        os.makedirs(d, exist_ok=True)
    for path_key in ("void", "ichor"):
        channels = Channels()
        base_texture(path_key, channels).save(
            os.path.join(armor_dir, "quant_%s.png" % path_key))
        for f, frame in enumerate(glow_frames(path_key, channels)):
            frame.save(os.path.join(
                armor_dir, "quant_%s_glow_%d.png" % (path_key, f)))
        for name, icon in icons(path_key).items():
            icon.save(os.path.join(items_dir, name + ".png"))
            write_json(os.path.join(models_dir, name + ".json"),
                       {"parent": "item/generated",
                        "textures": {"layer0": "unboundtech:items/" + name}})
    print("квант v3: сеть каналов, ячеистый композит, судьба сети по пути")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
