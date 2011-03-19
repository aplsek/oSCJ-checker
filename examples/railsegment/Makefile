

ifdef WINDIR
  SEP=\;
  EXT=.exe
  RM=cmd /c del
else
  EXT=
  RM=rm -f
  SEP=:
endif

MYCLASSPATH="..$(SEP)../../specsrc"

all: CommunicationService.class CommunicationsOversight.class \
  CommunicationsQueue.class CypherQueue.class GPSDriver.class \
  MobileCommService.class MobileInterruptHandler.class \
  MobileOversight.class MobileQueue.class ModulatedCommService.class \
  ModulatedInterruptHandler.class ModulatedOversight.class \
  NavigationInfo.class NavigationOversight.class NavigationService.class \
  NavigationServiceSequencer.class SatCommService.class \
  SatCommServiceSequencer.class SatInterruptHandler.class \
  SatOversight.class SatQueue.class SecurityInterruptHandler.class \
  SecurityOversight.class SecurityService.class \
  SecurityServiceSequencer.class TimeServiceSequencer.class \
  TrackModulatedCommServiceSequencer.class TrainControl.class \
  TrainControlApp.class TrainControlSequencer.class \
  TrainControlThread.class TrainMission.class TrainMissionSequencer.class \
  clock/SynchronizedTime.class \
  clock/TimeService.class clock/TimerOversight.class \
  clock/TimerTick.class clock/TrainClock.class


%.class: %.java
	javac -classpath $(MYCLASSPATH) $*.java


clock/%.class: clock/%.java
	javac -classpath $(MYCLASSPATH) clock/%.java


clean:
	rm *.class *.tex; cd clock; rm *.class *.tex