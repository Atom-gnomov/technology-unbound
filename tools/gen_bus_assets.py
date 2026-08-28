# -*- coding: utf-8 -*-
"""Ассеты шины эссенции и Накопителя (T3).

Стиль тот же, что в gen_blocks_t3.py: сталь мода + латунь IC2-школы +
фиолетовая нота ТК. Кабель — тонкий, с жилами на торце (2/4/8 — тир
читается глазом, §8 карточки); корпус накопителя — глухая плита с
заклёпками; контроллер — окно и индикаторная полоса.
"""
import json
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")

STEEL = [(0x1B, 0x1A, 0x22), (0x2E, 0x2C, 0x36), (0x42, 0x3F, 0x4C),
         (0x57, 0x54, 0x62), (0x6E, 0x6B, 0x79), (0x8A, 0x87, 0x95),
         (0xA6, 0xA3, 0xB1), (0xC0, 0xBE, 0xC9)]
MURK = [(0x1C, 0x0E, 0x2A), (0x2E, 0x17, 0x44), (0x41, 0x22, 0x60),
        (0x56, 0x30, 0x7C), (0x6E, 0x41, 0x9A), (0x88, 0x55, 0xB8),
        (0xA4, 0x6E, 0xD2), (0xC4, 0x92, 0xE8)]
BRASS = [(0x2E, 0x24, 0x0C), (0x50, 0x3E, 0x14), (0x74, 0x59, 0x1E),
         (0x96, 0x74, 0x2A), (0xB4, 0x8E, 0x3C), (0xD0, 0xAC, 0x52)]
GLASS = (0x8F, 0xB8, 0xC9)


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.load()[x, y] = c + (255,)


def fill(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, c)


def plate(base_shade=2):
    """Глухая плита закалённого таумия: панель с фаской и шумом."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = STEEL[base_shade]
            if (x * 7 + y * 13) % 11 == 0:
                c = STEEL[base_shade + 1]
            if x == 0 or y == 0:
                c = STEEL[base_shade + 2]
            if x == 15 or y == 15:
                c = STEEL[max(0, base_shade - 2)]
            put(img, x, y, c)
    return img


def rivets(img):
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(img, x, y, BRASS[4])
        put(img, x + (1 if x < 8 else -1), y, BRASS[2])
    return img


def vault_casing():
    img = rivets(plate())
    # фиолетовая печать-полка по центру: библиотека, а не просто ящик
    fill(img, 6, 6, 9, 9, STEEL[1])
    fill(img, 7, 7, 8, 8, MURK[3])
    return img


def vault_controller_front(active):
    img = rivets(plate())
    # окно: латунная рама, за стеклом полки с эссенцией
    fill(img, 4, 3, 11, 9, BRASS[2])
    fill(img, 5, 4, 10, 8, (0x10, 0x0C, 0x16))
    inner = MURK[5] if active else MURK[1]
    for i, x in enumerate(range(6, 10)):
        fill(img, x, 7 - i % 2, x, 8, inner)
    # индикаторная полоса
    band = (0x3E, 0xC8, 0x52) if active else (0xB0, 0x30, 0x30)
    fill(img, 3, 12, 12, 12, band)
    return img


def vault_golem_port_front():
    img = rivets(plate())
    # ниша с полкой: тёмный проём, латунный порожек
    fill(img, 4, 4, 11, 11, (0x14, 0x11, 0x1A))
    fill(img, 5, 5, 10, 10, (0x0C, 0x0A, 0x10))
    fill(img, 4, 11, 11, 11, BRASS[3])
    put(img, 7, 8, MURK[4])
    put(img, 8, 7, MURK[3])
    return img


def bus_node_side(active):
    """Коробка узла: сталь, латунный патрубок-кольцо по центру стороны."""
    img = plate(1)
    for y in range(16):
        for x in range(16):
            d2 = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if 3.2 ** 2 <= d2 <= 5.0 ** 2:
                put(img, x, y, BRASS[4 if (x + y) % 3 else 2])
            elif d2 < 3.2 ** 2:
                put(img, x, y, MURK[4] if active else (0x14, 0x11, 0x1A))
    band = (0x3E, 0xC8, 0x52) if active else (0x2E, 0x6E, 0xB0)
    for x in range(2, 5):
        put(img, x, 1, band)
    return img


def conduit_braid():
    """Оплётка кабеля: тёмная сталь с диагональной латунной нитью."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = STEEL[1] if (x + y) % 4 else BRASS[2]
            if (x * 3 + y) % 7 == 0:
                c = STEEL[2]
            put(img, x, y, c)
    return img


def conduit_end(channels):
    """Торец: стеклянная жила, внутри 2/4/8 светящихся рядов."""
    img = canvas()
    fill(img, 0, 0, 15, 15, STEEL[1])
    fill(img, 4, 4, 11, 11, BRASS[1])
    fill(img, 5, 5, 10, 10, (0x10, 0x0C, 0x16))
    spots = {
        2: [(7, 7), (8, 8)],
        4: [(6, 6), (9, 6), (6, 9), (9, 9)],
        8: [(6, 6), (8, 6), (6, 8), (8, 8), (7, 7), (9, 7), (7, 9), (9, 9)],
    }[channels]
    for (x, y) in spots:
        put(img, x, y, MURK[6])
    return img


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def conduit_assets(name):
    core = "unboundtech:block/%s_core" % name
    arm = "unboundtech:block/%s_arm" % name
    multipart = [{"apply": {"model": core}}]
    for when, extra in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}),
                        ("west", {"y": 270}), ("up", {"x": -90}), ("down", {"x": 90})):
        entry = {"model": arm}
        entry.update(extra)
        multipart.append({"when": {when: "true"}, "apply": entry})
    write_json(os.path.join(ROOT, "blockstates", name + ".json"),
               {"multipart": multipart})
    braid = "unboundtech:blocks/conduit_braid"
    end = "unboundtech:blocks/%s_end" % name
    write_json(os.path.join(ROOT, "models", "block", name + "_core.json"), {
        "textures": {"particle": braid, "braid": braid, "end": end},
        "elements": [{
            "from": [5, 5, 5], "to": [11, 11, 11],
            "faces": {f: {"texture": "#end"} for f in
                      ("north", "south", "east", "west", "up", "down")},
        }],
    })
    write_json(os.path.join(ROOT, "models", "block", name + "_arm.json"), {
        "textures": {"particle": braid, "braid": braid},
        "elements": [{
            "from": [5, 5, 0], "to": [11, 11, 5],
            "faces": {f: {"texture": "#braid"} for f in
                      ("north", "south", "east", "west", "up", "down")},
        }],
    })
    write_json(os.path.join(ROOT, "models", "item", name + ".json"),
               {"parent": "unboundtech:block/" + name + "_core"})


def simple_block_assets(name, model_textures):
    write_json(os.path.join(ROOT, "blockstates", name + ".json"),
               {"variants": {"normal": {"model": "unboundtech:" + name}}})
    write_json(os.path.join(ROOT, "models", "block", name + ".json"), model_textures)
    write_json(os.path.join(ROOT, "models", "item", name + ".json"),
               {"parent": "unboundtech:block/" + name})


def machine_assets(name, front_active_pairs):
    """Блокстейт facing x active для блоков на BlockMachineBase."""
    variants = {}
    for active in ("false", "true"):
        model = "unboundtech:" + name + ("_active" if active == "true" else "")
        for facing, rot in (("north", None), ("east", 90), ("south", 180), ("west", 270)):
            entry = {"model": model}
            if rot:
                entry["y"] = rot
            variants["active=%s,facing=%s" % (active, facing)] = entry
    write_json(os.path.join(ROOT, "blockstates", name + ".json"), {"variants": variants})
    for active, textures in front_active_pairs:
        write_json(os.path.join(ROOT, "models", "block",
                                name + ("_active" if active else "") + ".json"),
                   {"parent": "block/cube", "textures": textures})
    write_json(os.path.join(ROOT, "models", "item", name + ".json"),
               {"parent": "unboundtech:block/" + name})


def main():
    blocks = os.path.join(ROOT, "textures", "blocks")
    os.makedirs(blocks, exist_ok=True)

    vault_casing().save(os.path.join(blocks, "essentia_vault_casing.png"))
    vault_controller_front(False).save(
        os.path.join(blocks, "essentia_vault_controller_front.png"))
    vault_controller_front(True).save(
        os.path.join(blocks, "essentia_vault_controller_front_active.png"))
    vault_golem_port_front().save(
        os.path.join(blocks, "essentia_vault_golem_port_front.png"))
    bus_node_side(False).save(os.path.join(blocks, "bus_node_side.png"))
    bus_node_side(True).save(os.path.join(blocks, "bus_node_side_active.png"))
    conduit_braid().save(os.path.join(blocks, "conduit_braid.png"))
    for name, ch in (("essentia_conduit_i", 2), ("essentia_conduit_ii", 4),
                     ("essentia_conduit_iii", 8)):
        conduit_end(ch).save(os.path.join(blocks, name + "_end.png"))
        conduit_assets(name)

    casing_tex = "unboundtech:blocks/essentia_vault_casing"
    simple_block_assets("essentia_vault_casing", {
        "parent": "block/cube_all", "textures": {"all": casing_tex}})
    simple_block_assets("essentia_vault_golem_port", {
        "parent": "block/cube", "textures": {
            "particle": casing_tex,
            "north": "unboundtech:blocks/essentia_vault_golem_port_front",
            "south": casing_tex, "east": casing_tex, "west": casing_tex,
            "up": casing_tex, "down": casing_tex}})
    machine_assets("essentia_vault_controller", [
        (False, {"particle": casing_tex,
                 "north": "unboundtech:blocks/essentia_vault_controller_front",
                 "south": casing_tex, "east": casing_tex, "west": casing_tex,
                 "up": casing_tex, "down": casing_tex}),
        (True, {"particle": casing_tex,
                "north": "unboundtech:blocks/essentia_vault_controller_front_active",
                "south": casing_tex, "east": casing_tex, "west": casing_tex,
                "up": casing_tex, "down": casing_tex}),
    ])
    node = "unboundtech:blocks/bus_node_side"
    node_active = "unboundtech:blocks/bus_node_side_active"

    def node_model(tex):
        return {"textures": {"particle": tex, "side": tex},
                "elements": [{
                    "from": [2, 2, 2], "to": [14, 14, 14],
                    "faces": {f: {"texture": "#side"} for f in
                              ("north", "south", "east", "west", "up", "down")},
                }]}

    variants = {}
    for active in ("false", "true"):
        model = "unboundtech:bus_node" + ("_active" if active == "true" else "")
        for facing in ("north", "east", "south", "west"):
            variants["active=%s,facing=%s" % (active, facing)] = {"model": model}
    write_json(os.path.join(ROOT, "blockstates", "bus_node.json"),
               {"variants": variants})
    write_json(os.path.join(ROOT, "models", "block", "bus_node.json"),
               node_model(node))
    write_json(os.path.join(ROOT, "models", "block", "bus_node_active.json"),
               node_model(node_active))
    write_json(os.path.join(ROOT, "models", "item", "bus_node.json"),
               {"parent": "unboundtech:block/bus_node"})

    print("шина: узел, кабели I/II/III (жилы на торце), корпус/контроллер/порт")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
