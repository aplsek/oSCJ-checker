#!/bin/bash

#set -e
#set -x

BUILD=./build

rm -rf $BUILD
mkdir $BUILD

CLASSPATH=../lib/my-checkers.jar:../lib/langtools.jar:../lib/SCJChecker.jar:../lib/jsr308-all.jar:../lib/checkers.jar
if [ -f "../../../../../lib/scj.jar" ]
then
        CLASSPATH=$CLASSPATH:../../../../../lib/scj.jar
else
        CLASSPATH=$CLASSPATH:../lib/scj.jar
fi

find ./src -name "*.java" | xargs javac -cp $CLASSPATH -d $BUILD



