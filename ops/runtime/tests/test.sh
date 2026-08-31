#!/usr/bin/env bash
set -euo pipefail

test_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_dir="$(cd "$test_dir/.." && pwd)"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

bash -n "$runtime_dir"/*.sh "$runtime_dir"/tests/test.sh

flock_available=true
if ! command -v flock >/dev/null; then
  flock_available=false
  test_bin="$tmp_dir/bin"
  mkdir -p "$test_bin"
  cat > "$test_bin/flock" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
  chmod +x "$test_bin/flock"
  PATH="$test_bin:$PATH"
fi

fixture="$tmp_dir/fixture"
state="$tmp_dir/state"
mkdir -p "$fixture"
for service in app postgres redis; do printf 'running|healthy|0\n' > "$fixture/$service.status"; done
printf 'ok\n' > "$fixture/readiness"
printf '10\n' > "$fixture/disk_percent"
printf '20\n' > "$fixture/memory_percent"
printf 'INFO healthy\n' > "$fixture/app.logs"

run_check() {
  MUDDA_RUNTIME_FIXTURE_DIR="$fixture" MUDDA_RUNTIME_STATE_DIR="$state" MUDDA_RUNTIME_NO_SEND=true MUDDA_RUNTIME_NOW="$1" \
    bash "$runtime_dir/mudda-runtime-alert.sh"
}
[[ "$(run_check 100)" == *'runtime status: healthy'* ]]

printf 'running|unhealthy|0\n' > "$fixture/app.status"
printf 'ERROR password=secret Bearer abc\n' > "$fixture/app.logs"
[[ "$(run_check 200)" == *'runtime status: incident'* ]]
[[ "$(run_check 201)" == *'incident suppressed'* ]]
[[ "$(cat "$state/state.json")" != *'secret'* ]]

printf 'running|healthy|0\n' > "$fixture/app.status"
printf 'INFO recovered\n' > "$fixture/app.logs"
[[ "$(run_check 900)" == *'runtime status: recovery'* ]]
[[ ! -e "$state/state.XXXXXX" ]]

printf 'exited|none|0\n' > "$fixture/postgres.status"
[[ "$(run_check 1000)" == *'runtime status: incident'* ]]
printf 'running|healthy|0\n' > "$fixture/postgres.status"
printf 'running|unhealthy|0\n' > "$fixture/redis.status"
[[ "$(run_check 1100)" == *'runtime status: incident'* ]]
printf 'running|healthy|0\n' > "$fixture/redis.status"
printf 'fail\n' > "$fixture/readiness"
[[ "$(run_check 1200)" == *'runtime status: incident'* ]]
printf 'ok\n' > "$fixture/readiness"
printf 'running|healthy|1\n' > "$fixture/app.status"
[[ "$(run_check 1300)" == *'runtime status: incident'* ]]
printf 'running|healthy|0\n' > "$fixture/app.status"
printf '95\n' > "$fixture/disk_percent"
printf '92\n' > "$fixture/memory_percent"
[[ "$(run_check 1400)" == *'runtime status: incident'* ]]
printf '10\n' > "$fixture/disk_percent"
printf '20\n' > "$fixture/memory_percent"
[[ "$(run_check 2000)" == *'runtime status: recovery'* ]]

fake_bin="$tmp_dir/bin"
mkdir -p "$fake_bin"
cat > "$fake_bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
config=""
while (($#)); do
  [[ "$1" == --config ]] && config="$2" && shift 2 && continue
  shift
done
cp "$(sed -n 's/^data-binary = @//p' "$config")" "${FAKE_RUNTIME_PAYLOAD:?}"
EOF
chmod +x "$fake_bin/curl"
payload="$tmp_dir/runtime-payload.json"
PATH="$fake_bin:$PATH" FAKE_RUNTIME_PAYLOAD="$payload" \
  DISCORD_RUNTIME_WEBHOOK_URL='https://discord.com/api/webhooks/test/token' \
  RUNTIME_SUMMARY=$'ERROR "quoted"\npassword=secret' \
  RUNTIME_LOG_EXCERPT=$'Bearer abc\nAKIA1234567890ABCDEF' \
  bash "$runtime_dir/discord-runtime-notify.sh" incident
jq -e '.embeds | length == 1 and (.[0].fields | length == 8)' "$payload" >/dev/null
! grep -q 'secret' "$payload"
! grep -q 'AKIA1234567890ABCDEF' "$payload"

if DISCORD_RUNTIME_WEBHOOK_URL='' bash "$runtime_dir/discord-runtime-notify.sh" incident 2>"$tmp_dir/missing.err"; then exit 1; fi
! grep -q 'discord.com' "$tmp_dir/missing.err"

if [[ "$flock_available" == true ]]; then
  exec 9>"$state/.lock"
  flock -n 9
  if MUDDA_RUNTIME_FIXTURE_DIR="$fixture" MUDDA_RUNTIME_STATE_DIR="$state" MUDDA_RUNTIME_NO_SEND=true MUDDA_RUNTIME_NOW=1000 \
    bash "$runtime_dir/mudda-runtime-alert.sh" >"$tmp_dir/lock.out"; then exit 1; fi
  grep -q 'already running' "$tmp_dir/lock.out"
  exec 9>&-
else
  grep -q 'flock -n 9' "$runtime_dir/mudda-runtime-alert.sh"
fi

echo "runtime alert tests passed"
