package test.jvm;

/**
 * Test basic execution of shutdown hooks. This is not a JUnit style test but
 * a simple standalone application.
 * <p>Usage:
 * <pre><tt>
 *       TestShutdownHook [-e] [n]
 * </tt></pre>
 * where <tt>-e</tt> requests use of <tt>System.exit</tt> rather than simple
 * termination, and <tt>n</tt> is the number of hook threads to install.
 *
 * @author David Holmes
 */
public class TestShutdownHook {

    static class Hook extends Thread {
        static int num = 0;
        static synchronized int nextNum() { return num++; }

        int id = nextNum();

        public void run() {
            System.out.println("Shutdown hook: " + id + " executing");
        }
    }

    public static void main(String[] args) {

        int nHooks = 1;

        boolean useExit = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-e")) {
                useExit = true;
                continue;
            }
            int n = 0;
            try {
                n = Integer.parseInt(args[i]);
                nHooks = n;
            }
            catch(NumberFormatException ignore) {}
        }

        Runtime rt = Runtime.getRuntime();

        for (int i = 0; i < nHooks; i++) {
            rt.addShutdownHook(new Hook());
        }

        System.out.println("About to terminate ...");
        try { Thread.sleep(3000); } catch (InterruptedException ex) {}

        if (useExit) {
            System.exit(0);
        }
        else {
            return;
        }
    }
}
