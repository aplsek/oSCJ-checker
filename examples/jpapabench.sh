#!/bin/bash

set -e
set -x
cd .. && ant jar && cd examples/

rm -rf list

find ./jpapabench/jpaparazzi/jpapabench-core/src -name "*.java" > list
find ./jpapabench/jpaparazzi/jpapabench-core-flightplans/src -name "*.java" >> list
find ./jpapabench/jpaparazzi/jpapabench-scj/src -name "*.java" >> list

../localbin/javac -proc:only -cp ../lib/scj.jar:../lib/scj-checker.jar  -processor checkers.SCJChecker @list


