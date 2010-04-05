#! /bin/bash

JBB_DIR=${JBB_DIR:-/home/kalibera/SPECjbb05}
#OVM_BIN=${GEN_OVM:-`dirname $0`}
OVM_BIN=${GEN_OVM:-/home/kalibera/sj/again/svn1/bin/bin}

engine=j2c
#opt=run  
opt=debug 
#model=MostlyCopyingWB-B_Mf_F_H
#model=TriPizloConservative-FakeScopesWithAreaOf-Bg_Mf_F_H_S
model=TriPizloPtrStack-FakeScopesWithAreaOf-Bg_Mf_F_H_S
#threadconfigurator=JVM
threadconfigurator=RealtimeJVM
#ioconfigurator=SIGIOSockets_StallingFiles_PollingOther
ioconfigurator=SelectSockets_PollingOther
heapsize=1024m
gcthreshold=768m

rm -rf build
bootclasspath=`$OVM_BIN/ovm-config -threads=$threadconfigurator -model=$model -io=$ioconfigurator get-string bootclasspath`
ant -Dbootclasspath=$bootclasspath -Dbuilddir=`pwd`/build -f $JBB_DIR/build.xml compile

$OVM_BIN/gen-ovm -main=spec.jbb.JBBmain \
         -classpath=build/tmp \
	 -ud-reflective-classes=@$JBB_DIR/rc \
	 -ud-reflective-methods=@$JBB_DIR/rm \
	 -engine=$engine \
         -opt=$opt \
	 -model=$model \
	 -heap-size=$heapsize \
	 -threads=$threadconfigurator \
	 -io=$ioconfigurator \
	  -gc-threshold=$gcthreshold \
	 "$@" 

#	-reflective-class-trace="rclasses" \
#	-reflective-method-trace="rmethods" \

cp $JBB_DIR/SPEC*props $JBB_DIR/logging.properties .
cp -R $JBB_DIR/xml .
