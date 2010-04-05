#!/bin/bash

BIN_DIR=bin
PROP_DIR=tck/properties/tck
RTSHOME=/opt/jrts2.1
#export LD_LIBRARY_PATH=./lib:$LD_LIBRARY_PATH

#---- Sun RTS Debug ----#
LAUNCHER="$RTSHOME/bin/java_g -XX:+RTSJIgnoreThrowBoundaryError -classpath $CLASSPATH:$BIN_DIR edu.purdue.scjri.Launcher"

#---- IBM  J9 ----#
#LAUNCHER="$RTSHOME/bin/java -Xrealtime -classpath $RTHOME/jre/lib/i386/realtime/jclSC160/realtime.jar:$CLASSPATH:$BIN_DIR edu.purdue.scjri.Launcher"

#---- Sun RTS ----#
#LAUNCHER="java -classpath $CLASSPATH:$BIN_DIR edu.purdue.scjri.Launcher"

TESTCASES="\
HelloWorld
"

for TC in $TESTCASES; do
	$LAUNCHER $TC $PROP_DIR/TestSchedule403.prop
done