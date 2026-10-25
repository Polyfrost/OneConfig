#!/usr/bin/env bash

set -euo pipefail

modules=()

for f in "$@"; do
    module="$(printf '%s' "$f" | cut -d '/' -f 2)"
    for m in "${modules[@]}"; do
        if [[ "$module" == "$m" ]]; then
            continue 2
        fi
    done
    modules+=("$module")
done

if [[ "${#modules[@]}" -lt 1 ]]; then
    exit 0
fi

cmdline=("./gradlew")
for m in "${modules[@]}"; do
    cmdline+=(":modules:$m:apiCheck")
done
"${cmdline[@]}"
