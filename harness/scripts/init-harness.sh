#!/usr/bin/env bash
# Install entry points and discover the consumer; AI owns product/project creation.
set -euo pipefail

HARNESS_ROOT="$(cd -P "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROJECT_ROOT="$PWD"
PYTHON_BIN="${HARNESS_PYTHON:-python3}"

usage() {
  echo "Usage: init-harness.sh [--project-root <directory>] [--verify]"
  echo "Edit setup_prompt.md, then give it to your AI agent to complete onboarding."
}

ACTION=prepare
while [ "$#" -gt 0 ]; do
  case "$1" in
    --project-root)
      [ "$#" -ge 2 ] || { usage; exit 2; }
      PROJECT_ROOT="$2"
      shift 2
      ;;
    --verify) ACTION=verify; shift ;;
    --help|-h) usage; exit 0 ;;
    *) usage; exit 2 ;;
  esac
done

exec "$PYTHON_BIN" "$HARNESS_ROOT/harness/scripts/project_onboarding.py" \
  --project-root "$PROJECT_ROOT" --harness-root "$HARNESS_ROOT" "$ACTION"
