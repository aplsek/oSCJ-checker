package edu.purdue.scjtck.tck;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.HighResolutionTime;
import javax.realtime.RelativeTime;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.Level;

/**
 * Level 2
 * 
 * - The real-time clock should be monotonic and non-decreasing
 * 
 * - waitForObject should be allowed on level 2
 * 
 */
public class TestClock600 extends TestCase {

    public MissionSequencer getSequencer() {
        return new GeneralSingleMissionSequencer(new GeneralMission() {

            public void initialize() {
                new MyPeriodicEventHandler();
                new LockNotifier();
            }

            private final MyLock _myLock = new MyLock();

            class MyLock {
                public synchronized void doWaitForObject(RelativeTime t)
                        throws InterruptedException {
                    HighResolutionTime.waitForObject(this, t);
                }

                public synchronized void doNotifyAll() {
                    this.notifyAll();
                }
            }

            class MyPeriodicEventHandler extends GeneralPeriodicEventHandler {
                @Override
                public void handleEvent() {

                    AbsoluteTime preTime = new AbsoluteTime(0, 0), curTime;
                    for (int i = 0; i < _prop._iterations; i++) {
                        curTime = Clock.getRealtimeClock().getTime();
                        if (curTime.getMilliseconds() * 1000000
                                + curTime.getNanoseconds() < preTime
                                .getMilliseconds()
                                * 1000000 + preTime.getNanoseconds()) {
                            fail("The real-time clock should be monotonic and non-decreasing");
                            break;
                        }
                        preTime = curTime;
                    }

                    try {
                        _myLock.doWaitForObject(new RelativeTime(500, 0));
                    } catch (Throwable t) {
                        if (getLevel() == Level.LEVEL_2)
                            fail("HighResolutionTime.waitForObject() should be allowed on level 2");
                    } finally {
                        requestSequenceTermination();
                    }
                }
            }

            class LockNotifier extends GeneralPeriodicEventHandler {
                public void handleEvent() {
                    _myLock.doNotifyAll();
                }
            }
        });
    }
}
