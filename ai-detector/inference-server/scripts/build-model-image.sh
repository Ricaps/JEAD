#!/usr/bin/env bash

# Resolve the script directory so source paths are absolute (works when invoked from anywhere)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_MODELS_DIR="$SCRIPT_DIR/../models"

# Files to copy (source_path=dest_name)
FILES_TO_COPY=(
  "$SOURCE_MODELS_DIR/Dockerfile=Dockerfile"
  "$SOURCE_MODELS_DIR/socket_worker.py=socket_worker.py"
)

# Required files that must exist in each target subfolder
REQUIRED_TARGET_FILES=("worker.py" "requirements.txt")

# Prompt for parent folder containing subfolders
read -rp "Enter parent folder path containing model subfolders: " PARENT_DIR
PARENT_DIR="${PARENT_DIR/#~/$HOME}"

if [[ -z "$PARENT_DIR" ]]; then
  echo "Error: Parent folder path is empty."
  exit 1
fi

if [[ ! -d "$PARENT_DIR" ]]; then
  echo "Error: Parent folder does not exist: $PARENT_DIR"
  exit 1
fi

# Optional image prefix
read -rp "Enter optional image prefix (e.g., myrepo) or leave empty: " IMAGE_PREFIX

# Ensure source files exist before iterating
for entry in "${FILES_TO_COPY[@]}"; do
  SRC_PATH="${entry%%=*}"
  if [[ ! -f "$SRC_PATH" ]]; then
    echo "Error: Required source file not found: $SRC_PATH"
    exit 1
  fi
done

# Prepare result trackers
SUCCESS=()
FAILED=()

# Iterate over immediate subdirectories
shopt -s nullglob
for dir in "$PARENT_DIR"/*/; do
  # dir ends with a slash; skip if not a directory for safety
  [[ -d "$dir" ]] || continue
  DIR_NAME="$(basename "$dir")"

  # Determine image name (use folder name, add prefix if provided)
  if [[ -n "$IMAGE_PREFIX" ]]; then
    IMAGE_NAME="$IMAGE_PREFIX/$DIR_NAME:latest"
  else
    IMAGE_NAME="$DIR_NAME:latest"
  fi

  printf "\nProcessing folder: %s -> image: %s" "$dir" "$IMAGE_NAME"

  # Check required files in this folder
  MISSING=false
  for rf in "${REQUIRED_TARGET_FILES[@]}"; do
    if [[ ! -f "$dir/$rf" ]]; then
      echo "  Skipping: required file missing in '$dir': $rf"
      MISSING=true
    fi
  done
  if [[ "$MISSING" = true ]]; then
    FAILED+=("$DIR_NAME (missing files)")
    continue
  fi

  # Copy model files into the folder
  COPIED_FILES=()
  for entry in "${FILES_TO_COPY[@]}"; do
    SRC="${entry%%=*}"
    DEST_NAME="${entry#*=}"
    DEST_PATH="$dir/$DEST_NAME"

    cp -v "$SRC" "$DEST_PATH"
    COPIED_FILES+=("$DEST_PATH")
  done

  # Build docker image
  if (cd "$dir" && docker buildx build -t "$IMAGE_NAME" .); then
    echo "  Built image: $IMAGE_NAME"
    SUCCESS+=("$DIR_NAME")
  else
    echo "  Failed to build image for $DIR_NAME"
    FAILED+=("$DIR_NAME (build failed)")
  fi

  # Cleanup copied files
  for f in "${COPIED_FILES[@]}"; do
    rm -f "$f"
  done
  echo "  Cleaned up temporary files in $dir"
done

# Summary
printf "\nBuild summary:"
if (( ${#SUCCESS[@]} )); then
  echo "  Successes:"
  for s in "${SUCCESS[@]}"; do
    echo "    - $s"
  done
else
  echo "  No successful builds."
fi

if (( ${#FAILED[@]} )); then
  echo "  Failures:"
  for f in "${FAILED[@]}"; do
    echo "    - $f"
  done
else
  echo "  No failures."
fi

exit 0
