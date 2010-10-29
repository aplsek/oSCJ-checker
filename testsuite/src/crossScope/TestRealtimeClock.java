package crossScope;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;

public class TestRealtimeClock {
	
	public void foo() {

		Clock c = Clock.getRealtimeClock();
		AbsoluteTime dest = new AbsoluteTime() ;

		
		c.getTime(dest);
		
		
	}
}
