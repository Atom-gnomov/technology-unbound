# -*- coding: utf-8 -*-
"""Иконки, выведенные из РОДСТВЕННЫХ предметов соседних модов.

Правило владельца: «если есть по смыслу схожий предмет, основу взять с
его». Это самый надёжный способ попасть в стиль: мы не пытаемся
имитировать Minecraft/Thaumcraft/IndustrialCraft, а берём их настоящую
иконку и перекрашиваем в нашу идентичность, сохраняя силуэт, зерно и
посадку в клетке инвентаря.

Идентичность мода:
  закалённый таумий  — тёмно-лиловый металл с латунным ободом
  нано-таум          — почти чёрный металл + зелёные жилы
  квант-ихор         — золото + тёплый янтарь
  квант-пустота      — пурпурно-чёрная ткань + лиловые узлы
  электрика          — латунь + фиолетовый флюкс

Файлы соседей читаются из установленных джаров (правило проекта:
ассеты только с реальных образцов), наружу ничего не копируется —
результат всегда перекрашен и помечен нашими акцентами.
"""
import os
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(os.path.dirname(HERE), "src", "main", "resources",
                    "assets", "unboundtech", "textures", "items")
MODS = os.path.expandvars(r"%APPDATA%\.minecraft\mods")

TC_JAR = os.path.join(MODS, "Thaumcraft-1.2.8.2-universal.jar")
IC_JAR = os.path.join(MODS, "industrialcraft-2-2.8.222-ex112.jar")
TC = "assets/thaumcraft/textures/items/"
IC = "assets/ic2/textures/items/"

# наш предмет -> (джар, путь, схема перекраски)
KIN = {
    # --- закалённый таумий: берём таумиевую линейку ТК ---
    "tempered_thaumium_ingot": (TC_JAR, TC + "thaumiumingot.png", "tempered"),
    "tempered_sword":    (TC_JAR, TC + "thaumiumsword.png", "tempered"),
    "tempered_pickaxe":  (TC_JAR, TC + "thaumiumpick.png", "tempered"),
    "tempered_axe":      (TC_JAR, TC + "thaumiumaxe.png", "tempered"),
    "tempered_shovel":   (TC_JAR, TC + "thaumiumshovel.png", "tempered"),
    "tempered_hoe":      (TC_JAR, TC + "thaumiumhoe.png", "tempered"),
    "tempered_helmet":   (TC_JAR, TC + "thaumiumhelm.png", "tempered"),
    "tempered_chestplate": (TC_JAR, TC + "thaumiumchest.png", "tempered"),
    "tempered_leggings": (TC_JAR, TC + "thaumiumlegs.png", "tempered"),
    "tempered_boots":    (TC_JAR, TC + "thaumiumboots.png", "tempered"),
    # --- нано-таум: нано-броня IC2 в зелень ---
    "nano_thaum_helmet":     (IC_JAR, IC + "armor/nano_helmet.png", "nano"),
    "nano_thaum_chestplate": (IC_JAR, IC + "armor/nano_chestplate.png", "nano"),
    "nano_thaum_leggings":   (IC_JAR, IC + "armor/nano_leggings.png", "nano"),
    "nano_thaum_boots":      (IC_JAR, IC + "armor/nano_boots.png", "nano"),
    # --- квант-сеты: квантовая броня IC2 ---
    "quant_ichor_helmet":     (IC_JAR, IC + "armor/quantum_helmet.png", "ichor"),
    "quant_ichor_chestplate": (IC_JAR, IC + "armor/quantum_chestplate.png", "ichor"),
    "quant_ichor_leggings":   (IC_JAR, IC + "armor/quantum_leggings.png", "ichor"),
    "quant_ichor_boots":      (IC_JAR, IC + "armor/quantum_boots.png", "ichor"),
    "quant_void_helmet":     (IC_JAR, IC + "armor/quantum_helmet.png", "void"),
    "quant_void_chestplate": (IC_JAR, IC + "armor/quantum_chestplate.png", "void"),
    "quant_void_leggings":   (IC_JAR, IC + "armor/quantum_leggings.png", "void"),
    "quant_void_boots":      (IC_JAR, IC + "armor/quantum_boots.png", "void"),
    # --- бижутерия: заготовки баблсов ТК ---
    "resonance_amulet": (TC_JAR, TC + "bauble_amulet.png", "flux"),
    "tesla_girdle":     (TC_JAR, TC + "bauble_belt.png", "flux"),
    "ring_frame":  (TC_JAR, TC + "bauble_ring.png", "ring_cyan"),
    "ring_drive":  (TC_JAR, TC + "bauble_ring.png", "ring_orange"),
    "ring_stride": (TC_JAR, TC + "bauble_ring.png", "ring_green"),
    "ring_brace":  (TC_JAR, TC + "bauble_ring_iron.png", "ring_violet"),
    # --- жезл и фокус ---
    "iridium_wand_cap": (TC_JAR, TC + "wand_cap_void.png", "iridium"),
    "focus_charge":     (TC_JAR, TC + "focus.png", "flux"),
    # --- инструменты ---
    "thaumium_wrench":    (IC_JAR, IC + "tool/wrench.png", "tempered"),
    "thaumic_overclocker": (IC_JAR, IC + "upgrade/overclocker.png", "flux"),
    "vis_edge":           (IC_JAR, IC + "tool/electric/nano_saber.png", "flux"),
}

# схема -> (целевой тон тёмного, целевой тон светлого, акцент)
SCHEMES = {
    "tempered":   ((0x2A, 0x1E, 0x3A), (0x8E, 0x76, 0xB4), (0xC8, 0x9A, 0x3C)),
    "nano":       ((0x1C, 0x20, 0x1E), (0x86, 0x92, 0x88), (0x3E, 0xC8, 0x52)),
    "ichor":      ((0x3A, 0x26, 0x0A), (0xD8, 0xAC, 0x4E), (0xFF, 0xD8, 0x73)),
    "void":       ((0x12, 0x0C, 0x1E), (0x5A, 0x48, 0x74), (0xC4, 0x92, 0xE8)),
    "flux":       ((0x24, 0x1C, 0x30), (0x9A, 0x86, 0xB0), (0xC4, 0x92, 0xE8)),
    "iridium":    ((0x4A, 0x4E, 0x5A), (0xDD, 0xE2, 0xEC), (0xC4, 0x92, 0xE8)),
    "ring_cyan":  ((0x24, 0x2A, 0x30), (0xA8, 0xB4, 0xC0), (0x58, 0xD8, 0xE0)),
    "ring_orange": ((0x30, 0x22, 0x12), (0xC8, 0xA0, 0x60), (0xE0, 0x8A, 0x28)),
    "ring_green": ((0x24, 0x2A, 0x26), (0xB4, 0xC0, 0xB4), (0x4E, 0xD0, 0x62)),
    "ring_violet": ((0x1E, 0x1C, 0x24), (0x8E, 0x8A, 0x9C), (0xB0, 0x72, 0xE0)),
}


def read(jar, path):
    with zipfile.ZipFile(jar) as z:
        with z.open(path) as fh:
            img = Image.open(fh).convert("RGBA")
            img.load()
    if img.height > img.width:          # анимированная лента — первый кадр
        img = img.crop((0, 0, img.width, img.width))
    return img


def recolour(img, scheme, accent_top=0.86):
    """Перекрасить по светлоте: тени -> тёмный тон, света -> светлый,
    самые яркие пиксели -> акцент (это «наш» опознавательный цвет)."""
    dark, light, accent = SCHEMES[scheme]
    px = img.load()
    lums = [(r * 299 + g * 587 + b * 114) // 1000
            for r, g, b, a in img.getdata() if a > 128]
    if not lums:
        return img
    lo, hi = min(lums), max(lums)
    span = max(1, hi - lo)
    cut = lo + span * accent_top
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a <= 128:
                px[x, y] = (0, 0, 0, 0)
                continue
            lum = (r * 299 + g * 587 + b * 114) // 1000
            if lum >= cut:
                px[x, y] = accent + (255,)
                continue
            t = (lum - lo) / span
            px[x, y] = tuple(
                int(d + (l - d) * t) for d, l in zip(dark, light)) + (255,)
    return img


def main():
    scale = 4            # 16 -> 64: канва как у остальных наших иконок
    done = 0
    for name, (jar, path, scheme) in sorted(KIN.items()):
        if not os.path.exists(jar):
            print("нет джара:", jar)
            return 1
        try:
            src = read(jar, path)
        except KeyError:
            print("нет иконки-родителя:", path)
            continue
        out = recolour(src, scheme)
        if scale != 1:
            out = out.resize((out.width * scale, out.height * scale),
                             Image.NEAREST)
        out.save(os.path.join(ROOT, name + ".png"))
        done += 1
    print("выведено из родственных иконок:", done)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
