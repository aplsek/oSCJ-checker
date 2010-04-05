#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./checkers/binary/javac

# the checker to be used for the verification
CHECKER=checkers.SCJChecker

# where to look for the SCJ-Checker implementation
CP=./bin/scjChecker.jar

# directory for: output .class files of the verified classes 
OUTPUT=./build/output


mkdir build
mkdir $OUTPUT

# ---------------------------------------------------
#
#
#
# ---------------------------------------------------

if [[ !($1) ]]; then 
    echo 'ERROR: no input parameters'
    echo 'Specify input directory containing source files to be check.'
    exit 1
fi


echo ------------------
echo SCJ-Checker:
echo     version: 0.1
echo        - using only the SCJAllowed Checker
echo ------------------
echo Starting verification:
echo ""


# ---------------------------------------------------

INPUT=`find $1 -name "*.java"`


$JAVAC -cp $CP -processor $CHECKER -d $OUTPUT -Awarns $INPUT


#removing the build files
rm -rf $OUTPUT
mkdir $OUTPUT



