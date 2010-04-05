package edu.purdue.scjtck.tck;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;

/**
 * Level 1
 * 
 * - missions can be executed sequentially
 */
public class TestSchedule400 extends TestCase {

    private final int _nMissions = 3;
    private long[] _timeline = new long[_nMissions * 2];
    private int _missionCounter;

    public MissionSequencer getSequencer() {
        return new MyMissionSequencer();
    }

    class MyMissionSequencer extends GeneralMissionSequencer {

        private Mission[] _missions;
        private int _counter;

        public MyMissionSequencer() {
            _missions = new Mission[_nMissions + 1];

            for (int i = 0; i < _nMissions; i++)
                _missions[i] = new MyMission();
            _missions[_nMissions] = new FinalMission();
        }

        @Override
        protected Mission getInitialMission() {
            return _missions[_counter++];
        }

        @Override
        protected Mission getNextMission() {
            return _counter < _nMissions + 1 ? _missions[_counter++] : null;
        }
    }

    class MyMission extends GeneralMission {

        @Override
        public void initialize() {
            /* record the mission start time */
            _timeline[_missionCounter++] = getCurrentTimeInNano();

            new GeneralPeriodicEventHandler() {
                @Override
                public void handleEvent() {
                    doWork();
                    requestTermination();
                }
            };
        }

        @Override
        protected void cleanup() {
            /* record the mission finish time */
            _timeline[_missionCounter++] = getCurrentTimeInNano();
        }

        private void doWork() {
            /* kill some time */
            for (int i = 0; i < 50000; i++)
                ;
        }

        private long getCurrentTimeInNano() {
            AbsoluteTime time = Clock.getRealtimeClock().getTime();
            long nanos = time.getMilliseconds() * 1000000
                    + time.getNanoseconds();
            if (nanos < 0)
                nanos = Long.MAX_VALUE;

            return nanos;
        }
    }

    class FinalMission extends GeneralMission {
        @Override
        public void initialize() {

            new GeneralPeriodicEventHandler() {
                @Override
                public void handleEvent() {
                    if (_missionCounter != _nMissions * 2)
                        fail("Failed to launch all missions");

                    for (int i = 0; i < _nMissions * 2 - 1; i++)
                        if (_timeline[i] > _timeline[i + 1])
                            fail("Mission timelines overlap");

                    requestSequenceTermination();
                }
            };
        }
    }
}
