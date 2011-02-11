#!/bin/bash

set -e
set -x
./build.sh

./localbin/checkers/binary/javac -proc:only -cp lib/scj.jar:lib/SCJChecker.jar  -processor checkers.SCJChecker  $1


