//crossScope_A/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//1 error

package crossScope_A.scjlib;

import javax.safetycritical.PrivateMemory;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;

/**
 * BigDecimal
 * 
 * BigDecimal.ONE one = BigDecimal.ONE; --> one is singleton,
 * 
 * @Allocate({javax.safetycritical.annotate.Allocate.Area.THIS ) 
 * public   BigDecimal( BigInteger val )
 *          ---> Does not allow "this" or local variables.
 *               WHAT THIS MEANS??
 *               
 * @Allocate(current) public BigDecimal add()
 * 
 *  Example:
 * @RunsIn(PrivateMemory) hanldleEvent {
 * 
 *                        BigDecimal myBig = new BigDecimal(); 
 *                        BigDecimal one = BigDecimal.ONE;   // --> inferred @Scope(Immortal)
 *                        BigDecimal one_loc = myBig.One     //---> scope of one_loc is @Scope(current)
 * 
 *                        myBig.add(one);  //   ----> OK
 * 
 *                        one.add(myBig);   //  ---> here you need to know that "add"
 *                        						method is reference immutable - REF-IMMUTABLE }
 * 
 * @author plsek
 * 
 */
public class TestBigDecimal {

	@DefineScope(name = "Mission", parent = "Immortal")
	PrivateMemory mission = new PrivateMemory(0);

	@DefineScope(name = "PrivateMemory", parent = "Mission")
	PrivateMemory privateMem = new PrivateMemory(0);

	@RunsIn("PrivateMemory")
	public void handleEvent() {

		BigDecimal myBig = new BigDecimal(11);
		BigDecimal one = BigDecimal.ONE; // --> inferred @Scope(Immortal)
		BigDecimal one_loc = myBig.ONE; // ---> scope of one_loc is
										// @Scope(current)

		one = one_loc; // OK, the same object 

		one = myBig;   /// ERROR
		
		
		myBig.add(one); 		// OK!! passing immortal... trivial TODO:----> OK, but add must be @CrossScope or
								// ref-immutable!!!!!

		one.add(myBig); 		// TODO: ---> here you need to know that "add" method
								// is reference immutable - REF-IMMUTABLE
		
		BigDecimal bd = one.add(myBig);
	
	}

}
