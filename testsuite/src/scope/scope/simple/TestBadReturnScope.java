package scope.scope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadReturnScope extends Mission {
    @Scope(Scope.IMMORTAL) @RunsIn("b")
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

    @Scope(Scope.IMMORTAL)
    public int[] getArray() {
        //## checkers.scope.ScopeChecker.ERR_BAD_RETURN_SCOPE
        return new int[] { 1 };
    }

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission { }

    static class Y { }
}


