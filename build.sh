#!/bin/bash

#set -e
#set -x

BUILD=./build

rm -rf $BUILD
rm -rf sources
rm -rf SCJChecker.jar
rm -rf lib/SCJChecker.jar
mkdir $BUILD
rm -rf ./lib/scj.jar


JAVAC=./localbin/checkers/binary/javac



#CLASSPATH=lib/my-checkers.jar:lib/langtools.jar:lib/build/:lib/jsr308-all.jar:lib/checkers.jar

CLASSPATH=lib/build/:lib/jsr308-all.jar:lib/checkers.jar

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
cd $BUILD && find . -name "*.class" | xargs jar cf ../SCJChecker.jar && cd ..
mv SCJChecker.jar lib/

echo "SCJ-Checker installation completed."

#test-suite
#echo "SCJChecker Test Suite compilation..."
#
#cd testsuite && ./build.sh