package scope.defineScope.simple;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestReservedScopeNameImmortal {
    @DefineScope(name=Scope.IMMORTAL, parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME
    static class X { }
}
