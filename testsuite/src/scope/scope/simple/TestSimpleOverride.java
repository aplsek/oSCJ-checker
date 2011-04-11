package scope.scope.simple;

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
        protected Mission getNextMission() {
            return null;
        }
    }

    @Scope("ONE")
    @DefineScope(name="ONE", parent=IMMORTAL)
    static abstract class B extends A {

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
