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


def revolver_plate():
    """Палитра-текстура для 3D-модели: зоны 4x4 по материалам."""
    img = canvas()
    zones = [
        (0, 0, STEEL[1]), (4, 0, STEEL[3]), (8, 0, STEEL[4]), (12, 0, BRASS[3]),
        (0, 4, WOOD[0]), (4, 4, WOOD[1]), (8, 4, WOOD[2]), (12, 4, MURK[2]),
        (0, 8, STEEL[2]), (4, 8, STEEL[0] if len(STEEL) > 5 else STEEL[1]),
        (8, 8, BRASS[1]), (12, 8, MURK[1]),
    ]
    for (zx, zy, c) in zones:
        for y in range(4):
            for x in range(4):
                # лёгкий шум, чтобы грани не были заливкой
                cc = c
                if (x + y) % 3 == 0:
                    cc = tuple(max(0, v - 12) for v in c)
                put(img, zx + x, zy + y, cc)
    return img


# зоны палитры для faces: uv = [x0, y0, x1, y1]
UV_STEEL_D = [0, 0, 4, 4]
UV_STEEL_M = [4, 0, 8, 4]
UV_STEEL_L = [8, 0, 12, 4]
UV_BRASS = [12, 0, 16, 4]
UV_WOOD_D = [0, 4, 4, 8]
UV_WOOD_M = [4, 4, 8, 8]
UV_WOOD_L = [8, 4, 12, 8]
UV_GLOW = [12, 4, 16, 8]
UV_STEEL_F = [0, 8, 4, 12]


def _box(fr, to, uv, uv_ends=None, rotation=None):
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        faces[f] = {"texture": "#plate",
                    "uv": uv_ends if uv_ends and f in ("east", "west") else uv}
    el = {"from": fr, "to": to, "faces": faces}
    if rotation:
        el["rotation"] = rotation
    return el


def revolver_3d():
    """3D-модель (`flux_revolver.md` §8 — решение владельца): короткий
    ствол, крупный барабан со светящимися гнёздами, деревянная рукоять.
    Ствол лежит вдоль X, дуло к нулю; display поворачивает в руках."""
    elements = [
        # ствол
        _box([1, 8, 7], [8, 10, 9], UV_STEEL_M, uv_ends=UV_STEEL_D),
        # мушка
        _box([1, 10, 7.6], [2, 10.7, 8.4], UV_STEEL_L),
        # рама над барабаном и казённик
        _box([7, 9.7, 6.9], [13.2, 10.6, 9.1], UV_STEEL_M),
        # барабан
        _box([7.6, 6.2, 6.2], [11.6, 10.2, 9.8], UV_STEEL_D),
        # светящиеся гнёзда патронов по бокам барабана
        _box([8.4, 7.2, 5.9], [10.8, 9.2, 6.2], UV_GLOW),
        _box([8.4, 7.2, 9.8], [10.8, 9.2, 10.1], UV_GLOW),
        # латунная ось барабана
        _box([7.2, 7.8, 7.6], [12, 8.6, 8.4], UV_BRASS),
        # курок
        _box([13, 10.2, 7.5], [14, 11.4, 8.5], UV_STEEL_F,
             rotation={"origin": [13, 10.2, 8], "axis": "z", "angle": -22.5}),
        # рукоять под углом
        _box([11.8, 2.8, 7.1], [14.2, 8.2, 8.9], UV_WOOD_M,
             rotation={"origin": [13, 8, 8], "axis": "z", "angle": -22.5}),
        # латунный тыльник рукояти
        _box([13.1, 2.2, 7.2], [14.4, 3.4, 8.8], UV_BRASS,
             rotation={"origin": [13, 8, 8], "axis": "z", "angle": -22.5}),
        # спусковая скоба
        _box([9.8, 5.4, 7.7], [12.4, 6.1, 8.3], UV_BRASS),
    ]
    write_json(os.path.join(ROOT, "models", "item", "flux_revolver.json"), {
        "textures": {"particle": "unboundtech:items/flux_revolver_plate",
                     "plate": "unboundtech:items/flux_revolver_plate"},
        "elements": elements,
        "display": {
            "thirdperson_righthand": {
                "rotation": [0, 90, 0], "translation": [0, 3, 1],
                "scale": [0.8, 0.8, 0.8]},
            "thirdperson_lefthand": {
                "rotation": [0, -90, 0], "translation": [0, 3, 1],
                "scale": [0.8, 0.8, 0.8]},
            "firstperson_righthand": {
                "rotation": [0, 85, 0], "translation": [1.5, 1.5, 0],
                "scale": [0.7, 0.7, 0.7]},
            "firstperson_lefthand": {
                "rotation": [0, -85, 0], "translation": [1.5, 1.5, 0],
                "scale": [0.7, 0.7, 0.7]},
            "gui": {
                "rotation": [10, -95, 10], "translation": [0, 0, 0],
                "scale": [0.95, 0.95, 0.95]},
            "ground": {
                "rotation": [0, 0, 0], "translation": [0, 2, 0],
                "scale": [0.5, 0.5, 0.5]},
            "fixed": {
                "rotation": [0, -90, 0], "translation": [0, 0, 0],
                "scale": [1.0, 1.0, 1.0]},
        },
    })


def main():
    items = os.path.join(ROOT, "textures", "items")
    casing().save(os.path.join(items, "casing.png"))
    cartridge(FIRE).save(os.path.join(items, "cartridge_incendiary.png"))
    cartridge([(0xC9, 0xC2, 0xA8), (0xFF, 0xFC, 0xEE), (0xF2, 0xEC, 0xD5)]).save(
        os.path.join(items, "cartridge_illuminating.png"))
    revolver().save(os.path.join(items, "flux_revolver.png"))
    revolver_plate().save(os.path.join(items, "flux_revolver_plate.png"))

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
