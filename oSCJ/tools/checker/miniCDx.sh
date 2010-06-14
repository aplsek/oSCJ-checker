#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./distribution/scjChecker/checkers/binary/javac

# where to look for the SCJ-Checker class files 
#CP=./build/src:../minicdx/lib/scj.jar:../minicdx/lib/rtsj/rt.jar:../minicdx/lib/rtsj/rt2.jar:../minicdx/cdx:../minicdx/utils:../minicdx/simulator

# directory for: output .class files of the verified classes 
OUTPUT=build/miniCDx


MINICDX=../miniCDx-J4

SCJHOME=build/spec


# the checker to be used for the verification
CHECKER=checkers.SCJChecker 

#FLAGS=-Adebug


################ CP



#CP=build/src:build/spec:$MINICDX/bin/

CP=build/src:/Users/plsek/_work/workspace_RT/scj-current/bin:$MINICDX/bin/
#CP=build/src:/Users/plsek/_work/workspace_RT/scj-current/bin

#build/src:/Users/plsek/_work/workspace_RT/scj-current/bin:/Users/plsek/_work/workspace_RT/scj-current/miniCDx-J4/bin/





################ INPUT

#INPUT=`find $MINICDX -name "*.java" -not -path "*utils*"`

INPUT=`find $MINICDX -name "*.java" -not -path "*utils*"`


#INPUT=`find $MINICDX -name "TransientDetectorScope*.java" -not -path "*utils*"`

#rm -rf ./build
#mkdir build
#find ./src -name "*.java" | xargs javac -jar lib/checkers.jar -d build/






################ RUN


#$JAVAC -cp $CP -processor $CHECKER -Adebug -d $OUTPUT -Awarns  $INPUT
#echo "$JAVAC -cp $CP -processor $CHECKER -Adebug -Awarns  $INPUT"
$JAVAC -proc:only -cp $CP -processor $CHECKER -Awarns $FLAGS $INPUT






