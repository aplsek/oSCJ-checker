package s3.services.bootimage;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.ovmj.util.Runabout;

import ovm.core.domain.Blueprint;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.execution.Context;
import ovm.core.execution.Processor;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;
import ovm.util.CommandLine;
import ovm.util.HashSet;
import ovm.util.Iterator;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Domain;
import s3.core.domain.S3ExecutiveDomain;
import s3.core.domain.S3JavaUserDomain;
import s3.core.domain.S3TypeContext;
import s3.core.execution.S3BottomFrame;
import s3.util.Walkabout;
import s3.util.Walkabout.ObjectAdvice;
import ovm.core.execution.RuntimeExports;
import ovm.core.services.memory.MemoryManager;
import ovm.core.domain.JavaUserDomain;
import ovm.core.domain.ExecutiveDomain;

/**
 * Per-domain state that is needed only a VM-generation time.
 * This class enables classloading at VM-generation time, and ensures
 * that all the code we will need at runtime is loaded and compiled.
 *
 * @see s3.services.bootimage
 **/
public class DomainSprout extends BootBase
    implements ImageObserver.Gardener
{
    final S3Domain dom;
    final Type.Context defaultTypeCtx;
    final Analysis anal;

    final private Method mainMethod;
    final static String ENTRY_POINT = "startup:()V";
    final static Class EXECUTIVE = ovm.core.Executive.Interface.class;
   
    private final BuildTimeLoader sysLoader;

    /**
     * Bootstrap the user domain, and process reflection hints.
     **/
    public DomainSprout(final JavaUserDomain domain) {
	this.dom = (S3Domain) domain;
	try {
	    String bpath = InvisibleStitcher.getString("bootclasspath");
	    String cpath = InvisibleStitcher.getString("classpath");
	    BuildTimeLoader bootLoader = 
		new BuildTimeLoader(domain.getSystemTypeContext(),
				    null, bpath);
	    sysLoader =
		new BuildTimeLoader(domain.getApplicationTypeContext(), 
				    bootLoader, cpath);
	    pln("    b. bootstrapping user domain");
	    domain.bootstrap();
	}
	catch (LinkageException e) { throw e.unchecked(); }

	anal = Analysis.factory().make(domain);
	this.defaultTypeCtx = domain.getApplicationTypeContext();
        this.mainMethod = null;

	// list of classes to be added explicity to run SpecJVM
	// which uses lots of reflection
	String rt=InvisibleStitcher.getString("ud-reflective-classes");
	if (rt == null) rt = "";
	for (StringTokenizer t = new StringTokenizer(rt, ", \n\t");
	     t.hasMoreTokens();) {
	    String rawName = t.nextToken();
	    TypeName tn = JavaFormat._.parseTypeName(rawName);
	    if (tn.isArray()) {
		dom.registerNew(tn.asArray());
	    } else if (tn.isScalar()) {
		TypeName.Scalar gtn = tn.asScalar().getGemeinsamTypeName();
		dom.registerCall(Selector.Method.make(JavaNames.CLINIT, gtn));
	    } else {
		throw new MisconfiguredException("bad reflective class " + rawName);
	    }
	}

	String udtargets = InvisibleStitcher.getString("udtargets");
	udtargets = udtargets.replaceAll(" T:", "!T:");  // TRANSACTION/PAR 
	// We have methods with white spaces.... -jv 
	    
	// these strings are easy to mess up.  Provide good error reporting
	StringTokenizer tokenizer = new StringTokenizer(udtargets == null ? "" : udtargets);
	if ((tokenizer.countTokens() & 1) == 1) 
	    throw new MisconfiguredException
		(	"odd number of tokens in -udtargets option"+
			"-udtargets consists of TypeName, UnboundSelector pairs"+
			"(value provided: " + udtargets + ")");	    
	while (tokenizer.hasMoreTokens()) {
	    String tn = tokenizer.nextToken();
	    String msel = tokenizer.nextToken();
	    msel = msel.replaceAll("!T:"," T:"); // TRANSACTION/PAR undo previous replace
	    try {
		dom.registerCall(RepositoryUtils.methodSelectorFor(tn, msel));
	    } catch (Error e) {
		System.err.println("bad udtarget " + tn + " " + msel);
		throw e;
	    } catch (RuntimeException e) {
		System.err.println("bad udtarget " + tn + " " + msel);
		throw e;
	    }
	}

	String appClassList = InvisibleStitcher.getString("main");
	if (appClassList != null && appClassList.length() != 0) {
	    String[] appClasses = appClassList.split(",");
	    for (int i = 0; i < appClasses.length; i++) {
		String str = appClasses[i].trim();
		if (i == 0)
		    RuntimeExports.setDefaultMainClass(str);
		TypeName name = JavaFormat._.parseTypeName(str);
		name = name.asScalar().getGemeinsamTypeName();
		Selector.Method msel =
		    	Selector.Method.make(JavaNames.MAIN, name.asCompound());
		domain.registerCall(msel);
	    }
	} else if (ImageObserver.the().isJ2c()) {
	    throw new MisconfiguredException("Can't run ahead-of-time " +
					     "compiler with no -main " +
					     "specified");
	}
    }

    /**
     * Bootstrap the executive domain, record the VM's entry point and
     * declare {@link BootImage} header fields.  The {@link
     * BuildTimeLoader} for the executive domain is special in a
     * couple ways.  First, it detects {@code static void boot_()}
     * methods, and arranges for them to be called on VM startup via
     * {@link ExecutiveDomain#addBootMethod}.  Second, it filters
     * specific classes and packages out of the classpath for a
     * variety of reasons (see {@link #defineExcludes}).<p>
     *
     * Half way through this constructor, we complete core
     * bootstrapping activities (by calling {@link
     * ovm.core.domain.ExecutiveDomain#bootstrap}).  This happens just
     * in time, because the second half of this constructor uses two
     * invisibly stitched components that should not be touched during
     * core bootstrapping.
     **/
    public DomainSprout(final ExecutiveDomain domain) {
	this.dom = (S3Domain) domain;
	this.defaultTypeCtx = domain.getSystemTypeContext();

	try {
	    String xdpath = InvisibleStitcher.getString("xdpath");
	    sysLoader = new BuildTimeLoader(defaultTypeCtx, null, xdpath) {
		public Type loadType(TypeName.Scalar tn)
		    throws LinkageException
		{
		    Type ret = super.loadType(tn);
		    ReflectionSupport.classForTypeName(tn);
		    if (!((S3TypeContext) defaultTypeCtx).bootstrapping()) {
			Type gret = ret.getSharedStateType();
			Method boot = gret.getMethod(JavaNames.BOOT_);
			if (boot != null) domain.addBootMethod(boot);
		    }
		    return ret;
		}
	    };
	    defineExcludes();
	    pln("    a. bootstrapping executive domain");
	    domain.bootstrap();
	}
	catch (LinkageException e) { throw e.unchecked(); }
	// Executive domain bootstrapped, proceed with wild abandon.

	// We should not have loaded any of the classes we want to
	// exclude yet, and we need to delay the first active use of
	// ImageObserver untile after the domain has bootstrapped
	if (ImageObserver.the().isJ2c())
	    defineStaticCompilerExcludes();
	
	anal = Analysis.factory().make(domain);

        /*
         * Create the objects that are in the image header.
         *
         * The field names of header object will be used as identifiers of a
         * generated C-struct.<p> Basic types are included in the header to
         * ensure that corresponding Java classes are included in the image.
         * It is necessary to force inclusion of basic array types because the
         * interpreter needs C-structs for compiling its array ops.
         */
        try {
            Object mainObject = InvisibleStitcher.singletonFor(EXECUTIVE.getName());
	    Selector.Method sel = 
		ReflectionSupport.methodSelectorFor(mainObject.getClass(), ENTRY_POINT, false);
	    domain.registerCall(sel);
	    mainMethod = defaultTypeCtx.typeFor(sel.getDefiningClass()).asScalar().getMethod(sel.getUnboundSelector());
	    assert(mainMethod != null);
            declare(mainObject, "mainObject");
	    // Umm, why?  This is interpreter-specific: we don't have
	    // the executable code for the main method yet in other
	    // configs.
            declare(mainMethod.getByteCode(), "mainMethod");
        } catch (Exception e) {
            throw failure(e);
        }

        declare(domain.getCoreServicesAccess(), "coreServicesAccess");
        declare(new Processor(), "bootProcessor");
	// FIXME: more interpreter-specific crap!
        declare(getBottomCode(), "bottomFrameCode");
        declare(new S3BottomFrame(), "bottomFrameObject");
        declare(Context.factory().make(), "bootContext");
	declare(new Integer(MemoryManager.the().getImageBaseAddress()),
		"baseAddress");
    }

    /**
     * Declare a {@link BootImage} header field or, if name is the
     * empty string, declare a root in the bootimage object graph.<p>
     * FIXME:
     * {@link ovm.core.services.memory.MemoryManager#pin MemoryManager.pin()}
     * and {@link ovm.core.services.memory.VM_Address#asInt VM_Address.asInt()}
     * are both valid ways to declare new roots.  Why offer a third
     * alternative?
     **/
    public void declare(Object target, String name) {
        assert(!(target instanceof Class));
	BootImage.the().addHeader(name, target);
        if (target != null) {
            GC.the().addRoot(target);
        }
    }

    /**
     * Prevent the named class from being pre-loaded in the generated VM.
     **/
    public void excludeClass(String pkg, String cls) {
	sysLoader.excludeClass(RepositoryUtils.makeTypeName(pkg, cls));
    }
    /**
     * Allow the named class from being pre-loaded in the generated
     * VM, regardless of any calls to excludePackage.
     **/
    public void includeClass(TypeName tn) {
	sysLoader.includeClass(tn);
    }
    /**
     * Prevent classes in the named package from being pre-loaded in
     * the generated VM.
     **/
    public void excludePackage(String pkg) {
	sysLoader.excludePackage(pkg);
    }
    /**
     * Prevent the named classes from being pre-loaded in the generated VM.
     **/
    public void excludeClasses(TypeName[] tn) {
	sysLoader.excludeClasses(tn);
    }

    public String toString() {
        return "DomainSprout[" + dom + "]";
    }

    public S3Domain getDomain() {
        return dom;
    }

    private S3ByteCode getByteCode(Class definer, String method) {
	Selector.Method sel = ReflectionSupport.methodSelectorFor(definer, method, false);
	TypeName dtn = sel.getDefiningClass();
	Method mm;
	try {
	    mm = defaultTypeCtx.typeFor(dtn).asScalar().getMethod(sel.getUnboundSelector());
	    if (null == mm) { // try static methods now
		sel = ReflectionSupport.methodSelectorFor(definer, method, true);
		dtn = sel.getDefiningClass();
		mm = defaultTypeCtx.typeFor(dtn).asScalar().getMethod(sel.getUnboundSelector());
	    }
	} catch (LinkageException e) { throw e.unchecked(); }
	assert(null != mm);
	return mm.getByteCode();
    }

    // FIXME: Is this interpreter-only?  I think that simplejit has a
    // similar notion, but it doesn't use S3ByteCode
    S3ByteCode getBottomCode() {
	return getByteCode(s3.core.execution.S3BottomFrame.class, "bottomFrame:()V");
    }

    /**
     * This method is at the core of the OVM VM-generation process.
     * It performs static analysis to find the live code in a domain,
     * transforms the code to OVM's final intermediate representation,
     * and invokes the compiler.
     *
     * @see s3.services.bootimage the package description
     **/
    public void importCode(GC heap, boolean runTimeLoading) {
	if (heap == null) {
	    pln("    a. finding live code");
	    anal.analyzeCode();
	} else {
	    pln("    a. finding live code (with heap)");
	    heap.fixObjectGraph(anal);
	}
	anal.analysisComplete();

	pln("    b. freezing classloading");
	dom.freezeClassLoading(!runTimeLoading);

	pln("    c. whole program transformations");
	if (!dom.isExecutive() && !runTimeLoading)
	    new CallingClassTransform(anal).run();
	if (Inliner.enabled())
	    new Inliner(dom, anal, runTimeLoading).run();

	pln("    d. doing final tranformations");
	// Do transformations that would interfere with static
	// analysis and inlining, such as pollcheck insertion and
	// quickification.
	dom.getRewriter().finishRewriting(anal);

	pln("    e. invoking compiler backend");
	ImageObserver.the().compileDomain(dom, anal);
    }

    /**
     * Customize the reflective walkabout used to manage objects that
     * exist at runtime.  Code defined here effects the {@link GC}
     * mark phase, and the way that fields are finally written to disk
     * by {@link ISerializer}.
     **/
    public static void registerAdvice(Walkabout walker_) {
	walker_.registerAfter(new Walkabout.IgnoreObjectAdvice(Ephemeral.class));
	walker_.register(new Walkabout.IgnoreStaticFieldAdvice(Ephemeral.class));
        ignoreClass(walker_, Ephemeral.Void.class);
        ignoreClass(walker_, java.net.URL.class);
        ignoreClass(walker_, Runabout.class);
	ignoreClass(walker_, ThreadLocal.class);
	ignoreClass(walker_, java.io.PrintWriter.class);
	ignoreClass(walker_, ovm.core.repository.Bytecode.class);
    }

    static public final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    static public final char[] EMPTY_CHAR_ARRAY = new char[0];

    void defineStaticCompilerExcludes() {
	// In j2c-only configurations, dump all code and data related
	// to dynamic loading.
	RuntimeExports.defineVMProperty("org.ovmj.staticBuild", true);
	excludeClass("s3/core/domain", "S3ByteCode");
	excludeClass("s3/core/domain", "Subtyping");
	excludeClass("s3/core/domain", "DispatchBuilder");
	excludeClass("s3/services/bytecode/ovmify", "IRewriter");
	excludeClass("s3/services/bytecode/reader", "S3Parser");
	//excludePackage("s3/services/bytecode/reader");
    }

    void defineExcludes() {
	excludeClasses(new TypeName[] { JavaNames.java_lang_ClassLoader,
		RepositoryUtils.makeTypeName("s3/services/bytecode", "S3ClassProcessor"),
		RepositoryUtils.makeTypeName("ovm/services/io", "Resource$HostedFile"),
		RepositoryUtils.makeTypeName("ovm/services/io", "Resource$HostedZippedFile"),
		RepositoryUtils.makeTypeName("ovm/services/io", "ResourceContainer$HostedDir"),
		RepositoryUtils.makeTypeName("ovm/services/io", "ResourceContainer$HostedZip"), });

	excludePackage("java/lang/reflect");
	excludePackage("java/util/jar");
	excludePackage("java/util/zip");
	excludePackage("java/net");
	excludePackage("s3/services/bytecode/verifier");
	excludePackage("s3/services/bytecode/writer");
	excludePackage("s3/services/bootimage");
	excludePackage("s3/services/process");
	excludePackage("s3/services/bytecode/interpreter");
	//excludePackage("s3/services/simplejit/bytecode");
	//excludePackage("s3/services/simplejit/ir");
	//excludePackage("ovm/services/bytecode/analysis");
	//excludePackage("ovm/services/bytecode/editor");
	//excludePackage("ovm/services/bytecode");
	excludePackage("s3/services/j2c");
	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "J2cCodeFragment"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "J2cCodeFragment$1"));
 	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "J2cActivation"));
 	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "J2cActivation$Factory"));
 	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "J2cActivation$Native"));
 	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "HendersonLocalReferenceIterator"));
 	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "HendersonLocalReferenceIterator$Native"));
 	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "PtrStackLocalReferenceIterator"));
 	includeClass(RepositoryUtils.makeTypeName("s3/services/j2c", "PtrStackLocalReferenceIterator$Nat"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "JVMConstants"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "JVMConstants$InvokeSystemArguments"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "JVMConstants$Opcodes"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "JVMConstants$PrimitiveArrayTypes"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "JVMConstants$Throwables"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/bytecode/writer",
						"S3Dumper$ConstantWriter_synthetic_Acceptor"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/bytecode/writer",
						"S3OVMIRDumper$OVMIRConstantWriter_synthetic_Acceptor"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/bootimage", "Ephemeral"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/bootimage", "Ephemeral$Void"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/simplejit/bytecode", "Translator$RegisterTable"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/simplejit/bytecode", "Translator$Liveness"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/simplejit/bytecode", "Translator$RegisterTable$Entry"));
	includeClass(RepositoryUtils.makeTypeName("s3/services/simplejit/bytecode", "Translator$Liveness$Entry"));

	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValueError"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$Int"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$Float"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$Long"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$Double"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$Reference"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$Primitive"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$WidePrimitive"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$Null"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$InValid"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$Array"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode/analysis", "AbstractValue$JumpTarget"));
	/*
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "Instruction"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "InstructionSet"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "InstructionBuffer"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "Instruction$Visitor"));
	includeClass(RepositoryUtils.makeTypeName("ovm/services/bytecode", "Instruction$IVisitor"));
	*/
//	includeClass(RepositoryUtils.makeTypeName("s3/services/simplejit/powerpc", "CodeGeneratorImpl"));

        }

    public final static void ignoreClass(Walkabout walker_, Class cls) {
        walker_.register(new Walkabout.IgnoreObjectAdvice(cls));
        walker_.register(new Walkabout.IgnoreClassAdvice(cls));
        walker_.register(new Walkabout.IgnoreFieldAdvice(cls));
        walker_.register(new Walkabout.IgnoreStaticFieldAdvice(cls));
    }
}
