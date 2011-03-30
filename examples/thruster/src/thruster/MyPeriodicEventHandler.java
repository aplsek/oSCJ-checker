package thruster;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.AperiodicEvent;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.StorageParameters;

/**
 * This PEH will be released for 10 times.
 * At the 5th release, it fires the AE of the APEH.
 * It terminates the mission at the 10th release.
 *
 * @author Lilei Zhai
 *
 */
public class MyPeriodicEventHandler extends PeriodicEventHandler {

	public AperiodicEvent myAPE = null;
	private int releaseCounter = 0;

	public MyPeriodicEventHandler(PriorityParameters priority,
			PeriodicParameters release, StorageParameters storage, long memSize, String name) {
		super(priority, release, storage, name);
	}

	@Override
    public void handleAsyncEvent() {

		releaseCounter++;
		System.out.println("TestCase 09: PASS. PEH is released for the "+releaseCounter+" time.");

		if(releaseCounter == 1) {

			ManagedMemory curManagedMem = ManagedMemory.getCurrentManagedMemory();
			if(curManagedMem instanceof PrivateMemory) {
				System.out.println("TestCase 10: PASS. PrivateMemory is the current memory of PEH."+((PrivateMemory)curManagedMem).toString());

				curManagedMem.enterPrivateMemory(10000, new Runnable() {
					public void run() {
						ManagedMemory curManagedMem = ManagedMemory.getCurrentManagedMemory();
						if(curManagedMem instanceof PrivateMemory)
							System.out.println("TestCase 11: PASS. Nested PrivateMemory of PEH is entered. "+((PrivateMemory)curManagedMem).toString());
						else
							System.out.println("TestCase 11: FAIL. Nested ManagedMemory of PEH should be PrivateMemory . "+((PrivateMemory)curManagedMem).toString());
					}
				});

				curManagedMem.enterPrivateMemory(10000, new Runnable() {
					public void run() {
						ManagedMemory curManagedMem = ManagedMemory.getCurrentManagedMemory();
						if(curManagedMem instanceof PrivateMemory)
							System.out.println("TestCase 12: PASS. Nested PrivateMemory of PEH is entered again. "+((PrivateMemory)curManagedMem).toString());
						else
							System.out.println("TestCase 12: FAIL. Nested ManagedMemory of PEH should be PrivateMemory. "+((PrivateMemory)curManagedMem).toString());
					}
				});

				Object privateMemPortalObj = new Object();
				PrivateMemory curPrivateMemory = (PrivateMemory)ManagedMemory.getCurrentManagedMemory();
				curPrivateMemory.setPortal(privateMemPortalObj);
				if(curPrivateMemory.getPortal().toString().compareTo(privateMemPortalObj.toString()) == 0) {
					System.out.println("TestCase 13: PASS. PEH PrivateMemory.setPortal & PrivateMemory.getPortal "
							+ curPrivateMemory.getPortal().toString());
				}
				else {
					System.out.println("TestCase 13: FAIL. PEH PrivateMemory.setPortal & PrivateMemory.getPortal "
							+ curPrivateMemory.getPortal().toString());
				}
			}
			else {
				System.out.println("TestCase 10: FAIL. Current memory of PEH should be PrivateMemory."+((PrivateMemory)curManagedMem).toString());
			}
		}
		else if(releaseCounter == 5) {
				myAPE.fire();
		}
		else if(releaseCounter == 10) {
			Mission.getCurrentMission().requestTermination();
		}

	}

	public void cleanup() {
		System.out.println("TestCase 19: PASS. PEH.cleanup() is executed.");
	}

}
