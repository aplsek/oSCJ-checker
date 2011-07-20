#!/bin/bash

set -e
set -x

ant jar




./localbin/javac -proc:only -cp lib/scj.jar:lib/scj-checker.jar -bootclasspath lib/scj.jar  -processor checkers.SCJChecker $@


