#!/bin/bash

set -e
set -x

BUILD=./build

rm -rf $BUILD
mkdir $BUILD

find ./src -name "*.java" | xargs javac -cp lib/my-checkers.jar:lib/langtools.jar:lib/scjChecker.jar:lib/build/:../../../../lib/scj.jar:lib/jsr308-all.jar:lib/checkers.jar: -d $BUILD

