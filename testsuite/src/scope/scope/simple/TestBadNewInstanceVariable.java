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
public abstract class TestBadNewInstanceVariable extends MissionSequencer  {

    @SCJRestricted(INITIALIZATION)
    public TestBadNewInstanceVariable() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        public void method() {
            try {
                @DefineScope(name="b", parent="a")
                @Scope("a")
                ManagedMemory mem = null;
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
                mem.newInstance(Y.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Scope("a")
    static class Y { }
}

