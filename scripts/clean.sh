#!/bin/bash

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd) || exit 1
PROJECT_ROOT=$(cd "$SCRIPT_DIR/.." && pwd) || exit 1

cd "$PROJECT_ROOT" || exit 1

echo "Nettoyage du projet..."
rm -rf "class"
rm -rf "build"
rm -rf "distrib"
echo "Nettoyage terminé."
