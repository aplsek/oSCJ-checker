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

@SCJAllowed(members = true)
@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
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

        public void foo() {
            try {
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(Z.class);
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(Y[].class);
                Class<?> c = null;
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(c);
                // TODO: Fix ## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(void.class);
                // TODO: Fix ## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE_TYPE
                mem.newInstance(int.class);
            } catch (Exception e) { }
        }
    }

    @Scope("a")
    static class Y { }

    @Scope("a")
    static abstract class Z { }
}
