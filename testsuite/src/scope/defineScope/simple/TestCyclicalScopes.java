package scope.defineScope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJRestricted;

public class TestCyclicalScopes {
    @DefineScope(name="a", parent="b")
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}
    }
    @DefineScope(name="b", parent="c")
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);} }
    @DefineScope(name="c", parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_CYCLICAL_SCOPES
    static abstract class Z extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Z() {super(null, null);}
    }
    // Suppresses the error on class Y.
    @DefineScope(name="c", parent=IMMORTAL)
    static abstract class W extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public W() {super(null, null);}
    }
}
