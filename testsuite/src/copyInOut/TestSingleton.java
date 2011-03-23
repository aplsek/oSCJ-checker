package copyInOut;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

class MyBigInteger {

    public int value;

    public static final MyBigInteger ZERO = new MyBigInteger(0);

    public MyBigInteger(int value) {
        this.value = value;
    }

    public MyBigInteger(MyBigInteger bI) {
        this.value = bI.value;
    }

    public MyBigInteger add(MyBigInteger bI) {
        return ZERO; // ERROR: returns a singleton!!!!
    }

    public MyBigInteger add2(MyBigInteger bI) {
        boolean bool = true;
        if (bool)
            return bI; // OK
        else
            return ZERO; // ERROR: returns a singleton!!!!
    }

    public MyBigInteger add4(MyBigInteger bI) {
        return new MyBigInteger(ZERO); // OK, copying into the current scope
    }
}

@Scope("copyInOut.TestSingleton")
public class TestSingleton extends Mission {

    @Scope("copyInOut.TestSingleton")
    @RunsIn("copyInOut.MyHandler")
    class MyHandler extends PeriodicEventHandler {

        public MyHandler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp,
                long memSize) {
            super(priority, parameters, scp);
        }

        @Override
        public void handleAsyncEvent() {
            MyBigInteger bI = new MyBigInteger(0); // OK
            MyBigInteger bI2 = new MyBigInteger(0); // OK
            MyBigInteger bI3 = bI.add(bI2); // OK ??
        }
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Override
    protected void initialize() { }
}
