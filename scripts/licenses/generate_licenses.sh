#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# Ensure JDK 17 is active
# ---------------------------------------------------------------------------
if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java is not installed or not on PATH. Please install JDK 17 and ensure 'java' is available."
  exit 1
fi

JAVA_VERSION_LINE=$(java -version 2>&1 | head -n1)
JAVA_MAJOR=$(echo "$JAVA_VERSION_LINE" | awk -F'[\".]' '/version/ { if ($2=="1") print $3; else print $2 }')

if [[ "$JAVA_MAJOR" != "17" ]]; then
  echo "ERROR: JDK 17 is required for license generation. Detected: $JAVA_VERSION_LINE"
  echo "Tip: set JAVA_HOME to a JDK 17 installation or use a tool like jenv/sdkman to switch."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="$ROOT_DIR/build/reports/licenses"
NODE_DIR="$ROOT_DIR/scripts/maps"

mkdir -p "$OUT_DIR"

echo "[1/3] Generating Gradle dependency license report..."
( cd "$ROOT_DIR" && ./gradlew --stacktrace generateLicenseReport )

# The jk1 plugin writes JSON per subproject as licenses-gradle.json into OUT_DIR
#
# ---------------------------------------------------------------------------
# Merge per-module Gradle JSON reports into one file for easier consumption
# ---------------------------------------------------------------------------

# Merge only if 'jq' is available on the system
if command -v jq >/dev/null 2>&1; then
  echo "Merging Gradle license reports..."
  # Collect all per-module JSONs produced by the jk1 plugin
  GRADLE_JSONS=("$OUT_DIR"/*/licenses-gradle.json)
  if [[ ${#GRADLE_JSONS[@]} -gt 0 ]]; then
    # Combine them: later objects override earlier ones when keys clash
    jq -s 'reduce .[] as $item ({}; . * $item)' "${GRADLE_JSONS[@]}" \
      > "$OUT_DIR/licenses-gradle-merged.json"
    echo "Merged Gradle report written to: $OUT_DIR/licenses-gradle-merged.json"
  else
    echo "No per-module Gradle JSON reports found to merge."
  fi
else
  echo "jq not found; skipping Gradle license merge. Individual files are in $OUT_DIR/*/licenses-gradle.json"
fi

# Node dependencies (optional: only if scripts/maps exists)
if [[ -f "$NODE_DIR/package.json" ]]; then
  echo "[2/3] Generating Node license report from scripts/maps..."
  ( 
  	cd "$NODE_DIR" &&\
	npm install --silent &&\
       	npm ci --silent &&\
  	npx --yes license-checker@25.0.1 --production --direct --json > "$OUT_DIR/licenses-node.json"
  )
else
  echo "[2/3] No scripts/maps/package.json found; skipping Node license report."
fi

echo "[3/3] Done. Reports available in: $OUT_DIR"
ls -1 "$OUT_DIR" || true


