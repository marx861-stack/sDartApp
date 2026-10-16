#!/usr/bin/env bash
# s'Dart App Linux Launcher
# Behebt den nativen C++-Symbolkonflikt zwischen Skiko (libskiko-linux-x64.so) und ONNX Runtime (libonnxruntime.so)
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Suche nach dem gebauten JAR
JAR_PATH=""
if [ -f "$SCRIPT_DIR/dist/sDartApp-linux-x64-1.0.0.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/dist/sDartApp-linux-x64-1.0.0.jar"
elif [ -f "$SCRIPT_DIR/composeApp/build/compose/jars/sDartApp-linux-x64-1.0.0.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/composeApp/build/compose/jars/sDartApp-linux-x64-1.0.0.jar"
elif [ -f "$SCRIPT_DIR/composeApp/build/compose/jars/sDartApp-1.0.0.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/composeApp/build/compose/jars/sDartApp-1.0.0.jar"
elif [ -f "$SCRIPT_DIR/composeApp/build/compose/jars/sDartApp-windows-x64-1.0.0.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/composeApp/build/compose/jars/sDartApp-windows-x64-1.0.0.jar"
fi

if [ -z "$JAR_PATH" ]; then
    echo "[s'Dart App] Fehler: Kein JAR gefunden. Bitte zuerst './gradlew :composeApp:packageLinuxJar' ausführen."
    exit 1
fi

# Preload system libstdc++.so.6 um Symbol-Konflikte (SIGSEGV in std::ostream::_M_insert) zu verhindern
if [ -f "/usr/lib/x86_64-linux-gnu/libstdc++.so.6" ]; then
    export LD_PRELOAD="/usr/lib/x86_64-linux-gnu/libstdc++.so.6${LD_PRELOAD:+:$LD_PRELOAD}"
elif [ -f "/usr/lib64/libstdc++.so.6" ]; then
    export LD_PRELOAD="/usr/lib64/libstdc++.so.6${LD_PRELOAD:+:$LD_PRELOAD}"
elif [ -f "/usr/lib/libstdc++.so.6" ]; then
    export LD_PRELOAD="/usr/lib/libstdc++.so.6${LD_PRELOAD:+:$LD_PRELOAD}"
fi

echo "[s'Dart App] Starte $JAR_PATH..."
exec java -jar "$JAR_PATH" "$@"
