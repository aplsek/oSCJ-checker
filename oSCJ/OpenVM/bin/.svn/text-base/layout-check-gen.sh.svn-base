#! /bin/bash

# feed with cstructs.h 

#
# run from build directory
#
# gcc -I ../../../OpenVM/src/native/common/include/ -I../../src/native/common/include -I. -o t t.c 
#


cat <<EOF

#include<stdio.h>
#include<stddef.h>

#include "ctypes.h"

int main(int argc, char **argv) {

EOF
 

grep '^/\* CHK' | cut -d' ' -f3,4 | {

N=0
while read STR FLD ; do
	N=`expr $N + 1`
	DSTR=str${N}
	echo "    struct $STR $DSTR;"
	echo "    printf(\"$STR $FLD offset %d size %d strsize %d\\n\", offsetof(struct $STR,$FLD), sizeof($DSTR.$FLD), sizeof($DSTR));"
done	

cat <<EOF
}

EOF
}

