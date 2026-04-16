#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${1:-http://localhost}"
MODEL="${MODEL:-qwen2.5:0.5b}"

echo "Running presentation smoke test against: ${BASE_URL}"
echo "Using model: ${MODEL}"

query_tags() {
  curl -fsS "${BASE_URL}/api/v1/rag/query_rag_tag_list"
}

retrieve_appscan() {
  curl -fsS "${BASE_URL}/api/v1/rag/retrieve?ragTag=techqa&message=AppScan%20Source%20not%20a%20supported%20operating%20system&topK=2"
}

query_mq() {
  curl -fsS "${BASE_URL}/api/v1/rag/query?ragTag=techqa&message=AMQ9208%20AMQ6048%20AMQ9492%20data%20conversion&model=${MODEL}&topK=1"
}

assert_json() {
  local mode="$1"
  local payload="$2"

  PAYLOAD="$payload" python3 - "$mode" <<'PY'
import json
import os
import sys

payload = json.loads(os.environ["PAYLOAD"])
mode = sys.argv[1]

if payload.get("code") != "0000":
    raise SystemExit(f"{mode} failed: unexpected code {payload.get('code')}")

data = payload.get("data") or {}

if mode == "tags":
    tags = data
    if "techqa" not in tags:
        raise SystemExit("tags failed: techqa tag missing")
    print("tags ok:", tags)

elif mode == "retrieve":
    citations = data.get("citations") or []
    if not citations:
        raise SystemExit("retrieve failed: no citations")
    first = citations[0]
    if first.get("documentId") != "swg21512700":
        raise SystemExit(f"retrieve warning: expected swg21512700, got {first.get('documentId')}")
    print("retrieve ok:", first.get("documentId"), "| latency:", data.get("latencyMs"), "ms")

elif mode == "query":
    citations = data.get("citations") or []
    if not citations:
        raise SystemExit("query failed: no citations")
    first = citations[0]
    answer = (data.get("answer") or "").strip()
    if not answer:
        raise SystemExit("query failed: empty answer")
    if first.get("documentId") != "swg1SE46234":
        raise SystemExit(f"query warning: expected swg1SE46234, got {first.get('documentId')}")
    print("query ok:", first.get("documentId"), "| latency:", data.get("latencyMs"), "ms")

else:
    raise SystemExit(f"unknown mode {mode}")
PY
}

echo
echo "[1/3] Checking knowledge-base tags"
assert_json tags "$(query_tags)"

echo
echo "[2/3] Checking retrieval-only demo path"
assert_json retrieve "$(retrieve_appscan)"

echo
echo "[3/3] Checking grounded-answer demo path"
assert_json query "$(query_mq)"

echo
echo "Smoke test passed."
