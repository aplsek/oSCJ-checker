/**
 **/
package test;

/**
 * Test the StringBuffer class.
 * Note that string literals are from the kernel domain
 * but not other strings, so casting may give funny results ...
 * @author Christian Grothoff
 **/
public class TestStringBuffer
    extends TestBase {

    public TestStringBuffer(Harness domain) {
	super("StringBuffer", domain);
    }

    public void run() {
	testSimpleNew();
	testSimpleAppend();
	testSimpleToString();
	testJavacStringPlusInt();
    }

    public void testSimpleNew() {
	setModule("simpleNew");
	StringBuffer sb = new StringBuffer();
	COREassert(sb != null, "null StringBuffer");
    }

    public void testSimpleAppend() {
	setModule("simpleAppend");
	StringBuffer sb = new StringBuffer("Hello");
	sb.append(" World"); 
	COREassert(sb.length() == 11);
    }


    public void testSimpleToString() {
	setModule("simpleToString");
	StringBuffer sb = new StringBuffer("Hello");
	sb.append(" World"); 
	String s = sb.toString();
	if (!s.equals("Hello World")) {
	    p("sb is '"); 
	    p(s);
	    p("' not ");
	    p("'Hello World'\n");
	    COREfail("!");
	}
     }


    public void testJavacStringPlusInt() {
	setModule("javacStringPlusInt");
	int i;
	i = 4;
	i++;
	Object hi = "Hi ";
	String foo = ((String)hi) + i;
	//p("got "); p(foo); p("\n");
	COREassert(foo.equals("Hi 5"));
    }


} // end of TestStringBuffer
