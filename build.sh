#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mvn clean package
printf '\nBuilt: target/ChristmasFeatures-1.21.10.jar\n'
