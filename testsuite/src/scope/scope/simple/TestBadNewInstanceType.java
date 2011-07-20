package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@SCJAllowed(members = true)
@DefineScope(name="a", parent=IMMORTAL)
@Scope(IMMORTAL)
public abstract class TestBadNewInstanceType extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadNewInstanceType() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}


        @DefineScope(name="b", parent="a")
        @Scope("a")
        ManagedMemory mem;

        @RunsIn("b")
        public void foo() {
            try {
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(Z.class);
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(Y[].class);
                Class<?> c = null;
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(c);

                mem.newInstance(void.class);  // OK

                mem.newInstance(int.class);    // OK
            } catch (Exception e) { }
        }

        public void foo2() {
            try {

                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_REPRESENTED_SCOPE
                mem.newInstance(void.class);
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_REPRESENTED_SCOPE
                mem.newInstance(int.class);
            } catch (Exception e) { }
        }
    }

    @Scope("a")
    static class Y { }

    @Scope("a")
    static abstract class Z { }
}
