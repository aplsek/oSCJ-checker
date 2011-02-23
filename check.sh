#!/bin/bash

set -e
set -x
./build.sh


./localbin/javac -proc:only -cp lib/scj.jar:lib/scj-checker.jar -Adebug  -processor checkers.SCJChecker  $1


