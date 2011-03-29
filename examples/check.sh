#!/bin/bash

set -e
set -x
cd .. && ./build.sh && cd examples/

../localbin/javac -proc:only -cp ../localbin/jdk.jar:../lib/scj.jar:../lib/scj-checker.jar:../lib/scj-jdk-spec.jar  -processor checkers.SCJChecker `find ./railsegment -name "*.java"`


