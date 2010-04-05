/**
 **/
package test.common;
import test.common.TestBase;

public class TestString 
    extends TestBase {

    public TestString() {
	super("String");
    }
    public void run() {
	testIndexing();
	testLength();
	testPlusEquals();
    }

    public void testLength() {
	check_condition("".length() == 0);
	check_condition("xyz".length() == 3);
    }

    public void testIndexing() {
	String s = new String("0123456789");
	for (int i = 0; i < 10; i++) {
	    check_condition(s.charAt(i) == (char)('0' + i));
	}
    }
    public void testPlusEquals() {
	check_condition(("" + null).equals("null"));
	check_condition(("this" + "that").equals("thisthat"));
    }

	
}
