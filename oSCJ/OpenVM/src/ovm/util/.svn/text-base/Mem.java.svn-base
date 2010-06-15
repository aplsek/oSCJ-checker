package ovm.util;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;
import ovm.core.execution.NativeInterface;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;

import s3.util.PragmaNoPollcheck;

public abstract class Mem {
    /**
     * Copy nb-many bytes from the src to dest.  The arrays may not
     * overlap.  See memcpy(3).
     **/
    public abstract void cpy(VM_Address dest, VM_Address src, int nb);
    /**
     * Copy nb-many bytes from a buffer within the src object to a
     * buffer within the dest object.  The offsets and bounds should
     * be within appropriate ranges.  src and dest need not be
     * protected from GC.  See memcpy(3).
     **/
    public abstract void cpy(Oop dest, int doffset,
			     Oop src, int soffset,
			     int nb);

    public abstract void cpy(Oop dest, int doffset,
			     VM_Address src,
			     int nb);

    /**
     * Copy nb-many bytes from src to dest.  The arrays may overlap.
     * See memmove(3).
     **/
    public abstract void move(VM_Address dest, VM_Address src, int nb);
    /**
     * Copy nb-many bytes from a buffer within the src object to a
     * buffer within the dest object.  The offsets and bounds should
     * be within appropriate ranges.  src and dest may be identical,
     * and need not be protected from GC.  See memmove(3).
     **/
    public abstract void move(Oop dest, int doffset,
			      Oop src, int soffset,
			      int nb);
    /**
     * Initialize nb-many bytes at addr to zero.  See bzero(3).
     **/
    public abstract void zero(VM_Address addr, int nb);
    
    /**
     * Initialize nb-many bytes at addr to zero, occasionally calling
     * the given callback.  Currently only used by the RTGC.
     */
    public abstract void zero(VM_Address addr, int nb, Runnable cback);
    
    // the same as zero, but without pollcheck
    public abstract void zeroAtomic(VM_Address addr, int nb, Runnable cback);
    
    /**
     * Initialize nb-many bytes at offset off from object ref to
     * zero.  ref need not be protected from GC.  See bzero(3).
     **/
    public abstract void zero(Oop ref, int off, int nb);

    /**
     * Initialize nb-many bytes at addr to the fill-byte fill.  See
     * memset(3).
     **/
    public abstract void set(VM_Address addr, byte fill, int nb);
    /**
     * Initialize nb-many bytes at offset off from the object ref to
     * the fill-byte fill.  ref need not be protected from GC.  See
     * memset(3).
     **/
    public abstract void set(Oop ref, int off, byte fill, int nb);

    /**
     * Copy elements from one array to another.  This method cannot be
     * used to copy elements within a single array.
     **/
    public final void copyArrayElements(Oop fromArray, int fromOffset,
					Oop toArray, int toOffset, 
					int nElems) {
	Blueprint.Array toBP = (Blueprint.Array) toArray.getBlueprint();
	Blueprint.Array fromBP = (Blueprint.Array) fromArray.getBlueprint();
	cpy(toArray, toBP.byteOffset(toOffset),
	    fromArray, fromBP.byteOffset(fromOffset),
	    nElems * fromBP.getComponentSize());
    }

    /**
     * Copy elements within an array.
     **/
    public final void copyOverlapping(Oop array,
				      int soff, int doff,
				      int nelt) {
	Blueprint.Array bp = (Blueprint.Array) array.getBlueprint();
	move(array, bp.byteOffset(doff),
	     array, bp.byteOffset(soff),
	     nelt * bp.getComponentSize());
    }
    
    public static Mem the() throws PragmaStitchSingleton {
	return (Mem) InvisibleStitcher.singletonFor(Mem.class);
    }

    public static class PollingAware extends Mem {
	public final int MAX_ATOMIC_COPY;
	public final int MAX_ATOMIC_TOUCH;

	public PollingAware(String nb) {
	    MAX_ATOMIC_TOUCH = CommandLine.parseSize(nb);
	    MAX_ATOMIC_COPY = MAX_ATOMIC_TOUCH / 2;
	}

	static final class Nat implements NativeInterface {
	    // FIXME: memcpy and memmove actually return void*, but
	    // j2c can't make a direct call to a C function that
	    // returns a value.
	    static native void memcpy(VM_Address to,
				      VM_Address from,
				      int nb);
	    static native void memmove(VM_Address to,
				       VM_Address from,
				       int nb);
	    // FIXME: memset is supposed to be a function returning
	    // void*, but in some versions of Linux this is not so.
	    static native void memset(VM_Address addr,
				      int fill,
				      int nb);
	    static native void bzero(VM_Address addr, int nb);
	}
	
	/**
	 * PARBEGIN PAREND
	 * This implementation is required if you want to be able to
	 * call these methods from within PARs.  This is nice if you want
	 * to be able to use, say, System.arraycopy().
	 */
	static final class NatJava {
	    // FIXME: memcpy and memmove actually return void*, but
	    // j2c can't make a direct call to a C function that
	    // returns a value.
	    static void memcpy(VM_Address to,
			       VM_Address from,
			       int nb) {
		int width = VM_Word.widthInBytes();
		int i;
		for (i = 0; i + width < nb; i += width) {
		    to.add(i).setWord(from.add(i).getWord());
		}
		for (; i < nb; i++) {
		    to.add(i).setByte(from.add(i).getByte());
		}

	    }
	    static void memmove(VM_Address to,
				VM_Address from,
				int nb) {

		if(from.EQ(to)) {
		    return;
		} else if(from.uGT(to)) {
		    for(int i = 0; i < nb; i++) {
			to.add(i).setByte(from.add(i).getByte());
		    }
		} else {
		    for(int i = nb - 1; i >= 0; i--) {
			to.add(i).setByte(from.add(i).getByte());
		    }
		}
	    }


	    // FIXME: memset is supposed to be a function returning
	    // void*, but in some versions of Linux this is not so.
	    static void memset(VM_Address addr,
			       int fill,
			       int nb) {
		byte fillVal = (byte) fill;
		for (int i = 0; i < nb; i++) {
		    addr.add(i).setByte(fillVal);
		}
	    }
	    static void bzero(VM_Address addr, int nb) {
		byte zero = (byte) 0;
		int width = VM_Word.widthInBytes();
		int i;
		for (i = 0; i + width < nb; i += width) {
		    addr.add(i).setInt(zero);
		}
		for (; i < nb; i++) {
		    addr.add(i).setByte(zero);
		}
	    }
	}


	public void cpy(VM_Address to, VM_Address from, int nb)	{
	    while (nb > MAX_ATOMIC_COPY) {
		Nat.memcpy(to, from, MAX_ATOMIC_COPY);
		to = to.add(MAX_ATOMIC_COPY);
		from = from.add(MAX_ATOMIC_COPY);
		nb -= MAX_ATOMIC_COPY;
	    }
	    Nat.memcpy(to, from, nb);
	}

	public void cpy(Oop to, int toff, Oop from, int foff, int nb) {
	    while (nb > MAX_ATOMIC_COPY) {
		Nat.memcpy(VM_Address.fromObject(to).add(toff),
			   VM_Address.fromObject(from).add(foff),
			   MAX_ATOMIC_COPY);
		toff += MAX_ATOMIC_COPY;
		foff += MAX_ATOMIC_COPY;
		nb -= MAX_ATOMIC_COPY;
	    }
	    Nat.memcpy(VM_Address.fromObject(to).add(toff),
		       VM_Address.fromObject(from).add(foff),
		       nb);
	}

	public void cpy(Oop to, int toff, VM_Address from, int nb) {
	    while (nb > MAX_ATOMIC_COPY) {
		Nat.memcpy(VM_Address.fromObject(to).add(toff),
			   from,
			   MAX_ATOMIC_COPY);
		toff += MAX_ATOMIC_COPY;
		from = from.add(MAX_ATOMIC_COPY);
		nb -= MAX_ATOMIC_COPY;
	    }
	    Nat.memcpy(VM_Address.fromObject(to).add(toff),
		       from,
		       nb);
	}

	private void moveMulti(VM_Address to, VM_Address from, int nb) {
	    if (to.uLT(from)) {
		while (nb > MAX_ATOMIC_COPY) {
		    Nat.memmove(to, from, MAX_ATOMIC_COPY);
		    to = to.add(MAX_ATOMIC_COPY);
		    from = from.add(MAX_ATOMIC_COPY);
		    nb -= MAX_ATOMIC_COPY;
		}
		Nat.memmove(to, from, nb);
	    } else {
		int off = nb - MAX_ATOMIC_COPY;
		while (nb > MAX_ATOMIC_COPY) {
		    Nat.memmove(to.add(off), from.add(off), MAX_ATOMIC_COPY);
		    nb -= MAX_ATOMIC_COPY;
		    off -= MAX_ATOMIC_COPY;
		}
		Nat.memmove(to, from, nb);
	    }
	}

	public void move(VM_Address to, VM_Address from, int nb) {
	    if (nb > MAX_ATOMIC_COPY)
		moveMulti(to, from, nb);
	    else
		Nat.memmove(to, from, nb);
	}

	private void moveMulti(Oop to, int toff, Oop from, int foff, int nb) {
	    // if to != from, we can do the move in any direction, but
	    // if to == from, we must be careful not to overwrite a
	    // from index before it is copied.
	    if (toff < foff) {
		while (nb > MAX_ATOMIC_COPY) {
		    Nat.memmove(VM_Address.fromObject(to).add(toff),
				VM_Address.fromObject(from).add(foff),
				MAX_ATOMIC_COPY);
		    toff += MAX_ATOMIC_COPY;
		    foff += MAX_ATOMIC_COPY;
		    nb -= MAX_ATOMIC_COPY;
		}
		Nat.memmove(VM_Address.fromObject(to).add(toff),
			    VM_Address.fromObject(from).add(foff),
			    nb);
	    } else {
		toff += nb;
		foff += nb;
		while (nb > MAX_ATOMIC_COPY) {
		    toff -= MAX_ATOMIC_COPY;
		    foff -= MAX_ATOMIC_COPY;
		    Nat.memmove(VM_Address.fromObject(to).add(toff),
				VM_Address.fromObject(from).add(foff),
				MAX_ATOMIC_COPY);
		    nb -= MAX_ATOMIC_COPY;
		}
		Nat.memmove(VM_Address.fromObject(to).add(toff - nb),
			    VM_Address.fromObject(from).add(foff - nb),
			    nb);
	    }
	}
	public void move(Oop to, int toff, Oop from, int foff, int nb) {
	    if (nb > MAX_ATOMIC_COPY)
		moveMulti(to, toff, from, foff, nb);
	    else
		Nat.memmove(VM_Address.fromObject(to).add(toff),
			    VM_Address.fromObject(from).add(foff),
			    nb);
	}

	public void zero(VM_Address addr, int nb) {
	    while (nb > MAX_ATOMIC_TOUCH) {
		Nat.bzero(addr, MAX_ATOMIC_TOUCH);
		addr = addr.add(MAX_ATOMIC_TOUCH);
		nb -= MAX_ATOMIC_TOUCH;
	    }
	    Nat.bzero(addr, nb);
	}

	public void zero(VM_Address addr, int nb, Runnable cback) {
	    while (nb > MAX_ATOMIC_TOUCH) {
		Nat.bzero(addr, MAX_ATOMIC_TOUCH);
		addr = addr.add(MAX_ATOMIC_TOUCH);
		nb -= MAX_ATOMIC_TOUCH;
		cback.run();
	    }
	    Nat.bzero(addr, nb);
	}

	public void zeroAtomic(VM_Address addr, int nb, Runnable cback) throws PragmaNoPollcheck {
	    while (nb > MAX_ATOMIC_TOUCH) {
		Nat.bzero(addr, MAX_ATOMIC_TOUCH);
		addr = addr.add(MAX_ATOMIC_TOUCH);
		nb -= MAX_ATOMIC_TOUCH;
		cback.run();
	    }
	    Nat.bzero(addr, nb);
	}

	public void zero(Oop oop, int off, int nb) {
	    while (nb > MAX_ATOMIC_TOUCH) {
		Nat.bzero(VM_Address.fromObject(oop).add(off), MAX_ATOMIC_TOUCH);
		off += MAX_ATOMIC_TOUCH;
		nb -= MAX_ATOMIC_TOUCH;
	    }
	    Nat.bzero(VM_Address.fromObject(oop).add(off), nb);
	}

	public void set(VM_Address addr, byte fill, int nb) {
	    while (nb > MAX_ATOMIC_TOUCH) {
		Nat.memset(addr, fill, nb);
		addr = addr.add(MAX_ATOMIC_TOUCH);
		nb -= MAX_ATOMIC_TOUCH;
	    }
	    Nat.memset(addr, fill, nb);
	}

	public void set(Oop oop, int off, byte fill, int nb) {
	    while (nb > MAX_ATOMIC_TOUCH) {
		Nat.memset(VM_Address.fromObject(oop).add(off), fill, nb);
		off += MAX_ATOMIC_TOUCH;
		nb -= MAX_ATOMIC_TOUCH;
	    }
	    Nat.memset(VM_Address.fromObject(oop).add(off), fill, nb);
	}
    }
}

