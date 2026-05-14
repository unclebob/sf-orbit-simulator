#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
mvn -q -DskipTests compile exec:java
