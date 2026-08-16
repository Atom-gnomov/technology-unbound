# -*- coding: utf-8 -*-
"""Блокстейты, модели и текстуры «морд» двух машин T2."""
import json
import os
from PIL import Image

ROOT = os.path.join(r"C:\Users\Game-On-Dp\technology-unbound",
                    "src", "main", "resources", "assets", "unboundtech")

BLOCKS = ["phial_station", "essentia_burner"]
ROT = {"north": None, "east": 90, "south": 180, "west": 270}


def blockstate(name):
    variants = {}
    for active in ("false", "true"):
        model = "unboundtech:" + name + ("_active" if active == "true" else "")
        for facing, y in ROT.items():
            entry = {"model": model}
            if y:
                entry["y"] = y
            # Ключи вариантов строго АЛФАВИТНЫЕ: 1.12.2 строит строку по
            # ImmutableSortedMap, иначе состояние не резолвится.
            variants["active=%s,facing=%s" % (active, facing)] = entry
    return {"variants": variants}


def model(name, active):
    front = "unboundtech:blocks/%s_front%s" % (name, "_active" if active else "")
    return {
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
    }


P = {
    '1': (0x34, 0x31, 0x3A, 255),
    '2': (0x4B, 0x48, 0x54, 255),
    '3': (0x6B, 0x67, 0x73, 255),
    '4': (0x84, 0x7F, 0x8D, 255),
    'g': (0x2A, 0x3A, 0x3A, 255),   # стекло колонны, тень
    'G': (0x6C, 0x9A, 0x9E, 255),   # стекло колонны, свет
    'p': (0x6A, 0x3F, 0x9E, 255),
    'P': (0x9B, 0x5B, 0xC4, 255),
    'f': (0x7A, 0x2E, 0x12, 255),   # пламя, тень
    'F': (0xE0, 0x7A, 0x22, 255),   # пламя, свет
    'k': (0x1A, 0x18, 0x1E, 255),   # решётка
}

# Фиал-станция: стеклянная колонна с зажимом и иглой (§8 карточки).
PHIAL_OFF = """
2222222222222222
2111111111111112
21333333333333 2
213kkkkkkkkkk312
213kggggggggk312
213kgGGGGGGgk312
213kgG3223Ggk312
213kgG2  2Ggk312
213kgG2  2Ggk312
213kgG3223Ggk312
213kgGGGGGGgk312
213kggggggggk312
213kkkkkkkkkk312
21333333333333 2
2111111111111112
2222222222222222
"""

PHIAL_ON = PHIAL_OFF.replace("G", "P").replace("g", "p")

# Горелка: решётка с пламенем (§8 карточки).
BURNER_OFF = """
2222222222222222
2111111111111112
21333333333333 2
213kkkkkkkkkk312
213k22222222k312
213k2      2k312
213k2      2k312
213k2      2k312
213k2      2k312
213k2      2k312
213k22222222k312
213kkkkkkkkkk312
21333333333333 2
2111111111111112
2222222222222222
2222222222222222
"""

BURNER_ON = """
2222222222222222
2111111111111112
21333333333333 2
213kkkkkkkkkk312
213k22222222k312
213k2  ff  2k312
213k2 fFFf 2k312
213k2 fFFf 2k312
213k2fFFFFf2k312
213k2fFFFFf2k312
213k22222222k312
213kkkkkkkkkk312
21333333333333 2
2111111111111112
2222222222222222
2222222222222222
"""

FACES = {
    "phial_station_front": PHIAL_OFF,
    "phial_station_front_active": PHIAL_ON,
    "essentia_burner_front": BURNER_OFF,
    "essentia_burner_front_active": BURNER_ON,
}


def draw(template):
    rows = [r for r in template.split("\n") if r.strip() != ""]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows[:16]):
        for x, ch in enumerate(row[:16]):
            colour = P.get(ch)
            if colour:
                px[x, y] = colour
            else:
                px[x, y] = P['2']   # пробел внутри корпуса — тот же металл
    return img


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("json ", path)


def main():
    for name in BLOCKS:
        write_json(os.path.join(ROOT, "blockstates", name + ".json"), blockstate(name))
        write_json(os.path.join(ROOT, "models", "block", name + ".json"), model(name, False))
        write_json(os.path.join(ROOT, "models", "block", name + "_active.json"),
                   model(name, True))
        write_json(os.path.join(ROOT, "models", "item", name + ".json"),
                   {"parent": "unboundtech:block/" + name})
    for name, tpl in FACES.items():
        path = os.path.join(ROOT, "textures", "blocks", name + ".png")
        draw(tpl).save(path)
        print("tex  ", path)


if __name__ == "__main__":
    main()
