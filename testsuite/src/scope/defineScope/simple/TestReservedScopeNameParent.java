package scope.defineScope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestReservedScopeNameParent {
    @DefineScope(name="a", parent=Scope.CALLER)
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class X extends Mission { }

    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static abstract class Y extends Mission { }

    @DefineScope(name="a", parent=Scope.THIS)
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class Z extends Mission { }

    @DefineScope(name="a", parent=Scope.UNKNOWN)
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class W extends Mission { }
}
