package test.jvm;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Christian Grothoff
 */
public class TestDNS extends TestBase {

    boolean doBlocking;
    
    public TestDNS(Harness domain, long disabled) {
        super("DNS", domain);
	doBlocking = (disabled & TestSuite.DISABLE_DNS_LATENCY) == 0;
    }

    // FIXME: This should check if looking up local host name works,
    // but how can we do it, since it needs a way to know the correct
    // answer?
    public void run() {
        testHostForName();
        testHostForName2();
        testHostByAddr();
        testHostByAddr2();
        if (doBlocking)
	    testBlocking();
    }

    public void testHostForName() {
        try {
            InetAddress ovmj = InetAddress.getByName("www.ovmj.org");
           // System.out.println("DNS resolution: www.ovmj.org is " + ovmj);
        } catch (UnknownHostException uhe) {
            System.out.println("ERROR: could not resolve www.ovmj.org: " + uhe);
        }
    }

    public void testHostForName2() {
        try {
            InetAddress ovmj = InetAddress.getByName("non-existant.ovmj.org");
            System.out.println("ERROR: resolved non-existant.ovmj.org to " + ovmj);
        } catch (UnknownHostException uhe) {
            System.out.println("OK: could not resolve non-existant.ovmj.org: " + uhe);
        }
    }

    public void testHostByAddr() {
        try {
            InetAddress ovmj = InetAddress.getByAddress(new byte[] {(byte) 128, (byte) 211, 1, 47 });
            System.out.println("OK: resolved 128.211.1.47 to: " + ovmj);
        } catch (UnknownHostException uhe) {
            System.out.println("ERROR: host by addr failed: " + uhe);
        }
    }

    public void testHostByAddr2() {
        try {
            InetAddress ovmj = InetAddress.getByAddress(new byte[] { 10, 32, 42, 62 });
            //System.out.println("Resolved 10.32.42.62 to: " + ovmj);
        } catch (UnknownHostException uhe) {
            System.out.println("Huh: host by addr failed for 10.32.42.62");
        }
    }

    public void testBlocking() {
        LatencyThread t = new LatencyThread();
        t.start();
        try {
            synchronized (this) {
                this.wait(1000);
            }
        } catch (InterruptedException ie) {
        }
        String[] addrs =
            new String[] {
                "nowhere.anywhere.nu",
                "nonext.uni-wuppertal.de",
                "nonext.faraway.au",
                "do-not-find-me.ru",
                };
        for (int i = 0; i < addrs.length; i++) {
            try {
                InetAddress.getByName(addrs[i]);
                System.out.println("ERROR: resolved " + addrs[i]);
            } catch (UnknownHostException uhe) {
            }
        }
        synchronized (t) {
            t.exit = true;
        }
        try {
            t.join();
        } catch (InterruptedException ie) {
        }
    }

    static class LatencyThread extends Thread {
        boolean exit = false;
        public void run() {
            boolean go = true;
            long badness = 0;
            long last = System.currentTimeMillis();
            while (go) {
                long next = System.currentTimeMillis();
                if (next - last > 1)
                    badness += next - last;
                synchronized (this) {
                    go = exit;
                }
            }
           // System.err.println("DNS blocking accumulated jitter: " + badness);
        }
    }
}
