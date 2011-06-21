package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="D", parent=IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestUpcast2 extends MissionSequencer {

    @Scope("D")
    static class Test {
        //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
        Runnable r = new Y();
    }

    @SCJRestricted(INITIALIZATION)
    public TestUpcast2() {
        super(null, null);
    }

    @RunsIn("D")
    public void bar() {
        Y y = new Y();

        //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
        bar(y);

        //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
        bar(y);

        @Scope(IMMORTAL)
        @DefineScope(name = "D", parent = IMMORTAL)
        ManagedMemory mem = null;
        mem.enterPrivateMemory(1000, y);



    }

    @RunsIn("D")
    public void bar(Runnable run) {}

    @RunsIn("D")
    public void bar2() {
        YY  yy  = new YY();
        Y  y  = new Y();

        @Scope(IMMORTAL)
        @DefineScope(name = "D", parent = IMMORTAL)
        ManagedMemory mem = null;
        //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
        mem.enterPrivateMemory(1000, (yy = y));

        //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
        Runnable[] arr = new Runnable[]{(yy=y)};
    }

    @SCJAllowed(members = true)
    @Scope("D")
    @DefineScope(name = "YY", parent = "D")
    static class YY implements Runnable {
        @RunsIn("YY")
        public void run() {}
    }


    @SCJAllowed(members = true)
    @Scope("D")
    @DefineScope(name = "C", parent = "D")
    static class Y extends YY {
        @Override
        @RunsIn("C")
        public void run() {}
    }

}
