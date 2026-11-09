#!/usr/bin/env bash
# Runs a Gradle task and retries it when a dependency download fails for a
# transient reason. GitHub's release proxy and Maven Central occasionally answer
# 502/504 (or just drop the connection) for single jars, which used to fail a
# whole run before anything was compiled or tested.
#
# Usage: gradle-retry.sh <log-file> <gradle arguments...>
set -uo pipefail

LOG="$1"
shift
MAX_ATTEMPTS="${GRADLE_ATTEMPTS:-3}"

attempt=1
while :; do
  gradle "$@" --console=plain --stacktrace 2>&1 | tee "$LOG"
  status=${PIPESTATUS[0]}
  if [ "$status" -eq 0 ]; then
    exit 0
  fi
  if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
    exit "$status"
  fi
  if ! grep -qE 'Could not download|Could not resolve|Could not GET|Could not HEAD|status code 5[0-9][0-9]|Gateway Time-?out|Connection reset|Read timed out|Connection timed out|Remote host terminated' "$LOG"; then
    exit "$status"
  fi
  wait=$((attempt * 20))
  echo "::warning::Transient dependency download failure (attempt ${attempt}/${MAX_ATTEMPTS}); retrying in ${wait}s."
  sleep "$wait"
  attempt=$((attempt + 1))
done
