package scope.defineScope.simple;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=Scope.IMMORTAL)
//## warning: checkers.scope.DefineScopeChecker.ERR_UNUSED_DEFINE_SCOPE
public class TestUnusedDefineScope { }
