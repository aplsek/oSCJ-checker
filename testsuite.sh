#!/bin/bash

set -e
set -x


TESTSUITE=./testsuite

cd $TESTSUITE && ./clean.sh && ./build.sh && cd ..


python localbin/runtests.py  --scj-path ./lib/scj.jar --directory testsuite

