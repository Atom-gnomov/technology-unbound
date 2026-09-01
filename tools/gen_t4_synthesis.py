# -*- coding: utf-8 -*-
"""Ассеты T4 (#23) по арт-правилам (texture-style-rules): пофейсовая
отрисовка, без монотонных градиентов, материал деталями.

- void_iridium: слиток тусклого серебра с «провалом» посередине —
  область темнее фона со сдвинутыми пикселями; очень медленный пульс
  провала — 8 кадров вертикальной ленты + .mcmeta (§8 карточки);
- iridium_wand_cap: иконка наконечника + текстура жезла 32x32 —
  ПЕРЕКРАСКА реального wand_cap_void.png ТК в холодное серебро
  (правило реальных образцов);
- singulator: front/side/top (+active): тумба с вилкой-держателем
  сверху и эмиссивными катушками по бокам.
"""
import io
import json
import os
import random
import zipfile

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")
TC_JAR = (r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\mods"
          r"\Thaumcraft-1.2.8.2-universal.jar")

SILVER = [(0x2A, 0x2C, 0x34), (0x4A, 0x4E, 0x5A), (0x6E, 0x73, 0x82),
          (0x93, 0x99, 0xA8), (0xB8, 0xBE, 0xCC), (0xDD, 0xE2, 0xEC)]
VOIDP = [(0x07, 0x05, 0x0D), (0x12, 0x0C, 0x1E), (0x22, 0x16, 0x38)]
LILAC = (0xC4, 0x92, 0xE8)
STEEL = [(0x1B, 0x1A, 0x22), (0x2E, 0x2C, 0x36), (0x42, 0x3F, 0x4C),
         (0x57, 0x54, 0x62), (0x6E, 0x6B, 0x79), (0x8A, 0x87, 0x95)]
BRASS = [(0x2E, 0x24, 0x0C), (0x50, 0x3E, 0x14), (0x74, 0x59, 0x1E),
         (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52)]


def load_tc(path):
    with zipfile.ZipFile(TC_JAR) as z:
        img = Image.open(io.BytesIO(z.read(path)))
        img.load()
    return img.convert("RGBA")


def ingot_frame(phase):
    """Кадр слитка: контур слитка + «провал» с пульсом фазы 0..7."""
    rnd = random.Random(20260901)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # форма слитка (как у ванильных: параллелограмм)
    for y in range(4, 12):
        x0 = 2 + (11 - y) // 3
        x1 = 13 + (11 - y) // 3 - 2
        for x in range(x0, x1 + 1):
            edge = y in (4, 11) or x in (x0, x1)
            if edge:
                c = SILVER[1]
            else:
                c = SILVER[3] if (x * 3 + y * 7) % 5 else SILVER[2]
                if (x + y) % 7 == 0:
                    c = SILVER[4]
            px[x, y] = c + (255,)
    # светлая верхняя фаска
    for x in range(3, 12):
        if px[x, 5][3]:
            px[x, 5] = SILVER[5] + (255,)
    # «провал»: середины будто нет — тёмная клякса с рваным краем,
    # пульс фазы двигает кромку на 1 px
    grow = 1 if phase in (3, 4, 5) else 0
    for y in range(6, 10):
        for x in range(6 - grow, 10 + grow):
            if px[x, y][3] == 0:
                continue
            i = 0 if (x + y + phase) % 3 else 1
            px[x, y] = VOIDP[i] + (255,)
    # лиловая искра на кромке провала — раз в 4 кадра
    if phase % 4 == 0:
        px[6 - grow, 7] = LILAC + (255,)
    return img


def void_iridium():
    frames = [ingot_frame(f) for f in range(8)]
    strip = Image.new("RGBA", (16, 16 * 8), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, 16 * i))
    items = os.path.join(ROOT, "textures", "items")
    strip.save(os.path.join(items, "void_iridium.png"))
    with open(os.path.join(items, "void_iridium.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 10}}, fh)


def cap_icon():
    """Иконка наконечника: колпачок-полусфера серебром, тёмный зев."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(3, 10):
        half = 5 - abs(6 - y)
        for x in range(8 - half, 8 + half + 1):
            edge = abs(x - 8) == half or y == 3
            c = SILVER[1] if edge else (
                SILVER[4] if (x * 3 + y * 5) % 4 else SILVER[3])
            px[x, y] = c + (255,)
    # зев снизу — тёмный, с лиловой искрой «квантового бардака»
    for x in range(5, 12):
        px[x, 10] = VOIDP[1] + (255,)
        px[x, 11] = SILVER[1] + (255,)
    px[8, 10] = LILAC + (255,)
    # блик
    px[6, 5] = SILVER[5] + (255,)
    items = os.path.join(ROOT, "textures", "items")
    img.save(os.path.join(items, "iridium_wand_cap.png"))


def cap_model():
    """Текстура жезла: перекраска реального wand_cap_void.png ТК в
    холодное серебро с лиловыми остатками пустоты."""
    img = load_tc("assets/thaumcraft/textures/models/wand_cap_void.png")
    px = img.load()
    lo, hi = 255, 0
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            c = px[x, y]
            if c[3] > 8:
                l = (c[0] * 299 + c[1] * 587 + c[2] * 114) // 1000
                lo, hi = min(lo, l), max(hi, l)
    span = max(1, hi - lo)
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            c = px[x, y]
            if c[3] <= 8:
                continue
            l = (c[0] * 299 + c[1] * 587 + c[2] * 114) // 1000
            i = (l - lo) * (len(SILVER) - 1) // span
            base = SILVER[i]
            # лиловый отлив там, где у пустотного был фиолет
            if c[2] > c[0] + 12:
                base = tuple(min(255, v + d) for v, d in
                             zip(base, (10, 0, 24)))
            px[x, y] = base + (c[3],)
    models = os.path.join(ROOT, "textures", "models")
    img.save(os.path.join(models, "wand_cap_iridium.png"))


def machine_face(kind):
    """Грань Сингулятора: kind = front/side/top/side_active/front_active."""
    rnd = random.Random(hash(kind) & 0xFFFF)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = STEEL[1]
            elif y == 1:
                c = STEEL[5]
            elif x == 1:
                c = STEEL[4]
            else:
                c = STEEL[3] if (x * 3 + y * 7) % 5 else STEEL[2]
            px[x, y] = c + (255,)
    active = kind.endswith("_active")
    coil = LILAC if active else (0x3A, 0x33, 0x4A)
    if kind.startswith("side"):
        # эмиссивные катушки: две вертикальные ленты с витками
        for cx in (4, 11):
            for y in range(3, 13):
                px[cx, y] = BRASS[2] + (255,)
                if y % 2 == 0:
                    px[cx, y] = coil + (255,)
        # заклёпки углов
        for (rx, ry) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            px[rx, ry] = STEEL[5] + (255,)
    elif kind.startswith("front"):
        # смотровое окно с дугой к вилке
        for y in range(5, 11):
            for x in range(5, 11):
                px[x, y] = VOIDP[1] + (255,)
        for i in range(4):
            px[6 + i, 9 - i] = coil + (255,)
        for x in range(4, 12):
            px[x, 4] = BRASS[3] + (255,)
            px[x, 11] = BRASS[3] + (255,)
    else:   # top: вилка-держатель
        for x in range(3, 13):
            px[x, 3] = BRASS[2] + (255,)
            px[x, 12] = BRASS[2] + (255,)
        for y in range(3, 13):
            px[3, y] = BRASS[2] + (255,)
            px[12, y] = BRASS[2] + (255,)
        # гнездо жезла в центре
        for y in range(6, 10):
            for x in range(6, 10):
                px[x, y] = VOIDP[0] + (255,)
        px[7, 7] = BRASS[4] + (255,)
        px[8, 8] = BRASS[4] + (255,)
    return img


def singulator():
    blocks = os.path.join(ROOT, "textures", "blocks")
    for kind in ("front", "side", "top", "front_active", "side_active"):
        machine_face(kind).save(
            os.path.join(blocks, "singulator_%s.png" % kind))


def line_face(kind):
    """Грани Патронной Линии: низкий станок, каретка на верхней грани."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = STEEL[1]
            elif y == 1:
                c = STEEL[5]
            elif x == 1:
                c = STEEL[4]
            else:
                c = STEEL[3] if (x * 3 + y * 7) % 5 else STEEL[2]
            px[x, y] = c + (255,)
    active = kind.endswith("_active")
    if kind.startswith("top"):
        # направляющие каретки + сама каретка (в active сдвинута)
        for x in range(2, 14):
            px[x, 5] = BRASS[2] + (255,)
            px[x, 10] = BRASS[2] + (255,)
        cx = 10 if active else 4
        for y in range(4, 12):
            for x in range(cx, cx + 3):
                px[x, y] = STEEL[5] + (255,)
        px[cx + 1, 7] = (0xE0, 0x64, 0x1F, 255)
    elif kind.startswith("front"):
        # лоток выдачи и щель пресса
        for x in range(3, 13):
            px[x, 6] = VOIDP[1] + (255,)
            px[x, 7] = VOIDP[0] + (255,)
        for x in range(3, 13, 2):
            px[x, 11] = BRASS[3] + (255,)
        if active:
            px[7, 6] = (0xE0, 0x64, 0x1F, 255)
            px[8, 7] = (0xE0, 0x64, 0x1F, 255)
    else:
        # боковина: заклёпки и патронная лента-барельеф
        for (rx, ry) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            px[rx, ry] = STEEL[5] + (255,)
        for x in range(4, 12, 2):
            px[x, 8] = BRASS[4] + (255,)
            px[x, 9] = BRASS[1] + (255,)
    return img


def cartridge_line():
    blocks = os.path.join(ROOT, "textures", "blocks")
    for kind in ("front", "side", "top", "front_active", "top_active"):
        line_face(kind).save(
            os.path.join(blocks, "cartridge_line_%s.png" % kind))


def vis_edge_icon():
    """Тонкая полоса металла с фиолетовой кромкой (§8 карточки)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i in range(11):
        x = 2 + i
        y = 12 - i
        px[x, y] = SILVER[3] + (255,)
        px[x + 1, y] = SILVER[2] + (255,)
        # кромка — лиловое свечение сверху
        px[x, y - 1] = LILAC + (255,)
        if i % 3 == 0:
            px[x, y - 2] = (0x8A, 0x64, 0xB4, 255)
    px[2, 13] = SILVER[1] + (255,)
    px[3, 13] = SILVER[1] + (255,)
    items = os.path.join(ROOT, "textures", "items")
    img.save(os.path.join(items, "vis_edge.png"))


def belt_icon():
    """Лента: дуга патронов в латунных звеньях."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i in range(6):
        x = 2 + i * 2
        y = 5 + (i % 2)
        # гильза
        px[x, y] = BRASS[4] + (255,)
        px[x, y + 1] = BRASS[3] + (255,)
        px[x, y + 2] = BRASS[2] + (255,)
        px[x + 1, y + 1] = BRASS[1] + (255,)
        # звено
        px[x, y + 3] = STEEL[3] + (255,)
        px[x + 1, y + 3] = STEEL[4] + (255,)
    for x in range(2, 14):
        px[x, 10] = STEEL[2] + (255,)
        px[x, 11] = STEEL[1] + (255,)
    items = os.path.join(ROOT, "textures", "items")
    img.save(os.path.join(items, "cartridge_belt.png"))


def write_json(path, data):
    with open(path, "w") as fh:
        json.dump(data, fh, indent=2)


def machine_blockstate(name):
    bs = {"variants": {}}
    for active in ("false", "true"):
        suffix = "_active" if active == "true" else ""
        for facing, rot in (("north", 0), ("east", 90),
                            ("south", 180), ("west", 270)):
            entry = {"model": "unboundtech:" + name + suffix}
            if rot:
                entry["y"] = rot
            bs["variants"]["active=%s,facing=%s" % (active, facing)] = entry
    write_json(os.path.join(ROOT, "blockstates", name + ".json"), bs)


def models():
    machine_blockstate("singulator")
    for suffix in ("", "_active"):
        write_json(os.path.join(ROOT, "models", "block",
                                "singulator%s.json" % suffix),
                   {"parent": "block/cube",
                    "textures": {
                        "particle": "unboundtech:blocks/singulator_side" + suffix,
                        "down": "unboundtech:blocks/machine_bottom",
                        "up": "unboundtech:blocks/singulator_top",
                        "north": "unboundtech:blocks/singulator_front" + suffix,
                        "south": "unboundtech:blocks/singulator_side" + suffix,
                        "east": "unboundtech:blocks/singulator_side" + suffix,
                        "west": "unboundtech:blocks/singulator_side" + suffix,
                    }})
    write_json(os.path.join(ROOT, "models", "item", "singulator.json"),
               {"parent": "unboundtech:block/singulator"})
    machine_blockstate("cartridge_line")
    for suffix in ("", "_active"):
        top = "unboundtech:blocks/cartridge_line_top" + suffix
        front = "unboundtech:blocks/cartridge_line_front" + suffix
        write_json(os.path.join(ROOT, "models", "block",
                                "cartridge_line%s.json" % suffix),
                   {"parent": "block/cube",
                    "textures": {
                        "particle": "unboundtech:blocks/cartridge_line_side",
                        "down": "unboundtech:blocks/machine_bottom",
                        "up": top,
                        "north": front,
                        "south": "unboundtech:blocks/cartridge_line_side",
                        "east": "unboundtech:blocks/cartridge_line_side",
                        "west": "unboundtech:blocks/cartridge_line_side",
                    }})
    write_json(os.path.join(ROOT, "models", "item", "cartridge_line.json"),
               {"parent": "unboundtech:block/cartridge_line"})
    for name in ("void_iridium", "iridium_wand_cap", "vis_edge",
                 "cartridge_belt"):
        write_json(os.path.join(ROOT, "models", "item", name + ".json"),
                   {"parent": "item/generated",
                    "textures": {"layer0": "unboundtech:items/" + name}})


def main():
    void_iridium()
    cap_icon()
    cap_model()
    singulator()
    cartridge_line()
    vis_edge_icon()
    belt_icon()
    models()
    print("T4: iridium strip x8, cap icon+model, singulator faces, jsons")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
