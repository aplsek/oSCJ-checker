package scope.scope.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="D", parent=IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestUpcast3 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestUpcast3() {
        super(null, null);
    }

    @SCJAllowed(members = true)
    @Scope("D")
    static class MyMission extends Mission {

        @Override
        @SCJAllowed(SUPPORT)
        public long missionMemorySize() {
            return 0;
        }

        @Override
        @SCJAllowed(SUPPORT)
        @SCJRestricted(INITIALIZATION)
        protected void initialize() {
            MyHandlerRun r = new MyHandlerRun();

            //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
            new RealPEH(r);

            //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
            new RealPEH2(new MyHandlerRun());
        }

    }

    @RunsIn("D")
    public Runnable method() {
        MyHandlerRun r = new MyHandlerRun();

        //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
        return r;
    }

    @SCJAllowed(members=true)
    @Scope("D")
    @DefineScope(name="PEH", parent="D")
    static class RealPEH extends PeriodicEventHandler {

        Runnable run;

        @SCJRestricted(INITIALIZATION)
        public RealPEH(Runnable run) {
            super(null, null, null);
            this.run = run;
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("PEH")
        public void handleAsyncEvent() {
        }

    }

    @SCJAllowed(members=true)
    @Scope("D")
    @DefineScope(name="PEH2", parent="D")
    static class RealPEH2 extends PeriodicEventHandler {

        Runnable run;

        @SCJRestricted(INITIALIZATION)
        public RealPEH2(Runnable run) {
            super(null, null, null);
            this.run = run;
        }

        @Override
        @SCJAllowed(SUPPORT)
        @RunsIn("PEH2")
        public void handleAsyncEvent() {
        }

    }

    @Scope("D")
    static class MyHandlerRun implements Runnable {

        @RunsIn("PEH")
        public void run() { }
    }
}