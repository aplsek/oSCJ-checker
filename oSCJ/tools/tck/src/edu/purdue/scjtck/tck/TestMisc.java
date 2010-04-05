package edu.purdue.scjtck.tck;

import javax.realtime.RelativeTime;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Services;
import javax.safetycritical.Terminal;

/**
 * Level 2
 * 
 * Testing the functionality of Services & Terminal class
 */
public class TestMisc extends TestCase {
    private MissionSequencer _sequencer = new GeneralSingleMissionSequencer(
            new GeneralMission() {

                volatile int _ready;
                volatile int _done;

                public void initialize() {

                    final RelativeTime delay = new RelativeTime(
                            _prop._duration, 0);

                    final Thread sleep = new GeneralManagedThread() {
                        @Override
                        public void run() {
                            try {
                                _ready++;
                                Services.sleep(delay);
                                fail("Error occurred in javax.safetycritical.Services.sleep(RelativeTime)");
                            } catch (InterruptedException e) {
                                // expected
                            }
                            _done++;
                        }
                    };

                    final Thread sleepBusy = new GeneralManagedThread() {
                        @Override
                        public void run() {
                            try {
                                _ready++;
                                Services.sleep(delay, true);
                                fail("Error occurred in javax.safetycritical.Services.sleep(RelativeTime, boolean true)");
                            } catch (InterruptedException e) {
                                // expected
                            }
                            _done++;
                        }
                    };
                    final Thread sleepNonBusy = new GeneralManagedThread() {
                        @Override
                        public void run() {
                            try {
                                _ready++;
                                Services.sleep(delay, false);
                                fail("Error occurred in javax.safetycritical.Services.sleep(RelativeTime, boolean false)");
                            } catch (InterruptedException e) {
                                // expected
                            }
                            _done++;
                        }
                    };
                    final Thread sleepNonInt = new GeneralManagedThread() {
                        @Override
                        public void run() {
                            _ready++;
                            try {
                                Services.sleepNonInterruptable(delay);
                            } catch (Throwable t) {
                                fail("Error occurred in javax.safetycritical.Services.sleepNonInterruptable(RelativeTime)");
                            }
                            _done++;

                        }
                    };
                    final Thread sleepNonIntBusy = new GeneralManagedThread() {
                        @Override
                        public void run() {
                            _ready++;
                            try {
                                Services.sleepNonInterruptable(delay, true);
                            } catch (Throwable t) {
                                fail("Error occurred in javax.safetycritical.Services.sleepNonInterruptable(RelativeTime, boolean true)");
                            }
                            _done++;
                        }
                    };
                    final Thread sleepNonIntNonBusy = new GeneralManagedThread() {
                        @Override
                        public void run() {
                            _ready++;
                            try {
                                Services.sleepNonInterruptable(delay, false);
                            } catch (Throwable t) {
                                fail("Error occurred in javax.safetycritical.Services.sleepNonInterruptable(RelativeTime, boolean false)");
                            }
                            _done++;
                        }
                    };

                    sleep.start();
                    sleepBusy.start();
                    sleepNonBusy.start();
                    sleepNonInt.start();
                    sleepNonIntBusy.start();
                    sleepNonIntNonBusy.start();

                    new GeneralPeriodicEventHandler() {
                        public void handleEvent() {
                            // Test Terminal
                            Terminal
                                    .getTerminal()
                                    .write(
                                            "Testing Unicode print (you should see an "
                                                    + "accented e and parallel lines): ");
                            Terminal.getTerminal().writeln(
                                    (char) 2405 + " " + (char) 233);

                            // Test sleep function family in Services

                            try {
                                long timeout = 1000000;

                                while (_ready < 6 && timeout-- > 0)
                                    Thread.yield();

                                if (timeout <= 0)
                                    fail("Time out in TestMisc");
                                if (_done > 0)
                                    fail("Error occurred in javax.safetycritical.Services.sleep family (threads not sleep)");

                                sleep.interrupt();
                                sleepBusy.interrupt();
                                sleepNonBusy.interrupt();
                                sleepNonInt.interrupt();
                                sleepNonIntBusy.interrupt();
                                sleepNonIntNonBusy.interrupt();

                                timeout = 1000000;

                                while (_done < 6 && timeout-- > 0)
                                    Thread.yield();

                                if (timeout <= 0)
                                    fail("Time out in TestMisc");

                            } catch (Throwable t) {
                                fail(t.getMessage());
                            } finally {
                                _sequencer.requestTermination();
                                requestSequenceTermination();
                            }
                        }
                    };
                }
            });

    public MissionSequencer getSequencer() {
        return _sequencer;
    }
}
