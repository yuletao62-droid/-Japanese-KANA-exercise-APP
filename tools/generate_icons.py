"""Generate launcher icons for the Android app from the desktop artwork."""

from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "artwork" / "app_icon.png"
RES = ROOT / "res"

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def main() -> None:
    image = Image.open(SOURCE).convert("RGBA")
    res = RES
    (res / "drawable-nodpi").mkdir(parents=True, exist_ok=True)
    image.save(res / "drawable-nodpi" / "app_icon.png")

    for folder, size in DENSITIES.items():
        target = res / folder
        target.mkdir(parents=True, exist_ok=True)
        image.resize((size, size), Image.Resampling.LANCZOS).save(
            target / "ic_launcher.png"
        )
        image.resize((size, size), Image.Resampling.LANCZOS).save(
            target / "ic_launcher_round.png"
        )

    # Adaptive icon foreground: place the rounded red icon inside the 108 dp
    # safe zone of a 108 dp canvas (Android scales the canvas to 432 px here).
    size = 432
    icon_size = 292
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    icon = image.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    offset = (size - icon_size) // 2
    canvas.alpha_composite(icon, (offset, offset))
    canvas.save(res / "drawable-nodpi" / "ic_launcher_foreground.png")

    print("Generated Android launcher icons.")


if __name__ == "__main__":
    main()
