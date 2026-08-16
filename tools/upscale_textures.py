# -*- coding: utf-8 -*-
"""
Поднимает текстуры предметов T2 до 32x32 и рисует настоящую штриховку.

Почему не рисуем 32x32 руками по клеточкам: 12 предметов по 32 строки — это
почти четыреста строк ASCII, где одна опечатка ломает силуэт молча. Здесь
силуэт берётся из выверенной версии 16x16 (он снят с реальных образцов),
масштабируется вдвое, а объём наводится расчётом:

  * контур — по внешней границе маски;
  * освещение — сверху слева: чем дальше пиксель от «теневого» края
    (правый-нижний), тем светлее;
  * материал сохраняется исходный (сталь / дерево / таумий / стекло /
    заряд), меняется только оттенок внутри его рампы;
  * вис-вкрапления ставятся заново, по паре на предмет, только по металлу.

Палитра — art_guide §1: сталь плюс фиолетовые прожилки, фиолетовый = вис.
"""
import os
from PIL import Image

BASE = os.path.join(r"C:\Users\Game-On-Dp\technology-unbound",
                    "src", "main", "resources", "assets", "unboundtech", "textures", "items")

# Рампы по материалам: от тени к блику. Первый цвет — контур.
RAMPS = {
    "steel":    [(0x1B, 0x1A, 0x22), (0x33, 0x31, 0x3C), (0x47, 0x44, 0x51),
                 (0x5C, 0x59, 0x67), (0x74, 0x71, 0x7F), (0x8E, 0x8B, 0x99),
                 (0xA8, 0xA5, 0xB3)],
    "wood":     [(0x1E, 0x14, 0x0C), (0x33, 0x24, 0x16), (0x44, 0x31, 0x1D),
                 (0x54, 0x3C, 0x24), (0x66, 0x4A, 0x2D), (0x7A, 0x5A, 0x38),
                 (0x8E, 0x6B, 0x45)],
    "thaumium": [(0x1B, 0x10, 0x2A), (0x33, 0x1E, 0x4E), (0x4A, 0x2C, 0x6E),
                 (0x60, 0x3A, 0x8D), (0x7B, 0x4C, 0xB0), (0x96, 0x6C, 0xCC),
                 (0xB4, 0x93, 0xE0)],
    "glass":    [(0x12, 0x1C, 0x1E), (0x24, 0x33, 0x36), (0x36, 0x4C, 0x50),
                 (0x4B, 0x69, 0x6F), (0x63, 0x8B, 0x92), (0x7F, 0xB0, 0xB6),
                 (0x9E, 0xCF, 0xD4)],
    "charge":   [(0x08, 0x1E, 0x33), (0x11, 0x36, 0x56), (0x17, 0x4A, 0x74),
                 (0x22, 0x69, 0xA0), (0x2F, 0x8B, 0xC6), (0x3F, 0xAE, 0xE8),
                 (0x74, 0xCC, 0xF3)],
}

VIS = [(0x4A, 0x2A, 0x74), (0x74, 0x44, 0xAE), (0x9E, 0x66, 0xD8)]

# По какому материалу красить исходные цвета версии 16x16.
SOURCE_MATERIAL = {
    (0x21, 0x20, 0x2A): "steel", (0x38, 0x36, 0x42): "steel",
    (0x55, 0x52, 0x5E): "steel", (0x74, 0x71, 0x7F): "steel",
    (0x99, 0x96, 0xA4): "steel",
    (0x5E, 0x38, 0x8C): "steel", (0x93, 0x55, 0xBE): "steel",
    (0x33, 0x24, 0x16): "wood", (0x54, 0x3C, 0x24): "wood",
    (0x72, 0x53, 0x33): "wood",
    (0x3E, 0x24, 0x5E): "thaumium", (0x6B, 0x41, 0x9C): "thaumium",
    (0x96, 0x6C, 0xCC): "thaumium",
    (0x24, 0x33, 0x36): "glass", (0x7F, 0xB0, 0xB6): "glass",
    (0x17, 0x4A, 0x74): "charge", (0x3F, 0xAE, 0xE8): "charge",
}

# Куда поставить вис-вкрапления (координаты в 32x32, по металлу).
FLECKS = {
    "tempered_thaumium_ingot": [(12, 12), (19, 15), (9, 17)],
    "tempered_sword": [(17, 13), (21, 9)],
    "tempered_pickaxe": [(13, 8), (19, 6)],
    "tempered_axe": [(12, 9), (16, 6)],
    "tempered_shovel": [(19, 6), (22, 10)],
    "tempered_hoe": [(12, 5), (17, 4)],
    "tempered_helmet": [(9, 13), (22, 16)],
    "tempered_chestplate": [(11, 14), (21, 9), (16, 24)],
    "tempered_leggings": [(12, 8), (20, 17)],
    "tempered_boots": [(8, 13), (23, 13)],
    "thaumium_wrench": [],
    "electric_scribing_tools": [(9, 25), (14, 22)],
}


def material_of(colour):
    best, dist = "steel", 1 << 30
    for src, mat in SOURCE_MATERIAL.items():
        d = sum((a - b) ** 2 for a, b in zip(src, colour[:3]))
        if d < dist:
            best, dist = mat, d
    return best


def upscale(name):
    src = Image.open(os.path.join(BASE, name + ".png")).convert("RGBA")
    sp = src.load()

    # 1. Маска, материал и «это была линия» — в двойном разрешении.
    #
    # Внутренние тёмные линии исходника (забрало шлема, прорезь между ног,
    # стыки пластин) обязаны пережить масштабирование: без них предмет
    # превращается в ровное пятно. Поэтому пиксели цвета контура помечаются
    # отдельно и потом красятся в самый тёмный тон, а не пересчитываются.
    outline_src = (0x21, 0x20, 0x2A)
    mask = [[False] * 32 for _ in range(32)]
    mat = [[None] * 32 for _ in range(32)]
    ink = [[False] * 32 for _ in range(32)]
    for y in range(16):
        for x in range(16):
            c = sp[x, y]
            if c[3] == 0:
                continue
            m = material_of(c)
            line = c[:3] == outline_src
            for dy in range(2):
                for dx in range(2):
                    mask[y * 2 + dy][x * 2 + dx] = True
                    mat[y * 2 + dy][x * 2 + dx] = m
                    ink[y * 2 + dy][x * 2 + dx] = line

    def inside(x, y):
        return 0 <= x < 32 and 0 <= y < 32 and mask[y][x]

    out = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    op = out.load()

    for y in range(32):
        for x in range(32):
            if not mask[y][x]:
                continue
            ramp = RAMPS[mat[y][x]]
            # внутренняя линия исходника — всегда самый тёмный тон
            if ink[y][x]:
                op[x, y] = ramp[0] + (255,)
                continue
            # внешний контур
            edge = not (inside(x - 1, y) and inside(x + 1, y)
                        and inside(x, y - 1) and inside(x, y + 1))
            if edge:
                op[x, y] = ramp[0] + (255,)
                continue
            # глубина: сколько шагов до края вправо-вниз (теневая сторона)
            depth = 0
            while depth < 6 and inside(x + depth + 1, y + depth + 1):
                depth += 1
            # подсветка сверху-слева
            top = 0
            while top < 6 and inside(x - top - 1, y - top - 1):
                top += 1
            # Разброс шире, чем «плюс-минус один»: иначе крупная пластина
            # заливается одним тоном и объём не читается.
            level = 4 + (depth - top)
            op[x, y] = ramp[max(1, min(len(ramp) - 1, level))] + (255,)

    # 2. Вис-вкрапления — только по металлу и только внутри силуэта.
    for (x, y) in FLECKS.get(name, []):
        for dx, dy, shade in ((0, 0, 1), (1, 0, 2), (0, 1, 0)):
            px, py = x + dx, y + dy
            if inside(px, py) and mat[py][px] == "steel":
                op[px, py] = VIS[shade] + (255,)

    out.save(os.path.join(BASE, name + ".png"))
    return name


def main():
    for name in FLECKS:
        print("32x32", upscale(name))


if __name__ == "__main__":
    main()
