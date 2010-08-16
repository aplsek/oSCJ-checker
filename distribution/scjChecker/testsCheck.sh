#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./checkers/binary/javac

# the checker to be used for the verification
CHECKER=checkers.scjAllowed.SCJAllowedChecker 


# where to look for the SCJ-Checker class files 
CP=./bin/scjChecker.jar:./build/tests:./tests


# directory for: output .class files of the verified classes 
OUTPUT=build/tests

mkdir build
mkdir $OUTPUT

#error counter
let "ERR = 0" 

# a first bunch to test cases:
TESTCASES="\
TestSCJAllowed101 \
TestSCJAllowed102 \
TestSCJAllowed103 \
TestSCJMembers201 \
TestSCJMembers201 \
TestSuppress301 \
TestSuppress302 \
TestOverride401 \
TestOverride402 \
TestGuard501 \
TestNestedClass601 \
"




#
echo ---------------------------------------------------
echo @SCJAllowed Test:





#################################################################################
# FOR SCJ Allowed test cases
for TC in $TESTCASES; do
    INPUT=tests/scjAllowed/$TC.java 
	#$JAVAC -Xstdout ./tests/reportLogs/log$TC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
	$JAVAC -implicit:class -Xstdout logTest -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
	#$JAVAC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
	
	#$JAVAC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
	
    if diff logTest ./tests/reportLogs/log$TC >/dev/null ; then
         echo $INPUT testcase OK. 
    else
         let "ERR = ERR + 1" 
         echo 
         echo ERROR for $INPUT:
         diff logTest ./tests/reportLogs/log$TC
         echo 
    fi
done






#################################################################################
# A second bunch of test cases

TESTCASES="\
FakeMemory \
SCJFakeNestedClasses \
FakeSCJMembers \
FakeSCJ \
ManagedEventHandler \
Safelet \
"



# FOR SCJ Allowed test cases
for TC in $TESTCASES; do
    INPUT=tests/javax/safetycritical/$TC.java 
	$JAVAC -implicit:class -Xstdout logTest -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
	
    if diff logTest ./tests/reportLogs/log$TC >/dev/null ; then
         echo $INPUT testcase OK. 
    else
         let "ERR = ERR + 1" 
         echo 
         echo ERROR for $INPUT:
         diff logTest ./tests/reportLogs/log$TC
         echo 

    fi
done





#################################################################################
#Finally:


#removing the build files
rm logTest
rm -rf $OUTPUT
mkdir $OUTPUT


if [ $ERR -ne 0 ]
then
 echo ""
 echo "*** Test Failed, $ERR ERRORS found ***"
else
 echo ""
 echo "*** Test Successfull No errors were found ***"
fi


exit 1


