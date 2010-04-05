package s3.core.domain;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.ExecutiveDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.RealtimeJavaDomain;
import ovm.core.domain.ReflectiveField;
import ovm.core.domain.ReflectiveMethod;
import ovm.core.domain.Type;
import ovm.core.execution.InvocationMessage;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.JVMConstants;
import ovm.util.ArrayList;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;
import s3.util.PragmaTransformCallsiteIR;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import ovm.core.Executive;
import ovm.core.services.memory.MemoryPolicy;

/**
 * The domain built and frozen at image build time that contains
 * kernel code.
 *
 * The executive domain has a fully functional intern table, which is
 * used at both build-time and run-time.  As of 3/04, this table plays
 * a critical role in the image build process.  Strings that are only
 * reachable through Oop proxies in constant pools will not be written
 * correctly.
 **/
public class S3ExecutiveDomain extends S3Domain
    implements ExecutiveDomain, RealtimeJavaDomain, JVMConstants.Throwables {
  
    /**
     * In the user domain, we expect that pragma types may not be
     * found, and we cache getPragma calls to avoid repeatedly
     * searching the bootclasspath for nonexistent classes.  In the
     * executive domain, there is simply no way for a pragma to go
     * missing, so the Type.Context's name -&gt; type map serves as a
     * perfectly good cache.
     **/
    public Type getPragma(TypeName.Scalar exc) {
	try {
	    return getSystemTypeContext().typeFor(exc);
	} catch (LinkageException e) {
	    throw Executive.panicOnException(e);
	}
    }

    /**
     * Record all virtual methods for a domain's RTE and CSA objects
     * as live.  Right now, we are limited to one concrete type for
     * all CoreServicesAccess instances, and one type
     * (ovm.core.execution.RuntimeExports) for all RTE instances.
     * However, there have been discussions about loosening these
     * restrictions.<p>
     *
     * This method is called early in the image build process by the
     * driver.  It cannot be called too early in the build process,
     * because it uses VM_Adress.fromObject().<p>
     *
     * @see S3Domain#bootstrap
     **/
    void registerBoundaryMethods(Domain d) {
 	registerVTable(d.getCoreServicesAccess());
 	registerVTable(d.getRuntimeExports());
    }

    private void registerVTable(Object o) {
	Oop oops = VM_Address.fromObject(o).asOop();
	S3Blueprint bp = (S3Blueprint) oops.getBlueprint();
	Code[] vt = bp.getVTable();
	for (int i = 0; i < vt.length; i++)
	    if (vt[i].getMethod().getDeclaringType() != ROOT_TYPE)
		registerCall(vt[i].getMethod().getSelector());
    }

    private final ArrayList bootMethods = new ArrayList();
    public void runAllBootMethods() {
	Object r = MemoryPolicy.the().enterClinitArea(getSystemTypeContext());
	try {
	    for (int i = 0; i < bootMethods.size(); i++) 
		((ReflectiveMethod) bootMethods.get(i)).call(null);
	} finally {
	    MemoryPolicy.the().leave(r);
	}
    }

    public void addBootMethod(Method m) throws BCdead {
	bootMethods.add(new ReflectiveMethod(this, m.getSelector()));
    }

    public void startup() {
        super.startup(); // thaw
        getCoreServicesAccess().boot();
	runAllBootMethods();
    }

    public String toString() {
	return "ExecutiveDomain";
    }
    
    protected TypeName.Scalar throwableTypeName(int code) {
	switch (code) {
	// The following errors need not be defined in the ED, because
	// they can never happen.  None the less, LinkageError and
	// many of its subtypes are defined.
	case LINKAGE_ERROR:
	case CLASS_FORMAT_ERROR:
	case CLASS_CIRCULARITY_ERROR:
	case VERIFY_ERROR:
	case NO_CLASS_DEF_FOUND_ERROR:
	case INCOMPATIBLE_CLASS_CHANGE_ERROR:
	case INSTANTIATION_ERROR:
	case EXCEPTION_IN_INITIALIZER_ERROR:
	case NO_SUCH_METHOD_ERROR:

	    return JavaNames.throwables[INTERNAL_ERROR];

	default:
	    return super.throwableTypeName(code);
	}
    }

    public Method findFinalizer(Oop o) { return null; }

    public S3ExecutiveDomain(String rpath)   {
	super(rpath);
	throwableCode =
	    new ReflectiveField.Reference(this,
					  JavaNames.arr_ovm_core_domain_Code,
					  JavaNames.java_lang_Throwable,
					  "code");
	throwablePC =
	    new ReflectiveField.Reference(this,
					  JavaNames.arr_int,
					  JavaNames.java_lang_Throwable,
					  "pc");
    }
    
    /**
     * ExecutiveDomain has just one context
     **/
    public Type typeFor(TypeName name)
        throws LinkageException {
	return context_.typeFor(name);
    }
    /**
     * ExecutiveDomain has just one context
     **/
    public Blueprint blueprintFor(TypeName.Compound name)
        throws LinkageException {
	return blueprintFor(name, context_);
    }
    
    public boolean isExecutive() {
	return true;
    }

    // this is the leaf type context for this domain
    public Type.Context getApplicationTypeContext() {
	return getSystemTypeContext();
    }

    ReflectiveField.Reference string_data =
	new ReflectiveField.Reference(this, JavaNames.arr_byte,
				      JavaNames.java_lang_String, "data");
    ReflectiveField.Integer string_offset =
	new ReflectiveField.Integer(this,
				    JavaNames.java_lang_String, "offset");
    ReflectiveField.Integer string_count =
	new ReflectiveField.Integer(this,
				    JavaNames.java_lang_String, "count");

    private static class BCswappop extends PragmaTransformCallsiteIR
	implements JVMConstants.Opcodes
    {
        static {
            register("s3.core.domain.S3ExecutiveDomain$BCswappop",
		     new byte[] { SWAP, POP });
        }
    }


    // RTSJ memory semantics - ED code should never fail these checks so
    // we convert them to internal errors.

    /**
     * Throws a <tt>OVMError.Internal</tt> always
     */
    public void readBarrierFailed() {
        throw new OVMError.Internal("read barrier failed");
    }

    /**
     * Throws a <tt>OVMError.Internal</tt> always
     */
    public void storeBarrierFailed() {
        throw new OVMError.Internal("store barrier failed");    
    }


    /**
     * Used in the implemenation of hostJvmIntern()
     **/
    private ArrayList imageBuildHack = new ArrayList();

    /**
     * At runtime, it is equivalent to
     * <pre>VM_Address.fromObject(s).asOop()</pre>.<p>
     *
     * At image build time, this method is typically (exclusively?)
     * called from internString(int) when resolving string literals
     * from the constant pool.  We must be careful to ensure that
     * literals referenced at image build time (and interned in the
     * host JVM) are <code>==</code> to literals referenced at runtime
     * (and interned in the ED's internTable).<p>
     *
     * FIXME: The above is painful, but more or less rational.  Things do,
     * however, get stranger from here.  At build time, constant pools
     * don't contain strings, but Oop proxy objects.  Our build time
     * intern table similarly maps UnicodeBuffers to proxy objects.
     * Unfortunately, the magic for converting strings from jdk's
     * char[]-based representation to the ED's byte[] based
     * representation will not occur when an Oop proxy is encountered
     * in the jdk's heap.  We must store the String in a well-typed
     * location to get it mangled.  imageBuildHack is used for this
     * purpose.
     **/
    private Oop hostJvmIntern(String s) throws BCswappop {
	s = s.intern();
	Blueprint bp = blueprintFor(commonTypes().java_lang_String);
	imageBuildHack.add(s);
	return VM_Address.fromObject(s, bp).asOop();
    }

    public Oop makeString(UnicodeBuffer s) {
	return hostJvmIntern(s.toString());
    }

    // Avoid copying around bytes and reflectively calling
    // constructors to build a string identical to the one we started
    // with.
    public Oop stringFromLocalizedCString(String edString) {
	return VM_Address.fromObject(edString).asOop();
    }

    public byte[] getLocalizedCString(Oop s) {
	return getUTF8CString(s);
    }

    UnicodeBuffer getString(Oop s, int offset, int length) {
        Object o = string_data.get(s);
	return UnicodeBuffer.factory().wrap((byte[]) o,    
					    offset + string_offset.get(s),
					    length);
    }

    public UnicodeBuffer getString(Oop s) {
        Object o = s;
	return UnicodeBuffer.factory().wrap((String) o);
    }

    public static class UnicodeBufferFactory extends UnicodeBuffer.Factory {
	public UnicodeBuffer wrap(String s, int off, int len)  {
	    S3ExecutiveDomain ed =
		(S3ExecutiveDomain) DomainDirectory.getExecutiveDomain();
	    return ed.getString(VM_Address.fromObject(s).asOop(), off, len);
	}

	public String toString(UnicodeBuffer b) {
	    // FIXME: should special case byte[] buffers, and
	    // possibly ED string buffers
	    return new String(b.toByteArray());
	}
    }


}
