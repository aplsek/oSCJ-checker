package javax.safetycritical;
import javax.safetycritical.annotate.*;

@SCJAllowed()
public abstract class CyclicExecutive extends Mission implements Safelet {
	/**
	 * Constructor for a Cyclic Executive. Level 0 Applications need to extend
	 * CyclicExecutive and define a getSchedule() method. Level 1 and Level 2
	 * applications should instead extend Safelet.
	 */
	@SCJAllowed()
	public CyclicExecutive() {
	}

	@SCJAllowed()
	/*
	 * @return the schedule to be used by the application. This will typically
	 * be tooling-generated.
	 */
	public abstract CyclicSchedule getSchedule(PeriodicEventHandler[] peh);

	/*
	 * @return the sequencer to be used for the Level 0 application. By default
	 * this is a SingleMissionSequencer, although this method can be overridden
	 * by the application if an alternative sequencer is desired.
	 */
	@SCJAllowed()
	public MissionSequencer getSequencer() {
		return null;
	}

}
