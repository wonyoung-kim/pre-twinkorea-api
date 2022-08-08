#!/usr/bin/env bash

echo 'Remove unnecessary files.(*.jar, *.zip)'

rm -r .gradle
rm -r build
rm *.zip
rm *.jar

bash ./gradlew clean build -x test

cp build/libs/*.jar .

docker build -t twinkorea-api:latest .

DATE=$(date '+%Y%m%d%H%M')
zip -r twinkorea-api-${DATE}.zip twinkorea-api-0.0.1-SNAPSHOT.jar Dockerfile Dockerrun.aws.json
