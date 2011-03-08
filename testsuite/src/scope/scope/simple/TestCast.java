package scope.scope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

//TODO: rename this once its clear which error this should throw.
public class TestCast {

    @DefineScope(name = "a", parent = IMMORTAL)
    class A extends Mission {

        @Override
        public long missionMemorySize() {
            return 0;
        }

        @Override
        @SCJRestricted(INITIALIZATION)
        protected void initialize() {
        }
    }

    @Scope("b")
    @DefineScope(name = "b", parent = "a")
    class B extends A {
    }

    @Scope("a")
    class C extends A {
    }

    @RunsIn("a")
    public void method() {
        A a = new A();

        // ## ERROR TODO:
        B b = (B) a;
    }
}
