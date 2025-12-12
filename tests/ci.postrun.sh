#!/bin/bash
source ./set-env.sh

docker logs wiremock > ./artifacts/results/wiremock.log
