package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

import static javax.safetycritical.annotate.Scope.CALLER;
import javax.safetycritical.annotate.Scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;

@SCJAllowed(members = true)
@Scope("a")
public abstract class TestBadContextChangeCaller {
    MyRun1 runnable1 = new MyRun1();
    MyRun2 runnable2 = new MyRun2();

    @DefineScope(name="a", parent=IMMORTAL)
    @Scope(IMMORTAL)
    @SCJAllowed(members = true)
    static abstract class MS extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public MS(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }
    }

    MS ms;

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

    public void method3() {
        @DefineScope(name="a",parent=IMMORTAL)
        @Scope(IMMORTAL)
        MemoryArea m = ManagedMemory.getMemoryArea(ms);

        @DefineScope(name="a",parent=IMMORTAL)
        @Scope(IMMORTAL)
        ImmortalMemory imm = (ImmortalMemory) m;
    }

    @DefineScope(name="child", parent="a")
    static class MyRun1 implements Runnable {
        @RunsIn("child")
        public void run() {  }
    }

    static class MyRun2 implements Runnable {
        @RunsIn("a")
        public void run() {  }
    }
}
