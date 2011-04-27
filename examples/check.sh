#!/bin/bash

set -e
set -x
cd .. && ant jar && cd examples/

../localbin/javac -proc:only -cp ../lib/scj.jar:../lib/scj-checker.jar  -processor checkers.SCJChecker "$@" `/usr/bin/find ./railsegment -name "*.java"`
