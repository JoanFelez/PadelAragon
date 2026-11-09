#!/usr/bin/env bash
# Upload backend/rules/reglamento.md to the Worker KV (key rules:current).
# Usage: backend/scripts/upload-rules.sh [--local] [path/to/reglamento.md]
# Needs `wrangler login` (or CLOUDFLARE_API_TOKEN) and the real KV id in backend/worker/wrangler.toml.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
remote=(--remote)
if [[ "${1:-}" == "--local" ]]; then remote=(--local); shift; fi
file="${1:-$root/rules/reglamento.md}"

[[ -f "$file" ]] || { echo "Rulebook not found: $file (run backend/scripts/convert-rules first)" >&2; exit 1; }

cd "$root/worker"
npx wrangler kv key put --binding PADEL rules:current --path "$file" "${remote[@]}"
