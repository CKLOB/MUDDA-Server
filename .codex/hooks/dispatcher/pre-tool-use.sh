#!/bin/bash
INPUT=$(cat)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULES_DIR="$SCRIPT_DIR/modules"
[[ -d "$MODULES_DIR" ]] || MODULES_DIR="${SCRIPT_DIR%/*}/modules"

[[ -d "$MODULES_DIR" ]] || exit 0

for hook in "$MODULES_DIR"/*/pre-tool-use.sh; do
    [[ -f "$hook" ]] || continue
    OUTPUT=$(echo "$INPUT" | bash "$hook")
    STATUS=$?
    if [[ $STATUS -ne 0 ]]; then
        if [[ -n "$OUTPUT" ]]; then
            echo "$OUTPUT"
        else
            echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"Dangerous command blocked by project hook"}}'
        fi
        exit 0
    fi
done

exit 0