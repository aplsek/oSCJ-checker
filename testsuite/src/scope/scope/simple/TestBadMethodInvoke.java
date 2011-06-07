package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@DefineScope(name="a", parent=IMMORTAL)
@Scope(IMMORTAL)
public abstract class TestBadMethodInvoke extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadMethodInvoke() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

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

    @SCJAllowed(members = true)
    @Scope("b")
    @DefineScope(name="c", parent="b")
    static abstract class W extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public W() {super(null, null);}

    }
}
