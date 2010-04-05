package scope;

import javax.realtime.MemoryArea;
import javax.safetycritical.annotate.Scope;

@Scope("immortal")
public class TestGetMemoryArea {
    public void foo() {
        TestGetMemoryArea a = new TestGetMemoryArea();
        MemoryArea.getMemoryArea(a).newInstance(TestGetMemoryArea.class);
        Object o = new Object();
        MemoryArea.getMemoryArea(o).newInstance(TestGetMemoryArea.class);
    }
}
