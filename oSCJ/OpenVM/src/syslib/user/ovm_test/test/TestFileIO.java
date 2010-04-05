package test;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;


// FIXME: this is a JVM level test NOT a basic user-domain test
public class TestFileIO extends TestBase {
    boolean doThrow;
    
    public TestFileIO(Harness domain, long disabled) {
	super("FileIO", domain);
	doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }
    
    public void run() {
	testFileInputStream();
	testFileLength();
	if (doThrow)
	    testMissing();
    }

    // need to change these for non-UNIX-like systems
    static final String TEST_FILE = "/etc/hosts";
    // second choice as OSX may not have /etc/hosts :(
    static final String TEST_FILE2 = "/etc/group";

    public void testFileLength() {
	File file = new File(TEST_FILE);
        long len = 0;
        if (file.exists()) {
            len = file.length();
            COREassert(len != 0L, "File.length zero");
        }
        else {
            file = new File(TEST_FILE2);
            if (file.exists()) {
                len = file.length();
                COREassert(len != 0L, "File.length zero");
            }
        }
	file = new File("no such file");
	len = file.length();
	COREassert(len == 0L, "nonexistant file has nonzero length: " + len);
    }

    public void testFileInputStream() {
	try {
	    File file = new File(TEST_FILE);
            if (!file.exists()) {
                file = new File(TEST_FILE2);
                COREassert(file.exists(), "Can't find " + TEST_FILE + " or " +
                           TEST_FILE2);
            }	    
	    FileInputStream str = new FileInputStream(file);
            System.out.println(" --- OK: have a file input stream");
	    byte[] buf = new byte[100];
	    int len=str.read(buf);
	    String contents = new String(buf,0,len);
	    System.out.println(" --- OK: read " + contents); 
	    str.close();
	} catch (IOException e) {
	    System.out.println(" --- Failed to read file: " + e);
	}
    }
    public void testMissing() {
	String name = "no such file";
	try {
	    File file = new File(name);
	    FileInputStream str = new FileInputStream(file);
	    str.close();
	} catch (IOException e) {
	    System.err.println(" --- OK: didn't find " + name + ": " + e);
	}
    }

}
