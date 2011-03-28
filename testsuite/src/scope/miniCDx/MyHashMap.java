package scope.miniCDx;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import static javax.safetycritical.annotate.Scope.THIS;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
public class MyHashMap {

    @RunsIn(CALLER) @Scope(THIS)
    public Object get(@Scope(UNKNOWN) Object key) {
        return null;
    }

    public void put(Object key, Object value) {
        // TODO Auto-generated method stub

    }

}
