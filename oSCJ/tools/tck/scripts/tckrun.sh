#!/bin/bash

BIN_DIR=../../bin
PROP_DIR=../properties/tck
RTSHOME=/opt/jrts2.1
#export LD_LIBRARY_PATH=./lib:$LD_LIBRARY_PATH

#---- Sun RTS Debug ----#
LAUNCHER="$RTSHOME/bin/java_g -XX:+RTSJIgnoreThrowBoundaryError -classpath $CLASSPATH:$BIN_DIR edu.purdue.scjri.Launcher"

#---- IBM  J9 ----#
#LAUNCHER="$RTSHOME/bin/java -Xrealtime -classpath $RTHOME/jre/lib/i386/realtime/jclSC160/realtime.jar:$CLASSPATH:$BIN_DIR edu.purdue.scjri.Launcher"

#---- Sun RTS ----#
#LAUNCHER="java -classpath $CLASSPATH:$BIN_DIR edu.purdue.scjri.Launcher"

TESTCASES="\
TestSchedule400 \
TestSchedule401 \
TestSchedule402 \
TestSchedule403 \
TestSchedule404 \
TestSchedule405 \
TestSchedule406 \
TestSchedule407 \
TestSchedule408 \
TestSchedule409 \
TestMemory501 \
TestMemory502 \
TestMemory504 \
TestClock600 \
TestException000 \
TestMisc\
"

for TC in $TESTCASES; do
	$LAUNCHER edu.purdue.scjtck.tck.$TC $PROP_DIR/$TC.prop
done

#TestSchedule400 \
#TestSchedule401 \
#TestSchedule402 \
#TestSchedule403 \
#TestSchedule404 \
#TestSchedule405 \
#TestSchedule406 \
#TestSchedule407 \
#TestSchedule408 \
#TestSchedule409 \
#TestMemory501 \
#TestMemory502 \
#TestMemory504 \
#TestClock600 \
#TestJNI900 \
#TestException000 \
#TestMisc\