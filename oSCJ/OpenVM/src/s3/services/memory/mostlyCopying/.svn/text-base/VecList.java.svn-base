package s3.services.memory.mostlyCopying;

import ovm.core.OVMBase;

/*
 * Suppose we trashed the freelist.  Instead, we have some bit
 * vectors and some byte arrays:
 * free-blocks:  bit-vector of free blocks
 * white-blocks: bit-vector of collectable blocks
 * grey-blocks:  bit-vector of blocks allocated during GC
 * cont:         bit-vector of all non-zeroth blocks for
 *               mutli-block allocations
 *
 * Allocating a block searches free-blocks for a suitably long
 * string of 1's, clears the bits and sets the corresponding bits
 * in white-blocks and cont.
 *
 * Sweeping the blocks is implemented as follows:
 *   cont |= ~white-blocks
 *   free-blocks |= white-blocks
 *   white-blocks = grey-blocks
 *
 * This sets us up for a the next simple generational collection.
 * If we want to perform a full GC next time around, we need to
 * set white-blocks = ~free-blocks before the GC starts.
 *
 * Pinning a conservative pointer means removing its allocation
 * from white-blocks and setting the dirty flag.
 *
 * Explicit pinning would have to be supported through some
 * auxillary data structures.
 *
 * WHAT ABOUT THE WORK QUEUE?  If blocks are allocated in address
 * order (with wrapping), and no blocks are freed during copying,
 * I can simply scan the grey list starting at allocation pointer
 * when GC started, and stopping when I catch up to the current
 * allocation pointer.
 *
 * GC works as follows:
 * if full GC: white blocks = ~free-blocks
 * pin conservative roots (dirty = 2; white = false)
 * if full GC: update all precise roots
 * else:       update dirty blocks (dirty == 1)
 * update pinned blocks (dirty == 2)
 * update grey blocks
 * reset bit vectors
 *
 * All this is meant to reduce the amount of meta-data needed for
 * a generational GC.  With a block size of 512 bytes and a heap
 * size of 64M, the current scheme will scan a 512k array to return
 * blocks to the freelist, but the bit-vector scheme will only
 * scan four 8k arrays.
 *
 * On the other hand, there is a great deal of overhead associated
 * with large blocks, both in terms of allocation and walking.
 * Maybe a seperate array that holds a multi-block's size and
 * continued blocks offsets (stored as negative numbers) would
 * help.
 *
 * What about the image?  If there where no image, we would only
 * need enough dirty flags to cover the heap.  However, the image
 * makes things complicated.  Ideally, we would ensure that small
 * objects in the heap don't span block boundaries, and record
 * the boundaries that large objects span.  The heap could then be
 * mapped immediately after the image.  With this setup, the free,
 * grey, and white vectors would be indexed relative to the heap
 * base, while the continued and dirty vectors would be indexed
 * relative to the image base.  All this can only be done if there
 * is some way to inject GC-specific code into the image layout
 * logic (which lives in VM_Address).
 */
public class VecList extends OVMBase {
    final int size;
    
    /*
     * We maintain four bit vectors to track memory usage
     */
    int[] freeBlocks;		// free storage
    int[] whiteBlocks;		// blocks in the from space, that are
				// elibible for collection
    int[] greyBlocks;		// blocks allocated during GC
    int[] cont;			// the 2nd through nth block of each
				// multi-block allocation.  This set
				// covers both the heap and the bootimage.
    int[] dirty;		// bit-set of dirty blocks.  A block
				// is marked as dirty by the SIGSEGV/SIGBUS
				// handler on an attempted write.  The
				// dirty bit may be cleared, and the
				// block may be reprotected by the
				// GC.  This set covers both the heap
				// and the bootimage.
    int[] freshBlocks;		// bit-set to record freshly allocated
				// blocks.  (Grey during GC and white
				// the rest of the time.)


    int allocPtr = 0;
    int updatePtr;
    int alloced;

    VecList(int size) {
	this.size = size;
    }


    void initFreelist() {
	for (int i = 0, size=this.size;  size > 0; i++, size -= 32) {
	    if (size > 31)
		freeBlocks[i] = -1;
	    else {
		for (int j = 0; j < size; j++)
		    setBit(freeBlocks, i+j);
	    }
	}
    }

    boolean getBit(int[] bits, int b) {
	return (bits[b >> 5] & (1 << (31&b))) != 0;
    }
    void setBit(int[] bits, int b) {
	bits[b >> 5] |= (1 << (31&b));
    }
    void clearBit(int[] bits, int b) {
	bits[b >> 5] &= ~(1 << (31&b));
    }
    int ffs(int[] bits, int start) {
	int idx = start >> 5;
	while (idx < bits.length) {
	    int word = bits[idx] >>> (31&start);
	    if (word == 0) {
		start = (start + 32) & ~31;
		idx++;
	    } else {
		while ((word & 1) == 0) {
		    start++;
		    word >>>= 1;
		}
		return start;
	    }
	}
	return -1;
    }
    int ffc(int[] bits, int start) {
	int idx = start >> 5;
	while (idx < bits.length) {
	    int word = bits[idx] >> start;
	    if (word == -1) {
		start = (start + 32) & ~31;
		idx++;
	    } else {
		while ((word & 1) == 1) {
		    start++;
		    word >>= 1;
		}
	    return start;
	  }
	}
	return -1;
    }
    void copyBits(int[] from, int[] to) {
	for (int i = 0; i < to.length; i++)
	    to[i] = from[i];
    }
    void clearBits(int[] bs) {
	for (int i = 0; i < bs.length; i++)
	    bs[i] = 0;
    }

    /**
     * Remove a contiguous range of sz-many blocks from the freelist
     * and add it to the list given by <code>blocks</code>.  Return
     * the index of the first block in the allocated range.
     */
    protected int alloc(int[] blocks, int sz) {
	if (sz == 1) {
	    int ret = ffs(freeBlocks, allocPtr);
	    if (ret == -1 || ret >= size)
		ret = ffs(freeBlocks, 0);
	    if (ret == -1 || ret >= size)
		return -1;
	    clearBit(freeBlocks, ret);
	    setBit(blocks, ret);
	    allocPtr = ret + 1;
	    alloced++;
	    return ret;
	} else {
	    boolean looped = false;
	    int start = allocPtr;
	    findRun: while (true) {
		start = ffs(freeBlocks, start);
		if (start == -1 || start + sz > this.size) {
		    if (looped)
			return -1;
		    looped = true;
		    start = 0;
		    continue;
		    
		}
		for (int i = 1; i < sz; i++) {
		    if (!getBit(freeBlocks, start+i)) {
			start = start + i;
			continue findRun;
		    }
		}
		clearBit(freeBlocks, start);
		setBit(blocks, start);
		for (int i = 1; i < sz; i++) {
		    clearBit(freeBlocks, start + i);
		    setBit(blocks, start + i);
		    setBit(cont, start + i);
		}
		// Note: we may have skipped over some fragmented
		// space, but we have to pay this price if we want the
		// updatePtr used by walkGrey to catch up to the
		// allocaPtr used here.  
		//
		// Actually, I'm not sure the
		// whole thing works at all.  The heap may be so
		// fragmented that we end up walking the grey list out
		// of sequence.  allocPtr may wrap trying to allocate
		// a large block, then allocate a block it already
		// scanned past.  When the updatePtr hits this out of
		// sequence block, it will stop prematurely.
		allocPtr = start + sz;
		alloced += sz;
		return start;
	    }
	}
    }

    /**
     * Allocate a fresh block (white before GC or grey during GC).
     **/
    public int alloc(int sz) {
	return alloc(freshBlocks, sz);
    }

    public void startGC(boolean full) {
	freshBlocks = greyBlocks;
	updatePtr = allocPtr;
	alloced = 0;
// 	for (int i = 0; i < freeBlocks.length; i++)
// 	    assert((freeBlocks[i] | whiteBlocks[i] | cont[i]) == -1);
    }

    public int getSize(int idx) {
	int ret = 1;
	// This assertion fails for the bootimage
	assert(/*!getBit(freeBlocks, idx) &&*/ !getBit(cont, idx));
	// FIXME: With WB, cont covers both heap and bootimage,
	// whereas size is only the size of the heap
	while (idx + ret < 32*cont.length && getBit(cont, idx+ret))
	    ret++;
	return ret;
    }

    public boolean inFromSpace(int idx) {
	assert(!getBit(cont, idx));
	return getBit(whiteBlocks, idx);
    }

    public boolean isContinued(int idx) {
	return getBit(cont, idx);
    }
    public int getBase(int idx) {
	assert(getBit(cont, idx));
	while (getBit(cont, idx))
	    idx--;
	return idx;
    }
    public void pin(int idx) {
	assert(getBit(whiteBlocks, idx) && !getBit(cont, idx));
	clearBit(whiteBlocks, idx);
	setBit(greyBlocks, idx);
	alloced++;
	for (idx++; idx < size && getBit(cont, idx); idx++) {
	    assert(getBit(whiteBlocks, idx));
	    clearBit(whiteBlocks, idx);
	    setBit(greyBlocks, idx);
	    alloced++;
	}
    }

    public void walkGrey(Manager.BlockWalker w) {
	for (int i = allocPtr; alloced > 0; ) {
	    i = ffs(greyBlocks, i);
	    if (i == -1 || i >= size)
		i = ffs(greyBlocks, 0);
	    // i is the block we are currently allocating from, and it
	    // is not the only grey block left.  Walk the other grey
	    // blocks, and copy objects into block i before walking i
	    // itself.
	    if (i == allocPtr - 1 && alloced > 1) {
		i++;
		continue;
	    }
	    int sz = getSize(i);
	    w.walk(i, sz);
	    alloced -= sz;
	    while (sz-- > 0)
		clearBit(greyBlocks, i++);
	}
    }

    public void walkWhite(Manager.BlockWalker w) {
	for (int i = ffs(freshBlocks, 0);
	     i != -1 && i < size;
	     i = ffs(freshBlocks, i)) {
	    int sz = getSize(i);
	    w.walk(i, sz);
	    i += sz;
	}
    }

    public void walkFree(Manager.BlockWalker w) {
	for (int i = ffs(freeBlocks, 0);
	     i != -1 && i < size;
	     i = ffs(freeBlocks, i)) {
	    int nonFree = ffc(freeBlocks, i);
	    if (nonFree == -1)
		nonFree = size;
	    int sz = nonFree - i;
	    w.walk(i, sz);
	    i += sz;
	}
    }

    public void endGC() {
	for (int i = 0; i < whiteBlocks.length; i++) {
	    assert((whiteBlocks[i] & freeBlocks[i]) == 0);
	    assert(greyBlocks[i] == 0);
	    cont[i] &= ~whiteBlocks[i];
	    freeBlocks[i] |= whiteBlocks[i];
	    whiteBlocks[i] = ~freeBlocks[i];
	}
	freshBlocks = whiteBlocks;
    }

    public void resetAllocationPointer() {
	allocPtr = 0;
    }
    
}
