#! /bin/bash

# to be run from bin/test/build* where OpenVM is at the same level as bin

../../../OpenVM/bin/layout-check-gen.sh < cstructs.h > /tmp/t$$.c
gcc -m32 -I ../../../OpenVM/src/native/common/include/ -I../../src/native/common/include -I. -o /tmp/t$$ /tmp/t$$.c
/tmp/t$$

rm -f /tmp/t$$ /tmp/t$$.c

