package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Level.SUPPORT;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;


@SCJAllowed(members=true)
public class TestSimpleOverride {

    @SCJAllowed(members=true)
    @Scope("ZERO")
    @DefineScope(name="ZERO", parent=IMMORTAL)
    static class A extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public A() {super(null, null);}

        A a;

        public void methodY() {
            a = new A();
        }

        public A methodZ() {
            return a;
        }

        @Override
        @SCJAllowed(SUPPORT)
        protected Mission getNextMission() {
            return null;
        }
    }

    @SCJAllowed(members=true)
    @Scope("ONE")
    @DefineScope(name="ONE", parent="ZERO")
    //## checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE
    static abstract class B extends A {

        @SCJRestricted(INITIALIZATION)
        public B() {
            super();
        }
        A aa;

        public void method2() {
            this.methodY();
        }

        public void method3() {
            aa = this.methodZ();
        }

        @Scope("ONE")
        public A method4() {
            return this.methodZ();
        }
    }
}
