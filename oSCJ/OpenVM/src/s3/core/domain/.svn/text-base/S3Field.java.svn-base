package s3.core.domain;

import ovm.core.OVMBase;
import ovm.core.domain.Domain;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.Mode;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryManager;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.editor.Cursor;
import ovm.util.NumberRanges;
import s3.core.S3Base;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;
import ovm.core.repository.JavaNames;
import ovm.core.repository.UnboundSelector;

public abstract class S3Field
extends S3Base implements Field, JVMConstants.Opcodes {
    public static final S3Field[] EMPTY_ARRAY = new S3Field[0];

    final private S3Type.Scalar declaringType;
    final private Mode.Field mode;
    final private Selector.Field sel;
    int offset;			// Set by S3Type.Scalar.createLayout

    public Selector.Field getSelector() {
        return sel;
    }

    public String toString() {
	return getSelector().toString();
    }

    // May change if we support renaming
    public int hashCode() {
	return sel.hashCode();
    }

    // FIXME haven't thought about how correct this could possibly be ...
    protected Type resolve(TypeName tn) throws LinkageException {
        return declaringType.getContext().typeFor(tn);    
    }

    public Mode.Field getMode() {
        return mode;
    }

    public Object getConstantValue() {
	return declaringType.context_.constantValues.get(this);
    }

    public Type getType() throws LinkageException {
        return resolve(sel.getDescriptor().getType());
    }

    public Type.Compound getDeclaringType() {
        return declaringType;
    }

    public S3Field(RepositoryMember.Field info, S3Type.Scalar declaringType) {
        assert( declaringType.isSharedState() == info.getMode().isStatic());
        this.declaringType = declaringType;
	mode = info.getMode();
	sel = Selector.Field.make(info.getUnboundSelector(),
				  declaringType.getName());
	try {
	    Object cv = info.getConstantValue();
	    if (cv != null)
		declaringType.context_.constantValues.put(this, cv);
	} catch (AssertionError e) {
	    throw new Error("bad constant value in " + this);
	}
    }
    
    /**
     * 
     * This is temporarily useful while working on bug 417.
     * It will go away eventually in favor of API that does not expose actual field
     * offsets. It is currently used only by the StructDumper and some simplejit code
     * that caches a handful of offsets of commonly-accessed fields, and it probably
     * isn't a good idea to ignore blueprints like that. The current implementation
     * of this method assumes offsets cannot differ by blueprint for a single type.
     * That's ok because this method should be long gone by the time that changes.
     * TODO: deprecate this method (it used to be deprecated, but since noone seems
     * to be keen about removing, we can live with it) --jv
     **/
    public int getOffset() {
	assert (offset != 0);
	return offset;
    }

    /** Helper method to convert a null target into the sharedstate instance */
    final Oop checkSharedState(Oop receiver) {
	boolean assertsEnabled = false;
	assert assertsEnabled = true;
        if (assertsEnabled) {
            Type.Compound declType = getDeclaringType();
            boolean isSharedState = declType.isSharedState();
            Oop sharedState;
	    sharedState = declType.getDomain().
                blueprintFor(declType).getSharedState();
            assert ((receiver == null && isSharedState) ||
		    (receiver != null && !isSharedState)||
		    (receiver == sharedState && isSharedState)):
		"Receiver mis-match";
        }
        if (receiver == null) {
            Type.Compound declType = getDeclaringType();
	    receiver = declType.getDomain().
                blueprintFor(declType).getSharedState();
        }
        return receiver;
    }

    public int getFieldID() {
      return ((S3Type.Scalar)declaringType).bug417fieldID( this );
    }
    
    // inlining this would be an obvious optimization. can use PragmaISB in the
    // absence of anything more automatic.
    public VM_Address bug370addressWithin( Oop o) {
    
        o = checkSharedState(o);
        return ((S3Blueprint)o.getBlueprint())
                .bug417addressWithin( o,
                                      ((S3Type.Scalar)declaringType)
                                      .bug417fieldID( this));
    }

/*
    public VM_Address bug370addressWithinForwarded( Oop o) {

      o = checkSharedState(o);
      return ((S3Blueprint)o.getBlueprint())
                .bug417addressWithinForwarded( o,
                                      ((S3Type.Scalar)declaringType)
                                      .bug417fieldID( this));
    }
*/

    public void addPushEffectiveAddress(Cursor c) {
// FIXME 547. The commented code below allows for per-blueprint variations.
// The faster, uncommented code depends on getOffset, which ignores blueprints.
//        int id = ((S3Type.Scalar)declaringType).bug417fieldID( this);
//        c.addSimpleInstruction( DUP);               // oop oop
//        c.addINVOKEINTERFACE( sel_getBlueprint);    // oop bp
//        c.addSimpleInstruction( SWAP);              // bp oop
//        c.addLoadConstant( id);                     // bp oop id
//        c.addINVOKEVIRTUAL( sel_addressWithin);     // adr
        c.addLoadConstant( getOffset());
        // FIXME if used in user-domain code, needs privilege mark of some kind:
        c.addINVOKEVIRTUAL( sel_addressAdd);
    }



    private static final Selector.Method sel_addressAdd = RepositoryUtils
        .methodSelectorFor( "Lovm/core/services/memory/VM_Address;",
                            "add:(I)Lovm/core/services/memory/VM_Address;");
    private static final Class integerClass = java.lang.Integer.class;
    
    public static class Reference extends S3Field implements Field.Reference {
        public // only because of EphemeralBureau, yuck
        Reference( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, Oop value) {
            if (OVMBase.isBuildTime()) {
              bug370addressWithin( dst).setAddress( VM_Address.fromObject( value)); 
            } else {
              MemoryManager.the().setReferenceField( checkSharedState(dst), getFieldID(), value ); // FIXME:: use assertions instead to elide some barriers
            }
        }
        public Oop get( Oop src) {
            return bug370addressWithin( src).getAddress().asOop();
        }
        public void setUnrefined( Oop dst, Object value) {
            if (OVMBase.isBuildTime() || dst==null ) { 
              bug370addressWithin( dst).setAddress( VM_Address.fromObject( value));
            } else {
              MemoryManager.the().setReferenceField( dst, getFieldID(), VM_Address.fromObjectNB(value).asOop() );
            }
        }
        public VM_Address addressWithin( Oop o) { //FIXME: remove this
            return bug370addressWithin( o);
        }
        
        public void addGetfieldQuick(Cursor c) {
         int opcode = REF_GETFIELD_QUICK;
         // FIXME specialcase VM_Address Ephemeral - should go away
         // with advent of general solution
         // another approach would be to dole out Field.Integer for a field
         // of VM_Address type - that's the intended general solution anyway
         if ( JavaNames.ovm_core_services_memory_VM_Address ==
              getSelector().getDescriptor().getType() ) {
             Type t = getDeclaringType();
             Domain d = t.getDomain();
             if ( d.isExecutive()
                 && ( t.getContext() == d.getSystemTypeContext() ) )
                 fail( "This is NOT happening.");
//               opcode = GETFIELD_QUICK; // tag VM_Address as a primitive
         }
         c.addQuickOpcode( opcode, (char)getOffset());
        }
        
        
       public void addPutfieldQuick(Cursor c) {
            c.addQuickOpcode( PUTFIELD_QUICK, (char)getOffset());
        }
        public void addPutfieldQuickWithBarrier(Cursor c) {
            c.addQuickOpcode( PUTFIELD_QUICK_WITH_BARRIER_REF, (char)getOffset());
        }
    }
    
    public static abstract class Primitive extends S3Field {
        Primitive( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void addGetfieldQuick(Cursor c) {
            c.addQuickOpcode( GETFIELD_QUICK, (char)getOffset());
        }
        public void addPutfieldQuick(Cursor c) {
            c.addQuickOpcode( PUTFIELD_QUICK, (char)getOffset());
        }
        public void addPutfieldQuickWithBarrier(Cursor c) {
         c.addQuickOpcode( PUTFIELD_QUICK, (char)getOffset());
     }
        
        public static abstract class Wide extends Primitive {
            Wide( RepositoryMember.Field info, S3Type.Scalar declaringType) {
                super( info, declaringType);
            }
            public void addGetfieldQuick(Cursor c) {
                c.addQuickOpcode( GETFIELD2_QUICK, (char)getOffset());
            }
            public void addPutfieldQuick(Cursor c) {
                c.addQuickOpcode( PUTFIELD2_QUICK, (char)getOffset());
            }
            public void addPutfieldQuickWithBarrier(Cursor c) {
                c.addQuickOpcode( PUTFIELD2_QUICK, (char)getOffset());
            }
        }
    }
    
    public static class Boolean extends Primitive implements Field.Boolean {
        Boolean( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, boolean value) {
            if (OVMBase.isBuildTime() || dst==null) { // FIXME: this is a hack - that we know here that shared state can be done directly...       
              bug370addressWithin( dst).setInt( value ? 1 : 0);
            } else {
              MemoryManager.the().setPrimitiveField( dst, getFieldID(), value );
            }
        }
        public boolean get( Oop src) {
            return 0 != bug370addressWithin( src).getInt();
        }
        public void setUnrefined( Oop dst, Object value) {
            if ( value.getClass() != integerClass )
                set( dst, ((java.lang.Boolean)value).booleanValue());
            else {
                int v = ((java.lang.Integer)value).intValue();
                if ( 0 != ( v & ~1 ) )
                    throw new NumberRanges.NumberRangeException( v, "boolean");
                if (OVMBase.isBuildTime() || dst==null) {        
                  bug370addressWithin( dst).setInt( v);
                } else {
                  MemoryManager.the().setPrimitiveField( dst, getFieldID(), v );
                }
            }
        }
    }
    
    public static class Byte extends Primitive implements Field.Byte {
        Byte( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, byte value) {
            if (OVMBase.isBuildTime() || dst==null) {        
              bug370addressWithin( dst).setInt( value);
            } else {
              MemoryManager.the().setPrimitiveField( dst, getFieldID(), value );
            }
        }
        public byte get( Oop src) {
            return (byte)bug370addressWithin( src).getInt();
        }
        public void setUnrefined( Oop dst, Object value)
          throws BCbootTime {
            if ( value.getClass() == integerClass )
                set( dst,
                     NumberRanges.checkByte( ((java.lang.Integer)value).intValue()));
            else
                set( dst, ((java.lang.Byte)value).byteValue());
        }
    }
    
    public static class Short extends Primitive implements Field.Short {
        Short( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, short value) {
            if (OVMBase.isBuildTime() || dst==null) {        
              bug370addressWithin( dst).setInt( value);
            } else {
              MemoryManager.the().setPrimitiveField( dst, getFieldID(), value );
            }
        }
        public short get( Oop src) {
            return (short)bug370addressWithin( src).getInt();
        }
        public void setUnrefined( Oop dst, Object value) {
            if ( value.getClass() == integerClass )
                set( dst,
                     NumberRanges.checkShort( ((java.lang.Integer)value).intValue()));
            else
                set( dst, ((java.lang.Short)value).shortValue());
        }
    }
    
    public static class Character extends Primitive implements Field.Character {
        Character( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, char value) {
            if (OVMBase.isBuildTime() || dst==null) {        
              bug370addressWithin( dst).setInt( value);
            } else {
              MemoryManager.the().setPrimitiveField( dst, getFieldID(), value );
            }
        }
        public char get( Oop src) {
            return (char)bug370addressWithin( src).getInt();
        }
        public void setUnrefined( Oop dst, Object value) throws BCbootTime {
            if ( value.getClass() == integerClass )
                set( dst,
                     NumberRanges.checkChar( ((java.lang.Integer)value).intValue()));
            else
                set( dst, ((java.lang.Character)value).charValue());
        }
    }
    
    public static class Integer extends Primitive implements Field.Integer {
        public // only because of EphemeralBureau, yuck
        Integer( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, int value) {
            if (OVMBase.isBuildTime() || dst==null) {        
              bug370addressWithin( dst).setInt( value);
            } else {
               MemoryManager.the().setPrimitiveField( dst, getFieldID(), value );
            }
        }
        public int get( Oop src) {
            return bug370addressWithin( src).getInt();
        }
        public void setUnrefined( Oop dst, Object value) {
            set( dst, ((java.lang.Integer)value).intValue());
        }
    }
    
    public static class Long extends Primitive.Wide implements Field.Long {
        Long( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, long value) {
            if (OVMBase.isBuildTime() || dst==null) {        
              bug370addressWithin( dst).setLong( value);
            } else {
              MemoryManager.the().setPrimitiveField( dst, getFieldID(), value );
            }
        }
        public long get( Oop src) {
            return bug370addressWithin( src).getLong();
        }
        public void setUnrefined( Oop dst, Object value) {
            set( dst, ((java.lang.Long)value).longValue());
        }
    }
    
    public static class Float extends Primitive implements Field.Float {
        Float( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, float value) {
            if (OVMBase.isBuildTime() || dst==null) {        
              bug370addressWithin( dst).setFloat( value);
            } else {
              MemoryManager.the().setPrimitiveField( dst, getFieldID(), value );
            }
        }
        public float get( Oop src) {
            return bug370addressWithin( src).getFloat();
        }
        public void setUnrefined( Oop dst, Object value) {
            set( dst, ((java.lang.Float)value).floatValue());
        }
    }
    
    public static class Double extends Primitive.Wide implements Field.Double {
        Double( RepositoryMember.Field info, S3Type.Scalar declaringType) {
            super( info, declaringType);
        }
        public void set( Oop dst, double value) {
            if (OVMBase.isBuildTime() || dst==null) {        
              bug370addressWithin( dst).setDouble( value);
            } else {
              MemoryManager.the().setPrimitiveField( dst, getFieldID(), value );
            }
        }
        public double get( Oop src) {
            return bug370addressWithin( src).getDouble();
        }
        public void setUnrefined( Oop dst, Object value) {
            set( dst, ((java.lang.Double)value).doubleValue());
        }
    }
}
