/*
 *
 * $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestPragmaAtomic.java,v 1.2 2004/05/28 06:20:39 dholmes Exp $
 *
 */
package test.runtime;
import s3.util.PragmaAtomic;
import s3.util.PragmaInline;
import s3.util.PragmaNoInline;
/**
 * Test the correct application of PragmaAtomic in terms of the state of
 * rescheduling in the ThreadManager
 *
 * @author David Holmes
 *
 */
public class TestPragmaAtomic extends TestThreadsBase {

    class Atomic {
        int val = 0;

        void doThrow(int arg) {
            if (arg < 0) {
                val = 0;
                throw new Error();
            }
            else
                val = arg;
        }

        void doAtomic() throws PragmaAtomic, PragmaNoInline {
            A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doAtomic");
            val = 1;
        }

        void doAtomicThrow() throws PragmaAtomic, PragmaNoInline {
            A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doAtomicThrow");
            doThrow(-1);
        }

        void doAtomicTryCatch() throws PragmaAtomic, PragmaNoInline {
            A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doAtomicTryCatch");
            try {
                doThrow(-1);
            }
            catch(Error e) { val = 2;}
        }

        void doAtomicTryFinally() throws PragmaAtomic, PragmaNoInline {
            A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doAtomicTryFinally");
            try {
                doThrow(-1);
            }
            finally {
                val = 10;
            }
        }
        
        void doAtomicInline() throws PragmaAtomic, PragmaInline {
            A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doAtomicInline");
            val = 1;
        }

        void doAtomicThrowInline() throws PragmaAtomic, PragmaInline {
            A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doAtomicThrowInline");
            doThrow(-1);
        }

        void doAtomicTryCatchInline() throws PragmaAtomic, PragmaInline {
            A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doAtomicTryCatchInline");
            try {
                doThrow(-1);
            }
            catch(Error e) { val = 2;}
        }

        void doAtomicTryFinallyInline() throws PragmaAtomic, PragmaInline {
            A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doAtomicTryFinallyInline");
            try {
                doThrow(-1);
            }
            finally {
                val = 10;
            }
        }

        void doNestedAtomicInnerInline() throws PragmaAtomic, PragmaInline {
          A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doNestedAtomicInnerInline (1)");
        }
        
        void doNestedAtomicInnerNoInline() throws PragmaAtomic, PragmaNoInline {
          A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doNestedAtomicInnerNoInline (1)");
        }
                
        void doNestedAtomic() throws PragmaAtomic, PragmaNoInline {
        
          A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doNesteAtomic (1)");
          doNestedAtomicInnerNoInline();
          A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doNesteAtomic (2)");
          doNestedAtomicInnerInline();
          A(threadMan.isReschedulingEnabled() == false, "scheduling enabled in doNesteAtomic (3)");
        }        

    }

    public TestPragmaAtomic() {
        super("Test effects of PragmaAtomic");
    }


    public void run() {
        Atomic a = new Atomic();

        A(threadMan.isReschedulingEnabled(), "scheduling not enabled initially");
        A(a.val == 0, "Fail 1");
        a.doAtomic();
        A(a.val == 1, "Fail 2");
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled after doAtomic");
        try {
            a.doAtomicThrow();
            A(false, "no exception!");
        }
        catch (Error e) {
        }
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled after doAtomicThrow");
        A(a.val == 0, "Fail 3");

        a.doAtomicTryCatch();
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled after doAtomicTryCatch");
        A(a.val == 2, "Fail 4");
        try {
            a.doAtomicTryFinally();
            A(false, "no exception!");
        }
        catch (Error e) {
        }
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled after doAtomicTryFinally");
        A(a.val == 10, "Fail 5");
        
        a.doAtomicInline();
        A(a.val == 1, "Fail 2 (inline)");
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled after doAtomicInline");
        try {
            a.doAtomicThrowInline();
            A(false, "no exception! (inline)");
        }
        catch (Error e) {
        }
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled after doAtomicThrowInline");
        A(a.val == 0, "Fail 3 (inline)");

        a.doAtomicTryCatchInline();
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled after doAtomicTryCatchInline");
        A(a.val == 2, "Fail 4 (inline)");
        try {
            a.doAtomicTryFinallyInline();
            A(false, "no exception! (inline)");
        }
        catch (Error e) {
        }
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled after doAtomicTryFinallyInline");
        A(a.val == 10, "Fail 5 (inline)");
        
        A(threadMan.isReschedulingEnabled(), "scheduling not enabled before doNestedAtomic");        
        a.doNestedAtomic();
        A(threadMan.isReschedulingEnabled(), "scheduling enabled afted doNestedAtomic");        
    }
}











