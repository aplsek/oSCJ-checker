package thruster.myMission;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.AperiodicEvent;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

/**
 * This PEH will be released for 10 times. At the 5th release, it fires the AE
 * of the APEH. It terminates the mission at the 10th release.
 *
 * @author Lilei Zhai
 *
 */
@SCJAllowed(value = LEVEL_1, members = true)
@Scope("ThrusterControl")
@DefineScope(name = "MyPeriodicEventHandler", parent = "ThrusterControl")
public class MyPeriodicEventHandler extends PeriodicEventHandler {

    public AperiodicEvent myAPE = null;
    private int releaseCounter = 0;

    @SCJRestricted(INITIALIZATION)
    public MyPeriodicEventHandler(PriorityParameters priority,
            PeriodicParameters release, StorageParameters storage,
            long memSize, String name) {
        super(priority, release, storage, name);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("MyPeriodicEventHandler")
    public void handleAsyncEvent() {

        releaseCounter++;
        // System.out.println("TestCase 09: PASS. PEH is released for the "+releaseCounter+" time.");

        if (releaseCounter == 1) {
            @DefineScope(name = "MyPeriodicEventHandler", parent = "ThrusterControl")
            @Scope("ThrusterControl")
            ManagedMemory curManagedMem = ManagedMemory
                    .getCurrentManagedMemory();
            if (curManagedMem instanceof PrivateMemory) {
                // System.out.println("TestCase 10: PASS. PrivateMemory is the current memory of PEH."+((PrivateMemory)curManagedMem).toString());
                Runnable11 run11 = new Runnable11();
                curManagedMem.enterPrivateMemory(10000, run11);

                Runnable12 run12 = new Runnable12();
                curManagedMem.enterPrivateMemory(10000, run12);

                Object privateMemPortalObj = new Object();

                @DefineScope(name = "MyPeriodicEventHandler", parent = "ThrusterControl")
                @Scope("ThrusterControl")
                PrivateMemory curPrivateMemory = (PrivateMemory) ManagedMemory
                        .getCurrentManagedMemory();
                curPrivateMemory.setPortal(privateMemPortalObj);
                if (curPrivateMemory.getPortal().toString()
                        .compareTo(privateMemPortalObj.toString()) == 0) {
                    // System.out.println("TestCase 13: PASS. PEH PrivateMemory.setPortal & PrivateMemory.getPortal "
                    // + curPrivateMemory.getPortal().toString());
                } else {
                    // System.out.println("TestCase 13: FAIL. PEH PrivateMemory.setPortal & PrivateMemory.getPortal "
                    // + curPrivateMemory.getPortal().toString());
                }
            } else {
                // System.out.println("TestCase 10: FAIL. Current memory of PEH should be PrivateMemory."+((PrivateMemory)curManagedMem).toString());
            }
        } else if (releaseCounter == 5) {
            myAPE.fire();
        } else if (releaseCounter == 10) {
            Mission.getCurrentMission().requestTermination();
        }

    }

    @SCJAllowed(value = LEVEL_1, members=true)
    @DefineScope(name = "child-scope11", parent = "MyPeriodicEventHandler")
    class Runnable11 implements Runnable {

        @RunsIn("child-scope11")
        public void run() {

            @DefineScope(name = "child-scope11", parent = "MyPeriodicEventHandler")
            @Scope("MyPeriodicEventHandler")
            ManagedMemory curManagedMem = ManagedMemory
                    .getCurrentManagedMemory();
            if (curManagedMem instanceof PrivateMemory) {
                // System.out.println("TestCase 11: PASS. Nested PrivateMemory of PEH is entered. "+((PrivateMemory)curManagedMem).toString());
            } else {
                // System.out.println("TestCase 11: FAIL. Nested ManagedMemory of PEH should be PrivateMemory . "+((PrivateMemory)curManagedMem).toString());
            }
        }
    }

    @SCJAllowed(value = LEVEL_1, members=true)
    @DefineScope(name = "child-scope12", parent = "MyPeriodicEventHandler")
    class Runnable12 implements Runnable {

        @RunsIn("child-scope12")
        public void run() {
            @DefineScope(name = "child-scope12", parent = "MyPeriodicEventHandler")
            @Scope("MyPeriodicEventHandler")
            ManagedMemory curManagedMem = ManagedMemory
                    .getCurrentManagedMemory();
            if (curManagedMem instanceof PrivateMemory) {
                // System.out.println("TestCase 12: PASS. Nested PrivateMemory of PEH is entered again. "+((PrivateMemory)curManagedMem).toString());
            } else {
                // System.out.println("TestCase 12: FAIL. Nested ManagedMemory of PEH should be PrivateMemory. "+((PrivateMemory)curManagedMem).toString());
            }
        }
    }

    @Override
    @SCJAllowed(SUPPORT)
    public void cleanUp() {
        // System.out.println("TestCase 19: PASS. PEH.cleanup() is executed.");
    }
}
