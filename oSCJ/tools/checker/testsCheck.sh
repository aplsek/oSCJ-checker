#!/bin/bash

# location of the JSR-308 Javac distribution
JAVAC=./distribution/scjChecker/checkers/binary/javac


# where to look for the SCJ-Checker class files 
CP=./build/src:./build/tests:

# directory for: output .class files of the verified classes 
OUTPUT=build/tests

mkdir build
mkdir $OUTPUT

#error counter
let "ERR = 0" 


#################################################################################
#################################################################################
################################ TEST 1 #########################################
#################################################################################
#################################################################################



# the checker to be used for the verification
CHECKER=checkers.scjAllowed.SCJAllowedChecker 


TESTNAME="SCJAllowed" 


# a first bunch to test cases:
TESTCASES="\
TestSCJAllowed101 \
TestSCJAllowed102 \
TestSCJAllowed103 \
TestSCJMembers201 \
TestSCJMembers202 \
TestSuppress301 \
TestSuppress302 \
TestOverride401 \
TestOverride402 \
TestGuard501 \
TestNestedClass601 \
TestAllowedProtectedClash \
NoAllowed \
TestNoSCJAllowed \
"




#
echo ---------------------------------------------------
echo $TESTNAME Test:

RUN=1


#################################################################################
# FOR SCJ Allowed test cases
if [ $RUN -eq 1 ]; then

	for TC in $TESTCASES; do
	    INPUT=tests/scjAllowed/$TC.java 
		#$JAVAC -Xstdout ./tests/reportLogs/log$TC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
	    $JAVAC  -implicit:class -Xstdout logTest -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		#$JAVAC  -implicit:class -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		
		
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
		#$JAVAC -Xstdout ./tests/reportLogs/log$TC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		##$JAVAC -Xstdout ./tests/reportLogs/tmp/log$TC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		$JAVAC -implicit:class -Xstdout logTest -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		#$JAVAC -implicit:class -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		
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
	rm logTest
	if [ $ERR -ne 0 ]
	then
	 echo ""
	 echo "*** " $TESTNAME " TEST: Failed, $ERR ERRORS found ***"
	else
	 echo ""
	 echo "*** " $TESTNAME " TEST: Successfull, No errors were found ***"
	fi
	#removing the build files
	rm logTest
	#rm -rf $OUTPUT
	#mkdir $OUTPUT

else
   echo "Test Switched OFF"
fi







#################################################################################
#################################################################################
################################ TEST 2 #########################################
#################################################################################
#################################################################################


TESTNAME=@BlockFree 


#
echo ""
echo "---------------------------------------------------"
echo $TESTNAME "Test:"

RUN=0

# the checker to be used for the verification
CHECKER=checkers.blockfree.BlockFreeChecker

#error counter
let "ERR = 0" 

# a first bunch to test cases:
TESTCASES="\
BlockFreeTest \
"


if [ $RUN -eq 1 ]; then

	
	#################################################################################
	# FOR test cases
	for TC in $TESTCASES; do
	    INPUT=tests/blockfree/$TC.java 
		#$JAVAC -Xstdout ./tests/reportLogs/log$TC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		$JAVAC  -implicit:class -Xstdout logTest -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		
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
	
	rm logTest
	
	if [ $ERR -ne 0 ]
	then
	 echo ""
	 echo "*** " $TESTNAME " TEST: Failed, $ERR ERRORS found ***"
	else
	 echo ""
	 echo "*** " $TESTNAME " TEST: Successfull, No errors were found ***"
	fi
	
	
	#removing the build files
	rm -rf $OUTPUT
	mkdir $OUTPUT
else
	echo "Test Switched OFF."
fi





#################################################################################
#################################################################################
################################ TEST 3 #########################################
#################################################################################
#################################################################################



TESTNAME=@AllocFree 


#
echo ""
echo "---------------------------------------------------"
echo $TESTNAME "Test:"

RUN=0

# the checker to be used for the verification
CHECKER=checkers.allocfree.AllocFreeChecker

#error counter
let "ERR = 0" 

# a first bunch to test cases:
TESTCASES="\
AutoboxAlloc \
ForeachAlloc \
MethodCalls \
MethodCallsAbstract \
MethodCallsInheritance \
MethodCallsInterface \
NewAlloc \
StringAlloc \
CompoundAssignementTest \
MethodParametersTest \
"


if [ $RUN -eq 1 ]; then


	
	
	#################################################################################
	# FOR test cases
	for TC in $TESTCASES; do
	    INPUT=tests/allocfree/$TC.java 
		#$JAVAC -Xstdout ./tests/reportLogs/log$TC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		$JAVAC  -implicit:class -Xstdout logTest -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		
	    
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
	
	rm logTest
	
	
	
	if [ $ERR -ne 0 ]
	then
	 echo ""
	 echo "*** " $TESTNAME " TEST: Failed, $ERR ERRORS found ***"
	else
	 echo ""
	 echo "*** " $TESTNAME " TEST: Successfull, No errors were found ***"
	fi
	
	
	#removing the build files
	rm -rf $OUTPUT
	mkdir $OUTPUT
else
	echo "Test Switched OFF."
fi





#################################################################################
#################################################################################
################################ TEST 4 #########################################
#################################################################################
#################################################################################




TESTNAME="@Scope and @ScopeDef" 


#
echo ""
echo "---------------------------------------------------"
echo $TESTNAME "Test:"

RUN=1

# the checker to be used for the verification
CHECKER=checkers.scope.TwoScopeCheckers

#error counter
let "ERR = 0" 

# a first bunch to test cases:
TESTCASES="\
TestAllocation \
TestConstructor \
TestEscaping \
TestEscaping2 \
TestEscaping3 \
testExecuteInArea \
testFieldScope \
TestInheritance \
TestInvoke \
testNonExistentScope \
TestReturn \
TestRunsInClass \
TestScopeOnMethod \
TestStaticField \
TestUnannotatedRunnable \
TestVariableScope \
TestPrivateMemoryAssignment \
TestSimple \
TestUpcast \
"


if [ $RUN -eq 1 ]; then


	
	
	#################################################################################
	# FOR test cases
	for TC in $TESTCASES; do
	    INPUT=tests/scope/$TC.java 
		#$JAVAC -Xstdout ./tests/reportLogs/log$TC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		$JAVAC -implicit:class -Xstdout logTest -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		#echo "Testing for:" $INPUT
		#$JAVAC  -implicit:class -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		
		
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
	# a second bunch of test cases:
	TESTCASES="\
	RunsInTest \
	"

	# FOR RUNS-IN tests
	for TC in $TESTCASES; do
	    INPUT=tests/runsIn/$TC.java 
		#$JAVAC -Xstdout ./tests/reportLogs/log$TC -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		#$JAVAC -Xstdout logTest -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		#$JAVAC  -implicit:class -cp $CP  -processor  $CHECKER  -d $OUTPUT $INPUT 
		
		
	    #if diff logTest ./tests/reportLogs/log$TC >/dev/null ; then
	    #     echo $INPUT testcase OK. 
	    #else
	    #     let "ERR = ERR + 1" 
	    #     echo 
	    #     echo ERROR for $INPUT:
	    #     diff logTest ./tests/reportLogs/log$TC   
	    #     echo 
	    #fi
	done

	
	
	
	#################################################################################
	#Finally:
	
	rm logTest
	
	
	
	if [ $ERR -ne 0 ]
	then
	 echo ""
	 echo "*** " $TESTNAME " TEST: Failed, $ERR ERRORS found ***"
	else
	 echo ""
	 echo "*** " $TESTNAME " TEST: Successfull, No errors were found ***"
	fi
	
	
	#removing the build files
	#rm -rf $OUTPUT
	#mkdir $OUTPUT
else
	echo "Test Switched OFF."
fi



exit 1


