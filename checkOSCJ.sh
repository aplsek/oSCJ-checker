#!/bin/bash

set -e
set -x

FILES=`find ../../scj/ -name "*.java" -not -path "*ovm*" -not -path "*ri_*"`

echo $FILES

./localbin/javac -proc:only -Awarns -cp ../../../../lib/scj.jar:lib/scj-checker.jar  -processor checkers.SCJChecker  $FILES



