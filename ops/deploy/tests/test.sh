#!/usr/bin/env bash
set -euo pipefail

test_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
deploy_dir="$(cd "$test_dir/.." && pwd)"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

bash -n "$deploy_dir"/*.sh

redacted="$(printf '%s' 'Bearer abc password=secret AKIA1234567890ABCDEF https://discord.com/api/webhooks/123/token a@b.example :: 1234567890' | MAX_CHARS=2500 "$deploy_dir/redact-log.sh")"
[[ "$redacted" != *'abc'* && "$redacted" != *'secret'* && "$redacted" != *'AKIA1234567890ABCDEF'* ]]
[[ "$redacted" != *'discord.com/api/webhooks/123/token'* && "$redacted" != *'a@b.example'* && "$redacted" != *'::'* ]]
[[ "${#redacted}" -le 2500 ]]

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
data_file="$(sed -n 's/^data-binary = @//p' "$config")"
cp "$data_file" "${FAKE_CURL_PAYLOAD:?}"
EOF
chmod +x "$fake_bin/curl"

payload="$tmp_dir/payload.json"
PATH="$fake_bin:$PATH" \
FAKE_CURL_PAYLOAD="$payload" \
DISCORD_DEPLOY_WEBHOOK_URL='https://discord.com/api/webhooks/test/token' \
DEPLOY_COMMIT_MESSAGE=$'message "quoted"\nsecond line' \
GITHUB_RUN_URL='https://github.com/CKLOB/MUDDA-Server/actions/runs/1' \
  "$deploy_dir/discord-notify.sh" started
jq -e '.embeds | length == 1 and (.[0].fields | length == 6)' "$payload" >/dev/null
jq -e '.embeds[0].fields[2].value | contains("quoted")' "$payload" >/dev/null

if DISCORD_DEPLOY_WEBHOOK_URL='' "$deploy_dir/discord-notify.sh" started 2>"$tmp_dir/missing.err"; then
  echo "missing webhook unexpectedly succeeded" >&2
  exit 1
fi
! grep -q 'discord.com' "$tmp_dir/missing.err"

cat > "$fake_bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf 'docker %s\n' "$*" >> "${FAKE_CALLS:?}"
EOF
cat > "$fake_bin/git" <<'EOF'
#!/usr/bin/env bash
if [[ "$*" == *'log -1 --format=%s'* ]]; then printf 'safe test commit'; else printf '0000000000000000000000000000000000000000'; fi
EOF
cat > "$fake_bin/ssh" <<'EOF'
#!/usr/bin/env bash
printf 'ssh called\n' >> "${FAKE_CALLS:?}"
exit 1
EOF
chmod +x "$fake_bin/docker" "$fake_bin/git" "$fake_bin/ssh"

calls="$tmp_dir/calls"
fake_sha=0123456789012345678901234567890123456789
PATH="$fake_bin:$PATH" FAKE_CALLS="$calls" DRY_RUN=true GITHUB_SHA="$fake_sha" GITHUB_REPOSITORY=CKLOB/MUDDA-Server \
  "$deploy_dir/deploy.sh" >"$tmp_dir/dry-run.out"
grep -q 'Dry-run build completed' "$tmp_dir/dry-run.out"
grep -q 'docker buildx build' "$calls"
! grep -q '^ssh called' "$calls"

echo "deployment automation tests passed"
