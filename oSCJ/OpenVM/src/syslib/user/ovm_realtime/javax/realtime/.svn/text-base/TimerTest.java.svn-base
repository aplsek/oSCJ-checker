package javax.realtime;

/**
 * Tests the functionality of the {@link Timer}, {@link OneShotTimer} and
 * {@link PeriodicTimer} classes. These tests are just a start as there are
 * many permutations of tests that should be performed:
 * <ul>
 * <li>absolute versus relative fire times
 * <li>absolute times in the past
 * <li>starting before and after the absolute fire time
 * <li>rescheduling before/after start, with absolute/relative times, and
 * all variations
 * <li>changing interval for periodic timers.
 * <li> ...
 * </ul>
 *
 * @author David Holmes
 */
public class TimerTest {

    public static void main(final String[] args) {
        // we want to run these in a realtime thread
        (new RealtimeThread(
            new PriorityParameters(
                PriorityScheduler.instance().getNormPriority())
                ) {
                public void run() {
                    rt_main(args);
                }
            }).start();
    }


    static volatile boolean fired = false;
    static volatile long fireTime = 0;

    public static void rt_main(String[] args) {
        boolean debug = args.length > 0;
        final Clock.RealtimeClock rtc = Clock.rtc;

        AsyncEventHandler handler = new AsyncEventHandler() {
                AbsoluteTime now = new AbsoluteTime();
                public void handleAsyncEvent() {
                    rtc.getTime(now);
                    System.out.println("Handled at:        " + now);
                    fireTime = now.toNanos();
                    if (fired) 
                        System.out.println("Warning: firing too fast");
                    fired = true;
                    
                }
            };

        handler.setSchedulingParameters(new PriorityParameters(PriorityScheduler.instance().getNormPriority()+1));

        final AbsoluteTime now = new AbsoluteTime();
        final AbsoluteTime absStartTime = new AbsoluteTime();
        final RelativeTime relStartTime = new RelativeTime();
        final long millis = 2000; // a simple offset

        Timer timer = null;

        long diff = 0;
        long diffMillis = 0;
        AbsoluteTime getFireTimeResult = null;

        int test = 1;


        // basically every test should show a fire time as expected, and a 
        // delay that is within 1 clock tick (typically 10ms)

        System.out.println("Test " + (test++) + ": one-shot - absolute time in future");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        timer = new OneShotTimer(absStartTime, handler);
        //if (debug) timer.ABS_DEBUG = true;
        getFireTimeResult = timer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");
        timer.start();
        getFireTimeResult = timer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        while (!fired) {
            RealtimeThread.yield();
        }
        fired = false;
        diff = fireTime - absStartTime.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Firing delay: " + diffMillis + "ms");

        timer.destroy();

        /*---*/

        System.out.println("Test " + (test++) + ": one-shot - absolute time in past");

        rtc.getTime(now);
        now.subtract(new RelativeTime(millis,0), absStartTime);
        timer = new OneShotTimer(absStartTime, handler);
        //        if (debug) timer.ABS_DEBUG = true;
        getFireTimeResult = timer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for: " + absStartTime + 
                            " (now-" + millis + "ms)");
        timer.start();
        if (!fired)
            Assert.check(false, "Absolute start in past did not fire immediately");
        fired = false;
        diff = fireTime - now.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Firing delay: " + diffMillis + "ms");
        timer.destroy();

        /*---*/

        System.out.println("Test " + (test++) + ": one-shot - relative time");

        relStartTime.set(millis, 0);
        timer = new OneShotTimer(relStartTime, handler);
        //        if (debug) timer.REL_DEBUG = true;
        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for approx: " + absStartTime + 
                            " (now+" + millis + "ms)");
        timer.start();
        getFireTimeResult = timer.getFireTime();
        Assert.check(getFireTimeResult.compareTo(absStartTime) >= 0, "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        while (!fired) {
            RealtimeThread.yield();
        }
        fired = false;
        diff = fireTime - absStartTime.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Firing delay: " + diffMillis + "ms");
        timer.destroy();

        /*---*/

        System.out.println("Test " + (test++) + ": one-shot - absolute time with absolute reschedule and restart after firing");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        timer = new OneShotTimer(absStartTime, handler);
        //        if (debug) timer.ABS_DEBUG = true;
        getFireTimeResult = timer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");
        timer.start();
        getFireTimeResult = timer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        while (!fired) {
            RealtimeThread.yield();
        }
        fired = false;
        diff = fireTime - absStartTime.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Firing delay: " + diffMillis + "ms");
        absStartTime.add(millis,0, absStartTime);
        System.out.println("Fire time reset for: " + absStartTime + 
                            " (old+" + millis + "ms)");
        timer.reschedule(absStartTime);
        timer.start();
        getFireTimeResult = timer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after reschedule - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        while (!fired) {
            RealtimeThread.yield();
        }
        fired = false;
        diff = fireTime - absStartTime.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Rescheduled Firing delay: " + diffMillis + "ms");

        timer.destroy();

        /* --- */

        System.out.println("Test " + (test++) + ": one-shot - absolute time with absolute reschedule before start");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        timer = new OneShotTimer(absStartTime, handler);
        //        if (debug) timer.ABS_DEBUG = true;
        getFireTimeResult = timer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");

        absStartTime.add(millis,0, absStartTime);
        timer.reschedule(absStartTime);
        getFireTimeResult = timer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after reschedule - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        System.out.println("Fire rescheduled : " + absStartTime + 
                            " (now+" + (millis*2) + "ms)");
        timer.start();
        getFireTimeResult = timer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        while (!fired) {
            RealtimeThread.yield();
        }
        fired = false;
        diff = fireTime - absStartTime.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Rescheduled firing delay: " + diffMillis + "ms");
        timer.destroy();

        /*---*/

        System.out.println("Test " + (test++) + ": one-shot - absolute time with absolute reschedule after start but before firing");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        timer = new OneShotTimer(absStartTime, handler);
        //        if (debug) timer.ABS_DEBUG = true;
        getFireTimeResult = timer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");

        timer.start();
        getFireTimeResult = timer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        absStartTime.add(millis,0, absStartTime);
        timer.reschedule(absStartTime);
        getFireTimeResult = timer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after reschedule - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        System.out.println("Fire rescheduled : " + absStartTime + 
                            " (now+" + (millis*2) + "ms)");
        while (!fired) {
            RealtimeThread.yield();
        }
        fired = false;
        diff = fireTime - absStartTime.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Rescheduled firing delay: " + diffMillis + "ms");
        timer.destroy();

        /*---*/


        System.out.println("Test " + (test++) + ": one-shot - absolute time with relative reschedule and restart after firing ");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        timer = new OneShotTimer(absStartTime, handler);
//         if (debug) {
//             timer.ABS_DEBUG = true;
//             timer.REL_DEBUG = true;
//         }
        getFireTimeResult = timer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");
        timer.start();
        getFireTimeResult = timer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        while (!fired) {
            RealtimeThread.yield();
        }
        fired = false;
        diff = fireTime - absStartTime.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Firing delay: " + diffMillis + "ms");

        relStartTime.set(millis,0);
        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        timer.reschedule(relStartTime);
        System.out.println("Fire rescheduled : " + absStartTime + 
                            " (now+" + (millis*2) + "ms)");
        timer.start();
        getFireTimeResult = timer.getFireTime();
        Assert.check(getFireTimeResult.compareTo(absStartTime) >= 0, "wrong fire time after reschedule - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        while (!fired) {
            RealtimeThread.yield();
        }
        fired = false;
        diff = fireTime - absStartTime.toNanos();
        diffMillis = diff / (1000*1000);
        System.out.println("Rescheduled Firing delay: " + diffMillis + "ms");
        timer.destroy();




        /* Periodic Timer tests */

        timer = null;
        PeriodicTimer ptimer = null;
        RelativeTime period = new RelativeTime(millis+1000, 0);
        AbsoluteTime reschedAbs = new AbsoluteTime();
        final int PERIODS = 5;


        System.out.println("Test " + (test++) + ": Periodic - absolute start time in future");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        ptimer = new PeriodicTimer(absStartTime, period, handler);
        System.out.println(ptimer.createReleaseParameters());
        //        if (debug) ptimer.ABS_DEBUG = true;
        Assert.check(ptimer.getInterval().equals(period), "interval != period");
        getFireTimeResult = ptimer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("First Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");
        ptimer.start();
        System.out.println(ptimer.createReleaseParameters());
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            getFireTimeResult = ptimer.getFireTime();
            Assert.check(getFireTimeResult.equals(absStartTime.add(i*period.getMilliseconds(), 0)), "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime + " - iteration: " + i);
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }
        ptimer.destroy();

        /*---*/

        System.out.println("Test " + (test++) + ": Periodic - absolute start time in future, with change in interval after start");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        ptimer = new PeriodicTimer(absStartTime, period, handler);
        //        if (debug) ptimer.ABS_DEBUG = true;
        Assert.check(ptimer.getInterval().equals(period), "interval != period");
        getFireTimeResult = ptimer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("First Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");
        ptimer.start();
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            getFireTimeResult = ptimer.getFireTime();
            Assert.check(getFireTimeResult.equals(absStartTime.add(i*period.getMilliseconds(), 0)), "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime + " - iteration: " + i);
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }
        long oldPeriod = period.toNanos();
        period.add(millis, 0, period);
        ptimer.setInterval(period);
        Assert.check(ptimer.getInterval().equals(period), "interval != period");
        for (int i = 0; i < PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (absStartTime.toNanos() + i*period.toNanos() + 
                               PERIODS*oldPeriod);
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }

        ptimer.destroy();

        period.subtract(new RelativeTime(millis, 0), period);

        /*---*/

        System.out.println("Test " + (test++) + ": Periodic - relative start");

        relStartTime.set(millis, 0);
        ptimer = new PeriodicTimer(relStartTime, period, handler);
        System.out.println(ptimer.createReleaseParameters());
        //        if (debug) ptimer.ABS_DEBUG = true;
        Assert.check(ptimer.getInterval().equals(period), "interval != period");
        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for approx: " + absStartTime + 
                            " (now+" + millis + "ms)");
        ptimer.start();
        System.out.println(ptimer.createReleaseParameters());
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.compareTo(absStartTime)>=0, "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            getFireTimeResult = ptimer.getFireTime();
            Assert.check(getFireTimeResult.compareTo(absStartTime.add(i*period.getMilliseconds(), 0))>=0, "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime + " - iteration: " + i);
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }

        ptimer.destroy();


        System.out.println("Test " + (test++) + ": Periodic - relative start with relative reschedule and restart");

        relStartTime.set(millis, 0);
        ptimer = new PeriodicTimer(relStartTime, period, handler);
        //        if (debug) ptimer.ABS_DEBUG = true;
        Assert.check(ptimer.getInterval().equals(period), "interval != period");
        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for approx: " + absStartTime + 
                            " (now+" + millis + "ms)");
        ptimer.start();
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.compareTo(absStartTime)>=0, "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            getFireTimeResult = ptimer.getFireTime();
            Assert.check(getFireTimeResult.compareTo(absStartTime.add(i*period.getMilliseconds(), 0))>=0, "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime + " - iteration: " + i);
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }

        relStartTime.set(millis,0);
        ptimer.reschedule(relStartTime);
        System.out.println("reschedule() while active should have no effect");
        // check new firing was unaffected
        for (int i = 1; i <= 1; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (absStartTime.toNanos() + PERIODS*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Next Firing delay: " + diffMillis + "ms");
        }
        ptimer.stop();
        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        ptimer.start();
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.compareTo(absStartTime)>=0, "wrong fire time after restart - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        System.out.println("Restarted after reschedule - expected firing at :"
                           + absStartTime);
        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }

        ptimer.destroy();


        /*---*/

        System.out.println("Test " + (test++) + ": Periodic - relative start with absolute reschedule and restart");

        relStartTime.set(millis, 0);
        ptimer = new PeriodicTimer(relStartTime, period, handler);
        //        if (debug) ptimer.ABS_DEBUG = true;
        Assert.check(ptimer.getInterval().equals(period), "interval != period");
        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        System.out.println("Current time is:   " + now);
        System.out.println("Fire time set for approx: " + absStartTime + 
                            " (now+" + millis + "ms)");
        ptimer.start();
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.compareTo(absStartTime)>=0, "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            getFireTimeResult = ptimer.getFireTime();
            Assert.check(getFireTimeResult.compareTo(absStartTime.add(i*period.getMilliseconds(), 0))>=0, "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime + " - iteration: " + i);
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }

        rtc.getTime(now);
        now.add(millis*2,0, reschedAbs);
        ptimer.reschedule(reschedAbs);
        System.out.println("reschedule() while active should have no effect");
        // check next firing unaffected
        for (int i = 1; i <= 1; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (absStartTime.toNanos() + PERIODS*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Next Firing delay: " + diffMillis + "ms");
        }
        ptimer.stop();
        ptimer.start();
        System.out.println("Restarted after reschedule - expected firing at :"
                           + reschedAbs);
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.equals(reschedAbs), "wrong fire time after restart - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +reschedAbs);
        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (reschedAbs.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }


        ptimer.destroy();

        /*---*/

        System.out.println("Test " + (test++) + ": Periodic - absolute start with absolute reschedule and restart");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        ptimer = new PeriodicTimer(absStartTime, period, handler);
        //        if (debug) ptimer.ABS_DEBUG = true;
        Assert.check(ptimer.getInterval().equals(period), "interval != period");
        getFireTimeResult = ptimer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("First Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");
        ptimer.start();
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            getFireTimeResult = ptimer.getFireTime();
            Assert.check(getFireTimeResult.equals(absStartTime.add(i*period.getMilliseconds(), 0)), "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime + " - iteration: " + i);
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }

        rtc.getTime(now);
        now.add(millis*2,0, reschedAbs); // millis*2 as we wait for 1 period
        ptimer.reschedule(reschedAbs);
        System.out.println("reschedule() while active should have no effect");
        // check next firing unaffected
        for (int i = 1; i <= 1; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (absStartTime.toNanos() + PERIODS*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Next Firing delay: " + diffMillis + "ms");
        }
        ptimer.stop();
        ptimer.start();
        System.out.println("Restarted after reschedule - expected firing at :"
                           + reschedAbs);
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.equals(reschedAbs), "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +reschedAbs);
        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (reschedAbs.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }


        ptimer.destroy();

        /*---*/

        System.out.println("Test " + (test++) + ": Periodic - absolute start with relative reschedule and restart");

        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        ptimer = new PeriodicTimer(absStartTime, period, handler);
        //        if (debug) ptimer.ABS_DEBUG = true;
        Assert.check(ptimer.getInterval().equals(period), "interval != period");
        getFireTimeResult = ptimer.getExpectedFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong initial fire time - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        System.out.println("Current time is:   " + now);
        System.out.println("First Fire time set for: " + absStartTime + 
                            " (now+" + millis + "ms)");
        ptimer.start();
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.equals(absStartTime), "wrong fire time after start - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);

        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            getFireTimeResult = ptimer.getFireTime();
            Assert.check(getFireTimeResult.equals(absStartTime.add(i*period.getMilliseconds(), 0)), "wrong fire time after firing - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime + " - iteration: " + i);
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }

        relStartTime.set(millis,0);
        ptimer.reschedule(relStartTime);
        System.out.println("reschedule() while active should have no effect");
        // check new firing was unaffected
        for (int i = 1; i <= 1; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (absStartTime.toNanos() + PERIODS*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Next Firing delay: " + diffMillis + "ms");
        }
        ptimer.stop();
        rtc.getTime(now);
        now.add(millis,0, absStartTime);
        ptimer.start();
        getFireTimeResult = ptimer.getFireTime();
        Assert.check(getFireTimeResult.compareTo(absStartTime)>=0, "wrong fire time after restart - getFireTimeresult: " + getFireTimeResult + " vs. abs fire time: " +absStartTime);
        System.out.println("Restarted after reschedule - expected firing at :"
                           + absStartTime);
        for (int i = 1; i <= PERIODS; i++) {
            while (!fired) {
                RealtimeThread.yield();
            }
            fired = false;
            diff = fireTime - (absStartTime.toNanos() + (i-1)*period.toNanos());
            diffMillis = diff / (1000*1000);
            System.out.println("Firing delay[" + i+ "]: " + diffMillis + "ms");
        }


        ptimer.destroy();

    }
}

                         




