package s3.services.bootimage;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.services.memory.VM_Address;
import ovm.util.ArrayList;
import ovm.util.HashMap;
import ovm.util.Iterator;
import ovm.util.UnicodeBuffer;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import s3.util.Walkabout;
import java.util.Map;
import java.util.IdentityHashMap;
import java.io.IOException;

/**
 * A reflective mark/sweep garbage collector for objects that will
 * form the bootimage.  Bootimage creation is closely tied to
 * {@link ovm.core.services.memory.VM_Address VM_Address's} build-time
 * behavior.  We exploit this fact by treating the object to
 * VM_Address map as our heap, and storing each object's mark bit in
 * the corresponding VM_Address.<p>
 *
 * 
 **/
public class GC extends Walkabout implements Ephemeral.Void {
    private static GC the;
    
    Domain domain;
    Type.Context sysCtx;
    ArrayList rootObjects = new ArrayList();
    // Maps object references to their corresponding VM_Address.
    Map map;

    /**
     * When an object becomes pinned to a particular bootimage
     * address, we should add it to the list of roots, unless we are
     * saving the final image contents via {@link #dumpImage}.
     **/
    public boolean rootsFrozen = false;

    public GC(Domain domain, IdentityHashMap map) {
	super(true, false);

	assert the == null;
	the = this;

	this.domain = domain;
	this.map = map;
	sysCtx = domain.getSystemTypeContext();
    }

    public void init() {
	DomainSprout.registerAdvice(this);
	ImageObserver.the().registerGCAdvice(this);
	register(new SproutObjectAdvice());
	registerAfter(new StringSquirter());
    }

    public static GC the() {
	assert (the != null);
	return the;
    }

    /** Discards all primitive fields during object graph computation.
     * Since primitive fields are boxed, the ObjectAdvice would mistakenly
     * try to serialize them.   **/
    private final class SproutObjectAdvice implements ObjectAdvice {

        public Class targetClass() {
            return Object.class;
        }

	HashMap definedTypes = new HashMap();
	
        public Object beforeObject(Object o) {
	    // Treat VM_Address references and the objects that they
	    // refer to uniformly.  Otherwise, we may end up marking
	    // an object after walking (and ignoring) it's VM_Address.
	    if (o instanceof VM_Address) {
		VM_Address addr = (VM_Address) o;
		if (!addr.inBootImage() || addr.isMarked())
		    return null;
		o = addr.asObject();
	    }
            if (o instanceof S3Blueprint) {
                S3Blueprint bp = (S3Blueprint) o;
                if (!bp.isSharedState()) // add shared state BPs to set
                    visit(bp.getSharedState().getBlueprint());
            }
	    Class c = o.getClass();
            if (Ephemeral.class.isAssignableFrom(o.getClass()))
                return o;
	    Object isDefined = definedTypes.get(o.getClass());
	    if (isDefined == null) {
		try {
		    TypeName tn = ReflectionSupport.typeNameForClass(c);
		    Type t = sysCtx.typeFor(tn);
		    isDefined = t != null ? Boolean.TRUE : Boolean.FALSE;
		} catch (LinkageException _) {
		    isDefined = Boolean.FALSE;
		}
		definedTypes.put(c, isDefined);
	    }
	    if (isDefined == Boolean.FALSE) {
		//System.err.println("skip " + c + " instance");
		return null;
	    } else {
		//System.err.println("push " + c + " instance");
		return o;
	    }
        }
    } // End of SproutObjectAdvice

    /**
     * Just convert-n-squirt Strings at once into the image as they are
     * encountered, even though it blurs the phases; no more Mr Nice Guy.
     * Exploits the happy fact that our replacement class happens for now to
     * be named exactly java.lang.String, so uncheckedBlueprintFor(String.class)
     * makes just the blueprint we want without any special effort.
     * <p>
     * FIXME: We are doing a lot of work for each GC pass here, and if
     * there ARE any 64k+ strings, we will allocate new byte[]s for
     * them on every GC.
     * <p>
     * This code could just as easily run in ISerializer, because data
     * is always one of the UTF8Store arrays.
     */
    private final class StringSquirter implements ObjectAdvice {
        //FIXME: document which of these are shared between
        //domains. --jv

        private final Blueprint myStringBP =
	    ReflectionSupport.blueprintFor(targetClass(), domain);
        private final Field.Reference dataField = (Field.Reference)fi( "data:[B");
	private final Field.Integer offsetField = (Field.Integer)fi( "offset:I");
        private final Field.Integer countField =  (Field.Integer)fi( "count:I");
        private final Field.Integer hashCodeField = (Field.Integer)fi( "hashCode:I");

	private final byte[] EMPTY_BYTE_ARRAY = new byte[0];
        private final Oop noBytes = VM_Address.fromObject(EMPTY_BYTE_ARRAY).asOop();

        public Class targetClass() {
            return String.class;
        }

        /** Write the address of the empty byte array singleton into
         *  the image at the offset of the static field emptySelector.*/
        StringSquirter() {
            Field.Reference emptyField = (Field.Reference)fs("empty_:[B");
            Oop shst = myStringBP.getSharedState();
            emptyField.set( shst, noBytes);
        }

        /**
         * Before each string object <em>s</em>:
         * Allocate space for <em>s</em> in the image (<code>sloc</code>).
         * What we want is for <code>sloc</code> to point to an allocated instance
         * of the Ovm class named <code>java.lang.String</code>, not the JDK class
         * of the same name, and the "happy fact" is that it Just Works: because the
         * class names are the same, <code>fromObject(s)</code> allocates the Ovm
         * class whose name is <em>s</em>'s class name, and there you have it.
          
         * Ovm's class <code>java.lang.String</code> has three fields,
         * <code>data</code>, <code>count</code>, and <code>hashCode</code>.
         * The last two are set from <em>s</em>'s <code>length()</code> and
         * <code>hashCode()</code> methods. The consequence should be noted that
         * <code>count</code> will be the number of <em>characters</em> in the
         * original String, and not necessarily (depending on the encoding into bytes)
         * the number of <em>bytes</em> in the byte array.  This is presumably the
         * desired behavior and will be reliable once we observe a consistent
         * convention for character encoding--which we don't at present (see Issue
         * #33).
         
         * If <em>s</em> is empty, the address of a statically allocated empty byte
         * array is copied into <code>data</code>. Otherwise, a byte array is
         * obtained from <em>s</em>, space allocated for it in the image, and the
         * allocation address is stored in <code>data</code>. The array is added to
         * the object set so it will later be visited (and its contents written to
         * the allocated space).
         
         * Finally, returning <code>null</code> ensures the fields of <em>s</em> will
         * not otherwise be processed.
         */
        public Object beforeObject(Object o) {
            String s = (String) o;
            int hash = s.hashCode();
            Oop dloc;
	    int off = 0;
            int len = 0;

            if (s.length() == 0) {
                dloc = noBytes;
            } else if (s.length() < 65536) {
		UnicodeBuffer sb = UnicodeBuffer.factory().wrap(s);
		// Interned strings are always stored in the Utf8
		// table, and aliasing the utf8 byte[] saves a second
		// copy.  Programatically generated strings, however,
		// will be added to the utf8 store.  We still save
		// space: the size of an array header - the utf8
		// entry's 2 byte length field.
		int index = UTF8Store._.installUtf8(sb);

		byte[] data = UTF8Store._.getUtf8Store(index);
		off = UTF8Store._.getUtf8Offset(index);
		len = UTF8Store._.getUtf8Length(index);
                dloc = VM_Address.fromObject( data).asOop();
		visit(data);
            } else {
		// We can't stuff extremely large strings in the utf8
		// store.  We also want to avoid storing multiple
		// copies of the Utf8 table in the image.
		UnicodeBuffer sb = UnicodeBuffer.factory().wrap(s);
		byte[] data = sb.toByteArray();
		off = 0;
		len = data.length;
		dloc = VM_Address.fromObject(data).asOop();
		visit(data);
	    }
            Oop sloc = VM_Address.fromObject( s).asOop(); // happy fact (see above)
	    
                dataField.set( sloc, dloc);
	      offsetField.set( sloc, off);
               countField.set( sloc, len);
            hashCodeField.set( sloc, hash);
            return null;
        }

        private Field fi( String f) {
            return f( myStringBP.getType().asScalar(), f);
        }
        private Field fs(String f) {
            return f( myStringBP.getType().getSharedStateType(), f);
        }
        private Field f( Type.Scalar ts, String f) {
            return ts.getField(
                RepositoryUtils.fieldSelectorFor( ts.getName().toString(), f).getUnboundSelector());
        }
    }

    public void addRoot(Object o) {
	if (!rootsFrozen)
	    rootObjects.add(o);
    }

    /**
     * Compute the  final object graph in three steps.
     * <ol>
     * <li> Build up a superset of the object graph by calling
     *      {@link Analysis#analyzePartialHeap} and discovering newly
     *      reachable objects until a fixpoint is reached.
     * <li> Remove unneeded objects through a hook: pruneObjectGraph
     * <li> Compute the final object graph, and pass this to the
     *      analysis once more via @see Analysis.analyzeFullHeap
     * </ol>
     **/
    public void fixObjectGraph(Analysis anal) {
	while (anal.analyzePartialHeap(map.values().iterator())) {
	    gc();
	} 
    }

    /**
     * If an object will form part of the bootimage and has not been
     * marked, mark it and return true.  Otherwise, return false;
     **/
    protected boolean markAsSeen(Object o) {
	assert (!(o instanceof VM_Address));
	VM_Address a = VM_Address.fromObject(o);
	if (a.inBootImage()) {
	    boolean wasMarked = a.setMarkBit(true);
	    return !wasMarked;
	} else
	    return false;
    }

    /**
     * Recompute the set of objects that should be written to the
     * bootimage for use at runtime.
     **/
    public void gc() {
	System.err.print("[garbage collecting... ");

	// Push roots onto mark stack

	// Make sure to walk the UTF8Store last.  StringSquirter can
	// insert new entries, possibly growing the byte[]s or a
	// collisions array.  The latter looks like this:
	// ERROR: element of class [[I found late: [I@129967
	visit(UTF8Store._);
	for (int i = 0; i < rootObjects.size(); i++)
	    visit(rootObjects.get(i));
	for (Iterator it = domain.getBlueprintIterator(); it.hasNext(); ) {
	    Blueprint bp = (Blueprint) it.next();
	    if (bp.isScalar() && !bp.isSharedState()) {
		TypeName tn = bp.getType().getUnrefinedName();
		Class c = ReflectionSupport.classForTypeName( tn);
		// Don't walk static fields of the runtime library.
		// Our runtime library implementation will change when
		// the executable is run.
		if (c.getClassLoader() != null) {
		    int before = worklist_.size();
		    visitFieldsOnly( c);
		    int after = worklist_.size();
		}
	    }
	}

	// Mark phase
	start();

	// Sweep phase
	int kept = 0;
	int removed = 0;
	for (java.util.Iterator it = map.entrySet().iterator();
	     it.hasNext(); ) {
	    Map.Entry ent = (Map.Entry) it.next();
	    VM_Address a = (VM_Address) ent.getValue();
	    assert (a.asObject() != null);
	    if (a.inBootImage()) {
		if (a.setMarkBit(false)) {
		    kept++;
		} else {
		    assert (!a.isPinned());
		    removed++;
		    it.remove();
		}
	    } else if (a.setMarkBit(false)) {
// 		System.out.println("external object " +
// 				   a.asObject() + " is marked");
	    }
	}
	System.out.println(" kept " + kept + " objects and removed " +
			   removed + "]");
    }

    /**
     * Write the state of live objects to the bootimage bytebuffer.
     **/
    void dumpImage(ISerializer dumper, String fname) throws IOException {
	gc();
	rootsFrozen = true;
	walkLiveObjects(dumper);
	BootImage.the().save(fname);
        ReflectionSupport.dumpOvmFieldStats();
    }

    /**
     * Apply a walkabout to all live objects in the bootimage, without
     * actually doing a depth-first traversal.
     **/
    void walkLiveObjects(Walkabout w) {
	for (java.util.Iterator it = map.values().iterator(); it.hasNext(); ) {
	    VM_Address addr = (VM_Address) it.next();
	    if (addr.inBootImage())
		w.visitFieldsOnly(addr.asObject());
	}
    }
}
 
