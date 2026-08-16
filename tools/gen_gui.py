# -*- coding: utf-8 -*-
"""
Фоны GUI двух машин T2.

Геометрия и палитра — по скилу minecraft-basic-texture (~/.claude/skills):
холст 256x256, сама панель 176x166, углы и рамка по его раскладке,
слот 18x18. Цвета оттуда же:
  A #000000  B #555555  C #c6c6c6  D #ffffff  E #373737  F #8b8b8b
"""
import os
from PIL import Image

OUT = os.path.join(r"C:\Users\Game-On-Dp\technology-unbound",
                   "src", "main", "resources", "assets", "unboundtech",
                   "textures", "gui")

A = (0x00, 0x00, 0x00, 255)
B = (0x55, 0x55, 0x55, 255)
C = (0xC6, 0xC6, 0xC6, 255)
D = (0xFF, 0xFF, 0xFF, 255)
E = (0x37, 0x37, 0x37, 255)
F = (0x8B, 0x8B, 0x8B, 255)

W, H = 176, 166

CORNER_TL = [[0, 0, A, A], [0, A, D, D], [A, D, D, D], [A, D, D, D]]
CORNER_TR = [[A, 0, 0, 0], [D, A, 0, 0], [D, C, A, 0], [C, B, B, A]]
CORNER_BL = [[A, D, D, C], [0, A, C, B], [0, 0, A, B], [0, 0, 0, A]]
CORNER_BR = [[B, B, B, A], [B, B, B, A], [B, B, A, 0], [A, A, 0, 0]]

# Порядок сверен по углам скила: у левого края идёт контур и блик
# (CORNER_BL начинается с A,D,D,C), у правого — тень (CORNER_TR кончается
# C,B,B,A). Все четыре массива читаются от края внутрь.
SIDE_L = [A, D, D, C]
SIDE_R = [C, B, B, A]
SIDE_T = [A, D, D, C]
SIDE_B = [C, B, B, A]

SLOT = [
    "EEEEEEEEEEEEEEEEEF",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "EFFFFFFFFFFFFFFFFD",
    "FDDDDDDDDDDDDDDDDD",
]
SLOT_COLOURS = {'A': A, 'B': B, 'C': C, 'D': D, 'E': E, 'F': F}


def panel():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = img.load()
    for y in range(H):
        for x in range(W):
            px[x, y] = C
    # рамка: стороны тянутся, углы накладываются поверх
    for x in range(W):
        for i, col in enumerate(SIDE_T):
            px[x, i] = col
        for i, col in enumerate(SIDE_B):
            px[x, H - 4 + i] = col
    for y in range(H):
        for i, col in enumerate(SIDE_L):
            px[i, y] = col
        for i, col in enumerate(SIDE_R):
            px[W - 4 + i, y] = col
    for dy in range(4):
        for dx in range(4):
            if CORNER_TL[dy][dx]:
                px[dx, dy] = CORNER_TL[dy][dx]
            if CORNER_TR[dy][dx]:
                px[W - 4 + dx, dy] = CORNER_TR[dy][dx]
            if CORNER_BL[dy][dx]:
                px[dx, H - 4 + dy] = CORNER_BL[dy][dx]
            if CORNER_BR[dy][dx]:
                px[W - 4 + dx, H - 4 + dy] = CORNER_BR[dy][dx]
    return img


def slot(img, x0, y0):
    px = img.load()
    for dy, row in enumerate(SLOT):
        for dx, ch in enumerate(row):
            px[x0 - 1 + dx, y0 - 1 + dy] = SLOT_COLOURS[ch]


def player_inventory(img, x0=8, y0=84):
    """Три ряда по девять, зазор в четыре пикселя, затем пояс."""
    for row in range(3):
        for col in range(9):
            slot(img, x0 + col * 18, y0 + row * 18)
    for col in range(9):
        slot(img, x0 + col * 18, y0 + 58)


def gauge(img, x0, y0, w, h, fill, src_x):
    """
    Пустой жёлоб индикатора на панели плюс «полная» полоса-донор в свободной
    части холста (правее панели): код рисует из донора нужную высоту.
    """
    px = img.load()
    for y in range(y0 - 1, y0 + h + 1):
        for x in range(x0 - 1, x0 + w + 1):
            px[x, y] = E
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = F
    for y in range(h):
        for x in range(w):
            shade = fill if (x not in (0, w - 1)) else tuple(
                max(0, c - 40) if i < 3 else c for i, c in enumerate(fill))
            px[src_x + x, y] = shade


def main():
    os.makedirs(OUT, exist_ok=True)

    # --- Фиал-станция: два слота, шкала EU, шкала эссенции ---
    img = panel()
    player_inventory(img)
    slot(img, 44, 35)    # вход
    slot(img, 116, 35)   # выход
    gauge(img, 8, 20, 8, 48, (0x3F, 0xAE, 0xE8, 255), 200)    # EU, голубой
    gauge(img, 160, 20, 8, 48, (0x93, 0x55, 0xBE, 255), 216)  # эссенция, вис
    img.save(os.path.join(OUT, "phial_station.png"))
    print("gui phial_station.png")

    # --- Горелка: слотов нет, две шкалы ---
    img = panel()
    player_inventory(img)
    gauge(img, 8, 20, 8, 48, (0x3F, 0xAE, 0xE8, 255), 200)
    gauge(img, 160, 20, 8, 48, (0xE0, 0x7A, 0x22, 255), 216)  # пламя, оранжевый
    img.save(os.path.join(OUT, "essentia_burner.png"))
    print("gui essentia_burner.png")


if __name__ == "__main__":
    main()
