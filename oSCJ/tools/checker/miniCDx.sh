#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./distribution/scjChecker/checkers/binary/javac

# where to look for the SCJ-Checker class files 
CP=./build/src:../minicdx/lib/scj.jar:../minicdx/lib/rtsj/rt.jar:../minicdx/lib/rtsj/rt2.jar:../minicdx/cdx:../minicdx/utils:../minicdx/simulator

# directory for: output .class files of the verified classes 
OUTPUT=build/miniCDx

mkdir $OUTPUT

SRC=../minicdx/





#
# 
#
#
#
#
echo ---------------------------------------------------
echo Checking miniCDx:
INPUT=`find $SRC -name "*.java"`

# the checker to be used for the verification
CHECKER=checkers.scope.ScopeChecker


$JAVAC -cp $CP -processor $CHECKER -d $OUTPUT -Awarns  $INPUT




