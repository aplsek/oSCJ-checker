package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;


import javax.safetycritical.annotate.SCJRestricted;
	


@SCJAllowed
public class PriorityScheduler extends javax.realtime.PriorityScheduler {

	/**
	 * 
	 * @return Returns the maximum hardware real-time priority supported by this virtual machine. @SCJAllowed
	 */
	@SCJAllowed 
	@SCJRestricted()
	public int getMaxHardwarePriority() {
		//TODO:
		return 0;
	}
	
	
	/**
	 * Returns the minimum hardware real-time priority supported by this virtual machine.
	 */
	@SCJAllowed 
	@SCJRestricted()
	public int getMinHardwarePriority() {
		//TODO:
		return 0;
	} 
}
