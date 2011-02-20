#!/bin/bash

set -e
set -x

./localbin/checkers/binary/javac -proc:only -cp lib/scj.jar:lib/scj-checker.jar  -processor checkers.SCJChecker  $1


