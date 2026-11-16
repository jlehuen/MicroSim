#!/bin/bash

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd) || exit 1
PROJECT_ROOT=$(cd "$SCRIPT_DIR/.." && pwd) || exit 1

cd "$PROJECT_ROOT" || exit 1

"$SCRIPT_DIR/clean.sh"

./compile.sh
./build.sh

mkdir -p "distrib"

"$SCRIPT_DIR/distrib-linux.sh"
"$SCRIPT_DIR/distrib-macos.sh"
"$SCRIPT_DIR/distrib-windows.sh"

echo "Copie du manuel dans distrib/..."
mkdir -p "$PROJECT_ROOT/distrib/man"
cp -r "$PROJECT_ROOT/man/"* "$PROJECT_ROOT/distrib/man/"
