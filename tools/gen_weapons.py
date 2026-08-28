# -*- coding: utf-8 -*-
"""Ассеты оружейной линейки T3: гильза, патроны, Флюкс-Револьвер.

Гильза и патроны — рисованные с shade_flat (прецедент чернильницы):
латунный цилиндр, у патрона цветная головка (§8 карточки: цвета —
оранжевый зажигательный, белый осветительный). Револьвер — пиксельный
силуэт «короткий ствол, крупный барабан» в стали мода с латунью;
полноценная 3D-модель — отдельной задачей (§8: решение владельца).
"""
import json
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")

STEEL = [(0x1B, 0x1A, 0x22), (0x2E, 0x2C, 0x36), (0x42, 0x3F, 0x4C),
         (0x57, 0x54, 0x62), (0x6E, 0x6B, 0x79), (0x8A, 0x87, 0x95)]
BRASS = [(0x2E, 0x24, 0x0C), (0x50, 0x3E, 0x14), (0x74, 0x59, 0x1E),
         (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52)]
MURK = [(0x41, 0x22, 0x60), (0x6E, 0x41, 0x9A), (0xA4, 0x6E, 0xD2)]
FIRE = [(0xB3, 0x3B, 0x14), (0xE0, 0x6B, 0x1F), (0xF7, 0xB0, 0x3C)]
LIGHT = [(0xC9, 0xC2, 0xA8), (0xEDE, 0xE6, 0xC9), (0xFF, 0xFC, 0xEE)]
WOOD = [(0x3E, 0x2A, 0x18), (0x5C, 0x40, 0x24), (0x7A, 0x57, 0x33)]


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.load()[x, y] = (c[0] & 255, c[1] & 255, c[2] & 255, 255)


def shell(img, x0, y0, y1, head=None):
    """Вертикальная гильза шириной 4: тень слева, блик справа-центр."""
    for y in range(y0, y1 + 1):
        put(img, x0, y, BRASS[1])
        put(img, x0 + 1, y, BRASS[4])
        put(img, x0 + 2, y, BRASS[3])
        put(img, x0 + 3, y, BRASS[1])
    # закраина снизу
    for x in range(x0 - 1, x0 + 5):
        put(img, x, y1 + 1, BRASS[2])
    if head:
        for y in range(y0 - 3, y0):
            put(img, x0, y, head[0])
            put(img, x0 + 1, y, head[2])
            put(img, x0 + 2, y, head[1])
            put(img, x0 + 3, y, head[0])
        put(img, x0 + 1, y0 - 4, head[1])
        put(img, x0 + 2, y0 - 4, head[1])


def casing():
    img = canvas()
    shell(img, 6, 5, 12)
    # пустое жерло
    put(img, 7, 4, (0x14, 0x11, 0x1A))
    put(img, 8, 4, (0x14, 0x11, 0x1A))
    put(img, 6, 4, BRASS[2])
    put(img, 9, 4, BRASS[2])
    return img


def cartridge(head):
    img = canvas()
    shell(img, 6, 7, 13, head=head)
    return img


def revolver():
    """Вид сбоку, ствол влево: короткий ствол, крупный барабан, рукоять."""
    img = canvas()
    # ствол
    for x in range(1, 7):
        put(img, x, 5, STEEL[4])
        put(img, x, 6, STEEL[2])
    put(img, 1, 4, STEEL[3])
    # рама
    for x in range(6, 12):
        put(img, x, 4, STEEL[3])
    # барабан с латунными гнёздами
    for y in range(4, 9):
        for x in range(6, 11):
            d2 = (x - 8) ** 2 + (y - 6) ** 2
            if d2 <= 4:
                put(img, x, y, STEEL[1] if d2 > 2 else BRASS[3])
    put(img, 8, 6, MURK[2])   # заряженное гнездо светится
    # курок
    put(img, 11, 3, STEEL[4])
    put(img, 12, 4, STEEL[3])
    # рукоять из дерева, вниз-вправо
    for i in range(5):
        y = 8 + i
        put(img, 10 + i // 2, y, WOOD[1])
        put(img, 11 + i // 2, y, WOOD[2])
        put(img, 12 + i // 2, y, WOOD[0])
    # спусковая скоба
    put(img, 8, 9, BRASS[2])
    put(img, 8, 10, BRASS[2])
    put(img, 9, 10, BRASS[2])
    return img


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def revolver_map():
    """Текстурная карта 128x64 для Java-модели (школа Flan's Mod):
    полосы материалов, развёртки боксов ложатся в свою полосу.
      y 0..30  — сталь (шум панелей)
      y 32..46 — дерево рукояти
      y 48..56 — латунь
      y 58..64 — свечение флюкса"""
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    px = img.load()
    for y in range(64):
        for x in range(128):
            if y < 31:
                c = STEEL[2]
                if (x * 7 + y * 13) % 11 == 0:
                    c = STEEL[3]
                if (x + y) % 9 == 0:
                    c = STEEL[1]
                if y % 10 == 9:
                    c = STEEL[0]
            elif y < 47:
                c = WOOD[1]
                if (x * 3 + y) % 5 == 0:
                    c = WOOD[2]
                if (x + y * 2) % 11 == 0:
                    c = WOOD[0]
            elif y < 57:
                c = BRASS[3]
                if (x + y) % 4 == 0:
                    c = BRASS[4]
                if (x * 5 + y) % 13 == 0:
                    c = BRASS[1]
            else:
                c = MURK[2]
                if (x + y) % 3 == 0:
                    c = MURK[1]
            px[x, y] = (c[0] & 255, c[1] & 255, c[2] & 255, 255)
    return img


def revolver_3d():
    """Модель — Java (ModelFluxRevolver) через TEISR; json даёт только
    display-повороты. parent builtin/entity — как щит и сундук ванилы."""
    write_json(os.path.join(ROOT, "models", "item", "flux_revolver.json"), {
        "parent": "builtin/entity",
        "textures": {"particle": "unboundtech:items/flux_revolver"},
        "display": {
            "thirdperson_righthand": {
                "rotation": [0, 90, 0], "translation": [0, 2.5, 1],
                "scale": [0.55, 0.55, 0.55]},
            "thirdperson_lefthand": {
                "rotation": [0, -90, 0], "translation": [0, 2.5, 1],
                "scale": [0.55, 0.55, 0.55]},
            "firstperson_righthand": {
                "rotation": [0, 95, 0], "translation": [1, 1.5, 0],
                "scale": [0.5, 0.5, 0.5]},
            "firstperson_lefthand": {
                "rotation": [0, -95, 0], "translation": [1, 1.5, 0],
                "scale": [0.5, 0.5, 0.5]},
            "gui": {
                "rotation": [0, 0, 0], "translation": [1, 0.5, 0],
                "scale": [0.8, 0.8, 0.8]},
            "ground": {
                "rotation": [0, 0, 0], "translation": [0, 2, 0],
                "scale": [0.4, 0.4, 0.4]},
            "fixed": {
                "rotation": [0, 0, 0], "translation": [0, 0, 0],
                "scale": [0.9, 0.9, 0.9]},
        },
    })


def main():
    items = os.path.join(ROOT, "textures", "items")
    casing().save(os.path.join(items, "casing.png"))
    cartridge(FIRE).save(os.path.join(items, "cartridge_incendiary.png"))
    cartridge([(0xC9, 0xC2, 0xA8), (0xFF, 0xFC, 0xEE), (0xF2, 0xEC, 0xD5)]).save(
        os.path.join(items, "cartridge_illuminating.png"))
    revolver().save(os.path.join(items, "flux_revolver.png"))
    models_dir = os.path.join(ROOT, "textures", "models")
    os.makedirs(models_dir, exist_ok=True)
    revolver_map().save(os.path.join(models_dir, "flux_revolver.png"))

    for name in ("casing", "cartridge_incendiary", "cartridge_illuminating"):
        write_json(os.path.join(ROOT, "models", "item", name + ".json"),
                   {"parent": "item/generated",
                    "textures": {"layer0": "unboundtech:items/" + name}})
    revolver_3d()

    # невидимый свет: пустая модель, чтобы лог не ругался на блокстейт
    write_json(os.path.join(ROOT, "blockstates", "photon_light.json"),
               {"variants": {"normal": {"model": "unboundtech:photon_light"}}})
    write_json(os.path.join(ROOT, "models", "block", "photon_light.json"),
               {"textures": {}, "elements": []})

    print("оружейка T3: гильза, два патрона, револьвер")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
