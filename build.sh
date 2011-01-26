#!/bin/bash

set -e
set -x

BUILD=./build

rm -rf $BUILD
mkdir $BUILD

JAVAC=./localbin/checkers/binary/javac

CLASSPATH=lib/my-checkers.jar:lib/langtools.jar:lib/scjChecker.jar:lib/build/:lib/jsr308-all.jar:lib/checkers.jar
if [ -f "../../../../lib/scj.jar" ] 
then 
 	CLASSPATH=$CLASSPATH:../../../../lib/scj.jar
else
	CLASSPATH=$CLASPATH:./lib/scj.jar
fi

echo "Compiling SCJChecker"
find ./src -name "*.java" > sources 
find ./spec -name "*.java" >> sources 
$JAVAC -cp $CLASSPATH -d $BUILD @sources 
echo sources
rm -rf sources

cd $BUILD && find . -name "*.class" | xargs jar cf ../SCJChecker.jar && cd ..

mv SCJChecker.jar lib/

echo "SCJ-Checker installation completed."

#test-suite
echo "SCJChecker Test Suite compilation..."

cd testsuite && ./build.sh