package scope.defineScope.simple;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.THIS;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;

public class TestReservedScopeNameParent {
    @DefineScope(name="a", parent=CALLER)
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class X extends Mission { }

    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class Y extends Mission { }

    @DefineScope(name="a", parent=THIS)
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class Z extends Mission { }

    @DefineScope(name="a", parent=UNKNOWN)
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class W extends Mission { }
}
