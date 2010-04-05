
package test;

import org.ovmj.transact.*;

public class TestPAR extends TestBase {
    
    boolean doThrow;
    
    int i = 230567; double d = 234.235; long l = 100000233434L;
    byte b = 16; char c = 'a'; String s = "hi";
    char[] ca = new char[] {'a','b','c'}; byte[] ba = new byte[]{1};
    byte [] src = new byte [] {(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6};
    byte [] dst = new byte[src.length];
    
    public TestPAR(Harness domain, long disabled) {
	super("PAR methods", domain);
	doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
	reset();
    }
    
    protected TestPAR(String description, Harness domain) {
	super(description, domain);
	reset();
    }
    
    public void run() {
	if (!doThrow) return;
	if (!LibraryImports.PARenabled()) return;
	String retval;
	COREassert((retval = validate(0)) == null,"Failed init " + retval);
	abortTest();
	COREassert((retval = validate(0)) == null,"Failed abort test " + retval);
	reset();	
	commitTest();
	COREassert((retval = validate(1)) == null,"Failed commit test " + retval);
	reset();
	try { 
	    throwTest(); 
	    COREassert(false, "Failed to throw"); 
	} catch (Error e) {}
	COREassert((retval = validate(1)) == null,"Failed exception test " + retval);
	reset();
	retryTest();
	nestedAbortTest();
	COREassert((retval = validate(0)) == null,"Failed nested abort test " + retval);
	reset();	
	M7(); 
    }
    
    static class ThrowAway {
	char a = 'a';
    }
    
    void commitTest() throws org.ovmj.transact.Atomic {
	i = 32; d= 0.1; l = 10320023343L; b = 8; c = 'v'; s= "ho"; ca[0] = 'c'; ca[1] = 'a'; ca[2] = 'b'; ba[0] = 31;
	System.arraycopy(src, 0, dst, 0, src.length);
    }    

    void abortTest() throws org.ovmj.transact.Atomic {
	i = 32; d= 0.1; l = 10320023343L; b = 8; c = 'v'; s= "ho"; ca[0] = 'c'; ca[1] = 'a'; ca[2] = 'b'; ba[0] = 31;
	System.arraycopy(src, 0, dst, 0, src.length);
	ThrowAway ta = new ThrowAway();
	Transaction.undo(); 
    } 

    void throwTest() throws org.ovmj.transact.Atomic {
	i = 32; d= 0.1; l = 10320023343L; b = 8; c = 'v'; s= "ho"; ca[0] = 'c'; ca[1] = 'a'; ca[2] = 'b'; ba[0] = 31;
	System.arraycopy(src, 0, dst, 0, src.length);
	throw new Error(); // NB: this is the first Error we create in the program and thus run the class initializer.	
    }

    void retryTest() {
	int i = 0;
	try { 
	    Transaction.retry(); 
	    i ++; 
	} catch (AbortedException e) { 
	    i--; 
	}
	COREassert(i != 1, "Failed retry test");
    }
    
    void nestedAbortTest() throws org.ovmj.transact.Atomic {	
	abortTest();   
    }
    
    
    
    void M7() { if (Init.single != null) Transaction.undo(); }
    static private class Init { static Init single = new Init(); }
    
    void reset() {
	i = 230567; 
	d = 234.235; 
	l = 100000233434L; 
	b = 16; 
	c = 'a'; 
	s = "hi"; 
	ca = new char[] {'a','b','c'};	
	ba = new byte[]{1};
	dst = new byte[src.length];
    }
    
    private String validate(int I) {
	if (I == 0) {
	    if ( i != 230567) return "!i ";
	    if ( d < 234.234|| d> 234.236) return "!d "+d;
	    if ( l != 100000233434L) return "!l "+l;
	    if ( b != 16) return "!b ";
	    if ( c != 'a') return "!c ";
	    if ( !s .equals( "hi")) return "!s ";
	    if ( ca[0] != 'a') return "!ca1 ";
	    if ( ca[1] != 'b') return "!ca2 ";
	    if ( ca[2] != 'c') return "!ca3 ";
	    if ( ba[0] != 1) return "!ba0 ";
	    if ( dst[0] != 0 ) return "!dst0 " + dst[0];
	    if ( dst[1] != 0 ) return "!dst1 " + dst[1];
	    if ( dst[2] != 0 ) return "!dst2 " + dst[2];
	    if ( dst[3] != 0 ) return "!dst0 " + dst[3];
	    if ( dst[4] != 0 ) return "!dst1 " + dst[4];
	    if ( dst[5] != 0 ) return "!dst2 " + dst[5];

	} else if (I == 1) {
	    if ( i != 32) return "!i ";
	    if ( d != 0.1) return "!d "+d;
	    if ( l != 10320023343L) return "!l " + l;
	    if ( b != 8) return "!b ";
	    if ( c != 'v') return "!c ";
	    if ( !s .equals( "ho")) return "!s ";
	    if ( ca[0] != 'c') return "!ca1 ";
	    if ( ca[1] != 'a') return "!ca2 ";
	    if ( ca[2] != 'b') return "!ca3 ";
	    if ( ba[0] != 31) return "!ba0 ";
	    if ( dst[0] != 1 ) return "!dst0 " + dst[0];
	    if ( dst[1] != 2 ) return "!dst1 " + dst[1];
	    if ( dst[2] != 3 ) return "!dst2 " + dst[2];
	    if ( dst[3] != 4 ) return "!dst0 " + dst[3];
	    if ( dst[4] != 5 ) return "!dst1 " + dst[4];
	    if ( dst[5] != 6 ) return "!dst2 " + dst[5];
	}
	return null;
    }
}