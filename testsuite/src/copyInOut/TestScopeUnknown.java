package copyInOut;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestScopeUnknown extends Mission {

    @Scope("copyInOut.TestLLMission")
    @RunsIn("copyInOut.MyHandler")
    class MyHandler extends PeriodicEventHandler {

        public MyHandler(PriorityParameters priority,
                PeriodicParameters period, StorageParameters storage) {
            super(priority, period, storage);
        }

        @Override
        public void handleAsyncEvent() { }
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Override
    protected void initialize() { }
}
