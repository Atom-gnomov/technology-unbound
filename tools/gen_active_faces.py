# -*- coding: utf-8 -*-
"""Варианты граней «машина работает» — из спокойных граней.

Генерировать активную грань отдельно нельзя: модель нарисует другую
картинку, и при включении машины текстура будет «прыгать». Поэтому
берём готовую спокойную грань и зажигаем на ней ЦВЕТНЫЕ участки —
индикаторы, щели, кристаллы. Геометрия при этом совпадает пиксель в
пиксель, меняется только свет.

Цветным считается пиксель с заметной насыщенностью: серая сталь и
латунь остаются как есть, а фиолетовый флюкс, зелёные жилы и голубые
лампы разгораются.
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
BLOCKS = os.path.join(os.path.dirname(HERE), "src", "main", "resources",
                      "assets", "unboundtech", "textures", "blocks")

# грань -> насколько сильно разгорается (разные машины светят по-разному)
ACTIVE = {
    "thaum_generator_front": 1.9,
    "aetheric_engine_front": 1.9,
    "flux_condenser_front": 1.7,
    "resonant_splitter_front": 1.7,
    "resonant_splitter_top": 1.6,
    "induction_crucible_side": 1.8,
    "induction_crucible_top": 1.6,
    "bus_node_side": 1.6,
    "essentia_vault_controller_front": 2.0,
    "singulator_front": 2.0,
    "singulator_side": 1.8,
    "cartridge_line_front": 1.7,
    "cartridge_line_top": 1.6,
}

# порог насыщенности: ниже — это металл, его не зажигаем
CHROMA_MIN = 26


def lit(img, boost):
    out = img.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            chroma = max(r, g, b) - min(r, g, b)
            if chroma < CHROMA_MIN:
                continue          # серый металл не светится
            # разгораемся вдоль собственного тона, а не в белый
            k = boost
            px[x, y] = (min(255, int(r * k)), min(255, int(g * k)),
                        min(255, int(b * k)), a)
    return out


def main():
    done = 0
    for name, boost in sorted(ACTIVE.items()):
        base = os.path.join(BLOCKS, name + ".png")
        if not os.path.exists(base):
            print("нет спокойной грани:", name)
            continue
        lit(Image.open(base), boost).save(
            os.path.join(BLOCKS, name + "_active.png"))
        done += 1
    print("активных граней выведено:", done)


if __name__ == "__main__":
    raise SystemExit(main())
