#!/usr/bin/env python3
"""Render the README GIFs from the Android Coupi master. Requires Pillow."""

from dataclasses import dataclass
from math import cos, pi, sin
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ARTWORK = ROOT / "android/app/src/main/assets/avatars"
OUTPUT = ROOT / "docs/media/coupi"
EDGE = 144
FRAME_MS = 60
PIXEL = Image.Resampling.NEAREST
IVORY = "#fff4ec"


@dataclass(frozen=True)
class Theme:
    file: str
    state: str
    colors: tuple[str, ...]
    mark: str = ""


THEMES = (
    Theme("idle", "idle", ()),
    Theme("tracking", "tracking", ("#78D6A3",), "chatgpt"),
    Theme("thinking", "thinking", ("#E69B71",), "claude"),
    Theme("working", "working", ("#8C9EFF",), "gemini"),
    Theme("completed", "completed", ("#6366F1",), "codex"),
    Theme("approval", "approval", ("#B68AEF",), "muse"),
    Theme("error", "error", ("#4D80F5",), "deepseek"),
    Theme("disconnected", "disconnected", ("#A6ADB8",), "grok"),
    Theme("perplexity", "thinking", ("#43C7C4",), "perplexity"),
    Theme("copilot", "tracking", ("#B8A9FA", "#95DDF1", "#9AE0BC", "#F6B8D5"), "copilot"),
)

QUESTION = (".XXX.", "X...X", "...X.", "..X..", ".....", "..X..")
WARNING = ("..X..", ".XXX.", ".X.X.", "XX.XX", "XXXXX")
SLEEP = ("XXXX", "..X.", ".X..", "XXXX")


def pixel_mark(draw, xy, rows, fill, edge=1):
    x, y = xy
    for row, line in enumerate(rows):
        for column, value in enumerate(line):
            if value != ".":
                left, top = x + column * edge, y + row * edge
                draw.rectangle((left, top, left + edge - 1, top + edge - 1), fill)


def recolor(image, colors):
    if not colors:
        return image.copy()
    palette = [tuple(bytes.fromhex(color[1:])) for color in colors]
    width, height = image.size
    result = []
    for y in range(height):
        for x in range(width):
            red, green, blue, alpha = image.getpixel((x, y))
            nx, ny = x / width, y / height
            cheek = 0.54 <= ny <= 0.62 and (0.21 <= nx <= 0.32 or 0.67 <= nx <= 0.80)
            if not alpha or cheek or blue - red < 4 or blue < 77 or red > 226:
                result.append((red, green, blue, alpha))
                continue
            gradient_index = nx * (len(palette) - 1)
            left = int(gradient_index)
            right = min(left + 1, len(palette) - 1)
            tint = [a + (b - a) * (gradient_index - left) for a, b in zip(palette[left], palette[right])]
            light = (red + green + blue) / 3
            if light < 158:
                tinted = [round(c * (0.28 + 0.72 * light / 158)) for c in tint]
            else:
                tinted = [round(c + (255 - c) * (light - 158) / 97) for c in tint]
            result.append((*tinted, alpha))
    recolored = Image.new("RGBA", image.size)
    recolored.putdata(result)
    return recolored


def equip(master, theme):
    width = 112
    sprite = master.resize((width, round(master.height * width / master.width)), PIXEL)
    sprite = recolor(sprite, theme.colors)
    if theme.mark:
        edge = round(sprite.width * 0.117)
        with Image.open(ARTWORK / "badges" / (theme.mark + ".png")) as badge:
            logo = badge.convert("RGBA").resize((edge, edge), Image.Resampling.LANCZOS)
        x = (sprite.width - edge) // 2
        y = round(sprite.height * 0.827 - edge / 2)
        sprite.alpha_composite(logo, (x, y))
    return sprite


def blinking(sprite):
    closed = sprite.copy()
    draw = ImageDraw.Draw(closed)
    w, h = sprite.size
    face = sprite.getpixel((round(w * 0.49), round(h * 0.42)))
    eye = sprite.getpixel((round(w * 0.35), round(h * 0.5)))
    for x in (0.344, 0.658):
        draw.rectangle((w * (x - 0.075), h * 0.350, w * (x + 0.075), h * 0.557), face)
        draw.rectangle((w * (x - 0.053), h * 0.492, w * (x + 0.053), h * 0.516), eye)
    return closed


def sprite_pose(state, ms, phase):
    if state == "tracking":
        return 4 * sin(phase), 4 * sin(phase * 2), 1, 4 * sin(phase)
    if state == "thinking":
        return 0, 2 * sin(phase), 1, 5 * sin(phase)
    if state == "working":
        return 0, -8 * abs(sin(phase * 2)), 1 + 0.026 * sin(phase * 2), 6 * sin(phase)
    if state == "completed" and ms < 1200:
        bounce = sin(pi * ms / 1200)
        return 0, -15 * bounce, 1 + 0.07 * bounce, 0
    if state == "approval":
        return 0, -2 * sin(phase), 1 + 0.05 * sin(phase), 0
    if state == "error" and ms < 560:
        return 9 * sin(ms / 145 * 2 * pi) * (1 - ms / 560), 0, 1, 0
    return 0, 0, 1 + 0.032 * sin(phase), 0


def effects(draw, state, accent, ms, phase):
    if state in ("tracking", "thinking"):
        for i in range(3):
            orbit = phase + i * 2 * pi / 3
            if state == "tracking" or (ms // 300 + i) % 3:
                x = 71 + round(37 * cos(orbit))
                y = 11 + round(5 * sin(orbit))
                draw.rectangle((x, y, x + 3, y + 3), IVORY if i == 0 else accent)
            if state == "thinking" and i <= (ms // 240) % 3:
                x = 61 + i * 9
                draw.rectangle((x, 132, x + 3, 135), accent)
    elif state == "working":
        for i in range(3):
            travel = ((ms + i * 160) % 540) / 540
            x, y = (13 if i % 2 == 0 else 126), 33 + round(67 * travel)
            draw.rectangle((x, y, x + 4, y + 2), accent if i % 2 == 0 else IVORY)
    elif state == "completed" and ms < 1200:
        for i in range(6):
            angle = i * pi / 3 + 0.3
            travel = ms / 1200
            x = 71 + round(cos(angle) * (44 + 22 * travel))
            y = 72 + round(sin(angle) * (38 + 22 * travel))
            draw.line((x - 3, y, x + 3, y), accent, 2)
            draw.line((x, y - 3, x, y + 3), IVORY, 2)
    elif state == "approval":
        pixel_mark(draw, (117, 6 + round(3 * sin(phase))), QUESTION, accent, 2)
    elif state == "error":
        pixel_mark(draw, (118, 9), WARNING, "#ffac79", 2)
    elif state == "disconnected":
        for offset in (0, 960):
            drift = ((ms + offset) % 1920) / 1920
            pixel_mark(draw, (109 + round(15 * drift), 24 - round(21 * drift)), SLEEP, accent,
                       1 if drift < 0.45 else 2)


def indexed(image, palette):
    opaque = image.getchannel("A").point(lambda a: 0 if a else 255)
    frame = image.convert("RGB").quantize(palette=palette, dither=Image.Dither.NONE)
    frame.paste(63, mask=opaque)
    frame.info["transparency"] = 63
    return frame


def animate(master, theme):
    sprite = equip(master, theme)
    blink = blinking(sprite)
    accent = theme.colors[0] if theme.colors else IVORY
    swatches = Image.new("RGB", (sprite.width, sprite.height + 1))
    swatches.paste(sprite, mask=sprite.getchannel("A"))
    swatches.putpixel((0, sprite.height), tuple(bytes.fromhex(accent[1:])))
    swatches.putpixel((1, sprite.height), (255, 172, 121))
    palette = swatches.quantize(colors=60)
    frames = []
    for step in range(48):
        ms, phase = step * FRAME_MS, 2 * pi * step / 48
        x, y, stretch, tilt = sprite_pose(theme.state, ms, phase)
        resting = theme.state in ("idle", "disconnected") or (theme.state == "completed" and ms >= 1200)
        art = blink if resting and step in (39, 40) else sprite
        shifted = art.resize((art.width, round(art.height * stretch)), PIXEL).rotate(tilt, PIXEL, expand=True)
        frame = Image.new("RGBA", (EDGE, EDGE))
        frame.alpha_composite(shifted, (round((EDGE - shifted.width) / 2 + x), round((EDGE - shifted.height) / 2 + y + 3)))
        effects(ImageDraw.Draw(frame), theme.state, accent, ms, phase)
        frames.append(indexed(frame, palette))
    frames[0].save(OUTPUT / (theme.file + ".gif"), save_all=True, append_images=frames[1:],
                   duration=FRAME_MS, loop=0, disposal=2, transparency=63, optimize=True)


def main():
    with Image.open(ARTWORK / "robot.png") as image:
        master = image.convert("RGBA")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for theme in THEMES:
        animate(master, theme)


if __name__ == "__main__":
    main()
