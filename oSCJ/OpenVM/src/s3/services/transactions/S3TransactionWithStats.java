package s3.services.transactions;

import ovm.core.execution.Native;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.threads.OVMThread;
import ovm.util.Arrays;
import s3.core.execution.S3CoreServicesAccess;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;

public class S3TransactionWithStats extends S3Transaction {
    static final int MAX = 10 * 1000 * 1000;
    static private int IDX = 1;    
    static private short[] READS;
    static private short[] WRITES;
    static private short[] INFO;
    static private byte[] EVENTS;
    static private int EVENT_COUNT;
    static private int ABORTS;
    static private int ENTERS;
    static private int MAX_WRITES;
    static private int MAX_READS;
    static private int CUR_WRITES;
    static private int CUR_READS;
    
    public S3TransactionWithStats() {
	READS = new short[MAX]; WRITES = new short[MAX]; INFO = new short[MAX]; EVENTS = new byte[MAX]; int EVENT_COUNT;
    }
    
    static void recordRead() throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers{
	EVENT_COUNT++;	
	CUR_READS++;
	if (S3Transaction.inPAR == 0) {
	    throw new Error();
	}
	if (IDX>=MAX) return;
	READS[IDX-1]++;
    }
    static void recordWrite()throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers {
	EVENT_COUNT++;
	CUR_WRITES++;
	if (IDX>=MAX) return;
	WRITES[IDX-1]++;
    }
    static void recordContextSwitch(OVMThread next) throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers{
	EVENT_COUNT++;
	if (CUR_READS>MAX_READS)MAX_READS=CUR_READS;
	if (CUR_WRITES>MAX_WRITES)MAX_WRITES=CUR_WRITES;
	CUR_READS = CUR_WRITES= 0;
	if (IDX>=MAX) return;
	EVENTS[IDX] = 'c';
	INFO[IDX] = (short)VM_Address.fromObject( next).asInt();//we are loosing bits... oh well.
	IDX++;
    }
    static void recordAbort() throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers{
	EVENT_COUNT++;
	
	//new Error().printStackTrace();
	
	ABORTS++;
	if (IDX>=MAX) return;
	EVENTS[IDX] = 'a';
	IDX++;
    }
    static void recordEnter() throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers{
	EVENT_COUNT++;
	ENTERS++;
	  	if (CUR_READS>MAX_READS)MAX_READS=CUR_READS;
	if (CUR_WRITES>MAX_WRITES)MAX_WRITES=CUR_WRITES;
	CUR_READS = CUR_WRITES= 0;
	if (IDX>=MAX) return;
	EVENTS[IDX] = 'e';
	IDX++;	
  }
    static void recordExit() throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers{
	EVENT_COUNT++;
	if (IDX>=MAX) return;
	EVENTS[IDX] = 'x';
	IDX++;
    }

   public void dumpStats() {	
       Native.print_string("\nTransaction Statistics:\n" + EVENT_COUNT +" events were detected.\n");
       Native.print_string(IDX +" events were logged. (or up to max log size)\n");
	float avg_reads=0, avg_writes=0;
	int num_enters=0, num_aborts=0, num_cs=0, max_reads=0, max_writes=0;
	int median_reads=0, median_writes=0, tot_reads=0, tot_writes=0;
	int[] reads=null;
	int[] writes=null;
	
	
	for (int i=0; i < IDX; i++)  
	    if (EVENTS[i] == 'e') num_enters++;
	
	reads = new int[num_enters];
	writes = new int[num_enters];

	int pos=0;
	for (int i=0; i < IDX; i++) {
	    switch (EVENTS[i]) {
	    case 'c':
		num_cs++;
		break;
	    case 'a':
		num_aborts++;
		break;
	    case 'e':
		reads[pos] = READS[i];
		writes[pos] = WRITES[i];
		tot_reads+= READS[i];
		tot_writes+= WRITES[i];
		if (READS[i]>max_reads) max_reads=READS[i];
		if (WRITES[i]>max_writes) max_writes=WRITES[i];
		pos++;
		break;
	    case 'x':
		break;		
	    }
	}
	if (tot_reads>0) avg_reads = (float)tot_reads/num_enters;
	if (tot_writes>0) avg_writes = (float)tot_writes/num_enters;
	
	Arrays.sort(reads);
	Arrays.sort(writes);
	
	Native.print_string("  Number PAR enters (exact) " + ENTERS);
	Native.print_string("\nNumber PAR aborts (exact) " + ABORTS);
	Native.print_string("\nNumber context switch (exact) " + num_cs);
	Native.print_string("\nMax number of transactional reads (exact) " + MAX_READS);
	Native.print_string("\nMax number of logged writes (exact) " + MAX_WRITES);
	Native.print_string("\nFollowing data is take from a partial log of events: ");
	Native.print_string("\nTotal number of transactional reads " + tot_reads);
	Native.print_string("\nTotal number of logged writes " + tot_writes);
	Native.print_string("\nMax number of transactional reads " + max_reads);
	Native.print_string("\nMax number of logged writes " + max_writes);
	Native.print_string("\nAvg number of in-par reads per PAR " + avg_reads);
	Native.print_string("\nAvg number of logged writes per PAR " + avg_writes);
	if(reads.length>0)Native.print_string("\nMedian number of in-par reads per PAR " + reads[(int)(reads.length/2)]);
	if(writes.length>0)Native.print_string("\nMedian number of logged writes per PAR " + writes[(int)(writes.length/2)]);
					
    }

   
   void doLog(int field, int value) throws PragmaInline,PragmaNoPollcheck,PragmaNoBarriers{	
       S3TransactionWithStats.recordWrite();
       super.doLog(field,value);
   }
   public void undo() throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers  {
       S3TransactionWithStats.recordAbort();
       super.undo();
   }
   public void start(S3CoreServicesAccess csa) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers  {
       S3TransactionWithStats.recordEnter();	
       super.start(csa);
   }
   public void commit() throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers  {
       S3TransactionWithStats.recordExit();
       super.commit();
   }
   public boolean preRunThreadHook(OVMThread currentThread, OVMThread nextThread) throws PragmaNoPollcheck {
       S3TransactionWithStats.recordContextSwitch(nextThread);
       return super.preRunThreadHook(currentThread, nextThread);
   }
   public boolean gatherStatistics() {return true; }
}
