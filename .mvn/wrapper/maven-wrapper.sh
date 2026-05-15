#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"
MAVEN_VERSION="3.9.9"
MAVEN_DIR="$SCRIPT_DIR/apache-maven-$MAVEN_VERSION"
ARCHIVE="$SCRIPT_DIR/apache-maven-$MAVEN_VERSION-bin.tar.gz"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
  if [ ! -f "$ARCHIVE" ]; then
    echo "Downloading Maven $MAVEN_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$MAVEN_URL" -o "$ARCHIVE"
    else
      wget -q "$MAVEN_URL" -O "$ARCHIVE"
    fi
  fi
  echo "Extracting Maven $MAVEN_VERSION..."
  tar -xzf "$ARCHIVE" -C "$SCRIPT_DIR"
fi

exec "$MAVEN_DIR/bin/mvn" -f "$PROJECT_ROOT/pom.xml" "$@"
