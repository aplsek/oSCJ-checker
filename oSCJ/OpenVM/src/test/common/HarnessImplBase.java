// $Header: /p/sss/cvs/OpenVM/src/test/common/HarnessImplBase.java,v 1.4 2004/06/12 23:54:12 jv Exp $

package test.common;

import ovm.core.execution.Native;

/**
 * The base for the two <code>Harness</code> implementations.
 * @author Filip Pizlo
 */
abstract class HarnessImplBase implements Harness {
    String domain;
    boolean allGood = true;

    HarnessImplBase(String domain) {
        this.domain = domain;
    }

    public boolean allGood() {
        return allGood;
    }

    void printBegin(String name) {
        printPrefix();
        Native.print("[Testing ");
        Native.print(name);
        //Native.print("\n");
    }

    void printEnd(String name, boolean ok) {
        if (ok)     Native.print("]\n");
        else        Native.print("!! FAILED ]\n");
    }

    void printFailure(String test, String module, String description) {
        //printPrefix();
        Native.print(test);
        Native.print(": ");
        if (module != null) {
            Native.print(module);
            Native.print(": ");
        }
        Native.print("FAILURE: ");
        Native.print(description);
        Native.print("\n");
    }

    public void printPrefix() {
        Native.print("#<");
        Native.print(domain);
        Native.print("> ");
    }

    void printException() {
        Native.print("#<");
        Native.print(domain);
        Native.print("> EXCEPTION!\n");
    }

    void printError(String message) {
        printPrefix();
        Native.print("ERROR: ");
        Native.print(message);
        Native.print("\n");
    }
}
