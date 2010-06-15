/**
 **/
package test;

public class TestString 
    extends TestBase {

    public TestString(Harness domain) {
	super("String", domain);
    }
    public void run() {
	testIndexing();
	testLength();
	testPlusEquals();
    }

    public void testLength() {
	COREassert("".length() == 0);
	COREassert("xyz".length() == 3);
    }

    public void testIndexing() {
	String s = new String("0123456789");
	for (int i = 0; i < 10; i++) {
	    COREassert(s.charAt(i) == (char)('0' + i));
	}
    }
    public void testPlusEquals() {
	COREassert(("" + null).equals("null"));
	COREassert(("this" + "that").equals("thisthat"));
    }

	
}
