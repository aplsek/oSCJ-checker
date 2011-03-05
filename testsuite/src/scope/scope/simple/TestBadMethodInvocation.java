package scope.scope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
@Scope("a")
public abstract class TestBadMethodInvocation extends Mission {

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        Y y;
        Arg arg = new Arg();

        @RunsIn("b")
        public void method() {
            method();
            y.methodRunsInUnknown();
            y.methodRunsInB();

            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            y.method(arg);
            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            y.method();
            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            y.methodRunsInC();
        }

        void method2() {
            y.method();
        }
    }

    @Scope("a")
    static class Y {
        void method() { }

        @RunsIn("b")
        void method(Arg arg) { }

        @RunsIn(Scope.UNKNOWN)
        void methodRunsInUnknown() { }

        @RunsIn("b")
        void methodRunsInB() { }

        @RunsIn("c")
        void methodRunsInC() { }
    }

    static class Arg { }

    @Scope("c")
    @DefineScope(name="c", parent="b")
    static abstract class W extends Mission { }
}
