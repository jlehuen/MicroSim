#!/bin/bash

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd) || exit 1
PROJECT_ROOT=$(cd "$SCRIPT_DIR/.." && pwd) || exit 1
PKG_NAME="tp4-grader"
STAGE_DIR="$PROJECT_ROOT/build/$PKG_NAME"
OUT_ZIP="$PROJECT_ROOT/distrib/${PKG_NAME}.zip"
JAR_BIN="/Users/lehuen/bin/JDK-25.0.1+8/Contents/Home/bin/jar"

if [ ! -x "$JAR_BIN" ]; then
    JAR_BIN=$(command -v jar)
fi

# Compile latest Java sources
"$PROJECT_ROOT/compile.sh" || exit 1

rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR/grader" "$STAGE_DIR/TP4/correction" "$PROJECT_ROOT/distrib"

# Build standalone headless JAR (simulator + headless runner only)
"$JAR_BIN" cf "$STAGE_DIR/microsim-headless.jar" \
    -C "$PROJECT_ROOT/class" microsim/simulator \
    -C "$PROJECT_ROOT/class" microsim/headless || exit 1

# Copy Python grader sources (excluding venv and caches)
cp "$PROJECT_ROOT/run-grader.sh" "$STAGE_DIR/"
chmod +x "$STAGE_DIR/run-grader.sh"
cp "$PROJECT_ROOT/grader/"*.py "$STAGE_DIR/grader/"
cp "$PROJECT_ROOT/grader/TP4.yaml" "$STAGE_DIR/grader/"
cp "$PROJECT_ROOT/grader/requirements.txt" "$STAGE_DIR/grader/"
cp "$PROJECT_ROOT/grader/README.md" "$STAGE_DIR/README.md"

# Include reference correction and sample student files as smoke-test samples
cp -R "$PROJECT_ROOT/TP4/correction/asm" "$STAGE_DIR/TP4/correction/asm"
cp -R "$PROJECT_ROOT/TP4/etudiant_test" "$STAGE_DIR/TP4/etudiant_test"

# Create zip archive
rm -f "$OUT_ZIP"
(cd "$PROJECT_ROOT/build" && zip -qr "$OUT_ZIP" "$PKG_NAME") || exit 1
rm -rf "$STAGE_DIR"

echo "Package created: $OUT_ZIP"
