package crossScope_A.scjlib;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;

public class TestClock {
	
	public void testClock() {
		
		Clock cl = Clock.getRealtimeClock();           // ERROR, should be @Allocate(static/immortal)
															//  -- inferrint type to be immortal, singleton!!
											
		AbsoluteTime dest = null;
		AbsoluteTime ab = cl.getTime(dest);      // OK, will return the same scope as the dest is!
	}
	
	/*
	@SCJRestricted(mayAllocate = false)
	getRealtimeClock()


	Clock cl = Clock().getRealtimeClock();
	   --> singleton instance
	   ---> cl has which scope? Immortal? ---> scope inference?
	   ---> annotated as what?
	   
	AbsoluteTime ab =  cl.getTime(); 
	   ---> allocated in current scope
	   ---> can be solved by annotating getTime with @Allocate(current)

	    @returns - the instance of AbsoluteTime passed as parameter or null
		@SCJAllowed
		@SCJRestricted(maySelfSuspend = false, mayAllocate = false)
		public abstract AbsoluteTime getTime(AbsoluteTime time);

	AbsoluteTime ab = cl.getTime(AbsoluteTime dest)
		---> is the "dest" stored in the current??
	    ---> solved by "reference immutable"?
	*/
}
