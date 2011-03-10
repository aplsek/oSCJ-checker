package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestBadMethodInvoke extends Mission {

    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends Mission {
        Y y;

        @RunsIn("b")
        public void foo() {
            foo();
            y.methodRunsInUnknown();
            y.methodRunsInB();

            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            y.method();
            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            y.methodRunsInC();
        }

        void bar() {
            y.method();
        }
    }

    @Scope("a")
    static class Y {
        void method() { }

        @RunsIn(CALLER)
        void methodRunsInUnknown() {
            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            methodRunsInB();
        }

        @RunsIn("b")
        void methodRunsInB() { }

        @RunsIn("c")
        void methodRunsInC() { }
    }

    @Scope("c")
    @DefineScope(name="c", parent="b")
    static abstract class W extends Mission { }
}
