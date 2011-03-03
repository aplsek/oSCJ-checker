//scope/TestExecuteInArea.java:17: Runnable and PrivateMemory scopes disagree.
//        a.executeInArea(r);
//                       ^
//1 error

package scope;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.DefineScope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("a")
@DefineScope(name = "a", parent = IMMORTAL)
public abstract class TestExecuteInArea extends Mission {

    //PrivateMemory b;
    //PrivateMemory c;

   // GoodR r2 = new GoodR();

    //@RunsIn("c")
    //public void bar() {
    //    a.executeInArea(r2);
   // }

    @Scope("b")
    @DefineScope(name = "b", parent = "a")
    abstract static class x extends Mission {

        @DefineScope(name = "b", parent = "a")
        @Scope("a")
        ManagedMemory a;

        public void m() {
            Run r = new Run();
            a.executeInArea(r);
        }
    }

    @Scope("b")
    static class Run implements SCJRunnable {
        @RunsIn("a")
        public void run() {
        }
    }

    /*
    @Scope(IMMORTAL)
    static class GoodR implements Runnable {
        @RunsIn(IMMORTAL)
        public void run() {
        }
    }*/

}

