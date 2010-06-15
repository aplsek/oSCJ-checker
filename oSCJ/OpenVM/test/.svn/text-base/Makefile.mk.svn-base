
# worthless garbage to make gmake happy 
# (need these because without them there is no way of specifying a space or
# a comma to the subst function.)
comma:= ,
empty:=
space:= $(empty) $(empty)

# by default, test our biggest configurations.
# NOTE: OPT_FLAG can have a space-separated list of optimization flags
ENGINE=j2c
J2C_OPT=debug
#OPT_FLAG=-gc-threshold=43m -heap-size=128m
## seems ok for replicating, but not for brooks: OPT_FLAG=-gc-threshold=240m -heap-size=256m
OPT_FLAG=-gc-threshold=128m -heap-size=256m
#MODEL=MostlyCopyingSplitRegions-B_Mf_F_H
#MODEL=MostlyCopyingWB-B_Mf_F_H
#MODEL=TriPizloConservative-FakeScopesWithAreaOf-Bg_Mf_F_H_S
MODEL=TriPizloPtrStack-FakeScopesWithAreaOf-Bg_Mf_F_H_S
#MODEL=AllCopyingList-B_Mf_F_H_CExceptions
#MODEL=AllCopyingPtrStack-B_Mf_F_H_CExceptions
THREADS=RealtimeJVM
#THREADS=JVM
IO=SelectSockets_PollingOther
TRANS=false
DYNAMIC=false
ASPECT=
ASPECTPATH=
CLASSPATH=$(top_builddir)/src/apps/ovm_apps.jar
OVM:=$(QEMU) ./ovm
REFLECTION=								   \
	Ltest/TestReflection$$MyClass; <init>:()V			   \
	Ltest/TestReflection$$MyClass; <init>:(Ljava/lang/String;)V	   \
	Ltest/TestReflection$$MyClass; <init>:(Ljava/lang/String;I)V	   \
	Ltest/TestReflection$$MyClass; methodA:(Ljava/lang/String;)I	   \
	Ltest/TestReflection$$MyClass; methodA:(Ljava/lang/String;I)I	   \
	Ltest/TestReflection$$MyClass; nonstaticAdd:(II)I		   \
	Gtest/TestReflection$$MyClass; staticAdd:(II)I			   \
	Ltest/jvm/TestFinalization$$Finalizable; <init>:()V		   \
	Ltest/jvm/TestReflection; <init>:(Ltest/jvm/TestBase$$Harness;ZZ)V \
	Ltest/jvm/TestReflection$$PkgConstructor; <init>:()V		   \
	Ltest/jvm/TestReflection$$PvtConstructor; <init>:()V		   \
	Gjava/lang/Class; forName:(Ljava/lang/String;)Ljava/lang/Class;	   \
	Ltest/jvm/TestReflection$$Helper; <init>:(ZZ)V 			   \
	Ltest/jvm/TestReflection$$Helper; setField:(Z)V			   \
	Ltest/jvm/TestReflection; method:(Ltest/jvm/TestReflection$$Helper;I)I \
	Gtest/jvm/TestReflection; staticMethod:(Ltest/jvm/TestReflection$$Helper;I)I

ifneq (,$(findstring RealtimeJVM,$(THREADS)))
MAIN=test.rtjvm.TestSuite
REFLECTION+=							\
	Ltest/rtjvm/TestMemoryAreas$$Finalizable; <init>:()V	\
	Ltest/rtjvm/TestMemoryAreas$$Cell; <init>:(II)V
else
MAIN=test.jvm.TestSuite
endif

ARCH_JIT=$(shell if [ $(OVM_ARCH) = "powerpc" -a `uname` = "Darwin"  -o \
                      $(OVM_ARCH) = "i686"    -a `uname` = "Darwin"  -o \
                      $(OVM_ARCH) = "i686"    -a `uname` = "NetBSD"  -o \
                      $(OVM_ARCH) = "i686"    -a `uname` = "Linux"   ] ;\
                 then echo simplejit					 ;\
                 else echo NOT_AVAIL					 ;\
                 fi)

#
# on the ARM and the SPARC some configurations are currently unsupported for
# j2c and the interpreter. Use J2C_IFAVAILABLE for those configurations only
#
J2C_IFAVAILABLE=$(shell if [ $(OVM_ARCH) = "arm" -o $(OVM_ARCH) = "sparc" ] ;\
                        then echo NOT_AVAIL         ;\
                        else echo j2c               ;\
                        fi)


# On some systems, we may need to pass the bootimage file name as an
# argument to ovm.  make IMG=img will at least allow regressions tests
# to work on such systems
#
# By default the bootimage is not linked in when cross-compiling,
# although --with-link-image can be used to force the desired mechanism
#
IMG:=$(shell $(OVM_CONFIG) get-string image-argument)

RUN_OVM=ulimit -s 10240 -t 3600; $(OVM) $(IMG)

# make regression-test produces minimal results to stdout that can
# easily be processed by our test-runner.
ifeq ($(REGRESS),true)
SHOW=$(shell if [ `uname` = "SunOS" ]      ;\
                  then echo @/usr/ucb/echo ;\
                  else echo @echo          ;\
                  fi)
REDIR=> ../$@ 2>&1
REDIRA=>$(REDIR)

# remove build directory for passing tests
all: check clean-1

else
SHOW=@true
REDIR=
REDIRA=

all: check
endif

# Each invocation of make will build and test a particular
# configuration, storing the results in a file derived from the config
# parameters.  Multiple configurations can be tested by changing these
# 5 variables, as is done in regression-test
check: $(ENGINE)$(subst true,-Trans,$(subst false,,$(TRANS)))$(subst true,-dynamic,$(subst false,,$(DYNAMIC)))$(subst $(space),$(comma),${OPT_FLAG})-$(MODEL)-$(IO)-$(THREADS)

clean-1:
	-rm -rf build-$(ENGINE)$(subst true,-Trans,$(subst false,,$(TRANS)))$(subst true,-dynamic,$(subst false,,$(DYNAMIC)))$(subst $(space),$(comma),${OPT_FLAG})-$(MODEL)-$(IO)-$(THREADS)

clean:
	-rm -rf build-* interpreter-* simplejit-* j2c-*

launch:
	$(MAKE) JAVA=$(top_builddir)/bin/record-eclipse-launch

launches:
	$(MAKE) JAVA=$(top_builddir)/bin/record-eclipse-launch regression-test

NOT_AVAIL-%JVM:
	echo "test $@ skipped (configuration is not supported)"

ifeq ($(DYNAMIC),true)
%JVM: force
	$(SHOW) -n "testing $@ ..."
	$(MKTREE) build-$@
	-rm -f build-$@/*
	cd build-$@; $(GEN_OVM) -regress=true				\
				-engine=$(ENGINE)			\
				$(OPT_FLAG)				\
				-opt=$(J2C_OPT)				\
				-model=$(MODEL)				\
				-threads=$(THREADS)			\
				-io=$(IO)				\
				-transaction=$(TRANS)			\
				$(REDIR)
	cd build-$@; $(RUN_OVM) -doexectests -cp $(CLASSPATH) $(MAIN) $(REDIRA)
	$(SHOW) passed
else
# .DEFAULT matches all kinds of strange targets we don't want, but we can 
# rely on the fact that configuration names always end with JVM.
%JVM: force
	$(SHOW) -n "testing $@ ..."
	$(MKTREE) build-$@
	-rm -f build-$@/*
	cd build-$@; $(GEN_OVM) -regress=true				\
				-engine=$(ENGINE)			\
				$(OPT_FLAG)				\
				-opt=$(J2C_OPT)				\
				-model=$(MODEL)				\
				-threads=$(THREADS)			\
				-io=$(IO)				\
				-transaction=$(TRANS)			\
				-classpath=$(CLASSPATH)			\
				-main=$(MAIN)				\
				-ud-reflective-methods='$(REFLECTION)'	\
				$(REDIR)
	cd build-$@; $(RUN_OVM) -doexectests $(REDIRA)
	$(SHOW) passed
endif

TEST=@-echo; time $(MAKE) -s REGRESS=true

# More thorough tests.
#
# There are various permutations of engine, threading, model and IO:
#  engine:    interpreter, simplejit or j2c
#  threading: JVM, real-time JVM, real-time JLThread (allows for multi-threaded
#                                                     ED tests)
#  model:  various combinations of allocators/GC, monitor support and object
#          layout
#  IO:  Various permutations of select() based, SIGIO based and polling based
#       I/O
# The last 2 tests run j2c on our largest configuration with optimization 
# turned up and again with optimization turned off.  
# 
# We try to avoid using precise garbage collectors with the interpreter
# because interpreter stack walking is an order of magnitude slower than
# it should be.
#
# RealtimeJLThread and AllCopyingCounter are not presently supported
# on the ARM, but this is likely to be fixed in the near future.
# AllCopyingThunk, however, will probably never be available on the ARM.
#
regression-test:
	$(TEST) ENGINE=interpreter		    \
		MODEL=MostlyCopyingSplitRegions-B_M_F_H \
		THREADS=RealtimeJVM		    \
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=j2c			   \
		MODEL=MostlyCopyingSplitRegions-B_Mf_F_H \
		THREADS=RealtimeJVM		   \
		TRANS=false			   \
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=j2c			   \
		MODEL=MostlyCopyingSplitRegions-B_Mf_F_H \
		THREADS=RealtimeJVM		   \
		TRANS=false			   \
		IO=SIGIOSockets_PollingOther
	$(TEST) ENGINE=j2c			   \
		MODEL=MostlyCopyingSplitRegions-B_Mf_F_H \
		THREADS=RealtimeJVM		   \
		TRANS=false			   \
		IO=SelectSockets_StallingFiles_PollingOther
	$(TEST) ENGINE=$(J2C_IFAVAILABLE)				\
		MODEL=AllCopyingCounter-B_Mf_F_H		\
		THREADS=JVM					\
		IO=SelectSockets_PollingOther                   \
		TRANS=false
	$(TEST) ENGINE=j2c					\
		MODEL=AllCopyingList-B_Mf_F_H			\
		THREADS=JVM					\
		IO=SelectSockets_PollingOther                   \
		TRANS=false
	$(TEST) ENGINE=j2c					\
		MODEL=AllCopyingPtrStack-B_Mf_F_H		\
		THREADS=JVM					\
		IO=SelectSockets_PollingOther                   \
		TRANS=false
	$(TEST) ENGINE=$(J2C_IFAVAILABLE)			\
		MODEL=AllCopyingThunk-B_Mf_F_H			\
		THREADS=JVM					\
		IO=SelectSockets_PollingOther                   \
		TRANS=false
	$(TEST) OPT_FLAG=-opt=none		    \
		MODEL=MostlyCopyingSplitRegions-B_M_F_H  \
		THREADS=RealtimeJVM		    \
		IO=SelectSockets_PollingOther       \
		TRANS=false
	$(TEST) THREADS=RealtimeJVM		    \
		MODEL=MostlyCopyingSplitRegions-B_Mf_F_H \
		OPT_FLAG=-opt=run		    \
		IO=SelectSockets_PollingOther       \
		TRANS=false
	$(TEST) THREADS=JVM			\
		MODEL=MostlyCopyingWB-B_Mf_F_H 	\
	        OPT_FLAG=-opt=run		\
		IO=SelectSockets_PollingOther   \
		TRANS=false
	$(TEST) OPT_FLAG=-opt=none		    \
		MODEL=MostlyCopyingSplitRegions-B_M_F_H  \
		THREADS=RealtimeJVM		    \
		IO=SelectSockets_PollingOther       \
		TRANS=true
	$(TEST) THREADS=RealtimeJVM		    \
		MODEL=MostlyCopyingSplitRegions-B_Mf_F_H \
		OPT_FLAG=-opt=run		    \
		IO=SIGIOSockets_PollingOther       \
		TRANS=true
	$(TEST) THREADS=JVM			\
		MODEL=MostlyCopyingWB-B_Mf_F_H 	\
	        OPT_FLAG=-opt=run		\
		IO=SelectSockets_StallingFiles_PollingOther   \
		TRANS=true
	$(TEST) MODEL=TriPizloConservative-Bg_Mf	\
		THREADS=JVM				\
		IO=SelectSockets_PollingOther
	$(TEST) MODEL=TriPizloConservative-FakeScopesWithAreaOf-Bg_Mf_S	\
		THREADS=RealtimeJVM					\
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=$(ARCH_JIT)		   \
		MODEL=MostlyCopyingSplitRegions-B_Mf_F_H \
		THREADS=RealtimeJVM		   \
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=$(ARCH_JIT)		   \
		OPT_FLAG=-simplejitopt 		   \
		MODEL=MostlyCopyingSplitRegions-B_Mf_F_H \
		THREADS=RealtimeJVM		   \
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=$(ARCH_JIT)		   \
		DYNAMIC=true	 		   \
		MODEL=MostlyCopyingSplitRegions-B_Mf_F_H \
		THREADS=RealtimeJVM		   \
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=$(ARCH_JIT)		   \
		MODEL=MostlyCopyingWB-B_Mf_F_H	   \
		THREADS=JVM			   \
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=$(ARCH_JIT)		   \
		OPT_FLAG=-simplejitopt 		   \
		MODEL=MostlyCopyingWB-B_Mf_F_H 	   \
		THREADS=JVM			   \
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=$(ARCH_JIT)		   \
		DYNAMIC=true	 		   \
		MODEL=MostlyCopyingWB-B_Mf_F_H 	   \
		THREADS=JVM			   \
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=$(ARCH_JIT)			\
		MODEL=TriPizloConservative-Bg_Mf	\
		THREADS=JVM				\
		IO=SelectSockets_PollingOther
	$(TEST) ENGINE=$(ARCH_JIT)					\
		MODEL=TriPizloConservative-FakeScopesWithAreaOf-Bg_Mf_S	\
		THREADS=RealtimeJVM					\
		IO=SelectSockets_PollingOther
