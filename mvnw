#!/usr/bin/env sh
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
sh "$DIR/.mvn/wrapper/maven-wrapper.sh" "$@"
