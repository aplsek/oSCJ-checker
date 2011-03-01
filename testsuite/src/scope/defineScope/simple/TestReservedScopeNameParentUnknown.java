package scope.defineScope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestReservedScopeNameParentUnknown {
    @DefineScope(name="a", parent=Scope.UNKNOWN)
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static abstract class X extends Mission { }
}
