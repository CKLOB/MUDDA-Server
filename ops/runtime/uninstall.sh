#!/usr/bin/env bash
set -euo pipefail

[[ "$(id -u)" -eq 0 ]] || { echo "run this uninstaller as root" >&2; exit 1; }
systemctl disable --now mudda-runtime-alert.timer 2>/dev/null || true
systemctl stop mudda-runtime-alert.service 2>/dev/null || true
rm -f /etc/systemd/system/mudda-runtime-alert.timer /etc/systemd/system/mudda-runtime-alert.service
rm -f /usr/local/libexec/mudda-runtime-alert/mudda-runtime-alert.sh
rm -f /usr/local/libexec/mudda-runtime-alert/discord-runtime-notify.sh
rm -f /usr/local/libexec/mudda-runtime-alert/redact-log.sh
rmdir /usr/local/libexec/mudda-runtime-alert 2>/dev/null || true
systemctl daemon-reload
echo "Removed runtime alert service files; webhook and state files were preserved."
