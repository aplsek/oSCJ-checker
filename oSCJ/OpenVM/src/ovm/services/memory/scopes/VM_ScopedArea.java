package ovm.services.memory.scopes;

import ovm.core.services.memory.*;
import s3.util.*;
import ovm.util.*;
import ovm.core.*;
import ovm.core.domain.Oop;

/** A memory area that might potentially have scope support.  Note that
    it is perfectly acceptable to subclass this when you have no
    intention of supporting scopes.  This happens because this class is
    really meant to be a mixin. */
public abstract class VM_ScopedArea extends VM_Area {
    /**
     * The RTSJ ScopedMemory object corresponding to a scoped memory
     * area.  The heap and immortal areas do not have mirrors, on the
     * assumption that there may one day be multiple domains sharing a
     * single heap.
     **/
    protected Oop mirror;

    /** The root of the scope tree we belong to.  Why isn't this
	static?  Because we want to allow for the possibility of
	multiple entirely independent scope trees, for example
	if we have multiple RTSJ domains.
	<p/>
	Unfortunately, current memory managers only allow for one
	scope root... */
    PrimordialScope root;

    /** Parent of this node. */
    VM_ScopedArea parent;
    /** First child of this node. */
    VM_ScopedArea childHead;
    /** Next sibling of this node. */
    VM_ScopedArea nextSibling;
    /** Previous sibling of this node. */
    VM_ScopedArea prevSibling;
    
    public static final int MSK = 0x80008000;
    public static final int RES = 0x00008000;
    protected int prange;
    protected int crange;
    protected boolean unchecked;

    public VM_ScopedArea(PrimordialScope root) {
	this.root=root;
    }
    
    public VM_ScopedArea() {
	this.root=null;
    }
    
    public void setRange(short min, short max) throws PragmaNoPollcheck {
	prange = (min << 16)|max|MSK;
	crange = (min << 16)|max;
    }

    /**
     * Return true if objects in this area may hold references to
     * objects in other
     **/
    public boolean canPointTo(VM_ScopedArea other) {
	return (((other.prange - crange) & MSK) == RES	|| unchecked);
    }
    /**
     * Return the reference associated with an allocator, or null if
     * for the heap and immortal allocators.  Since the heap and
     * immortal allocators are shared between domains, they cannot be
     * associated with a single reference.
     **/
    public Oop getMirror() {
	return mirror;
    }

    /** tells you if this really is a scope. */
    public boolean isScope() { return true; }
    
    /** Return the depth of the hierarchy that has this node as the 
	leaf
    */
    public int getHierarchyDepth() {
	int depth = 1;
	for (VM_ScopedArea ancestor = parent; ancestor != null;
	     ancestor = ancestor.parent)
	    depth++;
	return depth;
    }
    
    /**
     * Set a parent relationship - updating the tree as needed. 
     * This requires exclusive access to the tree.
     * <p/>
     * If the newParent is null, this makes the parent be the
     * root.
     */
    public void setParent(VM_ScopedArea newParent) throws PragmaNoBarriers,
							  PragmaAtomic {
	assert(parent == null);
	
	if (newParent==null) 
	    newParent=root;

	parent = newParent;                
	
	// a child is always inserted at the head of the siblings
	nextSibling = parent.childHead;
	prevSibling = null;
	if (parent.childHead != null) {
	    parent.childHead.prevSibling = this;
	}
	parent.childHead = this;
	root.recomputeRanges();
    }
    
    /**
     * Clear the parent of this node, updating the tree as needed.
     * Requires exclusive access to the tree.
     */
    public void resetParent() throws PragmaNoBarriers, PragmaAtomic {
	VM_ScopedArea prev = prevSibling;
	VM_ScopedArea next = nextSibling;
	
	assert(parent != null);
	
	if (prev == null) {
	    parent.childHead = next;
	} else {
	    prev.nextSibling = next;
	}
	if (next != null) {
	    next.prevSibling = prev;
	}
	nextSibling = prevSibling = parent = null;
	root.recomputeRanges();
    }
    
    short preNumber = 0;
    short postNumber = 32767;
    
    short walk(short number) {
	preNumber = number;
	if (childHead != null) {
	    ++number;
	    for (VM_ScopedArea cur = childHead;
		 cur != null;
		 cur = cur.nextSibling) {
		number = cur.walk(number);
	    }
	    ++number;
	}
	postNumber = number;
	setRange(preNumber,postNumber);
	return number;
    }
    
    public boolean isProperDescendantOf(VM_ScopedArea n) 
	throws PragmaInline {
	return preNumber > n.preNumber && postNumber < n.postNumber;
    }
	
    public boolean hasChild() {
	return childHead != null;
    }
	
    public boolean hasMultipleChildren() {
	return childHead != null && childHead.nextSibling != null;
    }

    public VM_ScopedArea getParent() throws PragmaInline {
	return parent;
    }
    
}

