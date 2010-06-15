// $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestDNS.java,v 1.6 2006/04/08 21:08:16 baker29 Exp $

package test.runtime;

import s3.services.threads.JLThread;
import test.common.TestSuite;
import ovm.core.execution.*;

/**
 *
 * @author Filip Pizlo
 */
public class TestDNS extends TestSyncBase {
    boolean doThrow;
    
    public TestDNS(long disabled) {
	super("ED DNS Lookup");
	doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }
    
    protected boolean needsIsolation() { return true; }
    
    public static final int ITERATIONS = 1;
    
    private void printIPs(byte[][] ip) {
        if (ip==null) {
            d("errno = "+LibraryImports.getErrno());
            d("h_errno = "+LibraryImports.getHErrno());
	    if (false)
		COREfail("got an error.");
	    else
		return;
        }
        for (int i=0;i<ip.length;++i) {
            p("IP address: ");
            for (int j=0;j<ip[i].length;++j) {
                if (j!=0) {
                    p(".");
                }
                p(""+(ip[i][j]&0xff));
            }
            d("");
        }
    }
    
    public void run() {
        if (dispatcher == null ||
            !(dispatcher.getCurrentThread() instanceof JLThread)) {
            p(" SKIPPED: not working with JLThreads");
            return;
        }
        if (!doThrow) {
            p(" SKIPPED: requires exceptions");
            return;
        }
	
	for (int iter=0;
	     iter<ITERATIONS;
	     ++iter) {
            d("Lookup localhost...");
            byte[][] ips=LibraryImports.getHostByName("localhost");
            printIPs(ips);
            boolean ok=false;
            for (int i=0;i<ips.length;++i) {
                if (ips[i][0]==(byte)127 &&
                    ips[i][1]==(byte)0 &&
                    ips[i][2]==(byte)0 &&
                    ips[i][3]==(byte)1) {
                    ok=true;
                }
                if (ok) {
                    break;
                }
            }
            check_condition(ok,"127.0.0.1 not one of the IPs for localhost");
            
            d("Lookup www.google.com...");
            printIPs(LibraryImports.getHostByName("www.google.com"));
            
            d("Lookup somewhere.nowhere.notadomain...");
            check_condition(null==LibraryImports.getHostByName("somewhere.nowhere.notadomain"),
                       "null not returned for lookup of somewhere.nowhere.notadomain");
            d("Not found (expected).");
            
            d("Lookup 127.0.0.1...");
            String h=LibraryImports.getHostByAddr(new byte[]{127,0,0,1},
                                                  NativeConstants.AF_INET);
            if (h==null) {
                d("errno = "+LibraryImports.getErrno());
                d("h_errno = "+LibraryImports.getHErrno());
                COREfail("null returned for lookup of 127.0.0.1");
            }
            d("Found: "+h);
            check_condition(h.equals("localhost"),"127.0.0.1 does not resolve to localhost");
	}
    }
}

