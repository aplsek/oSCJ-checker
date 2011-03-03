package scope.scope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public class TestBadReturnScope extends Mission {

    @Override
    protected void initialize() {
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

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


    @Scope("a")
    @DefineScope(name="b", parent="a")
    abstract static class Handler extends Mission {}

    class X {}
}


