/**
 * Tests static field access
 * @file s3/test/TestStaticFieldAccess.java
 **/
package test.common;
import test.common.TestBase;


/**
 * Test the static field access
 * @author James Liang
 * @author Christian Grothoff
 **/
public class TestStaticFieldAccess 
    extends TestBase {

    public TestStaticFieldAccess() {
	super("StaticFieldAccess");
    }
    
    public void run() {
	setModule("getstatic");
	check_condition(sb == (byte)21,      "found " + sb + " in sb");
	check_condition(sc == (char)3210,    "found " + sc + " in sc");
	check_condition(si == 11,            "found " + si + " in si");
	check_condition(sl == null,          "found " + sl + " in sl");
	check_condition(ss == (short)-17515, "found " + ss + " in ss");
	check_condition(sz == false,         "found " + sz + " in sz");
	check_condition(sj == 3923562356L,   "found " + sj + " in sj");
	check_condition(sd == .41D,          "found " + sd + " in sd");

	setModule("putstatic");
	sb = 29;
	sc = 1992;
	si = 21;
	sl = this;
	ss = -6969;
	sz = true;
	sj = 2537623572356L;
	sd = -05.35325D;
	check_condition(TestStaticFieldAccess.sb == (byte)29);
	check_condition(TestStaticFieldAccess.sc == (char)1992);
	check_condition(TestStaticFieldAccess.si == 21);
	check_condition(TestStaticFieldAccess.sl == this);
	check_condition(TestStaticFieldAccess.ss == (short)-6969); 
	check_condition(TestStaticFieldAccess.sz == true);
 	check_condition(TestStaticFieldAccess.sj == 2537623572356L);
	check_condition(TestStaticFieldAccess.sd == -05.35325D);
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
