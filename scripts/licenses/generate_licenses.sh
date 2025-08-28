#!/usr/bin/env bash
set -x
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="$ROOT_DIR/build/reports/licenses"
NODE_DIR="$ROOT_DIR/scripts/maps"

mkdir -p "$OUT_DIR"

echo "[1/3] Generating Gradle dependency license report..."
( cd "$ROOT_DIR" && ./gradlew --stacktrace generateLicenseReport )

# The jk1 plugin writes JSON per subproject as licenses-gradle.json into OUT_DIR

# Node dependencies (optional: only if scripts/maps exists)
if [[ -f "$NODE_DIR/package.json" ]]; then
  echo "[2/3] Generating Node license report from scripts/maps..."
  ( cd "$NODE_DIR" && npm ci --silent )
  npx --yes license-checker@25.0.1 --production --direct --json > "$OUT_DIR/licenses-node.json"
else
  echo "[2/3] No scripts/maps/package.json found; skipping Node license report."
fi

echo "[3/3] Done. Reports available in: $OUT_DIR"
ls -1 "$OUT_DIR" || true
