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

    @DefineScope(name="Mission",parent=IMMORTAL)
    @Scope(IMMORTAL)
    ManagedMemory mem;                              // OK

    //@Scope(IMMORTAL)
    //ManagedMemory mem2;

    @DefineScope(name="PEH",parent="Mission")
    @Scope("Mission")
    ManagedMemory mem3;                             // OK

    //@DefineScope(name="PEH",parent="Mission")
    //@Scope(IMMORTAL)                                // ERR
    //ManagedMemory mem4;

    @DefineScope(name="PEH",parent="Mission")
    ManagedMemory mem5;                             // OK

    //@DefineScope(name="Mission",parent=IMMORTAL)
    //ManagedMemory mem6;                             // ERROR

    public void method() {
       mem = ManagedMemory.getCurrentManagedMemory();
    }

    @RunsIn("PEH")
    public void handleAsyncEvent() {
           // mem.newInstance(Foo.class);
    }
}

