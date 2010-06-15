package test.jvm;



/**
 * @author janvitek
 * */
public class TestTLock extends TestBase {

    public TestTLock(String description, Harness h) {
        super(description, h);
    }

    public void run() {
       // int[] arr = new int[1000];
      //  int[] addr = new int[100];
        Object[] objs = new Object[10];
        for (int i = 0; i < objs.length; i++) {
            objs[i] = new Integer[i];
        }
/*
        int start = LibraryImports.tlock_startBuffer(arr);
        addr[0] = start;
        for (int i = 1; i < objs.length; i++) {
            int a = addr[i];
            addr[i + 1] = LibraryImports.tlock_copyIntoBuffer(objs[i], a);
        }

        for (int i = 0; i < objs.length; i++) {
            int a = addr[i];
            LibraryImports.tlock_copyFromBuffer(a, objs[i]);
            p(((Integer)objs[i]).intValue() + "\n");
        }
        */
    }
}
