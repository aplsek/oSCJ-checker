#!/bin/bash

#set -e
#set -x

BUILD=./build

rm -rf $BUILD
mkdir $BUILD

CP=../lib/my-checkers.jar:../lib/langtools.jar:../lib/scj-checker.jar:../lib/jsr308-all.jar:../lib/checkers.jar:../lib/junit-4.7.jar
if [ -f "../../../../../lib/scj.jar" ]
then
        CP=$CP:../../../../../lib/scj.jar
else
        CP=$CP:../lib/scj.jar
fi

find ./src -name "*.java" | xargs javac -cp $CP -d $BUILD



