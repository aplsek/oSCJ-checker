package ovm.core.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ovm.core.OVMBase;
import ovm.core.services.memory.MemoryPolicy;
import ovm.util.OVMRuntimeException;
import ovm.util.ReadSafeHashMap;
    
/**
 * The base class for all interned repository sybmols.  Repository symbols correspond, roughly, to
 * symbolic information found in constant pools.  These objects are
 * built up in trees, where the leaves are indexes into the
 * UTF8Store.  Each node in the tree is interned in a symbol table,
 * which allows RepositorySymbol objects to be compared using
 * <code>==</code>, and conserves space.
 **/
public abstract class RepositorySymbol extends OVMBase {
    /**
     * This class factors out so of the work involved in keeping
     * intern tables up to date.  In the OVM, interned
     * RepositorySymbols are stored in a special area of memory, so
     * we must allocate a temporary object when querying the symbol
     * table, then copy it into the special area if it is new.
     **/
    static abstract class Internable extends RepositorySymbol {
	/**
	 * Used in {@link SymbolTable} to maintain hashed sets.
	 **/
	Internable next;

	protected abstract Internable copy();

	protected final Internable intern(SymbolTable map) {
	    synchronized (map) {
		Internable ret = map.get(this);
		if (ret != null)
		    return ret;
		// Copy into immortal memory before interning.
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try {
		    ret = copy();
		    map.put(ret);
		} finally { MemoryPolicy.the().leave(r); }
		return ret;
	    }
	}
    }

    /**
     * This class implements an output stream in which the data is
     * written into a byte array. The buffer automatically grows as
     * data is written to it. The data can be retrieved using
     * toByteArray() and toString().<p>
     *
     * Closing a ByteArrayOutputStream has no effect. The methods in
     * this class can be called after the stream has been closed
     * without generating an IOException.<p>
     **/
    protected static class RetrievableByteArrayOutputStream
	extends ByteArrayOutputStream
    {
	/**
	 * Return the contents of this output stream's buffer in
	 * byte array form.
	 * @return the byte array representation of this output
	 *         stream's buffer
	 **/
	public byte[] toByteArray() {
	    if (count == buf.length) {
		return buf;
	    } else {
		return super.toByteArray();
	    }
	}
    }

    abstract public void write(OutputStream str) throws IOException;

    public String toString() {
	try {
	    ByteArrayOutputStream b = new ByteArrayOutputStream();
	    write(b);
	    return b.toString("UTF8");
	} catch (IOException e) { throw new OVMRuntimeException(e); }
    }
}
