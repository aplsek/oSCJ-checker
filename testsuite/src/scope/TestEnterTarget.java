//scope/TestEnterTarget.java:25: enter() must target a child scope.
//        a.enter(new TestEnterTargetRunnable());
//               ^
//Users/plsek/_work/workspace_RT/scj-annotations/tests/scope/TestEnterTarget.java:55: (Class may not have @RunsIn annotation with no @Scope annotation.)
//class TestEnterTargetRunnable implements Runnable {
//^
//2 errors

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope(IMMORTAL) 
@RunsIn(IMMORTAL)
@DefineScope(name="a", parent=IMMORTAL)
public class TestEnterTarget {
    PrivateMemory a;
    PrivateMemory b;

    @RunsIn("b")
    public void foo() {
        a.enter(new TestEnterTargetRunnable());  // FAIL : enter() must target a child scope.
    }
   
    public void foo2() {
            ManagedMemory.
                getCurrentManagedMemory().
                    enterPrivateMemory(0, new R1000());
        }
}

@Scope(IMMORTAL) 
@DefineScope(name="a1", parent=IMMORTAL)
class R1000 implements Runnable {
    @Override
    @RunsIn("a1")
    public void run() {
        ManagedMemory.
            getCurrentManagedMemory().
                enterPrivateMemory(0, new R2000());
    }
}

@Scope("a1") 
@DefineScope(name="b1", parent="a1")
class R2000 implements Runnable {
    @Override
    @RunsIn("b1")
    public void run() {
    }
}

@RunsIn("a")  // FAIL
class TestEnterTargetRunnable implements Runnable {
    public void run() {
    }
}

@DefineScope(name="b", parent="a")
class B1112 {}

