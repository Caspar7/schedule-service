#!/usr/bin/env bash
serviceName="schedule-service"
BUILD_NUMBER=$1

echo "start build docker image ..."
docker build -t ${serviceName}:${BUILD_NUMBER} .

echo "create docker images dir ..."
mkdir -p ./build/${serviceName}-${BUILD_NUMBER}

echo "save docker image ${serviceName}_${BUILD_NUMBER}.tar ..."
docker save -o ./build/${serviceName}-${BUILD_NUMBER}/${serviceName}_${BUILD_NUMBER}.tar ${serviceName}:${BUILD_NUMBER}
cp ./docker-deploy.sh ./build/${serviceName}-${BUILD_NUMBER}