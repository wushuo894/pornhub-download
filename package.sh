#!/bin/bash

mkdir src/main/resources
rm -rf src/main/resources/dist
mkdir src/main/resources/dist
cd ui
pnpm install
pnpm run build
cp -r dist/* ../src/main/resources/dist
cd ..
mvn -B package --file pom.xml
