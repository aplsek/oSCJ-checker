package s3.services.bootimage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import ovm.core.domain.Blueprint;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import ovm.core.services.memory.MemoryManager;
import s3.core.domain.S3Blueprint;
import s3.core.domain.MachineSizes;
import s3.util.Walkabout;

/**
 * Write object to memory.
 * @author Jan Vitek (based on everybody's ImageSerializer)
 * <blockquote><em>Y el ImageSerializer de ustedes, que es el mismo ImageSerializer
 * <br />Y el ImageSerializer de todos, que es mi propio ImageSerializer</em>
 *         <br />with apologies to Violeta Parra</blockquote>
 */
public class ISerializer extends Walkabout implements Ephemeral.Void {
    protected Blueprint blueprint_;
    private final DomainSprout dsprout;
    protected Oop oop_;
    private final S3Blueprint.Array bpO_;

    // We don't want to recursively walk about, we just want to visit
    // the fields of each object in turn.
    protected boolean markAsSeen(Object o) { return false; }

    public ISerializer(DomainSprout dsprout) {
        super(false, true);
        // ensures that primitive fields will be traversed
        this.dsprout = dsprout;
	bpO_ = blueprint(Object[].class);
   
	DomainSprout.registerAdvice(this);
        register(new ObjectHandler());
	registerAfter(new BlueprintHandler());
	registerAfter(new RefArrayHandler());
	registerAfter(new PrimArrayHandler(boolean[].class));
	registerAfter(new PrimArrayHandler(byte[].class));
	registerAfter(new PrimArrayHandler(char[].class));
	registerAfter(new PrimArrayHandler(short[].class));
	registerAfter(new PrimArrayHandler(int[].class));
	registerAfter(new PrimArrayHandler(long[].class));
	registerAfter(new PrimArrayHandler(float[].class));
	registerAfter(new PrimArrayHandler(double[].class));
	// Strings are "squirted" by GC, and we don't need to write
	// their fields normally.
	registerAfter(new IgnoreObjectAdvice(String.class));
    }
    // --------------------ObjectHandler----------------------------
    /**
     * This advice is responsible for writing instance field values
     * into the image.  Array elements and static fields are dealt
     * with seperately.
     **/
    class ObjectHandler extends BootBase implements FieldAdvice, StaticFieldAdvice {

        public Class targetClass() { return Object.class;  }

        public Object beforeField(Object o, Field f, Object value) {
            Oop oop;
            try { oop = VM_Address.fromObject( o).asOop(); }
            catch (LinkageException.Runtime le) {
                d( "WARNING: no type for " + o.getClass().getName() +
                   ". Field set to null.");
                return null;
            }

            Object orig = value;
	    value = before(value);
            if (value == null) 
                return null;
            
             writeField( oop, f, value);
            return null;
        }
    }

    /**
     * This advice is responsible for writing the values of static
     * fields into the image.  It is called just before the instance
     * fields of the corresponding shared-state blueprint are written.
     * We could also define advice on the shared-state placeholder
     * type, if that type where public.
     **/
    class BlueprintHandler extends BootBase implements ObjectAdvice {
	public Class targetClass() { return Blueprint.class; }

        public Object beforeObject(Object object) {
            assert(!(object instanceof Class));
	    Blueprint bp = (Blueprint) object;
	    if (bp.isSharedState()
		&& (bp.getType().getDomain()
		    == DomainDirectory.getExecutiveDomain()))
		// recover the oop
		writeStaticFields(bp.getInstanceBlueprint().getSharedState());
	    return object;
	}

    } // End of BlueprintHandler

    /**
     * This advice is responsible for writing primitive array
     * contents.  It should be uses as <em>after</em> advice to avoid
     * repeated calls.
     **/
    class PrimArrayHandler extends BootBase implements ObjectAdvice {
	private Class c;
	public PrimArrayHandler(Class c) { this.c = c; }
	
	public Class targetClass() { return c; }
	
	public Object beforeObject(Object object) {
	    Class objClass = object.getClass();
	    VM_Address address = VM_Address.fromObject(object);
	    Oop oop = address.asOop();

	    Blueprint.Array bp = (Blueprint.Array) oop.getBlueprint();
	    int len = bp.getLength(oop);
	    VM_Word size = VM_Word.fromInt(bp.byteOffset(len));
	    try {
		address.setBlock(address, size);
	    } catch (ArrayIndexOutOfBoundsException e) {
		System.err.println("writing " + object + " (" + bp + "):");
		System.err.println("real length = " +
				   java.lang.reflect.Array.getLength(object) +
				   ", ovm length = " + len);
		System.err.println("runtime address = " +
				   Integer.toHexString(address.asInt()));
		throw e;
	    }
	    return object;
	}
    }

    /**
     * This advice is responsible for writing reference array
     * contents.  It should be uses as <em>after</em> advice to avoid
     * repeated calls.
     **/
    class RefArrayHandler extends BootBase implements ObjectAdvice {
	public Class targetClass() { return Object[].class; }
	
	public Object beforeObject(Object object) {
	    Class objClass = object.getClass();
	    VM_Address address = VM_Address.fromObject(object);
	    Oop oop = address.asOop();
	    	    
	    Object[] array = (Object[]) object;
	    int bytesToData = 0;
	    if (MemoryManager.the().usesArraylets()) {
	      bytesToData = MemoryManager.the().continuousArrayBytesToData(bpO_,array.length);	    
	      if (false) {
  	        System.err.println(" RefArrayHandler, skipping arraylets in "+bpO_+" of length "+array.length+
	          "bytesToData = "+bytesToData+"without arraylets = "+bpO_.byteOffset(0)+ 
	          "component size = "+bpO_.getComponentSize());
              }
            } 
            // note that an array can be empty ; then it has no arraylets, so we don't care
            // that we have bytesToData == -1
	    for (int i = 0; i < array.length; i++) {
		Object value = array[i];
		if (value == null)
		    continue; // Assume zero-inited heap, ignore null vals
		value = before(value);
		if (value == null)
		    continue;
		VM_Address objectAddress = null;
		if (!(value instanceof VM_Word))
		    try {
			objectAddress = VM_Address.fromObject(value);
		    } catch (LinkageException.Runtime e) {
			//d(e.toString());
			objectAddress = VM_Address.fromObject(null);
		    } catch (IllegalStateException e) {
			d("ERROR: element of " + objClass + " found late: "
			  + value);
			objectAddress = VM_Address.fromObject(null);
		    }

		VM_Address addr = null;
                if (MemoryManager.the().usesArraylets()) {
                  addr = address.add(bytesToData + i*MachineSizes.BYTES_IN_WORD);
                } else {
                  addr = address.add(bpO_.byteOffset(i));
                }

		if (value instanceof VM_Word) {
		    addr.setWord((VM_Word) value);
		} else {
		    addr.setAddress(objectAddress);
		}
	    }
	    return object;
	}
    }
    // ------------ utility ---------------
    protected S3Blueprint.Array blueprint(Class cl) {
        ovm.core.domain.Domain dom = dsprout.getDomain();
        return (S3Blueprint.Array) ReflectionSupport.blueprintFor(cl, dom);

    }

    protected void writeFieldValue(
        Oop address,
        Blueprint blueprint,
        ovm.core.domain.Field ovmFld,
        Class fieldType,
        Object value) {

        try {
	    // If there is no VM_Address for value, we get an
	    // IllegalStateException
            ovmFld.setUnrefined(address, value);
        } catch (LinkageException.Runtime ovme) {
             d("boot-time value of " + ovmFld +
// 	       " in " + VM_Address.fromObject(address).asObject() +
// 	       " of type " + address.getBlueprint() + 
	       ": " + ovme.toString());
	} catch (IllegalStateException e) {
	    String suffix = "";
// 	    try { suffix = ": " + value; }
// 	    catch (NullPointerException _) { }
	    d("ERROR: value of " + ovmFld + " found late" + suffix);
	}
    }

    protected void writeField(Oop target, Field f, Object value) {
        // FIXME: used to be: value = dsprout.stringByteArrays.get(value);
        if (oop_ != target) {
            oop_ = target;
            blueprint_ = oop_.getBlueprint();
        }
        
        // We may be hitting an ephemeral object from a non-ephemeral
        // field, in which case we should avoid writing it, because
        // the object advice that will execute next will substitute
        // null anyway. Or, we may be hitting a shared-state
        // placeholder that should be given magic handling according
        // to VM_Address.fromObject.
        //         if (value instanceof Ephemeral)
        //             return;
        Class fieldType = f.getType();
        ovm.core.domain.Field ovmFld =
            ReflectionSupport.ovmFieldFor(f, blueprint_);
        if (ovmFld == null) {
            System.err.println(
                "ReflectionSupport could not find field "
                    + f.toString()
                    + " in blueprint "
                    + blueprint_);
	    return;
	}
	writeFieldValue(oop_, blueprint_, ovmFld, fieldType, value);
    }

    protected void writeStaticFields(Oop oop) {
        Blueprint bp = oop.getBlueprint();
	if (bp.isSharedState() && !bp.getInstanceBlueprint().isScalar())
	    return;
        TypeName.Compound bpName = bp.getName().getInstanceTypeName().asCompound();

        Class cl = ReflectionSupport.classForTypeName(bpName);
        if (cl == null)
            fail("not found " + bpName);
	else if (cl.getClassLoader() == null) {
	    // classes in ovm's runtime library will have different
	    // static fields than classes in our current runtime library.
            writeRTLStaticFields(oop, bp);
            return;
        } else if (Ephemeral.class.isAssignableFrom(cl)) {
	    // VM_Address is not the only such class.  For some
            // reason, GC never walks the static fields of classes
            // declared Ephemeral.  (Probably because it is not
            // supposed to.)  GC does, however, walk the type,
            // shared-state type, and shared state object of every
            // Ephemeral type.  If a class is declared Ephemeral, but
            // someone accesses it's static fields, they will show up
            // as uninitialized.
	    //
            // d(" dropping VM_Address Class and its statics ");
            return;
        }

        Field[] fields = cl.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (!Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            try {
                writeField(oop, field, field.get(cl));
            } catch (IllegalAccessException e) {
                throw failure(e);
            }
        }
    }

    protected void writeRTLStaticFields(Oop oop, Blueprint bp) {
        Type.Scalar type = bp.getType().getSharedStateType();
        for (ovm.core.domain.Field.Iterator it = type.localFieldIterator(); 
	     it.hasNext(); ) {
	    ovm.core.domain.Field ovmFld = it.next();
            Object value = ovmFld.getConstantValue();
            if (value == null)
                continue;
            Class jdkType = value.getClass();
            assertValidConstantValueType(jdkType);
	    writeFieldValue(oop, bp, ovmFld, jdkType, value);
        }
    }
    private static void assertValidConstantValueType(Class cl) {
        if ((cl == String.class)
            || (cl == Integer.class)
            || (cl == Long.class)
            || (cl == Float.class)
            || (cl == Double.class)) {
            return;
        }
        fail("bad value type: " + cl);
    }


}
