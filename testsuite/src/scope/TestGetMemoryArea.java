package scope;

import javax.realtime.InaccessibleAreaException;
import javax.realtime.MemoryArea;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL)
public class TestGetMemoryArea {
    public void foo() throws IllegalArgumentException,  IllegalAccessException, OutOfMemoryError, InaccessibleAreaException, InstantiationException {
        TestGetMemoryArea a = new TestGetMemoryArea();
        MemoryArea.getMemoryArea(a).newInstance(TestGetMemoryArea.class);
        Object o = new Object();
        MemoryArea.getMemoryArea(o).newInstance(TestGetMemoryArea.class);
    }
}
