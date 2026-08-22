# -*- coding: utf-8 -*-
"""Ассеты T3: Флюкс-Конденсатор, Флюкс-Заряд, Таум-Оверклокер.

Текстуры строятся из РЕАЛЬНЫХ ванильных основ (ХФ-2 канона), а не по
памяти, и собраны как смесь двух модов (решение владельца):

  * морда конденсатора — ванильная furnace_front, перекрашенная в сталь
    мода (корпус машины IC2-школы), с латунными болтами по углам и
    врезанной стеклянной колбой флюкс-мути (нота ТК); активная версия
    светится изнутри;
  * Флюкс-Заряд — ванильный ender_pearl: та же сфера и та же раскладка
    теней, но рампа перекрашена из бирюзы в муть, снизу латунная скоба;
  * оверклокер — плата: латунное поле с фиолетовыми дорожками и фиал.

Латунная рампа выведена из ванильного gold_ingot (снятая рампа приглушена),
стальная — общая рампа мода из recolour_from_vanilla.py.
"""
import json
import os
import zipfile

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")

CANDIDATE_JARS = [
    r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\versions\Forge 1.12.2\Forge 1.12.2.jar",
    r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\versions\1.12.2\1.12.2.jar",
]

STEEL = [(0x1B, 0x1A, 0x22), (0x2E, 0x2C, 0x36), (0x42, 0x3F, 0x4C),
         (0x57, 0x54, 0x62), (0x6E, 0x6B, 0x79), (0x8A, 0x87, 0x95),
         (0xA6, 0xA3, 0xB1), (0xC0, 0xBE, 0xC9)]

# Муть: от глухой тени к свечению.
MURK = [(0x1C, 0x0E, 0x2A), (0x2E, 0x17, 0x44), (0x41, 0x22, 0x60),
        (0x56, 0x30, 0x7C), (0x6E, 0x41, 0x9A), (0x88, 0x55, 0xB8),
        (0xA4, 0x6E, 0xD2), (0xC4, 0x92, 0xE8)]

# Латунь: рампа gold_ingot, приглушённая к меди IC2.
BRASS = [(0x2E, 0x24, 0x0C), (0x50, 0x3E, 0x14), (0x74, 0x59, 0x1E),
         (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52)]

GLASS_EDGE = (0x10, 0x0C, 0x16)


def find_jar():
    for path in CANDIDATE_JARS:
        if os.path.isfile(path):
            return path
    return None


def load(zf, path):
    with zf.open(path) as fh:
        img = Image.open(fh)
        img.load()
    return img.convert("RGBA")


def luminance(c):
    return (c[0] * 299 + c[1] * 587 + c[2] * 114) // 1000


def recolour(img, ramp):
    """Перекраска по яркости с растяжкой на фактический диапазон образца."""
    w, h = img.size
    sp = img.load()
    lums = [luminance(sp[x, y]) for y in range(h) for x in range(w) if sp[x, y][3] > 0]
    low, high = min(lums), max(lums)
    span = max(1, high - low)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            c = sp[x, y]
            if c[3] == 0:
                continue
            level = (luminance(c) - low) * (len(ramp) - 1) // span
            op[x, y] = ramp[max(0, min(len(ramp) - 1, level))] + (c[3],)
    return out


def put(img, x, y, colour):
    if 0 <= x < img.size[0] and 0 <= y < img.size[1]:
        img.load()[x, y] = colour + (255,)


def brass_bolt(img, x, y):
    """Латунный болт 2x2 с бликом — деталь IC2-школы."""
    put(img, x, y, BRASS[4])
    put(img, x + 1, y, BRASS[2])
    put(img, x, y + 1, BRASS[2])
    put(img, x + 1, y + 1, BRASS[1])


def condenser_front(furnace, active):
    """Корпус печи в стали мода + латунь + колба мути по центру."""
    face = recolour(furnace, STEEL)
    for (bx, by) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        brass_bolt(face, bx, by)

    # Колба: латунная рама, стеклянный край, внутри муть.
    x0, y0, x1, y1 = 4, 3, 11, 11
    for x in range(x0, x1 + 1):
        put(face, x, y0, BRASS[3])
        put(face, x, y1, BRASS[1])
    for y in range(y0, y1 + 1):
        put(face, x0, y, BRASS[2])
        put(face, x1, y, BRASS[2])
    for x in range(x0 + 1, x1):
        for y in range(y0 + 1, y1):
            edge = x in (x0 + 1, x1 - 1) or y in (y0 + 1, y1 - 1)
            put(face, x, y, GLASS_EDGE if edge else MURK[1])
    inner = [(x, y) for x in range(x0 + 2, x1 - 1) for y in range(y0 + 2, y1 - 1)]
    if active:
        # Клубится: светлеет к центру, два блика.
        cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
        for (x, y) in inner:
            d = abs(x - cx) + abs(y - cy)
            put(face, x, y, MURK[max(2, 7 - int(d))])
        put(face, 7, 6, MURK[7])
        put(face, 8, 8, MURK[6])
    else:
        # Потухла: муть осела на дно, выше пустое тёмное стекло.
        for (x, y) in inner:
            put(face, x, y, MURK[2] if y >= y1 - 3 else MURK[0])
        put(face, 6, y1 - 3, MURK[3])
        put(face, 9, y1 - 2, MURK[3])
    return face


def flux_charge(pearl):
    """Сфера жемчуга в рампе мути + латунная скоба снизу."""
    icon = recolour(pearl, MURK)
    px = icon.load()
    w, h = icon.size
    bottom = max(y for y in range(h) for x in range(w) if px[x, y][3] > 0)
    xs = [x for x in range(w) if px[x, bottom][3] > 0]
    mid = (min(xs) + max(xs)) // 2
    for dx in (-2, -1, 0, 1, 2):
        put(icon, mid + dx, bottom + 1, BRASS[3 if dx % 2 == 0 else 2])
    for dx in (-1, 0, 1):
        put(icon, mid + dx, bottom + 2, BRASS[1])
    return icon


def overclocker():
    """Плата: латунное поле, фиолетовые дорожки, стальной чип, фиал."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (2, 13)
            put(img, x, y, BRASS[0] if edge else BRASS[1])
    # контактные ножки снизу
    for x in (3, 5, 7, 9, 11):
        put(img, x, 14, BRASS[3])
    # дорожки: Г-образные, светящаяся муть
    traces = [((3, 4), (3, 8)), ((3, 8), (6, 8)), ((5, 4), (10, 4)),
              ((10, 4), (10, 6)), ((12, 4), (12, 9)), ((5, 11), (9, 11)),
              ((9, 9), (9, 11))]
    for (xa, ya), (xb, yb) in traces:
        if xa == xb:
            for y in range(min(ya, yb), max(ya, yb) + 1):
                put(img, xa, y, MURK[5])
        else:
            for x in range(min(xa, xb), max(xa, xb) + 1):
                put(img, x, ya, MURK[5])
    # стальной чип по центру с мутным глазком
    for y in range(6, 10):
        for x in range(5, 9):
            put(img, x, y, STEEL[3] if (x + y) % 2 else STEEL[4])
    put(img, 6, 7, MURK[6])
    put(img, 7, 8, MURK[4])
    # фиал в правом нижнем углу поля
    put(img, 12, 10, STEEL[6])
    for y in (11, 12):
        put(img, 11, y, GLASS_EDGE)
        put(img, 13, y, GLASS_EDGE)
        put(img, 12, y, MURK[6 if y == 11 else 3])
    put(img, 12, 13, GLASS_EDGE)
    return img


def coil_front(furnace, vertical, active):
    """Морды конвертеров: печной корпус + латунная катушка поверх мути.

    Генератор — горизонтальные витки (тянет вис), двигатель — вертикальные
    (льёт в узел): силуэты различаются направлением, по правилу арт-гайда.
    """
    face = recolour(furnace, STEEL)
    for (bx, by) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        brass_bolt(face, bx, by)
    x0, y0, x1, y1 = 3, 3, 12, 12
    for x in range(x0, x1 + 1):
        put(face, x, y0, BRASS[3])
        put(face, x, y1, BRASS[1])
    for y in range(y0, y1 + 1):
        put(face, x0, y, BRASS[2])
        put(face, x1, y, BRASS[2])
    for x in range(x0 + 1, x1):
        for y in range(y0 + 1, y1):
            put(face, x, y, MURK[3 if active else 1])
    if vertical:
        for x in range(x0 + 2, x1, 2):
            for y in range(y0 + 1, y1):
                put(face, x, y, BRASS[4 if y % 2 else 2])
    else:
        for y in range(y0 + 2, y1, 2):
            for x in range(x0 + 1, x1):
                put(face, x, y, BRASS[4 if x % 2 else 2])
    if active:
        if vertical:
            for x in range(x0 + 1, x1, 2):
                put(face, x, 7, MURK[6])
                put(face, x, 8, MURK[5])
        else:
            for y in range(y0 + 1, y1, 2):
                put(face, 7, y, MURK[6])
                put(face, 8, y, MURK[5])
    return face


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def blockstate_and_models():
    name = "flux_condenser"
    variants = {}
    for active in ("false", "true"):
        model = "unboundtech:" + name + ("_active" if active == "true" else "")
        for facing, rot in (("north", None), ("east", 90), ("south", 180), ("west", 270)):
            entry = {"model": model}
            if rot:
                entry["y"] = rot
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
    for item in ("flux_charge", "thaumic_overclocker"):
        write_json(os.path.join(ROOT, "models", "item", item + ".json"),
                   {"parent": "item/generated",
                    "textures": {"layer0": "unboundtech:items/" + item}})


def main():
    jar = find_jar()
    if jar is None:
        print("Клиентский jar 1.12.2 не найден — текстуры не тронуты.")
        return 1
    with zipfile.ZipFile(jar) as zf:
        furnace = load(zf, "assets/minecraft/textures/blocks/furnace_front_off.png")
        pearl = load(zf, "assets/minecraft/textures/items/ender_pearl.png")

    blocks = os.path.join(ROOT, "textures", "blocks")
    items = os.path.join(ROOT, "textures", "items")
    condenser_front(furnace.copy(), False).save(
        os.path.join(blocks, "flux_condenser_front.png"))
    condenser_front(furnace.copy(), True).save(
        os.path.join(blocks, "flux_condenser_front_active.png"))
    flux_charge(pearl).save(os.path.join(items, "flux_charge.png"))
    coil_front(furnace.copy(), False, False).save(
        os.path.join(blocks, "thaum_generator_front.png"))
    coil_front(furnace.copy(), False, True).save(
        os.path.join(blocks, "thaum_generator_front_active.png"))
    coil_front(furnace.copy(), True, False).save(
        os.path.join(blocks, "aetheric_engine_front.png"))
    coil_front(furnace.copy(), True, True).save(
        os.path.join(blocks, "aetheric_engine_front_active.png"))
    overclocker().save(os.path.join(items, "thaumic_overclocker.png"))
    blockstate_and_models()
    print("T3: морды из furnace_front, заряд из ender_pearl, плата оверклокера")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
