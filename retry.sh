#!/usr/bin/env bash
# Runs a command up to 3 times (used by CI for network hiccups).
for i in 1 2 3; do
  "$@" && exit 0
  echo "Attempt $i failed; retrying in $((i * 20)) s..."
  sleep $((i * 20))
done
exit 1
