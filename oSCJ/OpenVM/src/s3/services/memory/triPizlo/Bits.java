package s3.services.memory.triPizlo;

import ovm.core.services.memory.*;
import s3.util.*;

class Bits {
    static boolean getBit(int[] array,
			  int idx)
	throws PragmaNoPollcheck {
	return (array[idx/32]&(1<<(idx&31)))!=0;
    }
    
    static void setBit(int[] array,
		       int idx)
	throws PragmaNoPollcheck {
	array[idx/32]|=(1<<(idx&31));
    }
    
    static void clrBit(int[] array,
		       int idx)
	throws PragmaNoPollcheck {
	array[idx/32]&=~(1<<(idx&31));
    }
    
    static void flipBit(int[] array,
			int idx)
	throws PragmaNoPollcheck {
	array[idx/32]^=(1<<(idx&31));
    }
    
    static void setBit(int[] array,
		       int idx,
		       boolean value)
	throws PragmaNoPollcheck {
	if (value) {
	    setBit(array,idx);
	} else {
	    clrBit(array,idx);
	}
    }
    
    static int findSet(int[] array,
		       int n, // = number of words in the bitvector
		       int idx)
	throws PragmaNoPollcheck {
	int wordi=idx/32;
	int val=array[wordi];
	int biti=idx%32;
	val>>>=biti;
	if (val!=0) {
	    for (;biti<32;++biti,val>>>=1) {
		if ((val&1)!=0) {
		    return wordi*32+biti;
		}
	    }
	}
	for (wordi++;wordi<n;++wordi) {
	    val=array[wordi];
	    if (val!=0) {
		for (biti=0;biti<32;++biti,val>>>=1) {
		    if ((val&1)!=0) {
			return wordi*32+biti;
		    }
		}
	    }
	}
	return -1;
    }

    static int findSetIncremental(int[] array,
		       int n, // = number of words in the bitvector
		       int idx,
		       Runnable poller)
	throws PragmaNoPollcheck {
	int wordi=idx/32;
	int val=array[wordi];
	int biti=idx%32;
	val>>>=biti;
	if (val!=0) {
	    for (;biti<32;++biti,val>>>=1) {
		if ((val&1)!=0) {
		    return wordi*32+biti;
		}
	    }
	}
	for (wordi++;wordi<n;++wordi) {
	    val=array[wordi];
	    if (val!=0) {
		for (biti=0;biti<32;++biti,val>>>=1) {
		    if ((val&1)!=0) {
			return wordi*32+biti;
		    }
		}
	    }
	    poller.run();
	}
	return -1;
    }
    
    static int findClr(int[] array,
		       int n, // = number of words in the bitvector
		       int idx)
	throws PragmaNoPollcheck {
	int wordi=idx/32;
	int val=array[wordi];
	int biti=idx%32;
	val>>>=biti;
	if (val!=-1) {
	    for (;biti<32;++biti,val>>>=1) {
		if ((val&1)!=1) {
		    return wordi*32+biti;
		}
	    }
	}
	for (++wordi;wordi<n;++wordi) {
	    val=array[wordi];
	    if (val!=-1) {
		for (biti=0;biti<32;++biti,val>>>=1) {
		    if ((val&1)!=1) {
			return wordi*32+biti;
		    }
		}
	    }
	}
	return -1;
    }
    
    static int findClr(int[] array,
		       int idx)
	throws PragmaNoPollcheck {
	int wordi=idx/32;
	int val=array[wordi];
	int biti=idx%32;
	val>>>=biti;
	if (val!=-1) {
	    for (;biti<32;++biti,val>>>=1) {
		if ((val&1)!=1) {
		    return wordi*32+biti;
		}
	    }
	}
	for (++wordi;;++wordi) {
	    val=array[wordi];
	    if (val!=-1) {
		for (biti=0;biti<32;++biti,val>>>=1) {
		    if ((val&1)!=1) {
			return wordi*32+biti;
		    }
		}
	    }
	}
    }
    
    static int rfindClr(int[] array,
			int idx)
	throws PragmaNoPollcheck {
	int wordi=idx/32;
	int val=array[wordi];
	int biti=idx%32;
	if (val!=-1) {
	    for (;biti>=0;--biti) {
		if (((val>>>biti)&1)!=1) {
		    return wordi*32+biti;
		}
	    }
	}
	for (--wordi;wordi>=0;--wordi) {
	    val=array[wordi];
	    if (val!=-1) {
		for (biti=31;biti>=0;--biti) {
		    if (((val>>>biti)&1)!=1) {
			return wordi*32+biti;
		    }
		}
	    }
	}
	return -1;
    }
    
    static int find(int[] array,
		    int n, // = number of words in the bitvector
		    int idx,
		    boolean value)
	throws PragmaNoPollcheck {
	if (value) {
	    return findSet(array,n,idx);
	} else {
	    return findClr(array,n,idx);
	}
    }
    
    static int getSize(int[] array,
		       int idx) throws PragmaNoPollcheck {
	return findClr(array,idx+1)-idx;
    }
}

