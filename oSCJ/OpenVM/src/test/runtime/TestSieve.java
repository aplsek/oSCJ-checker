package test.runtime;

import ovm.core.execution.Native;
import ovm.core.services.timer.TimeConversion;
import test.common.TestBase;

public class TestSieve extends TestBase {

    public TestSieve() {
       super("Sieve");
    }

    public void run() {
	int SIZE = 8190;
	boolean flags[] = new boolean[SIZE+1];
	int i, prime, k, count;
	int iterations = 0;
	double seconds = 0.0;
	long startTime, elapsedTime;
	
	startTime = Native.getCurrentTime(); // in nanoseconds
	d("Start: " +startTime + "\n");
	while (true) {
	    count=0;
	    for(i=0; i<=SIZE; i++) flags[i]=true;
	    for (i=0; i<=SIZE; i++) {
		if(flags[i]) {
		    prime=i+i+3;
		    for(k=i+prime; k<=SIZE; k+=prime)
			flags[k]=false;
		    count++;
		}
	    }
	    iterations++;
	    elapsedTime = Native.getCurrentTime() - startTime;
	    if (elapsedTime / (double) TimeConversion.NANOS_PER_SECOND >= 10) break;
	}
	d("End: " + elapsedTime + "\n");
	seconds = elapsedTime / ((double)TimeConversion.NANOS_PER_SECOND);
	d("Iterations: " + iterations + " in " + seconds + " seconds.\n");
	/*
	results1 = iterations + " iterations in " + 
	    elapsedTime + " milliseconds (" + seconds + "s)\n";
	if (count != 1899)
	    results2 = "Error: count <> 1899";
	else
	    results2 = "Sieve score = " + score;
	    p(results1 + results2);*/
    }    

} // end of TestSieve















