// $Header: /p/sss/cvs/OpenVM/src/test/runtime/TcpSocketUtil.java,v 1.6 2004/10/13 06:12:14 pizlofj Exp $

package test.runtime;

import test.common.*;
import ovm.core.execution.NativeConstants;
import s3.services.threads.JLThread;

/**
 *
 * @author Filip Pizlo
 */
class TcpSocketUtil {
    static class sockaddr_in {
        int family;
        int addr;
        int port;
    }

    protected static final class StatusBits {
        public boolean done=false;
    }
    
    static void createTcpSocket(final TestBase b,
				final int[] pipe) {
	createTcpSocket(b,pipe,false,false);
    }
    
    static void createAndCloseTcpSocket(final TestBase b,
					final boolean delayAccept,
					final boolean delayConnect) {
	int[] pipe=new int[2];
	createTcpSocket(b,pipe,delayAccept,delayConnect);
	LibraryImports.close(pipe[0]);
	LibraryImports.close(pipe[1]);
    }
    
    static void createTcpSocket(final TestBase b,
				final int[] pipe,
				final boolean delayAccept,
				final boolean delayConnect) {
        final int localHost=(127<<24)|(0<<16)|(0<<8)|(1);
        final int myPort=1633;  // 163 is 's' in ascii, and 3 is, well... 3.
        
        final int serverSock=
            LibraryImports.socket(NativeConstants.AF_INET,
                                  NativeConstants.SOCK_STREAM,
                                  0);
        b.check_err(serverSock>=0,"Socket creation");
        
        b.check_err(LibraryImports.setSoReuseAddr(serverSock,true)>=0,
                "Setting the reuseAddr flag");
        
        sockaddr_in serverAddr=new sockaddr_in();
        serverAddr.family=NativeConstants.AF_INET;
        serverAddr.addr=0;
        serverAddr.port=myPort;
        b.check_err(LibraryImports.bind(serverSock,serverAddr)>=0,
                "Binding the socket to port "+myPort);
        
        b.check_err(LibraryImports.listen(serverSock,1)>=0,
                "Setting the socket's listen queue to 1");
        
        final StatusBits bits=new StatusBits();
        
        new JLThread(){
            public void run() {
                try {
                    sockaddr_in acceptAddr=new sockaddr_in();
		    
		    if (delayAccept) {
			try {
			    JLThread.sleep(1000);
			} catch (InterruptedException e) {
			    b.COREfail("interrupted while delaying accept");
			}
		    }
		    
                    pipe[0]=LibraryImports.accept(serverSock,acceptAddr,true);
		    
                    b.check_err(pipe[0]>=0,
                            "Accepting on a server socket");
                    b.check_condition(acceptAddr.family
                                 == NativeConstants.AF_INET,
                                 "Address family returned from accept is not "+
                                 "AF_INET");
                    b.check_condition(acceptAddr.addr
                                 == localHost,
                                 "IP address returned from accept is not "+
                                 "127.0.0.1");
                } catch (Throwable e) {
		    b.COREfail("Error in accept thread: "+e);
                } finally {
                    bits.done=true;
                }
            }
        }.start();
        
        pipe[1]=LibraryImports.socket(NativeConstants.AF_INET,
                                      NativeConstants.SOCK_STREAM,
                                      0);
        b.check_err(pipe[1]>=0,"Socket creation");
        
        sockaddr_in clientAddr=new sockaddr_in();
        clientAddr.family=NativeConstants.AF_INET;
        clientAddr.addr=localHost;
        clientAddr.port=myPort;
	
	if (delayConnect) {
	    try {
		JLThread.sleep(1000);
	    } catch (InterruptedException e) {
		b.COREfail("interrupted while delaying connect");
	    }
	}
	
        b.check_err(LibraryImports.connect(pipe[1],clientAddr)>=0,
                "Connection failed for some reason");
        
        try {
            JLThread.sleep(1000);
	    if (delayAccept) {
		JLThread.sleep(1000);
	    }
        } catch (InterruptedException e) {
            b.d("Interrupted!  (Didn't expect that...)");
        }
        
        b.check_condition(bits.done,
                     "Accept thread not done");
        
        LibraryImports.close(serverSock);
    }
}

