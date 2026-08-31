#!/usr/bin/env bash
set -euo pipefail

[[ "$(id -u)" -eq 0 ]] || { echo "run this installer as root" >&2; exit 1; }
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
install_dir=/usr/local/libexec/mudda-runtime-alert
state_dir=/var/lib/mudda/runtime-alerts
env_dir=/etc/mudda

[[ -f "$repo_dir/mudda-runtime-alert.sh" && -f "$repo_dir/discord-runtime-notify.sh" ]] || { echo "runtime alert sources are missing" >&2; exit 1; }
install -d -m 0750 "$install_dir" "$state_dir" "$env_dir"
install -m 0750 "$repo_dir/mudda-runtime-alert.sh" "$install_dir/mudda-runtime-alert.sh"
install -m 0750 "$repo_dir/discord-runtime-notify.sh" "$install_dir/discord-runtime-notify.sh"
install -m 0750 "$repo_dir/redact-log.sh" "$install_dir/redact-log.sh"
install -m 0644 "$repo_dir/mudda-runtime-alert.service" /etc/systemd/system/mudda-runtime-alert.service
install -m 0644 "$repo_dir/mudda-runtime-alert.timer" /etc/systemd/system/mudda-runtime-alert.timer

if [[ ! -e "$env_dir/discord-alert.env" ]]; then
  install -m 0600 /dev/null "$env_dir/discord-alert.env"
  echo "Created empty $env_dir/discord-alert.env; add DISCORD_RUNTIME_WEBHOOK_URL before enabling the timer."
fi
chmod 0600 "$env_dir/discord-alert.env"
systemctl daemon-reload
echo "Installed runtime alert files. Enable after configuring the root-owned webhook file:"
echo "  systemctl enable --now mudda-runtime-alert.timer"
