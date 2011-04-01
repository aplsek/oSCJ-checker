package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Level.SUPPORT;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class TestSimpleOverride {

    static class A extends Mission {
        A a;

        public void methodY() {
            a = new A();
        }

        public A methodZ() {
            return a;
        }

        @Override
        public long missionMemorySize() {
            return 0;
        }

        @Override
        @SCJRestricted(INITIALIZATION)
        @SCJAllowed(SUPPORT)
        protected void initialize() {
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
