package test.runtime;

import ovm.util.ByteBuffer;
import test.common.TestBase;

/**
 * Tests some operations on ByteBuffers, particularly duplicate().
 * @author Chapman Flack
 **/
public class TestByteBuffer
    extends TestBase {
    
    public TestByteBuffer() {
	super("ByteBuffer");
    }

    public void run() {
	testDuplicateGrowableByteBuffer();
    }
    
    public void testDuplicateGrowableByteBuffer() {
	setModule("duplicate growable buffer");
	ByteBuffer a = ByteBuffer.allocate( Integer.MAX_VALUE);
	int i;
	for ( i = 0 ; i < 4095 ; ++ i )
	    a.put( (byte)97);
	check_condition( i == a.position());
	ByteBuffer b = a.duplicate();
	byte[] stuff = { 42, 42, 42 };
	b.put( stuff);
	b.flip();
	a.limit( b.limit());
	check_condition( 3 == a.remaining());
	check_condition( 3*42 == a.get() + a.get() + a.get());
    }
}
