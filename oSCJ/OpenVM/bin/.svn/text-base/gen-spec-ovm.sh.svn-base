#!/bin/sh

usage() {
    cat - >& 2 <<EOF
$2+
usage: $0 {OPTION}* --? {GEN-OVM-OPTION}*
where option is one of
--test=<list>           tests to compile [$tests]
--spec-dir=<directory>  SPECjvm98 directory [$SPEC_DIR]
--applive               force compile with -app-methods-live rather
                        than -ud-reflective-methods=... [false]

Remaining arguments are passed to the Ovm generator program (gen-ovm,
or the value of the GEN_OVM environment variable), and override the
default settings for SPEC compilation.  

By default we compile spec with j2c with full optimization, and use
the fastest garbage collector available with a 256M heap.

This program will create an ovm executable and image file in the
current directory, but as always, the benchmarks must be run in the
SPECjvm98 directory.  Benchmarks are run by the main class
SpecApplication. 
EOF
    exit $1
}

GEN_OVM=${GEN_OVM:-`dirname $0`/gen-ovm}
SPEC_DIR=${SPEC_DIR:-/p/sss/project/SPECjvm98}
tests=check,compress,jess,raytrace,db,javac,mpegaudio,mtrt,jack

engine=j2c
opt=run
model=MostlyCopyingWB-B_Mf_F_H
threadconfigurator=JVM
ioconfigurator=SIGIOSockets_StallingFiles_PollingOther
heapsize=256m

# First, we need to declare each benchmarks use of reflection.  We
# will massage these class names to generate -ud-reflective-classes
# and -ud-reflective-methods.
check_NEWINSTANCE=`cat - <<EOF
		 Lspec/benchmarks/_200_check/Main;
 		 Lspec/benchmarks/_200_check/LoopBounds2;
EOF
`
# NOTE: <foo>_FORNAME is in Class.forName format, rather than the
# internal bytecode format.  Doing translation is sort of tough when
# arrays are taken into account
check_FORNAME=""
compress_NEWINSTANCE="Lspec/benchmarks/_201_compress/Main;"
compress_FORNAME=""
jess_NEWINSTANCE=`cat - <<EOF
		 Lspec/benchmarks/_202_jess/Main;
 		 Lspec/benchmarks/_202_jess/jess/_return;
 		 Lspec/benchmarks/_202_jess/jess/_assert;
 		 Lspec/benchmarks/_202_jess/jess/_retract;
 		 Lspec/benchmarks/_202_jess/jess/_printout;
 		 Lspec/benchmarks/_202_jess/jess/_read;
 		 Lspec/benchmarks/_202_jess/jess/_readline;
 		 Lspec/benchmarks/_202_jess/jess/_gensym_star;
 		 Lspec/benchmarks/_202_jess/jess/_while;
 		 Lspec/benchmarks/_202_jess/jess/_if;
 		 Lspec/benchmarks/_202_jess/jess/_bind;
 		 Lspec/benchmarks/_202_jess/jess/_modify;
 		 Lspec/benchmarks/_202_jess/jess/_and;
 		 Lspec/benchmarks/_202_jess/jess/_or;
 		 Lspec/benchmarks/_202_jess/jess/_not;
 		 Lspec/benchmarks/_202_jess/jess/_eq;
 		 Lspec/benchmarks/_202_jess/jess/_equals;
 		 Lspec/benchmarks/_202_jess/jess/_not_equals;
 		 Lspec/benchmarks/_202_jess/jess/_gt;
 		 Lspec/benchmarks/_202_jess/jess/_lt;
 		 Lspec/benchmarks/_202_jess/jess/_gt_or_eq;
 		 Lspec/benchmarks/_202_jess/jess/_lt_or_eq;
 		 Lspec/benchmarks/_202_jess/jess/_neq;
 		 Lspec/benchmarks/_202_jess/jess/_mod;
 		 Lspec/benchmarks/_202_jess/jess/_plus;
 		 Lspec/benchmarks/_202_jess/jess/_times;
 		 Lspec/benchmarks/_202_jess/jess/_minus;
 		 Lspec/benchmarks/_202_jess/jess/_divide;
 		 Lspec/benchmarks/_202_jess/jess/_sym_cat;
 		 Lspec/benchmarks/_202_jess/jess/_reset;
 		 Lspec/benchmarks/_202_jess/jess/_run;
 		 Lspec/benchmarks/_202_jess/jess/_facts;
 		 Lspec/benchmarks/_202_jess/jess/_rules;
 		 Lspec/benchmarks/_202_jess/jess/_halt;
 		 Lspec/benchmarks/_202_jess/jess/_exit;
 		 Lspec/benchmarks/_202_jess/jess/_clear;
 		 Lspec/benchmarks/_202_jess/jess/_watch;
 		 Lspec/benchmarks/_202_jess/jess/_unwatch;
 		 Lspec/benchmarks/_202_jess/jess/_jess_version_string;
 		 Lspec/benchmarks/_202_jess/jess/_jess_version_number;
 		 Lspec/benchmarks/_202_jess/jess/_load_facts;
 		 Lspec/benchmarks/_202_jess/jess/_save_facts;
 		 Lspec/benchmarks/_202_jess/jess/_assert_string;
 		 Lspec/benchmarks/_202_jess/jess/_undefrule;
EOF
`
jess_FORNAME=""
raytrace_NEWINSTANCE="Lspec/benchmarks/_205_raytrace/Main;"
raytrace_FORNAME=""
db_NEWINSTANCE="Lspec/benchmarks/_209_db/Main;"
db_FORNAME=""
javac_NEWINSTANCE="Lspec/benchmarks/_213_javac/Main;"
javac_FORNAME="[Lspec.benchmarks._213_javac.Constants;"
mpegaudio_NEWINSTANCE="Lspec/benchmarks/_222_mpegaudio/Main;"
mpegaudio_FORNAME=""
mtrt_NEWINSTANCE="Lspec/benchmarks/_227_mtrt/Main;"
mtrt_FORNAME=""
jack_NEWINSTANCE="Lspec/benchmarks/_228_jack/Main;"
jack_FORNAME=""

applive=""
done=""

while [ -z "$done" ]
do
	case "$1"
	in
		--test=*)  tests=`echo $1 | sed 's/--test=//'`; shift;;
		--spec-dir=*) SPEC_DIR=`echo $1 | sed 's/--spec-dir=//'`; shift;;
		--applive) applive=true; shift;;
		--)        done=true; shift;;
		--*)	   usage 1 $0 "unrecognized switch: $1"
		           exit 1;;
		*)         done=true;;
	esac
done

classpath=$SPEC_DIR

# maybe java.net.URL is only used by jack?  I kind of suspect that
# javac uses it too
reflectiveClasses="java.net.URL"
reflectiveMethods="" #"Gspec/benchmarks/_222_mpegaudio/Main; main:([Ljava/lang/String;)V"

for t in `echo $tests | sed 's/,/ /g'`
do
        str="eval echo \"\$${t}_NEWINSTANCE\""
        cls=`$str`
	dotted=`echo "$cls" | sed -e 's/Lspec/spec/' -e 's,/,.,g' -e 's/;//'`
	reflectiveClasses="$reflectiveClasses $dotted"
	ctors=`echo "$cls" | sed 's/$/ <init>:()V/'`
	reflectiveMethods="$reflectiveMethods $ctors"

	str="eval echo \"\$${t}_FORNAME\""
	rc=`$str`
	reflectiveClasses="$reflectiveClasses $rc"	
done

if [ ! -z "$applive" ]
then
    reflectiveMethods=""
fi

cat - <<EOF
$GEN_OVM -main=SpecApplication \\
         -classpath=$classpath \\
    "-ud-reflective-classes=$reflectiveClasses" \\
    "-ud-reflective-methods=$reflectiveMethods" $applive \\
    -engine=$engine \\
    -opt=$opt \\
    -model=$model \\
    -heap-size=$heapsize \\
    -threads=$threadconfigurator \\
    -io=$ioconfigurator \\
    "$@"
EOF

$GEN_OVM -main=SpecApplication \
         -classpath=$classpath \
	 -ud-reflective-classes="$reflectiveClasses" \
	 -ud-reflective-methods="$reflectiveMethods" $applive \
	 -engine=$engine \
         -opt=$opt \
	 -model=$model \
	 -heap-size=$heapsize \
	 -threads=$threadconfigurator \
	 -io=$ioconfigurator \
	 "$@" \
|| usage $? $0 "$GEN_OVM failed"

