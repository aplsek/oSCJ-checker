#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./checkers/binary/javac

# the checker to be used for th e verification
CHECKER=checkers.scjAllowed.SCJAllowedChecker 


# where to look for the SCJ-Checrfdsddssffddssaaffddssaafffddaafddssaaffddssaa class files 
CP=./bin/scjChecker.jar

# directory for: output .class files of the verified classes 
OUTPUT=build/specscj


mkdir build
mkdir $OUTPUT

SRC=../scj/specsrc/



echo ---------------------------------------------------
echo SCJ-Spec Check:

INPUT_SCD=`find $SRC -name "*.java"`

$JAVAC -cp $CP -processor $CHECKER -d $OUTPUT -Awarns $INPUT_SCD



#removing the build files
rm -rf $OUTPUT
mkdir $OUTPUT

