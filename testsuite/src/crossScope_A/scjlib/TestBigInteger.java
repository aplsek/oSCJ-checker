//crossScope_A/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope_A.scjlib;


public class TestBigInteger {
	public void foo() {
		
		BigInteger myBI = new BigInteger("1");
		BigInteger one = BigInteger.ONE;
		
		BigInteger result = null;
		result = myBI.add(one);					// OK
		one	 = myBI.add(one);					//ERROR
		
	}
	
}
