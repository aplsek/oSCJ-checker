package scope.defineScope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true)
@DefineScope(name="a", parent=IMMORTAL)
//## checkers.scope.DefineScopeChecker.ERR_UNUSED_DEFINE_SCOPE
public class TestUnusedDefineScopeClass { }
