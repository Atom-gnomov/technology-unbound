# -*- coding: utf-8 -*-
"""Блокстейт, модели и «морды» Флюкс-Конденсатора + иконка Флюкс-Заряда.

Морда — по арт-гайду и карточке §8: корпус линейки машин, на лицевой грани
стеклянная колба с фиолетовой мутью; активная версия — муть клубится и
светится. «Грязное» фиолетовое свечение — маркер силуэта.
"""
import json
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "unboundtech")

P = {
    '1': (0x34, 0x31, 0x3A, 255),
    '2': (0x4B, 0x48, 0x54, 255),
    '3': (0x6B, 0x67, 0x73, 255),
    'k': (0x1A, 0x18, 0x1E, 255),
    'g': (0x2A, 0x24, 0x33, 255),   # стекло колбы, тень
    'G': (0x57, 0x4E, 0x66, 255),   # стекло колбы, свет
    'f': (0x3A, 0x1E, 0x52, 255),   # муть, тень
    'F': (0x6A, 0x3F, 0x9E, 255),   # муть
    'X': (0x9B, 0x5B, 0xC4, 255),   # муть, свечение (актив)
    'b': (0x17, 0x4A, 0x74, 255),   # заряд фиала, тень
    'B': (0x3F, 0xAE, 0xE8, 255),   # заряд фиала, свет
}

FRONT_OFF = """
2222222222222222
2111111111111112
2133333333333312
213kkkkkkkkkk312
213kgGGGGGGgk312
213kgg    ggk312
213kg  ff  gk312
213kg ffff gk312
213kg  ff  gk312
213kgg    ggk312
213kgGGGGGGgk312
213kkkkkkkkkk312
2133333333333312
2111111111111112
2222222222222222
2222222222222222
"""

FRONT_ON = """
2222222222222222
2111111111111112
2133333333333312
213kkkkkkkkkk312
213kgGGGGGGgk312
213kgfFXXFfgk312
213kgFXXXXFgk312
213kgXXFFXXgk312
213kgXXFFXXgk312
213kgFXXXXFgk312
213kgfFXXFfgk312
213kkkkkkkkkk312
2133333333333312
2111111111111112
2222222222222222
2222222222222222
"""

# Оверклокер: плата с дорожками (силуэт апгрейда IC2), тёмно-фиолетовая
# подложка, светящиеся дорожки, маленький фиал в углу (§8 карточки).
OVERCLOCKER = """
................
.kkkkkkkkkkkkk..
.kffffffffffFk..
.kfXXfXXXfXfFk..
.kffXffXffXfFk..
.kfXXfXXXfXfFk..
.kffffffffffFk..
.kfXfXXfXXXfFk..
.kfXffXffXffFk..
.kfXfXXXfXXfFk..
.kffffffkkkkkk..
.kfXXfXfkgGgk...
.kffffffkgGgk...
.kFFFFFFkbBbk...
.kkkkkkkkkkkk...
................
"""

# Иконка Флюкс-Заряда: гранёный сгусток мути в металлической оправе.
CHARGE = """
................
................
.....kkkkk......
....kFXXXFk.....
...kFXXXXXFk....
..kFXXfXXXXFk...
..kXXffXXXXXk...
..kXXfXXXXfXk...
..kXXXXXXffXk...
..kFXXXXffXFk...
...kFXXXXXFk....
....kFXXXFk.....
.....k121k......
.....k121k......
......kkk.......
................
"""


def draw(template, size=16):
    rows = [r for r in template.split("\n") if r.strip() != ""]
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows[:size]):
        row = (row + "." * size)[:size]
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            px[x, y] = P.get(ch if ch != ' ' else 'g', P['2'])
    return img


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main():
    name = "flux_condenser"
    variants = {}
    for active in ("false", "true"):
        model = "unboundtech:" + name + ("_active" if active == "true" else "")
        for facing, y in (("north", None), ("east", 90), ("south", 180), ("west", 270)):
            entry = {"model": model}
            if y:
                entry["y"] = y
            # Ключи вариантов строго алфавитные — иначе 1.12.2 их не резолвит.
            variants["active=%s,facing=%s" % (active, facing)] = entry
    write_json(os.path.join(ROOT, "blockstates", name + ".json"), {"variants": variants})

    for active in (False, True):
        front = "unboundtech:blocks/%s_front%s" % (name, "_active" if active else "")
        write_json(os.path.join(ROOT, "models", "block",
                                name + ("_active" if active else "") + ".json"), {
            "parent": "block/cube",
            "textures": {
                "particle": "unboundtech:blocks/machine_side",
                "down": "unboundtech:blocks/machine_bottom",
                "up": "unboundtech:blocks/machine_top",
                "north": front,
                "south": "unboundtech:blocks/machine_side",
                "east": "unboundtech:blocks/machine_side",
                "west": "unboundtech:blocks/machine_side",
            },
        })
    write_json(os.path.join(ROOT, "models", "item", name + ".json"),
               {"parent": "unboundtech:block/" + name})
    write_json(os.path.join(ROOT, "models", "item", "flux_charge.json"),
               {"parent": "item/generated",
                "textures": {"layer0": "unboundtech:items/flux_charge"}})

    draw(FRONT_OFF).save(os.path.join(ROOT, "textures", "blocks", name + "_front.png"))
    draw(FRONT_ON).save(os.path.join(ROOT, "textures", "blocks", name + "_front_active.png"))
    draw(CHARGE).save(os.path.join(ROOT, "textures", "items", "flux_charge.png"))
    draw(OVERCLOCKER).save(os.path.join(ROOT, "textures", "items",
                                        "thaumic_overclocker.png"))
    write_json(os.path.join(ROOT, "models", "item", "thaumic_overclocker.json"),
               {"parent": "item/generated",
                "textures": {"layer0": "unboundtech:items/thaumic_overclocker"}})
    print("flux_condenser: блокстейт, модели, морды; flux_charge + overclocker: иконки")


if __name__ == "__main__":
    main()
