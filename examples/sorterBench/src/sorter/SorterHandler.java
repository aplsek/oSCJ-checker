package sorter;


import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import sorter.bench.BenchConf;

@SCJAllowed(members=true)
@Scope("Level0App")
@DefineScope(name="SorterHandler", parent="Level0App")
public class SorterHandler extends PeriodicEventHandler {

    private Data array[];


    @SCJAllowed()
    @SCJRestricted(INITIALIZATION)
    public SorterHandler(long psize) {
        super(null, null, null);

        array = new Data[BenchConf.SIZE];
        for (int i= 0; i < BenchConf.FRAMES; i++) {
            array[i] = new Data();
        }
    }

    private int counter = 0;

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("SorterHandler")
    public void handleAsyncEvent() {
        mix();
        sort();

        counter++;
        if (counter > BenchConf.FRAMES)
            Mission.getCurrentMission().requestSequenceTermination();
    }

    @RunsIn("SorterHandler")
    private void sort() {

    }

    @RunsIn("SorterHandler")
    private void mix() {
        for (int i= BenchConf.FRAMES; i < 0; i--) {
            array[i].value = BenchConf.FRAMES - i;
        }
    }


    @Override
    @SCJAllowed()
    public void cleanUp() {
    }


}


class Data {
    public int value;
}