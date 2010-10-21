package crossScope;



public class TestBigInteger {

	
	public void foo() {
		BigInteger myBI = new BigInteger("1");
		BigInteger one = BigInteger.ONE;

		myBI.add(one);
		
	}
	
}
