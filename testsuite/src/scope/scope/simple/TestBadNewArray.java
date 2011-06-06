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
public abstract class TestBadNewArray extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadNewArray() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}


        @DefineScope(name="a", parent=IMMORTAL)
        @Scope(IMMORTAL)
        ManagedMemory mem;

        @DefineScope(name="b", parent="a")
        @Scope("a")
        ManagedMemory mem2;

        public void foo() {
            try {
                mem.newArray(Y.class, 1);
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_ARRAY
                mem2.newArray(Y.class, 1);
            } catch (Exception e) {
                //...
            }
        }
    }

    @Scope("a")
    static class Y { }
}
