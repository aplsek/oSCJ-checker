package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Scope.CALLER;
import javax.safetycritical.annotate.Scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestBadContextChangeCaller extends MissionSequencer {
    MyRun1 runnable1 = new MyRun1();
    MyRun2 runnable2 = new MyRun2();

    @SCJRestricted(INITIALIZATION)
    public TestBadContextChangeCaller() {super(null, null);}

    @RunsIn(CALLER)
    public void method() {
        @DefineScope(name="a",parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory m = (ManagedMemory) ManagedMemory.getMemoryArea(this);
        //## checkers.scope.ScopeChecker.ERR_BAD_CONTEXT_CHANGE_CALLER
        m.enterPrivateMemory(1000, runnable1);
    }

    @RunsIn(CALLER)
    public void method2() {
        @DefineScope(name="a", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory m = (ManagedMemory) ManagedMemory.getMemoryArea(this);
        //## checkers.scope.ScopeChecker.ERR_BAD_CONTEXT_CHANGE_CALLER
        m.executeInArea(runnable2);
    }

    @DefineScope(name="child", parent="a")
    static class MyRun1 implements SCJRunnable {
        @RunsIn("child")
        public void run() {  }
    }

    static class MyRun2 implements SCJRunnable {
        @RunsIn("a")
        public void run() {  }
    }
}
