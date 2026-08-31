#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root_dir="$(cd "$script_dir/../.." && pwd)"
step="initialization"
remote_output=""
rollback_result="not attempted"
started_at="$(date +%s)"

mask_value() {
  local value="$1" line
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -n "$line" ]] && printf '::add-mask::%s\n' "$line"
  done <<< "$value"
}

required_secret() {
  local name="$1" value="${!1:-}"
  if [[ -z "$value" ]]; then
    echo "Missing required secret: $name" >&2
    return 1
  fi
  mask_value "$value"
}

redact_output() {
  MAX_CHARS=2500 "$script_dir/redact-log.sh"
}

notify() {
  local event="$1"
  [[ "$DRY_RUN" == true ]] && return 0
  set +e
  DEPLOY_LOG_EXCERPT="${REMOTE_OUTPUT:-}" \
    DEPLOY_ROLLBACK_RESULT="$rollback_result" \
    DEPLOY_FAILED_STEP="$step" \
    DEPLOY_EXIT_CODE="${DEPLOY_EXIT_CODE:-1}" \
    "$script_dir/discord-notify.sh" "$event"
  local result=$?
  set -e
  if (( result != 0 )); then echo "Discord notification status: failed" >&2; fi
  return 0
}

on_error() {
  local exit_code=$?
  DEPLOY_EXIT_CODE="$exit_code"
  if [[ "$DRY_RUN" != true ]]; then
    REMOTE_OUTPUT="$(printf '%s' "${REMOTE_OUTPUT:-}" | redact_output)"
    notify failed
  fi
  echo "Deployment failed at step: $step (exit code: $exit_code)" >&2
  exit "$exit_code"
}
trap on_error ERR

DRY_RUN="${DRY_RUN:-false}"
[[ "$DRY_RUN" == true || "$DRY_RUN" == false ]] || { echo "DRY_RUN must be true or false" >&2; exit 2; }

git_sha="${GITHUB_SHA:-$(git rev-parse HEAD)}"
[[ "$git_sha" =~ ^[0-9a-f]{40}$ ]] || { echo "GITHUB_SHA must be a 40-character SHA" >&2; exit 2; }
short_sha="${git_sha:0:7}"
commit_message="$(git -C "$root_dir" log -1 --format=%s "$git_sha" 2>/dev/null || git -C "$root_dir" log -1 --format=%s)"
repo="${GITHUB_REPOSITORY:-CKLOB/MUDDA-Server}"
repo_lc="${repo,,}"
image_sha="ghcr.io/${repo_lc}:${git_sha}"
image_branch="ghcr.io/${repo_lc}:develop"

export DEPLOY_SHORT_SHA="$short_sha"
export DEPLOY_COMMIT_MESSAGE="$commit_message"
export DEPLOY_IMAGE_TAG="$image_sha"
export DEPLOY_TARGET="${DEPLOY_HOST:-GSMSV}"
export DEPLOY_TIME="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
export DEPLOY_ENVIRONMENT="develop"

step="validation"
if [[ "$DRY_RUN" == true ]]; then
  echo "Dry-run enabled: SSH, GHCR push, deployment, and Discord notification are disabled."
else
  required_secret DEPLOY_HOST
  required_secret DEPLOY_PORT
  required_secret DEPLOY_USER
  required_secret DEPLOY_SSH_KEY
  required_secret DISCORD_DEPLOY_WEBHOOK_URL
  [[ "$DEPLOY_HOST" != *[[:space:]]* ]] || { echo "DEPLOY_HOST contains whitespace" >&2; exit 2; }
  [[ "$DEPLOY_PORT" =~ ^[1-9][0-9]{0,4}$ ]] || { echo "DEPLOY_PORT is invalid" >&2; exit 2; }
  [[ "$DEPLOY_USER" =~ ^[A-Za-z_][A-Za-z0-9_.-]*$ ]] || { echo "DEPLOY_USER is invalid" >&2; exit 2; }
  [[ "${DEPLOY_PATH:-/opt/mudda}" =~ ^/opt/mudda(/[A-Za-z0-9._-]+)*$ ]] || { echo "DEPLOY_PATH is invalid" >&2; exit 2; }
fi

step="build"
docker buildx build --file "$root_dir/Dockerfile" --tag "$image_sha" --load "$root_dir"
if [[ "$DRY_RUN" == true ]]; then
  echo "Dry-run build completed for commit $short_sha."
  exit 0
fi

step="notification-start"
notify started
step="registry-login"
printf '%s' "$GITHUB_TOKEN" | docker login ghcr.io --username "${GITHUB_ACTOR:-github-actions[bot]}" --password-stdin >/dev/null
step="image-push"
docker buildx build --file "$root_dir/Dockerfile" \
  --tag "$image_sha" --tag "$image_branch" \
  --cache-from type=gha --cache-to type=gha,mode=max --push "$root_dir"

step="server-deployment"
key_file="$(mktemp)"
remote_output_file="$(mktemp)"
cleanup() { rm -f "$key_file" "$remote_output_file"; }
trap cleanup EXIT
chmod 600 "$key_file"
printf '%s\n' "$DEPLOY_SSH_KEY" > "$key_file"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/mudda}"
ssh -i "$key_file" -p "$DEPLOY_PORT" \
  -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new \
  "$DEPLOY_USER@$DEPLOY_HOST" bash -s -- "$image_sha" "$DEPLOY_PATH" \
  < "$script_dir/remote-deploy.sh" > "$remote_output_file" 2>&1 || {
    REMOTE_OUTPUT="$(cat "$remote_output_file")"
    rollback_result="$(printf '%s\n' "$REMOTE_OUTPUT" | sed -n 's/^ROLLBACK_RESULT=//p' | tail -n 1)"
    rollback_result="${rollback_result:-not attempted}"
    exit 1
  }
REMOTE_OUTPUT="$(cat "$remote_output_file")"
rollback_result="$(printf '%s\n' "$REMOTE_OUTPUT" | sed -n 's/^ROLLBACK_RESULT=//p' | tail -n 1)"
rollback_result="${rollback_result:-not needed}"

step="notification-success"
finished_at="$(date +%s)"
export DEPLOY_DURATION="$((finished_at - started_at)) seconds"
notify succeeded
echo "Deployment succeeded: $image_sha"
