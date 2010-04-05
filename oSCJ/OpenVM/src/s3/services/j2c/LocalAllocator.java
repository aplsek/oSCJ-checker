package s3.services.j2c;

import s3.core.S3Base;
import ovm.core.domain.Type;
import ovm.core.repository.TypeCodes;
import ovm.util.HashSet;
import ovm.util.Iterator;
import s3.core.domain.S3Blueprint;
import s3.services.j2c.J2cValue.*;
import ovm.util.ArrayList;
import ovm.core.OVMBase;

/**
 * @author baker29
 **/
public class LocalAllocator extends S3Base {
    static final int INT_VAR = 0;
    static final int LONG_VAR = 1;
    static final int FLOAT_VAR = 2;
    static final int DOUBLE_VAR = 3;
    static final int PC_VAR = 4;
    static final int REF_VAR = 5;
    static final int N_VAR_FLAVORS = 6;
    
    static final int[] tagFlavor = new int[128];
    static final char[] flavorTag = new char[N_VAR_FLAVORS];
    static {
        flavorTag[INT_VAR] = TypeCodes.INT;
        tagFlavor[TypeCodes.BOOLEAN] = INT_VAR;
        tagFlavor[TypeCodes.BYTE] = INT_VAR;
        tagFlavor[TypeCodes.UBYTE] = INT_VAR;
        tagFlavor[TypeCodes.CHAR] = INT_VAR;
        tagFlavor[TypeCodes.USHORT] = INT_VAR;
        tagFlavor[TypeCodes.SHORT] = INT_VAR;
        tagFlavor[TypeCodes.INT] = INT_VAR;
        tagFlavor[TypeCodes.UINT] = INT_VAR;
        
        flavorTag[LONG_VAR] = TypeCodes.LONG;
        tagFlavor[TypeCodes.LONG] = LONG_VAR;
        tagFlavor[TypeCodes.ULONG] = LONG_VAR;
        
        flavorTag[FLOAT_VAR] = TypeCodes.FLOAT;
        tagFlavor[TypeCodes.FLOAT] = FLOAT_VAR;
        
        flavorTag[DOUBLE_VAR] = TypeCodes.DOUBLE;
        tagFlavor[TypeCodes.DOUBLE] = DOUBLE_VAR;
        
        flavorTag[REF_VAR] = TypeCodes.OBJECT;
        tagFlavor[TypeCodes.OBJECT] = REF_VAR;
        tagFlavor[TypeCodes.ARRAY] = REF_VAR;
        tagFlavor[TypeCodes.GEMEINSAM] = REF_VAR;
    }
    
    J2cValue[][] locals = new J2cValue[N_VAR_FLAVORS][];
    HashSet names = new HashSet();
    int cnt = 0;
    MethodCompiler mc;
    Context ctx;
    int nlocals;

    public String genSym(String hint) {
	String name = hint;
	while (names.contains(name))
	    name = hint + "_" + cnt++;
	names.add(name);
	return name;
    }

    int bpFlavor(S3Blueprint bp) {
        if (bp == null)
            return PC_VAR;
	else if (bp == ctx.VM_AddressBP || bp == ctx.VM_WordBP)
	    return INT_VAR;
        else {
            Type t = bp.getType();
            char tag = t.getUnrefinedName().getTypeTag();
            return tagFlavor[tag];
        }
    }

    int varFlavor(J2cValue var) {
	return bpFlavor(var.getBlueprint(ctx.domain));
    }

    public J2cValue findLocal(int tag, int index) {
	return locals[tagFlavor[tag]][index];
    }

    public J2cValue findLocal(S3Blueprint bp, int index) {
	return locals[bpFlavor(bp)][index];
    }

    public J2cValue[] findLocals(int flavor) {
	return (J2cValue[]) locals[flavor].clone();
    }

    public J2cValue allocateLocal(J2cValue fromValue, int idx, String _name) {
	S3Blueprint bp = fromValue.getBlueprint(ctx.domain);
        int flavor = bpFlavor(bp);
        J2cValue ret = locals[flavor][idx];
        
        if (ret == null) {
	    String name = genSym(_name);
            switch (flavor) {
	    case REF_VAR:
		ret = new J2cReference(null, bp);
		break;
	    case INT_VAR:
		ret = new J2cInt(null, TypeCodes.INT);
		break;
	    case LONG_VAR:
		ret = new J2cLong(null, null, TypeCodes.LONG);
		break;
	    case FLOAT_VAR:
		ret = new J2cFloat(null, null);
		break;
	    case DOUBLE_VAR:
		ret = new J2cDouble(null, null);
		break;
            case PC_VAR:
                ret = new J2cJumpTarget(-1);
	    }
	    ret.allocate(name, J2cValue.LOCAL_VAR, flavor, idx);
	    locals[flavor][idx] = ret;
	    allocateVar(ret);
        } else if (flavor == REF_VAR) {
            ((J2cReference) ret).destructiveMerge(fromValue);
        }
        return ret;
    }

    ArrayList allVars = new ArrayList();

    /**
     * This method is used to establish a unique number for every
     * explict local variable, spill variable, block-local stack slot,
     * and merged stack slot.  It differs from allocateSpill and
     * allocateLocal in that it is also used for stack slots.
     **/
    void allocateVar(J2cValue var) {
	if (var.number != -1) {
	    throw new Error("duplicate definition of " + var.getName());
	} else {
	    var.number = allVars.size();
	    allVars.add(var);
	}
    }

    void allocateTemp(J2cValue v, int kind, int index) {
	int flavor = bpFlavor(v.getBlueprint(mc.ctx.domain));
	String name = genSym(kind == J2cValue.STACK_SLOT
			     ? "_stack"
			     : "_phi");
	v.allocate(name, kind, flavor, index);
	allocateVar(v);
    }

    J2cValue[] getAllVars() {
	J2cValue[] ret = new J2cValue[allVars.size()];
	allVars.toArray(ret);
	return ret;
    }

    int nVars() { return allVars.size(); }

    J2cValue getVar(int i) { return (J2cValue) allVars.get(i); }

    public void rename(J2cValue _from, J2cValue _to) {
	// Perhaps the entry in allVars should simply be nulled out?
	// That way, one could find the canonical value for a
	// variable, v, using allVars.get(v.number)
	//
	// One could also canonicalize both from and to if needed.
	J2cValue to = (J2cValue) allVars.get(_to.number);
	J2cValue from = (J2cValue) allVars.get(_from.number);
	if (to == null) {
	    System.err.println(_to.name + " (" + _to.number + ") maps to null");
	    throw new Error("");
	}
	if (from == null) {
	    System.err.println(_from.name + " (" + _from.number +
			       ") maps to null");
	    throw new Error("");
	}
	if (to == from) {
	    System.err.println(_to.name + " (" + _to.number + ") and " +
			       _from.name + " (" + _from.number + 
			       ") both map to " +
			       to.name + " (" + to.number + ")");
	    throw new Error("");
	}
	assert(to != from);
	allVars.set(from.number, null);
	J2cValue tl = from;
	while (true) {
	    assert(tl != to);
	    tl.name = to.name;
	    tl.kind = to.kind;
	    tl.number = to.number;
	    tl.flavor = to.flavor;
	    tl.index = to.index;
	    if (tl.renamed == null)
		break;
	    else
		tl = tl.renamed;
	}
	tl.renamed = to.renamed;
	to.renamed = from;
    }

    public void remove(J2cValue var) {
	allVars.set(var.number, null);
    }

    public void compact() {
	J2cValue[] all = getAllVars();
	allVars.clear();
	for (int i = 0; i < all.length; i++) {
	    if (all[i] != null) {
		J2cValue v = all[i];
		int ni = allVars.size();
		if (ni != v.number)
		    while (v != null) {
			v.number = ni;
			v = v.renamed;
		    }
		allVars.add(v);
	    }
	}
    }

    public LocalAllocator(MethodCompiler mc, int maxLocals, int maxStack) {
	this.mc = mc;
	this.ctx = mc.ctx;
	this.nlocals = maxLocals + 1;
	for (int i = 0; i < REF_VAR; i++) {
	    locals[i] = new J2cValue[maxLocals];
	}
	locals[REF_VAR] = new J2cValue[maxLocals + maxStack + 1];
	assert(REF_VAR == N_VAR_FLAVORS - 1);
    }

    public Iterator varIterator(final int flavor) {
	return new Iterator() {
                J2cValue[] vars = locals[flavor];
		int nextIdx = -1;
		void findNext() {
		    while (++nextIdx < vars.length
			   && vars[nextIdx] == null)
			;
		}

		{ findNext(); }

		public boolean hasNext() {
		    return nextIdx < vars.length;
		}
		public Object next() {
		    Object ret = vars[nextIdx];
		    findNext();
		    return ret;
		}
		public void remove() {
		    throw new UnsupportedOperationException();
		}
	    };
    }
}
