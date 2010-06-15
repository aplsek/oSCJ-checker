#!/bin/sh
#
# makes a tarball to release the OVM

whereami=`dirname $0`

date=`date "+%y%m%d"`
filename=OVM-${date}.tgz

tempdir=`mktemp -d -q OVMcheckoutXXXXXX`
if [ $? -ne 0 ] ; then
	echo could not create temporary directory
	exit 1
else
	echo created tmp dir $tempdir
fi

cd $tempdir
cvs -d /p/sss/cvs checkout -r RELEASE_BRANCH_0 OpenVM
rm -rf `find ./OpenVM/ -path "*/\.svn/*" -o -name '\.svn' -type d`

echo creating archive 
mv ./OpenVM "./OVM-$date"
tar czvf ../$filename "./OVM-$date"

echo cleaning up
rm -rf "./OVM-$date"

cd ..

echo removing temporary directory
rm -r $tempdir
