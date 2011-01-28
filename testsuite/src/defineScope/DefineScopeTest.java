//testsuite/src/defineScope/DefineScopeTest.java:22: Duplicate scope name from @DefineScope.
//    public static class A implements Runnable {
//                  ^
//scope def: b par:a
//testsuite/src/defineScope/DefineScopeTest.java:29: Cyclical scope names detected.
//    public static class B implements Runnable {
//                  ^
//scope def: IMMORTAL par:b
//testsuite/src/defineScope/DefineScopeTest.java:36: Reserved scope name used in @DefineScope.
//    public static class C implements Runnable {
//                  ^
//3 warnings

package defineScope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.ManagedMemory;

@DefineScope(name="a", parent="b")
public class DefineScopeTest {
   
    @DefineScope(name="a", parent="b")
    public static class A implements Runnable {
        public void run() {
            
        }
    }
    
    @DefineScope(name="b", parent="a")
    public static class B implements Runnable {
        public void run() {
            
        }
    }
    
    @DefineScope(name=IMMORTAL, parent="b")
    public static class C implements Runnable {
        public void run() {
            
        }
           
    }
    
    public void test() {
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new A());
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new A());
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new A());
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new A());
    }
}
