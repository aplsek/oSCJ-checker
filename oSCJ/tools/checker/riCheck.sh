#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./distribution/scjChecker/checkers/binary/javac

# the checker to be used for the verification
CHECKER=checkers.SCJChecker

# where to look for the SCJ-Checker class files 
#CP=./build/src:./build/tests:./build/scj-src:./build/tck:./lib/rtsj/rt.jar:./lib/rtsj/rt2.jar


CP=./build/src:./lib/rtsj/rt.jar:./lib/rtsj/rt2.jar
#CP=./build/src:../jsr-302-Spec/bin/javax/realtime/:

#../jsr-302-Spec/bin/javax/realtime/

# directory for: output .class files of the verified classes 
OUTPUT=build/tests

echo Initialization.
mkdir build
mkdir $OUTPUT

RI_SRC=../jsr302/RI/src/








echo ---------------------------------------------------
#javac -cp scj-annotations/lib/rtsj/rt2.jar:scj-annotations/build/src -processor checkers.scjAllowed.SCJAllowedChecker `find scj-annotations/scj-src -name "*.java"`

INPUT=`find $RI_SRC -name "*.java" ` 
CHECKER_SCD=checkers.scjAllowed.SCJAllowedChecker
echo RI Check:
$JAVAC -classpath $CP -processor $CHECKER_SCD -d $OUTPUT -Awarns $INPUT




#removing the build files
rm -rf $OUTPUT
mkdir $OUTPUT


