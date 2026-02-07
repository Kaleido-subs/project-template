#!/usr/bin/env -S uv run --script
#
# /// script
# requires-python = ">=3.13"
# dependencies = [
#   "ass",
#   "rich",
#   "srt",
# ]
# ///

from __future__ import annotations

import argparse
import re
from glob import glob
from pathlib import Path
from typing import Any, TypedDict

import ass
import srt
from rich.console import Console
from rich.highlighter import RegexHighlighter
from rich.theme import Theme
from rich.text import Text


# Constants. Adjust for your filename structure. Use * as wildcard; {episode} for episode number.
DIALOGUE_FILENAME_GLOB = "* - Dialogue.ass"
CC_FILENAME_GLOB = "* - Closed Captions *.*"


class SrtHighlighter(RegexHighlighter):
    base_style = "srt."
    highlights = [
        r"^(?P<index>\d+)$",
        r"(?P<timecode>\d{2}:\d{2}:\d{2},\d{3}\s*-->\s*\d{2}:\d{2}:\d{2},\d{3})",
        r"(?P<speaker>[（\(][^）\)]+[）\)])",
        r"(?P<newline>\\[nN])",
        r"(?P<musical>[♪～]+)",
    ]


# You can adjust the theme here.
theme = Theme(
    {
        "ass.comment": "dim white",
        "ass.newline": "dim bright_black",
        "ass.actor": "dim",
        "search_match": "bold on red",
        "inline_comment": "dim bright_black",
    }
)

#! ------ Do not touch anything below this comment!

if __name__ != "__main__":
    raise ImportError("This script is not meant to be imported!")

parser = argparse.ArgumentParser(
    description="Search for subtitle events in dialogue and CC files."
)
parser.add_argument(
    "search_term",
    help="The term to search for in the subtitle files.",
)
parser.add_argument(
    "-E",
    "--episode",
    type=int,
    help="Specify a specific episode number to search in.",
)
parser.add_argument(
    "-m",
    "--max-episode",
    type=int,
    help="Specify the maximum episode number to search up to.",
)
parser.add_argument(
    "-C",
    "--context",
    type=int,
    default=0,
    help="Number of context lines to show before and after a match.",
)
parser.add_argument(
    "-p",
    "--previous-seasons",
    action="store_true",
    help="Include episodes from previous seasons in the search.",
)
parser.add_argument(
    "-x",
    "--exact",
    action="store_true",
    help="Only match the exact term as a whole word (case-insensitive).",
)

args = parser.parse_args()


class AssHighlighter(RegexHighlighter):
    base_style = "ass."
    highlights = [
        r"(?P<comment>\{.+?\})",
        r"(?P<newline>\\[nN])",
        r"(?P<actor>^(?!\[\d).*?:)",
    ]


ass_highlighter = AssHighlighter()
srt_highlighter = SrtHighlighter()

highlighter_by_ext: dict[str, RegexHighlighter] = {
    ".ass": ass_highlighter,
    ".srt": srt_highlighter,
}


RE_FORMATTING_TAG = re.compile(r"^\{[^}]*\}")
RE_JP_ACTOR = re.compile(r"^[（(]([^\s）)]+)[）)]\s*")
RE_COLON_ACTOR = re.compile(r"^([^\W\d_][\w\u201c\u201d]{0,23}):\s*", re.UNICODE)


class SubtitleEvent:
    def __init__(
        self,
        start: float,
        end: float,
        text: str,
        *,
        original: Any = None,
        actor: str | None = None,
    ) -> None:
        self.start = start
        self.end = end
        self.text = text
        self.original = original
        self.actor = actor


class ParsedFile(TypedDict):
    events: list[SubtitleEvent]
    highlighter: RegexHighlighter | None
    type: str | None
    filename: Path | None


def is_episode(name: str) -> bool:
    return name.isdigit() or name.startswith(("SP", "OVA"))


def clean_text(text: str) -> str:
    return re.sub(r"(\\[Nn]|[\r\n]+)", " ", text).strip()


def _extract_actor_from_text(
    text: str, existing_actor: str | None
) -> tuple[str, str | None]:
    actor = existing_actor
    formatting_match = RE_FORMATTING_TAG.match(text)
    after_tag = (
        text[len(formatting_match.group(0)) :].lstrip() if formatting_match else text
    )

    m_jp = RE_JP_ACTOR.match(after_tag)

    if m_jp:
        actor = actor or m_jp.group(1).strip()
        after_tag = after_tag[m_jp.end() :].lstrip()
    else:
        m_colon = RE_COLON_ACTOR.match(after_tag)

        if m_colon:
            actor = actor or m_colon.group(1).strip()
            after_tag = after_tag[m_colon.end() :].lstrip()

    if after_tag != (
        text[len(formatting_match.group(0)) :].lstrip() if formatting_match else text
    ):
        out = (formatting_match.group(0) + after_tag) if formatting_match else after_tag

        return out, actor

    return text, actor


def _total_seconds(obj: Any) -> float:
    return getattr(obj, "total_seconds", lambda: float(str(obj)))()


def get_first_file(pattern: str) -> Path | None:
    files = sorted(glob(pattern))
    return Path(files[0]) if files else None


def episode_matches(episode: str) -> bool:
    ep_for_filter = episode.split("/")[-1] if "/" in episode else episode

    if not ep_for_filter.isdigit():
        if args.episode is not None:
            return False

        return True

    ep_num = int(ep_for_filter)

    if args.episode is not None and ep_num != args.episode:
        return False

    if args.max_episode is not None and ep_num > args.max_episode:
        return False

    return True


def find_matches(
    events: list[SubtitleEvent], term: str
) -> list[tuple[int, SubtitleEvent]]:
    if getattr(args, "exact", False):
        pattern = re.compile(rf"\b{re.escape(term)}\b", re.IGNORECASE)
        return [(i, e) for i, e in enumerate(events) if pattern.search(e.text)]
    else:
        low = term.lower()
        return [(i, e) for i, e in enumerate(events) if low in e.text.lower()]


def format_ts(seconds: float) -> str:
    t = int(seconds)
    h, m, s = t // 3600, (t % 3600) // 60, t % 60
    return f"[{h}:{m:02d}:{s:02d}]"


def ass_events(path: Path) -> list[SubtitleEvent]:
    try:
        with open(path, encoding="utf_8_sig") as f:
            doc = ass.parse(f)
    except Exception:
        return []

    events: list[SubtitleEvent] = []

    for e in doc.events:
        actor_field = getattr(e, "actor", None) or getattr(e, "name", None)
        actor = (actor_field or "").strip() or None
        text = clean_text(e.text)
        text, actor = _extract_actor_from_text(text, actor)

        events.append(
            SubtitleEvent(
                _total_seconds(e.start),
                _total_seconds(e.end),
                text,
                original=e,
                actor=actor,
            )
        )

    return events


def srt_events(path: Path) -> list[SubtitleEvent]:
    try:
        with open(path, encoding="utf_8_sig") as f:
            content = f.read()
    except Exception:
        return []

    events = []

    for s in srt.parse(content):
        text = clean_text(s.content)
        text, actor = _extract_actor_from_text(text, None)

        events.append(
            SubtitleEvent(
                s.start.total_seconds(),
                s.end.total_seconds(),
                text,
                original=s,
                actor=actor,
            )
        )

    return events


def _find_brace_spans(text: str) -> list[tuple[int, int]]:
    spans = []
    i = 0

    while i < len(text):
        if text[i] != "{":
            i += 1
            continue

        start = i
        depth = 1
        i += 1

        while i < len(text) and depth > 0:
            if text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1

                if depth == 0:
                    spans.append((start, i + 1))
                    break

            i += 1

        i += 1

    return spans


def _append_segment_with_search(
    result: Text, segment: str, search_pattern: re.Pattern[str] | None
) -> None:
    if not search_pattern:
        result.append(segment)
        return

    seg_last = 0

    for m in search_pattern.finditer(segment):
        result.append(segment[seg_last : m.start()])
        result.append(segment[m.start() : m.end()], style="search_match")
        seg_last = m.end()

    result.append(segment[seg_last:])


def _text_with_highlight(text: str, search_term: str | None) -> Text:
    result = Text()
    if not search_term:
        search_pattern = None
    else:
        if getattr(args, "exact", False):
            search_pattern = re.compile(rf"\b{re.escape(search_term)}\b", re.IGNORECASE)
        else:
            search_pattern = re.compile(re.escape(search_term), re.IGNORECASE)
    last = 0

    for start, end in _find_brace_spans(text):
        _append_segment_with_search(result, text[last:start], search_pattern)
        result.append(text[start:end], style="inline_comment")
        last = end

    _append_segment_with_search(result, text[last:], search_pattern)

    return result


def print_event_line(
    console: Console,
    event: SubtitleEvent,
    *,
    tag: str | None = None,
    indent: str = "",
    search_term: str | None = None,
) -> None:
    tag_part = Text(f"{tag}: ", style="green") if tag else Text()
    prefix = Text(indent) + tag_part

    rest = (
        _text_with_highlight(event.text, search_term)
        if search_term
        else Text(event.text)
    )

    if event.actor:
        actor_part = Text(f"({event.actor}) ", style="ass.actor")
        line = prefix + actor_part + rest
    else:
        line = prefix + rest

    console.print(line)


def search_episode(
    episode: str,
    a: ParsedFile,
    b: ParsedFile,
    tags: tuple[str, str],
    context: int,
) -> None:
    a_events, b_events = a["events"], b["events"]

    if not a_events or not episode_matches(episode):
        return

    matches = find_matches(a_events, args.search_term)

    if not matches:
        return

    console = Console(highlighter=a["highlighter"], theme=theme)

    console.print()
    console.print(Text(f"Episode {episode}:", style="bold white"))
    console.print()

    for match_idx, match in matches:
        console.print(format_ts(match.start), style="dim cyan")

        mn = max(0, match_idx - context) if context > 0 else match_idx
        mx = min(len(a_events) - 1, match_idx + context) if context > 0 else match_idx
        tl_indices = (
            list(range(mn, match_idx))
            + [match_idx]
            + list(range(match_idx + 1, mx + 1))
        )

        for i in tl_indices:
            print_event_line(
                console, a_events[i], tag=tags[0], search_term=args.search_term
            )

        cc_lines: list[SubtitleEvent] = []
        seen_cc: set[int] = set()

        for i in tl_indices:
            ev = a_events[i]

            for be in b_events:
                if ev.start <= be.end and be.start <= ev.end:
                    be_id = id(be)

                    if be_id not in seen_cc:
                        seen_cc.add(be_id)
                        cc_lines.append(be)

        for ev in cc_lines:
            print_event_line(console, ev, tag=tags[1], search_term=args.search_term)


def discover_episodes() -> list[str]:
    return sorted(
        p.name for p in Path.cwd().iterdir() if p.is_dir() and is_episode(p.name)
    )


def discover_previous_seasons() -> list[tuple[Path, str]]:
    parent = Path.cwd().parent
    current_name = Path.cwd().name
    pairs: list[tuple[Path, str]] = []

    for path in sorted(parent.iterdir()):
        if not path.is_dir() or path.name == current_name:
            continue

        episode_dirs = sorted(
            p.name for p in path.iterdir() if p.is_dir() and is_episode(p.name)
        )

        if episode_dirs:
            for ep in episode_dirs:
                pairs.append((path, ep))
        else:
            # Movie/special dir: files live directly in the dir
            if any(path.glob("*.ass")) or any(path.glob("*.srt")):
                pairs.append((path, ""))

    return pairs


def collect_files(
    episode: str, base: Path | None = None
) -> tuple[Path | None, Path | None]:
    if base is None:
        prefix = Path(episode)
    else:
        prefix = base / episode if episode else base

    cc_glob = (
        CC_FILENAME_GLOB.format(episode=episode)
        if "{episode}" in CC_FILENAME_GLOB and episode
        else ("* - S*E* - Closed Captions *.*" if not episode else CC_FILENAME_GLOB)
    )

    dl_glob = (
        DIALOGUE_FILENAME_GLOB.format(episode=episode)
        if "{episode}" in DIALOGUE_FILENAME_GLOB and episode
        else ("* - * - Dialogue.ass" if not episode else DIALOGUE_FILENAME_GLOB)
    )

    cc = get_first_file(str(prefix / cc_glob)) or get_first_file(str(prefix / "*.srt"))
    dl = get_first_file(str(prefix / dl_glob)) or get_first_file(str(prefix / "*.ass"))

    return cc, dl


def parse_file(file: Path | None) -> ParsedFile:
    empty: ParsedFile = {
        "events": [],
        "highlighter": None,
        "type": None,
        "filename": file,
    }

    if not file:
        return empty

    ext = file.suffix.lower()

    if ext == ".ass":
        return {
            "events": ass_events(file),
            "highlighter": ass_highlighter,
            "type": "ass",
            "filename": file,
        }

    if ext == ".srt":
        return {
            "events": srt_events(file),
            "highlighter": srt_highlighter,
            "type": "srt",
            "filename": file,
        }

    return empty


def main() -> None:
    context = getattr(args, "context", 0)

    use_season_label = getattr(args, "previous_seasons", False)

    for episode in discover_episodes():
        cc, dl = collect_files(episode)

        if not cc or not dl:
            continue

        label = f"{Path.cwd().name}/{episode}" if use_season_label else episode

        cc_data = parse_file(cc)
        dl_data = parse_file(dl)

        search_episode(label, dl_data, cc_data, ("TL", "CC"), context)
        search_episode(label, cc_data, dl_data, ("CC", "TL"), context)

    if getattr(args, "previous_seasons", False):
        for base, episode in discover_previous_seasons():
            cc, dl = collect_files(episode, base=base)

            if not cc or not dl:
                continue

            label = f"{base.name}/{episode}" if episode else base.name

            cc_data = parse_file(cc)
            dl_data = parse_file(dl)

            search_episode(label, dl_data, cc_data, ("TL", "CC"), context)
            search_episode(label, cc_data, dl_data, ("CC", "TL"), context)


if __name__ == "__main__":
    main()
