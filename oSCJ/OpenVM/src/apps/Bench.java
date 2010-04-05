import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
//import edu.purdue.util.CommandLineArgument;


public class Bench {

    static Tlock.LList list;
    static Argument arg = new Argument();
    static DataRecord[] results;
    static DataRecord[] warmup_results;
    static Timer time = new Timer();
    static ThreadUtils tu = new ThreadUtils( new Configuration());
    static final int RAW = 0, SYNC = 1, TLF = 2;
    static int CURRENT_MODE = RAW;
    static volatile boolean stop;


    static class Low implements Runnable {
        public void run() {   
            stop = false;
            while (!stop)
                switch (CURRENT_MODE) {
                    case RAW: list.raw_sum(); break;
                    case SYNC:list.s_sum();   break;
                    case TLF: list.t_sum();
    	        }
        }
    }

    static class High implements Runnable {
        public void run() {
            // Warm up

          for (int i = 0; i < arg.REPEAT; i++) {
                while (!RealtimeThread.currentRealtimeThread().waitForNextPeriod())
                    warmup_results[CURRENT_MODE].missed++;
                long before = time.getAbsoluteTime();
                switch (CURRENT_MODE) {
                    case RAW: list.raw_insert(arg.POS); break;
                    case SYNC:list.s_insert(arg.POS);   break;
                    case TLF: list.t_insert(arg.POS); 
                }
                warmup_results[CURRENT_MODE].set(time.getAbsoluteTime() - before);
                
          }

            // Run
            for (int i = 0; i < arg.REPEAT; i++) {
                while (!RealtimeThread.currentRealtimeThread().waitForNextPeriod()) 
                    results[CURRENT_MODE].missed++;
                long before = time.getAbsoluteTime();
                switch (CURRENT_MODE) {
                    case RAW: list.raw_insert(arg.POS); break;
                    case SYNC:list.s_insert(arg.POS);   break;
                    case TLF: list.t_insert(arg.POS); 
                }
                results[CURRENT_MODE].set(time.getAbsoluteTime() - before);
            }
            stop = true; // stop Low pri thread
        }
    }

    static void run() {
        list = new Tlock.LList();
        for (int i = 0; i < arg.POS + 1; i++) list.s_insert(i);
        tu.startRealtimeThread(
            new PriorityParameters(PriorityScheduler.MAX_PRIORITY),
            new PeriodicParameters(new RelativeTime(0, 0),
                new RelativeTime(arg.PERIOD, 0),
			    null, null, null, null),
            new High());
        tu.startRealtimeThread(
           new PriorityParameters(PriorityScheduler.MIN_PRIORITY),
	       null, new Low());
        tu.join();// join on all threads
    }

    static class Argument {//extends CommandLineArgument.Optional {
        int PERIOD = 5;    // High-priority thread period
        int REPEAT = 1000; // How many times repeat the insert
        int POS = 1000; }

    static public void main(String[] args) {
        Tlock.init();
        if (args.length == 0) 
            System.err.println("Args: POS PERIOD REPEAT");
        else {
            arg.POS = Integer.parseInt(args[0]);
            arg.PERIOD = Integer.parseInt(args[1]);
            arg.REPEAT = Integer.parseInt(args[2]);
        }
        System.err.println("POS= " + arg.POS+"\nPERIOD= " + arg.PERIOD+"\nREPEAT= " + arg.REPEAT);
        results = new DataRecord[]{ new DataRecord(),
                new DataRecord(), 
                new DataRecord()};
        warmup_results = new DataRecord[]{ new DataRecord(),
                new DataRecord(), 
                new DataRecord()};
        CURRENT_MODE = RAW;  run();
        CURRENT_MODE = SYNC; run();
        CURRENT_MODE = TLF;  run();
        pln(arg.POS  + "              ; "
                + results[RAW].min / 1000  + " ; "
                + results[RAW].max / 1000  + " ; "
                + results[SYNC].min / 1000  + " ; "
                + results[SYNC].max / 1000  + " ; "
                + results[TLF].min / 1000  + " ; "
                + results[TLF].max / 1000  + "    ; missed = ; "
                + results[RAW].missed + "/" + results[SYNC].missed
                + "/" + results[TLF].missed);
        pln(arg.POS  + "  warmup      ; "
                        + warmup_results[RAW].min / 1000  + " ; "
                        + warmup_results[RAW].max / 1000  + " ; "
                        + warmup_results[SYNC].min / 1000  + " ; "
                        + warmup_results[SYNC].max / 1000  + " ; "
                        + warmup_results[TLF].min / 1000  + " ; "
                        + warmup_results[TLF].max / 1000  + "    ; missed = ; "
                        + warmup_results[RAW].missed + "/" + results[SYNC].missed
                        + "/" + warmup_results[TLF].missed);
       }

    static class DataRecord {
        long count, value, sum, squared_sum, missed;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        double avg;

        public void set(long val) {
            count++;  value = val;
            if (value > max) max = value;
            if (value < min)  min = value;
            sum += value;
            squared_sum += (value * value);
            avg = sum/count;
        }
        public double getStdDev() { return Math.sqrt(
           (squared_sum - ((squared_sum) / count)) / (count - 1));
        }
    }

    static void pln(String s) { System.out.println(s); }
    static void p(String s) { System.out.print(s); }
}
