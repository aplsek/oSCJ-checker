#!/bin/bash

BIN_DIR=../bin
INSTR_DIR=instrbin
PROP_DIR=../properties/tck

#export LD_LIBRARY_PATH=../lib:$LD_LIBRARY_PATH
LAUNCHER="java -classpath lib/emma.jar:$INSTR_DIR:$BIN_DIR:$CLASSPATH javax.safetycritical.S3Launcher"

TESTCASES="\
TestSchedule407 \
"

for TC in $TESTCASES; do
	$LAUNCHER s3scj.tck.$TC $PROP_DIR/$TC.prop
done

