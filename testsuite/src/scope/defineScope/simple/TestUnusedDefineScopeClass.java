package scope.defineScope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.DefineScope;

@DefineScope(name="a", parent=IMMORTAL)
//## warning: checkers.scope.DefineScopeChecker.ERR_UNUSED_DEFINE_SCOPE
public class TestUnusedDefineScopeClass { }
