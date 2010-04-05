#! /bin/bash

export PATH=/opt/jdk1.6.0_16/bin/bin:$PATH
OVM_BIN=../bin/bin
SCJDIR=../ri_rtsjBased_j4/src
CDDIR=../minicdx-j4

engine=j2c
#opt=run  
opt=debug 
model=TriPizloPtrStack-FakeScopesWithAreaOf-Bg_Mf_F_H_S
threadconfigurator=RealtimeJVM
ioconfigurator=SelectSockets_PollingOther

# intentionally high
heapsize=1024m
gcthreshold=512m

rm -f OVMMakefile structs.h cstructs.h native_calls.gen img gen-ovm.c \
      empty.s empty.o img.o ld.script ovm_inline.c ovm_inline.h ovm_heap_exports.s ovm_heap_exports.h ovm ovm_heap_imports
      
bootclasspath=`$OVM_BIN/ovm-config -threads=$threadconfigurator -model=$model -io=$ioconfigurator get-string bootclasspath`
echo "bootclasspath is $bootclasspath"

$OVM_BIN/gen-ovm -main=edu.purdue.scj.Launcher \
	-bootclasspath=$bootclasspath \
        -classpath="${SCJDIR}:${CDDIR}/simulator:${CDDIR}/utils:${CDDIR}/cdx" \
	-engine=$engine \
        -opt=$opt \
	-model=$model \
	-heap-size=$heapsize \
	-threads=$threadconfigurator \
	-io=$ioconfigurator \
	-gc-threshold=$gcthreshold \
	-ud-reflective-classes=@rc \
	-ud-reflective-methods=@rm \
        -reflective-class-trace="rclasses" \
        -reflective-method-trace="rmethods" \
	 "$@"


exit 0

        -reflective-class-trace="rclasses" \
        -reflective-method-trace="rmethods" \