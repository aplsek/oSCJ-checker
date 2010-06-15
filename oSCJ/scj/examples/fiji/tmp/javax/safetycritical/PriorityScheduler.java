package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class PriorityScheduler extends javax.realtime.PriorityScheduler {

	/**
	 * 
	 * @return Returns the maximum hardware real-time priority supported by this virtual machine. @SCJAllowed
	 */
	@SCJAllowed 
	public int getMaxHardwarePriority() {
		//TODO:
		return 0;
	}
	
	
	/**
	 * Returns the minimum hardware real-time priority supported by this virtual machine.
	 */
	@SCJAllowed 
	public int getMinHardwarePriority() {
		//TODO:
		return 0;
	} 
}
