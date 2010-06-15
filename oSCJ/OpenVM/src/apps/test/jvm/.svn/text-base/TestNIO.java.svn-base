package test.jvm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author Christian Grothoff
 */
public class TestNIO extends TestBase {
    
    public TestNIO(Harness domain, 
		   long disabled) {
	super("NIO", domain);
    }

    public static void main(String[] args) throws IOException {
	new TestNIO(new Harness() {
		public void print(String s) {
		    System.out.print(s);
		}
		public String getDomain() {
		    return "user-domain-JVM";
		}
		public void exitOnFailure() {
		    if (failures > 0) {
                        System.exit(failures);
		    }
		}
	    }, 0).testTCPChain(new Integer(args[0]).intValue(),
			       new Integer(args[1]).intValue(),
			       new Integer(args[2]).intValue());
    }

    public void run() {
	try {
	    java.net.InetAddress.getByName("localhost");
	} catch (Exception e) {
	    e.printStackTrace();
	    fail(e.toString());
	}
	try {
	    testTCPChain(1024, 64, 10000);
	    testNonblockingAccept();
	} catch (IOException io) {
	    fail(io.toString());
	}
    }


    /**
     * @param mlen size of the individual messages
     * @param clen length of the tcp chain
     * @param mcnt number of total messages transmitted
     */
    public void testTCPChain(final int mlen,
			     final int clen,
			     final int mcnt) 
	throws IOException  {
	//long start = System.currentTimeMillis();
	final ServerSocketChannel[] sscs
	    = new ServerSocketChannel[clen];
	final SocketChannel[] accs
	    = new SocketChannel[clen];
	final SocketChannel[] cons
	    = new SocketChannel[clen];
	Thread[] ts
	    = new Thread[clen];
	for (int i=0;i<clen;i++) {
	    sscs[i] 
		= ServerSocketChannel.open();
	    sscs[i].socket().bind(new InetSocketAddress(i+12345)); 
	    final int ii = i;
	    ts[i] = new Thread() {
		    public void run() {
			try {
			    cons[ii] = SocketChannel.open
				(new InetSocketAddress("localhost", ii+12345));
			    ByteBuffer buffer = ByteBuffer.wrap(new byte[mlen]);
			    for (int k=0;k<mcnt;k++) {
				buffer.rewind();
				while (buffer.hasRemaining()) 
				    cons[ii].read(buffer);
				if (ii < clen-1) {
				    buffer.rewind();
				    if (mlen != accs[ii+1].write(buffer))
					throw new Error("Assertion failed: short write");
				}
			    }
			} catch (IOException io) {
			    fail(io.toString());
			}
		    }
		};	
	    ts[i].start();	    
	}
	for (int i=0;i<clen;i++) 
	    accs[i] = sscs[i].accept();
	ByteBuffer source 
	    = ByteBuffer.wrap(new byte[mlen]);
	for (int i=0;i<mlen;i++)
	    source.put((byte)i);
	for (int i=0;i<mcnt;i++) {
	    source.rewind();
	    if (mlen != accs[0].write(source))
		throw new Error("Assertion failed: short write");
	}
	try {
	    for (int i=0;i<clen;i++) 
		ts[i].join();
	} catch (InterruptedException ie) {
	    fail(ie.toString());
	}
	for (int i=0;i<clen;i++) {
	    sscs[i].close();
	    accs[i].close();
	    cons[i].close();
	}
	//d("TCP Chain("+mlen+","+clen+","+mcnt+") took "
	//  + (System.currentTimeMillis() - start) + " ms");
    }



    public void testNonblockingAccept() 
	throws IOException {
	int port = 1234;
	ServerSocketChannel ssc
	    = ServerSocketChannel.open();
	ssc.socket().bind(new InetSocketAddress(port));
	ssc.configureBlocking(false);
	Thread t = new ConnectThread();
	t.start();
	long now = System.currentTimeMillis();
	SocketChannel sc = ssc.accept();
	if (System.currentTimeMillis() - now > 4000) {
	    fail("accept was not non-blocking!");
	} else {
	    t.interrupt();
	    if (sc != null) {
		//d("Strange, accept succeeded early...");
		sc.close();
	    }
	}
	ssc.close();
    }
      
    static class ConnectThread 
	extends Thread {
	public void run() {
	    try {
		synchronized(this) {
		    this.wait(5000);
		}
	    } catch (InterruptedException ie) {
		return; // the normal thing!
	    }
	    try {
		SocketChannel sc = SocketChannel.open
		    (new InetSocketAddress("localhost", 1234));
		sc.close();
	    } catch (IOException io) {
	    }
	}
    }

}