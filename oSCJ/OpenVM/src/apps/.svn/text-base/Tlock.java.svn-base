//import test.jvm.LibraryImports;

public class Tlock {
    static public interface Transactional {
    }
    static public class Acid extends Error {
    }
    static public class Abort extends Error {
    }
    static class PragmaNoPollcheck extends Error {
    }

    static public final int[] log = new int[10 * 1024];
    static public final int[] offsets = new int[100];
    static public final TObject[] objects = new TObject[100];
    static public int counter = -1;
    static final Abort abort = new Abort();

    static public class TObject {
        boolean logged;
        Thread thread;

        static void tlock_log(TObject obj) throws PragmaNoPollcheck {
            //            Thread.setReschedulingEnabled( false);
            counter++;
            objects[counter] = obj;
            //            offsets[counter + 1] =
            //                LibraryImports.tlock_copyIntoBuffer(obj, offsets[counter]);
            obj.logged = true;
            //            Thread.setReschedulingEnabled( true);
        }

        static void tlock_abort() throws PragmaNoPollcheck {
            while (counter >= 0) {
                //  LibraryImports.tlock_copyFromBuffer(
                //                    offsets[counter],
                //                    objects[counter]);
                counter--;
            }
        }
    }

    static public class LList extends TObject implements Transactional {
        LList next, prev;
        int value;

        synchronized int s_get(int pos) {
            return r_get(pos);
        }
        int r_get(int pos) {
            LList cur = this;
            for (int i = 0; i < pos; i++) {
                if (cur == null)
                    throw new Error("Cant retrieve from this position");
                cur = cur.next;
            }
            return cur.value;
        }

        int t_get(int pos) throws Acid, PragmaNoPollcheck {
            int value = 0;
            try {
                //                Thread.setReschedulingEnabled(false);
                while (true)
                    try {
                        if (thread != null) {
                            tlock_abort();
                            //          thread.setExceptionToThrow(abort);
                        }
                        thread = Thread.currentThread();
                        LList cur = this;
                        value = r_get(pos);
                        break;
                    } catch (Abort _) {
                        continue;
                    }
            } finally {
                thread = null;
                //                Thread.setReschedulingEnabled(true);
                return value;
            }
        }

        synchronized void s_insert(int v) {
            raw_insert(v);
        }
        private static final int INSERT = 0, SUM = 1;

        void t_insert(int v) throws Acid, PragmaNoPollcheck {
            t_do(v, INSERT);
        }

        void t_do(int v, int ACTION) throws Acid, PragmaNoPollcheck {
            try {
                //                Thread.setReschedulingEnabled(false);
                while (true)
                    try {
                        if (thread != null) {
                            tlock_abort();
                            //          thread.setExceptionToThrow( abort);
                        }
                        thread = Thread.currentThread();
                        //                        Thread.setReschedulingEnabled( true);
                        if (ACTION == INSERT)
                            log_insert(v);
                        else
                            log_sum();
                        //                        Thread.setReschedulingEnabled(false);
                        break;
                    } catch (Abort _) {
                        continue;
                    }
            } finally {
                thread = null;
                while (counter >= 0)
                    objects[counter--].logged = false;
                //                Thread.setReschedulingEnabled(true);
            }
        }

        void raw_insert(int v) {
            LList cur = this;
            while (cur.next != null && v > cur.value)
                cur = cur.next;
            LList lk = new LList();
            lk.value = v;
            lk.prev = cur;
            lk.next = cur.next;
            cur.next = lk;
            if (lk.next != null)
                lk.next.prev = lk;
        }

        void log_insert(int v) {
            LList cur = this;
            while (cur.next != null && v > cur.value)
                cur = cur.next;
            LList lk = new LList();
            lk.value = v;
            lk.prev = cur;
            lk.next = cur.next;
            if (!cur.logged)
                tlock_log(cur);
            cur.next = lk;
            if (lk.next != null) {
                if (!lk.next.logged)
                    tlock_log(lk.next);
                lk.next.prev = lk;
            }
        }

        int raw_sum() {
            LList cur = this, prev = null;
            int i = 0;
            while (cur != null) {
                if (cur.value < 0)
                    throw new Error();
                i += cur.value;
                prev = cur;
                cur = cur.next;
            }
            return i;
        }

        void t_sum() throws Acid, PragmaNoPollcheck {
            t_do(0, SUM);
        }
        synchronized int s_sum() {
            return raw_sum();
        }

        int log_sum() {
            LList cur = this, prev = null;
            int i = 0;
            if (cur != null && !cur.logged)
                tlock_log(cur);
            while (cur != null) {
                if (cur.value < 0)
                    throw new Error();
                i += cur.value;
                prev = cur;
                cur = cur.next;
            }
            return i;
        }

        void print() {
            LList item = this;
            while (item != null) {
                System.out.print(item.value + " ");
                item = item.next;
            }
            System.out.println("");
        }

        void cut(int v) {
            LList cur = this;
            for (int i = 0; i < v; i++) {
                if (cur == null)
                    return;
                cur = cur.next;
            }
            cur.next = null;
        }
    }

    static void init() {
    } //offsets[0] = LibraryImports.tlock_startBuffer(log);}

    static public void main(String[] args) {
        init();
        LList test = new LList();
        test.raw_insert(1);
        test.raw_insert(4);
        test.raw_insert(8);
        for (int i = 0; i < 100; i++)
            test.raw_insert(9);
        test.raw_insert(13);
        long l1 = System.currentTimeMillis();
        System.out.println("T-inserting...");
        for (int i = 0; i < 10000; i++)
            test.t_insert(12);
        long l2 = System.currentTimeMillis();
        System.out.println(l2 - l1);
        System.out.println("S-inserting...");
        for (int i = 0; i < 10000; i++)
            test.s_insert(11);
        long l3 = System.currentTimeMillis();
        System.out.println(l3 - l2);
        System.out.println("Inserting...");
        for (int i = 0; i < 10000; i++)
            test.raw_insert(10);
        long l4 = System.currentTimeMillis();
        System.out.println(l4 - l3);
    }
}
