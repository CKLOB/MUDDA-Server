#!/usr/bin/env bash
set -euo pipefail

image="${1:-}"
deploy_path="${2:-}"
[[ "$image" =~ ^ghcr\.io/cklob/mudda-server:[0-9a-f]{40}$ ]] || { echo "invalid deployment image"; exit 2; }
[[ "$deploy_path" =~ ^/opt/mudda(/[A-Za-z0-9._-]+)*$ ]] || { echo "invalid deployment path"; exit 2; }
cd "$deploy_path"

compose_file="$deploy_path/docker-compose.prod.yml"
env_file="$deploy_path/.env.production"
init_sql="$deploy_path/src/main/resources/db/init/001_enable_postgis.sql"
[[ -f "$compose_file" && -f "$env_file" && -f "$init_sql" ]] || { echo "required deployment files are missing"; exit 1; }

compose=(docker compose --env-file "$env_file" -f "$compose_file")
previous_image=""
app_id="$("${compose[@]}" ps -q app 2>/dev/null || true)"
if [[ -n "$app_id" ]]; then
  previous_image="$(docker inspect --format '{{.Config.Image}}' "$app_id" 2>/dev/null || true)"
fi

redact() {
  MAX_CHARS=2500 "$(dirname "$0")/redact-log.sh"
}

diagnostics() {
  echo "COMPOSE_STATUS_START"
  "${compose[@]}" ps --all 2>/dev/null | redact || true
  echo "COMPOSE_STATUS_END"
  echo "APP_LOGS_START"
  app_id="$("${compose[@]}" ps -q app 2>/dev/null || true)"
  if [[ -n "$app_id" ]]; then docker logs --tail 100 "$app_id" 2>&1 | redact || true; fi
  echo "APP_LOGS_END"
}

wait_for_readiness() {
  local attempt=1 status health
  while (( attempt <= 30 )); do
    app_id="$("${compose[@]}" ps -q app 2>/dev/null || true)"
    if [[ -n "$app_id" ]]; then
      status="$(docker inspect --format '{{.State.Status}}' "$app_id" 2>/dev/null || true)"
      health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$app_id" 2>/dev/null || true)"
      if [[ "$status" == running && "$health" == healthy ]]; then return 0; fi
    fi
    sleep 5
    ((attempt++))
  done
  return 1
}

rollback() {
  if [[ "$previous_image" =~ ^ghcr\.io/cklob/mudda-server:[0-9a-f]{40}$ ]]; then
    echo "ROLLBACK_ATTEMPTED=true"
    if APP_IMAGE="$previous_image" "${compose[@]}" pull app >/dev/null 2>&1 &&
      APP_IMAGE="$previous_image" "${compose[@]}" up -d --no-deps app >/dev/null 2>&1 &&
      wait_for_readiness; then
      echo "ROLLBACK_RESULT=succeeded"
      return 0
    fi
    echo "ROLLBACK_RESULT=failed"
    return 1
  fi
  echo "ROLLBACK_ATTEMPTED=false"
  echo "ROLLBACK_RESULT=unavailable"
  return 1
}

echo "DEPLOY_STEP=pull"
if ! APP_IMAGE="$image" "${compose[@]}" pull app; then
  echo "DEPLOY_RESULT=failed"
  diagnostics
  exit 1
fi
echo "DEPLOY_STEP=update"
if APP_IMAGE="$image" "${compose[@]}" up -d --no-deps app && wait_for_readiness; then
  echo "DEPLOY_RESULT=succeeded"
  echo "DEPLOYED_IMAGE=$image"
  exit 0
fi

echo "DEPLOY_STEP=readiness"
diagnostics
rollback || true
echo "DEPLOY_RESULT=failed"
exit 1
