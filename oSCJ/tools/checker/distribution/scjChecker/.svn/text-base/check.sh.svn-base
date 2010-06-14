#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./checkers/binary/javac

# directory for: output .class files of the verified classes 
OUTPUT=build/

SCJHOME=build/spec


# the checker to be used for the verification
CHECKER=checkers.SCJChecker 

#FLAGS=-Adebug

################################################################################################################################


################ INPUT

if [ $# -ne 1 ]; then
        echo "Provide path to the source directory that you want to check!"
fi;


MINICDX=$1


INPUT=`find $MINICDX -name "*.java" 


################ CP

CP=./lib/oSCJ-OVM.jar:./lib/SCJchecker.jar:$MINICDX/bin/




################ RUN

mkdir $OUTPUT

$JAVAC -proc:only -cp $CP -processor $CHECKER -Awarns $FLAGS $INPUT






