#!/usr/bin/env python3
"""Render Coupi’s shared face atlas and README GIFs. Requires Pillow."""

import json
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ARTWORK = ROOT / "android/app/src/main/assets/avatars"
OUTPUT = ROOT / "docs/media/coupi"
EDGE = 144
PIXEL = Image.Resampling.NEAREST
SPRITE_WIDTH = 112
INK = "#211f2d"


@dataclass(frozen=True)
class Theme:
    file: str
    state: str
    colors: tuple[str, ...]
    mark: str = ""


STATES = {
    "idle": "IDLE",
    "tracking": "TRACKING",
    "thinking": "THINKING",
    "working": "WORKING",
    "completed": "COMPLETED",
    "approval": "WAITING",
    "error": "ERROR",
    "disconnected": "DISCONNECTED",
}
PROVIDERS = {
    "chatgpt": ("#78D6A3",),
    "claude": ("#E69B71",),
    "gemini": ("#8C9EFF",),
    "deepseek": ("#4D80F5",),
    "perplexity": ("#43C7C4",),
    "muse": ("#B68AEF",),
    "copilot": ("#B8A9FA", "#95DDF1", "#9AE0BC", "#F6B8D5"),
    "grok": ("#A6ADB8",),
    "codex": ("#6366F1",),
}

EYES = ((29, 35, 48, 58), (64, 35, 84, 58))
EYE_X = (38, 74)
SMILE = ("XXXXXXX", ".XXXXX.", "..XXX..")
CLOSED = ("XX...XX", ".XXXXX.")
HEART = (".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X...")
QUESTION = (".XXX.", "XX.XX", "...XX", "..XX.", "..X..", ".....", "..X..")
WARNING = ("...X...", "..XXX..", "..X!X..", ".XX!XX.", ".XXXXX.", "XXX!XXX", "XXXXXXX")
CLOCK = ("...XXXXX...", "..XX...XX..", ".X.......X.", "XX.......XX",
         "X.........X", "X.........X", "X.........X", "XX.......XX",
         ".X.......X.", "..XX...XX..", "...XXXXX...")
BUBBLE = ("..XXXXXXXXX..", ".XX.......XX.", "XX.........XX", "X...........X",
          "X...........X", "XX.........XX", ".XXXXXXXXXXX.", "...XX........", "...X.........")
SLEEP = ("XXXXXXX", ".....XX", "....XX.", "...XX..", "..XX...", ".XX....", "XXXXXXX")

def glyph(draw, center, rows, colors, cell=2):
    x = center[0] - len(rows[0]) * cell // 2
    y = center[1] - len(rows) * cell // 2
    for row, line in enumerate(rows):
        for col, value in enumerate(line):
            if value in colors:
                left, top = x + col * cell, y + row * cell
                draw.rectangle((left, top, left + cell - 1, top + cell - 1), fill=colors[value])


def read_timeline():
    timelines = {}
    for line in (ARTWORK / "face-timeline.txt").read_text().splitlines():
        fields = line.partition("#")[0].split()
        if fields:
            state, *frames = fields
            timelines[state] = [(name, int(ms)) for name, ms in (frame.split(":") for frame in frames)]
    return timelines


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
    sprite = recolor(master, theme.colors)
    if theme.mark:
        edge = round(sprite.width * 0.19)
        with Image.open(ARTWORK / "badges" / (theme.mark + ".png")) as badge:
            logo = badge.convert("RGBA").resize((edge, edge), Image.Resampling.LANCZOS)
        x = (sprite.width - edge) // 2
        y = round(sprite.height * 0.827 - edge / 2)
        sprite.alpha_composite(logo, (x, y))
    return sprite


def source_art():
    source = json.loads((ARTWORK / "pixel-art.json").read_text())
    edge = source["size"]
    palette = [tuple(bytes.fromhex(color)) for color in source["palette"]]

    def sprite(pixels):
        image = Image.new("RGBA", (edge, edge))
        image.putdata([palette[pixel] for pixel in pixels])
        return image

    master = sprite(source["master"]).resize((128, 128), PIXEL)
    overlays = {name: sprite(pixels) for name, pixels in zip(source["names"], source["poses"])}
    return master, overlays


def indexed(image, palette):
    opaque = image.getchannel("A").point(lambda a: 0 if a else 255)
    frame = image.convert("RGB").quantize(palette=palette, dither=Image.Dither.NONE)
    frame.paste(63, mask=opaque)
    frame.info["transparency"] = 63
    return frame


def animate(master, overlays, timeline, theme):
    sprite = equip(master, theme)
    sequence = timeline[theme.state]
    frames, durations = [], []
    for name, duration in sequence:
        art = Image.alpha_composite(sprite, overlays[name].resize(sprite.size, PIXEL))
        frame = Image.new("RGBA", (EDGE, EDGE))
        frame.alpha_composite(art, ((EDGE - art.width) // 2, (EDGE - art.height) // 2))
        frames.append(frame)
        durations.append(duration)

    swatches = Image.new("RGB", (EDGE, EDGE * len(frames)))
    for index, frame in enumerate(frames):
        swatches.paste(frame, (0, EDGE * index), frame)
    palette = swatches.quantize(colors=60)
    indexed_frames = [indexed(frame, palette) for frame in frames]
    indexed_frames[0].save(OUTPUT / (theme.file + ".gif"), save_all=True, append_images=indexed_frames[1:],
                           duration=durations, loop=0, disposal=2, transparency=63, optimize=True)


def main():
    master, overlays = source_art()
    timeline = read_timeline()
    assert list(overlays) == list(dict.fromkeys(name for frames in timeline.values() for name, _ in frames))
    master.save(ARTWORK / "robot.png", optimize=True)
    edge = next(iter(overlays.values())).width
    atlas = Image.new("RGBA", (edge * len(overlays), edge))
    for index, overlay in enumerate(overlays.values()):
        atlas.paste(overlay, (index * edge, 0))
    atlas.save(ARTWORK / "faces.png", optimize=True)

    OUTPUT.mkdir(parents=True, exist_ok=True)
    for file, state in STATES.items():
        animate(master, overlays, timeline, Theme(file, state, ()))
    for name, colors in PROVIDERS.items():
        animate(master, overlays, timeline, Theme(name, "IDLE", colors, name))


if __name__ == "__main__":
    main()
