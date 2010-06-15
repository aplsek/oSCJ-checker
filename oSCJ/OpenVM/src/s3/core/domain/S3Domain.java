package s3.core.domain;

import java.lang.reflect.Array;

import ovm.core.Executive;
import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Field;
import ovm.core.domain.JavaDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.domain.ReflectiveArray;
import ovm.core.domain.ReflectiveConstructor;
import ovm.core.domain.ReflectiveField;
import ovm.core.domain.ReflectiveMethod;
import ovm.core.domain.Type;
import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.execution.Engine;
import ovm.core.execution.InstantiationMessage;
import ovm.core.execution.Native;
import ovm.core.execution.ReturnMessage;
import ovm.core.execution.RuntimeExports;
import ovm.core.repository.Attribute;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Mode;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.ScopedMemoryContext;
import ovm.core.services.memory.ScopedMemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.util.ArrayList;
import ovm.util.ByteBuffer;
import ovm.util.Collections;
import ovm.util.HashMap;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.Map;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;
import ovm.util.UnsafeAccess;
import s3.services.bytecode.ovmify.IRewriter;
import s3.services.simplejit.CodeGenContext;
import s3.services.simplejit.SimpleJIT;
import s3.services.simplejit.dynamic.SimpleJITDynamicCompiler;
import s3.services.simplejit.dynamic.SimpleJITDynamicDomainCompiler;
import s3.services.transactions.Transaction;
import s3.util.PragmaAtomic;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import ovm.util.Mem;
import s3.util.PragmaMayNotLink;
import ovm.core.stitcher.InvisibleStitcher;

/**
 * The base implementation class for the OVM's executive domain and Java
 * user-domain(s). As well as the common Java domain functionality this class
 * provides the support methods needed as part of the image building process.
 */
public abstract class S3Domain extends OVMBase 
    implements JavaDomain, UnsafeAccess {

    // UnicodeBuffer -> Oop 
    private final Map internTable = new HashMap(127);
    
    // our common types utility class
    private volatile JavaTypes commonTypesInstance;

    public JavaTypes commonTypes() {
        // here's a race but it's OK because if two JavaTypes objects
        // are created, they have the same contents (hopefully nobody
        // cares about identity and such
        if (commonTypesInstance == null) {
	    Type.Context sctx = getSystemTypeContext();
	    Object r = MemoryPolicy.the().enterMetaDataArea(sctx);
	    try { commonTypesInstance = new JavaTypes(this); }
	    finally { MemoryPolicy.the().leave(r); }
        }
        return commonTypesInstance;
    }

    /* --- sizeOf utility methods --- */

    /** 
     * Utility method to find sizeOf information given a system class name
     * in the exective domain.
     * @param name the raw class name in / format eg. ovm/core/OVMBase
     */
    public static int sizeOfInstance(String name) {
        return ((S3Domain)DomainDirectory.getExecutiveDomain()).sizeOfClassInstance(name);
    }

    /** 
     * Utility method to find sizeOf information given a system class name.
     *
     * @param classname the raw class name in / format eg. ovm/core/OVMBase
     * or javax/realtime/RealtimeThread
     */
    public int sizeOfClassInstance(String classname) {
        TypeName.Compound name = 
            JavaFormat._.parseTypeName(classname).asCompound();
        if (name == null)
            throw new OVMError.Internal("parsing: apparent error in classname");
        Type.Context ctx = getSystemTypeContext();
        try {
            Blueprint bp = blueprintFor(name, ctx);
            return bp.getFixedSize();
        }
        catch(LinkageException e) {
            throw new OVMError.Internal("blueprint not found", e);
        }
    }

    public long sizeOfReferenceArray(int length) {
        Type.Array arr = makeType(commonTypes().java_lang_Object, 1);
	return blueprintFor(arr).asArray().computeSizeFor(length);    
    }

    /**
     * Returns a size estimate in bytes for an array of the given length for
     * the given primitive type. 
     */
    public long sizeOfPrimitiveArray(int length, char tag) {
        Type.Array arr = null;
        JavaTypes t = commonTypes();
        switch (tag) {
        case 'Z': arr = t.arr_boolean; break;
        case 'B': arr = t.arr_byte; break;
        case 'C': arr = t.arr_char; break;
        case 'S': arr = t.arr_short; break;
        case 'I': arr = t.arr_int; break;
        case 'J': arr = t.arr_long; break;
        case 'F': arr = t.arr_float; break;
        case 'D': arr = t.arr_double; break;
        default: throw new OVMError.Internal("invalid type tag " + tag);
        }
	return blueprintFor(arr).asArray().computeSizeFor(length);
    }

    /*-----*/

    // Blueprint factory and image related stuff

    private static char globalInstanceCounter_ = 0; // char??
    // FIXME: thread safety?
    final char instanceCounter_ = globalInstanceCounter_++;

    public char getUID() { return instanceCounter_; }

    /**
     * Map from this domain's <code>Type</code> objects
     * to its <code>S3Blueprint</code> objects.
     * Type objects are created in two places: SharedStateClass types are
     * created in the constructors of their corresponding plain types (so
     * are naturally unique), and all other types are created here (uniquely).
     * So here again an IdentityHashMap is appropriate.
     *
     * Because S3Type.hashCode is pretty fast, and S3Type.equals is
     * based on pointer equality, IdentityHashMap doesn't buy us much,
     * if anything.  We can use a plain HashMap to avoid rehashing
     * when System.identityHashCode changes.
     **/
    protected Map blueprints = new HashMap();

    protected final S3TypeContext context_;
    protected final String bootClassPath;

    protected Subtyping subtyping = new Subtyping(this);

    /** 
     * the root types that must be pulled into the type closure for this
     * domain. 
     */
    protected HashSet rootTypes = new HashSet();
    protected HashSet reflectiveFields = new HashSet();
    protected HashSet reflectiveCalls = new HashSet();
    protected HashSet reflectiveVirtualCalls = new HashSet();
    protected HashSet reflectiveNews = new HashSet();

    public void registerRoot(TypeName.Compound tn) {
	tn = tn.getInstanceTypeName().asCompound();
	//System.err.println("registerRoot " + tn);
        if (subtyping == null)
            throw new IllegalStateException("Domain frozen; Trying to add "+ tn);
	rootTypes.add(tn);
    }

    public void registerNew(TypeName.Compound tn) {
	//System.err.println("registerNew " + tn);
	registerRoot(tn);
	reflectiveNews.add(tn);
    }
    
    public void registerField(Selector.Field sel) {
	registerRoot(sel.getDefiningClass());
	reflectiveFields.add(sel);
    }

    public void registerCall(Selector.Method sel) {
	//System.err.println(this + ".registerCall(" + sel + ")");
	registerRoot(sel.getDefiningClass());
	reflectiveCalls.add(sel);
	if (sel.isConstructor())
	    registerNew(sel.getDefiningClass());
    }

    public void registerVirtualCall(Selector.Method sel) {
	registerRoot(sel.getDefiningClass());
	reflectiveVirtualCalls.add(sel);
    }
    /**
     * Return the specific root types for this domain. These are used by
     * the build process to determine the type closure.
     * @return The array of specific roots, or a zero length array if there
     * are none.
     */
    public TypeName.Compound[] getRoots() {
	TypeName.Compound[] ret = new TypeName.Compound[rootTypes.size()];
	rootTypes.toArray(ret);
	return ret;
    }

    private Object[] shrink(Object[] big, int size) throws BCdead {
	if (size == big.length)
	    return big;
	Object[] ret =
	    (Object[]) Array.newInstance(big.getClass().getComponentType(),
					 size);
	System.arraycopy(big, 0, ret, 0, size);
	return ret;
    }

    public Type[] getRootTypes() {
	Type.Context ctx = getApplicationTypeContext();

	// Ick.  root types in the executive domain contain static
	// initializers that define additional root types, so we may
	// need to compute the list a second time.  Also, some root
	// type names may not correspond to actual types.  For reasons
	// that are now forgotten, we ignore these "funny" type names.
	while (true) {
	    TypeName[] names = getRoots();
	    Type[] ret = new Type[rootTypes.size()];
	    int i, j;
	    for (i = 0, j = 0; i < ret.length; i++) {
		try { ret[j] = ctx.typeFor(names[i]); j++; }
		catch (LinkageException _) { }
	    }

	    if (j <= rootTypes.size())
		return (Type[]) shrink(ret, j);
	}
    }

    public Type[] getReflectiveNews() {
	Type[] ret = new Type[reflectiveNews.size()];
	Type.Context ctx = getApplicationTypeContext();
	int i = 0;
	Iterator it = reflectiveNews.iterator();
	while (it.hasNext())
	    try {
		ret[i] = ctx.typeFor((TypeName) it.next());
		i++;
	    } catch (LinkageException e) {
		System.err.println(e);
	    }
	return (Type[]) shrink(ret, i); 
    }

    public Field[] getReflectiveFields() {
	Field[] ret = new Field[reflectiveFields.size()];
	Type.Context ctx = getApplicationTypeContext();
	int i = 0;
	for (Iterator it = reflectiveFields.iterator(); it.hasNext(); ) {
	    Selector.Field sel = (Selector.Field) it.next();
	    try {
		Type t = ctx.typeFor(sel.getDefiningClass());
		Field f = t.getField(sel.getUnboundSelector());
		if (f != null)
		    ret[i++] = f;
	    } catch (LinkageException e) {
		System.err.println(e);
	    }
	}
	return (Field[]) shrink(ret, i);
    }

    public Method[] getReflectiveCalls() {
	Method[] ret = new Method[reflectiveCalls.size()];
	Type.Context ctx = getApplicationTypeContext();
	int i = 0;
	for (Iterator it = reflectiveCalls.iterator(); it.hasNext(); ) {
	    Selector.Method sel = (Selector.Method) it.next();
	    try {
		Type.Scalar t =
		    (Type.Scalar) ctx.typeFor(sel.getDefiningClass());
		Method m = t.getMethod(sel.getUnboundSelector(), true);
		if (m == null)
		    // FIXME: Too many warnings for exception ctors.
		    // Maybe we should resolve ReflectiveCall methods
		    // early, and generate a warning unless a special
		    // nowarn argument is provided to the ReflectiveCall
		    // constructor? 
		    //System.err.println(sel + " not found")
		    ;
		else
		    ret[i++] = m;
	    } catch (LinkageException e) {
		System.err.println(e);
	    }
	}
	return (Method[]) shrink(ret, i);
    }

    public Method[] getReflectiveVirtualCalls() {
	Method[] ret = new Method[reflectiveVirtualCalls.size()];
	Type.Context ctx = getApplicationTypeContext();
	int i = 0;
	for (Iterator it = reflectiveVirtualCalls.iterator(); it.hasNext(); ) {
	    Selector.Method sel = (Selector.Method) it.next();
	    try {
		Type.Scalar t =
		    (Type.Scalar) ctx.typeFor(sel.getDefiningClass());
		Method m = t.getMethod(sel.getUnboundSelector(), true);
		if (m == null)
		    // FIXME: Too many warnings for exception ctors.
		    // Maybe we should resolve ReflectiveCall methods
		    // early, and generate a warning unless a special
		    // nowarn argument is provided to the ReflectiveCall
		    // constructor? 
		    //System.err.println(sel + " not found")
		    ;
		else
		    ret[i++] = m;
	    } catch (LinkageException e) {
		System.err.println(e);
	    }
	}
	return (Method[]) shrink(ret, i);
    }

    private IRewriter rewriter;
    public IRewriter getRewriter() {
	return rewriter;
    }

    protected DispatchBuilder dispatchBuilder = new DispatchBuilder();
    public DispatchBuilder getDispatchBuilder() {
	return dispatchBuilder;
    }

    /**
     * internString subroutine.  When interning a newly seen domain
     * string, we should only allocate a copy if RTSJ forces us to.
     * @param key          A UnicodeBuffer for the string's contents
     * @param defaultValue an existing domain string, or null.
     */
    private Oop internString(UnicodeBuffer key, Oop defaultValue) {
	Object r = MemoryPolicy.the().enterInternedStringArea(this);
	try {
	    synchronized (internTable) {
		Oop ret = VM_Address.fromObject(internTable.get(key)).asOop();
		if (ret == null) {
		    // Make sure we have a domain string in the internedString area 
		    if (defaultValue == null
			|| !MemoryPolicy.the().isInternable(this,defaultValue))
			ret = makeString(key);
		    else
			ret = defaultValue;

		    // And make sure our hash key is also in this area
		    if (!MemoryPolicy.the().isInternable
			(this, VM_Address.fromObject(key).asOop()))
			key = getString(ret);

		    internTable.put(key, ret);
		} 
		return ret;
	    }
	} finally { MemoryPolicy.the().leave(r); }
    }

    /**
     * This method should either return null, or delegate to
     * ReflectiveVirtualFunction.findMethod.  It is not defined on the
     * OVM side, because it really should not exist.  Our current
     * handling of finalizer in RTSJ memory areas is broken, and is
     * destined to be replaced once we have a framework for
     * finalizers, weak references, and phantom references in
     * ovm.core.services.memory.
     **/
    public abstract Method findFinalizer(Oop oop);
    
    private ReflectiveConstructor decodeString;
    private ReflectiveArray byteArray;

    // the localized C string is contiguous ( char * in C )
    private Oop stringFromLocalizedCString(VM_Address cstring, int len) {
	Oop arr = byteArray.make(len);

	if (MemoryManager.the().usesArraylets()) {
	  int arrayletSize = MemoryManager.the().arrayletSize();
	  int aPtrOffset = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD /* array length field */;
	  
	  int toStore = len;
	  int toStoreNow = 0;
	  VM_Address cptr = cstring;
	  
	  while (toStore > 0) {
            VM_Address curArr = VM_Address.fromObject(arr);
            
	    if (toStore >= arrayletSize) {
	      toStoreNow = arrayletSize;
	      // here we know that the array cannot move, because we are in arraylet that is either
	      // external, or internal within a large object
	      
	      if (true) {
	        MemoryManager.the().checkAccess( curArr.add(aPtrOffset).getAddress() );
	        MemoryManager.the().checkAccess( curArr.add(aPtrOffset).getAddress().add(toStoreNow-1) );
	        MemoryManager.the().checkAccess( cptr );
	        MemoryManager.the().checkAccess( cptr.add(toStoreNow-1) );
	      }
	      Mem.the().cpy( curArr.add(aPtrOffset).getAddress(), cptr, toStoreNow );
	      aPtrOffset += MachineSizes.BYTES_IN_ADDRESS;
	      toStore -= toStoreNow;
	      cptr = cptr.add(toStoreNow);
	      
            } else {

              // here we have to be prepared that the array can move
              // we also know that this is the last chunk to be copied
              
              int relativeOffset = curArr.add(aPtrOffset).getAddress().diff(curArr).asInt();  
              
	      if (true) {
	        MemoryManager.the().checkAccess( curArr.add(relativeOffset) );
	        MemoryManager.the().checkAccess( curArr.add(relativeOffset + toStore-1) );
	        MemoryManager.the().checkAccess( cptr );
	        MemoryManager.the().checkAccess( cptr.add(toStore-1) );
	      }              
              Mem.the().cpy( arr, relativeOffset, cptr, toStore );
                // since len < arrayletSize, we are in the spine
                //   thus, if the array moves, the byte offset will still work
              break ;
            }
	  }
	  
	} else {
	  /*
  	  Mem.the().cpy(byteArray.bp().addressOfElement(arr, 0), // FIXME-FATAL!!! - the array can move
		      cstring, len);
          */
          Mem.the().cpy(arr, byteArray.bp().byteOffset(0), cstring, len);
        }
	return decodeString.make(arr);
    }

    public Oop stringFromLocalizedCString(VM_Address cstring) {
	int len = 0;
	while (cstring.add(len).getByte() != 0)
	    len++;
	return stringFromLocalizedCString(cstring, len);
    }

    public Oop stringFromLocalizedCString(String edString) {
	S3ExecutiveDomain ed =(S3ExecutiveDomain)
	    DomainDirectory.getExecutiveDomain();
	Oop soop = VM_Address.fromObject(edString).asOop();
	Oop arr = ed.string_data.get(soop);
	int offset = ed.string_offset.get(soop);
	Blueprint.Array arrBp =  arr.getBlueprint().asArray();
	
	Oop ret = null;
	
	if (MemoryManager.the().usesArraylets()) {
	  // although the executive domain string is probably contiguous, I
	  // don't think we have this guaranteed
	  
	  int length = edString.length();
	  Oop udarr = byteArray.make(length);
	  MemoryManager.the().copyArrayElements( arr, offset, udarr, 0, length );
	  ret = decodeString.make(udarr);

	} else {
  	  MemoryManager.the().pin(arr);
  	  ret = stringFromLocalizedCString(arrBp.addressOfElement(arr, offset),
					     edString.length());
          MemoryManager.the().unpin(arr);
        }
	return ret;
    }

    public Oop internString(int utf8Index) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    UnicodeBuffer b = UTF8Store._.getUtf8(utf8Index);
	    return internString(b, null);
	} finally { MemoryPolicy.the().leave(r1); }
    }

    public Oop internString(UnicodeBuffer b) {
	return internString(b, null);
    }

    public Oop internString(Oop string) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
	try {
	    UnicodeBuffer b = getString(string);
	    return internString(b, string);
	} finally { MemoryPolicy.the().leave(r1); }
    }

    public byte[] getUTF8CString(Oop dstr) {
	UnicodeBuffer b = getString(dstr);
//	byte[] ret = new byte[b.byteCount() + 1];
        byte[] ret = MemoryManager.the().allocateContinuousByteArray( b.byteCount() + 1 ); // passed to native code
	b.rewind();
	for (int i = 0; b.hasMore(); i++)
	    ret[i] = (byte) b.getByte();
	return ret;
    }

    /**
     * {@inheritDoc}
     * <p>Based on the parameters passed we require the throwable to have
     * a constructor of a suitable signature, either:
     * <ul>
     * <li><tt>throwable()</tt></li>
     * <li><tt>throwable(String message)</tt></li>
     * <li><tt>throwable(Throwable cause)</tt></li>
     * <li><tt>throwable(String message, Throwable cause)</tt></li>
     * </ul>
    */
    public Oop makeThrowable(int code,
			     Oop message,
			     Oop throwableCause) {
	return
	    (message == null
	     ? (throwableCause == null
		? makeThrowable[code].make()
		: makeThrowable_cause[code].make(throwableCause))
	     : (throwableCause == null
		? makeThrowable_mesg[code].make(message)
		: makeThrowable_mesg_cause[code].make(message, throwableCause)));
    }

    /**
     * A field of the object passed to fillInStackTrace and
     * getStackTrace where an array of code objects can be stored.
     * This must be set by concrete S3Domain subtypes.
     **/
    protected ReflectiveField.Reference throwableCode;
    /**
     * A field of the object passed to fillInStackTrace and
     * getStackTrace where an array of pc values can be stored.
     * This must be set by concrete S3Domain subtypes.
     **/
    protected ReflectiveField.Reference throwablePC;

    private final ReflectiveConstructor makeStackTraceElement;
    private final ReflectiveArray stackTraceElementArr;

    // SCJ
	final static boolean DEBUG_SCJ = false;
	private final ReflectiveMethod throwableFillInStackTrace;

    // Warning: this is called as part of exception processing. Any
    // exceptions here will trigger a recursive exception panic.
    public void fillInStackTrace(Oop throwable, Context ctx) {
    	
    	Object o = null;
	
    	if(ctx instanceof ScopedMemoryContext) {
    		ScopedMemoryContext sctx = (ScopedMemoryContext) ctx;
    		sctx.setMostRecentThrowable(throwable);

    		if(DEBUG_SCJ){
    			Native.print_string("[SCJ DB] S3Domain.fillInStackTrace() - set current exception at: ");
    			Native.print_ptr(VM_Address.fromObject(throwable));
    			Native.print_string("\n");
    		}	
    	
    		// allocate stack trace in thread local area, 
    		o = ((ScopedMemoryPolicy)MemoryPolicy.the()).enterStackTraceBufArea();
    		VM_Area ssbuf = MemoryManager.the().getCurrentArea();
    		if(DEBUG_SCJ){
    			Native.print_string("[SCJ DB] S3Domain.fillInStackTrace() - enter ");
    			Native.print_string(ssbuf.toString());
    			Native.print_string(" bytes used: ");
    			Native.print_int(ssbuf.memoryConsumed());        
    			Native.print_string("\n");
    		}
    		ssbuf.reset();
    	}
        //------------ thread local buffer entered --------------

        int stackSize = 0; // the depth of the stack trace
        int startIndex = -1; // the frame at which we start populating

        // if we're in the call-chain from generateThrowable then chop off
        // the stacktrace to start two above generateThrowableInternal
        if (ctx.flags[Context.PROCESSING_GENERATED_EXCEPTION] == true) {
	    ctx.flags[Context.PROCESSING_GENERATED_EXCEPTION] = false;
            for (Activation act = ctx.getCurrentActivation();
                 act != null;
                 act = act.caller(act)) {

                stackSize++;

                // FIXME: there has to be a better way to identify this
                // frame. Every method has a unique ID but somehow we have to
                // find out what generateThrowableInternals unique ID is.
                if (act.getCode().getMethod().getSelector().
                    getName().equals("generateThrowableInternal")) {

                    // now we start to count again
                    // note: there are two frames above us to skip, and
                    // stacksize is one more than the last index
                    startIndex = stackSize + 1;
                    stackSize = 0;
                    act = act.caller(act); // generateThrowable*()
                    act = act.caller(act); // real caller that caused exception
                    while (act != null) {
                        stackSize++;
                        act = act.caller(act);
                    }
                    break; // stop processing the stack
                }
            }
        }
        else {
            // just count everything
            for (Activation act = ctx.getCurrentActivation();
                 act != null;
                 act = act.caller(act)) {
                stackSize++;
            }
        }

	Code[] code = new Code[stackSize];
	throwableCode.set(throwable, asOop(code));
	int[] pc = new int[stackSize];
	throwablePC.set(throwable, asOop(pc));
	
        Activation act = null;
        // skip to the starting frame
        int j = 0;
	for (act = ctx.getCurrentActivation(); 
             j < startIndex;
	     act = act.caller(act), j++) {
            // skip
        }

        // now store everything else
	int i = 0;
        do {
	    code[i] = act.getCode();
	    pc[i] = act.getPC();
            act = act.caller(act);
            i++;
	} while (act != null);
        
        //------------ thread local buffer exited --------------
    	if(ctx instanceof ScopedMemoryContext) {
    		MemoryPolicy.the().leave(o);
    		if(DEBUG_SCJ){
    			Native.print_string("[SCJ DB] S3Domain.fillInStackTrace() - leave to ");
    			Native.print_string(MemoryManager.the().getCurrentArea().toString());
    			Native.print_string("\n");
    		}
    	}
    }


    // note: this isn't called as part of exception processing, it is used to
    // get the stack trace of an exception that has already been created, 
    // perhaps one that has been caught.
    public Oop getStackTrace(Oop throwable) {
    	Context ctx = Context.getCurrentContext();
    	if(ctx instanceof ScopedMemoryContext) {
    		ScopedMemoryContext sctx = (ScopedMemoryContext)ctx;
    		if(DEBUG_SCJ){
    			Native.print_string("[SCJ DB] S3Domain.getStackTrace() - \n current exception at: ");
    			Native.print_ptr(VM_Address.fromObject(sctx.getMostRecentThrowable()));
    			Native.print_string("\n other exception at: ");
    			Native.print_ptr(VM_Address.fromObject(throwable));
    			Native.print_string("\n");
    		}
    		if(sctx.getMostRecentThrowable() != throwable){
    			// if not the one associated with the buffered stack trace, return null
    			if(DEBUG_SCJ){
    				Native.print_string("[SCJ DB] S3Domain.getStackTrace() - not expected throwable, return null\n");
    			}
    			return stackTraceElementArr.make(0);
    		}
    	}
    	
	Code[] code = (Code[]) (Object) throwableCode.get(throwable);
	int[] pc = (int[]) (Object) throwablePC.get(throwable);
	Oop ret = stackTraceElementArr.make(code.length);
	InstantiationMessage msg = makeStackTraceElement.makeMessage();
	for (int i = 0; i < code.length; i++) {
	    Method meth = code[i].getMethod();
	    Type.Compound declType = meth.getDeclaringType();

	    int srcIndex = declType.getSourceFileNameIndex();
	    msg.getInArgAt(0).setOop(srcIndex == 0
				     ? null
				     : internString(srcIndex));

	    msg.getInArgAt(1).setInt(code[i].getLineNumber(pc[i]));

	    // This allocates a whole hell of a lot!
	    String classNameED = JavaFormat._.format(declType.getName());
	    // This is cheap
	    UnicodeBuffer classNameBuf =
		UnicodeBuffer.factory().wrap(classNameED);
	    // This is cheap in a non-RT context, but will copy in a
	    // non-immortal RT context
	    msg.getInArgAt(2).setOop(internString(classNameBuf));

	    Oop methName = internString(meth.getSelector().getNameIndex());
	    msg.getInArgAt(3).setOop(methName);

	    msg.getInArgAt(4).setBoolean(meth.getMode().isNative());

	    try {
		ReturnMessage rmsg = msg.instantiate();
		rmsg.rethrowWildcard();
		stackTraceElementArr.setOop(ret, i,
					    rmsg.getReturnValue().getOop());
	    } catch (LinkageException e) {
		throw Executive.panic("exception creating stack trace");
	    }
	}
	return ret;
    }

    /**
     * The root <code>Type</code> for this domain
     * (the <code>Type</code> for <code>java.lang.Object</code>)
     *
     * Setting ROOT_TYPE and CLASS_TYPE are a rather tricky part of
     * the domain bootstrapping process (see {@link #bootstrap}}.  The best
     * approach is to initialize these fields in
     * {@link s3.core.domain.S3TypeContext#defineType}.
     **/
    public Type.Class ROOT_TYPE;

    /**
     * The user-domain interface to type objects, and the superclass
     * of all shared-state types.  See {@link #getMetaClass}.
     **/
    public Type.Class CLASS_TYPE;

    /** Blueprint for the root Type object (in this case, the
     * <code>Type</code> for <code>java.lang.Object</code>). **/
    public  S3Blueprint.Scalar ROOT_BLUEPRINT;

    private final Type.Interface[] arrayInterfaces = new Type.Interface[2];

    // ---------------- Primitive type objects for the domain ---------
    /**
     * This domain's <code>Type</code> object for the <code>byte</code>
     * primitive.     **/
    public final Type.Primitive BYTE;
    /**
     * This domain's <code>Type</code> object for the char primitive.     
     * **/
    public final Type.Primitive CHAR;
    /**
     * This domain's <code>Type</code> object for the <code>double</code>
     * primitive.*/
    public final Type.Primitive DOUBLE;
    /**
     * This domain's <code>Type</code> object for the <code>float</code>
     * primitive.*/
    public final Type.Primitive FLOAT;
    /**
     * This domain's <code>Type</code> object for the <code>int</code>
     * primitive.*/
    public final Type.Primitive INT;
    /**
     * This domain's <code>Type</code> object for the <code>long</code>
     * primitive.*/
    public final Type.Primitive LONG;
    /**
     * This domain's <code>Type</code> object for the <code>short</code>
     * primitive.*/
    public final Type.Primitive SHORT;
    /**
     * This domain's <code>Type</code> object for the <code>void</code>
     * primitive.*/
    public final Type.Primitive VOID;
    /**
     * This domain's <code>Type</code> object for the <code>boolean</code>
     * primitive.*/
    public final Type.Primitive BOOLEAN;


    private ReflectiveConstructor[] makeThrowable;
    private ReflectiveConstructor[] makeThrowable_cause;
    private ReflectiveConstructor[] makeThrowable_mesg;
    private ReflectiveConstructor[] makeThrowable_mesg_cause;
    
    protected TypeName.Scalar throwableTypeName(int code) {
	return JavaNames.throwables[code];
    }
    
    /**
     * Construct a domain with the given main class type-name and
     * resource path
     */
    protected S3Domain(String bootrpath) {
        assert bootrpath != null : "bootrpath may not be null";
	this.bootClassPath = bootrpath;
        context_ = new S3TypeContext(this, true); // used below
        VOID = addPrimitive(TypeCodes.VOID); // these use context_
        BOOLEAN = addPrimitive(TypeCodes.BOOLEAN);
	BYTE = addPrimitive(TypeCodes.BYTE);
        SHORT = addPrimitive(TypeCodes.SHORT);
        CHAR = addPrimitive(TypeCodes.CHAR);
        INT = addPrimitive(TypeCodes.INT);
        FLOAT = addPrimitive(TypeCodes.FLOAT);
        LONG = addWidePrimitive(TypeCodes.LONG);
        DOUBLE = addWidePrimitive(TypeCodes.DOUBLE);

	makeThrowable = new ReflectiveConstructor[JavaNames.throwables.length];
	makeThrowable_cause = new ReflectiveConstructor[JavaNames.throwables.length];
	makeThrowable_mesg = new ReflectiveConstructor[JavaNames.throwables.length];
	makeThrowable_mesg_cause = new ReflectiveConstructor[JavaNames.throwables.length];
	for (int i = 0; i < JavaNames.throwables.length; i++) {
	    TypeName.Scalar tn = throwableTypeName(i);
	    makeThrowable[i] = new ReflectiveConstructor(this, tn, new TypeName[] {});
	    makeThrowable_cause[i] = new ReflectiveConstructor(this, tn,
		    new TypeName[] { JavaNames.java_lang_Throwable });
	    makeThrowable_mesg[i] = new ReflectiveConstructor(this, tn, new TypeName[] { JavaNames.java_lang_String });
	    makeThrowable_mesg_cause[i] = new ReflectiveConstructor(this, tn, new TypeName[] {
		    JavaNames.java_lang_String, JavaNames.java_lang_Throwable });
	}
	makeStackTraceElement =
	    new ReflectiveConstructor(this,
				      JavaNames.java_lang_StackTraceElement,
				      new TypeName[] {
					  JavaNames.java_lang_String,
					  TypeName.INT,
					  JavaNames.java_lang_String,
					  JavaNames.java_lang_String,
					  TypeName.BOOLEAN
				      });
	stackTraceElementArr =
	    new ReflectiveArray(this, JavaNames.java_lang_StackTraceElement);
	decodeString =
	    new ReflectiveConstructor(this, JavaNames.java_lang_String,
				      new TypeName[] {
					  JavaNames.arr_byte,
				      });
	byteArray = new ReflectiveArray(this, TypeName.BYTE);
	
	//SCJ
	throwableFillInStackTrace = new ReflectiveMethod(this, 
			Selector.Method.make(JavaNames.THROWABLE_FILLINSTACKTRACE, 
					JavaNames.java_lang_Throwable));
    }
    
    // SCJ
    public void callFillInStackTraceBeforeThrowPreallocatedThrowable(Oop recv){
    	throwableFillInStackTrace.call(recv);
    }

    protected CoreServicesAccess myCSA;
    public CoreServicesAccess getCoreServicesAccess() { return myCSA; }
    
    protected RuntimeExports myRTE;
    public RuntimeExports getRuntimeExports() { return myRTE; }

    /**
     * {@inheritDoc}
     *
     * In addition this method loads the basic classes needed to
     * define array types, and creates the bytecode rewriter.
     */
    public void bootstrap() throws LinkageException {
	// Defining java.lang.Object implicitly sets ROOT_TYPE and iterates
        // over its methods
	context_.bootstrap();

        arrayInterfaces[0] = 
            (Type.Interface) context_.typeFor(JavaNames.java_lang_Cloneable);
        arrayInterfaces[1] =
            (Type.Interface) context_.typeFor(JavaNames.java_io_Serializable);

	ROOT_BLUEPRINT = (S3Blueprint.Scalar) blueprints.get(ROOT_TYPE);
	assert ROOT_BLUEPRINT != null;

	BasicIO.out.println("core bootstrapping complete in " + this);
	if (isExecutive())
	    InvisibleStitcher.bootstrapComplete();

	myCSA = CoreServicesAccess.Factory.the().make(this);
	myRTE = new RuntimeExports(this);
	rewriter = new IRewriter(this);

	// Now, make sure that all the blueprints we loaded early have
	// myCSA set.  This field is about as useless as it sounds,
	// unless your are the bytecode interpreter.
	for (Iterator it = getBlueprintIterator(); it.hasNext(); ) {
	    S3Blueprint bp = (S3Blueprint) it.next();
	    bp.myCSA = myCSA;
	}

	// Tell the executive domain about our CSA and RTE
	// implementations.
	S3ExecutiveDomain ed =
	    (S3ExecutiveDomain) DomainDirectory.getExecutiveDomain();
	ed.registerBoundaryMethods(this);
    }


    /**
     * If the domain is not yet frozen, the iterator returned is a 
     * <em>snapshot</em> of the blueprint map, and will not complain 
     * (or even detect) if the map is
     * modified during traversal. That's because this iterator is used
     * <em>during</em> {@link #freezeClassLoading}, but only to
     * ensure all shared state blueprints have been created.  The only
     * new blueprints created can be shared states, and it is not
     * necessary to traverse them. <em>When using this iterator in
     * other code, be sure this behavior is acceptable.</em>
     **/
    public Iterator getBlueprintIterator() {
        return ( false /*null == subtyping*/         // i.e. domain is frozen
                ? blueprints.values()                // return direct iterator
                : new ArrayList( blueprints.values())// else snapshot iterator
                ).iterator();
    }

    public Blueprint blueprintFor(TypeName.Compound name, Type.Context context)
        throws LinkageException {
        assert(context != null);
        try {
            return blueprintFor(context.typeFor(name));
        } catch (ovm.util.ReadonlyViewException e) { // HT... flavor
            throw new LinkageException("adding " + name, e);
        } catch (java.lang.UnsupportedOperationException e) { // Map flavor
            throw new LinkageException("adding " + name, e);
        }
    }

    public void freezeClassLoading(boolean permanently) {
	// getSharedState triggers verification.  This may involve
	// class loading (in particular, the loading of exception
	// types that are never thrown).  We want to make sure that
	// loading and verification reaches a fixpoint before we call
	// context_.freeze().  (Otherwise, verify may crash later on,
	// when we finally allocate the shared-state objects for these
	// newly loaded exception types.)
	int nbp;
	do {
	    nbp = blueprints.size();
	    for (Iterator i = getBlueprintIterator(); i.hasNext();) {
		S3Blueprint bp = (S3Blueprint) i.next();
		if (!bp.isSharedState()) 
		    bp.getSharedState();
	    }
	} while (nbp != blueprints.size());

        subtyping.recomputeSubtypeInfo();
	if (permanently) {
 	    subtyping = null;
	}
        context_.freeze(permanently);
    }

    public void dropCompileTimeData(boolean allCallsSeen) {
	 // check "the characteristic mark of a frozen domain"
	assert (subtyping == null);

	rewriter = null;
	dispatchBuilder = null;
	// Some of this is needed by J2cImageCompiler.compilationComplete():
// 	rootTypes = reflectiveFields = reflectiveNews = null;
// 	reflectiveCalls = reflectiveVirtualCalls = null;

	if  (s3.services.bootimage.ImageObserver.the() instanceof
	     s3.core.services.interpreter.ImageCompiler)
	    return;

	boolean dropConstantPools = 
	    s3.services.bootimage.ImageObserver.the().isJ2c();
	// J2c depends on interpretet dispatch tables until it is done 
	boolean dropDispatchTables = dropConstantPools;

	for (Iterator it = getBlueprintIterator(); it.hasNext(); ) {
	    S3Blueprint bp = (S3Blueprint) it.next();
	    if (!bp.isReference())
		continue;
	    if (allCallsSeen) {
		bp.vtableOffsets_ = null;
		bp.nvtableOffsets_ = null;
	    }
	    S3Type.Reference t = (S3Type.Reference) bp.getType();
	    if (t instanceof S3Type.Interface) {
		// Remove fake code from synthetic S3Method.Multiple
		// interface methods
		// System.err.println("remove all methods of " + t);
		for (Method.Iterator mit = t.methodIterator();  mit.hasNext(); )
		    mit.next().removeCode(S3ByteCode.KIND);
	    } 
	    // System.err.println("remove local methods of " + t);
	    for (int i = t.localMethodCount(); i --> 0; ) {
		t.getLocalMethod(i).removeCode(S3ByteCode.KIND);
	    }

	    if (dropConstantPools && (t instanceof S3Type.Scalar))
		((S3Type.Scalar) t).constants = null;
	    if (dropDispatchTables)
		bp.vTable = bp.ifTable = bp.nvTable = Code.EMPTY_ARRAY;
	}
    }

    public Blueprint blueprintFor(Type type)  {
        synchronized (blueprints) {
            S3Blueprint result = (S3Blueprint) blueprints.get(type);
            if (result != null) {
                return result;
            }
            if (type instanceof Type.Reference) {
                result = buildS3Blueprint((Type.Reference) type);
                try { // FIXME SharedStateClass interfaces[] initialized wrong
                    if ( true || !type.isSharedState()) // so keep s.s. types out of
                        blueprints.put(type, result); // the map (Issue #368)
                } catch (ovm.util.ReadonlyViewException e) { // HT... flavor
                    throw new LinkageException("adding " + type, e).unchecked();
                } catch (java.lang.UnsupportedOperationException e) { // Map
                    throw new LinkageException("adding " + type + "(" + 
                                               type.getContext() + ")", e)
				.unchecked();
                }
                return result;
            }
            throw new OVMError.UnsupportedOperation("Can't handle " + type);
        }
    }

    // look: you can hang an aspect off of this method!
    protected void newTypeHook(Type.Compound type)
	throws LinkageException, PragmaMayNotLink
    {
	if (subtyping == null)
	    throw new LinkageException.DomainFrozen(type.getName());
        subtyping.addType(type);
        blueprintFor(type);
    }

    //FIXME: shouldn't this be called makeArrayType instead ? --JT 01/30/04
    public Type.Array makeType(Type innermost, int depth) {
        TypeName.Array tn =
	    TypeName.Array.make(innermost.getUnrefinedName(), depth);
        try {
            return innermost.getContext().typeFor(tn).asArray();
        } catch (LinkageException e) {
            throw e.fatal("Making array types should always succeed");
        }
    }


    private S3Blueprint buildS3Blueprint(Type.Reference type) {
        assert(blueprints.get(type) == null);
        S3Blueprint.Scalar parent = null;
        Type.Class parentType = type.getSuperclass();
	S3Blueprint ret;
        if (parentType != null) {
            parent = (S3Blueprint.Scalar) blueprintFor(parentType);
        }
        if (type.isArray()) {
            Type.Array atype = type.asArray();
            Blueprint bp = blueprintFor(atype.getComponentType());
	    ret = new S3Blueprint.Array(atype, parent, (S3Blueprint) bp);
        } else if (type.isScalar()) {
            ret =  (parent == null)
                ? new S3Blueprint.Scalar(type.asScalar())
                : new S3Blueprint.Scalar(type.asScalar(), parent);
        } else
            throw new OVMError.UnsupportedOperation("can't deal with " + type);
	if (!isBuildTime() && MemoryManager.the().shouldPinCrazily())
	    MemoryManager.the().pin(ret);
	return ret;
    }


   
    private Type.Primitive addPrimitive(char tag) {
        TypeName.Primitive n = TypeName.Primitive.make(tag);
        Type.Primitive type = new S3Type.Primitive(n, context_);
        context_.getTypes().put(n, type); // unsynchronized access
	Blueprint bp = new S3Blueprint.Primitive(n, type);
	// FIXME: MemoryManager <-> DomainDirectory init cycle
	// We don't really need to pin here, since domains are only
	// created at image-build-time
	// MemoryManager.the().pin(bp);
        blueprints.put(type, bp);
        return type;
    }

    private Type.WidePrimitive addWidePrimitive(char tag) {
        TypeName.WidePrimitive n =
	    TypeName.Primitive.make(tag).asWidePrimitive();
        Type.WidePrimitive type = new S3Type.WidePrimitive(n, context_);
        context_.getTypes().put(n, type); // unsynchronized access
	Blueprint bp = new S3Blueprint.Primitive(n, type);
	// FIXME: MemoryManager <-> DomainDirectory init cycle
	// We don't really need to pin here, since domains are only
	// created at image-build-time
	// MemoryManager.the().pin(bp);
        blueprints.put(type, bp);
        return type;
    }

    public Type.Context getSystemTypeContext() {
        return context_;
    }
    public String getBootClassPath() {
	return bootClassPath;
    }
    public Type.Class getHierarchyRoot() {
        return ROOT_TYPE;
    }
    public Type.Class getMetaClass() {
	if (CLASS_TYPE == null)
	    try {
		getSystemTypeContext().typeFor(JavaNames.java_lang_Class);
	    } catch (LinkageException e) {
		Executive.panicOnException(e);
	    }
	return CLASS_TYPE;
    }
    public Type.Interface[] getArrayInterfaces() {
        return arrayInterfaces;
    }
    Code[] getArrayVTable() {
        return ROOT_BLUEPRINT.getVTable();
    }
    public VM_Address[] getArrayJ2cVTable() {
	return ROOT_BLUEPRINT.j2cVTable;
    }

    // An array of GC roots referenced by statically compiled code.
    // This array contains pointers to objects within our domain (such
    // as strings) that may be gcable.  ldc of a reference (other than
    // blueprint) should be indirected through this aray.
    public Oop[] j2cRoots = new Oop[0];

    /** Must run at image boot to make maps accessible again (not just because
     *  freeze made them read-only, but to rehash them because our trivial
     *  implementation of identityHashCode may not produce the same values as
     *  the host VM).*/
    public void thaw() {
        // blueprints = new IdentityHashMap(blueprints);
        context_.thaw();
	Engine.observeThaw(this);
    }

    /**
     * This code is temporary.  See
     * <a href="http://sss.cs.purdue.edu/index.php/OVM_mixed-mode_execution">
     * the wiki</a> for ideas on how to remove it.
     **/
    public SimpleJITDynamicCompiler sj;

    /**
     * This code is temporary.  See
     * <a href="http://sss.cs.purdue.edu/index.php/OVM_mixed-mode_execution">
     * the wiki</a> for ideas on how to remove it.
     **/
    public VM_Area compileArea;

    /**
     * This code is temporary.  See
     * <a href="http://sss.cs.purdue.edu/index.php/OVM_mixed-mode_execution">
     * the wiki</a> for ideas on how to remove it.
     **/
    public void compile() {
        if (sj == null) 
            return; // non-SJ configs
        SimpleJITDynamicDomainCompiler.run(this);
    }
	
    /**
     * Prepares this domain for starting up. This performs a {@link #thaw}.
     */
    public void startup() {
        thaw();
    }
    
    public void shutdown() {
    }

} // end of S3Domain
