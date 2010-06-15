package javax.safetycritical;

import javax.safetycritical.annotate.SCJAllowed;


/**
 * In SCJ, all schedulable objects are managed by a mission. 
 * 
 * @author plsek
 *
 */
@SCJAllowed
public interface ManagedSchedulable extends Schedulable{
	
	/**
	 * Register this schedulable object with the current mission.
	 */
	@SCJAllowed 
	public void register();
	
	/**
	 * Runs any end-of-mission clean up code associated with this schedulable object.
	 */
	@SCJAllowed 
	public void cleanUp();
}
