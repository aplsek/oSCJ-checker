package scope.defineScope.simple;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestReservedScopeNameParentCurrent {
    @DefineScope(name="a", parent=Scope.CURRENT)
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static class X { }
}
