#!/bin/sh
set -eu

if [ -z "${MVN:-}" ] && [ -x /usr/local/bin/mvn ]; then
  MVN="/usr/local/bin/mvn"
fi
MVN="${MVN:-mvn}"

"$MVN" -q -DskipTests package test-compile dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt
java -cp "target/classes:target/test-classes:$(cat target/test-classpath.txt)" orbit.acceptance.GherkinMutator --feature features/orbit-simulator.feature "$@"
