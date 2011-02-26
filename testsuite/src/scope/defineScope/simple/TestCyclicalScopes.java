package scope.defineScope.simple;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestCyclicalScopes {
    @DefineScope(name="a", parent="b")
    static class X { }
    @DefineScope(name="b", parent="c")
    static class Y { }
    @DefineScope(name="c", parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_CYCLICAL_SCOPES
    static class Z { }
    // Suppresses the error on class Y.
    @DefineScope(name="c", parent=Scope.IMMORTAL)
    static class W { }
}
