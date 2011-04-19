package sorter;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.CALLER;

import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import sorter.bench.BenchConf;
import sorter.bench.NanoClock;

@SCJAllowed(members = true)
@Scope("Level0App")
@DefineScope(name = "SorterHandler", parent = "Level0App")
public class SorterHandler extends PeriodicEventHandler {

    private Data array[];

    @SCJAllowed()
    @SCJRestricted(INITIALIZATION)
    public SorterHandler(long psize) {
        super(null, null, new StorageParameters(psize, 0, 0), new String("SorterHandler"));

        array = new Data[BenchConf.SIZE];
        for (int i = 0; i < BenchConf.FRAMES; i++) {
            array[i] = new Data();
        }
    }

    private int counter = 0;

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("SorterHandler")
    public void handleAsyncEvent() {
        final long timeBefore = NanoClock.now();

        mix();
        sort();
        final long timeAfter = NanoClock.now();

        BenchConf.timesBefore[BenchConf.recordedRuns] = timeBefore;
        BenchConf.timesAfter[BenchConf.recordedRuns] = timeAfter;
        BenchConf.recordedRuns++;

        counter++;
        if (counter >= BenchConf.FRAMES)
            Mission.getCurrentMission().requestSequenceTermination();
    }

    /**
     * Quicksort algorithm.
     *
     * @param a
     *            an array of Comparable items.
     */
    @RunsIn("SorterHandler")
    private void sort() {
        quicksort(array, 0, array.length - 1);
    }

    @RunsIn("SorterHandler")
    private void mix() {
        for (int i = BenchConf.FRAMES; i < 0; i--) {
            array[i].value = BenchConf.FRAMES - i;
        }
    }

    private static final int CUTOFF = 10;

    /**
     * Internal quicksort method that makes recursive calls. Uses
     * median-of-three partitioning and a cutoff of 10.
     *
     * @param a
     *            an array of Comparable items.
     * @param low
     *            the left-most index of the subarray.
     * @param high
     *            the right-most index of the subarray.
     */
    @RunsIn("SorterHandler")
    private static void quicksort(@Scope("Level0App") Data[] a, int low, int high) {
        if (low + CUTOFF > high)
            insertionSort(a, low, high);
        else {
            // Sort low, middle, high
            int middle = (low + high) / 2;
            if (a[middle].compareTo(a[low]) < 0)
                swapReferences(a, low, middle);
            if (a[high].compareTo(a[low]) < 0)
                swapReferences(a, low, high);
            if (a[high].compareTo(a[middle]) < 0)
                swapReferences(a, middle, high);

            // Place pivot at position high - 1
            swapReferences(a, middle, high - 1);
            Data pivot = a[high - 1];

            // Begin partitioning
            int i, j;
            for (i = low, j = high - 1;;) {
                while (a[++i].compareTo(pivot) < 0)
                    ;
                while (pivot.compareTo(a[--j]) < 0)
                    ;
                if (i >= j)
                    break;
                swapReferences(a, i, j);
            }

            // Restore pivot
            swapReferences(a, i, high - 1);

            quicksort(a, low, i - 1); // Sort small elements
            quicksort(a, i + 1, high); // Sort large elements
        }
    }

    /**
     * Method to swap to elements in an array.
     *
     * @param a
     *            an array of objects.
     * @param index1
     *            the index of the first object.
     * @param index2
     *            the index of the second object.
     */
    @RunsIn("SorterHandler")
    public static final void swapReferences(@Scope("Level0App") Data[] a, int index1, int index2) {
        Data tmp = a[index1];
        a[index1] = a[index2];
        a[index2] = tmp;
    }

    /**
     * Internal insertion sort routine for subarrays that is used by quicksort.
     *
     * @param a
     *            an array of Comparable items.
     * @param low
     *            the left-most index of the subarray.
     * @param n
     *            the number of items to sort.
     */
    @RunsIn("SorterHandler")
    private static void insertionSort(@Scope("Level0App") Data[] a, int low, int high) {
        for (int p = low + 1; p <= high; p++) {
            Data tmp = a[p];
            int j;

            for (j = p; j > low && tmp.compareTo(a[j - 1]) < 0; j--)
                a[j] = a[j - 1];
            a[j] = tmp;
        }
    }

    @Override
    @SCJAllowed(SUPPORT)
    public void cleanUp() {
    }

}

@SCJAllowed(members=true)
@Scope("Level0App")
class Data  {
    public int value;

    @RunsIn(CALLER)
    public int compareTo(Data o) {
        if (o == null || !(o instanceof Data))
            return -1;
        Data elem = o;
        if (this.value > elem.value)
            return +1;
        else if (this.value < elem.value)
            return -1;
        else
            return 0;
    }
}