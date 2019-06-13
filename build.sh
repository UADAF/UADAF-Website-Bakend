#!/bin/sh
ARCH_NAME=$(gradle properties | grep archive | sed 's/.*\: //g')
VERSION=$(gradle properties | grep version | sed 's/.*\: //g')

FULL_NAME="$ARCH_NAME-$VERSION.jar"

gradle jar

docker build --build-arg JAR_NAME="$FULL_NAME" -t bakend:$VERSION .