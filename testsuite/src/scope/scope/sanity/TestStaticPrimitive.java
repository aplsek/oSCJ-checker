package scope.scope.sanity;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class TestStaticPrimitive {

    private static final int INTERRUPT_PRIORITY = 32;

    public void run() {
        takePrimitive(INTERRUPT_PRIORITY);
    }

    void takePrimitive(int primivite) {
        // do something.
    }
}
