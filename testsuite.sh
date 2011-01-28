#!/bin/bash

#set -e
#set -x

# ReCompile the Checker
./build.sh


TESTSUITE=./testsuite
cd $TESTSUITE && ./clean.sh && ./build.sh && cd ..

if [ $# -eq 0 ]
then
    DIRECTORY="testsuite"
else
    DIRECTORY=$1
fi

python localbin/runtests.py  --scj-path ./lib/scj.jar --directory $DIRECTORY

