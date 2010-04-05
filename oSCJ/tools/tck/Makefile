
#
#   ri	 -- scj reference implementation
#   tck   -- scj testcases (out-of-date)
#   bench -- scj benchmark (out-of-date)
#   clean -- remove all generated classes, headers, objects, and shared libs
#
 
RI_DIR  := ri/spec/src:ri/s3/src
TCK_DIR := tck/src
JNI_DIR := tck/lib
BIN_DIR := bin
EG_DIR  := example
VPATH   := $(RI_DIR):$(BIN_DIR)

JC      := javac
JFLAGS  := -sourcepath $(RI_DIR) -d $(BIN_DIR) -classpath $(BIN_DIR):lib/rt.jar:lib/rt2.jar

############################    Java Sources    ##############################

JAVAX_REALTIME = \
AbsoluteTime.java              ImmortalMemory.java            ProcessingGroupParameters.java \
AperiodicParameters.java       InaccessibleAreaException.java RealtimeThread.java \
AsyncEvent.java                InterruptHappening.java        RelativeTime.java \
AsyncEventHandler.java         LTMemory.java                  ReleaseParameters.java \
AutonomousHappening.java       MemoryArea.java                Schedulable.java \
BoundAsyncEventHandler.java    MemoryInUseException.java      Scheduler.java \
Clock.java                     MemoryParameters.java          SchedulingParameters.java \
ClockCallBack.java             MemoryScopeException.java      ScopedMemory.java \
ControlledHappening.java       MonitorControl.java            SizeEstimator.java \
EventExaminer.java             NoHeapRealtimeThread.java      ThrowBoundaryError.java \
EventHappening.java            PeriodicParameters.java        Timer.java \
Happening.java                 PeriodicTimer.java             UnknownHappeningException.java \
HighResolutionTime.java        PriorityParameters.java        IllegalAssignmentError.java \
PriorityScheduler.java         IdentityArraySet.java          RealtimeJavaDispatcher.java \
ScopeStack.java


JAVAX_SAFETYCRITICAL = \
AperiodicEvent.java                 Mission.java                        Safelet.java \
AperiodicEventHandler.java          MissionManager.java                 Schedulable.java \
CyclicExecutive.java                MissionMemory.java                  SingleMissionSequencer.java \
CyclicSchedule.java                 MissionSequencer.java               StorageConfigurationParameters.java \
ExternalEvent.java                  PeriodicEventHandler.java           Terminal.java \
InterruptHandler.java               PortalExtender.java                 ThrowBoundaryError.java \
ManagedEventHandler.java            PriorityCeilingEmulation.java       ManagedMemory.java \
PrivateMemory.java

JAVAX_SAFETYCRITICAL_ANNOTATE = \
Allocate.java           Level.java              Pure.java \
BlockFree.java          MemoryAreaEncloses.java SCJAllowed.java \
Immutable.java          MemoryAreaSame.java     SCJProtected.java

EDU_PURDUE_SCJ = \
BackingStoreID.java  VMSupport.java
 
EDU_PURDUE_SCJ_ANNOTATE = \
NoScopeChecks.java

############################    Classes    ##############################

RICLASSES = \
$(JAVAX_REALTIME:%.java=javax/realtime/%.class) \
$(JAVAX_SAFETYCRITICAL:%.java=javax/safetycritical/%.class) \
$(JAVAX_SAFETYCRITICAL_ANNOTATE:%.java=javax/safetycritical/annotate/%.class) \
$(EDU_PURDUE_SCJ:%.java=edu/purdue/scj/%.class) \
$(EDU_PURDUE_SCJ_ANNOTATE:%.java=edu/purdue/scj/annotate/%.class)
	
	
TCKClASSES = 
BENCHCLASSES = 
EGCLASSES = 
	

############################    Targets    ##############################
	
all: ri

ri: $(RICLASSES)

tck: $(TCKClASSES)

%.class: %.java
	$(JC) $(JFLAGS) $<

############################    JNI Stuff (out-of-date)    ##############################

#--------------  For Linux  --------------
CC=gcc
TestJNI900OBJ=TestJNI900.o
TestJNI900LIB=libTestJNI900.so
TestJNI900OBJ_OPTION=-Wall -fPIC -c -I$(RTSHOME)/include -I$(RTSHOME)/include/linux -o $(JNI_DIR)/$(TestJNI900OBJ) 
TestJNI900LIB_OPTION=-shared -W1,-soname,$(JNI_DIR)/$(TestJNI900LIB) -o $(JNI_DIR)/$(TestJNI900LIB)

S3UtilTimeOBJ=S3UtilTime.o
S3UtilTimeLIB=libS3UtilTime.so
S3UtilTimeOBJ_OPTION=-Wall -fPIC -c -I$(RTSHOME)/include -I$(RTSHOME)/include/linux -lrt -o $(JNI_DIR)/$(S3UtilTimeOBJ) 
S3UtilTimeLIB_OPTION=-shared -W1,-soname,$(JNI_DIR)/$(S3UtilTimeLIB) -o $(JNI_DIR)/$(S3UtilTimeLIB)
#-----------------------------------------------------------

#$(JNI_DIR)/$(TestJNI900LIB)

$(JNI_DIR)/$(TestJNI900LIB): $(JNI_DIR)/$(TestJNI900OBJ)
	$(CC) $(TestJNI900LIB_OPTION) $(JNI_DIR)/$(TestJNI900OBJ)

$(JNI_DIR)/$(TestJNI900OBJ): $(JNI_DIR)/TestJNI900.c $(JNI_DIR)/s3scj_tck_TestJNI900.h
	$(CC) $(TestJNI900OBJ_OPTION) $(JNI_DIR)/TestJNI900.c 

$(JNI_DIR)/s3scj_tck_TestJNI900.h: $(BIN_DIR)/edu/purdue/scjtck/tck/TestJNI900.class
	javah -classpath $(BIN_DIR) -d $(JNI_DIR) edu/purdue/scjtck.tck.TestJNI900

#--------------------  S3UtilTime  --------------------

$(JNI_DIR)/$(S3UtilTimeLIB): $(JNI_DIR)/$(S3UtilTimeOBJ)
	$(CC) $(S3UtilTimeLIB_OPTION) $(JNI_DIR)/$(S3UtilTimeOBJ)

$(JNI_DIR)/$(S3UtilTimeOBJ): $(JNI_DIR)/S3UtilTime.c $(JNI_DIR)/s3scj_bench_util_Time.h
	$(CC) $(S3UtilTimeOBJ_OPTION) $(JNI_DIR)/S3UtilTime.c 	
	
$(JNI_DIR)/s3scj_bench_util_Time.h: $(BIN_DIR)/edu/purdue/scjtck/bench/util/Time.class
	javah -classpath $(BIN_DIR) -d $(JNI_DIR) edu/purdue/scjtck.bench.util.Time
	
	
############################    Clean    ##############################
clean:
	find $(BIN_DIR) -name \*.class -print -exec rm -rf {} \; 
	rm $(JNI_DIR)/*.o $(JNI_DIR)/*.so $(JNI_DIR)/*.h
