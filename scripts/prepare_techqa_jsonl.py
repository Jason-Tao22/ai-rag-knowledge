#!/usr/bin/env python3
"""Convert the official TechQA technote corpus JSON into a JSONL file for ingestion.

Expected official corpus layout from IBM TechQA:
- a JSON object keyed by document id
- each document typically contains `_id`, `title`, and `text`

This script is intentionally defensive so it can handle small schema differences.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def normalize_record(doc_id: str, record: dict) -> dict | None:
    metadata = record.get("metadata") or {}
    text = (record.get("text") or record.get("content") or record.get("body") or "").strip()
    if not text:
        return None

    title = (record.get("title") or doc_id).strip()
    source_url = (
        record.get("url")
        or record.get("source_url")
        or record.get("document_url")
        or metadata.get("canonicalUrl")
        or ""
    )
    source_name = metadata.get("productName") or "TechQA Technote"

    return {
        "id": str(record.get("_id") or record.get("id") or doc_id),
        "title": title,
        "text": text,
        "url": source_url,
        "source": source_name,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="Path to training_dev_technotes.json")
    parser.add_argument("--output", required=True, help="Path to output JSONL file")
    parser.add_argument("--limit", type=int, default=None, help="Optional max number of documents")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with input_path.open("r", encoding="utf-8") as infile:
        corpus = json.load(infile)

    written = 0
    with output_path.open("w", encoding="utf-8") as outfile:
        for doc_id, record in corpus.items():
            normalized = normalize_record(str(doc_id), record)
            if normalized is None:
                continue
            outfile.write(json.dumps(normalized, ensure_ascii=False) + "\n")
            written += 1
            if args.limit is not None and written >= args.limit:
                break

    print(f"Wrote {written} records to {output_path}")


if __name__ == "__main__":
    main()
