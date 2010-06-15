/**
 * Tests static field access
 * @file s3/test/TestStaticFieldAccess.java
 **/
package test;


/**
 * Test the static field access
 * @author James Liang
 * @author Christian Grothoff
 **/
public class TestStaticFieldAccess 
    extends TestBase {

    public TestStaticFieldAccess(Harness domain) {
	super("StaticFieldAccess", domain);
    }
    
    public void run() {
	setModule("getstatic");
	COREassert(sb == (byte)21,      "found " + sb + " in sb");
	COREassert(sc == (char)3210,    "found " + sc + " in sc");
	COREassert(si == 11,            "found " + si + " in si");
	COREassert(sl == null,          "found " + sl + " in sl");
	COREassert(ss == (short)-17515, "found " + ss + " in ss");
	COREassert(sz == false,         "found " + sz + " in sz");
	COREassert(sj == 3923562356L,   "found " + sj + " in sj");
	COREassert(sd == .41D,          "found " + sd + " in sd");

	setModule("putstatic");
	sb = 29;
	sc = 1992;
	si = 21;
	sl = this;
	ss = -6969;
	sz = true;
	sj = 2537623572356L;
	sd = -05.35325D;
	COREassert(TestStaticFieldAccess.sb == (byte)29);
	COREassert(TestStaticFieldAccess.sc == (char)1992);
	COREassert(TestStaticFieldAccess.si == 21);
	COREassert(TestStaticFieldAccess.sl == this);
	COREassert(TestStaticFieldAccess.ss == (short)-6969); 
	COREassert(TestStaticFieldAccess.sz == true);
 	COREassert(TestStaticFieldAccess.sj == 2537623572356L);
	COREassert(TestStaticFieldAccess.sd == -05.35325D);
   }
    
    // pre-initialized static fields 
    static int si = 11;
    static float sf = (float)25.41;
    static boolean sz = false;
    static long sj = 3923562356L;
    static double sd =.41D; 
    static Object sl = null;
    static byte sb = (byte)21;
    static char sc = (char)3210;
    static short ss = (short)-17515;

} // end of TestStaticFieldAccess
