package ovm.core.domain;

import java.io.Serializable;

import ovm.core.OVMBase;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Selector;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import ovm.core.services.memory.FreeList;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.services.bytecode.editor.Marker;
import ovm.util.IdentityHashMap;
import ovm.util.Map;
import ovm.util.OVMError;
import s3.util.PragmaTransformCallsiteIR;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.util.PragmaTransformCallsiteIR.Rewriter;
import ovm.core.services.memory.PragmaNoReadBarriers;

/** A concrete implementation of this class defines a configuration's object
 *  model, the information that needs to be associated with objects and may
 *  (or may not) be stored in reserved space in an object's allocated memory.
 * <p>
 *  Different components may have their own sets of data they need to
 *  associate with objects, and any one OVM configuration may mix and match
 *  those components.  Each such component should define its own interface
 *  (extending {@link Oop}) that includes methods for manipulating the
 *  appropriate data. The methods should be chosen to reflect the natural,
 *  logical operations the component will need on its data, not on any
 *  consideration of how the data might be concretely laid out (or even whether
 *  they are actually collocated in the object's allocated space, or kept in
 *  auxiliary tables, or indexed by a few collocated bits, or whatever).
 *  In other words, each {@link Oop} subinterface should provide a purely
 *  logical view of the part of the object model some component cares about.
 *  The methods should be declared to throw {@link PragmaModelOp}.
 * <p>
 *  A particular OVM configuration will include some subset of those components,
 *  and so it needs (repeat after me) a concrete subclass of ObjectModel
 *  whose {@link #newInstance()} method instantiates a concrete subclass of
 *  {@link ObjectModel.Proxy Proxy} that is declared to implement all of the
 *  {@link Oop} subinterfaces used by the components included in the
 *  configuration.
 * <p>
 *  <strong>How to use it:</strong> As an example of a component, consider
 *  the package <code>ovm.services.monitors</code> which defines the notion of
 *  a monitor associated with an object. The interface 
 * {@link ovm.services.monitors.MonitorMapper}, which extends {@link Oop}, 
 * provides a method {@link ovm.services.monitors.MonitorMapper#getMonitor()}. 
 * This would be the intuitive code for getting the monitor of some Oop o:
 * <pre>((MonitorMapper)o).getMonitor();</pre>
 *  This intuitive code is not quite right: the checked cast to
 *  <code>MonitorMapper</code> would fail at run time (when you view an object
 *  as an Oop, its dynamic type still reflects the real type of the object).
 *  Instead, you need the unchecked cast obtained with
 *  {@link Oop#asAnyOop()} (which is faster, anyway):
 * <pre>((MonitorMapper)o.asAnyOop()).getMonitor();</pre>
 *  If you start with something declared Object instead of Oop, there is an
 *  extra step:
 * <pre>((MonitorMapper)VM_Address.fromObject(o).asAnyOop()).getMonitor();</pre>
 * <p>
 *  <strong>How it works:</strong> At run time, there is nowhere any actual
 *  instance of the concrete proxy class that implements the necessary Oop
 *  subinterfaces; instead, you are treating arbitrary object references as
 *  Oop references and invoking interface methods on them. That works because
 *  the interface methods declaring {@link PragmaModelOp} are all rewritten
 *  to inlined bytecode. The concrete subclass of ObjectModel is
 *  required to have an {@link #init()} method that will register
 *  {@link s3.util.PragmaTransformCallsiteIR.Rewriter}s for the methods
 *  of all Oop interfaces it supports.
 *  The rewriters really implement the behavior of the methods (except at
 *  image-build time, when real instances of the concrete proxy do exist and
 *  their methods are really called).
 * <p>
 *  The rewriting can be arbitrarily clever.  For example, the implementor may
 *  know that two logical fields need not both be valid at the same time, and
 *  contrive a way to use one word for both--even if the two are defined in
 *  interfaces of different components.  Each component Oop subinterface
 *  provides a logical view of one component's data in isolation, but
 *  the writer of a concrete implementing class has a global view of the
 *  interfaces to be supported and is unconstrained as to implementation
 *  technique.
 * <p>
 *  Bitfields can be defined; see
 *  {@link ovm.core.services.memory.VM_Word.Bitfield}.  See
 *  {@link ovm.core.services.memory.VM_Word.BfRewriter}.effect(...) for how to
 *  incorporate bitfield operations in your ModelOp rewriting.
 * <p>
 *  A concrete ObjectModel will supply implementations of
 *  {@link #newInstance()}, {@link #headerSkipBytes()},
 *  {@link #identityHash(Object)},
 *  {@link #toCStruct()}, and override {@link #stamp(VM_Address,Blueprint)} to
 *  set any initial values (certainly at least the Blueprint) needed
 *  when an object is allocated.
 *
 *  @author Chapman Flack
 **/
public abstract class ObjectModel
    extends OVMBase 
    implements FreeList, InvisibleStitcher.CoreComponent {
    /** The rewriters that have been registered. **/
    private static Map rewriters = new IdentityHashMap();
    /** Cache of the last declaring class given in a
     *  {@link #rewrite(String,String,PragmaTransformCallsiteIR.Rewriter)}
     *  call.
     **/
    private static String declarer = null;
    /** Rehash the rewriters map at boot time in case identity hashes change **/
    private static void boot_() { rewriters = new IdentityHashMap( rewriters); }

    /** Cache of {@link #headerSkipBytes()} result **/
    private static final int SKIPBYTES = getObjectModel().headerSkipBytes();
    
    /** Cache of {@link #maxReferences()} result **/
    private static final int MAXREFS = getObjectModel().maxReferences();
    

    public static final int FWD_IGNORE = 0; // no explicit setting needed
    public static final int FWD_SELF = 1;
    // public static final int FWD_ZERO
    
    public int getUnforwardedSemantics() {
      return FWD_IGNORE;
    }
    
    public int getForwardOffset() {
      return -1;
    }

    public int getMonitorOffset() {
      return -1;
    }

    /** Return the ObjectModel for the current configuration.
     *  @return Singleton instance of the implementation class configured via
     *  the {@link InvisibleStitcher}
     *  @throws PragmaStitchSingleton so invocations of this method are
     *  rewritten to load-constant of the singleton
     **/
    public static ObjectModel getObjectModel()
    throws PragmaStitchSingleton {
      	return (ObjectModel)InvisibleStitcher.singletonFor(
	    "ovm.core.domain.ObjectModel");
    }

    /** Method we call on the implementing class to get it to register its
     *  rewriters (and it could do other stuff it needed to).
     **/
    protected abstract void init() throws BCdead;
    
    /** OVM-specific: implementation detail: only hosted: return a new instance
     *  of a class that extends VM_Address and implements the currently
     *  configured model methods. <em>Only VM_Address should use this
     *  method.</em>
     *  @throws BCdead <em>this is a pragma - no one calls this method at
     *  runtime</em>
     **/
    public abstract VM_Address newInstance() throws BCdead;

    /** Return the number of bytes that must be added to an Oop to obtain
     *  the address of the first object data. A rewriter is supplied
     *  automatically to replace calls of this method with the inlined value.
     **/
    public abstract int headerSkipBytes() throws PragmaModelOp;

    /**
     * Return an object's alignment.  Currently, s3.core.domain
     * assumes an alignment of 4 and aligns double-words to word
     * boundaries.  Ideally, s3.core.domain would determine it's
     * alignment constraints from the configured object model.
     **/
    public int alignment() throws BCiconst_4 {
	return 4;
    }

    /**Return the identityHash for an object <em>or a null reference</em>.
     * By definition zero must be returned in the case of a null reference.
     * In addition to handling null references (which would actually not be
     * impossible with the rewritten {@link Oop#getHash() Oop.getHash} instance
     * method, but would be counterintuitive), this method must work at hosted
     * (build) time <em>without</em> allocating image space as a side effect
     * (which would happen if you just wrote
     * {@link VM_Address#fromObject(Object)}.{@link VM_Address#asOop()
     *  asOop()}.{@link Oop#getHash() getHash()}.
     * <p>
     * Call sites of this method are rewritten to a check for null followed
     * by the rewriting of {@link Oop#getHash() Oop.getHash()}.
     * @see Oop#getHash()
     **/
    public abstract int identityHash( Object o) throws PragmaModelOp;
    
    /**The object model may associate, with every object, references in addition to
     * the user-visible reference fields of the object, and a memory manager needs to
     * retrieve them; use this method to query how many (n) there may be.
     * The memory manager then calls <code>o.getReference(k)</code>, on any
     * {@link Oop} o, for k from 0 to n, and processes the reference. For a given
     * Oop o, some of the references returned may be null; n is only the maximum
     * number of hidden references an Oop <em>may</em> have in the prevailing model.
     * <p />
     * <strong>Revisitable design decisions:</strong>
     * This idiom is chosen to simplify scanning references in a memory manager loop
     * without allocating an array for results, and supposing the logic of
     * {@link Oop#getReference(int) getReference} can be efficiently inlined into the
     * loop.  It might be slightly faster to preallocate (per thread?) an array of
     * this size, and have a <code>getReferences()</code> method to fill it, but that
     * would touch more code, and a <code>for</code> would still be necessary to step
     * through the array.
     * <p />
     * We do not now include the Blueprint in this count, as we believe all blueprints
     * are reachable through the domain and so there is no need to query all Oops for
     * them. If an object model's only reference is a blueprint, this method should
     * return zero. The principle can be extended to any model that keeps references
     * known to be reachable always through some other path.
     * @return The integer <em>n</em> such that, in the prevailing model, calls
     * of {@link Oop#getReference(int) getReference(<em>k</em>)} are valid for
     * 0 &lt;= <em>k</em> &lt; <em>n</em>.
     * @throws PragmaModelOp
     * @see Oop#getReference(int)
     */
    public abstract int maxReferences() throws PragmaModelOp;
    
    /**
     * Returns a string with a description of the header data
     * structure in C.  The structure is called HEADER, and its
     * contents are opaque.  Two accessor macros are also defined
     * <dl>
     * <dt>HEADER_BLUEPRINT</dt><dd>Returns an appropriately typed
     * reference to an object's blueprint.  For example,
     * HEADER_BLUEPRINT for an S3 object model should return an
     * S3Blueprint pointer.</dd>
     * <dt>HEADER_HASHCODE</dt><dd>Returns the object's identity
     * hashCode if available</dd>
     **/ 
    public abstract String toCStruct();

    /**
     * Returns a string with a description of the header data
     * structure in C++.  As with toCStruct, this string defines a
     * struct called HEADER and two macros that operate on the
     * struct.
     *
     * @see #toCStruct()
     **/
    public abstract String toCxxStruct();

    /**
     * Returns a machine code that performs 'getBlueprint' on X86.
     * The contents of the registers other than the given two
     * registers (the two may be the same register) should be
     * preserved. -HY
     *<p />
     * This method gives me nightmares. -CF
     */
    public abstract byte[] x86_getBlueprint(byte objectRegister,
					    byte blueprintRegister);

    /**
     * Returns a machine code that performs 'getBlueprint' on PowerPC.
     * The contents of the registers other than the given two
     * registers (the two may be the same register) should be
     * preserved. -HY
     *<p />
     */
    public abstract int[] ppc_getBlueprint(int blueprintRegister, int objectRegister);
    
    /** Stamp the Blueprint (at least) and any other appropriate model data
     *  at the address <em>a</em>, by which act it becomes an Oop, which is
     *  returned. Subclasses should override to stamp the right stuff in the
     *  right place, but should always end with a <code>super</code> call to
     *  this method.  This method is for use by the allocate methods of
     *  Blueprint.
     *  @param a VM_Address of newly-allocated space, to be stamped and made
     *  into an Oop.
     *  @param bp Blueprint to be associated with the new Oop. It is hoped that
     *  anything else needing to be stamped in a particular object model
     *  (hash code, GC state, etc.) is fixed or can be determined without
     *  additional parameters.
     *  @return the address <em>a</em>, now considered to be of type Oop.
     **/
    public Oop stamp( VM_Address a, Blueprint bp) throws PragmaNoReadBarriers {
      	Proxy ah = (Proxy)a.asAnyOop();
	return ah.stamp( bp);
    }
    
    public VM_Address getPrev(VM_Address slot) {
	return slot.getAddress();
    }
    
    public void setPrev(VM_Address slot,VM_Address prev) {
	slot.setAddress(prev);
    }
    
    public VM_Address getNext(VM_Address slot) {
	return slot.add(VM_Word.widthInBytes()).getAddress();
    }
    
    public void setNext(VM_Address slot,VM_Address next) {
	slot.add(VM_Word.widthInBytes()).setAddress(next);
    }

    public VM_Address getCustom(VM_Address slot) {
	return slot.add(VM_Word.widthInBytes()*2).getAddress();
    }
    
    public void setCustom(VM_Address slot,VM_Address value) {
	slot.add(VM_Word.widthInBytes()*2).setAddress(value);
    }
 
    /** Return the rewriter associated with a given PragmaModelOp method.
     *  This method is for the use of the PragmaModelOp generic rewriter
     *  and the access modifier could probably be tightened.
     *  @param sel Selector of the methods whose rewriter is wanted
     **/
    public Rewriter rewriter( Selector.Method sel) {
      	Rewriter r = (Rewriter)rewriters.get( sel);
	if ( null == r )
            throw new Rewriter.PuntException( "Unimplemented PragmaModelOp");
	return r;
    }
    
    static {
        final Selector.Method oopGetHash =
      	    RepositoryUtils.methodSelectorFor( Oop.ID, "getHash:()I");

      	rewrite( "Lovm/core/domain/ObjectModel;",
	         "headerSkipBytes:()I",
		  new Rewriter() {
		      protected boolean rewrite() {
		      	  cursor.addSimpleInstruction( POP);
			  cursor.addLoadConstant( SKIPBYTES);
			  return true;
		      }
		  });
        rewrite( "maxReferences:()I",
                 new Rewriter() {
                     protected boolean rewrite() {
                         cursor.addSimpleInstruction( POP);
                         cursor.addLoadConstant( MAXREFS);
                         return true;
                     }
                 });
	rewrite( "identityHash:(Ljava/lang/Object;)I",
	      	  new Rewriter() {
		      protected boolean rewrite() {
		      	  Marker a = cursor.makeUnboundMarker();
		      	  Marker b = cursor.makeUnboundMarker();
			  cursor.addSimpleInstruction( SWAP); // oop model
			  cursor.addSimpleInstruction( POP);  // oop
			  cursor.addSimpleInstruction( DUP);  // oop oop
			  cursor.addIf( IFNONNULL, a);
			  // 3.4: "The Java virtual machine specification does
			  // not mandate a concrete value encoding null."
			  // Better supply a zero, not just assume it is zero.
			  cursor.addSimpleInstruction( POP);
			  cursor.addLoadConstant( 0);
			  cursor.addGoto( b);
			  cursor.bindMarker( a); // oop
			  getObjectModel().rewriter( oopGetHash).effect( this);
			  cursor.bindMarker( b);
			  return true;
		      }
		  });
      	rewrite( Oop.ID, "headerSkip:()Lovm/core/services/memory/VM_Address;",
	      	  new Rewriter() {
		      protected boolean rewrite() {
			  cursor.addSimpleInstruction(IFIAT);
			  cursor.addLoadConstant( SKIPBYTES); // should effect
			  cursor.addSimpleInstruction( IADD); // VM_Address.add
			  addFiat();
			  return true;
		      }
	      	  });
        rewrite( "metaToString:()Ljava/lang/String;",
                  new Rewriter() {
                      final Selector.Method gbp =
                          RepositoryUtils.methodSelectorFor(
                              Oop.ID,
                              "getBlueprint:()Lovm/core/domain/Blueprint;");
                      final Selector.Method vats =
                          RepositoryUtils.selectorFor(
                              VM_Address.typeNameVMA,
                              JavaNames.java_lang_String,
                              "toString",
                              TypeName.EMPTY_ARRAY);
                      protected boolean rewrite() {
                          cursor.addSimpleInstruction( DUP);    // oop oop
                          cursor.addINVOKEINTERFACE( gbp);      // oop bp
                          cursor.addINVOKEVIRTUAL( JavaNames.   // oop nm
                              java_lang_Object_toString);
                          cursor.addNEW( JavaNames.java_lang_StringBuffer);
                          cursor.addSimpleInstruction( DUP_X1); // oop b nm b
                          cursor.addSimpleInstruction( SWAP);   // oop b b nm
                          cursor.addINVOKESPECIAL( JavaNames.   // oop b
                              java_lang_StringBuffer_STRING_Constructor);
                          cursor.addSimpleInstruction( SWAP);   // b oop
                          cursor.addINVOKEINTERFACE( vats);     // b @
                          cursor.addINVOKEVIRTUAL( JavaNames.   // b
                              java_lang_StringBuffer_STRING_append);
                          cursor.addINVOKEVIRTUAL( JavaNames.
                              java_lang_Object_toString);       // s
                          return true;
                      }
                  });
        /* One could consider adding here a default rewriter for getReference(k)
         * for models with maxReferences()==0.  Assuming clients properly use
         * the Iterito pattern, their loops will execute 0 times and callsites
         * of getReference will be unreachable. In fact they will be statically
         * provably dead because maxReferences() is an inlined constant.
         * 
         * So we have choices here:
         *
         * 1. register nothing by default. The rewriter will warn about callsites
         *    of oop.getReference(k), and replace them with unconditional throws.
         *    This is not such a bad thing to have happen; it's essentially an
         *    assertion that the callsites are unreachable, which is pretty much
         *    what we mean.  On the other hand, the warnings will be extra noise,
         *    and the throws take up space in the code that we really are pretty
         *    darn sure is dead.
         * 2. register a trivial rewriter that emits a zero-bytecode sequence.
         *    That would eliminate the warning, and reduce the dead-code size to
         *    zero.  But static analysis would see unbalanced stack effect and
         *    the wrong type on ToS: [ oop k ] instead of [ ref ].
         * 3. register a rewriter that emits POP. That would eliminate warnings
         *    and make static analysis happy, as it would leave a stack with a
         *    single ref on it.  It should even be a safe value to return if by
         *    some bug client code actually reaches a callsite.  And the dead
         *    code size is still only one byte.
         * 4. register a rewriter that emits POP2 ACONST_NULL. This only differs
         *    from 3 in the value it would return to buggy code that reaches a
         *    callsite.  And it's two bytes of dead code.
         * 
         * For now I've done (1), as you can see by the absence of code below. :)
         * If we ever care enough to change that, my choice would then be (3).
         */

	getObjectModel().init();
    }
    
    /**
     * In object models that support fast locks, no late
     * initialization is needed.
     **/
    public void initialize() throws BCdead { }

    /** Declare a rewriter for a given method of a given declaring class
     *  (interface, really).
     *  @param dclString name (in Lfoo/bar; form) of the declaring
     *  class/interface. Will be remembered for following invocations of
     *  {@link #rewrite(String,PragmaTransformCallsiteIR.Rewriter)}
     *  to reduce tedium of declaring several
     *  methods for one interface.
     *  @param selString selector (in name:(param)return form) for the method
     *  whose rewriter is being declared.
     *  @param rewriter a Rewriter to use for the method.
     **/
    protected static final void rewrite( String dclString,
      	      	      	      	      	 String selString,
					 Rewriter rewriter) {
      	declarer = dclString;
	rewrite( selString, rewriter);
    }
    
    /** Declare a rewriter for a given method of the last mentioned declaring
     *  class (interface, really).
     *  @param selString selector (in name:(param)return form) for the method
     *  whose rewriter is being declared. The declaring class/interface will be
     *  the one named in the most recent
     *  {@link #rewrite(String,String,PragmaTransformCallsiteIR.Rewriter)}.
     *  @param rewriter a Rewriter to use for the method.
     **/
    protected static final void rewrite( String selString, Rewriter rewriter) {
      	assert( declarer != null );
	Selector.Method sel =
	    RepositoryUtils.methodSelectorFor( declarer, selString);
	rewriters.put( sel, rewriter);
    }
    
    /** OVM-specific: implementation detail: only hosted: every non-null
     *  VM_Address you get when running hosted is actually one of these, so
     *  that the casts to Oop types can succeed in the host VM - but one should
     *  not write code that knows this!  Implementations of ObjectModel
     *  must provide concrete subclasses of this that implement whatever
     *  combination of Oop interfaces will be supported.
     **/
    protected static abstract class Proxy
    extends VM_Address implements Oop, Serializable {
      	/** Advance past the header of this Oop. Implementation and rewriter
	 *  supplied automatically based on {@link #headerSkipBytes()}.
	 *  @return VM_Address of the first object data following the header.
	 **/
	public VM_Address headerSkip() throws BCdead {
	    return add( SKIPBYTES);
	}

        /** Default implementation for build time that says
         *  "don't do this at build time."
         **/
        public Oop getReference( int k) throws BCdead {
            throw new OVMError.Unimplemented( "getReference at build time");
        }

	/** Used by {@link ObjectModel#stamp(VM_Address,Blueprint)}.
	 *  For the time being, that method is the one that should be overridden
	 *  in concrete models, and this one handles only the magic
	 *  VM_Address internals and is final. I may rethink this division of
	 *  labor.
	 **/
	final Oop stamp( Blueprint bp) throws BCpop {
	    setBlueprint( bp);
      	    return (Oop)this;
	}
	
	/** Return the blueprint for this Oop. The hosted behavior is special
	 *  and shouldn't be overridden: if there's a blueprint for the object
	 *  it is part of the VM_Address state and we get it by calling this
	 *  handy protected super method.
	 **/
      	public final Blueprint getBlueprint() throws BCdead {
	    return super.getBlueprint();
	}
	
	/** Get the identity hash for this Oop. What this should do hosted
	 *  is perhaps debatable; right now it returns the host VM's identity
	 *  hash for the object, but that might differ from our own hash,
	 *  causing headaches for hash tables created at build time and used
	 *  at run time. Another approach would be to return the same value
	 *  we will return at run time--ovm.util.IdentityHashMap would then
	 *  work without headaches.
	 **/
	public int getHash() throws BCdead {
	    return System.identityHashCode( asObject());
	}

        /** Return a string indicating the type and address of this Oop.
         *  Hosted, this just delegates to {@link VM_Address#toString()},
         *  which (hosted) does include the type information when available,
         *  so it's what we want.
         **/
        public String metaToString() throws BCdead {
            return toString();
        }
    }
    
    /** Pragma that turns any method into a ModelOp. A concrete model
     *  is expected to register rewriters for all methods wearing this pragma
     *  declared in all Oop interfaces the concrete class means to support.
     **/
    public static class PragmaModelOp extends PragmaTransformCallsiteIR {
      	static {
	    register( "ovm.core.domain.ObjectModel$PragmaModelOp",
	      	      new Rewriter() {
		      	  protected boolean rewrite() {
			      getObjectModel()
			      	  .rewriter( targetSel).effect( this);
			      return true; // delete the original instruction
			  }
		      });
	}
    }
    
    /** A pragma */
    public static class BCiconst_4 extends PragmaTransformCallsiteIR {
	static {
	    register(BCiconst_4.class.getName(),
		     new byte[] { ICONST_4 });
	}
    }
    public static class BCpop extends PragmaTransformCallsiteIR {
      	static {
	    register( BCpop.class.getName(),
	      	      new byte[] { POP });
	}
    }
}
