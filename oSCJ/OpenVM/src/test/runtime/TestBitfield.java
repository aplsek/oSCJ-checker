package test.runtime;

import ovm.core.services.memory.VM_Word;
import ovm.core.services.memory.VM_Word.Bitfield;
import ovm.util.UnsafeAccess;
import test.common.TestBase;

/**
 * Tests the runtime evaluation of bitfields.
 * @author Chapman Flack
 **/
public class TestBitfield
    extends TestBase implements UnsafeAccess {
    
    static class A extends Bitfield {
      	static final int WIDTH = 5, SHIFT = 0;
	static final Bitfield bf = bf( WIDTH, SHIFT);
    }
    
    static class B extends Bitfield {
      	static final int WIDTH = 5, SHIFT = 8;
	static final Bitfield bf = bf( WIDTH, SHIFT);
    }
    
    static class C extends Bitfield {
      	static final int WIDTH = 5, SHIFT = 16;
	static final Bitfield bf = bf( WIDTH, SHIFT);
    }
    
    static class D extends Bitfield {
      	static final int WIDTH = 1, SHIFT = 31;
	static final Bitfield bf = bf( WIDTH, SHIFT);
    }

    public TestBitfield() {
	super("Bitfield");
    }

    public void run() {
	testGetBitfieldUnshifted();
	testGetBitfieldShifted();
	testSetBitfieldUnshifted();
	testSetBitfieldShifted();
    }
    
    public void testGetBitfieldUnshifted() {
	setModule("bitfield get unshifted");
	VM_Word w = VM_Word.fromInt( 0xa5f19bd5);
	// / a\/ 5\/ f\/ 1\/ 9\/ b\/ d\/ 5\
	// 10100101111100011001101111010101
	//            \ C /   \ B /   \ A /
	
	VM_Word a = VM_Word.fromInt( 0x000015);
	VM_Word b = VM_Word.fromInt( 0x001b00);
	VM_Word c = VM_Word.fromInt( 0x110000);
	
	check_condition( a.EQ( w.unshiftedGet( A.bf)) );
	check_condition( b.EQ( w.unshiftedGet( B.bf)) );
	check_condition( c.EQ( w.unshiftedGet( C.bf)) );
	
	check_condition( 0x000015 == w.unshiftedAsInt( A.bf) );
	check_condition( 0x001b00 == w.unshiftedAsInt( B.bf) );
	check_condition( 0x110000 == w.unshiftedAsInt( C.bf) );
	
	check_condition( 0x000015L == w.unshiftedAsLong( A.bf) );
	check_condition( 0x001b00L == w.unshiftedAsLong( B.bf) );
	check_condition( 0x110000L == w.unshiftedAsLong( C.bf) );
	
	// test that uI2L is really unsigned
	check_condition( 0x80000000L == w.unshiftedAsLong( D.bf) );
    }
    
    public void testGetBitfieldShifted() {
	setModule("bitfield get shifted");
	VM_Word w = VM_Word.fromInt( 0xa5f19bd5);
	// / a\/ 5\/ f\/ 1\/ 9\/ b\/ d\/ 5\
	// 10100101111100011001101111010101
	//            \ C /   \ B /   \ A /
	
	VM_Word a = VM_Word.fromInt( 0x15);
	VM_Word b = VM_Word.fromInt( 0x1b);
	VM_Word c = VM_Word.fromInt( 0x11);
	
	check_condition( a.EQ( w.get( A.bf)) );
	check_condition( b.EQ( w.get( B.bf)) );
	check_condition( c.EQ( w.get( C.bf)) );
	
	check_condition( 0x15 == w.asInt( A.bf) );
	check_condition( 0x1b == w.asInt( B.bf) );
	check_condition( 0x11 == w.asInt( C.bf) );
	
	check_condition( 0x15L == w.asLong( A.bf) );
	check_condition( 0x1bL == w.asLong( B.bf) );
	check_condition( 0x11L == w.asLong( C.bf) );
    }
    
    public void testSetBitfieldUnshifted() {
	setModule("bitfield set unshifted");
	VM_Word w = VM_Word.fromInt( 0);
	
	VM_Word a = VM_Word.fromInt( 0x000015);
	VM_Word b = VM_Word.fromInt( 0x001b00);
	VM_Word c = VM_Word.fromInt( 0x110000);
	
	w = w.unshiftedSet( a, A.bf);
	w = w.unshiftedSet( b, B.bf);
	w = w.unshiftedSet( c, C.bf);
	
	check_condition( w.EQ( VM_Word.fromInt( 0x00111b15)) );
	
	w = w.unshiftedSet( 0x00000e, A.bf);
	w = w.unshiftedSet( 0x001500, B.bf);
	w = w.unshiftedSet( 0x040000, C.bf);
	
	check_condition( w.asInt() == 0x04150e );
	
	w = w.unshiftedSet( 0x000011L, A.bf);
	w = w.unshiftedSet( 0x001500L, B.bf);
	w = w.unshiftedSet( 0x1b0000L, C.bf);
	
	check_condition( w.asInt() == 0x1b1511 );
    }
    
    public void testSetBitfieldShifted() {
	setModule("bitfield set shifted");
	VM_Word w = VM_Word.fromInt( 0);
	
	VM_Word a = VM_Word.fromInt( 0x15);
	VM_Word b = VM_Word.fromInt( 0x1b);
	VM_Word c = VM_Word.fromInt( 0x11);
	
	w = w.set( a, A.bf);
	w = w.set( b, B.bf);
	w = w.set( c, C.bf);
	
	check_condition( w.EQ( VM_Word.fromInt( 0x00111b15)) );
	
	w = w.set( 0x0e, A.bf);
	w = w.set( 0x15, B.bf);
	w = w.set( 0x04, C.bf);
	
	check_condition( w.asInt() == 0x04150e );
	
	w = w.set( 0x11L, A.bf);
	w = w.set( 0x15L, B.bf);
	w = w.set( 0x1bL, C.bf);
	
	check_condition( w.asInt() == 0x1b1511 );
    }
}
