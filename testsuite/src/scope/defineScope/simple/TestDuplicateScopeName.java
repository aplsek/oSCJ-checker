package scope.defineScope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.SCJAllowed;


import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed(members=true)
public class TestDuplicateScopeName {
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}
    }
    @DefineScope(name="b", parent=IMMORTAL)
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);}
    }
    @DefineScope(name="b", parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_DUPLICATE_SCOPE_NAME
    static abstract class Z extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Z() {super(null, null);} }
}
