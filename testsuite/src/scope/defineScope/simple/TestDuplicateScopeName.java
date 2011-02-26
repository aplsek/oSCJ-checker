package scope.defineScope.simple;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestDuplicateScopeName {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static class X { }
    @DefineScope(name="b", parent=Scope.IMMORTAL)
    static class Y { }
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    //## checkers.scope.DefineScopeChecker.ERR_DUPLICATE_SCOPE_NAME
    static class Z { }
}
