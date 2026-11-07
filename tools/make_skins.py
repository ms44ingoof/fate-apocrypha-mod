#!/usr/bin/env python3
"""Генератор детализированных скинов-заглушек 64x64 для NPC мода.

Файлы без метки генератора считаются вашими собственными скинами и НЕ перезаписываются
(можно принудительно: python make_skins.py --force). Положите настоящие скины в
src/main/resources/assets/fateapocrypha/textures/entity/<id>.png
"""
import os
import random
import sys
from PIL import Image
from PIL.PngImagePlugin import PngInfo

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'fateapocrypha', 'textures', 'entity')

def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)

class Skin:
    def __init__(self, seed):
        self.im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.px = self.im.load()
        self.rnd = random.Random(seed)

    def rect(self, x1, y1, x2, y2, c, noise=5):
        for x in range(x1, x2):
            for y in range(y1, y2):
                self.px[x, y] = shade(c, self.rnd.randint(-noise, noise)) if noise else tuple(c[:3]) + (255,)

    def dot(self, x, y, c):
        self.px[x, y] = tuple(c[:3]) + (255,)

# карта граней: (x, y, w, h) для каждой части тела в раскладке 64x64
HEAD = {'top': (8, 0), 'bottom': (16, 0), 'r': (0, 8), 'f': (8, 8), 'l': (16, 8), 'b': (24, 8)}

def build(name, p):
    sk = Skin(sum(map(ord, name)))
    skin, hair, eye = p['skin'], p['hair'], p['eye']
    cloth, trim, pants, boots = p['cloth'], p.get('trim', p['cloth']), p['pants'], p.get('boots', p['pants'])
    style = p.get('style', 'short')
    nz = 4

    # ---- голова ----
    for fx, fy in HEAD.values():
        sk.rect(fx, fy, fx + 8, fy + 8, skin, 3)
    sk.rect(8, 0, 16, 8, hair, nz)                      # макушка
    for fx in (0, 16, 24):                               # бока и затылок
        depth = 8 if style in ('long', 'hood') else 4 if style in ('spiky', 'short') else 2
        sk.rect(fx, 8, fx + 8, 8 + depth, hair, nz)
    sk.rect(24, 8, 32, 16, hair, nz) if style in ('long', 'hood') else None
    sk.rect(8, 8, 16, 10, hair, nz)                      # чёлка
    if style == 'spiky':
        for x in (9, 11, 14):
            sk.dot(x, 10, hair)
    if style == 'long':
        sk.rect(8, 10, 9, 14, hair, nz)
        sk.rect(15, 10, 16, 14, hair, nz)
    if style == 'hood':
        sk.rect(8, 8, 16, 10, trim, nz)
        sk.rect(8, 10, 9, 15, trim, nz)
        sk.rect(15, 10, 16, 15, trim, nz)
    if style == 'bald':
        sk.rect(8, 0, 16, 8, skin, 3)
        sk.rect(8, 8, 16, 9, skin, 3)
        for fx in (0, 16, 24):
            sk.rect(fx, 8, fx + 8, 12, skin, 3)
    if p.get('hat'):
        sk.rect(8, 0, 16, 8, p['hat'], nz)
        sk.rect(8, 8, 16, 10, p['hat'], nz)
        for fx in (0, 16, 24):
            sk.rect(fx, 8, fx + 8, 10, p['hat'], nz)
    # лицо: брови, глаза, нос, рот
    brow = shade(hair, -30)
    for x in (9, 10, 13, 14):
        sk.dot(x, 11, brow)
    for ex in (9, 13):
        sk.dot(ex, 12, (250, 250, 250))
        sk.dot(ex + 1, 12, eye)
        sk.dot(ex + 1, 13, shade(eye, -40))
    sk.dot(12, 13, shade(skin, -18))
    sk.dot(11, 14, shade(skin, -35))
    sk.dot(12, 14, shade(skin, -35))
    if p.get('scar'):
        sk.dot(14, 12, (150, 70, 70)); sk.dot(14, 13, (150, 70, 70))
    if p.get('beard'):
        sk.rect(9, 14, 15, 16, p['beard'], 3)
        sk.dot(11, 14, shade(p['beard'], -10)); sk.dot(12, 14, shade(p['beard'], -10))
    if p.get('stubble'):
        for x in range(9, 15):
            if x % 2 == 0:
                sk.dot(x, 15, shade(skin, -45))
    if p.get('stitches'):
        for y in range(9, 16, 2):
            sk.dot(8, y, (60, 50, 60))
        for x in (9, 10, 11):
            sk.dot(x, 10, (70, 55, 60))

    # ---- торс ----
    body = [(20, 20, 8, 12), (16, 20, 4, 12), (28, 20, 4, 12), (32, 20, 8, 12)]
    for x, y, w, h in body:
        sk.rect(x, y, x + w, y + h, cloth, nz)
    sk.rect(20, 16, 28, 20, cloth, nz); sk.rect(28, 16, 36, 20, cloth, nz)
    sk.rect(20, 20, 28, 22, trim, 3)                      # воротник
    sk.rect(23, 22, 25, 24, skin, 3) if p.get('open') else None
    if p.get('belt'):
        sk.rect(20, 28, 28, 29, p['belt'], 3)
        sk.dot(23, 28, (230, 200, 80)); sk.dot(24, 28, (230, 200, 80))
    if p.get('stripe'):
        sk.rect(23, 22, 25, 32, p['stripe'], 3)
    if p.get('armor'):
        sk.rect(20, 22, 28, 28, p['armor'], 4)
        sk.rect(23, 23, 25, 27, shade(p['armor'], 25), 2)
    if p.get('bare'):
        sk.rect(20, 22, 28, 28, skin, 4)                  # обнажённый торс
        for x in (21, 22, 25, 26):
            sk.dot(x, 24, shade(skin, -25))
        sk.rect(20, 22, 28, 23, p.get('strap', (60, 40, 30)), 2)
        sk.rect(23, 22, 25, 32, p.get('strap', (60, 40, 30)), 2)
    if p.get('cross'):
        sk.rect(23, 23, 25, 27, (210, 210, 220), 2); sk.rect(22, 24, 26, 25, (210, 210, 220), 2)
    if p.get('cape'):
        sk.rect(32, 20, 40, 32, p['cape'], nz)

    # ---- руки (обе): рукав + кисть ----
    for ax, ay in ((40, 16), (32, 48)):
        for fx, fy, w, h in ((ax, ay + 4, 4, 12), (ax + 4, ay + 4, 4, 12), (ax + 8, ay + 4, 4, 12), (ax + 12, ay + 4, 4, 12)):
            sk.rect(fx, fy, fx + w, fy + h, cloth, nz)
        sk.rect(ax + 4, ay, ax + 12, ay + 4, cloth, nz)
        sleeve = 8 if not p.get('bare') else 0
        for fx in (ax, ax + 4, ax + 8, ax + 12):
            sk.rect(fx, ay + 4 + sleeve, fx + 4, ay + 16, p.get('glove', skin), 3)
        if p.get('bare'):
            sk.rect(ax, ay + 4, ax + 16, ay + 16, skin, 3)
            sk.rect(ax, ay + 12, ax + 16, ay + 16, p.get('strap', (220, 220, 200)), 2)
        else:
            sk.rect(ax, ay + 4 + sleeve - 1, ax + 16, ay + 4 + sleeve, trim, 3)
        if p.get('pauldron'):
            sk.rect(ax, ay + 4, ax + 16, ay + 7, p['pauldron'], 3)

    # ---- ноги (обе): штаны + сапоги ----
    for lx, ly in ((0, 16), (16, 48)):
        for fx, fy, w, h in ((lx, ly + 4, 4, 12), (lx + 4, ly + 4, 4, 12), (lx + 8, ly + 4, 4, 12), (lx + 12, ly + 4, 4, 12)):
            sk.rect(fx, fy, fx + w, fy + h, pants, nz)
        sk.rect(lx + 4, ly, lx + 12, ly + 4, pants, nz)
        for fx in (lx, lx + 4, lx + 8, lx + 12):
            sk.rect(fx, ly + 12, fx + 4, ly + 16, boots, 3)
        sk.rect(lx, ly + 11, lx + 16, ly + 12, trim, 3)
    return sk.im

C = lambda r, g, b: (r, g, b, 255)
SKINS = {
 'achilles': dict(skin=C(240,205,175), hair=C(196,140,70), eye=C(60,130,210), cloth=C(240,236,225), trim=C(212,175,60),
                  pants=C(240,236,225), boots=C(150,100,50), style='spiky', armor=C(205,165,60), pauldron=C(212,175,60), belt=C(130,90,40)),
 'karna':    dict(skin=C(222,182,140), hair=C(244,244,248), eye=C(220,150,40), cloth=C(240,240,245), trim=C(235,195,60),
                  pants=C(245,245,250), boots=C(235,195,60), style='short', armor=C(235,195,60), pauldron=C(235,195,60), belt=C(200,160,40), glove=C(235,195,60)),
 'justice':  dict(skin=C(246,224,205), hair=C(246,246,252), eye=C(200,50,60), cloth=C(250,250,252), trim=C(215,185,90),
                  pants=C(236,236,246), boots=C(215,185,90), style='long', stripe=C(215,185,90)),
 'zig':      dict(skin=C(236,208,188), hair=C(236,238,246), eye=C(70,200,120), cloth=C(52,56,68), trim=C(200,200,210),
                  pants=C(36,38,48), boots=C(28,28,34), style='spiky', belt=C(120,120,130), stripe=C(200,200,210)),
 'atalanta': dict(skin=C(240,212,190), hair=C(214,232,176), eye=C(80,170,80), cloth=C(70,140,80), trim=C(240,240,235),
                  pants=C(110,80,50), boots=C(90,64,40), style='long', belt=C(90,60,40)),
 'avicebron':dict(skin=C(205,185,160), hair=C(120,120,130), eye=C(230,200,60), cloth=C(48,46,58), trim=C(90,70,120),
                  pants=C(36,34,44), style='hood', stripe=C(90,70,120)),
 'caules':   dict(skin=C(238,210,185), hair=C(60,44,36), eye=C(110,70,40), cloth=C(34,48,96), trim=C(240,240,240),
                  pants=C(40,40,52), boots=C(30,30,36), style='short', stripe=C(240,240,240), belt=C(70,60,40)),
 'celenike': dict(skin=C(244,218,198), hair=C(98,44,36), eye=C(170,60,50), cloth=C(70,40,96), trim=C(220,190,90),
                  pants=C(50,30,70), boots=C(30,20,40), style='long', stripe=C(220,190,90)),
 'chiron':   dict(skin=C(226,190,155), hair=C(170,170,175), eye=C(80,130,150), cloth=C(80,110,70), trim=C(150,110,60),
                  pants=C(100,80,56), boots=C(70,50,34), style='short', beard=C(176,176,180), belt=C(110,76,40), pauldron=C(110,76,40)),
 'darnic':   dict(skin=C(232,204,180), hair=C(228,228,232), eye=C(90,90,110), cloth=C(104,24,44), trim=C(226,190,80),
                  pants=C(40,24,34), boots=C(24,16,22), style='short', beard=C(220,220,224), stripe=C(226,190,80)),
 'farmer':   dict(skin=C(224,184,150), hair=C(110,76,48), eye=C(70,50,30), cloth=C(184,60,50), trim=C(240,230,220),
                  pants=C(56,84,150), boots=C(100,70,44), style='short', hat=C(224,196,100), stubble=True, stripe=C(56,84,150)),
 'fiore':    dict(skin=C(244,218,198), hair=C(152,102,64), eye=C(80,140,200), cloth=C(200,222,244), trim=C(250,250,252),
                  pants=C(230,236,248), boots=C(150,170,200), style='long', stripe=C(250,250,252)),
 'frankenstein': dict(skin=C(204,216,196), hair=C(30,30,38), eye=C(220,200,60), cloth=C(44,32,52), trim=C(170,40,50),
                  pants=C(36,28,44), boots=C(24,20,30), style='long', stitches=True, belt=C(150,150,160), stripe=C(170,40,50)),
 'gordes':   dict(skin=C(226,196,170), hair=C(150,150,156), eye=C(70,70,80), cloth=C(32,32,40), trim=C(240,240,240),
                  pants=C(26,26,34), boots=C(20,20,26), style='short', stripe=C(160,30,40), stubble=True),
 'jack':     dict(skin=C(236,214,200), hair=C(226,226,232), eye=C(200,40,50), cloth=C(36,34,40), trim=C(170,40,40),
                  pants=C(26,24,30), boots=C(20,18,22), style='hood', stripe=C(210,210,200)),
 'kairi':    dict(skin=C(232,200,170), hair=C(36,32,34), eye=C(70,60,60), cloth=C(104,92,80), trim=C(60,52,46),
                  pants=C(44,44,52), boots=C(30,28,30), style='spiky', stubble=True, stripe=C(230,230,226)),
 'reika':    dict(skin=C(244,220,202), hair=C(122,84,52), eye=C(110,76,50), cloth=C(240,200,206), trim=C(250,250,250),
                  pants=C(70,60,90), boots=C(90,60,50), style='long'),
 'roche':    dict(skin=C(242,216,196), hair=C(232,210,120), eye=C(70,130,210), cloth=C(236,236,240), trim=C(40,40,50),
                  pants=C(60,60,74), boots=C(40,40,50), style='long', stripe=C(40,40,50)),
 'shiro':    dict(skin=C(236,208,182), hair=C(96,64,44), eye=C(210,170,50), cloth=C(28,28,34), trim=C(244,244,244),
                  pants=C(22,22,28), boots=C(18,18,22), style='short', cross=True, stripe=C(30,30,38)),
 'spartacus':dict(skin=C(210,160,120), hair=C(70,48,34), eye=C(210,50,40), cloth=C(80,56,40), trim=C(60,60,66),
                  pants=C(92,68,50), boots=C(60,44,32), style='short', bare=True, strap=C(70,48,34), scar=True, stubble=True),
 'vlad':     dict(skin=C(226,198,176), hair=C(34,26,28), eye=C(200,40,50), cloth=C(70,70,82), trim=C(150,26,40),
                  pants=C(40,40,48), boots=C(24,24,30), style='long', armor=C(96,96,108), pauldron=C(96,96,108), cape=C(150,26,40), belt=C(150,26,40)),
}

if __name__ == '__main__':
    force = '--force' in sys.argv
    os.makedirs(OUT, exist_ok=True)
    for name, spec in SKINS.items():
        path = os.path.join(OUT, name + '.png')
        if os.path.exists(path) and not force:
            try:
                mine = Image.open(path).info.get('fa_placeholder') == '1'
            except OSError:
                mine = False
            if not mine and os.path.getsize(path) >= 1000:
                print('пропущен (ваш скин):', name)
                continue
        meta = PngInfo()
        meta.add_text('fa_placeholder', '1')
        build(name, spec).save(path, pnginfo=meta)
        print('создан:', name)
