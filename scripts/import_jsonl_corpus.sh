#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <rag_tag> <jsonl_file> [api_base_url]"
  exit 1
fi

RAG_TAG="$1"
JSONL_FILE="$2"
API_BASE_URL="${3:-http://localhost}"

curl -X POST \
  -F "ragTag=${RAG_TAG}" \
  -F "file=@${JSONL_FILE}" \
  "${API_BASE_URL}/api/v1/rag/corpus/import_jsonl"
