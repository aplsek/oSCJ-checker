#!/bin/bash

set -e
set -x


TESTSUITE=./testsuite


cd $TESTSUITE && ./clean.sh && ./build.sh

python localbin/runtests.py  --scj-path ../../../../../lib/scj.jar:testsuite/build:build/ --directory ./testsuite/src

