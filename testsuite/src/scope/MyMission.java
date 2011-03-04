//scope/MyMission.java:60: Cannot assign expression in scope scope.MyHandler to variable in scope scope.MyMission.
//        ARunnable1 aRunnable = new ARunnable1();
//                   ^
//scope/MyMission.java:60: Object allocation in a context (scope.MyHandler) other than its designated scope (scope.MyMission).
//        ARunnable1 aRunnable = new ARunnable1();
//                               ^
//scope/MyMission.java:65: The Runnable class must have a matching @Scope annotation.
//        ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, aRunnable);  // ERROR
//                                                                  ^
//testsuite/src/scope/MyMission.java:75: The Runnable passed into the enterPrivateMemory() call must have a run() method with a @RunsIn annotation.
//        mem.enterPrivateMemory(2000, (Runnable) cRun);                 // OK
//                              ^
//scope/MyMission.java:70: The Runnable passed into the enterPrivateMemory() call must have a run() method with a @RunsIn annotation.
//        mem.enterPrivateMemory(2000, runNull);
//                              ^
//
//5 errors

package scope;


import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission")
public class MyMission extends Mission {

    @Override
    @SCJRestricted(INITIALIZATION)
    protected void initialize() {
        new MyHandler(null, null, null, 0);
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Scope("MyMission")
    @DefineScope(name="scope.MyHandler",parent="MyMission")
    static class MyHandler extends PeriodicEventHandler {

        @SCJRestricted(INITIALIZATION)
        public MyHandler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp, long memSize) {
            super(priority, parameters, scp);
        }

        @Override
        @RunsIn("scope.MyHandler")
        public void handleAsyncEvent() {

            A aObj = new A();
            B bObj = new B(); // OK

            ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();

            RunY aRunnable = new RunY();                // ERROR
            ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000, aRunnable);  // ERROR

            RunZ cRun = new RunZ();
            mem.enterPrivateMemory(2000, cRun);                 // OK
        }

        static class A {
            void bar() { }
        }

        static class B {
            A a;
            A a2 = new A();
            Object o;

            @SCJRestricted(mayAllocate=false)
            void foo(A a) {
                o = a;
                //a.bar();                     // ERROR  (not reported since its commented out
            }
        }

        @Override
        public StorageParameters getThreadConfigurationParameters() {
            return null;
        }
    }

    @Scope("MyMission")
    @DefineScope(name="MyMissionInitA", parent="MyMission")
    static class RunY implements SCJRunnable {
        @Override
        @RunsIn("MyMissionInitA")
        public void run() {
        }
    }

    @Scope("scope.MyHandler")
    @DefineScope(name="MyMissionRunB", parent="scope.MyHandler")
    static class RunZ implements SCJRunnable {
        @Override
        @RunsIn("MyMissionRunB")
        public void run() {
        }
    }
}
