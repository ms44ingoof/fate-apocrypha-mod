#!/usr/bin/env python3
"""Рисует текстуры Командных заклинаний: эмблему печати (96x96) и три медальона приказов (32x32).
Рисуется с суперсэмплингом (сглаженные края). Результат: src/main/resources/assets/fateapocrypha/textures/gui/"""
import math, os
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'fateapocrypha', 'textures', 'gui')
RED = (217, 46, 58, 255)
RED_D = (120, 22, 34, 255)
RED_L = (255, 96, 104, 255)
GOLD = (236, 200, 96, 255)
DARK = (18, 11, 15, 235)

def blade(d, cx, cy, ang, r0, r1, wmax, bend, fill, line=None):
    """Изогнутый клинок-лепесток от радиуса r0 до r1 (ang в градусах, 0 = вправо)."""
    n = 40
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        r = r0 + (r1 - r0) * t
        a = math.radians(ang) + math.radians(bend) * t * t
        w = wmax * (math.sin(math.pi * t ** 0.75) ** 0.9)
        px, py = cx + r * math.cos(a), cy + r * math.sin(a)
        nx, ny = -math.sin(a), math.cos(a)
        left.append((px + nx * w, py + ny * w))
        right.append((px - nx * w, py - ny * w))
    poly = left + right[::-1]
    d.polygon(poly, fill=fill)
    if line:
        pts = []
        for i in range(n + 1):
            t = i / n
            r = r0 + (r1 - r0) * t
            a = math.radians(ang) + math.radians(bend) * t * t
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
        d.line(pts, fill=line, width=max(1, int(wmax * 0.18)))

def emblem(size=96, ss=6):
    S = size * ss
    c = S / 2
    base = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(base)
    u = S / 96.0
    # внешнее кольцо и засечки
    d.ellipse([c - 44*u, c - 44*u, c + 44*u, c + 44*u], outline=RED_D, width=int(1.4*u))
    d.ellipse([c - 40*u, c - 40*u, c + 40*u, c + 40*u], outline=(150, 28, 40, 200), width=int(0.8*u))
    for k in range(24):
        a = math.radians(k * 15)
        r0, r1 = (40*u, 44*u) if k % 2 == 0 else (41.5*u, 43*u)
        d.line([(c + r0*math.cos(a), c + r0*math.sin(a)), (c + r1*math.cos(a), c + r1*math.sin(a))], fill=RED_D, width=int(0.8*u))
    # малые клинки между большими
    for k in range(3):
        blade(d, c, c, 90 + 120*k + 60, 6*u, 26*u, 4.0*u, 14, RED_D, None)
    # три больших клинка (как когти печати)
    for k in range(3):
        blade(d, c, c, 90 + 120*k, 7*u, 39*u, 8.5*u, -22, RED, RED_L)
    # центральный ромб
    for r, col in ((9*u, RED_D), (7*u, RED), (4.2*u, GOLD)):
        d.polygon([(c, c - r), (c + r, c), (c, c + r), (c - r, c)], fill=col)
    # точки на кончиках
    for k in range(3):
        a = math.radians(90 + 120*k + (-22))
        px, py = c + 41.5*u*math.cos(a), c + 41.5*u*math.sin(a)
        d.ellipse([px - 1.6*u, py - 1.6*u, px + 1.6*u, py + 1.6*u], fill=GOLD)
    glow = base.filter(ImageFilter.GaussianBlur(3.2*u))
    glow = Image.eval(glow, lambda v: v)  # только для читаемости
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(glow)
    out.alpha_composite(base)
    return out.resize((size, size), Image.LANCZOS)

def medallion(kind, size=32, ss=10):
    S = size * ss
    c = S / 2
    u = S / 32.0
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([c - 15.5*u, c - 15.5*u, c + 15.5*u, c + 15.5*u], fill=DARK)
    d.ellipse([c - 15.5*u, c - 15.5*u, c + 15.5*u, c + 15.5*u], outline=RED, width=int(1.5*u))
    d.ellipse([c - 13*u, c - 13*u, c + 13*u, c + 13*u], outline=(236, 200, 96, 140), width=max(1, int(0.6*u)))
    if kind == 'teleport':
        for r, col, w in ((8.5*u, RED, 1.4), (5.4*u, GOLD, 1.2)):
            d.polygon([(c, c - r), (c + r, c), (c, c + r), (c - r, c)], outline=col, width=int(w*u))
        d.ellipse([c - 1.5*u, c - 1.5*u, c + 1.5*u, c + 1.5*u], fill=GOLD)
        for a in (45, 135, 225, 315):
            x, y = c + 10.5*u*math.cos(math.radians(a)), c + 10.5*u*math.sin(math.radians(a))
            d.ellipse([x - 1.0*u, y - 1.0*u, x + 1.0*u, y + 1.0*u], fill=RED_L)
    elif kind == 'buff':
        # меч, направленный вверх, и две шевронные стрелки усиления
        d.polygon([(c, c - 10*u), (c + 2.2*u, c + 3*u), (c - 2.2*u, c + 3*u)], fill=GOLD)
        d.rectangle([c - 5.5*u, c + 3*u, c + 5.5*u, c + 5*u], fill=RED)
        d.rectangle([c - 1.1*u, c + 5*u, c + 1.1*u, c + 9*u], fill=GOLD)
        for dx in (-7.5, 7.5):
            x = c + dx*u
            d.line([(x - 2.6*u, c - 1*u), (x, c - 4.2*u), (x + 2.6*u, c - 1*u)], fill=RED_L, width=int(1.3*u))
            d.line([(x - 2.6*u, c + 3*u), (x, c - 0.2*u), (x + 2.6*u, c + 3*u)], fill=RED, width=int(1.3*u))
    else:
        # Благородный Фантазм: восьмиконечная звезда в кольце
        pts = []
        for k in range(16):
            r = 11*u if k % 2 == 0 else 4.2*u
            a = math.radians(k * 22.5 - 90)
            pts.append((c + r*math.cos(a), c + r*math.sin(a)))
        d.polygon(pts, fill=RED)
        pts2 = []
        for k in range(16):
            r = 7*u if k % 2 == 0 else 2.6*u
            a = math.radians(k * 22.5 - 90)
            pts2.append((c + r*math.cos(a), c + r*math.sin(a)))
        d.polygon(pts2, fill=GOLD)
        d.ellipse([c - 2*u, c - 2*u, c + 2*u, c + 2*u], fill=RED_D)
    return im.resize((size, size), Image.LANCZOS)

def dragon_emblem(src, size=96):
    """Эмблема из картинки: чёрное на белом превращается в алую фигуру со свечением на прозрачном фоне."""
    import numpy as np
    img = Image.open(src).convert('L')
    arr = np.array(img).astype('float32') / 255.0
    alpha = np.clip((1.0 - arr) * 1.15 - 0.05, 0.0, 1.0)          # чёрное = непрозрачное
    ys, xs = np.where(alpha > 0.08)
    box = (xs.min(), ys.min(), xs.max() + 1, ys.max() + 1)
    mask = Image.fromarray((alpha * 255).astype('uint8')).crop(box)
    inner = size - 8
    scale = min(inner / mask.width, inner / mask.height)
    w, h = max(1, round(mask.width * scale)), max(1, round(mask.height * scale))
    mask = mask.resize((w, h), Image.LANCZOS)
    layer = Image.new('L', (size, size), 0)
    layer.paste(mask, ((size - w) // 2, (size - h) // 2))
    # цвет: насыщенный алый, сверху светлее, снизу темнее
    col = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = col.load()
    lp = layer.load()
    for y in range(size):
        k = y / (size - 1)
        r = int(236 - 56 * k)
        g_ = int(44 - 20 * k)
        b = int(56 - 22 * k)
        for x in range(size):
            if lp[x, y] > 0:
                px[x, y] = (r, g_, b, lp[x, y])
    # тёмный контур для читаемости на светлом фоне
    outline_a = layer.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(0.6)).point(lambda v: min(255, int(v * 1.2)))
    outline = Image.new('RGBA', (size, size), (24, 4, 8, 0))
    outline.putalpha(outline_a.point(lambda v: int(v * 0.85)))
    # узкое свечение
    glow_a = layer.filter(ImageFilter.GaussianBlur(2.0)).point(lambda v: int(v * 0.55))
    glow = Image.new('RGBA', (size, size), (230, 40, 54, 0))
    glow.putalpha(glow_a)
    out = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(outline)
    out.alpha_composite(col)
    return out

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    src = os.path.join(os.path.dirname(__file__), 'seal_dragon_source.png')
    (dragon_emblem(src) if os.path.exists(src) else emblem()).save(os.path.join(OUT, 'seal_emblem.png'))
    for k in ('teleport', 'buff', 'phantasm'):
        medallion(k).save(os.path.join(OUT, f'seal_{k}.png'))
    print('готово')
