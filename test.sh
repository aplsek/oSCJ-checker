#!/bin/bash

OUT=build_out

rm -rf $OUT
mkdir $OUT

#echo `find ./src -name "*.java"`
#find ./src -name "*.java" | xargs javac -cp /Users/plsek/workspace/checkers/bin:/Users/plsek/workspace/checkers/build:./build/src:build/tests:./distribution/scjChecker/checkers/lib/javaparser.jar:./distribution/scjChecker/checkers/lib/asmx.jar -d $OUT

#find ./src -name "*.java" | xargs javac -cp ./build/src:build/tests:lib/checkers.jar: -d $OUT

CP=`find /Users/plsek/workspace/checkers/bin -name "*.class"`
CP=`echo $CP | sed 's/ /:/g'`
find ./src -name "*.java" | xargs javac -cp $CP -d $OUT


#./distribution/scjChecker/checkers/binary/javac -proc:only -cp build/moreTests:build/src:build/spec -processor checkers.SCJChecker /Users/plsek/_work/workspace_RT/scj-annotations/moreTests/moreTests/TestVector.java 