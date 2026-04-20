#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Split a JSONL file into multiple chunks bounded by bytes.")
    parser.add_argument("input", type=Path, help="Path to the source JSONL file.")
    parser.add_argument("output_dir", type=Path, help="Directory where chunk files will be written.")
    parser.add_argument("--max-bytes", type=int, default=800_000, help="Maximum bytes per chunk file.")
    parser.add_argument("--prefix", default="batch_", help="Prefix for chunk filenames.")
    return parser.parse_args()


def write_chunks(input_path: Path, output_dir: Path, max_bytes: int, prefix: str) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    chunk_index = 0
    chunk_size = 0
    handle = None

    try:
        with input_path.open("rb") as source:
            for line in source:
                if handle is None or (chunk_size > 0 and chunk_size + len(line) > max_bytes):
                    if handle is not None:
                        handle.close()
                    chunk_path = output_dir / f"{prefix}{chunk_index:04d}.jsonl"
                    handle = chunk_path.open("wb")
                    chunk_index += 1
                    chunk_size = 0

                handle.write(line)
                chunk_size += len(line)
    finally:
        if handle is not None:
            handle.close()

    return chunk_index


def main() -> None:
    args = parse_args()
    if not args.input.exists():
        raise SystemExit(f"Input file not found: {args.input}")

    chunk_count = write_chunks(args.input, args.output_dir, args.max_bytes, args.prefix)
    print(f"Created {chunk_count} chunk files in {args.output_dir}")


if __name__ == "__main__":
    main()
