package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_0;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members=true,value=Level.LEVEL_1)
public class SCJFakeNestedClasses {
    
    public static class Nested {
        public static void nested1 () {        
        }        
    }
    
    /**
     * calling a nested class...
     */
    @SCJAllowed(Level.LEVEL_1)
    public static void foo() {
        Nested.nested1();
    }  
    
    
    
    /**
     * calling a nested class...
     */
    public static void fooSuppress() {
        Nested.nested1();
    }  
    
}
