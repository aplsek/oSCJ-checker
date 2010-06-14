//defineScope/DefineScopeTest.java:19: Cyclical scope names detected.
//ManagedMemory.enterPrivateMemory(0, new /*@DefineScope(name="b", parent="a")*/ A());
//                                ^
//defineScope/DefineScopeTest.java:20: Duplicate scope name from @DefineScope.
//ManagedMemory.enterPrivateMemory(0, new /*@DefineScope(name="a", parent="b")*/ A());
//                                ^
//defineScope/DefineScopeTest.java:21: Reserved scope name used in @DefineScope.
//ManagedMemory.enterPrivateMemory(0, new /*@DefineScope(name="immortal", parent="b")*/ A());
//                                ^
//3 errors

package defineScope;


import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.ManagedMemory;

public class DefineScopeTest {
    public static class A implements Runnable {
        public void run() {
            
        }
    }
    public static class B implements Runnable {
        public void run() {
            
        }
    }
    public void test() {
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new /*@DefineScope(name="a", parent="b")*/ A());
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new /*@DefineScope(name="b", parent="a")*/ A());
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new /*@DefineScope(name="a", parent="b")*/ A());
        ManagedMemory.
        getCurrentManagedMemory().
            enterPrivateMemory(0, new /*@DefineScope(name="immortal", parent="b")*/ A());
    }
}
