#!/usr/bin/env python3
"""Генератор ассетов оружия и материалов.

Для каждого оружия по описанию кубоидов создаёт:
  - 3D-модель для руки/земли/рамки (models/item/<id>_3d.json), клинок повёрнут на 45 градусов, как у спрайтов-мечей;
  - модель-обёртку forge:separate_transforms (models/item/<id>.json): в руке 3D, в инвентаре плоская иконка;
  - палитру-текстуру (textures/item/<id>_palette.png) и иконку 32x32 (textures/item/<id>.png), нарисованную из той же геометрии.
А также иконки материалов-наград (32x32) и их модели.
"""
import json
import math
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'fateapocrypha')
TEX = os.path.join(ROOT, 'textures', 'item')
MOD = os.path.join(ROOT, 'models', 'item')

C = lambda r, g, b: (r, g, b, 255)


def _shift(boxes, dx=0.0, dy=0.0, dz=0.0):
    return [(a + dx, b + dy, c + dz, d + dx, e + dy, f + dz, col) for (a, b, c, d, e, f, col) in boxes]


def _mirror_x(boxes):
    return [(-d, b, c, -a, e, f, col) for (a, b, c, d, e, f, col) in boxes]


def _black_key():
    """Один Чёрный ключ: рукоять с фиолетовой оплёткой, гарда-рога и узкий клинок."""
    return [
        (-1.3, -5.2, 7.4, 1.3, -4, 8.6, 'gold'),
        (-0.7, -4, 7.3, 0.7, 3, 8.7, 'black'),
        (-0.85, -3.2, 7.15, 0.85, -2.4, 8.85, 'violet'), (-0.85, -1.2, 7.15, 0.85, -0.4, 8.85, 'violet'),
        (-0.85, 0.8, 7.15, 0.85, 1.6, 8.85, 'violet'),
        (-2.6, 3, 7.0, 2.6, 4, 9.0, 'steel'),
        (-3.3, 3.4, 7.3, -2.6, 5.2, 8.7, 'black'), (2.6, 3.4, 7.3, 3.3, 5.2, 8.7, 'black'),
        (-1.0, 4, 7.5, 1.0, 17, 8.5, 'steel'),
        (-1.0, 4, 7.4, -0.6, 17, 8.6, 'steel_dark'), (0.6, 4, 7.4, 1.0, 17, 8.6, 'edge'),
        (-0.2, 6, 8.5, 0.2, 15, 8.65, 'violet'),
        (-0.7, 17, 7.6, 0.7, 18.5, 8.4, 'steel'), (-0.3, 18.5, 7.8, 0.3, 19.8, 8.2, 'edge'),
    ]


def _bow():
    """Лук: дуга ступенями (углы наклона у блоков модели ограничены), тетива, золотые наконечники."""
    steps = [(4, 7, 1.2, 2.8), (7, 10, 0.4, 2.0), (10, 13, -0.4, 1.2), (13, 15.5, -1.2, 0.4)]
    out = []
    for (y1, y2, x1, x2) in steps:
        out.append((x1, y1, 7.4, x2, y2, 8.6, 'silver'))
        out.append((x1 + 0.2, y1, 8.6, x2 - 0.2, y2, 8.75, 'green'))
        out.append((x1, 2 - y2, 7.4, x2, 2 - y1, 8.6, 'silver'))
        out.append((x1 + 0.2, 2 - y2, 8.6, x2 - 0.2, 2 - y1, 8.75, 'green'))
    out.append((-1.8, 13.5, 7.3, -0.4, 15.5, 8.7, 'gold'))
    out.append((-1.8, -13.5, 7.3, -0.4, -11.5, 8.7, 'gold'))
    out.append((1.0, -2.0, 7.2, 3.0, 4.0, 8.8, 'wrap'))
    out.append((1.4, 0.2, 8.8, 2.6, 1.8, 9.1, 'blue'))
    out.append((-1.15, -12.5, 7.85, -0.95, 14.5, 8.15, 'string'))
    return out


# ---------------------------------------------------------------- оружие
# бокс: (x1, y1, z1, x2, y2, z2, цвет). Клинок вдоль +Y, рукоять у y~0, толщина вокруг z=8.
WEAPONS = {
    'balmung': {
        'colors': {'steel': C(205, 212, 226), 'steel_dark': C(150, 158, 178), 'gold': C(236, 192, 72), 'gold_dark': C(170, 120, 40),
                   'leather': C(84, 52, 36), 'leather_dark': C(54, 34, 24), 'blue': C(96, 206, 255), 'blue_dark': C(44, 114, 196),
                   'red': C(204, 42, 52)},
        'boxes': [
            (-1.5, -3, 7, 1.5, -1, 9, 'gold'), (-0.6, -2.6, 8.9, 0.6, -1.4, 9.3, 'blue'), (-0.6, -2.6, 6.7, 0.6, -1.4, 7.1, 'blue'),
            (-0.8, -1, 7.2, 0.8, 5, 8.8, 'leather'),
            (-0.95, 0, 7.05, 0.95, 0.8, 8.95, 'leather_dark'), (-0.95, 1.8, 7.05, 0.95, 2.6, 8.95, 'leather_dark'),
            (-0.95, 3.6, 7.05, 0.95, 4.4, 8.95, 'leather_dark'),
            (-5, 5, 6.8, 5, 6.6, 9.2, 'gold'), (-3, 5, 6.7, 3, 5.6, 9.3, 'gold_dark'),
            (-6.2, 6.6, 7.2, -4.2, 8.8, 8.8, 'gold'), (4.2, 6.6, 7.2, 6.2, 8.8, 8.8, 'gold'),
            (-1, 5.2, 9.2, 1, 6.4, 9.6, 'blue'), (-1, 5.2, 6.4, 1, 6.4, 6.8, 'blue'),
            (-2, 6.6, 7.2, 2, 24.5, 8.8, 'steel'),
            (-2, 6.6, 7.4, -1.4, 24.5, 8.6, 'steel_dark'), (1.4, 6.6, 7.4, 2, 24.5, 8.6, 'steel_dark'),
            (-0.5, 8, 8.8, 0.5, 23, 9.0, 'blue'), (-0.5, 8, 7.0, 0.5, 23, 7.2, 'blue'),
            (-1.0, 10, 8.8, -0.55, 10.9, 9.0, 'blue_dark'), (0.55, 13, 8.8, 1.0, 13.9, 9.0, 'blue_dark'),
            (-1.0, 16, 8.8, -0.55, 16.9, 9.0, 'blue_dark'), (0.55, 19, 8.8, 1.0, 19.9, 9.0, 'blue_dark'),
            (-1.2, 24.5, 7.4, 1.2, 26, 8.6, 'steel'), (-0.5, 26, 7.7, 0.5, 27.2, 8.3, 'steel'),
        ]},
    'clarent': {
        'colors': {'blade': C(74, 18, 30), 'edge': C(196, 200, 212), 'red': C(234, 54, 66), 'gold': C(234, 190, 72),
                   'gold_dark': C(168, 118, 40), 'black': C(30, 24, 28), 'leather': C(62, 32, 32)},
        'boxes': [
            (-1, -2.5, 7.3, 1, -1, 8.7, 'gold'), (-0.7, -1, 7.4, 0.7, 4.5, 8.6, 'leather'),
            (-0.85, 0.2, 7.25, 0.85, 1, 8.75, 'black'), (-0.85, 2.2, 7.25, 0.85, 3, 8.75, 'black'),
            (-3.8, 4.5, 7, 3.8, 5.8, 9, 'gold'), (-4.6, 3.8, 7.4, -3.2, 5, 8.6, 'gold_dark'), (3.2, 3.8, 7.4, 4.6, 5, 8.6, 'gold_dark'),
            (-0.8, 4.6, 9, 0.8, 5.7, 9.4, 'red'), (-0.8, 4.6, 6.6, 0.8, 5.7, 7, 'red'),
            (-1.6, 5.8, 7.4, 1.6, 22, 8.6, 'blade'),
            (-1.6, 5.8, 7.3, -1.1, 22, 8.7, 'edge'), (1.1, 5.8, 7.3, 1.6, 22, 8.7, 'edge'),
            (-0.35, 7, 8.6, 0.35, 20, 8.8, 'red'), (-0.35, 7, 7.2, 0.35, 20, 7.4, 'red'),
            (-1.1, 22, 7.5, 1.1, 23.5, 8.5, 'blade'), (-0.4, 23.5, 7.7, 0.4, 24.8, 8.3, 'edge'),
        ]},
    'vasavi_shakti': {
        'colors': {'gold': C(242, 206, 92), 'gold_dark': C(182, 132, 42), 'white': C(246, 246, 252), 'blue': C(124, 192, 255),
                   'blue_dark': C(72, 124, 224), 'flame': C(255, 164, 62)},
        'boxes': [
            (-0.9, -5, 7.1, 0.9, -3.5, 8.9, 'gold'), (-0.55, -3.5, 7.45, 0.55, 16, 8.55, 'white'),
            (-0.7, 2, 7.3, 0.7, 2.8, 8.7, 'gold'), (-0.7, 6, 7.3, 0.7, 6.8, 8.7, 'gold'),
            (-0.7, 10, 7.3, 0.7, 10.8, 8.7, 'gold'), (-0.7, 14, 7.3, 0.7, 14.8, 8.7, 'gold'),
            (-2.4, 15.5, 6.9, 2.4, 16.7, 9.1, 'gold'), (-1.4, 15.6, 9.1, 1.4, 16.6, 9.35, 'flame'), (-1.4, 15.6, 6.65, 1.4, 16.6, 6.9, 'flame'),
            (-2.6, 16.7, 7.5, -1.6, 19, 8.5, 'blue_dark'), (1.6, 16.7, 7.5, 2.6, 19, 8.5, 'blue_dark'),
            (-1.6, 16.7, 7.3, 1.6, 21, 8.7, 'blue'), (-1.1, 21, 7.4, 1.1, 24, 8.6, 'blue'), (-0.5, 24, 7.6, 0.5, 26.5, 8.4, 'white'),
            (-0.3, 17, 8.7, 0.3, 24, 8.9, 'white'), (-0.3, 17, 7.1, 0.3, 24, 7.3, 'white'),
        ]},
    'argalia_lance': {
        'colors': {'gold': C(244, 208, 96), 'gold_dark': C(184, 134, 44), 'white': C(248, 246, 250), 'pink': C(240, 132, 172),
                   'sky': C(134, 202, 244)},
        'boxes': [
            (-0.8, -5, 7.2, 0.8, -3.5, 8.8, 'gold'), (-0.6, -3.5, 7.4, 0.6, 15, 8.6, 'white'),
            (-0.75, 1, 7.25, 0.75, 1.9, 8.75, 'pink'), (-0.75, 4, 7.25, 0.75, 4.9, 8.75, 'pink'), (-0.75, 7.5, 7.25, 0.75, 8.4, 8.75, 'pink'),
            (-0.75, 10.5, 7.25, 0.75, 11.4, 8.75, 'pink'), (-0.75, 13, 7.25, 0.75, 13.9, 8.75, 'pink'),
            (-2.2, 5.8, 7, 2.2, 6.8, 9, 'gold'), (-0.6, 5.9, 9, 0.6, 6.7, 9.3, 'sky'), (-0.6, 5.9, 6.7, 0.6, 6.7, 7, 'sky'),
            (-1.1, 15, 7.2, 1.1, 16, 8.8, 'sky'),
            (-0.9, 16, 7.3, 0.9, 26, 8.7, 'gold'), (-0.5, 26, 7.6, 0.5, 28, 8.4, 'gold'), (-0.2, 28, 7.8, 0.2, 29.2, 8.2, 'white'),
            (-0.3, 17, 8.7, 0.3, 25, 8.85, 'gold_dark'), (-0.3, 17, 7.15, 0.3, 25, 7.3, 'gold_dark'),
        ]},
    'la_pucelle': {
        'colors': {'white': C(242, 242, 248), 'gold': C(236, 194, 76), 'gold_dark': C(170, 122, 42), 'cloth': C(252, 252, 255),
                   'cloth_shade': C(214, 220, 238), 'blue': C(62, 92, 192)},
        'boxes': [
            (-0.5, -4, 7.5, 0.5, 23, 8.5, 'white'),
            (-0.7, -0.4, 7.3, 0.7, 0.4, 8.7, 'gold'), (-0.7, 7.6, 7.3, 0.7, 8.4, 8.7, 'gold'), (-0.7, 15.6, 7.3, 0.7, 16.4, 8.7, 'gold'),
            (-0.9, 23, 7.1, 0.9, 24.2, 8.9, 'gold'), (-0.5, 24.2, 7.5, 0.5, 27, 8.5, 'gold'),
            (0.5, 10, 7.8, 9.5, 22, 8.2, 'cloth'), (0.5, 10, 7.7, 9.5, 14, 8.3, 'cloth_shade'),
            (0.5, 22, 7.7, 9.5, 22.8, 8.3, 'gold'), (0.5, 9.2, 7.7, 9.5, 10, 8.3, 'gold'), (9.5, 9.2, 7.7, 10.3, 22.8, 8.3, 'gold'),
            (4, 14.5, 8.2, 6, 19.5, 8.4, 'blue'), (2.5, 16.2, 8.2, 7.5, 17.6, 8.4, 'blue'),
            (4, 14.5, 7.6, 6, 19.5, 7.8, 'blue'), (2.5, 16.2, 7.6, 7.5, 17.6, 7.8, 'blue'),
        ]},

    'black_keys': {
        'colors': {'black': C(34, 30, 42), 'steel': C(150, 156, 174), 'steel_dark': C(104, 108, 128), 'edge': C(220, 224, 236),
                   'violet': C(124, 78, 178), 'gold': C(204, 164, 72)},
        'boxes': _shift(_black_key(), -3.4, -1.0, -2.0) + _black_key()},
    'kazikli_bey': {
        'colors': {'wood': C(98, 66, 46), 'wood_dark': C(62, 40, 30), 'iron': C(96, 96, 112), 'iron_light': C(156, 156, 174),
                   'blood': C(176, 26, 38), 'blood_dark': C(112, 16, 28), 'bone': C(228, 222, 202)},
        'boxes': [
            (-0.4, -6.2, 7.6, 0.4, -5, 8.4, 'iron_light'), (-0.9, -5, 7.1, 0.9, -3.8, 8.9, 'iron'),
            (-0.6, -3.8, 7.4, 0.6, 18, 8.6, 'wood'), (-0.6, -3.8, 7.4, -0.2, 18, 8.6, 'wood_dark'),
            (-0.8, 0, 7.2, 0.8, 0.9, 8.8, 'blood_dark'), (-0.8, 5, 7.2, 0.8, 5.9, 8.8, 'blood_dark'),
            (-0.8, 10, 7.2, 0.8, 10.9, 8.8, 'blood_dark'),
            (-1.0, 13, 7.0, 1.0, 15, 9.0, 'bone'), (-0.5, 13.6, 9.0, 0.5, 14.4, 9.1, 'wood_dark'),
            (-1.3, 18, 7.1, 1.3, 19.2, 8.9, 'iron'),
            (-0.9, 19.2, 7.4, 0.9, 25, 8.6, 'iron_light'), (-0.5, 25, 7.6, 0.5, 28, 8.4, 'iron_light'),
            (-0.2, 28, 7.8, 0.2, 30, 8.2, 'iron_light'),
            (-3.2, 18.4, 7.4, -2.0, 19.5, 8.6, 'iron'), (-3.0, 19.5, 7.5, -2.0, 23, 8.5, 'iron_light'),
            (-2.7, 23, 7.7, -2.2, 24.5, 8.3, 'iron_light'),
            (2.0, 18.4, 7.4, 3.2, 19.5, 8.6, 'iron'), (2.0, 19.5, 7.5, 3.0, 23, 8.5, 'iron_light'),
            (2.2, 23, 7.7, 2.7, 24.5, 8.3, 'iron_light'),
            (-0.9, 19.4, 8.6, 0.9, 22, 8.75, 'blood'), (-0.9, 19.4, 7.25, 0.9, 22, 7.4, 'blood'),
        ]},
    'achilles_spear': {
        'colors': {'ash': C(178, 144, 100), 'ash_dark': C(130, 100, 66), 'bronze': C(206, 152, 72), 'bronze_dark': C(150, 102, 42),
                   'blue': C(112, 172, 238), 'white': C(246, 247, 252), 'steel': C(196, 204, 222)},
        'boxes': [
            (-0.7, -5, 7.3, 0.7, -3.8, 8.7, 'bronze'),
            (-0.6, -3.8, 7.4, 0.6, 19, 8.6, 'ash'),
            (-0.6, -2, 8.6, -0.2, 6, 8.7, 'ash_dark'), (0.2, 8, 8.6, 0.6, 16, 8.7, 'ash_dark'),
            (-0.8, 1, 7.2, 0.8, 8, 8.8, 'ash_dark'),
            (-0.85, 8.4, 7.15, 0.85, 9.2, 8.85, 'bronze'), (-0.85, 15.4, 7.15, 0.85, 16.2, 8.85, 'bronze'),
            (-1.0, 19, 7.2, 1.0, 21, 8.8, 'bronze'), (-0.7, 19.6, 8.8, 0.7, 20.4, 9.0, 'blue'),
            (-1.2, 21, 7.5, 1.2, 22.5, 8.5, 'steel'), (-2.0, 22.5, 7.5, 2.0, 26, 8.5, 'white'),
            (-1.4, 26, 7.6, 1.4, 28, 8.4, 'white'), (-0.5, 28, 7.8, 0.5, 30, 8.2, 'white'),
            (-2.0, 22.5, 7.4, -1.5, 26, 8.6, 'steel'), (1.5, 22.5, 7.4, 2.0, 26, 8.6, 'steel'),
            (-0.25, 21.5, 8.5, 0.25, 29, 8.65, 'blue'), (-0.25, 21.5, 7.35, 0.25, 29, 7.5, 'blue'),
            (-0.5, 17.6, 6.6, 0.5, 19, 7.2, 'bronze_dark'),
        ]},
    'atalanta_bow': {
        'colors': {'silver': C(214, 222, 240), 'silver_dark': C(150, 166, 204), 'gold': C(240, 200, 92), 'green': C(90, 170, 112),
                   'string': C(244, 244, 250), 'wrap': C(84, 52, 42), 'blue': C(120, 190, 255)},
        'boxes': _bow()},
    'semiramis_staff': {
        'colors': {'black': C(42, 32, 54), 'gold': C(238, 196, 84), 'gold_dark': C(172, 124, 44), 'purple': C(142, 72, 202),
                   'purple_dark': C(88, 42, 142), 'glow': C(210, 160, 255)},
        'boxes': [
            (-0.4, -8, 7.6, 0.4, -6, 8.4, 'gold'),
            (-0.6, -6, 7.4, 0.6, 20, 8.6, 'black'),
            (-0.85, -2.4, 7.15, 0.85, -1.6, 8.85, 'gold'), (-0.85, 5.6, 7.15, 0.85, 6.4, 8.85, 'gold'),
            (-0.85, 13.6, 7.15, 0.85, 14.4, 8.85, 'gold'), (-0.45, 0, 8.6, 0.45, 18, 8.75, 'purple_dark'),
            (-1.2, 20, 7.0, 1.2, 21.4, 9.0, 'gold'),
            (-2.0, 21.4, 7.0, -1.2, 26.5, 9.0, 'gold_dark'), (1.2, 21.4, 7.0, 2.0, 26.5, 9.0, 'gold_dark'),
            (-1.2, 22, 7.2, 1.2, 26, 8.8, 'purple'), (-0.7, 26, 7.4, 0.7, 27.6, 8.6, 'glow'),
            (-0.6, 22.6, 8.8, 0.6, 25, 9.05, 'glow'),
            (-4.0, 22.5, 7.5, -2.0, 23.6, 8.5, 'gold'), (-4.9, 23.6, 7.6, -4.0, 27, 8.4, 'gold'),
            (2.0, 22.5, 7.5, 4.0, 23.6, 8.5, 'gold'), (4.0, 23.6, 7.6, 4.9, 27, 8.4, 'gold'),
            (-0.4, 27.6, 7.6, 0.4, 29.2, 8.4, 'gold'),
        ]},
    'ripper_knife': {
        'colors': {'steel': C(188, 194, 210), 'steel_dark': C(124, 130, 150), 'edge': C(236, 240, 248), 'handle': C(62, 42, 38),
                   'wrap': C(150, 30, 42), 'gold': C(208, 168, 78), 'red': C(176, 24, 36)},
        'boxes': [
            (-1.1, -5.2, 7.4, 1.1, -4, 8.6, 'gold'),
            (-0.8, -4, 7.3, 0.8, 3, 8.7, 'handle'),
            (-0.95, -3.2, 7.15, 0.95, -2.4, 8.85, 'wrap'), (-0.95, -1.2, 7.15, 0.95, -0.4, 8.85, 'wrap'),
            (-0.95, 0.8, 7.15, 0.95, 1.6, 8.85, 'wrap'),
            (-2.2, 3, 7.0, 2.2, 4, 9.0, 'gold'),
            (-1.5, 4, 7.5, 1.5, 12, 8.5, 'steel'),
            (-1.5, 4, 7.4, -0.9, 12, 8.6, 'steel_dark'), (0.9, 4, 7.4, 1.5, 12, 8.6, 'edge'),
            (-1.1, 12, 7.6, 1.1, 14, 8.4, 'steel'), (-0.6, 14, 7.7, 0.6, 15.5, 8.3, 'edge'),
            (-0.2, 15.5, 7.8, 0.2, 16.4, 8.2, 'edge'),
            (-0.3, 6, 8.5, 0.3, 9, 8.65, 'red'), (-0.3, 10, 8.5, 0.3, 10.8, 8.65, 'red'),
        ]},
    'spartacus_blade': {
        'colors': {'iron': C(124, 124, 138), 'iron_dark': C(80, 82, 96), 'rust': C(142, 86, 52), 'edge': C(204, 208, 218),
                   'leather': C(86, 58, 42), 'chain': C(72, 72, 84), 'red': C(164, 32, 38)},
        'boxes': [
            (-1.4, -5, 7.1, 1.4, -3, 8.9, 'iron_dark'),
            (-0.8, -3, 7.2, 0.8, 4.4, 8.8, 'leather'),
            (-0.95, -2, 7.05, 0.95, -1.2, 8.95, 'red'), (-0.95, 0, 7.05, 0.95, 0.8, 8.95, 'red'), (-0.95, 2, 7.05, 0.95, 2.8, 8.95, 'red'),
            (-4.5, 4.4, 7.0, 4.5, 6, 9.0, 'iron_dark'),
            (-5.4, 3.4, 7.3, -4.4, 4.4, 8.7, 'chain'), (-5.1, 2.0, 7.6, -4.7, 3.4, 8.4, 'iron_dark'),
            (-5.4, 0.8, 7.3, -4.4, 2.0, 8.7, 'chain'), (-5.1, -0.6, 7.6, -4.7, 0.8, 8.4, 'iron_dark'),
            (4.4, 3.4, 7.3, 5.4, 4.4, 8.7, 'chain'), (4.7, 2.0, 7.6, 5.1, 3.4, 8.4, 'iron_dark'),
            (4.4, 0.8, 7.3, 5.4, 2.0, 8.7, 'chain'), (4.7, -0.6, 7.6, 5.1, 0.8, 8.4, 'iron_dark'),
            (-2.6, 6, 7.2, 2.6, 24, 8.8, 'iron'),
            (2.0, 6, 7.1, 2.6, 24, 8.9, 'edge'), (-2.6, 6, 7.1, -2.0, 24, 8.9, 'iron_dark'),
            (-1.2, 10, 8.8, 0.4, 13, 8.95, 'rust'), (0.2, 17, 8.8, 1.6, 19.5, 8.95, 'rust'), (-1.8, 7, 7.05, -0.8, 9, 7.2, 'rust'),
            (-2.0, 24, 7.3, 2.0, 26, 8.7, 'iron'), (-1.2, 26, 7.4, 1.2, 28, 8.6, 'iron'), (-0.5, 28, 7.6, 0.5, 29.5, 8.4, 'edge'),
        ]},
}

HAND_DISPLAY = {
    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]},
}




def palette(colors):
    names = list(colors)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i, n in enumerate(names):
        x0, y0 = (i % 8) * 2, (i // 8) * 2
        for dx in range(2):
            for dy in range(2):
                px[x0 + dx, y0 + dy] = colors[n]
    return img, names


def write_model(wid, spec):
    img, names = palette(spec['colors'])
    img.save(os.path.join(TEX, f'{wid}_palette.png'))
    elements = []
    for (x1, y1, z1, x2, y2, z2, col) in spec['boxes']:
        k = names.index(col)
        u0, v0 = (k % 8) * 2, (k // 8) * 2
        uv = [u0, v0, u0 + 2, v0 + 2]
        face = {'uv': uv, 'texture': '#0'}
        elements.append({
            'from': [x1, y1, z1], 'to': [x2, y2, z2],
            'rotation': {'origin': [0, 0, 8], 'axis': 'z', 'angle': -45},
            'faces': {f: dict(face) for f in ('north', 'east', 'south', 'west', 'up', 'down')},
        })
    model3d = {'textures': {'0': f'fateapocrypha:item/{wid}_palette', 'particle': f'fateapocrypha:item/{wid}_palette'},
               'elements': elements, 'display': HAND_DISPLAY, 'gui_light': 'front'}
    with open(os.path.join(MOD, f'{wid}_3d.json'), 'w') as f:
        json.dump(model3d, f, indent=1)
    wrapper = {
        'loader': 'forge:separate_transforms',
        'gui_light': 'front',
        'base': {'parent': f'fateapocrypha:item/{wid}_3d'},
        'perspectives': {'gui': {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'fateapocrypha:item/{wid}'}}},
    }
    with open(os.path.join(MOD, f'{wid}.json'), 'w') as f:
        json.dump(wrapper, f, indent=1)


def draw_icon(wid, spec, size=32, ss=8):
    """Иконка из той же геометрии: вид спереди, поворот на -45 градусов, подгонка по размеру, тёмный контур."""
    cs, sn = math.cos(math.radians(-45)), math.sin(math.radians(-45))

    def rot(x, y):
        return x * cs - y * sn, x * sn + y * cs

    polys = []
    xs, ys = [], []
    for (x1, y1, z1, x2, y2, z2, col) in spec['boxes']:
        pts = [rot(x1, y1), rot(x2, y1), rot(x2, y2), rot(x1, y2)]
        polys.append((pts, spec['colors'][col], (z1 + z2) / 2))
        for p in pts:
            xs.append(p[0])
            ys.append(p[1])
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    span = max(maxx - minx, maxy - miny)
    scale = (size * ss - 4 * ss) / span
    ox = (size * ss - (maxx - minx) * scale) / 2 - minx * scale
    oy = (size * ss - (maxy - miny) * scale) / 2 + maxy * scale

    S = size * ss
    layer = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for pts, col, _z in polys:
        d.polygon([(ox + x * scale, oy - y * scale) for x, y in pts], fill=col)
    alpha = layer.split()[3]
    outline_a = alpha.filter(ImageFilter.MaxFilter(ss * 2 + 1))
    outline = Image.new('RGBA', (S, S), (22, 14, 20, 255))
    outline.putalpha(outline_a)
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out.alpha_composite(outline)
    out.alpha_composite(layer)
    out.resize((size, size), Image.LANCZOS).save(os.path.join(TEX, f'{wid}.png'))


# ---------------------------------------------------------------- материалы
def mat_canvas(ss=8, size=32):
    S = size * ss
    return Image.new('RGBA', (S, S), (0, 0, 0, 0)), S


def finish(img, name, ss=8, size=32, glow=None):
    S = size * ss
    alpha = img.split()[3]
    outline = Image.new('RGBA', (S, S), (22, 14, 20, 255))
    outline.putalpha(alpha.filter(ImageFilter.MaxFilter(ss * 2 + 1)))
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    if glow:
        g = Image.new('RGBA', (S, S), glow)
        g.putalpha(alpha.filter(ImageFilter.GaussianBlur(ss * 1.6)).point(lambda v: int(v * 0.5)))
        out.alpha_composite(g)
    out.alpha_composite(outline)
    out.alpha_composite(img)
    out.resize((size, size), Image.LANCZOS).save(os.path.join(TEX, f'{name}.png'))


def poly(d, pts, col, u):
    d.polygon([(x * u, y * u) for x, y in pts], fill=col)


def icon_prana(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(16, 3), (24, 10), (22, 24), (16, 29), (10, 24), (8, 10)], C(70, 170, 235), u)
    poly(d, [(16, 3), (24, 10), (16, 13)], C(150, 225, 255), u)
    poly(d, [(16, 3), (8, 10), (16, 13)], C(110, 200, 250), u)
    poly(d, [(8, 10), (16, 13), (16, 29), (10, 24)], C(50, 130, 215), u)
    poly(d, [(24, 10), (16, 13), (16, 29), (22, 24)], C(40, 110, 190), u)
    poly(d, [(14, 8), (16, 6), (17, 9), (15, 11)], C(235, 250, 255), u)
    finish(img, name, glow=(90, 200, 255, 255))


def icon_holy_steel(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(5, 14), (11, 9), (27, 9), (22, 14)], C(240, 244, 252), u)         # верх
    poly(d, [(5, 14), (22, 14), (22, 23), (5, 23)], C(190, 198, 214), u)         # лицо
    poly(d, [(22, 14), (27, 9), (27, 18), (22, 23)], C(140, 150, 172), u)        # бок
    poly(d, [(5, 17), (22, 17), (22, 19), (5, 19)], C(236, 192, 72), u)          # золотая полоса
    poly(d, [(22, 17), (27, 12.5), (27, 14.5), (22, 19)], C(170, 124, 42), u)
    poly(d, [(8, 11.5), (13, 9.8), (14, 10.4), (9, 12.2)], C(255, 255, 255), u)
    finish(img, name, glow=(255, 240, 170, 255))


def icon_dragon_scale(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(16, 3), (27, 9), (26, 21), (16, 30), (6, 21), (5, 9)], C(210, 70, 40), u)
    poly(d, [(16, 3), (27, 9), (16, 12)], C(250, 140, 70), u)
    poly(d, [(16, 3), (5, 9), (16, 12)], C(235, 105, 55), u)
    poly(d, [(5, 9), (16, 12), (16, 30), (6, 21)], C(170, 46, 30), u)
    poly(d, [(27, 9), (16, 12), (16, 30), (26, 21)], C(140, 34, 26), u)
    for y in (15, 20, 25):
        poly(d, [(11, y), (16, y + 2), (21, y), (21, y + 1), (16, y + 3), (11, y + 1)], C(255, 190, 90), u)
    finish(img, name, glow=(255, 120, 50, 255))


def icon_fafnir_blood(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(13, 4), (19, 4), (19, 8), (13, 8)], C(150, 100, 60), u)            # пробка
    poly(d, [(14, 8), (18, 8), (18, 12), (24, 22), (24, 27), (8, 27), (8, 22), (14, 12)], C(210, 225, 235), u)
    poly(d, [(14, 13), (18, 13), (23, 22), (23, 26), (9, 26), (9, 22)], C(150, 14, 28), u)
    poly(d, [(15, 13), (17, 13), (20, 19), (12, 19)], C(215, 40, 50), u)
    poly(d, [(11, 21), (13, 21), (13, 25), (11, 25)], C(255, 120, 110), u)
    finish(img, name, glow=(230, 40, 50, 255))


def icon_crimson_shard(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(16, 2), (23, 12), (21, 28), (14, 30), (9, 20), (11, 9)], C(190, 28, 44), u)
    poly(d, [(16, 2), (23, 12), (16, 15)], C(250, 100, 108), u)
    poly(d, [(16, 2), (11, 9), (16, 15)], C(225, 60, 74), u)
    poly(d, [(11, 9), (16, 15), (14, 30), (9, 20)], C(130, 18, 32), u)
    poly(d, [(23, 12), (16, 15), (14, 30), (21, 28)], C(100, 12, 26), u)
    poly(d, [(15, 12), (17, 16), (14, 23), (15, 17)], C(25, 10, 18), u)
    finish(img, name, glow=(255, 60, 80, 255))


def icon_lightning_core(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    d.ellipse([4 * u, 4 * u, 28 * u, 28 * u], fill=C(40, 70, 150))
    d.ellipse([6 * u, 6 * u, 26 * u, 26 * u], fill=C(70, 130, 235))
    d.ellipse([9 * u, 8 * u, 21 * u, 18 * u], fill=C(130, 190, 255))
    poly(d, [(18, 6), (11, 17), (16, 17), (13, 27), (22, 14), (17, 14), (20, 6)], C(255, 240, 120), u)
    poly(d, [(18, 8), (13, 17), (17, 17), (15, 24), (20, 15), (16, 15), (19, 8)], C(255, 255, 220), u)
    finish(img, name, glow=(120, 190, 255, 255))


def icon_feather(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(25, 3), (28, 7), (20, 16), (12, 22), (7, 25), (9, 19), (14, 11), (19, 6)], C(248, 248, 252), u)
    poly(d, [(25, 3), (20, 16), (12, 22), (19, 6)], C(215, 226, 246), u)
    poly(d, [(25, 3), (28, 7), (24, 10), (22, 6)], C(150, 205, 245), u)
    poly(d, [(26, 4), (6, 27), (5, 26), (25, 3)], C(236, 192, 72), u)
    poly(d, [(8, 24), (4, 29), (6, 29), (9, 26)], C(190, 140, 50), u)
    finish(img, name, glow=(255, 255, 255, 255))


def icon_holy_cloth(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(4, 8), (16, 5), (28, 8), (28, 26), (16, 29), (4, 26)], C(246, 246, 252), u)
    poly(d, [(16, 5), (28, 8), (28, 26), (16, 29)], C(214, 220, 238), u)
    poly(d, [(4, 8), (16, 5), (16, 8), (4, 11)], C(255, 255, 255), u)
    poly(d, [(4, 22), (16, 25), (28, 22), (28, 24), (16, 27), (4, 24)], C(236, 192, 72), u)
    poly(d, [(4, 10), (16, 7), (28, 10), (28, 12), (16, 9), (4, 12)], C(236, 192, 72), u)
    poly(d, [(14.5, 13), (17.5, 13), (17.5, 22), (14.5, 22)], C(62, 92, 192), u)
    poly(d, [(11, 16), (21, 16), (21, 18.5), (11, 18.5)], C(62, 92, 192), u)
    finish(img, name, glow=(255, 250, 220, 255))



def icon_black_steel(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(5, 14), (11, 9), (27, 9), (22, 14)], C(96, 92, 120), u)
    poly(d, [(5, 14), (22, 14), (22, 23), (5, 23)], C(46, 42, 62), u)
    poly(d, [(22, 14), (27, 9), (27, 18), (22, 23)], C(30, 28, 44), u)
    poly(d, [(5, 17), (22, 17), (22, 19), (5, 19)], C(140, 92, 200), u)
    poly(d, [(22, 17), (27, 12.5), (27, 14.5), (22, 19)], C(96, 60, 148), u)
    poly(d, [(8, 11.5), (13, 9.8), (14, 10.4), (9, 12.2)], C(190, 180, 230), u)
    poly(d, [(7, 20.5), (12, 20.5), (12, 21.5), (7, 21.5)], C(86, 80, 112), u)
    finish(img, name, glow=(150, 100, 220, 255))


def icon_blood_thorn(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(6, 28), (9, 22), (12, 24), (9, 29)], C(226, 220, 200), u)           # костяное основание
    poly(d, [(9, 22), (12, 24), (14, 20), (11, 18)], C(196, 190, 168), u)
    poly(d, [(11, 18), (14, 20), (24, 4), (26, 3), (22, 14), (18, 20), (15, 22)], C(140, 22, 36), u)   # шип
    poly(d, [(14, 20), (24, 4), (26, 3), (21, 12)], C(216, 52, 66), u)
    poly(d, [(26, 3), (24, 4), (23, 8), (25, 7)], C(255, 150, 150), u)
    poly(d, [(14, 20), (16, 22), (17, 26), (15, 24)], C(255, 90, 100), u)          # капля крови
    finish(img, name, glow=(220, 40, 60, 255))


def icon_pelian_ash(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(5, 27), (7, 29), (27, 7), (25, 5)], C(176, 140, 96), u)               # ветвь
    poly(d, [(5, 27), (7, 29), (27, 7), (26, 6)], C(132, 100, 66), u)
    poly(d, [(10, 20), (14, 14), (16, 15), (12, 22)], C(176, 140, 96), u)
    poly(d, [(14, 14), (14, 8), (16, 9), (16, 15)], C(132, 100, 66), u)
    poly(d, [(18, 12), (23, 8), (24, 13), (20, 15)], C(96, 168, 98), u)              # листья
    poly(d, [(18, 12), (23, 8), (21, 12)], C(150, 214, 140), u)
    poly(d, [(13, 8), (9, 4), (15, 5), (16, 9)], C(96, 168, 98), u)
    poly(d, [(13, 8), (9, 4), (12, 7)], C(150, 214, 140), u)
    poly(d, [(9, 24), (13, 20), (14, 21), (10, 25)], C(206, 152, 72), u)              # бронзовое кольцо
    finish(img, name, glow=(190, 220, 150, 255))


def icon_silver_bowstring(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    for i, (cx, cy) in enumerate([(16, 20), (16, 16), (16, 12)]):
        d.ellipse([(cx - 10) * u, (cy - 5) * u, (cx + 10) * u, (cy + 5) * u], outline=C(150, 166, 204), width=int(2.6 * u))
        d.ellipse([(cx - 10) * u, (cy - 5) * u, (cx + 10) * u, (cy + 5) * u], outline=C(224, 232, 250), width=int(1.4 * u))
    poly(d, [(24, 12), (28, 6), (29, 7), (26, 13)], C(224, 232, 250), u)             # хвост нити
    poly(d, [(27, 5), (30, 4), (29, 8)], C(240, 200, 92), u)                          # золотой узел
    finish(img, name, glow=(200, 220, 255, 255))


def icon_gardens_gem(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(9, 20), (16, 29), (23, 20), (21, 25), (16, 30), (11, 25)], C(206, 160, 60), u)   # золотая оправа
    poly(d, [(16, 3), (24, 10), (23, 20), (16, 27), (9, 20), (8, 10)], C(48, 160, 112), u)
    poly(d, [(16, 3), (24, 10), (16, 13)], C(150, 236, 190), u)
    poly(d, [(16, 3), (8, 10), (16, 13)], C(104, 210, 160), u)
    poly(d, [(8, 10), (16, 13), (16, 27), (9, 20)], C(34, 126, 96), u)
    poly(d, [(24, 10), (16, 13), (16, 27), (23, 20)], C(24, 96, 82), u)
    poly(d, [(13, 8), (15, 6), (17, 8.5), (14.5, 10.5)], C(240, 255, 248), u)
    poly(d, [(18, 17), (21, 14), (21, 18), (19, 21)], C(190, 120, 230), u)           # фиолетовый отблеск Семирамис
    finish(img, name, glow=(90, 230, 170, 255))


def icon_fog_essence(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    poly(d, [(13, 4), (19, 4), (19, 8), (13, 8)], C(112, 88, 72), u)                 # пробка
    poly(d, [(14, 8), (18, 8), (18, 12), (25, 21), (25, 27), (7, 27), (7, 21), (14, 12)], C(214, 224, 232), u)
    poly(d, [(14, 13), (18, 13), (24, 21), (24, 26), (8, 26), (8, 21)], C(112, 118, 132), u)
    poly(d, [(9, 21), (16, 18), (23, 21), (21, 24), (16, 22), (11, 25)], C(190, 196, 208), u)   # клубы тумана
    poly(d, [(12, 16), (17, 14), (20, 17), (15, 18)], C(224, 228, 238), u)
    poly(d, [(11, 22), (13, 22), (13, 25), (11, 25)], C(250, 250, 255), u)
    poly(d, [(18, 23), (21, 23), (21, 24), (18, 24)], C(176, 40, 50), u)              # красные глаза Джека
    finish(img, name, glow=(190, 200, 220, 255))


def icon_broken_chain(name):
    img, S = mat_canvas(); d = ImageDraw.Draw(img); u = S / 32
    d.rounded_rectangle([3 * u, 12 * u, 17 * u, 24 * u], radius=int(5 * u), outline=C(86, 88, 104), width=int(4 * u))
    d.rounded_rectangle([3 * u, 12 * u, 17 * u, 24 * u], radius=int(5 * u), outline=C(160, 164, 182), width=int(2 * u))
    d.rounded_rectangle([13 * u, 8 * u, 29 * u, 20 * u], radius=int(5 * u), outline=C(86, 88, 104), width=int(4 * u))
    d.rounded_rectangle([13 * u, 8 * u, 29 * u, 20 * u], radius=int(5 * u), outline=C(160, 164, 182), width=int(2 * u))
    poly(d, [(22, 7), (26, 7), (26, 11), (22, 11)], (0, 0, 0, 0), u)
    poly(d, [(21, 6), (25, 9), (24, 12), (20, 10)], C(164, 36, 42), u)               # излом и кровь
    poly(d, [(6, 25), (9, 27), (7, 29), (5, 27)], C(142, 86, 52), u)                  # ржавчина
    finish(img, name, glow=(140, 150, 180, 255))


MATERIALS = {
    'prana_crystal': icon_prana, 'holy_steel': icon_holy_steel, 'dragon_scale': icon_dragon_scale,
    'fafnir_blood': icon_fafnir_blood, 'crimson_shard': icon_crimson_shard, 'lightning_core': icon_lightning_core,
    'hippogriff_feather': icon_feather, 'holy_cloth': icon_holy_cloth,
    'black_steel': icon_black_steel, 'blood_thorn': icon_blood_thorn, 'pelian_ash': icon_pelian_ash,
    'silver_bowstring': icon_silver_bowstring, 'gardens_gem': icon_gardens_gem,
    'fog_essence': icon_fog_essence, 'broken_chain': icon_broken_chain,
}

if __name__ == '__main__':
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MOD, exist_ok=True)
    for wid, spec in WEAPONS.items():
        write_model(wid, spec)
        draw_icon(wid, spec)
    for mid, fn in MATERIALS.items():
        fn(mid)
        with open(os.path.join(MOD, f'{mid}.json'), 'w') as f:
            json.dump({'parent': 'item/generated', 'textures': {'layer0': f'fateapocrypha:item/{mid}'}}, f, indent=1)
    print('готово: оружие', len(WEAPONS), 'материалы', len(MATERIALS))
