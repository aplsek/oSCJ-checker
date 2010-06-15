/**
 * @file ovm/core/domain/Method.java
 **/
package ovm.core.domain;

import ovm.core.repository.Mode;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.util.NoSuchElementException;
import s3.core.domain.S3ByteCode;
import s3.util.PragmaMayNotLink;

/**
 * The interface for <code>Method</code> objects. This object should contain
 * modifier information, selector, declaring type, return type, thrown
 * exception types, argument types, and any other information 
 * that needs to be associated with the specific method object.
 **/
public interface Method extends Member {
    /**
     * Return an integer that uniquely identifiers this method
     * within it's Type.Context (as returned by
     * getDeclaringType().getContext()).
     **/
    int getUID();

    /**
     * Return the unique identifier for a blueprint's context.
     * This is equivalent to getDeclaringType().getContext().getUID(),
     * and the (CID,UID) pair uniquely identifies a method.
     **/
    int getCID();

    boolean isConstructor();
    boolean isClassInit();
    
    boolean isNonVirtual();
    boolean isVirtual();
    boolean isInterface();
    /**
     * Get the bound selector object for this method
     * @return this method's bound selector
     **/
    Selector.Method getSelector();
    void setSelector(Selector.Method sel);

    /**
     * Return the application-visible name of a method, this may
     * differ from it's internal name (obtained with {@link #getSelector}, 
     * as we may need multiple implementations for a single
     * application-level method.
     **/
    int getExternalNameIndex();
    Selector.Method getExternalSelector();

    int[] getSyntheticParameterOffsets();
    void markParameterAsSynthetic(int offset);
    /**
     * A convenience method implemented in terms of
     * {@link #setSelector} and {@link #markParameterAsSynthetic}.
     **/
    void appendSyntheticParameter(TypeName t);

    /**
     * Get the modifiers object for this method
     * @return this method's mode object
     **/
    Mode.Method getMode();
    /**
     * Get the <code>Type</code> object for the return type associated with 
     * this method
     * @return this method's return <code>Type</code>
     **/
    Type getReturnType() throws LinkageException;
    /**
     * Get the <code>Type</code> for the exceptions thrown by this method
     * @return this method's thrown exception <code>Type</code>
     **/
    Type.Class getThrownType(int i) throws LinkageException;
    /**
     * Get the number of  exceptions thrown by this method
     **/
    int getThrownTypeCount();
    /**
     * Get the <code>Type</code> for the <code>i</code>th argument of this method 
     * @return this method's argument <code>Types</code>
     **/
    Type getArgumentType(int i) throws LinkageException;
    /**
     * Get the number of arguments this method takes (excluding this)
     **/
    int getArgumentCount();
    /**
     * Return the preferred executable code for this method.  (The
     * prefered executable code is the last code added with
     * {@link #addCode}.)
     **/
    Code getCode();
    
    /**
     * Given a kind give the bytecode/native/whatever code for it in
     * form of a byte array.
     *
     * In general, you should either call {@link #getByteCode} to
     * retrieve the OvmIR code for this method, or {@link #getCode()}
     * to retrieve the best executable form of this method.
     **/
    Code getCode(Code.Kind kind);

    /**
     * Remove a particular representation of this method's code.
     * Return the object removed, or null if no matching {@link Code}
     * object was found.
     **/
    Code removeCode(Code.Kind kind);

    /**
     * Return the bytecode represntation of this method.  The
     * representation may be in one of several states.  See
     * {@link s3.core.domain.S3ByteCode#getState} for details.
     **/
    S3ByteCode getByteCode() throws PragmaMayNotLink;
    
    /**
     * Add executable code for this method.  The executable code
     * will replace any existing code in the engine-neutral dispatch
     * tables.
     * <p>
     * Can this be called more than once?  What does that mean?
     * Should quickified bytecode be kept separate from symbolic?
     * @see #getCode(Code.Kind)
     **/
    void addCode(Code code);
    
    /**
     * Iterator interface for <code>Method</code> objects
     **/
    interface Iterator {
	/**
	 * Determine if there is a next element in the sequence
	 * @return true if there else a next element, false if not
	 **/
	public boolean hasNext();
	/**
	 * Get the next element in the sequence
	 * @return the next <code>Method</code> in the sequence.
	 **/
	public Method next();

        final Iterator EMPTY_INSTANCE = new Iterator() {
            public boolean hasNext() { return false; }
            public Method next() { throw new NoSuchElementException(); }
        };
    }
}
