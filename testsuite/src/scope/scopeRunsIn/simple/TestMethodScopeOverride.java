package scope.scopeRunsIn.simple;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true)
public class TestMethodScopeOverride {


    static class X {
        @Override
        @RunsIn(CALLER)
        public String toString() {
            return "my override to String";
        }
    }
}
