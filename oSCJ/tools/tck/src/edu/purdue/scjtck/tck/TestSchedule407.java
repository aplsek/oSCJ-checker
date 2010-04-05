package edu.purdue.scjtck.tck;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.AperiodicEvent;
import javax.safetycritical.ManagedThread;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageConfigurationParameters;

/**
 * Level 2
 * 
 * - All no-heap schedulable objects(PEH, AEH, NHRT) supported
 * 
 * - Self suspension within synchronized code allowed;
 * 
 * - wait/notify/notifyAll allowed
 * 
 * - nested mission sequence allowed
 * 
 * - calls to join method are allowed
 */
public class TestSchedule407 extends TestCase {

    private boolean[] _check = new boolean[3];
    private final int _SubNHTRCheckIndex = 0;
    private final int _SubPEHCheckIndex = 1;
    private final int _SubAEHCheckIndex = 2;

    public MissionSequencer getSequencer() {
        return new GeneralSingleMissionSequencer(new GeneralMission() {

            private final int _nNHRTs = 5;
            private volatile int _nFinished;
            private final MyLock _myLock = new MyLock();
            private volatile boolean _notified = false;

            public void initialize() {
                final MySubMission mission = new MySubMission();
                final GeneralSingleMissionSequencer subMissionSequencer = new GeneralSingleMissionSequencer(
                        mission);

                final GeneralManagedThread[] rtthreads = new GeneralManagedThread[_nNHRTs];

                for (int i = 0; i < _nNHRTs; i++) {
                    rtthreads[i] = new GeneralManagedThread() {
                        @Override
                        public void run() {
                            try {
                                _myLock.doWait();

                                if (!_notified) {
                                    _myLock.doNotifyAll();
                                    _notified = true;
                                }
                                _nFinished++;
                            } catch (Throwable t) {
                                fail(t.getMessage());
                            }
                        }
                    };
                    rtthreads[i].start();
                }

                new PeriodicEventHandler(
                        new PriorityParameters(_prop._priority),
                        new PeriodicParameters(new RelativeTime(100, 0),
                                new RelativeTime(Long.MAX_VALUE, 0)),
                        new StorageConfigurationParameters(0, 0, 0),
                        _prop._schedObjMemSize) {
                    public void handleEvent() {

                        // notify the first NHRT that waiting on "monitor",
                        // then it will notify all others
                        try {
                            _myLock.doNotify();
                        } catch (Throwable t) {
                            fail(t.getMessage());
                        }
                        try {
                            for (int i = 0; i < rtthreads.length; i++) {
                                rtthreads[i].join();
                            }
                        } catch (Throwable t) {
                            fail("Error occured in calling Thread.join() - Msg: "
                                    + t.getMessage());
                        }
                        try {
                            // self-suspended code
                            _myLock.aSelfSuspendedSyncMethod();
                        } catch (Throwable t) {
                            fail("Self-suspension should be allowed on Level 2 - Msg: "
                                    + t.getMessage());
                        }
                        try {
                            // launch a sub mission
                            subMissionSequencer.start();

                            int timeout = (int) (_prop._duration / 10);
                            while (!mission._done && timeout-- > 0)
                                Thread.sleep(10);
                        } catch (Throwable t) {
                            fail(t.getMessage());
                        }

                        requestSequenceTermination();
                    }
                };

                this.createSchedObjWithBadArgs();
            }

            @Override
            protected void cleanup() {
                if (_nNHRTs != _nFinished)
                    fail("Error occurred in wait/notify/notifyAll");
                if (!_check[_SubNHTRCheckIndex])
                    fail("Error occurred in nested NHTR");
                if (!_check[_SubPEHCheckIndex])
                    fail("Error occurred in nested PEH");
                if (!_check[_SubAEHCheckIndex])
                    fail("Error occurred in nested AEH");
                super.cleanup();
            }

            private void createSchedObjWithBadArgs() {
                try {
                    new PeriodicEventHandler(null, new PeriodicParameters(null,
                            new RelativeTime(1, 0)),
                            new StorageConfigurationParameters(0, 0, 0), 50) {
                        @Override
                        public void handleEvent() {
                        }
                    };
                    fail("null illegally accepted as PriorityParameters");
                } catch (Throwable t) {
                }
                try {
                    new PeriodicEventHandler(new PriorityParameters(30), null,
                            new StorageConfigurationParameters(0, 0, 0), 50) {
                        @Override
                        public void handleEvent() {
                        }
                    };
                    fail("null illegally accepted as ReleaseParameters");
                } catch (Throwable t) {
                }
                try {
                    new PeriodicEventHandler(
                            new PriorityParameters(30),
                            new PeriodicParameters(null, new RelativeTime(1, 0)),
                            new StorageConfigurationParameters(0, 0, 0), -1) {
                        @Override
                        public void handleEvent() {
                        }
                    };
                    fail("negative memory size illegally accepted");
                } catch (Throwable t) {
                }

                try {
                    new ManagedThread(new PriorityParameters(_prop._priority),
                            new StorageConfigurationParameters(0, 0, 0));
                } catch (Throwable t) {
                    fail(t.getMessage());
                }
            }
        });
    }

    class MyLock {
        public synchronized void doWait() throws InterruptedException {
            this.wait();
        }

        public synchronized void doNotify() {
            this.notify();
        }

        public synchronized void doNotifyAll() {
            this.notifyAll();
        }

        public synchronized void aSelfSuspendedSyncMethod() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }
    }

    class MySubMission extends GeneralMission {

        public boolean _done;

        private volatile int _nFinished;

        @Override
        public void initialize() {
            final AperiodicEvent event = new AperiodicEvent(
                    new GeneralAperiodicEventHandler() {
                        public void handleEvent() {
                            _check[_SubAEHCheckIndex] = true;
                            if (_nFinished >= 2)
                                requestSequenceTermination();
                            else
                                _nFinished++;
                        }
                    });

            new GeneralManagedThread() {
                public void run() {
                    _check[_SubNHTRCheckIndex] = true;
                    if (_nFinished >= 2)
                        requestSequenceTermination();
                    else
                        _nFinished++;
                }
            }.start();

            new GeneralPeriodicEventHandler() {

                @Override
                public void handleEvent() {
                    event.fire();
                    _check[_SubPEHCheckIndex] = true;
                    if (_nFinished >= 2)
                        requestSequenceTermination();
                    else
                        _nFinished++;
                }
            };
        }

        protected void cleanup() {
            _done = true;
        }
    }
}
