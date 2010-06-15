/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/TimeTest.java,v 1.1 2004/10/15 01:53:12 dholmes Exp $
 */
package javax.realtime;

/**
 * Test driver for the HighResolutionTime, AbsoluteTime and RelativeTime
 * classes, to check that normalization and overflow are handled correctly.
 * These tests should go in the RTSJ TCK.
 *
 * @author David Holmes
 */
class TimeTest {

    static class TestPair {
        long millis;
        int[] nanos;

        TestPair(long m, int[] n) {
            millis = m;
            nanos = n;
        }
    }

    // nanos per millisecond
    static final int NPM = 1000000;
    // maximum millis value you can combine the maximum nanos without overflow
    static final long THRESHHOLD = Long.MAX_VALUE - (Integer.MAX_VALUE / NPM);

    // minimum millis value you can combine the minimum nanos without overflow
    static final long NEG_THRESHHOLD = Long.MIN_VALUE - (Integer.MIN_VALUE / NPM);

    /* We test the key millis values that divide the range:
       - min and max values,
       - -1, 0 and +1
       - the max and min values to which you can add max/min nanos,
       - the above thessholds +1 and -1

      We pair these up with the interesting nanos values:
       - min/max
       - 100000 +/- 1
       - +/- 1ms worth of nanos from extremes
       - -1, 0, +1

      We pair from the smallest key value to succeed, through to the largest
      key value to succeed.
    */
    static TestPair[] good = new TestPair[] {
        new TestPair(Long.MIN_VALUE, 
                     new int[]{ -NPM+1, -1, 0, 1, Integer.MAX_VALUE}),
        new TestPair(NEG_THRESHHOLD-1, 
                     new int[]{ Integer.MIN_VALUE+NPM, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE}),
        new TestPair(NEG_THRESHHOLD, 
                     new int[]{ Integer.MIN_VALUE, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE-1,Integer.MAX_VALUE}),
        new TestPair(NEG_THRESHHOLD+1, 
                     new int[]{ Integer.MIN_VALUE, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE-1, Integer.MAX_VALUE}),
        new TestPair(-1, 
                     new int[]{ Integer.MIN_VALUE, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE-1, Integer.MAX_VALUE}),
        new TestPair(0, 
                     new int[]{ Integer.MIN_VALUE, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE-1, Integer.MAX_VALUE}),
        new TestPair(1, 
                     new int[]{ Integer.MIN_VALUE, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE-1, Integer.MAX_VALUE}),
        new TestPair(THRESHHOLD-1, 
                     new int[]{ Integer.MIN_VALUE, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE-1, Integer.MAX_VALUE}),
        new TestPair(THRESHHOLD, 
                     new int[]{ Integer.MIN_VALUE, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE-1,Integer.MAX_VALUE}),
        new TestPair(THRESHHOLD+1, 
                     new int[]{ Integer.MIN_VALUE, -NPM-1, -NPM, -NPM+1, -1, 0, 1, NPM-1, NPM, NPM+1, Integer.MAX_VALUE-NPM}),
        new TestPair(Long.MAX_VALUE, 
                     new int[]{ Integer.MIN_VALUE, -1, 0, 1, NPM-1 }),
    };

    // for each of the ley millis values that can fail we pair with the
    // smallest/largest value that will cause failure
    static TestPair[] bad = new TestPair[] {
        new TestPair(Long.MIN_VALUE, new int[]{ -NPM }),
        new TestPair(NEG_THRESHHOLD-1, new int[]{ Integer.MIN_VALUE}),
        new TestPair(THRESHHOLD+1, new int[]{Integer.MAX_VALUE}),
        new TestPair(Long.MAX_VALUE, new int[]{ NPM }),
    };

    static void testNormalization() {
        for (int i = 0; i < good.length; i++) {
            int[] nanos = good[i].nanos;
            for (int j = 0; j < nanos.length; j++) {
                try {
                    new AbsoluteTime(good[i].millis, nanos[j]);
                }
                catch (Throwable t) {
                    System.err.println("Failure: " + t);
                    System.err.println(" - millis = " + good[i].millis + 
                                       ", nanos = " + nanos[j]);
                }
                try {
                    new RelativeTime(good[i].millis, nanos[j]);
                }
                catch (Throwable t) {
                    System.err.println("Failure: " + t);
                    System.err.println(" - millis = " + good[i].millis + 
                                       ", nanos = " + nanos[j]);
                }

            }
        }

        for (int i = 0; i < bad.length; i++) {
            int[] nanos = bad[i].nanos;
            for (int j = 0; j < nanos.length; j++) {
                try {
                    new AbsoluteTime(bad[i].millis, nanos[j]);
                    System.err.println("Failure - no exception: " +
                                       " - millis = " + bad[i].millis + 
                                       ", nanos = " + nanos[j]);
                }
                catch (Throwable t) {
                    // ok
                }
                try {
                    new RelativeTime(bad[i].millis, nanos[j]);
                    System.err.println("Failure - no exception: " +
                                       " - millis = " + bad[i].millis + 
                                       ", nanos = " + nanos[j]);
                }
                catch (Throwable t) {
                    // ok
                }
            }
        }
    }


    public static void main(String[] args) {
        Clock.getRealtimeClock(); // force load & init
        testNormalization();
    }
}

