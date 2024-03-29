package scope.defineScope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.THIS;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed(members=true)
public class TestReservedScopeName {
    @DefineScope(name=CALLER, parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}
    }

    @DefineScope(name=IMMORTAL, parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y() {super(null, null);}
    }

    @DefineScope(name=THIS, parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class Z extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Z() {super(null, null);}
    }

    @DefineScope(name=UNKNOWN, parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class W extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public W() {super(null, null);}
    }
}
