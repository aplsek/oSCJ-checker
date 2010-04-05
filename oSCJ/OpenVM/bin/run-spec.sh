#!/bin/sh
# Run SPECjvm98 against an ovm executable
# Override OVM and SPECDIR as appropriate

if [ -z $OVM ]
then
    OVM=`pwd`/ovm
fi
if [ -z $SPECDIR ]
then
    SPECDIR=/p/sss/project/SPECjvm98
fi

echo ""
echo ""
echo "Running SPECjvm98 "
echo "     on $OVM "
echo "     in $SPECDIR"
echo ""
echo ""
cd $SPECDIR
$OVM SpecApplication _201_compress
$OVM SpecApplication _202_jess
$OVM SpecApplication _209_db
$OVM SpecApplication _213_javac
$OVM SpecApplication _222_mpegaudio
$OVM SpecApplication _227_mtrt
$OVM SpecApplication _228_jack
