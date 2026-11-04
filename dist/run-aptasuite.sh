#!/bin/sh

DIR="$(cd "$(dirname "$0")" && pwd)"
APTA="$(ls "$DIR"/aptasuite-[0-9]*.jar 2>/dev/null | sort | tail -n 1)"
if [ -z "$APTA" ]; then
  echo "ERROR: Could not find aptasuite-x.y.z.jar in $DIR."
  exit 1
fi

# Example: JAVA_OPTS='-Xmx20G' ./run-aptasuite.sh
# Intentional word splitting allows JAVA_OPTS to contain multiple JVM options.
# shellcheck disable=SC2086
exec java $JAVA_OPTS -cp "$APTA:$DIR/aptasuite-chart-export.jar" chartexport.Launcher "$@"
