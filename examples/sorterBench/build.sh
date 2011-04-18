#!/bin/bash

set -e
set -x


FIJI_HOME="../../../../../../"
SCJFLAGS="--scj --scj-scope-backing 15000k --g-def-immortal-mem 12330k --pollcheck-mode none"   #700 scope, 500 imm
FIJIFLAGS="--max-threads 3 --more-opt"  # -v 1

# rebuild SCJ.jar                 
SCJ="../../../../scj/ri"
CWD=`pwd`
cd $SCJ && make scj.jar && cd $CWD

# CLEAN-UP
rm -rf build
mkdir build

# COMPILE & JAR
find ./src -name *.java > list
javac -cp $FIJI_HOME/lib/scj.jar -d build/ @list	
cd build/ && find . -name "*.class" | xargs jar cf ../sorterBench.jar && cd ..
rm -rf list

# 

# COMPILE FIJI
$FIJI_HOME/bin/fivmc -o sorterBench --scj-safelet sorter.SorterApp $FIJIFLAGS $SCJFLAGS sorterBench.jar
	
	
# RUN:	
sudo ./sorterBench | tee sorterBench-scj.cap



