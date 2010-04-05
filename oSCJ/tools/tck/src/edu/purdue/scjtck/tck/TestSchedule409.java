package edu.purdue.scjtck.tck;

import javax.safetycritical.AperiodicEventHandler;
import javax.safetycritical.ExternalEvent;
import javax.safetycritical.InterruptHandler;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Services;

/**
 * Level 1?
 * 
 * This tests "Interrupt Handling" section
 */
public class TestSchedule409 extends TestCase {

    private boolean[] _check = new boolean[3];
    private final int _singalExternalEventCheckIndex = 0;
    private final int _arrayExternalEventCheckIndex = 1;
    private final int _interruptHandlerCheckIndex = 2;

    public MissionSequencer getSequencer() {
        return new GeneralSingleMissionSequencer(new GeneralMission() {

            // TODO: happening string is VM implementation dependent
            private String _happenning = "TBD";
            // TODO: same as above
            private int _interruptID = 0;

            public void initialize() {

                AperiodicEventHandler aeh1 = new GeneralAperiodicEventHandler() {
                    public void handleEvent() {
                        _check[_singalExternalEventCheckIndex] = true;
                        requestSequenceTermination();
                    }
                };

                new ExternalEvent(aeh1, _happenning);

                AperiodicEventHandler aeh2 = new GeneralAperiodicEventHandler() {
                    @Override
                    public void handleEvent() {
                        _check[_arrayExternalEventCheckIndex] = true;
                        requestSequenceTermination();
                    }
                };

                new ExternalEvent(new AperiodicEventHandler[] { aeh2 },
                        _happenning);

                MyInterruptHandler mih = new MyInterruptHandler(_interruptID);

                InterruptHandler.enableGlobalInterrupts();
                InterruptHandler.getInterruptPriority(_interruptID);
                Services.getInterruptPriority(_interruptID);
                Services.registerInterruptHandler(_interruptID, mih);

                new Terminator();
            }

            @Override
            protected void cleanup() {
                if (!_check[_singalExternalEventCheckIndex])
                    fail("Error occurred in ExternalEvent (single handler)");
                if (!_check[_arrayExternalEventCheckIndex])
                    fail("Error occurred in ExternalEvent (multiple handlers)");
                if (!_check[_interruptHandlerCheckIndex])
                    fail("Error occurred in InterruptHandler");
                super.cleanup();
            }

        });
    }

    // TODO: how can we use this class?
    class MyInterruptHandler extends InterruptHandler {

        public MyInterruptHandler(int InterruptID) {
            super(InterruptID);
        }

        public synchronized void handleInterrupt() {
            _check[_interruptHandlerCheckIndex] = true;
        }
    }
}
