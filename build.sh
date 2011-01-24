#!/bin/bash

set -e
set -x

BUILD=./build

rm -rf $BUILD
mkdir $BUILD

echo "Compiling SCJChecker"
find ./src -name "*.java" | xargs ./localbin/checkers/binary/javac -cp lib/my-checkers.jar:lib/langtools.jar:lib/scjChecker.jar:lib/build/:../../../../lib/scj.jar:lib/jsr308-all.jar:lib/checkers.jar: -d $BUILD


cd $BUILD && find . -name "*.class" | xargs jar cf ../SCJChecker.jar && cd ..

mv SCJChecker.jar lib/

echo "SCJ-Checker installation completed."

#test-suite
echo "SCJChecker Test Suite compilation..."

cd testsuite && ./build.sh