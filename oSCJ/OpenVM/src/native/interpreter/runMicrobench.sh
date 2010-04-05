#!/bin/sh

FILENAME="sql.data"
#./interpreter img >& sql.data
./fast img >& sql.data
echo "UPDATE benchmarks SET vm = \"OVM\" where vm = \"CurrentVM\";" >> $FILENAME
