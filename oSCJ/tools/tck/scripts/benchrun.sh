#!/bin/bash

BIN_DIR=bin
PROP_DIR=properties/bench
DATA_DIR=data
RTSHOME=/usr/local/app/sunrts/jrts2.1
export LD_LIBRARY_PATH=./lib:$LD_LIBRARY_PATH
LAUNCHER="$RTSHOME/bin/java -verbose:gc -classpath $CLASSPATH:$BIN_DIR:$S3SCJ_DIR  javax.safetycritical.S3Launcher"

BENCHES="\
clock.ClockAccuracy \
clock.ClockResolution \
jitter.JitterPEH \
jitter.JitterFrame \
latency.ContextSwitchWaitNotify \
latency.ContextSwitchYield \
latency.DispatchingAEH \
latency.DispatchingNHRT \
latency.EnterExitMemory \
latency.MemAlloc \
latency.MissionLag \
#latency.SyncOverhead \
throughput.FloatingPoint \
throughput.IntegerOps \
throughput.Shifting \
memory.InfraOverhead \
"

for BENCH in $BENCHES; do
        $LAUNCHER s3scj.bench.$BENCH $PROP_DIR/$BENCH.prop > $DATA_DIR/$BENCH.data
done
