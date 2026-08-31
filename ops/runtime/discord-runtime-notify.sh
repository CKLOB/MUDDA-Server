#!/usr/bin/env bash
set -euo pipefail

status="${1:-}"
[[ "$status" == incident || "$status" == recovery ]] || { echo "invalid runtime notification status" >&2; exit 2; }
webhook_url="${DISCORD_RUNTIME_WEBHOOK_URL:-}"
[[ -n "$webhook_url" ]] || { echo "runtime Discord webhook is not configured" >&2; exit 1; }
[[ "$webhook_url" =~ ^https://discord(app)?\.com/api/webhooks/ ]] || { echo "runtime Discord webhook has an invalid host" >&2; exit 1; }
command -v jq >/dev/null || { echo "jq is required for runtime notifications" >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required for runtime notifications" >&2; exit 1; }

notify_dir="$(mktemp -d)"
payload_file="$notify_dir/payload.json"
config_file="$notify_dir/curl.conf"
error_file="$notify_dir/curl.err"
trap 'rm -rf "$notify_dir"' EXIT

logs="$(printf '%s' "${RUNTIME_LOG_EXCERPT:-}" | MAX_CHARS=2500 "$(dirname "$0")/redact-log.sh")"
summary="$(printf '%s' "${RUNTIME_SUMMARY:-unknown}" | MAX_CHARS=1000 "$(dirname "$0")/redact-log.sh")"
if [[ "$status" == recovery ]]; then
  title="MUDDA 운영 장애 복구"
  color=5763719
  state_label="복구"
else
  title="MUDDA 운영 장애 감지"
  color=15548997
  state_label="장애"
fi

jq -n \
  --arg title "$title" \
  --arg state "$state_label" \
  --arg time "${RUNTIME_TIME:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}" \
  --arg server "${RUNTIME_SERVER:-GSMSV VM}" \
  --arg environment "${RUNTIME_ENVIRONMENT:-production}" \
  --arg service "${RUNTIME_SERVICE:-app, postgres, redis}" \
  --arg container "${RUNTIME_CONTAINER_STATUS:-unknown}" \
  --arg health "${RUNTIME_HEALTH_STATUS:-unknown}" \
  --arg restarts "${RUNTIME_RESTART_COUNTS:-unknown}" \
  --arg summary "$summary" \
  --arg logs "$logs" \
  --argjson color "$color" \
  '{embeds: [{title: $title, color: $color, fields: [
    {name: "상태", value: $state, inline: true},
    {name: "발생 시각", value: $time, inline: true},
    {name: "서버/환경", value: ($server + " / " + $environment), inline: false},
    {name: "영향 서비스", value: $service, inline: false},
    {name: "컨테이너 상태", value: $container, inline: false},
    {name: "Health / Restart", value: ($health + " / " + $restarts), inline: false},
    {name: "요약", value: $summary, inline: false},
    {name: "최근 로그", value: (if $logs == "" then "(없음)" else ("```\\n" + $logs + "\\n```") end), inline: false}
  ]} ]}' > "$payload_file"

{
  printf 'url = "'
  printf '%s' "$webhook_url"
  printf '"\nrequest = "POST"\nheader = "Content-Type: application/json"\ndata-binary = @%s\n' "$payload_file"
} > "$config_file"

if ! curl --silent --show-error --fail --max-time 15 --config "$config_file" >/dev/null 2>"$error_file"; then
  echo "runtime Discord notification failed" >&2
  exit 1
fi
