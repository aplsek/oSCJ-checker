#!/bin/bash

set -e
set -x


FIJI_HOME="/home/plsek/fiji/fivm/"
SCJFLAGS="--scj --scj-scope-backing 1500k --g-def-immortal-mem 2330k --g-scope-checks no --pollcheck-mode none"   #700 scope, 500 imm
FIJIFLAGS="--max-threads 3 --more-opt"  # -v 1

# rebuild SCJ.jar                 
#SCJ=""
#CWD=`pwd`
#cd $SCJ && make scj.jar && cd $CWD

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

# COMPILE FIJI
$FIJI_HOME/bin/fivmc -o papabench --scj-safelet papabench.scj.PapaBenchSCJLevel0Application $FIJIFLAGS $SCJFLAGS papabench.jar
	
	
# RUN:	
sudo ./papabench | tee papabench.cap



