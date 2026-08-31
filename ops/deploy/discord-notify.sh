#!/usr/bin/env bash
set -euo pipefail

event="${1:-}"
case "$event" in
  started|succeeded|failed) ;;
  *) echo "usage: discord-notify.sh <started|succeeded|failed>" >&2; exit 2 ;;
esac

webhook_url="${DISCORD_DEPLOY_WEBHOOK_URL:-}"
[[ -n "$webhook_url" ]] || { echo "Discord deployment webhook is not configured" >&2; exit 1; }
[[ "$webhook_url" =~ ^https://discord(app)?\.com/api/webhooks/ ]] || {
  echo "Discord deployment webhook has an invalid host" >&2
  exit 1
}
command -v jq >/dev/null || { echo "jq is required for Discord deployment notifications" >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required for Discord deployment notifications" >&2; exit 1; }

payload_file="$(mktemp)"
config_file="$(mktemp)"
error_file="$(mktemp)"
trap 'rm -f "$payload_file" "$config_file" "$error_file"' EXIT

trim() {
  local value="$1"
  printf '%s' "$value" | awk 'BEGIN { ORS="" } { if (length(out)) out=out "\\n"; out=out $0 } END { print substr(out, 1, 1000) }'
}

commit_message="$(trim "${DEPLOY_COMMIT_MESSAGE:-unknown}")"
log_excerpt="$(printf '%s' "${DEPLOY_LOG_EXCERPT:-}" | MAX_CHARS=2500 "$(dirname "$0")/redact-log.sh")"
image_tag="${DEPLOY_IMAGE_TAG:-unknown}"
target="${DEPLOY_TARGET:-GSMSV}"
run_url="${GITHUB_RUN_URL:-unknown}"
now="${DEPLOY_TIME:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

case "$event" in
  started)
    payload="$(jq -n \
      --arg env "${DEPLOY_ENVIRONMENT:-develop}" \
      --arg branch "${GITHUB_REF_NAME:-develop}" \
      --arg sha "${DEPLOY_SHORT_SHA:-unknown}" \
      --arg message "$commit_message" \
      --arg actor "${GITHUB_ACTOR:-unknown}" \
      --arg url "$run_url" \
      --arg time "$now" \
      '{embeds: [{title: "GSMSV 배포 시작", color: 16753920, fields: [
        {name: "환경", value: $env, inline: true},
        {name: "브랜치", value: $branch, inline: true},
        {name: "Commit", value: ($sha + "\\n" + $message), inline: false},
        {name: "요청자", value: $actor, inline: true},
        {name: "실행", value: ("[GitHub Actions]\\(" + $url + "\\)"), inline: true},
        {name: "시작 시각", value: $time, inline: true}
      ]}]}')" ;;
  succeeded)
    payload="$(jq -n \
      --arg image "$image_tag" \
      --arg target "$target" \
      --arg endpoint "${DEPLOY_HTTPS_URL:-https://mudda-api.https.gsmsv.site}" \
      --arg duration "${DEPLOY_DURATION:-unknown}" \
      --arg url "$run_url" \
      --arg time "$now" \
      '{embeds: [{title: "GSMSV 배포 성공", color: 5763719, fields: [
        {name: "상태", value: "성공", inline: true},
        {name: "이미지", value: $image, inline: false},
        {name: "대상", value: $target, inline: true},
        {name: "주소", value: $endpoint, inline: false},
        {name: "소요 시간", value: $duration, inline: true},
        {name: "실행", value: ("[GitHub Actions]\\(" + $url + "\\)"), inline: true},
        {name: "완료 시각", value: $time, inline: true}
      ]}]}')" ;;
  failed)
    payload="$(jq -n \
      --arg step "${DEPLOY_FAILED_STEP:-unknown}" \
      --arg code "${DEPLOY_EXIT_CODE:-unknown}" \
      --arg rollback "${DEPLOY_ROLLBACK_RESULT:-not attempted}" \
      --arg logs "$log_excerpt" \
      --arg url "$run_url" \
      --arg time "$now" \
      '{embeds: [{title: "GSMSV 배포 실패", color: 15548997, fields: [
        {name: "실패 단계", value: $step, inline: true},
        {name: "종료 코드", value: $code, inline: true},
        {name: "Rollback", value: $rollback, inline: true},
        {name: "요약 로그", value: (if $logs == "" then "(없음)" else ("```\\n" + $logs + "\\n```") end), inline: false},
        {name: "실행", value: ("[GitHub Actions]\\(" + $url + "\\)"), inline: true},
        {name: "발생 시각", value: $time, inline: true}
      ]}]}')" ;;
esac

printf '%s' "$payload" > "$payload_file"
{
  printf 'url = "'
  printf '%s' "$webhook_url"
  printf '"\nrequest = "POST"\nheader = "Content-Type: application/json"\ndata-binary = @%s\n' "$payload_file"
} > "$config_file"

if ! curl --silent --show-error --fail --max-time 15 --config "$config_file" >/dev/null 2>"$error_file"; then
  echo "Discord deployment notification failed" >&2
  exit 1
fi
