# -*- coding: utf-8 -*-
"""Текстура Нано-Таум брони v3: реальные элементы IC2 на фортресс-основе.

Уроки двух примерок владельца, оба вшиты сюда:
 1. UV нормализованы — размер файла (256x128) и плоскость (128x64)
    НЕПРИКОСНОВЕННЫ; любые правки только внутри.
 2. «Свободные по альфе» зоны бипеда — не свободны: бипед-части рисуют их
    на теле (пятна на лице первой примерки). Патчи новых боксов живут
    ТОЛЬКО вне бипед-зон (юниты 0..64 x 0..32) и вне фортресс-занятости.

Состав (решение владельца: ассеты обоих модов — материал, всё с образца):
 - база: фортресс-развёртка, перекрашенная (нано-карбон + таумий);
 - бипед-зоны: РОДНЫЕ пиксели nano_1.png IC2 — настоящий нано-костюм со
   светящимися жилами;
 - очки: маска nightvision_1.png IC2 в зоне фортрессовского бокса Goggles
   (свои кривые линзы-боксы удалены);
 - паулдроны: обтянуты фрагментом КОВАНОЙ фортресс-пластины (таум-стиль);
 - лезвия, лампы, кабели, подсумки, сабатоны — патчи по карте дыр.
"""
import io
import os
import zipfile

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech")

TC_JARS = [
    r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\mods\Thaumcraft-1.2.8.2-universal.jar",
    os.path.join(REPO, "libs", "Thaumcraft-1.2.8.1-dev.jar"),
]
IC2_JARS = [
    r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\mods\industrialcraft-2-2.8.222-ex112.jar",
]

# ==== параметры ПО ВЕРДИКТУ спора критиков (2026-08-23) ====
# П.1: жилы и ПНВ — родной зелёный IC2, перекраска запрещена.
IC2_GLOW_OVERRIDE = None
# П.2: паулдроны 6x4x5 (7 не влезает в свободные UV-дыры; свес и наклон
# ±10 градусов дают нужный силуэт, арбитр: «решает свес, не объём»).
PAULDRON = (6, 4, 5)
# П.3: лезвия отложены в v2 консенсусом 3:0 («разрешение, не ТЗ»).
BLADES = False
# П.0/П.5: голубой на костюме — ВЕТО; эмиссив только зелёный + фиолетовый.
# ============================================================

# Рампы — числа вердикта (п.4): нано-чёрный не трогать, таум вверх.
NANO = [(0x0C, 0x0C, 0x10), (0x12, 0x12, 0x16), (0x18, 0x18, 0x20),
        (0x1E, 0x1E, 0x24), (0x26, 0x26, 0x2E), (0x2E, 0x2E, 0x38)]
TEMPER = [(0x4A, 0x3A, 0x66), (0x5A, 0x46, 0x79), (0x6B, 0x4F, 0x94),
          (0x7E, 0x63, 0xA8), (0x9B, 0x7B, 0xD4), (0xC9, 0xA9, 0xF5)]
GLOW = (0xC9, 0xA9, 0xF5)
GREEN = (0x00, 0xFF, 0x00)
GREEN_RIM = (0x1E, 0x6B, 0x1E)
STEEL_BLADE = [(0x2A, 0x2D, 0x33), (0x6E, 0x74, 0x80), (0xC8, 0xCD, 0xD6)]
BRASS = [(0x8A, 0x64, 0x28), (0xC8, 0x9B, 0x3C), (0xF2, 0xD0, 0x7A)]

# Дыры вне бипеда, фортресс-занятости и зоны ПНВ-маски (скан v3).
UV = {
    "pauldron": (76, 21),
    "sabaton": (12, 39),
    "chestCable": (120, 0),
    "armCable": (84, 8),
    "pouch": (90, 8),
    "lampGreen": (114, 8),
    "heartNode": (120, 9),
}


def find(paths):
    for p in paths:
        if os.path.isfile(p):
            return p
    return None


def load(jar, path):
    with zipfile.ZipFile(jar) as zf:
        img = Image.open(io.BytesIO(zf.read(path)))
        img.load()
    return img.convert("RGBA")


def luminance(c):
    return (c[0] * 299 + c[1] * 587 + c[2] * 114) // 1000


def dual_recolour(img, low_ramp, high_ramp, split=0.45, glow_top=0.92):
    w, h = img.size
    sp = img.load()
    lums = [luminance(sp[x, y]) for y in range(h) for x in range(w) if sp[x, y][3] > 8]
    lo, hi = min(lums), max(lums)
    span = max(1, hi - lo)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            c = sp[x, y]
            if c[3] <= 8:
                continue
            t = (luminance(c) - lo) / span
            if t >= glow_top:
                op[x, y] = GLOW + (c[3],)
            elif t < split:
                lv = int(t / split * (len(low_ramp) - 1) + 0.5)
                op[x, y] = low_ramp[lv] + (c[3],)
            else:
                lv = int((t - split) / (1.0 - split) * (len(high_ramp) - 1) + 0.5)
                op[x, y] = high_ramp[lv] + (c[3],)
    return out


def is_green(c):
    return c[1] > 90 and c[1] > c[0] * 1.6 and c[1] > c[2] * 1.6


def retint_green(img):
    """Перекраска родного зелёного свечения IC2 (по вердикту критиков)."""
    if IC2_GLOW_OVERRIDE is None:
        return img
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] > 0 and is_green(c):
                k = c[1] / 255.0
                px[x, y] = (int(IC2_GLOW_OVERRIDE[0] * k),
                            int(IC2_GLOW_OVERRIDE[1] * k),
                            int(IC2_GLOW_OVERRIDE[2] * k), c[3])
    return img


def _fill(img, x, y, w, h, colour):
    px = img.load()
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px[xx, yy] = colour + (255,)


def box_patch(img, u, v, w, h, d, body, front=None, top=None):
    S = 2
    _fill(img, u * S, v * S, 2 * (d + w) * S, (d + h) * S, body)
    if top is not None:
        _fill(img, (u + d) * S, v * S, w * S, d * S, top)
    if front is not None:
        _fill(img, (u + d) * S, (v + d) * S, w * S, h * S, front)


def tile_patch(img, u, v, w, h, d, tile, top=None):
    """Обтягивает развёртку бокса повторяющимся фрагментом-тайлом."""
    S = 2
    tw, th = tile.size
    tp = tile.load()
    px = img.load()
    x0, y0 = u * S, v * S
    width, height = 2 * (d + w) * S, (d + h) * S
    for yy in range(height):
        for xx in range(width):
            px[x0 + xx, y0 + yy] = tp[xx % tw, yy % th]
    if top is not None:
        _fill(img, (u + d) * S, v * S, w * S, d * S, top)


def paint_patches(canvas, forged_tile, nano_tone):
    S = 2

    # паулдрон (п.2): кованая фортресс-пластина, латунная окантовка крышки
    pu, pv = UV["pauldron"]
    pw, ph, pd = PAULDRON
    tile_patch(canvas, pu, pv, pw, ph, pd, forged_tile, top=BRASS[1])
    _fill(canvas, (pu + pd) * S, pv * S, pw * S, 1, BRASS[2])

    # сабатон: нано-подложка, кованая морда, латунный носок
    su, sv = UV["sabaton"]
    box_patch(canvas, su, sv, 5, 3, 5, nano_tone)
    tp = forged_tile.load()
    px = canvas.load()
    for yy in range(3 * S):
        for xx in range(5 * S):
            px[(su + 5) * S + xx, (sv + 5) * S + yy] = tp[
                xx % forged_tile.size[0], yy % forged_tile.size[1]]
    _fill(canvas, (su + 5) * S, (sv + 5 + 2) * S, 5 * S, 1 * S, BRASS[2])

    # кабели: тёмный кожух, ЗЕЛЁНАЯ жила (п.0: голубой — вето)
    cu, cv = UV["chestCable"]
    box_patch(canvas, cu, cv, 1, 6, 1, NANO[1], front=GREEN)
    au, av = UV["armCable"]
    box_patch(canvas, au, av, 1, 5, 1, NANO[1], front=GREEN)

    # подсумок: карбон с латунной клипсой
    ou, ov = UV["pouch"]
    box_patch(canvas, ou, ov, 2, 3, 1, NANO[3], front=NANO[2], top=BRASS[1])

    # лампы-терминалы (п.5): зелёное ядро, тёмно-зелёная обводка
    gu, gv = UV["lampGreen"]
    box_patch(canvas, gu, gv, 1, 1, 1, GREEN_RIM, front=GREEN, top=GREEN)

    # нагрудный таум-узел (п.5): единственный фиолетовый эмиссив-акцент
    hu, hv = UV["heartNode"]
    box_patch(canvas, hu, hv, 1, 1, 1, TEMPER[1], front=GLOW, top=GLOW)


def paste_alpha_scaled(canvas, src, scale, dx=0, dy=0):
    """Вставка src с масштабом, только непрозрачные пиксели."""
    big = src.resize((src.size[0] * scale, src.size[1] * scale), Image.NEAREST)
    bp = big.load()
    px = canvas.load()
    for y in range(big.size[1]):
        for x in range(big.size[0]):
            c = bp[x, y]
            if c[3] > 8 and dx + x < canvas.size[0] and dy + y < canvas.size[1]:
                px[dx + x, dy + y] = c


# Бипед-зоны слоя 64x32 (юниты; в нашем файле x2). У бипеда ОДНА зона руки
# и ОДНА зона ноги — вторая конечность зеркалится с тех же UV.
ZONE_HEAD = (0, 0, 32, 16)
ZONE_BODY = (16, 16, 40, 32)
ZONE_ARM = (40, 16, 56, 32)
ZONE_LEG = (0, 16, 16, 32)


def weave(ux, uy):
    """Нано-плетение: базовый тон + тень сетки + редкий блик (логика
    текстур: заливка одним цветом читается как дыра, плетение — как ткань)."""
    if (ux * 3 + uy * 5) % 11 == 0:
        return NANO[3]
    if (ux + uy) % 3 == 0:
        return NANO[1]
    return NANO[2]


def fill_zone(canvas, zone):
    """Глушит прозрачные пиксели бипед-зоны нано-плетением: броня
    полнотелая (решение владельца), кожа сквозь неё светить не должна.

    Урок третьей примерки: nano_1 IC2 — слой «шлем/торс/боты» с ОТКРЫТЫМ
    лицом и дырявыми рукавами, а штаны вообще в отдельном nano_2; вклейка
    одного nano_1 оставила голые бёдра и лицо."""
    x0, y0, x1, y1 = [v * 2 for v in zone]
    px = canvas.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            if px[x, y][3] <= 128:
                px[x, y] = weave(x // 2, y // 2) + (255,)


def face_plate(canvas):
    """Лицевая пластина глухого шлема: дыхательная решётка на месте рта.
    Глаза закрывает ПНВ-маска на фортресс-очках, решётке эмиссив не нужен."""
    px = canvas.load()
    for y in range(26, 30):
        for x in range(18, 30):
            px[x, y] = (NANO[4] if (x // 2) % 2 == 0 else NANO[0]) + (255,)


def main():
    tc = find(TC_JARS)
    ic2 = find(IC2_JARS)
    if tc is None or ic2 is None:
        print("Нет jar-ов TC4/IC2 — ассеты Нано-Таум не сгенерированы.")
        return 1

    fortress = load(tc, "assets/thaumcraft/textures/models/fortress_armor.png")
    base = dual_recolour(fortress, NANO, TEMPER)

    nano1 = retint_green(load(ic2, "assets/ic2/textures/armor/nano_1.png"))
    nano2 = retint_green(load(ic2, "assets/ic2/textures/armor/nano_2.png"))
    nv = retint_green(load(ic2, "assets/ic2/textures/armor/nightvision_1.png"))

    nl = nano1.load()
    nano_tone = None
    for y in range(nano1.size[1]):
        for x in range(nano1.size[0]):
            c = nl[x, y]
            if c[3] > 200 and not is_green(c):
                nano_tone = (c[0], c[1], c[2])
                break
        if nano_tone:
            break
    if nano_tone is None:
        nano_tone = NANO[2]

    armor_dir = os.path.join(ROOT, "textures", "models", "armor")
    os.makedirs(armor_dir, exist_ok=True)

    # === слой 1: шлем / торс / ботинки (ванильная логика layer_1) ===
    canvas = base.copy()
    # Родной нано-костюм на бипед-зоны: nano_1 (плоскость 64x32, 1px/юнит)
    # численно совпадает по UV с бипедом нашей плоскости → просто x2 в (0,0).
    paste_alpha_scaled(canvas, nano1, 2)
    # Броня полнотелая: глухой шлем, сплошные торс и рукава. Зону ноги НЕ
    # глушим — в слое 1 там ботиночная часть nano_1, бедро отдано слою 2.
    fill_zone(canvas, ZONE_HEAD)
    fill_zone(canvas, ZONE_BODY)
    fill_zone(canvas, ZONE_ARM)
    face_plate(canvas)

    # Маска ПНВ IC2 → зона фортрессовского бокса Goggles (100,18) 9x5x1:
    # front в файле = (202,38) 18x10. Источник: пояс головы nightvision_1
    # (128x64, 2px/юнит): front головы с маской = px(16,19)-(32,29).
    mask = nv.crop((16, 19, 32, 29)).resize((18, 10), Image.NEAREST)
    _fill(canvas, 200, 36, (1 + 9 + 1 + 9) * 2, (1 + 5) * 2, NANO[1])
    canvas.paste(mask, (202, 38), mask)

    # Тайл кованой фортресс-пластины: фрагмент плотной зоны развёртки.
    forged_tile = canvas.crop((74, 46, 74 + 12, 46 + 8))

    paint_patches(canvas, forged_tile, nano_tone)
    canvas.save(os.path.join(armor_dir, "nano_thaum_armor_1.png"))

    # === слой 2: поножи (штаны бипеда живут в ОТДЕЛЬНОМ файле, как у
    # ванилы и IC2 — nano_2; без него бёдра были голыми) ===
    canvas2 = base.copy()
    paste_alpha_scaled(canvas2, nano2, 2)
    fill_zone(canvas2, ZONE_LEG)
    paint_patches(canvas2, forged_tile, nano_tone)
    canvas2.save(os.path.join(armor_dir, "nano_thaum_armor_2.png"))

    # старый единый файл больше не используется
    old = os.path.join(armor_dir, "nano_thaum_armor.png")
    if os.path.isfile(old):
        os.remove(old)

    # Иконки: РОДНЫЕ нано-иконки IC2 + кованая пластина + лампа.
    items_dir = os.path.join(ROOT, "textures", "items")
    parts = {
        "nano_thaum_helmet": ("nano_helmet", (4, 6, 11, 8), (12, 5)),
        "nano_thaum_chestplate": ("nano_chestplate", (5, 6, 10, 11), (12, 3)),
        "nano_thaum_leggings": ("nano_leggings", (4, 4, 11, 6), None),
        "nano_thaum_boots": ("nano_boots", (3, 9, 6, 11), (12, 10)),
    }
    ft = forged_tile.load()
    for ours, (theirs, box, lamp) in parts.items():
        icon = retint_green(load(ic2, "assets/ic2/textures/items/armor/%s.png" % theirs))
        px = icon.load()
        x0, y0, x1, y1 = box
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if px[x, y][3] > 0:
                    edge = x in (x0, x1) or y in (y0, y1)
                    px[x, y] = (TEMPER[1] + (255,)) if edge else ft[
                        (x - x0) % forged_tile.size[0],
                        (y - y0) % forged_tile.size[1]]
        if lamp:
            # П.0 вердикта: голубой на костюме — вето, лампы зелёные
            px[lamp[0], lamp[1]] = GREEN + (255,)
        icon.save(os.path.join(items_dir, ours + ".png"))

    import json
    for ours in parts:
        with open(os.path.join(ROOT, "models", "item", ours + ".json"),
                  "w", encoding="utf-8") as f:
            json.dump({"parent": "item/generated",
                       "textures": {"layer0": "unboundtech:items/" + ours}}, f, indent=2)
            f.write("\n")
    print("nano-thaum v4: два слоя (1: шлем/торс/боты, 2: штаны), глухой шлем,"
          " полнотелые зоны")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
