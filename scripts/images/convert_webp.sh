#!/usr/bin/env bash
# TODO: move in the CI pipeline
set -euo pipefail

SRC_DIR=${1:-"$(dirname "$0")/../../shared/src/commonMain/composeResources/drawable"}
BACKUP_DIR=${2:-"/tmp/webpify_backup_$(date -u +%Y%m%dT%H%M%SZ)"}

JPEG_Q=${JPEG_Q:-85}            # lossy quality for JPG
PNG_MODE=${PNG_MODE:-lossless}  # "lossless" or "q"
PNG_Q=${PNG_Q:-85}              # used if PNG_MODE=q

command -v cwebp >/dev/null || { echo "cwebp not found (install 'webp')"; exit 1; }

SRC_DIR=${SRC_DIR%/}
echo "Source: $SRC_DIR"
echo "Backup: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

# Recurse robustly and handle spaces
export SRC_DIR BACKUP_DIR JPEG_Q PNG_MODE PNG_Q
find "$SRC_DIR" -type f \( -iname "*.png" -o -iname "*.jpg" -o -iname "*.jpeg" \) -print0 |
while IFS= read -r -d '' f; do
  rel="${f#"$SRC_DIR"/}"
  ext="${f##*.}"; ext="${ext,,}"          # lowercased extension
  base_noext="${f%.*}"
  dest="${base_noext}.webp"

  # 1) backup original
  backup_dest="$BACKUP_DIR/$rel"
  mkdir -p "$(dirname "$backup_dest")"
  cp -p "$f" "$backup_dest"

  # 2) convert -> .webp (overwrite if exists)
  if [[ "$ext" == "png" ]]; then
    if [[ "$PNG_MODE" == "lossless" ]]; then
      cwebp -quiet -lossless -z 9 "$f" -o "$dest"
    else
      cwebp -quiet -q "$PNG_Q" -m 6 -alpha_q 100 "$f" -o "$dest"
    fi
  else
    cwebp -quiet -q "$JPEG_Q" -m 6 "$f" -o "$dest"
  fi

  # 3) remove original after successful conversion
  rm -f "$f"
  echo "Converted & removed: $rel -> ${rel%.*}.webp"
done

echo "Done. Originals backed up in: $BACKUP_DIR"

