package s3.services.bootimage;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.util.Arrays;
import java.util.jar.JarFile;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.execution.NativeConstants;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.memory.VM_Address;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;
import ovm.util.BitSet;
import ovm.util.ByteBuffer;
import ovm.util.CommandLine;
import ovm.util.OVMError;
import ovm.util.SparseArrayList;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import s3.core.domain.S3ExecutiveDomain;
import s3.core.domain.S3JavaUserDomain;
import s3.core.domain.S3Type;
import s3.services.bytecode.ovmify.IRewriter;
import s3.services.bytecode.ovmify.NativeCallGenerator;
import s3.services.transactions.S3Transaction;
import s3.services.transactions.Transaction;
import s3.util.EphemeralBureau;
import ovm.core.execution.RuntimeExports;
import java.util.Map;
import java.util.IdentityHashMap;
import ovm.core.repository.RepositoryUtils;
import java.io.OutputStream;
import ovm.core.domain.ExecutiveDomain;
import ovm.core.domain.JavaUserDomain;
import ovm.core.domain.UserDomain;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.ConstantResolvedMethodref;
import ovm.core.domain.ConstantResolvedFieldref;
import ovm.util.HashSet;
import ovm.util.Iterator;

/**
 * Ovm's main class for virtual machine generation.  The VM-generation
 * process is described in detail in {@link s3.services.bootimage},
 * and each phase of execution corresponds to a method in this class.
 * <ol>
 *    <li> {@link #bootstrap}
 *    <li> {@link #compile}
 *    <li> {@link #genAuxFiles}
 *    <li> {@link #dump}
 *    <li> {@link #printStats}
 * </ol>
 *
 * @see s3.services.bootimage
 * @author Grothoff, Vitek, Palacz
 **/
public class Driver extends BootBase {
    /**
     * A C source file that will be part of every ovm executable.  Add
     * random declarations here.
     **/

    /* this file might not be needed anymore (it was only used by image barrier when it was implemented
       with hardware support */    
    public static final PrintWriter gen_ovm_c;
    static {
	try {
	    gen_ovm_c = new PrintWriter(new FileOutputStream("gen-ovm.c"));
	} catch (IOException e) { throw new RuntimeException(e); }
    }


    /**
     * When non-null, this is the stream where bytecode should be
     * disassembled.
     **/
    public static PrintWriter img_ovmir_ascii;

    /** The executive domain's generation-time state. **/
    static DomainSprout executiveDomainSprout;
    /** The initial user domain's generation-time state. **/
    static DomainSprout userDomainSprout;

    /**
     * The entry point for Ovm's virtual machine generator.
     * @see s3.services.bootimage
     **/
    public static void main(String[] args)  {
        CommandLine arguments = new CommandLine(args);
        pln("Driver invoked with " + arguments);
        try {
	    // We want to both initialize the invisible stitcher and
	    // set the assertion status as soon as possible.  However,
	    // the ndebug parameter probably comes from a stitcher
	    // file, so we need to initialize the stitcher first.
            InvisibleStitcher.init(arguments);
	    if (!InvisibleStitcher.getBoolean("ndebug"))
		Driver.class.getClassLoader().setDefaultAssertionStatus(true);

	    // verify and parse arguments
            if (InvisibleStitcher.getString("classpath") != null
		&& InvisibleStitcher.getString("classpath").length() == 0)
                throw new MisconfiguredException("-classpath empty");
            else if (InvisibleStitcher.getString("bootclasspath").length() == 0)
                throw new MisconfiguredException("-bootclasspath empty");
            else if (InvisibleStitcher.getString("xdpath").length() == 0)
                throw new MisconfiguredException("-xdpath empty");
            else if (arguments.argumentCount() != 0)
                throw new MisconfiguredException("extra arguments starting at "
						 + arguments.getArgument(0));
	    Inliner.getParameters();
	    BootBase.parseLogOptions(InvisibleStitcher.getString("log"));
	    if (InvisibleStitcher.getBoolean("dumpovmirascii")) {
		OutputStream s = new FileOutputStream("img.ovmir.ascii");
		s = new BufferedOutputStream(s);
		img_ovmir_ascii=new PrintWriter(s);
	    }

	    // Now the fun begins.
	    bootstrap();
	    compile();
	    genAuxFiles();
	    dump();
	    if (InvisibleStitcher.getBoolean("stats"))
		printStats();
        } catch (MisconfiguredException e) {
	    die("Configuration Error", e);
        } catch (IOException e) {
	    die("Error generating files", e);
	} catch (RuntimeException e) {
	    die("Internal error", e);
	} catch (Error e) {
	    die("Internal error", e);
	}
    }

    /**
     * Report a failure, and invoke {@code System.exit(1)}.
     **/
    private static void die(String how, Throwable t) {
	System.err.println(how + ": " + t.getMessage());
	t.printStackTrace(System.err);
	System.err.println("run gen-ovm -help for usage");
	System.exit(1);
    }

    /**
     * Bootstrap Ovm.
     * @see s3.services.bootimage#bootstrap
     **/
    private static void bootstrap() {
	pln("[0] bootstrap...");
	// Avoid constructing the ObjectModel inside the constructor
	// for the DomainDirectory.  This makes the bootstrapping
	// process easier to verify.
	ObjectModel.getObjectModel();

	// We might as well enable Driver.isSubtypeOf early.  This
	// code is harmless, and not having it when needed can easily
	// lead to NPEs
        superTypes = new SparseArrayList[DomainDirectory.maxContextID() + 1];
	for (int i = 0; i < superTypes.length; i++)
	    superTypes[i] = new SparseArrayList();

	// First, we must initialize VM_Address, GC, and BootImage
	ExecutiveDomain ed = DomainDirectory.getExecutiveDomain();
	IdentityHashMap addressMap = 
	    new IdentityHashMap(InvisibleStitcher.getInt("build-size"));
	ByteBuffer bootImage = ByteBuffer.allocate(Integer.MAX_VALUE);
	bootImage.order(NativeConstants.BYTE_ORDER);
	VM_Address.initialize(bootImage, addressMap);
	new GC(ed, addressMap);
	new BootImage(bootImage);

	// Here is the key step, constructing the executive
	// DomainSprout, which in turn bootstraps the executive domain
	executiveDomainSprout = new DomainSprout(ed);
	// The core bootstrapping phase is now complete.

	// Bootstrap the user domain
        String umainClassName = InvisibleStitcher.getString("udmain");
        TypeName.Scalar umain = 
	    JavaFormat._.parseTypeName(umainClassName).asScalar();
        UserDomain udom = DomainDirectory.createUserDomain(umain);
        userDomainSprout = new DomainSprout((JavaUserDomain) udom);

	// This method will call registerLoaderAdvice, which in turn
	// may effect classloading, or define new image header fields.
        ImageObserver.the().init(executiveDomainSprout);

	// Perform late initialization in the garbage collector.  It
	// calls a method on ImageObserver, and should reallly not be
	// called until the executive domain has booted.
	GC.the().init();
    }

    /**
     * Compile the code in a generated virtual machine
     * @see s3.services.bootimage#compile
     * @see DomainSprout#importCode
     **/
    private static void compile() {
        pln("[1] Loading user domain code...");
	// In the user domain, we don't have to worry about objects in
	// the bootimage, but we may allow for dynamic loading
	userDomainSprout.importCode(null, !ImageObserver.the().isJ2c());

	pln("[2] Loading executive domain code ...");
	// We must consider bootimage objects when analyzing the
	// executive domain, but there will not be any dynamic loading
	executiveDomainSprout.importCode(GC.the(), false);
    }

    /**
     * Generate auxillary files referneced by {@code make}.
     * @see s3.services.bootimage#genAuxFiles
     * @see ImageObserver#compilationComplete
     **/
    private static void genAuxFiles() throws IOException {
	pln("[3] generating auxiliary files and cleaning up...");
	gen_ovm_c.close();
	if (img_ovmir_ascii != null)
	    img_ovmir_ascii.close();
	StructGenerator.output("structs.h", executiveDomainSprout);
	CStructGenerator.output("cstructs.h", "cdstable.inc", new DomainSprout [] {
		executiveDomainSprout, userDomainSprout
	    });
	NativeCallGenerator.output("native_calls.gen");
	ImageObserver.the().compilationComplete();
        RepositoryUtils.Cache.clear();
	executiveDomainSprout.dom.dropCompileTimeData(ImageObserver.the().isJ2c());
	if (ImageObserver.the().isJ2c()) {
	    userDomainSprout.dom.dropCompileTimeData(true);
	    ConstantResolvedMethodref.dropCaches();
	    ConstantResolvedFieldref.dropCaches();
	}
    }

    /**
     * Dump pre-existing objects to the <file>img</file> file.
     * @see s3.services.bootimage#dump
     * @see GC#dumpImage
     **/
    private static void dump() throws IOException {
        pln("[4] Dumping live objects to img...");
	GC.the().dumpImage(new ISerializer(executiveDomainSprout), "img");
	pln("img file size = "+imgFileSize());
    }
    
    public static long imgFileSize() {
	return new File("img").length();
    }

    /**
     * Print statistics.
     * @see s3.services.bootimage#printStats
     * @see Statistics#printStats
     **/
    private static void printStats() {
	pln("[5] Statistics...");
	if (Transaction.the().transactionalMode()) {
	    System.out.println("PAR_GRAPH: begin ...");
	    s3.services.transactions.PARifyingRewriteVisitor.dump_par_graph();
	    System.out.println("PAR_GRAPH: end.");
	    System.out.println("# Method" + IRewriter.CNT);
	    System.out.println("# NOB " + IRewriter.NOB);
	    System.out.println("# NOA  " + IRewriter.NOA);
	    System.out.println("# NOC " + IRewriter.NOC);
	    System.out.println("# NONE" + IRewriter.NONE);
	    System.out.println("ATOMIC_METHOD_COUNT " +
			       S3Transaction.ATOMIC_METHOD_COUNT);
	    System.out.println("NESTED_ATOMIC_METHOD_COUNT " +
			       S3Transaction.NESTED_ATOMIC_METHOD_COUNT);
	    System.out.println("ATOMIC_BLOCK_COUNT " +
			       S3Transaction.ATOMIC_BLOCK_COUNT);
	    System.out.println("NESTED_ATOMIC_BLOCK_COUNT " +
			       S3Transaction.NESTED_ATOMIC_BLOCK_COUNT);
	    System.out.println("ATOMIC_NONLOGGED_METHOD_COUNT " +
			       S3Transaction.ATOMIC_NONLOGGED_METHOD_COUNT);
	    if (S3Transaction.IDX>0) {
		int[] r = new int[S3Transaction.IDX];
		int[] w = new int[S3Transaction.IDX];
		System.arraycopy(S3Transaction.READ_COUNTS,0,r,0,
				 S3Transaction.IDX);
		System.arraycopy(S3Transaction.WRITE_COUNTS,0,w,0,
				 S3Transaction.IDX);
		Arrays.sort(r);
		Arrays.sort(w);
		System.out.println("MAX WRITE " + w[w.length-1]);
		int m = (int)((w.length-1)/2);
		System.out.println("MEDIAN WRITE " + w[m]);
		System.out.println("MAX READ " + r[w.length-1]);
		System.out.println("MEDIAN WRITE " + r[m]);
	    }
	}

	Statistics.printStats();
	executiveDomainSprout.anal.printStats();
	userDomainSprout.anal.printStats();
	ovm.core.repository.UTF8Store._.dumpStats();
	int refSlots = 0;
	HashSet refMaps = new HashSet();
	for (Iterator it = DomainDirectory.domains(); it.hasNext(); ) {
	    Domain d = (Domain) it.next();
	    for (Iterator jt = d.getBlueprintIterator(); jt.hasNext(); ) {
		Blueprint bp = (Blueprint) jt.next();
		int[] refs = bp.getRefMap();
		if (refs != null && !refMaps.contains(refs)) {
		    refSlots += refs.length;
		    refMaps.add(refs);
		}
	    }
	}
	System.out.println("gc ref maps contain " + refSlots + " entries in " +
			   refMaps.size() + " int[]s");
    }

    /** Support for {@link #isSubtypeOf} **/
    private static void merge(BitSet[] to, BitSet[] from) {
	for (int i = 0; i < to.length; i++)
	    to[i].or(from[i]);
    }

    /** Support for {@link #isSubtypeOf} **/
    private static SparseArrayList[] superTypes = null;
    
    /** Support for {@link #isSubtypeOf} **/
    private static BitSet[] getSuperTypes(Blueprint b) {
	int cid = b.getCID();
	int uid = b.getUID();
	BitSet[] ret = (BitSet[]) superTypes[cid].get(uid);
	if (ret == null) {
	    // Note the recursion on Object and its interfaces
	    ret = new BitSet[superTypes.length];
	    for (int i = 0; i < superTypes.length; i++)
		ret[i] = new BitSet();
	    ret[cid].set(uid);
	    Blueprint parent = ((S3Blueprint) b).getParentBlueprint();
	    if (parent != null) 
		merge(ret, getSuperTypes(parent));
	    Type t = b.getType();
	    Type.Interface[] ifc = t.getInterfaces();
	    for (int i = 0; i < ifc.length; i++)
		merge(ret, getSuperTypes(blueprintFor(ifc[i])));
	    superTypes[cid].set(uid, ret);
	}
	return ret;
    }

    /**
     * Uses of this method should probably be replaced with
     * {@link ovm.core.domain.Blueprint#isSubtypeOf}.
     *
     * This method exists for two reasons.
     * <ol>
     *   <li> At one point, Blueprint.isSubtypeOf could not be called
     *        until all types where defined.
     *   <li> At one point, calling isSubtypeOf early could have
     *        increased the bootimage size.
     * </ol>
     * Neither is true any more.
     **/
    public static boolean isSubtypeOf(Blueprint derived, Blueprint base) {
	BitSet[] superTypes = getSuperTypes(derived);
	return superTypes[base.getCID()].get(base.getUID());
    }

    /**
     * Uses of this method should probably be replaced with
     * {@link s3.core.domain.S3Blueprint#leastCommonSupertypes}.
     *
     * This method exists for two reasons.
     * <ol>
     *   <li> At one point, Blueprint.isSubtypeOf could not be called
     *        until all types where defined.
     *   <li> At one point, calling isSubtypeOf early could have
     *        increased the bootimage size.
     * </ol>
     * Neither is true any more.
     **/
    public static Blueprint[] leastCommonSupertypes(Blueprint a,
						    Blueprint b)
    {
	if (a == b || isSubtypeOf(b, a)) return new Blueprint[] { a };
	else if (isSubtypeOf(a, b))      return new Blueprint[] { b };

	Type at = a.getType();
	Type bt = b.getType();
	Type[] lct = ((S3Type) at).getLeastCommonSupertypes(bt);
	Blueprint[] ret = new Blueprint[lct.length];
	Domain dom = a.getDomain();
	for (int i = 0; i < ret.length; i++) {
	    ret[i] = dom.blueprintFor(lct[i]);
	}
	return ret;
    }

    public static S3Blueprint blueprintFor(Class definingClass, Domain dom) throws LinkageException {
        TypeName.Compound name = ReflectionSupport.typeNameForClass(definingClass).asCompound();
        return blueprintFor(name, dom);
    }

    public static S3Blueprint blueprintFor(TypeName.Compound tname, Domain dom) throws LinkageException {
        return (S3Blueprint) dom.blueprintFor(tname, dom.getApplicationTypeContext());
    }

    /**
     * @deprecated
     * This method was created, in part, to hide the checked exception
     * thrown by Domain.blueprintFor(Type).  That method no longer
     * throws a checked exception, so this wrapper serves little
     * purpose now.
     */
    public static S3Blueprint blueprintFor(Type t) {
	return (S3Blueprint) t.getDomain().blueprintFor(t);
    }
}
