#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="${ROOT_DIR}/.runtime/cloudflared"
BIN_PATH="${RUNTIME_DIR}/cloudflared"
LOG_PATH="${RUNTIME_DIR}/tunnel.log"
PID_PATH="${RUNTIME_DIR}/tunnel.pid"
URL_PATH="${RUNTIME_DIR}/public_url.txt"
ARCHIVE_PATH="${RUNTIME_DIR}/cloudflared.tgz"
LOCAL_URL="${1:-http://localhost:80}"
PUBLIC_CHECK_PATH="${PUBLIC_CHECK_PATH:-/api/v1/rag/query_rag_tag_list}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-3}"

mkdir -p "${RUNTIME_DIR}"

download_cloudflared() {
  local arch
  arch="$(uname -m)"

  case "${arch}" in
    arm64)
      curl -L -o "${ARCHIVE_PATH}" \
        "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-darwin-arm64.tgz"
      ;;
    x86_64)
      curl -L -o "${ARCHIVE_PATH}" \
        "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-darwin-amd64.tgz"
      ;;
    *)
      echo "Unsupported architecture: ${arch}" >&2
      exit 1
      ;;
  esac

  tar -xzf "${ARCHIVE_PATH}" -C "${RUNTIME_DIR}"
  chmod +x "${BIN_PATH}"
}

extract_url() {
  if [[ -f "${LOG_PATH}" ]]; then
    grep -Eo 'https://[-a-z0-9]+\.trycloudflare\.com' "${LOG_PATH}" | tail -n 1 || true
  fi
}

verify_url() {
  local url="$1"
  curl -sS --max-time 10 "${url}${PUBLIC_CHECK_PATH}" >/dev/null 2>&1
}

stop_pid_if_running() {
  local pid="$1"
  if [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1; then
    kill "${pid}" >/dev/null 2>&1 || true
  fi
}

if [[ ! -x "${BIN_PATH}" ]]; then
  echo "Downloading cloudflared..."
  download_cloudflared
fi

if [[ -f "${PID_PATH}" ]]; then
  existing_pid="$(cat "${PID_PATH}")"
  if kill -0 "${existing_pid}" >/dev/null 2>&1; then
    existing_url="$(extract_url)"
    echo "Public tunnel is already running."
    echo "PID: ${existing_pid}"
    if [[ -n "${existing_url}" ]]; then
      echo "${existing_url}" > "${URL_PATH}"
      echo "URL: ${existing_url}"
    fi
    exit 0
  fi
fi

rm -f "${LOG_PATH}" "${PID_PATH}" "${URL_PATH}"

echo "Starting public tunnel for ${LOCAL_URL}..."

for attempt in $(seq 1 "${MAX_ATTEMPTS}"); do
  rm -f "${LOG_PATH}" "${PID_PATH}" "${URL_PATH}"
  nohup "${BIN_PATH}" tunnel --url "${LOCAL_URL}" --protocol http2 --no-autoupdate >"${LOG_PATH}" 2>&1 &
  current_pid="$!"
  echo "${current_pid}" > "${PID_PATH}"

  public_url=""
  for _ in {1..20}; do
    sleep 1
    public_url="$(extract_url)"
    if [[ -n "${public_url}" ]]; then
      break
    fi
  done

  if [[ -z "${public_url}" ]]; then
    stop_pid_if_running "${current_pid}"
    continue
  fi

  echo "${public_url}" > "${URL_PATH}"

  for _ in {1..20}; do
    if verify_url "${public_url}"; then
      echo "Public URL: ${public_url}"
      echo "Reachability check: ok"
      echo "Log file: ${LOG_PATH}"
      echo "PID file: ${PID_PATH}"
      exit 0
    fi
    sleep 1
  done

  echo "Attempt ${attempt}/${MAX_ATTEMPTS} did not verify yet: ${public_url}" >&2
  if [[ "${attempt}" -lt "${MAX_ATTEMPTS}" ]]; then
    stop_pid_if_running "${current_pid}"
    sleep 1
  fi
done

if [[ -s "${URL_PATH}" ]]; then
  echo "Public URL: $(cat "${URL_PATH}")"
fi
echo "Reachability check: pending DNS/edge propagation; if this URL still does not open, rerun the script once." >&2
echo "Log file: ${LOG_PATH}" >&2
echo "PID file: ${PID_PATH}" >&2
exit 0
