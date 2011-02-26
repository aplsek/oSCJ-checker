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
    
    public void bar1() {
        MyRunnable run = new MyRunnable();
        ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();                // OK
        mem.enterPrivateMemory(1000, run);  
    }
  
    
    public void bar2() {
        MyRunnable run = new MyRunnable();
        @Scope("Mission") ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();   // OK
        mem.enterPrivateMemory(1000, run);
    }
   
    public void bar3() {
        MyRunnable run = new MyRunnable();
        @Scope("foobar") ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();   // ERROR
        mem.enterPrivateMemory(1000, run);
    }
    
    @RunsIn("Mission")
    public void bar4() {
        MyRunnable run = new MyRunnable();
         ManagedMemory mem = ManagedMemory.getCurrentManagedMemory();   // OK

        try {
        
            mem.newInstance(Foo.class);

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

@Scope("B")
class Foo {
}


@Scope("Mission")
@DefineScope(name="PEH",parent="Mission")
class PEH {
    
    @Scope("Immortal") @DefineScope(name="Mission",parent="Immortal")
    ManagedMemory mem;
    
    PEH() {
        mem = ManagedMemory.getCurrentManagedMemory();
    }
    
    @RunsIn("PEH")
    public void handleAsyncEvent() {
        MyRun run = new MyRun();
        mem.enterPrivateMemory(1000, run);
        try {
            mem.newInstance(Foo.class);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}



@SCJAllowed(members=true)
@Scope("PEH")
@DefineScope(name="run2",parent="PEH")
class MyRun implements SCJRunnable {
    
    @RunsIn("run2")
    public void run() {
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


@DefineScope(name="foobar",parent="Mission")
class FooBar {}