package scope.scopeRunsIn.sanity;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;


@Scope(IMMORTAL)
@SCJAllowed(members=true)
public class TestArrayField {
    protected Object position;
    protected Object[] positions;
    protected Object[] lengths;
    protected Object[] callsigns;

    public void method () {
        Object o = positions[1];
    }
}


