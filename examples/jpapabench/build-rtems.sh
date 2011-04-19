#!/bin/bash

set -e
set -x


FIJI_HOME="/home/plsek/fiji/fivm/"
SCJFLAGS="--scj --scj-scope-backing 13500k --g-def-immortal-mem 12330k --g-scope-checks no --pollcheck-mode none"   #700 scope, 500 imm
FIJIFLAGS="--max-threads 3 --more-opt"  # -v 1
RTEMSFLAGS="--target sparc-rtems4.9"

# rebuild SCJ.jar                 
SCJ="../../../../scj/ri"
CWD=`pwd`
cd $SCJ && make scj.jar && cd $CWD

# CLEAN-UP
rm -rf build
mkdir build

# COMPILE & JAR
find ./jpapabench-jpaparazzi/jpapabench-core/src -name "*.java" > list
find ./jpapabench-jpaparazzi/jpapabench-core-flightplans/src -name "*.java" >> list
find ./jpapabench-jpaparazzi/jpapabench-scj/src -name "*.java" -not -name "*Level1*" >> list
javac -cp $FIJI_HOME/lib/scj.jar -d build/ @list	
cd build/ && find . -name "*.class" | xargs jar cf ../papabench.jar && cd ..
rm -rf list

# 
SAFELET=papabench.scj.PapaBenchSCJLevel0Application
#SAFELET=papabench.scj.HelloWorld

# COMPILE FIJI
$FIJI_HOME/bin/fivmc -o papabench-rtems --scj-safelet $SAFELET $RTEMSFLAGS $FIJIFLAGS $SCJFLAGS papabench.jar
	
	
# RUN:	
#sudo ./papabench | tee papabench.cap



