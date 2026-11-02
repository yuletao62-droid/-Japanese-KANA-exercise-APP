"""Generate bundled Japanese pronunciation clips for every kana.

The resulting MP3 files live in res/raw and are committed with the project, so
building and running the APK never needs a TTS engine or network access.

Requires the optional edge-tts package:
    python -m pip install edge-tts
"""

from __future__ import annotations

import asyncio
import re
from pathlib import Path

import edge_tts

ROOT = Path(__file__).resolve().parents[1]
KANA_DATA = ROOT / "java" / "com" / "kana" / "typing" / "KanaData.java"
RAW = ROOT / "res" / "raw"
VOICE = "ja-JP-NanamiNeural"
RATE = "-10%"

# Nanami's isolated "い" ends with a short extra blip that sounds like "iki".
# A terminal punctuation mark gives a clean single vowel without changing the
# duration of the clip much.
TEXT_OVERRIDES = {
    "i": "い。",
}

ENTRY = re.compile(r'new Kana\("(.)", "(.)", "([^"]+)"')


def load_kana() -> list[tuple[str, str]]:
    text = KANA_DATA.read_text(encoding="utf-8")
    entries: list[tuple[str, str]] = []
    seen: set[str] = set()
    for match in ENTRY.finditer(text):
        hiragana, _katakana, primary = match.groups()
        if primary in seen:
            raise RuntimeError(f"duplicate primary romaji: {primary}")
        seen.add(primary)
        entries.append((hiragana, primary))
    if len(entries) != 71:
        raise RuntimeError(f"expected 71 kana, found {len(entries)}")
    return entries


async def generate() -> None:
    RAW.mkdir(parents=True, exist_ok=True)
    entries = load_kana()
    for index, (hiragana, primary) in enumerate(entries, start=1):
        target = RAW / f"kana_{primary}.mp3"
        if target.exists() and target.stat().st_size > 1000:
            print(f"[{index:02d}/{len(entries)}] skip {target.name}")
            continue
        print(f"[{index:02d}/{len(entries)}] {hiragana} -> {target.name}")
        text = TEXT_OVERRIDES.get(primary, hiragana)
        communicate = edge_tts.Communicate(text, VOICE, rate=RATE)
        await communicate.save(str(target))
        if not target.exists() or target.stat().st_size <= 1000:
            raise RuntimeError(f"failed to generate {target}")
    print(f"Generated/verified {len(entries)} clips in {RAW}")


if __name__ == "__main__":
    asyncio.run(generate())
