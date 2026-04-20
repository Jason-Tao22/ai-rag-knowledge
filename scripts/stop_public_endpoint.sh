#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_PATH="${ROOT_DIR}/.runtime/cloudflared/tunnel.pid"
URL_PATH="${ROOT_DIR}/.runtime/cloudflared/public_url.txt"

if [[ ! -f "${PID_PATH}" ]]; then
  echo "No running public tunnel was found."
  exit 0
fi

pid="$(cat "${PID_PATH}")"

if kill -0 "${pid}" >/dev/null 2>&1; then
  kill "${pid}"
  echo "Stopped public tunnel process: ${pid}"
else
  echo "Tunnel process was not running."
fi

rm -f "${PID_PATH}"
rm -f "${URL_PATH}"
