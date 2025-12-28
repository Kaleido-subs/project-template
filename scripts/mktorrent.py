#!/usr/bin/env python3

import argparse
from pathlib import Path
import sys

try:
    import dottorrent  # type: ignore
except ImportError:
    print(
        "dottorrent not found, please install it with `pip install dottorrent` "
        "or get it here: <https://github.com/kz26/dottorrent>!"
    )
    sys.exit(1)


TRACKERS = [
    "http://nyaa.tracker.wf:7777/announce",
    "https://tracker.nekobt.to/api/tracker/public/announce",
    "udp://open.stealth.si:80/announce",
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://exodus.desync.com:6969/announce",
    "udp://tracker.torrent.eu.org:451/announce",
]


def main(args: argparse.Namespace) -> None:
    if not (file_path := Path(args.path)).exists():
        print(f"File {file_path} does not exist!")
        sys.exit(2)

    torr_dir = Path().cwd() / "torrent"

    if (torrent_path := (torr_dir / file_path.name).with_suffix(".torrent")).exists():
        print(f"Torrent {torrent_path} already exists!")
        sys.exit(3)

    t = dottorrent.Torrent(file_path, trackers=TRACKERS)
    t.generate()

    with open(torrent_path, "wb") as fo:
        t.save(fo)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("path")
    args = parser.parse_args()

    main(args)
