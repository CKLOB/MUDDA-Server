#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
compose_project="${MUDDA_COMPOSE_PROJECT:-mudda-prod}"
compose_file="${MUDDA_COMPOSE_FILE:-/opt/mudda/docker-compose.prod.yml}"
compose_env="${MUDDA_COMPOSE_ENV:-/opt/mudda/.env.production}"
state_dir="${MUDDA_RUNTIME_STATE_DIR:-/var/lib/mudda/runtime-alerts}"
webhook_env="${MUDDA_RUNTIME_WEBHOOK_ENV:-/etc/mudda/discord-alert.env}"
fixture_dir="${MUDDA_RUNTIME_FIXTURE_DIR:-}"
no_send="${MUDDA_RUNTIME_NO_SEND:-false}"
disk_warn="${MUDDA_DISK_WARN_PERCENT:-80}"
disk_critical="${MUDDA_DISK_CRITICAL_PERCENT:-90}"
memory_warn="${MUDDA_MEMORY_WARN_PERCENT:-85}"
memory_critical="${MUDDA_MEMORY_CRITICAL_PERCENT:-90}"
cooldown="${MUDDA_ALERT_COOLDOWN_SECONDS:-600}"
readiness_timeout="${MUDDA_READINESS_TIMEOUT_SECONDS:-5}"
now="${MUDDA_RUNTIME_NOW:-$(date +%s)}"

for value in "$now" "$cooldown" "$readiness_timeout" "$disk_warn" "$disk_critical" "$memory_warn" "$memory_critical"; do
  [[ "$value" =~ ^[0-9]+$ ]] || { echo "runtime alert numeric configuration is invalid" >&2; exit 2; }
done
(( disk_critical >= disk_warn && memory_critical >= memory_warn )) || { echo "runtime alert thresholds are invalid" >&2; exit 2; }
mkdir -p "$state_dir"
exec 9>"$state_dir/.lock"
flock -n 9 || { echo "runtime alert check is already running"; exit 0; }

state_file="$state_dir/state.json"
state='{}'
[[ -f "$state_file" ]] && state="$(cat "$state_file")"
last_check="$(jq -r '.last_check_epoch // 0' <<< "$state")"
had_problem="$(jq -r '.had_problem // false' <<< "$state")"
old_fingerprint="$(jq -r '.active_fingerprint // empty' <<< "$state")"
last_alert="$(jq -r '.last_alert_epoch // 0' <<< "$state")"
log_window=60
if (( last_check > 0 && now > last_check )); then
  log_window=$((now - last_check))
  (( log_window <= 600 )) || log_window=600
fi

fixture_read() {
  local name="$1" fallback="${2:-}"
  if [[ -n "$fixture_dir" && -f "$fixture_dir/$name" ]]; then cat "$fixture_dir/$name"; else printf '%s' "$fallback"; fi
}

container_info() {
  local service="$1" id
  if [[ -n "$fixture_dir" ]]; then
    fixture_read "$service.status" "missing|none|0"
    return
  fi
  id="$(docker ps -a --filter "label=com.docker.compose.project=$compose_project" --filter "label=com.docker.compose.service=$service" --format '{{.ID}}' | head -n 1)"
  [[ -n "$id" ]] || { printf 'missing|none|0'; return; }
  docker inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}|{{.RestartCount}}' "$id"
}

readiness_ok() {
  if [[ -n "$fixture_dir" ]]; then [[ "$(fixture_read readiness fail)" == ok ]]; return; fi
  curl --fail --silent --max-time "$readiness_timeout" http://127.0.0.1:8080/actuator/health/readiness >/dev/null
}

metric() {
  local name="$1"
  if [[ -n "$fixture_dir" ]]; then fixture_read "$name" 0; return; fi
  if [[ "$name" == disk_percent ]]; then df -P / | awk 'NR == 2 { gsub(/%/, "", $5); print $5 }'; else free | awk '/Mem:/ { printf "%d", ($2 - $7) * 100 / $2 }'; fi
}

logs=""
if [[ -n "$fixture_dir" ]]; then
  logs="$(fixture_read app.logs "")"
else
  app_id="$(docker ps -a --filter "label=com.docker.compose.project=$compose_project" --filter 'label=com.docker.compose.service=app' --format '{{.ID}}' | head -n 1)"
  [[ -z "$app_id" ]] || logs="$(docker logs --since "${log_window}s" --tail 100 "$app_id" 2>&1 || true)"
fi
logs="$(printf '%s' "$logs" | MAX_CHARS=2500 "$script_dir/redact-log.sh")"

declare -a problems=()
declare -a service_rows=()
declare -a restart_rows=()
for service in app postgres redis; do
  IFS='|' read -r status health restarts <<< "$(container_info "$service")"
  service_rows+=("$service=$status/$health")
  restart_rows+=("$service=$restarts")
  [[ "$status" == running ]] || problems+=("$service container=$status")
  [[ "$health" == healthy ]] || problems+=("$service health=$health")
  previous_restart="$(jq -r --arg service "$service" '.restart_counts[$service] // 0' <<< "$state")"
  [[ "$restarts" =~ ^[0-9]+$ && "$previous_restart" =~ ^[0-9]+$ && "$restarts" -le "$previous_restart" ]] || problems+=("$service restart_count=$restarts (previous=$previous_restart)")
done

if ! readiness_ok; then problems+=("app readiness=failed"); fi
disk="$(metric disk_percent)"
memory="$(metric memory_percent)"
if (( disk >= disk_critical )); then problems+=("disk critical=${disk}%"); elif (( disk >= disk_warn )); then problems+=("disk warning=${disk}%"); fi
if (( memory >= memory_critical )); then problems+=("memory critical=${memory}%"); elif (( memory >= memory_warn )); then problems+=("memory warning=${memory}%"); fi
if printf '%s' "$logs" | grep -Eqi '(^|[^[:alpha:]])ERROR([^[:alpha:]]|$)'; then problems+=("app ERROR log detected"); fi

problem_text="$(printf '%s\n' "${problems[@]:-}" | sed '/^$/d' | head -n 20)"
fingerprint=""
[[ -z "$problem_text" ]] || fingerprint="$(printf '%s' "$problem_text" | sha256sum | awk '{print $1}')"
service_text="$(IFS=', '; printf '%s' "${service_rows[*]}")"
restart_text="$(IFS=', '; printf '%s' "${restart_rows[*]}")"

write_state() {
  local next="$1" temp
  temp="$(mktemp "$state_dir/state.XXXXXX")"
  chmod 600 "$temp"
  printf '%s\n' "$next" > "$temp"
  mv -f "$temp" "$state_file"
}

make_state() {
  local active="$1" alerted="$2" had="$3"
  jq -n --argjson now "$now" --arg fingerprint "$active" --argjson alerted "$alerted" --argjson had "$had" \
    --argjson counts "$(printf '%s\n' "${restart_rows[@]}" | jq -Rn '[inputs | split("=") | {key: .[0], value: (.[1] | tonumber)}] | from_entries')" \
    '{last_check_epoch: $now, active_fingerprint: (if $fingerprint == "" then null else $fingerprint end), last_alert_epoch: $alerted, had_problem: $had, restart_counts: $counts}'
}

notify() {
  local status="$1"
  [[ "$no_send" == true ]] && { echo "runtime notification skipped (test mode)"; return 0; }
  local webhook
  [[ -f "$webhook_env" ]] || { echo "runtime webhook env file is missing" >&2; return 1; }
  webhook="$(sed -n 's/^DISCORD_RUNTIME_WEBHOOK_URL=//p' "$webhook_env" | head -n 1)"
  webhook="${webhook#\"}"; webhook="${webhook%\"}"
  [[ -n "$webhook" ]] || { echo "runtime webhook is not configured" >&2; return 1; }
  DISCORD_RUNTIME_WEBHOOK_URL="$webhook" \
    RUNTIME_SUMMARY="${problem_text:-normal}" \
    RUNTIME_LOG_EXCERPT="$logs" \
    RUNTIME_SERVICE="app, postgres, redis" \
    RUNTIME_CONTAINER_STATUS="$service_text" \
    RUNTIME_HEALTH_STATUS="readiness=$([[ -z "$fingerprint" ]] && echo healthy || echo failed); disk=${disk}%; memory=${memory}%" \
    RUNTIME_RESTART_COUNTS="$restart_text" \
    "$script_dir/discord-runtime-notify.sh" "$status"
}

if [[ -n "$fingerprint" ]]; then
  alert_epoch="$last_alert"
  should_alert=true
  if [[ "$fingerprint" == "$old_fingerprint" && $((now - last_alert)) -lt cooldown ]]; then should_alert=false; fi
  if [[ "$should_alert" == true ]]; then
    if notify incident; then alert_epoch="$now"; else echo "runtime incident notification could not be sent" >&2; fi
    write_state "$(make_state "$fingerprint" "$alert_epoch" true)"
    echo "runtime status: incident"
  else
    write_state "$(make_state "$fingerprint" "$alert_epoch" true)"
    echo "runtime status: incident suppressed"
  fi
elif [[ "$had_problem" == true ]]; then
  if notify recovery; then
    write_state "$(make_state "" "$last_alert" false)"
    echo "runtime status: recovery"
  else
    write_state "$(make_state "$old_fingerprint" "$last_alert" true)"
    echo "runtime recovery notification could not be sent" >&2
  fi
else
  write_state "$(make_state "" "$last_alert" false)"
  echo "runtime status: healthy"
fi
