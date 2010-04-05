package ovm.services.rusage;

import ovm.core.execution.NativeInterface;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryManager;
import ovm.core.domain.Oop;
import ovm.util.Mem;
import s3.util.PragmaAtomic;

public class RUsage {
    
    public long utime;
    public long stime;
    public long maxrss;
    public long ixrss;
    public long idrss;
    public long isrss;
    public long minflt;
    public long majflt;
    public long nswap;
    public long inblock;
    public long oublock;
    public long msgsnd;
    public long msgrcv;
    public long nsignals;
    public long nvcsw;
    public long nivcsw;
    
    public void toArray(long[] array) {
	array[0]=utime;
	array[1]=stime;
	array[2]=maxrss;
	array[3]=ixrss;
	array[4]=idrss;
	array[5]=isrss;
	array[6]=minflt;
	array[7]=majflt;
	array[8]=nswap;
	array[9]=inblock;
	array[10]=oublock;
	array[11]=msgsnd;
	array[12]=msgrcv;
	array[13]=nsignals;
	array[14]=nvcsw;
	array[15]=nivcsw;
    }
    
    public void setFromArray(long[] array) {
	utime=array[0];
	stime=array[1];
	maxrss=array[2];
	ixrss=array[3];
	idrss=array[4];
	isrss=array[5];
	minflt=array[6];
	majflt=array[7];
	nswap=array[8];
	inblock=array[9];
	oublock=array[10];
	msgsnd=array[11];
	msgrcv=array[12];
	nsignals=array[13];
	nvcsw=array[14];
	nivcsw=array[15];
    }
    
    public long[] toArray() {
	long[] result=new long[16];
	toArray(result);
	return result;
    }
    
    public static RUsage fromArray(long[] array) {
	RUsage result=new RUsage();
	result.setFromArray(array);
	return result;
    }
    
    static final class Nat implements NativeInterface {
	static native void getrusageSelf(long[] array);
    }
    
    private static long[] myArray=new long[16];
    
    public void setFromSystem() throws PragmaAtomic {
	Nat.getrusageSelf(myArray);
	setFromArray(myArray);
    }
    
    public static RUsage fromSystem() {
	RUsage result=new RUsage();
	result.setFromSystem();
	return result;
    }
    
    public void toAnyArray(Oop array) throws PragmaAtomic {
	toArray(myArray);
	MemoryManager.the().copyArrayElements(VM_Address.fromObject(myArray).asOop(),0,
				    array,0,
				    16);
    }
    
    public static void fromSystemToAnyArray(Oop array) throws PragmaAtomic {
	Nat.getrusageSelf(myArray);
	MemoryManager.the().copyArrayElements(VM_Address.fromObject(myArray).asOop(),0,
				    array,0,
				    16);
    }
}

