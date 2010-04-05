package test.common;

/**
 * A <code>Harness</code> implementation that simulates the way things were.
 *
 * @author Filip Pizlo
 */
public class ClassicHarnessImpl extends HarnessImplBase {
    private boolean testOK = false;

    private FailureCallback cback = new FailureCallback() {
        public void fail(String test, String module, String description) {
            printFailure(test, module, description);
            allGood = false;
            testOK = false;
        }
    };

    public ClassicHarnessImpl(String domain) { super(domain);  }

    public void run(Test t) {
        testOK = true;

        printBegin(t.getName());
        try {
            t.runTest(cback, false);
            printEnd(t.getName(), testOK);
        } catch (Throwable e) {
            allGood = false;
            printException();
            // does this thing even work?
            e.printStackTrace();
            printEnd(t.getName(), false);
        }
    }
}
