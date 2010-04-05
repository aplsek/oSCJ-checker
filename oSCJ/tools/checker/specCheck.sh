#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./distribution/scjChecker/checkers/binary/javac

# the checker to be used for th e verification
CHECKER=checkers.scjAllowed.SCJAllowedChecker 



# where to look for the SCJ-Checker class files 
CP=./build/src:./build/tests:./build/scj-src:./build/tck:

# directory for: output .class files of the verified classes 
OUTPUT=build/specscj

mkdir build
mkdir $OUTPUT

SRC=../jsr302/scj/specsrc/



echo ---------------------------------------------------
echo SCJ-Spec Check:

INPUT_SCD=`find $SRC -name "*.java"`

$JAVAC -cp $CP -processor $CHECKER -d $OUTPUT -Awarns $INPUT_SCD






#removing the build files
rm -rf $OUTPUT
mkdir $OUTPUT

