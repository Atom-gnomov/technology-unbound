# -*- coding: utf-8 -*-
"""Текстура Мортиры Механистов v2 — по арт-правилам владельца
(texture-style-rules): БЕЗ монотонных градиентов и полосного шума,
пофейсовая отрисовка каждого бокса, материал читается деталями:

 - чугун (тумба, труба, казённик) — литьё с РАКОВИНАМИ (случайные
   тёмные точки-каверны), светлая фаска по кромке, потёртость у жерла;
 - сталь (ноги, стол, цапфы, короб) — панель с кантом и заклёпками;
 - латунь (погон, маховик, срез, крышка) — точечные блики, тёмная
   гравировка;
 - свечение: вентщели — светлый greyscale (цвет даёт TESR), лампа
   авто-режима — зелёная с обводкой.

Плоскость 128x128 — раскладка синхронизирована с ModelMechanistMortar
(каждый бокс получил СВОЮ развёртку после ревью №11).
"""
import io
import json
import os
import random

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")

IRON = [(0x17, 0x16, 0x1C), (0x23, 0x22, 0x2A), (0x30, 0x2F, 0x39),
        (0x3E, 0x3D, 0x49), (0x4E, 0x4D, 0x5B), (0x62, 0x61, 0x70)]
STEEL = [(0x1B, 0x1A, 0x22), (0x2E, 0x2C, 0x36), (0x42, 0x3F, 0x4C),
         (0x57, 0x54, 0x62), (0x6E, 0x6B, 0x79), (0x8A, 0x87, 0x95)]
BRASS = [(0x2E, 0x24, 0x0C), (0x50, 0x3E, 0x14), (0x74, 0x59, 0x1E),
         (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52)]
GREEN = (0x3E, 0xC8, 0x52)
GREEN_DARK = (0x1E, 0x6B, 0x1E)

# имя -> (u, v, w, h, d, материал)
UV = {
    "tumba_x":  (0, 0, 14, 12, 10, "iron"),
    "tumba_z":  (48, 0, 10, 12, 14, "iron"),
    "pogon":    (0, 26, 16, 2, 16, "brass"),
    "korob":    (96, 0, 8, 7, 5, "steel"),
    "kryshka":  (96, 12, 9, 1, 6, "brass"),
    "kabel":    (96, 19, 4, 3, 3, "steel"),
    "thigh":    (64, 26, 4, 12, 5, "steel"),
    "boot":     (82, 26, 7, 3, 9, "iron"),
    "stol":     (0, 44, 16, 3, 14, "steel"),
    "tsapfa":   (60, 44, 3, 9, 8, "steel"),
    "makhovik": (82, 44, 1, 5, 5, "brass"),
    "os":       (94, 44, 2, 1, 1, "brass"),
    "truba":    (0, 64, 10, 10, 14, "iron"),
    "kazennik": (56, 64, 11, 11, 3, "iron"),
    "bandazh":  (84, 64, 11, 11, 2, "brass"),
    "bore_v":   (0, 104, 2, 10, 4, "iron"),
    "bore_h":   (16, 104, 6, 2, 4, "iron"),
    "srez_v":   (40, 104, 2, 10, 1, "brass"),
    "srez_h":   (48, 104, 6, 2, 1, "brass"),
    "band_v":   (66, 104, 2, 11, 2, "brass"),
    "band_h":   (76, 104, 7, 2, 2, "brass"),
    "vent":     (0, 92, 11, 6, 4, "glow"),
    "lampa":    (32, 92, 4, 1, 4, "lamp"),
}


def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def cast_iron(px, rect, rnd, muzzle=False, seam=False):
    """Литой чугун (правки арт-критика): спокойная база, РЕДКИЕ
    кластерные раковины 2px со светлым ободком, сплошная кромка без
    чекера, литейный шов на длинных деталях."""
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge = x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1)
            if edge:
                c = IRON[3]
            else:
                c = IRON[2]
                r = rnd.random()
                if r > 0.96:
                    c = IRON[4]          # редкое зерно
            px[x, y] = c + (255,)
    # кластерные раковины: тёмное пятно 2px + светлый ободок снизу-справа
    pits = max(1, w * h // 40)
    for _ in range(pits):
        cx = x0 + 1 + rnd.randrange(max(1, w - 3))
        cy = y0 + 1 + rnd.randrange(max(1, h - 3))
        px[cx, cy] = IRON[0] + (255,)
        if cx + 1 < x0 + w - 1:
            px[cx + 1, cy] = IRON[1] + (255,)
        if cy + 1 < y0 + h - 1 and cx + 1 < x0 + w - 1:
            px[cx + 1, cy + 1] = IRON[5] + (255,)
    if seam and w >= 8:
        # литейный шов вдоль детали
        sy = y0 + h // 2
        for x in range(x0 + 1, x0 + w - 1):
            px[x, sy] = IRON[1] + (255,)
    if muzzle and h >= 4:
        for x in range(x0 + 1, x0 + w - 1, 2):
            px[x, y0 + 1] = IRON[5] + (255,)


def steel_panel(px, rect, rnd):
    """Стальная панель: кант, фаска, заклёпки по углам."""
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                c = STEEL[1]
            elif y == y0 + 1:
                c = STEEL[5]             # светлая верхняя кромка (критик №3)
            elif x == x0 + 1:
                c = STEEL[4]
            elif x == x0 + w - 2 or y == y0 + h - 2:
                c = STEEL[2]
            else:
                c = STEEL[3]
            px[x, y] = c + (255,)
    if w >= 5 and h >= 4:
        for (rx, ry) in ((x0 + 1, y0 + 1), (x0 + w - 2, y0 + 1),
                         (x0 + 1, y0 + h - 2), (x0 + w - 2, y0 + h - 2)):
            px[rx, ry] = STEEL[5] + (255,)


def brass_part(px, rect, rnd):
    """Латунь: тёплая с точечными бликами и тёмной гравировкой."""
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                c = BRASS[1]
            else:
                # спокойная база из двух близких тонов (критик №2);
                # блики — редкие одиночные яркие точки, стиль бандажей
                c = BRASS[3] if (x * 3 + y * 7) % 13 else BRASS[4]
                if rnd.random() < 0.03:
                    c = BRASS[5]
            px[x, y] = c + (255,)
    # гравировка-риска по центру длинной стороны
    if w >= 8:
        for x in range(x0 + 2, x0 + w - 2, 3):
            px[x, y0 + h // 2] = BRASS[0] + (255,)


def glow_slits(px, rect):
    """Вентщели: тёмная решётка со светлыми прорезями (цвет даёт TESR)."""
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            slit = (x - x0) % 3 != 0 and y0 < y < y0 + h - 1
            v = 0xE8 if slit else 0x30
            px[x, y] = (v, v, v, 255)


def bore_pit(px, rect, rnd):
    """Дно канала ствола (видно в открытое жерло): почти чёрная яма,
    кольцо у кромки, редкие тлеющие точки нагара."""
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge = x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1)
            if edge:
                c = IRON[1]
            else:
                c = (0x0B, 0x0A, 0x0E)
                if rnd.random() < 0.05:
                    c = (0x8A, 0x4A, 0x18)   # тлеющий нагар
        # тонкое кольцо нарезки на полпути к кромке
            if not edge and (x - x0 in (2, w - 3) or y - y0 in (2, h - 3)):
                c = IRON[0]
            px[x, y] = c + (255,)


def lamp(px, rect):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge = x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1)
            px[x, y] = (GREEN_DARK if edge else GREEN) + (255,)


def main():
    rnd = random.Random(20260831)   # детерминизм: одна текстура на все прогоны
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    px = img.load()
    for name, (u, v, w, h, d, mat) in UV.items():
        for fname, rect in faces(u, v, w, h, d).items():
            if rect[2] <= 0 or rect[3] <= 0:
                continue
            if mat == "iron":
                if name == "truba" and fname == "front":
                    # передняя грань трубы = дно открытого канала
                    bore_pit(px, rect, rnd)
                else:
                    cast_iron(px, rect, rnd,
                              muzzle=(name in ("bore_v", "bore_h")
                                      and fname == "front"),
                              seam=(name in ("truba", "tumba_x", "tumba_z")
                                    and fname in ("right", "left")))
            elif mat == "steel":
                steel_panel(px, rect, rnd)
            elif mat == "brass":
                brass_part(px, rect, rnd)
            elif mat == "glow":
                glow_slits(px, rect)
            elif mat == "lamp":
                lamp(px, rect)
    out = os.path.join(ROOT, "textures", "models", "mechanist_mortar.png")
    img.save(out)
    print("мортира v3: открытое жерло — канал ствола, рамка среза, дно-яма")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
