package sorter.bench;

public class BenchConf {
    public static final int FRAMES = 1 * 1000;
    public static final int SIZE = 1 * 1000;

    public static long          PERIOD              = 50;
    public static long          MISSION_SCOPE_SIZE           = 5*1000*1000;// v 245*1000   250     //5*1000*1000;
    public static long          HANDLER_SCOPE_SIZE           =5*1000*1000;     //63  50     //5*1000*1000;


    private static long[]    traceTime                   = new long[SIZE];

    public static void set(long time, int index) {
        traceTime[index] = time;
    }

    public static void dump() {
        System.out.println("--TRACE BEGIN");
        int i = 0;
        while (i < SIZE) {
            String line= i + " "+ traceTime[i];
            System.out.println(line);
            i++;
        }
        System.out.println("--TRACE END");
    }

}
