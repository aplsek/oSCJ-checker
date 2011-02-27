package scope.memory;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;


@DefineScope(name="Mission",parent=IMMORTAL)
@Scope("Mission")
public class SimpleMemory {
}


@Scope("Mission")
@DefineScope(name="PEH",parent="Mission")
class PEH {

    @DefineScope(name="Mission",parent="Immortal")
    @Scope(IMMORTAL)
    ManagedMemory mem;

    public void method() {
        mem = ManagedMemory.getCurrentManagedMemory();
    }

    @RunsIn("PEH")
    public void handleAsyncEvent() {
           // mem.newInstance(Foo.class);
    }
}

@Scope("Mission")
class Foo {}