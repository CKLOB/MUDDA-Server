#!/usr/bin/env bash
set -euo pipefail

max_chars="${MAX_CHARS:-2500}"
[[ "$max_chars" =~ ^[1-9][0-9]*$ ]] || max_chars=2500

sed -E \
  -e 's#https://discord(app)?\.com/api/webhooks/[[:alnum:]_/-]+#[REDACTED_URL]#gI' \
  -e 's/(Bearer[[:space:]]+)[^[:space:]]+/\1[REDACTED]/gI' \
  -e 's/((password|passwd|secret|token|jwt|authorization|cookie|api[-_]?key|client[-_]?secret|access[-_]?key|secret[-_]?key)[[:space:]]*[:=][[:space:]]*)[^,[:space:]]+/\1[REDACTED]/gI' \
  -e 's/AKIA[0-9A-Z]{16}/[REDACTED_AWS_KEY]/g' \
  -e 's/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/[REDACTED_JWT]/g' \
  -e 's/[[:alnum:]._%+-]+@[[:alnum:].-]+\.[A-Za-z]{2,}/[REDACTED_EMAIL]/g' \
  -e 's/::/ : :/g' |
  awk -v limit="$max_chars" 'BEGIN { ORS="" } { remaining=limit-length(out); if (remaining > 0) out=out substr($0, 1, remaining) "\n" } END { printf "%s", substr(out, 1, limit) }'
