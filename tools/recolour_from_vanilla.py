# -*- coding: utf-8 -*-
"""
Текстуры закалённого таумия из ванильных, перекраской.

Почему так, а не рисованием с нуля: карточка `tempered_thaumium_tools.md` §8
прямо требует «силуэты алмазного сета», а слои брони на игроке вообще
невозможно нарисовать вслепую — там раскладка UV модели, и любая отсебятина
садится не по фигуре. Ванильные ассеты в модах для Minecraft использовать
нормально; запрет `art_guide.md` §6 — про текстуры СОСЕДНИХ МОДОВ (лицензии
их авторов), к самой игре он не относится.

Перекраска, а не копия: яркость исходного пикселя раскладывается по нашей
стальной рампе, тёплые пиксели (рукояти) — по деревянной, сверху ставятся
редкие вис-вкрапления. Силуэт ванильный, цвет наш.

Источник — клиентский jar 1.12.2 из папки игры. На другой машине путь
другой; если jar не найден, скрипт честно ругается и ничего не трогает,
а в репозитории остаются уже собранные PNG.
"""
import os
import sys
import zipfile

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(REPO, "src", "main", "resources", "assets", "unboundtech", "textures")

CANDIDATE_JARS = [
    r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\versions\Forge 1.12.2\Forge 1.12.2.jar",
    r"C:\Users\Game-On-Dp\AppData\Roaming\.minecraft\versions\1.12.2\1.12.2.jar",
]

# Сталь: от тени к блику. Первый тон — контур.
STEEL = [(0x1B, 0x1A, 0x22), (0x2E, 0x2C, 0x36), (0x42, 0x3F, 0x4C),
         (0x57, 0x54, 0x62), (0x6E, 0x6B, 0x79), (0x8A, 0x87, 0x95),
         (0xA6, 0xA3, 0xB1), (0xC0, 0xBE, 0xC9)]

# Дерево рукояти — темнее ванильного (§8 карточки).
WOOD = [(0x1A, 0x11, 0x09), (0x2A, 0x1D, 0x11), (0x3A, 0x29, 0x18),
        (0x4A, 0x35, 0x1F), (0x5C, 0x42, 0x28), (0x6E, 0x50, 0x31),
        (0x80, 0x5E, 0x3A), (0x93, 0x6D, 0x45)]

VIS = [(0x4A, 0x2A, 0x74), (0x7B, 0x4C, 0xB0), (0xA0, 0x76, 0xD4)]

# Что из чего делаем: наш файл -> ванильный.
ITEMS = {
    "tempered_thaumium_ingot": "iron_ingot",
    "tempered_sword": "diamond_sword",
    "tempered_pickaxe": "diamond_pickaxe",
    "tempered_axe": "diamond_axe",
    "tempered_shovel": "diamond_shovel",
    "tempered_hoe": "diamond_hoe",
    "tempered_helmet": "diamond_helmet",
    "tempered_chestplate": "diamond_chestplate",
    "tempered_leggings": "diamond_leggings",
    "tempered_boots": "diamond_boots",
    # Чернильница ванильной основы больше не имеет: перекрашенная книга
    # читалась книгой, а не прибором (решение владельца). Рисуется вручную
    # в gen_textures.py, как и ключ.
}

# Таумий: ключ по канону из ОБЫЧНОГО таумия, поэтому он лиловый, а не серый.
THAUMIUM = [(0x1B, 0x10, 0x2A), (0x2C, 0x1A, 0x44), (0x3E, 0x25, 0x5E),
            (0x52, 0x32, 0x79), (0x67, 0x40, 0x96), (0x7F, 0x51, 0xB4),
            (0x9A, 0x6D, 0xCE), (0xB8, 0x93, 0xE4)]

# Индикатор заряда на чернильнице: без него она — просто книжка.
CHARGE = (0x3F, 0xAE, 0xE8)

# Слои на игроке берём с ЖЕЛЕЗНОЙ брони: она серая, алмазная — бирюзовая,
# и после перекраски железная сохраняет рисунок пластин честнее.
LAYERS = {"tempered_layer_1": "iron_layer_1", "tempered_layer_2": "iron_layer_2"}

# Куда сажать вис-вкрапления: доля от размера, чтобы не зависеть от 16 или 32.
FLECK_SPOTS = [(0.35, 0.30), (0.62, 0.55), (0.48, 0.75)]


def find_jar():
    for path in CANDIDATE_JARS:
        if os.path.isfile(path):
            return path
    return None


def luminance(colour):
    r, g, b = colour[:3]
    return (r * 299 + g * 587 + b * 114) // 1000


def is_wood(colour):
    """Рукоять тёплая: красного заметно больше синего. Металл — наоборот."""
    r, g, b = colour[:3]
    return r > b + 20


def recolour(image, flecks=True):
    src = image.convert("RGBA")
    w, h = src.size
    sp = src.load()

    # Рампу растягиваем по фактическому диапазону яркости картинки, иначе
    # тёмная ванильная текстура вся уедет в контур.
    lums = [luminance(sp[x, y]) for y in range(h) for x in range(w) if sp[x, y][3] > 0]
    if not lums:
        return src
    low, high = min(lums), max(lums)
    span = max(1, high - low)

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            c = sp[x, y]
            if c[3] == 0:
                continue
            ramp = WOOD if is_wood(c) else STEEL
            level = (luminance(c) - low) * (len(ramp) - 1) // span
            op[x, y] = ramp[max(0, min(len(ramp) - 1, level))] + (c[3],)

    if flecks:
        for (fx, fy) in FLECK_SPOTS:
            x, y = int(w * fx), int(h * fy)
            if 0 <= x < w and 0 <= y < h and sp[x, y][3] > 0 and not is_wood(sp[x, y]):
                op[x, y] = VIS[1] + (255,)
                if x + 1 < w and sp[x + 1, y][3] > 0 and not is_wood(sp[x + 1, y]):
                    op[x + 1, y] = VIS[0] + (255,)
    return out


def shade_flat(path, ramp):
    """
    Наводит объём на рисованную вручную текстуру: тон берётся по тому, как
    глубоко пиксель сидит внутри силуэта. Нужно там, где ванильной основы
    нет и заливка вышла плоской.
    """
    image = Image.open(path).convert("RGBA")
    w, h = image.size
    sp = image.load()

    def inside(x, y):
        return 0 <= x < w and 0 <= y < h and sp[x, y][3] > 0

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            if sp[x, y][3] == 0:
                continue
            if not (inside(x - 1, y) and inside(x + 1, y)
                    and inside(x, y - 1) and inside(x, y + 1)):
                op[x, y] = ramp[0] + (255,)     # контур
                continue
            depth = 0
            while depth < 4 and inside(x + depth + 1, y + depth + 1):
                depth += 1
            top = 0
            while top < 4 and inside(x - top - 1, y - top - 1):
                top += 1
            level = 4 + (depth - top)
            op[x, y] = ramp[max(1, min(len(ramp) - 1, level))] + (255,)
    out.save(path)


def add_charge_light(path):
    """Три пикселя индикатора в правом верхнем углу — «в ней есть заряд»."""
    image = Image.open(path).convert("RGBA")
    w, h = image.size
    px = image.load()
    for (x, y) in ((w - 4, 2), (w - 3, 2), (w - 3, 3)):
        if 0 <= x < w and 0 <= y < h:
            px[x, y] = CHARGE + (255,)
    image.save(path)


def main():
    jar = find_jar()
    if jar is None:
        print("Клиентский jar 1.12.2 не найден — текстуры не тронуты.")
        print("Пути, где искали:")
        for p in CANDIDATE_JARS:
            print("  ", p)
        return 1

    items_dir = os.path.join(OUT, "items")
    armor_dir = os.path.join(OUT, "models", "armor")
    os.makedirs(items_dir, exist_ok=True)
    os.makedirs(armor_dir, exist_ok=True)

    with zipfile.ZipFile(jar) as zf:
        for ours, vanilla in ITEMS.items():
            path = "assets/minecraft/textures/items/%s.png" % vanilla
            with zf.open(path) as fh:
                image = Image.open(fh)
                image.load()
            recolour(image).save(os.path.join(items_dir, ours + ".png"))
            print("item ", ours, "<-", vanilla)

        for ours, vanilla in LAYERS.items():
            path = "assets/minecraft/textures/models/armor/%s.png" % vanilla
            with zf.open(path) as fh:
                image = Image.open(fh)
                image.load()
            # На слоях вкрапления не ставим: они лягут в случайное место
            # развёртки и на модели будут выглядеть грязью.
            recolour(image, flecks=False).save(os.path.join(armor_dir, ours + ".png"))
            print("armor", ours, "<-", vanilla)

    # Ключ и чернильница ванильного аналога не имеют — они нарисованы руками
    # (tools/gen_textures.py), здесь им только наводится объём.
    wrench = os.path.join(items_dir, "thaumium_wrench.png")
    if os.path.isfile(wrench):
        shade_flat(wrench, THAUMIUM)
        print("item  thaumium_wrench: рисованный, наведён объём")
    return 0


if __name__ == "__main__":
    sys.exit(main())
