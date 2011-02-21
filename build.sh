#!/bin/bash

#set -e
#set -x

BUILD=./build

rm -rf $BUILD
rm -rf sources
rm -rf SCJChecker.jar
rm -rf lib/SCJChecker.jar
mkdir $BUILD

CHECKERS=./localbin
JAVAC=$CHECKERS/javac
CLASSPATH=lib/build/:$CHECKERS/jsr308-all.jar:$CHECKERS/checkers.jar:lib/junit-4.7.jar

if [ ! -f "./lib/scj.jar" ]
then
	cp ../../../../lib/scj.jar ./lib/
fi

CLASSPATH=$CLASSPATH:./lib/scj.jar

echo "Compiling SCJChecker"
find ./src -name "*.java" > sources
find ./spec -name "*.java" >> sources
$JAVAC -cp $CLASSPATH -d $BUILD @sources
rm -rf sources
cd $BUILD && find . -name "*.class" | xargs jar cf ../lib/scj-checker.jar && cd ..

echo "SCJ-Checker installation completed."
