#!/bin/bash

set -e
set -x
cd .. && ./build.sh && cd examples/

../localbin/javac -proc:only -cp ../lib/scj.jar:../lib/scj-checker.jar  -processor checkers.SCJChecker `find ./thruster/src -name "*.java" -not -path "*tmp*" -not -path "*button*"`


