package s3.services.bootimage;

import java.io.IOException;
import java.util.Stack;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Method;
import ovm.core.services.memory.MemoryManager;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.util.BitSet;
import ovm.util.HashMap;
import ovm.util.Iterator;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import s3.util.Walkabout;

/**
 * This class should really be called StaticCompiler.  All
 * implementations of this class extend the VM-generation process with
 * a particular compiler implementation.<p>
 *
 * Every static compiler for OVM should consist of both java compiler
 * code, and a directory containing C/C++ support code that, among
 * other things, defines the executable program's <code>main</code>
 * function.  The default constructor copies files from this directory
 * into the current directory (where the executable is to be built).
 * After the java program finishes, the <code>gen-ovm</code> script
 * will run <code>make -f OVMMakefile</code> to construct the final
 * executable.<p>
 *
 * @author <a href="mailto://baker29@cs.purdue.edu"> Jason Baker </a>
 */
public abstract class ImageObserver extends BootBase {
    /**
     * An interface to type-name and object reachability analysis.  It
     * plants roots and prunes branches.
     * @author <a href="mailto://baker29@cs.purdue.edu"> Jason Baker </a>
     **/
    public interface Gardener {
	/** Add an object to the root set and image header */
	void declare(Object o, String s);

	/**
	 * Exclude a class from the executive domain's runtime
	 * incarnation.  Don't generate a blueprint and don't store
	 * references
	 *
	 * @param pkg The '/' delimited package name
	 * @param cls The simple class name within pkg
	 */
	void excludeClass(String pkg, String cls);

	/**
	 * Exclude all classes within a package from the runtime
	 * exective domain.  Don't generate blueprints and don't store
	 * references.
	 *
	 * @param pkg The '/' delimited package name
	 */
	void excludePackage(String pkg);
    }

    public ImageObserver() { }
    public ImageObserver(String OVMMakefile, String gdbinit) { }
    
    /** Used to inform the VM generation process what the engine is. */
    public boolean isJ2c() { return false; }

    /**
     * If this method returns true, the compiler expects numeric
     * field and method offsets rather than symbolic refereces.
     * By default, return false.
     **/
    public boolean shouldQuickify() { return false; }
    /**
     * The hook may be overriden to add fields to the image header, or
     * to customize class loading in the executive domain.
     **/
    public void registerLoaderAdvice(Gardener gardener) {  }

    /**
     * This hook may be overriden to customize the traversal of
     * objects that will form part of the runtime bootimage.
     **/
    public void registerGCAdvice(Walkabout w) { }

    /**
     * Called when the VM generator determines that a method is part
     * of the program.
     **/
    public void addMethod(Method m) { }

    /**
     * Compile all live code in a particular domain.  The
     * {@link Analysis} object holds the results of a static analysis,
     * and can be queried to determine which methods to compile, and
     * what optimizations are valid.
     **/
    public abstract void compileDomain(Domain d, Analysis anal);

    /**
     * This method is called after the bulk of the VM-generation
     * process is complete.  At this point, there will be no further
     * calls to {@link #compileDomain}, but objects that will survive
     * into the VM's runtime have not yet been saved to disk, so
     * mutations performed in this method wil be reflected at
     * runtime.<p>
     *
     * The default implementation uses
     * {@link InvisibleStitcher#expandOptions} to generate the
     * <file>OVMMakefile</file>.<p>
     *
     * {@link s3.services.j2c.J2cImageCompiler} uses this hook to find
     * pointers within the VM's text and data segments, and patch
     * references to these addresses with
     * {@link ovm.core.services.memory.VM_Address#bind}.<p>
     **/
    public void compilationComplete() {
	// The config files know the paths to the templates we need to expand
	String OVMMakefile = InvisibleStitcher.getString("OVMMakefile.in");
	String gdbinit = InvisibleStitcher.getString("gdbinit.in");

	// And the memory manager knows how it wants memory layed
	// out.  This will be saved in OVMMakefile
	int bootbase = MemoryManager.the().getImageBaseAddress();
	InvisibleStitcher.addOption("BOOTBASE",
				    "0x" + Integer.toHexString(bootbase));
	int heapBase = MemoryManager.the().getFixedHeapAddress();
	InvisibleStitcher.addOption("HEAPBASE",
				    "0x" + Integer.toHexString(heapBase));
	// Translate decimal with suffix to hex.
	int heapSize = InvisibleStitcher.getInt("heap-size");
	InvisibleStitcher.addOption("HEAPSIZE",
				    "0x" + Integer.toHexString(heapSize));

        if (!InvisibleStitcher.getBoolean("npassmain")) {
          InvisibleStitcher.addOption("arg0",
            InvisibleStitcher.getString("main"));
        }
        
        if (MemoryManager.the().usesArraylets()) {
          InvisibleStitcher.addOption("arraylets-with-size",
            Integer.toString(MemoryManager.the().arrayletSize()));
        }
        
	try {
	    // Peform stitcher parameter substituion on the templates
	    InvisibleStitcher.expandOptions(OVMMakefile, "OVMMakefile");
	    InvisibleStitcher.expandOptions(gdbinit, ".gdbinit");
	} catch (IOException e) {
	    throw new InvisibleStitcher.MisconfiguredException
		("generating Makefile", e);
	}
    }

    /**
     * Iterate over all domains in the VM.  This class attempts to
     * abstract away some of the VM generator process's statefulness.
     * The build process has a notion of a `current' domain, which
     * determines the behavior of VM_Address.fromObject().
     * forAllDomains updates the current domain on entry to walkDomain
     * and restores the current domain after all domains have been
     * walked.
     * 
     * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
     **/
    static public abstract class DomainWalker {
	public abstract void walkDomain(Domain dom);

	public void forAllDomains() {
	    for (Iterator it = DomainDirectory.domains(); it.hasNext(); )
		walkDomain((Domain) it.next());
	}
    }

    static public abstract class BlueprintWalker extends DomainWalker {
	public abstract void walkBlueprint(Blueprint bp);

	public void beforeDomain(Domain d) {
	}

	// return false to skip this domain
	public boolean acceptDomain(Domain d) {
	    beforeDomain(d);
	    return true;
	}

	public void walkDomain(Domain d) {
	    if (acceptDomain(d))
		for (Iterator it = d.getBlueprintIterator(); it.hasNext(); ) {
		    Blueprint bp = (Blueprint) it.next();
		    walkBlueprint(bp);
		}
	}
    }
	    
	
    /**
     * Called to extract field values from the image header.
     * Useful fields include:
     * <dl>
     * <dt>mainMethod<dd>        the VM's entry point as <tt>S3ByteCode</tt>
     * <dt>mainObject<dd>        mainMethod's receiver
     * <dt>coreServicesAccess<dd>the (initial?) CSA instance
     * <dt>repository<dd>        the Repository instance
     * </dl>
     *
     * @param key the name of the field
     */
    public final Object getHeader(String key) {
	return BootImage.the().getHeader(key);
    }

    public final Domain getExecutiveDomain() {
	return executiveDomain;
    }

    private HashMap headers = new HashMap();
    private S3Domain executiveDomain;
    private DomainSprout executiveDS;
    
    /**
     * Called after Domains have been bootstrapped.  This
     * method is responsible for calling registerLoaderAdvice, as
     * this is the first reasonable place to make such a call.<p>
     * <ul>
     * <li> At this point, the standard bootimage headers have been
     *      defined, but engine-specific headers may still be added
     * <li> Classloading is possible in both domains, but hopefully
     *      nothing beyond Object and Class has been loaded.
     * </ul>
     **/
    final void init(DomainSprout eds) {
	executiveDomain = (S3Domain) DomainDirectory.getExecutiveDomain();
	executiveDS = eds;
	registerLoaderAdvice(eds);
    }

    static public ImageObserver the() {
	return (ImageObserver) 
	    InvisibleStitcher.singletonFor(ImageObserver.class);
    }
}
