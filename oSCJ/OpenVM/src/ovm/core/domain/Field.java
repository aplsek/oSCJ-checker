/**
 * @file ovm/core/domain/Field.java
 **/
package ovm.core.domain;

import ovm.core.repository.Mode;
import ovm.core.repository.Selector;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.editor.Cursor;
import ovm.util.NoSuchElementException;

/**
 * The interface for <code>Field</code> objects. This object should contain
 * modifier information, selector, declaring type, and any other information 
 * that needs to be associated with the specific field object.
 **/
public interface Field extends Member {
    
    /**
     * Get the bound selector object for this field
     * @return this field's bound selector
     **/
    Selector.Field getSelector();

    /**
     * Get the modifiers object for this field
     * @return this field's mode object
     **/
    Mode.Field getMode();

    /**
     * Get the <code>Type</code> associated with this field
     * @return this field's <code>Type</code>
     **/
    Type getType() throws LinkageException;

    /**
     * Return a field's constant value as a {@link
     * ovm.core.repository.RepositoryString}, boxed primitive, or null
     * if no constant value is defined.
     */
    Object getConstantValue();
    
    /**
     * Get the <code>Type</code> associated with the <i>declaring</i>
     * class for this field
     * @return the declaring class's <code>Type</code> object
     **/
    Type.Compound getDeclaringType(); // inherited from Member
    /* there can be two fields of the same object 
       that only differ in the declaring type - a private field may be hidden 

    */
    
    /** Add to an OvmIR stream the appropriate snippet to fetch from
     *  this field, without exposing at this interface any implementation
     *  details like offsets, etc.
     * <br /><strong>Top of runtime stack before snippet:</strong>
     * (Oop o)
     * <br /><strong>After:<strong> (content of this field of o)
     * <p />The Oop is <strong>not</strong> verified to be of a type providing
     * this field.
     * @param c Cursor, at whose current position the snippet will be added.
     */
    void addGetfieldQuick( Cursor c);
    
    /** Add to an OvmIR stream the appropriate snippet to store to
     *  this field, without exposing at this interface any implementation
     *  details like offsets, etc.
     * <br /><strong>Top of runtime stack before snippet:</strong>
     * (Oop o) (new value)
     * <br /><strong>After:<strong> <em>consumed</em>
     * <p />The Oop is <strong>not</strong> verified to be of a type providing
     * this field.
     * @param c Cursor, at whose current position the snippet will be added.
     */
    void addPutfieldQuick( Cursor c);
    
    /** Add to an OvmIR stream the appropriate snippet to store to
     *  this field, without exposing at this interface any implementation
     *  details like offsets, etc.
     * <br /><strong>Top of runtime stack before snippet:</strong>
     * (Oop o) (new value)
     * <br /><strong>After:<strong> <em>consumed</em>
     * <p />The Oop is <strong>not</strong> verified to be of a type providing
     * this field.
     * @param c Cursor, at whose current position the snippet will be added.
     */
    void addPutfieldQuickWithBarrier(Cursor c);
    
    /** Add to an OvmIR stream the appropriate snippet to convert an Oop on the
     *  top of the runtime stack to the effective address of this field within
     *  that object, without exposing at this interface any implementation
     *  details like offsets, etc.
     * <br /><strong>Top of runtime stack before snippet:</strong>
     * (Oop o)
     * <br /><strong>After:<strong> (VM_Address fieldAdr)
     * <p />The Oop is <strong>not</strong> verified to be of a type providing
     * this field.
     * <p />At completion, the top of stack is considered to hold a VM_Address,
     * not a reference; memory manager activity can invalidate it.
     * @param c Cursor, at whose current position the snippet will be added.
     */
    void addPushEffectiveAddress( Cursor c);
    
    /** For convenience in undoing Java reflective boxing. No reverse operation
     *  is provided. The value's dynamic type must either be the exact box type
     *  corresponding to the dynamic receiver type or, for integer and narrower types,
     *  Integer (in which case the value will be checked for representability in the
     *  receiver field type). The special treatment for Integer is a sop for the JVM
     *  convention regarding constant values in class files.
     * @throws ClassCastException if the value isn't the proper type
     * @throws NumberRanges.NumberRangeException if the value has type Integer and is
     * not representable losslessly in the receiver field type.
     **/
    void setUnrefined( Oop dst, Object value);

    /**
     * Iterator interface for <code>Field</code> objects
     **/
    interface Iterator {
	
	/**
	 * Determine if there is a next element in the sequence
	 * @return true if there else a next element, false if not
	 **/
	boolean hasNext();

	/**
	 * Get the next element in the sequence
	 * @return the next <code>Field</code> in the sequence.
	 **/
	Field next();
        
        final Iterator EMPTY_INSTANCE = new Iterator() {
            public boolean hasNext() { return false; }
            public Field next() { throw new NoSuchElementException(); }
        };
    }
    
    interface Reference extends Field {
        void set( Oop dst, Oop value);
        Oop get( Oop src);
        /** Avoided putting this method on all field types, as it is relatively evil,
         *  but on a reference field it is handy for memory managers, I guess handy
         *  enough to be worth having.  Providing this method will imply the constraint
         *  that VM_Address.{get,set}Address and Field.Reference.{get,set} must be
         *  equivalent, another way of saying that object layouts are constrained to
         *  keep reference fields in slots of the natural width of VM_Address.
         **/
        VM_Address addressWithin( Oop o);
    }
    interface Boolean extends Field {
        void set( Oop dst, boolean value);
        boolean get( Oop src);
    }
    interface Byte extends Field {
        void set( Oop dst, byte value);
        byte get( Oop src);
    }
    interface Short extends Field {
        void set( Oop dst, short value);
        short get( Oop src);
    }
    interface Character extends Field {
        void set( Oop dst, char value);
        char get( Oop src);
    }
    interface Integer extends Field {
        void set( Oop dst, int value);
        int get( Oop src);
    }
    interface Long extends Field {
        void set( Oop dst, long value);
        long get( Oop src);
    }
    interface Float extends Field {
        void set( Oop dst, float value);
        float get( Oop src);
    }
    interface Double extends Field {
        void set( Oop dst, double value);
        double get( Oop src);
    }
}
