package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestBadReturnScope extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadReturnScope() {super(null, null);}

    @Scope(IMMORTAL) @RunsIn("b")
    public Y getFooErr() {
        //## checkers.scope.ScopeChecker.ERR_BAD_RETURN_SCOPE
        return new Y();
    }

    public int getPrimitive() {
        return 1;
    }

    public void getVoid() {
        return;
    }

    @Scope(IMMORTAL)
    public int[] getArray() {
        //## checkers.scope.ScopeChecker.ERR_BAD_RETURN_SCOPE
        return new int[] { 1 };
    }

    @Scope("a")
    public Y methNull() {
        return null;
    }

    @Scope(UNKNOWN)
    public Y methUNK() {
        @Scope("a") Y y = new Y();
        return y;
    }

    @Scope("a")
    public Y meth() {
        @Scope(UNKNOWN) Y y = new Y();
        //## checkers.scope.ScopeChecker.ERR_BAD_RETURN_SCOPE
        return y;
    }

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission { }

    static class Y { }
}


