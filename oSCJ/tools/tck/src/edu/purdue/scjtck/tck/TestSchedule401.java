package edu.purdue.scjtck.tck;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import javax.realtime.ScopedMemory;
import javax.safetycritical.AperiodicEvent;
import javax.safetycritical.MissionSequencer;

/**
 * Level 2
 * 
 * - AEH, PEH, NHRT are no-heap and non-daemon.
 */
public class TestSchedule401 extends TestCase {

    private volatile boolean[] _check = new boolean[3];
    private final int _AEHCheckIndex = 0;
    private final int _PEHCheckIndex = 1;
    private final int _NHRTCheckIndex = 2;
    private volatile int _done = 0;

    public MissionSequencer getSequencer() {
        return new GeneralSingleMissionSequencer(new GeneralMission() {

            public void initialize() {

                final AperiodicEvent event = new AperiodicEvent(
                        new GeneralAperiodicEventHandler() {

                            public void handleEvent() {

                                if (validateCurrentThread())
                                    fail("AEH should be non-daemon");
                                if (validateCurrentMemory())
                                    fail("AEH should be non-heap");

                                _check[_AEHCheckIndex] = true;

                                if (++_done >= _check.length)
                                    requestSequenceTermination();
                            }
                        });

                new GeneralPeriodicEventHandler() {

                    public void handleEvent() {
                        event.fire();

                        if (validateCurrentThread())
                            fail("PEH should be non-daemon");
                        if (validateCurrentMemory())
                            fail("PEH should be non-heap");

                        _check[_PEHCheckIndex] = true;

                        if (++_done >= _check.length)
                            requestSequenceTermination();
                    }
                };

                new GeneralManagedThread() {
                    @Override
                    public void run() {
                        if (validateCurrentThread())
                            fail("NHRT should be non-daemon");
                        if (validateCurrentMemory())
                            fail("NHRT should be non-heap");

                        _check[_NHRTCheckIndex] = true;

                        if (++_done >= _check.length)
                            requestSequenceTermination();
                    }
                }.start();
            }

            @Override
            protected void cleanup() {
                if (!_check[_AEHCheckIndex])
                    fail("AEH not executed");
                if (!_check[_PEHCheckIndex])
                    fail("PEH not executed");
                if (!_check[_NHRTCheckIndex])
                    fail("NHRT not executed");
                super.cleanup();
            }
        });
    }

    private static boolean validateCurrentThread() {
        return !RealtimeThread.currentRealtimeThread().isDaemon();
    }

    private static boolean validateCurrentMemory() {
        MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
        return (mem instanceof ImmortalMemory) || (mem instanceof ScopedMemory);
    }
}
