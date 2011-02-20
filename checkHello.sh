#!/bin/bash

set -e
set -x

FILES=`find ../../examples/helloworld -name "*.java"`

echo $FILES

./localbin/checkers/binary/javac -proc:only -Awarns -cp lib/scj.jar:lib/scj-checker.jar  -processor checkers.SCJChecker  $FILES


