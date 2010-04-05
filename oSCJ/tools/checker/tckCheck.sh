#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./distribution/scjChecker/checkers/binary/javac

# the checker to be used for the verification
CHECKER=checkers.SCJChecker

# where to look for the SCJ-Checker class files 
CP=./build/src:./build/tests:./build/scj-src:./build/tck:./lib/rtsj/rt.jar:./lib/rtsj/rt2.jar


# directory for: output .class files of the verified classes 
OUTPUT=build/tck

mkdir build
mkdir $OUTPUT

SRC=../scj/tck/src/





#
#
#
#
#
#
echo ---------------------------------------------------
echo Checking TCK:
INPUT=`find $SRC -name "*.java"`

CHECKER=checkers.scjAllowed.SCJAllowedChecker

$JAVAC -cp $CP -processor $CHECKER -d $OUTPUT -Awarns  $INPUT




