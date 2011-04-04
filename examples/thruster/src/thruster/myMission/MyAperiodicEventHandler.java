package thruster.myMission;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.PriorityParameters;
import javax.safetycritical.AperiodicEventHandler;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.SCJRunnable;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

/**
 * This APEH is fired by the PEH registered to the same mission.
 *
 * @author Lilei Zhai
 *
 */
@SCJAllowed(value = LEVEL_1, members=true)
@Scope("ThrusterControl")
@DefineScope(name="MyAperiodicEventHandler", parent="ThrusterControl")
public class MyAperiodicEventHandler extends AperiodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public MyAperiodicEventHandler(PriorityParameters priority,
            StorageParameters storage, long memSize, String name) {
        super(priority, null, storage, name);   // TODO: what is the release??
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("MyAperiodicEventHandler")
    public void handleAsyncEvent() {

        //System.out.println("TestCase 14: PASS. APEH is released.");

        Object privateMemPortalObj = new Object();

        @Scope("ThrusterControl")
        @DefineScope(name = "MyAperiodicEventHandler", parent = "ThrusterControl")
        PrivateMemory curPrivateMemory = (PrivateMemory) ManagedMemory
        .getCurrentManagedMemory();
        curPrivateMemory.setPortal(privateMemPortalObj);
        if (curPrivateMemory.getPortal().toString()
                .compareTo(privateMemPortalObj.toString()) == 0) {
            //System.out
            //.println("TestCase 15: PASS. APEH PrivateMemory.setPortal & PrivateMemory.getPortal "
            //        + curPrivateMemory.getPortal().toString());
        } else {
            //System.out
            //.println("TestCase 15: FAIL. APEH PrivateMemory.setPortal & PrivateMemory.getPortal "
            //        + curPrivateMemory.getPortal().toString());
        }

        @DefineScope(name = "MyAperiodicEventHandler", parent = "ThrusterControl")
        @Scope("ThrusterControl")
        ManagedMemory curManagedMem = ManagedMemory.getCurrentManagedMemory();
        if (curManagedMem instanceof PrivateMemory) {
            //System.out
            //.println("TestCase 16: PASS. PrivateMemory is the current memory of APEH."
            //        + ((PrivateMemory) curManagedMem).toString());

            MyRunnable1 run1 = new MyRunnable1();
            curManagedMem.enterPrivateMemory(10000, run1);

            MyRunnable2 run2 = new MyRunnable2();
            curManagedMem.enterPrivateMemory(10000, run2);

        } else {
            //System.out
            //.println("TestCase 16: FAIL. Current memory of APEH should be PrivateMemory."
            //        + ((PrivateMemory) curManagedMem).toString());
        }
    }

    @DefineScope(name = "aperiodic-child-scope-1", parent = "MyAperiodicEventHandler")
    class MyRunnable1 implements SCJRunnable {

        @RunsIn("aperiodic-child-scope-1")
        @SCJAllowed(SUPPORT)
        public void run() {

            @Scope("MyAperiodicEventHandler")
            @DefineScope(name = "aperiodic-child-scope-1", parent = "MyAperiodicEventHandler")
            ManagedMemory curManagedMem = ManagedMemory
            .getCurrentManagedMemory();
            if (curManagedMem instanceof PrivateMemory) {
                //System.out
                //.println("TestCase 17: PASS. Nested PrivateMemory of APEH is entered. "
                //        + ((PrivateMemory) curManagedMem)
                //        .toString());
            }
            else {
                //System.out
                //.println("TestCase 17: FAIL. Nested ManagedMemory of APEH should be PrivateMemory . "
                //        + ((PrivateMemory) curManagedMem)
                //        .toString());
            }
        }
    }

    @DefineScope(name = "aperiodic-child-scope-2", parent = "MyAperiodicEventHandler")
    class MyRunnable2 implements SCJRunnable {

        @RunsIn("aperiodic-child-scope-2")
        @SCJAllowed(SUPPORT)
        public void run() {

            @Scope("MyAperiodicEventHandler")
            @DefineScope(name = "aperiodic-child-scope-2", parent = "MyAperiodicEventHandler")
            ManagedMemory curManagedMem = ManagedMemory
            .getCurrentManagedMemory();
            if (curManagedMem instanceof PrivateMemory) {
                //System.out
                //.println("TestCase 18: PASS. Nested PrivateMemory of APEH is entered again. "
                //        + ((PrivateMemory) curManagedMem)
                //        .toString());
            } else {
                //System.out
                //.println("TestCase 18: FAIL. Nested ManagedMemory of APEH should be PrivateMemory. "
                //        + ((PrivateMemory) curManagedMem)
                //        .toString());
            }
        }
    }

    @Override
    @SCJAllowed(SUPPORT)
    public void cleanUp() {
        //System.out.println("TestCase 20: PASS. APEH.cleanup() is executed.");
    }

}
