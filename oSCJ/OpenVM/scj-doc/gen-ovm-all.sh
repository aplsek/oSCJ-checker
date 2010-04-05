#!/bin/bash

# This script is based on gen-ovm.sh script which is written by Tomas Kalibera.
# This script automates all the tasks which is needed to build an ovm image 
# with the minicdx application. Java5, Java6, OVM, SCJ, minicdx and output
# directories should be set according to the build environment before using
# this script.


CWD=`pwd`

# Java5 path. This is needed to compile ovm.
JAVA15=/opt/jdk1.5.0_22/bin

# Java6 path. This is needed to build an ovm image with the application.
JAVA16=/usr/lib/jvm/java-6-sun/bin

# This is the final output directory. This directory is used to store the
# executable and ovm image.
OVMOUTPUTDIR=$CWD/ovm_output

# This directory is the ovm directory.
OVMDIR=/home/harun/ovm-scj/OpenVM

# This is the directory in which built ovm runtime files will be stored. This
# directory will be deleted after build process is completed.
OVMTEMPDIR=$CWD/ovm_temp
OVMBINDIR=$CWD/ovm_temp/bin

# SCJ directory.
SCJDIR=/home/harun/ovm-scj/ri_rtsjBased_j4/bin

# minicdx directory.
CDXDIR=/home/harun/ovm-scj/minicdx-j4




# Create ovm temp directory.
if [ -d $OVMTEMPDIR ]; then
	rm -rf $OVMTEMPDIR
fi

mkdir $OVMTEMPDIR

# Add Java5 path to PATH variable.
export PATH=$JAVA15:$PATH

# Change directory to ovm temp directory and compile and install ovm.
cd $OVMTEMPDIR
$OVMDIR/configure --with-posix-rt-timers
make
cd $CWD

# Delete Java5 path from PATH variable.
PATH=$(echo $PATH | sed 's:$JAVA15\:::')

# Add Java6 path to PATH variable
export PATH=$JAVA16:$PATH


# Create final output directory.
if [ -d $OVMOUTPUTDIR ]; then
	rm -rf $OVMOUTPUTDIR
fi

mkdir $OVMOUTPUTDIR

# Change directory to final output directory.
cd $OVMOUTPUTDIR

# Copy reflection method and class lists to final output directory.
cp $CDXDIR/rm /$CDXDIR/rc .


# Choose the engine.
engine=j2c

# Choose the debug option. Other option is 'run' which is used to optimize ovm.
opt=debug

# Choose the model. Currently this model is know to be working properly.
model=TriPizloPtrStack-FakeScopesWithAreaOf-Bg_Mf_F_H_S

# Choose the thread configurator.
threadconfigurator=RealtimeJVM

# Choose the ioconfigurator.
ioconfigurator=SelectSockets_PollingOther

# Choose heap size and gc threashold values.
heapsize=1024m
gcthreshold=512m

# This is not needed any more because we are deleting the final output
# directory before compilation.
#rm -f OVMMakefile structs.h cstructs.h native_calls.gen img gen-ovm.c \
#      empty.s empty.o img.o ld.script ovm_inline.c ovm_inline.h ovm_heap_exports.s ovm_heap_exports.h ovm ovm_heap_imports

# Get boot classpath.      
bootclasspath=`$OVMBINDIR/ovm-config -threads=$threadconfigurator -model=$model -io=$ioconfigurator get-string bootclasspath`
# echo "bootclasspath is $bootclasspath"

# Build an ovm image with the application.
$OVMBINDIR/gen-ovm -main=edu.purdue.scj.Launcher \
	-bootclasspath=$bootclasspath \
        -classpath="${SCJDIR}:${CDXDIR}/simulator:${CDXDIR}/utils:${CDXDIR}/cdx" \
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

# Return to current working directory.
cd $CWD

# Delete ovm temp directory.
rm -rf $OVMTEMPDIR

# Exit
exit 0


