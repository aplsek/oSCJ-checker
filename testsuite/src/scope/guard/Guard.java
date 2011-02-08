//testsuite/src/scope/guard/Guard.java:29: The variables passed into the GUARD statement must be final. The argument myFoo is not.
//        if (ManagedMemory.allocInSame(bar,myFoo)) {     // ERROR, myFoo is not final
//                                    ^
//1 error

package scope.guard;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;
import static javax.safetycritical.annotate.Scope.UNKNOWN;

@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission") 
public class Guard {

    @RunsIn(UNKNOWN)
    public void method(final Foo foo) {
        final Bar bar = new Bar();
        
        if (ManagedMemory.allocInSame(bar,foo)) {
            // do something in the guard...
        }
         
        
        if (ManagedMemory.allocInParent(bar,foo)) {
            // do something in the guard...
        }
        
        Foo myFoo = new Foo();
        if (ManagedMemory.allocInSame(bar,myFoo)) {     // ERROR, myFoo is not final
            // do something in the guard...
        }
        
    }
    
}

class Foo {}

class Bar {}

