#!/usr/bin/env bash
set -euo pipefail

GRADLE_DIR="$(cd "$(dirname "$0")" && pwd)/.gradle"

# Remove stale Gradle lock files (those whose owner process is no longer alive)
if [[ -d "$GRADLE_DIR" ]]; then
  while IFS= read -r lock; do
    pid=$(grep -oP '(?<=Owner PID: )\d+' "$lock" 2>/dev/null || true)
    if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
      echo "Removing stale lock (dead PID $pid): $lock"
      rm -f "$lock"
    elif [[ -z "$pid" ]]; then
      echo "Removing lock with no owner PID: $lock"
      rm -f "$lock"
    fi
  done < <(find "$GRADLE_DIR" -name "*.lock" 2>/dev/null)
fi

exec ./gradlew test "$@"
