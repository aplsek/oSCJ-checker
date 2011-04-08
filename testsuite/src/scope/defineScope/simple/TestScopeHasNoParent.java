package scope.defineScope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;

public class TestScopeHasNoParent {
    @DefineScope(name="a", parent="b")
    //## checkers.scope.DefineScopeChecker.ERR_SCOPE_HAS_NO_PARENT
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}
    }
}
