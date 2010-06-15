package ovm.core.services.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import s3.services.bootimage.Ephemeral;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;

/**
 * Utility methods for managing subprocesses
 **/
public class HostedProcess implements Ephemeral.Void {
    /**
     * Call out to another program.  Execute the program named by
     * argv[0] in the directory dir, with stdout and stderr appearing
     * on our own System.out and System.err streams.  Wait for the
     * subprocess to finish, and return its exit value.
     *
     * @param dir  the directory in which to run the subprocess
     * @param argv process arguments.
     *
     * @return the subprocess exit value
     *
     * @throws IOException 
     *
     */
    public static int system(String dir, String[] argv) throws IOException, BCbootTime {
	Runtime r = Runtime.getRuntime();
	Process p = r.exec(argv, null, new File(dir));

	class Redirect extends Thread implements Ephemeral.Void {
	    byte[] buf = new byte[2048];
	    InputStream from;
	    OutputStream to;
	    Thread mom = currentThread();
	    
	    Redirect(InputStream f, OutputStream t) {
		from = f; to = t;
	    }
	    public void run() {
		while (true) {
		    try {
			int nr = from.read(buf);
			if (nr == -1) {
			    return;
			}
			to.write(buf, 0, nr);
		    }
		    catch (IOException e) {
			System.err.println("error redirecting process output");
			e.printStackTrace(System.err);
		    }
		}
	    }
	}
	Thread t1 = new Redirect(p.getInputStream(), System.out);
	Thread t2 = new Redirect(p.getErrorStream(), System.err);
	t1.start();
	t2.start();
	while (true) {
	    try {
		t1.join();
		t2.join();
		break;
	    } catch (InterruptedException _) { }
	}
	try {
	    System.err.print("calling Process.exitValue()... ");
	    int ret = p.exitValue();
	    System.err.println("got " + ret);
	    return ret;
	} catch (IllegalThreadStateException e) {
	    System.err.println("\nJVM BUG DETECTED: exitValue failed after " +
			       "subprocess completed.\n" +
			       "About to block indefinitely in " +
			       "Process.waitFor.  Sometimes the JVM can\n" +
			       "recover from this hang if you suspend and " +
			       "resume it from the shell.");
	    while (true) {
		try {
		    p.waitFor();
		    break;
		} catch (InterruptedException _) { }
	    }
	    try {
		return p.exitValue();
	    } catch (IllegalThreadStateException e2) {
		throw new Error("Impossible", e2);
	    }
	}
    }
}
