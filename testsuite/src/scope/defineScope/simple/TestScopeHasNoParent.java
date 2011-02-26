package scope.defineScope.simple;

import javax.safetycritical.annotate.DefineScope;

public class TestScopeHasNoParent {
    @DefineScope(name="a", parent="b")
    //## checkers.scope.DefineScopeChecker.ERR_SCOPE_HAS_NO_PARENT
    static class X { }
}
