#!/bin/bash

set -e
set -x

ant jar




./localbin/javac -proc:only -cp lib/spec/scj.jar:lib/scj-checker.jar  -processor checkers.SCJChecker $@


