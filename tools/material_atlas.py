# -*- coding: utf-8 -*-
"""Мост между сгенерированными материалами и UV-развёртками моделей.

Претензия владельца к старым текстурам — «однотонные и тошнотные».
Причина была в том, что грани боксов заливались процедурным шумом:
на развёртке в увеличении это выглядит как фактура, а в игре на
гранях 6-10 пикселей превращается в ровное пятно.

Решение: настоящие материалы (`tools/materials/*.png`, сгенерированы
через fal) и ЧЕСТНАЯ выборка из них по каждой грани:
  - каждой грани достаётся СВОЙ случайный (но детерминированный) кусок
    тайла — грани не повторяются;
  - яркость куска подгоняется под роль грани (верх светлее, низ темнее,
    бока средние) — объём читается без градиентной заливки;
  - палитра приводится к ограниченному числу тонов: пиксель-арт не
    терпит плавных переходов.

Детерминизм: seed считается от имени грани, поэтому повторный прогон
генератора даёт тот же результат, и diff в git остаётся осмысленным.
"""
import os
import random

from PIL import Image, ImageEnhance

MATERIALS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                             "materials")

# роль грани -> множитель яркости (свет сверху, как в ванильном MC)
FACE_LIGHT = {
    "top": 1.18,
    "up": 1.18,
    "bottom": 0.68,
    "down": 0.68,
    "front": 1.0,
    "back": 0.86,
    "left": 0.9,
    "right": 0.9,
}

_cache = {}


def load(material):
    """Тайл материала 128x128 (кэшируется)."""
    if material not in _cache:
        path = os.path.join(MATERIALS_DIR, material + ".png")
        if not os.path.exists(path):
            raise FileNotFoundError(
                "нет материала %s — сгенерируй его (tools/materials)" % path)
        _cache[material] = Image.open(path).convert("RGB")
    return _cache[material]


def patch(material, w, h, key, face="front", tones=6, tint=None):
    """Кусок материала под грань w x h.

    key   — имя грани (детерминированный сид),
    face  — роль грани (освещение),
    tones — сколько тонов оставить,
    tint  — (r, g, b) подкраска материала, 0..255, или None.
    """
    if w <= 0 or h <= 0:
        return None
    tile = load(material)
    rnd = random.Random(hash((material, key, w, h)) & 0xFFFFFFFF)
    # берём кусок крупнее грани и ужимаем: так в грань попадает
    # несколько «событий» фактуры, а не один гладкий участок
    zoom = max(2, min(6, 64 // max(w, h, 1) or 2))
    cw, ch = min(tile.width, w * zoom), min(tile.height, h * zoom)
    x = rnd.randrange(0, max(1, tile.width - cw))
    y = rnd.randrange(0, max(1, tile.height - ch))
    crop = tile.crop((x, y, x + cw, y + ch)).resize((w, h), Image.BOX)
    if tint:
        px = crop.load()
        for yy in range(h):
            for xx in range(w):
                r, g, b = px[xx, yy]
                lum = (r * 299 + g * 587 + b * 114) // 1000
                px[xx, yy] = tuple(min(255, lum * c // 160) for c in tint)
    light = FACE_LIGHT.get(face, 1.0)
    crop = ImageEnhance.Brightness(crop).enhance(light)
    crop = ImageEnhance.Contrast(crop).enhance(1.25)
    if tones:
        crop = crop.quantize(colors=tones, method=Image.MEDIANCUT,
                             dither=Image.NONE).convert("RGB")
    return crop


def paint(px, rect, material, key, face="front", tones=6, tint=None,
          alpha=255):
    """Положить кусок материала в прямоугольник развёртки."""
    x0, y0, w, h = rect
    crop = patch(material, w, h, key, face, tones, tint)
    if crop is None:
        return
    src = crop.load()
    for yy in range(h):
        for xx in range(w):
            r, g, b = src[xx, yy]
            px[x0 + xx, y0 + yy] = (r, g, b, alpha)


def rim(px, rect, shade=0.55):
    """Тёмная кромка грани: объём на 6 пикселях читается только так."""
    x0, y0, w, h = rect
    for xx in range(x0, x0 + w):
        for yy in (y0, y0 + h - 1):
            r, g, b, a = px[xx, yy]
            px[xx, yy] = (int(r * shade), int(g * shade), int(b * shade), a)
    for yy in range(y0, y0 + h):
        for xx in (x0, x0 + w - 1):
            r, g, b, a = px[xx, yy]
            px[xx, yy] = (int(r * shade), int(g * shade), int(b * shade), a)


def highlight(px, rect, shade=1.45):
    """Светлая верхняя кромка — вторая половина объёма."""
    x0, y0, w, h = rect
    for xx in range(x0 + 1, x0 + w - 1):
        r, g, b, a = px[xx, y0 + 1] if h > 2 else px[xx, y0]
        yy = y0 + 1 if h > 2 else y0
        px[xx, yy] = (min(255, int(r * shade)), min(255, int(g * shade)),
                      min(255, int(b * shade)), a)
