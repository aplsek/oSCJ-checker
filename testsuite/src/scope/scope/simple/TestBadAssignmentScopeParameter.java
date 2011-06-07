package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@DefineScope(name="a", parent=IMMORTAL)
@Scope(IMMORTAL)
public abstract class TestBadAssignmentScopeParameter extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadAssignmentScopeParameter() {super(null, null);}

    @Scope("a")
    @DefineScope(name="b", parent="a")
    @SCJAllowed(members = true)
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X(PriorityParameters priority, StorageParameters storage) {
            super(priority, storage);
        }

        Y y;
        Object o = new Object();

        @RunsIn("b")
        public void foo() {
            //## checkers.scope.ScopeChecker.ERR_BAD_ASSIGNMENT_SCOPE
            y.bar(o);
        }
    }

    @Scope("a")
    static class Y {
        @RunsIn("b")
        void bar(Object o) { }
    }
}
