package thruster;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.RelativeTime;
import javax.safetycritical.AperiodicEvent;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.StorageParameters;

/**
 * This class defines the first Mission that the implementation should execute.
 * It contains an APEH and a PEH. The APEH is released by the PEH. Finally the
 * mission is terminated by the PEH.
 *
 * @author Lilei Zhai
 *
 */
public class MyMission extends Mission {
    private MyAperiodicEventHandler myAPEH;
    private MyPeriodicEventHandler myPEH;

    /*
     * The default implementation of this method does nothing. User-defined
     * subclasses may override its implementation.
     */
    @Override
    protected void initialize() {
        /*
         * This method may create and register 1. ManagedEventHandler 2.
         * ManagedThread 3. MissionSequencer that institute this Mission,
         * instantiate and/or initialize certain Mission-level data structure
         */
        System.out
                .println("TestCase 05: PASS. Mission.initialize() is executed.");

        ManagedMemory curManagedMem = ManagedMemory.getCurrentManagedMemory();
        if (curManagedMem instanceof MissionMemory) {
            System.out
                    .println("TestCase 06: PASS. MissionMemory is the current memory of Mission."
                            + ((MissionMemory) curManagedMem).toString());

            Object missionMemPortalObj = new Object();
            MissionMemory curMissionMemory = (MissionMemory) curManagedMem;
            curMissionMemory.setPortal(missionMemPortalObj);
            if (curMissionMemory.getPortal().toString()
                    .compareTo(missionMemPortalObj.toString()) == 0) {
                System.out
                        .println("TestCase 07: PASS. MissionMemory.setPortal & MissionMemory.getPortal");
            } else
                System.out
                        .println("TestCase 07: FAIL. MissionMemory.setPortal & MissionMemory.getPortal");
        } else {
            System.out
                    .println("TestCase 06: FAIL. Current memory of Mission should be MissionMemory."
                            + ((MissionMemory) curManagedMem).toString());
        }

        myPEH = new MyPeriodicEventHandler(new PriorityParameters(
                PriorityScheduler.instance().getNormPriority()),
                new PeriodicParameters(new RelativeTime(), new RelativeTime(
                        500, 0)), new StorageParameters(100, 100, 100), 10000,
                "MyPEH");
        myAPEH = new MyAperiodicEventHandler(new PriorityParameters(
                PriorityScheduler.instance().getMinPriority()),
                new StorageParameters(100, 100, 100), 10000, "MyAPEH");

        // AEH.endHandlerThread() called by handlerThread.
        // The SO who is going to terminate the Mission must have priority
        // higher than NORMAL
        /*
         * System.out.println("MIN: "+PriorityScheduler.instance().getMinPriority
         * ());
         * System.out.println("NOR: "+PriorityScheduler.instance().getNormPriority
         * ());
         * System.out.println("MAX: "+PriorityScheduler.instance().getMaxPriority
         * ()); myPEH = new MyPeriodicEventHandler( new
         * PriorityParameters(PriorityScheduler.instance().getNormPriority()),
         * new PeriodicParameters(new RelativeTime(), new RelativeTime(500, 0)),
         * new StorageParameters(100, 100, 100), 10000, "MyPEH"); myAPEH = new
         * MyAperiodicEventHandler( new
         * PriorityParameters(PriorityScheduler.instance().getMinPriority()),
         * new StorageParameters(100, 100, 100), 10000, "MyAPEH");
         */

        AperiodicEvent myAPE = new AperiodicEvent(myAPEH);

        myPEH.myAPE = myAPE;

        myPEH.register();
        myAPEH.register();

        try {
            ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(1000,
                    new Runnable() {
                        public void run() {
                            System.out
                                    .println("TestCase 08: FAIL. enterPrivateMemory cannot be called in MissionMemory.");
                        }
                    });
        } catch (IllegalStateException e) {
            System.out
                    .println("TestCase 08: PASS. Calling enterPrivateMemory throws IllegalStateException.");
            // e.printStackTrace();
        }

    }

    /*
     * This method returns the desired size of the MissionMemory associated with
     * this Mission.
     */
    @Override
    public long missionMemorySize() {
        System.out
                .println("TestCase 04: PASS. Mission.missionMemorySize() is executed.");
        return 0;
    }

    /*
     * The default implementation of this method does nothing. User-defined
     * subclasses may override its implementation.
     */
    protected void cleanup() {
        System.out.println("TestCase 21: PASS. Mission.cleanup is executed.");
    }

}
