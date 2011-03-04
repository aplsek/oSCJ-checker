package scope.scope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadReturnScope extends Mission {
    @Scope(Scope.IMMORTAL) @RunsIn("b")
    public X getFooErr() {
        //## checkers.scope.ScopeChecker.ERR_BAD_RETURN_SCOPE
        return new X();
    }

    @Scope(Scope.IMMORTAL)
    public int getPrimitive() {
        return 1;
    }

    @Scope(Scope.IMMORTAL)
    public void getVoid() {
        return;
    }

    @Scope(Scope.IMMORTAL)
    public int[] getPrimitive2() {
        //## checkers.scope.ScopeChecker.ERR_BAD_RETURN_SCOPE
        return new int[]{1};
    }

    static class X { }
}


