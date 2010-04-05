package test.jvm;

import java.io.*;
import java.net.*;

public class TestSocketIO extends TestBase {
    public TestSocketIO(Harness domain) {
	super("Socket IO", domain);
    }

    static final String TEST_STRING = "hello world\n";
    
    public void run() {
	try {
	    final ServerSocket server = new ServerSocket();
	    server.bind(new InetSocketAddress("127.0.0.1", 0));
	    new Thread() {
		public void run() {
		    try {
			Socket client = new Socket(server.getInetAddress(),
						   server.getLocalPort());
			OutputStream o = client.getOutputStream();
			Writer w = new OutputStreamWriter(o);
			w.write(TEST_STRING);
			w.close();
		    } catch (IOException e) {
			fail(e);
		    }
		}
	    }.start();

	    Reader r = new InputStreamReader(server.accept().getInputStream());
	    char [] buf = new char[TEST_STRING.length()];
	    for (int nr = 0; nr < buf.length;
		 nr += r.read(buf, nr, buf.length - nr))
		;
	    String s = new String(buf);
	    check_condition(s.equals(TEST_STRING));
	    check_condition(r.read() == -1, "read past end of test string");
	} catch (IOException e) {
	    fail(e);
	}
    }
}
