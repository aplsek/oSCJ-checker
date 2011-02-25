package scope.enterPM;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@DefineScope(name="Mission",parent=IMMORTAL)
@Scope("Mission")
public class SimpleEnterPM {

  
    
    public void bar2() {
        MyRunnable run = new MyRunnable();
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();
        mem.enterPrivateMemory(1000, run);
    }
   
    
}

@SCJAllowed(members=true)
@Scope("Mission")
@DefineScope(name="run",parent="Mission")
class MyRunnable implements SCJRunnable {
    
    @RunsIn("run")
    public void run() {
    }
}