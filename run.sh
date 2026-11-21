#!/usr/bin/env bash
# ===================================================================
# E-LIBRARY SYSTEM RUN SCRIPT
# Compiles and starts the Java server on port 8080
# ===================================================================

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "============================================================"
echo "Compiling E-Library Java Backend..."
echo "============================================================"

mkdir -p bin
javac -d bin -sourcepath src src/ELibraryServer.java

echo "Compilation successful!"
echo "Starting E-Library Server on http://0.0.0.0:8080..."
echo "Press Ctrl+C to stop."
echo "============================================================"

exec java -Xms32m -Xmx256m -cp bin ELibraryServer
