#!/usr/bin/env python3
"""Render Coupi’s shared face atlas and README GIFs. Requires Pillow."""

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
FROWN = ("..XXX..", ".XXXXX.", "XX...XX")
CLOSED = ("XX...XX", ".XXXXX.")
HEART = (".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X...")
QUESTION = (".XXX.", "XX.XX", "...XX", "..XX.", "..X..", ".....", "..X..")
WARNING = ("...X...", "..XXX..", "..X!X..", ".XX!XX.", ".XXXXX.", "XXX!XXX", "XXXXXXX")
CROSSED = ("XX...XX", ".XX.XX.", "..XXX..", ".XX.XX.", "XX...XX")
FOCUS = ("XX.......", "XXXXX....", "XXXXXXXX.", ".XXXXXXX.", ".XX++XXX.", ".XXXXXXX.", "..XXXXX..")


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
        edge = round(sprite.width * 0.117)
        with Image.open(ARTWORK / "badges" / (theme.mark + ".png")) as badge:
            logo = badge.convert("RGBA").resize((edge, edge), Image.Resampling.LANCZOS)
        x = (sprite.width - edge) // 2
        y = round(sprite.height * 0.827 - edge / 2)
        sprite.alpha_composite(logo, (x, y))
    return sprite


def face_overlays(master, timeline):
    face = master.getpixel((55, 42))
    eye_patches = []
    for box in EYES:
        eye = master.crop(box)
        mask = []
        for y in range(eye.height):
            for x in range(eye.width):
                r, g, b, _ = eye.getpixel((x, y))
                lightest, darkest = max(r, g, b), min(r, g, b)
                glint = darkest > 235 and lightest - darkest <= 5
                mask.append(255 if lightest < 170 or glint else 0)
        alpha = Image.new("L", eye.size)
        alpha.putdata(mask)
        eye.putalpha(alpha)
        eye_patches.append(eye)

    def is_cream(pixel):
        return pixel[0] > 220 and pixel[0] > pixel[2] + 5

    cream_rows = [
        [x for x in range(17, 95) if is_cream(master.getpixel((x, y)))]
        for y in range(26, 74)
    ]

    def pose(name):
        overlay = Image.new("RGBA", master.size)
        d = ImageDraw.Draw(overlay)

        def draw_eyes(dx=0, dy=0):
            for (x, y, _, _), eye in zip(EYES, eye_patches):
                overlay.alpha_composite(eye, (x + dx, y + dy))

        def cover_eyes():
            for left, top, right, bottom in EYES:
                d.rectangle((left, top, right - 1, bottom - 1), fill=face)

        def cover_mouth():
            d.rectangle((48, 51, 64, 64), fill=face)

        def mark(center, rows, color=INK, cell=2):
            glyph(d, center, rows, {"X": color}, cell)

        def mouth_o(tall=False):
            rows = (".XXX.", "XX.XX", "XX.XX", ".XXX.") if tall else (".XX.", "X..X", ".XX.")
            mark((56, 57), rows)

        def dots(number, color):
            for i in range(number):
                x = 49 + 6 * i
                d.rectangle((x, 64, x + 2, 66), fill=color)

        if name == "neutral":
            return overlay
        if name in ("glance_left", "glance_right", "blink"):
            cover_eyes()
            if name == "blink":
                for x in EYE_X:
                    mark((x, 48), CLOSED)
            else:
                draw_eyes(-4 if name == "glance_left" else 4)
        elif name in ("smile", "mouth_o"):
            cover_mouth()
            if name == "smile":
                mark((56, 56), SMILE)
            else:
                mouth_o()
        elif name.startswith("timer_"):
            dots(("timer_one", "timer_two", "timer_three").index(name) + 1, "#65529a")
        elif name.startswith("think_"):
            cover_eyes()
            draw_eyes(3, -4)
            d.rectangle((71, 29, 78, 30), fill=INK)
            cover_mouth()
            mouth_o()
            dots(("think_one", "think_two", "think_three").index(name) + 1, "#485482")
        elif name in ("focused", "processing"):
            cover_eyes()
            cover_mouth()
            highlight = "#529ac9" if name == "focused" else "#83dae7"
            for x, reverse in ((38, False), (74, True)):
                rows = tuple(row[::-1] for row in FOCUS) if reverse else FOCUS
                glyph(d, (x, 46), rows, {"X": INK, "+": highlight})
            mark((56, 56), ("XX...XX", ".XXXXX." if name == "focused" else "XXXXXXX"))
        elif name in ("hearts", "hearts_big"):
            cover_eyes()
            cover_mouth()
            for x in EYE_X:
                mark((x, 46), HEART, "#118445" if name == "hearts" else "#169b54",
                     2 if name == "hearts" else 3)
            mark((56, 56), SMILE)
        elif name.startswith("question"):
            cover_eyes()
            cover_mouth()
            bright = name == "question_bright"
            mark((56, 48), QUESTION, "#7346b7" if bright else "#504180", 4)
            for x in EYE_X:
                mark((x, 50), ("XX", "XX"))
        elif name.startswith("warning"):
            red = "#f37e7c" if name == "warning" else "#e96a70"
            for y, cream in enumerate(cream_rows, start=26):
                if cream:
                    d.line((cream[0], y, cream[-1], y), fill=red)
            for x in EYE_X:
                mark((x, 47), CROSSED, "#641b34")
            glyph(d, (56, 36), WARNING, {"X": "#ffd775", "!": "#641b34"})
            mark((56, 60), FROWN, "#641b34")
        elif name.startswith("sleeping"):
            cover_eyes()
            cover_mouth()
            for x in EYE_X:
                mark((x, 47), CLOSED)
            mouth_o(tall=name == "sleeping_o")
        else:
            raise ValueError(f"Unknown face expression: {name}")
        return overlay

    return {name: pose(name) for name in dict.fromkeys(name for frames in timeline.values() for name, _ in frames)}


def indexed(image, palette):
    opaque = image.getchannel("A").point(lambda a: 0 if a else 255)
    frame = image.convert("RGB").quantize(palette=palette, dither=Image.Dither.NONE)
    frame.paste(63, mask=opaque)
    frame.info["transparency"] = 63
    return frame


def animate(master, overlays, timeline, theme):
    sprite = equip(master, theme)
    sequence = timeline[theme.state]
    if theme.state == "COMPLETED":
        sequence += timeline["IDLE"]
    frames, durations = [], []
    for name, duration in sequence:
        art = Image.alpha_composite(sprite, overlays[name])
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
    with Image.open(ARTWORK / "robot.png") as image:
        height = round(image.height * SPRITE_WIDTH / image.width)
        master = image.convert("RGBA").resize((SPRITE_WIDTH, height), PIXEL)
    timeline = read_timeline()
    overlays = face_overlays(master, timeline)
    atlas = Image.new("RGBA", (master.width * len(overlays), master.height))
    for index, overlay in enumerate(overlays.values()):
        atlas.paste(overlay, (index * master.width, 0))
    atlas.save(ARTWORK / "faces.png", optimize=True)

    OUTPUT.mkdir(parents=True, exist_ok=True)
    for file, state in STATES.items():
        animate(master, overlays, timeline, Theme(file, state, ()))
    for name, colors in PROVIDERS.items():
        animate(master, overlays, timeline, Theme(name, "IDLE", colors, name))


if __name__ == "__main__":
    main()
