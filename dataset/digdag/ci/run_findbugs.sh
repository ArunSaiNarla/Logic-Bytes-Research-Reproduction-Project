#!/bin/bash -xe
docker run \
-w /digdag \
-v `pwd`/:/digdag \
-v ~/.gradle:/root/.gradle \
$BUILD_IMAGE \
./gradlew spotbugsMain spotbugsTest --info --no-daemon
