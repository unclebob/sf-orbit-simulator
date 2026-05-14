#!/bin/sh
set -eu

mkdir -p build/acceptance acceptance/generated/orbit/acceptance/generated

if [ -z "${MVN:-}" ] && [ -x /usr/local/bin/mvn ]; then
  MVN="/usr/local/bin/mvn"
fi
MVN="${MVN:-mvn}"

"$MVN" -q -DskipTests package

java -cp target/classes orbit.acceptance.GherkinParser \
  features/orbit-simulator.feature \
  build/acceptance/orbit-simulator.json

java -cp target/classes orbit.acceptance.AcceptanceGenerator \
  build/acceptance/orbit-simulator.json \
  acceptance/generated/orbit/acceptance/generated/OrbitSimulatorAcceptanceTest.java

"$MVN" -q test
