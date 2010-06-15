package s3.core.domain;

import ovm.core.repository.Selector;
import ovm.core.domain.Blueprint;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.execution.Context;
import ovm.core.repository.RepositoryUtils;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word.Bitfield;
import ovm.core.services.memory.VM_Word.BfRewriter;
import ovm.services.bytecode.editor.Marker;
import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;
import ovm.services.monitors.FastLockable;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.util.OVMError;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.util.PragmaTransformCallsiteIR.Rewriter;

/** Describes an object layout with one 32word for blueprint,
 *  one 32word for fastlock, and hash==address (for nonmoving
 *  collectors only!). The fastlock word comprises a 'count' field
 *  (the two low bits) and an 'owner' field (the rest).  The owner field
 *  can hold either a Context or a Monitor (both assumed aligned on 4-byte
 *  boundaries so the two low bits can be stolen).  It is a Monitor if the
 *  count field holds the value INFLATED (3).  Otherwise, if it is nonzero,
 *  it is a Context and the count field holds a recursion count (0 for the
 *  first lock).  All-zeros indicate an unlocked, uninflated object.  A nonzero
 *  count field with zeros in the owner field should not happen; it will be
 *  treated here as a recursion count with null Context for owner.
 *  @author Chapman Flack
 **/
public class S3Model_B_Mf extends ObjectModel implements FastLockable.Model {

    /**
     * Should we add an explicit null check on fastLock(), or should
     * we assume it has already been checked.  If fastLock() is called
     * as part of the implemenation of MONITORENTER, there should have
     * been a null check already.  If attemptUpdate expands to
     * GETFIELD_QUICK/SETFIELD_QUICK, these instructions will perform
     * the null check.  If j2c is used, every memory access results in
     * a null check.
     *
     * Because the fast lock client code lives in
     * CoreServicesAccess.monitorEnter, a null check is not needed.
     */
    static final boolean CHECK_NULL = false;
    
    private static final char BP_OFFSET = 0;
    private static final char FASTLOCK_OFFSET = 4;
    // FIXME:  This should really be alignment() - 1, and
    // CountBits.WIDTH should be log2(alignment()).
    private static final int INFLATED = 3; // 0..00011
    private static int depth = 0;
    
    // the methods in this class should not be called directly (i.e. with
    // this class specified at the call site) but rather on the appropriate
    // interfaces or ObjectModel, where the pragmas giving the correct
    // runtime behavior are specified.  To catch perverse attempts to call
    // directly on this class, the methods here will all throw BCdead.
    
    public int maxRecursionCount() throws BCdead { return INFLATED - 1; }
    
    /**
     * Bits in the FASTLOCK word used for recursion count (and indicating
     * interpretation of the owner bits).
     **/
    static class CountBits extends Bitfield {
        static final int WIDTH = 2, SHIFT = 0;
        static final Bitfield bf = bf( WIDTH, SHIFT);
    }
    
    /**
     * Bits in the FASTLOCK word used for owner (with a recursion count) or for
     * a monitor (when the count field contains 11). Positioned so that unshifted
     * access will produce a valid reference (assumed below).
     **/
    static class OwnerBits extends Bitfield {
        static final int WIDTH = 30, SHIFT = 2;
        static final Bitfield bf = bf( WIDTH, SHIFT);
    }
    
    public VM_Address newInstance() throws BCdead { return new Proxy(); }
    
    public int headerSkipBytes() throws BCdead { return 8; }
    
    // The monitor counts (it's not otherwise reachable). The blueprint doesn't
    // (see ObjectModel comments).
    public int maxReferences() throws BCdead { return 1; }
    
    public int identityHash( Object o) throws BCdead
      	{ return System.identityHashCode( o); }
    
    // no BCdead here please - I am a real method.
    public Oop stamp( VM_Address a, Blueprint bp) {
        assert( 100 > ++depth);
      	a.add( BP_OFFSET).setAddress( VM_Address.fromObject( bp));
        --depth;
	// anything else that needs initial value (hash code, etc....)
	return super.stamp( a, bp);
    }

    /**Returns a string with a description of the header data structure in C.**/
    public String toCStruct() {
        return ("struct HEADER {\n"+
		"\tstruct s3_core_domain_S3Blueprint *_blueprint_;\n" +
                "\tjint _fastlock_;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"(((struct HEADER*)(H))->_blueprint_)\n" +
		"#define HEADER_HASHCODE(H)  ((jint) (H))\n");
    }
    public String toCxxStruct() {
	return ("struct HEADER {\n"+
		"\tstruct e_s3_core_domain_S3Blueprint *blueprint;\n" +
		"\tjint fastlock;\n" +
		"};\n"+
		"#define HEADER_BLUEPRINT(H) " +
		"(((struct HEADER*)(H))->blueprint)\n" +
		"#define HEADER_HASHCODE(H)  ((jint) (H))\n");		
    }

    public byte[] x86_getBlueprint(byte objectRegister,
				   byte blueprintRegister) {
	// X86 : mov 0(objectRegister) -> blueprintRegister
	return new byte[]
	    { (byte)0x8B, (byte)(0 | (blueprintRegister << 3) | objectRegister ) };
    }

    public int[] ppc_getBlueprint(int blueprintRegister, int objectRegister) {
        // PPC : lwz blueprintRegister <- 0(objectRegister)
        return new int[] { ((32 << 26) | (blueprintRegister << 21) | (objectRegister << 16) | (0 << 0)) };
    }

    // I won't bother adding BCdeads to this private class, here
    private static class Proxy
    extends ObjectModel.Proxy
    implements MonitorMapper, FastLockable {
        // MonitorMapper:
	public Monitor getMonitor() {
	    throw new OVMError.Unimplemented( "getMonitor at build time");
	}

	public void setMonitor(Monitor monitor) {
	    throw new OVMError.Unimplemented( "setMonitor at build time");
	}
	
	public void releaseMonitor() {
	    throw new OVMError.Unimplemented( "releaseMonitor at build time");
	}

        // FastLockable
        public boolean fastLock() throws PragmaModelOp {
            throw new OVMError.Unimplemented( "fastLock at build time");
        }

        public boolean fastUnlock() throws PragmaModelOp {
            throw new OVMError.Unimplemented( "fastUnlock at build time");
        }

        public Context getOwner() throws PragmaModelOp {
            throw new OVMError.Unimplemented( "getOwner at build time");
        }

        public int getRecursionCount() {
            throw new OVMError.Unimplemented( "getRecursionCount at build time");
        }

        public boolean isFastLocked() throws PragmaModelOp {
            throw new OVMError.Unimplemented( "isFastLocked at build time");
        }

        public boolean isMine() throws PragmaModelOp {
            throw new OVMError.Unimplemented( "isMine at build time");
        }

        public boolean isInflated() throws PragmaModelOp {
            throw new OVMError.Unimplemented( "isInflated at build time");
        }

        public boolean isFastUnlocked() throws PragmaModelOp {
            throw new OVMError.Unimplemented( "isFastUnlocked at build time");
        }

        public void setOwner(Context ctx) throws PragmaModelOp {
            throw new OVMError.Unimplemented( "setOwner at build time");
        }

        public void setRecursionCount(int rc) {
            throw new OVMError.Unimplemented( "setRecursionCount at build time");
        }
    }
    
    protected void init() throws BCdead {
        S3Model_B_M.initOop( BP_OFFSET);
        initFastLockable(  FASTLOCK_OFFSET);
        initMonitorMapper( FASTLOCK_OFFSET);
        initGetReference1( FASTLOCK_OFFSET);
        // initUpdate1( BP_OFFSET, FASTLOCK_OFFSET); only needed for moving MM
        initFastLockableModel( maxRecursionCount());
    }

    private interface SYS extends InvokeSystemArguments { }

    protected static void initFastLockable( final char fastlockOffset)
    throws BCdead {
        final Selector.Method vaai = // VM_Address.add(i)
            RepositoryUtils.methodSelectorFor(
                "Lovm/core/services/memory/VM_Address;",
                "add:(I)Lovm/core/services/memory/VM_Address;");
        final Selector.Method aoauaii = // AtomicOps.attemptUpdate(VMA,i,i)
            RepositoryUtils.methodSelectorFor(
                "Lovm/core/services/memory/AtomicOps;",
                "attemptUpdate:(Lovm/core/services/memory/VM_Address;II)Z");

        rewrite( FastLockable.ID, "fastLock:()Z",
            new Rewriter() {
                protected boolean rewrite() {
                    Marker end = cursor.makeUnboundMarker();
		    Marker fail = cursor.makeUnboundMarker();

		    if (CHECK_NULL) {
			cursor.addSimpleInstruction( DUP); // oop oop
			cursor.addIf( IFNULL, end); // null on ToS ==
						    // boolean false
		    }
		    cursor.addSimpleInstruction(DUP); // oop oop
		    cursor.addQuickOpcode(GETFIELD_QUICK, fastlockOffset); // oop fastlock-value
		    cursor.addIf(IFNE,fail); // oop
		    
		    cursor.addLoadConstant(0); // oop 0
		    cursor.addINVOKESYSTEM((byte) SYS.GET_CONTEXT); // oop context
		    cursor.addQuickOpcode(PUTFIELD_QUICK_WITH_BARRIER_REF, fastlockOffset); // <empty>
		    cursor.addLoadConstant(1); // 1
		    cursor.addGoto(end);
		    
		    cursor.bindMarker( fail);
		    cursor.addSimpleInstruction(POP); // <empty>
		    cursor.addLoadConstant(0); // 0
		    
		    cursor.bindMarker( end);
                    return true; // delete original instruction
                }
            });
        rewrite( "fastUnlock:()Z",
            new Rewriter() {
                protected boolean rewrite() {
                    Marker end = cursor.makeUnboundMarker();
		    Marker fail = cursor.makeUnboundMarker();

		    cursor.addSimpleInstruction(DUP); // oop oop
		    cursor.addQuickOpcode(GETFIELD_QUICK, fastlockOffset); // oop fastlock-value
		    cursor.addLoadConstant(0); // oop fastlock-value 0
		    cursor.addINVOKESYSTEM((byte) SYS.GET_CONTEXT); // oop fastlock-value context
		    cursor.addIf(IF_ICMPNE,fail); // oop
		    
		    cursor.addLoadConstant(0); // oop 0
		    cursor.addQuickOpcode(PUTFIELD_QUICK_WITH_BARRIER_REF, fastlockOffset); // <empty>
		    cursor.addLoadConstant(1); // 1
		    cursor.addGoto(end);
		    
		    cursor.bindMarker( fail);
		    cursor.addSimpleInstruction(POP); // <empty>
		    cursor.addLoadConstant(0); // 0
		    
		    cursor.bindMarker( end);
                    return true; // delete original instruction
                }
            });
        rewrite( "getOwner:()Lovm/core/execution/Context;",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset);
                    OwnerBits.bf.effect( this, false, false, BfRewriter.INT);
                    addFiat();
                    return true; // delete original instruction
                }
            });
        rewrite( "getRecursionCount:()I",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset);
                    CountBits.bf.effect( this, false, true, BfRewriter.INT);
                    return true; // delete original instruction
                }
            });
        rewrite( "isFastLocked:()Z",
            new Rewriter() {
                // compute owner-field-nonzero AND count-field-not-INFLATED
                // this implementation does not branch
                protected boolean rewrite() {                    
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // w
                    cursor.addSimpleInstruction( DUP); // w w
                    OwnerBits.bf.effect( this, false, false, BfRewriter.INT);
                    cursor.addSimpleInstruction( DUP); // w o o
                    cursor.addSimpleInstruction( INEG); // w o -o
                    cursor.addSimpleInstruction( IOR); // w o|-o
                    cursor.addLoadConstant( -1);
                    cursor.addSimpleInstruction( ISHR); // w ones-if-o-nonzero
                    cursor.addSimpleInstruction( SWAP); // ones w
                    CountBits.bf.effect( this, false, true, BfRewriter.INT);
                    cursor.addLoadConstant( INFLATED); // ones cnt INFLATED
                    cursor.addSimpleInstruction( DUP_X2); // INF ones cnt INF
                    cursor.addSimpleInstruction( IXOR); // INF ones cnt!=INF
                    cursor.addSimpleInstruction( IAND); // INF ones&cnt!=INF
                    cursor.addSimpleInstruction( IAND); // nonzero-if-fastlocked
                    addTrailingIntNe0();
                    return true; // delete original instruction
                }
            });
        rewrite( "isMine:()Z",
            // as long as a Context and a Monitor can't be the same object, a
            // simple comparison here suffices.
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset);
                    OwnerBits.bf.effect( this, false, false, BfRewriter.INT);
		    cursor.addLoadConstant( 0);
                    cursor.addINVOKESYSTEM((byte)SYS.GET_CONTEXT);
                    cursor.addSimpleInstruction( IXOR);
                    addTrailingIntEq0();
                    return true; // delete original instruction
                }
            });
        rewrite( "isInflated:()Z",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset);
                    CountBits.bf.effect( this, false, true, BfRewriter.INT);
                    cursor.addLoadConstant( INFLATED);
                    cursor.addSimpleInstruction( IXOR);
                    addTrailingIntEq0();
                    return true; // delete original instruction
                }
            });
        rewrite( "isFastUnlocked:()Z",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset);
                    addTrailingIntEq0();
                    return true; // delete original instruction
                }
            });
        rewrite( "setOwner:(Lovm/core/execution/Context;)V",
            new Rewriter() {
                protected boolean rewrite() { // oop ctx
                    cursor.addSimpleInstruction( SWAP); // ctx oop
                    cursor.addSimpleInstruction( DUP_X1); // oop ctx oop
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // w
                    cursor.addSimpleInstruction( SWAP); // oop w ctx
                    OwnerBits.bf.effect( this, true, false, BfRewriter.INT);
                    cursor.addQuickOpcode( PUTFIELD_QUICK, fastlockOffset);
                    return true; // delete original instruction
                }
            });
        rewrite( "setRecursionCount:(I)V",
            new Rewriter() {
                protected boolean rewrite() { // oop cnt
                    cursor.addSimpleInstruction( SWAP); // cnt oop
                    cursor.addSimpleInstruction( DUP_X1); // oop cnt oop
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // w
                    cursor.addSimpleInstruction( SWAP); // oop w cnt
                    CountBits.bf.effect( this, true, true, BfRewriter.INT);//oop w
                    cursor.addQuickOpcode( PUTFIELD_QUICK, fastlockOffset);
                    return true; // delete original instruction
                }
            });
    }

    /** Set up rewriters for MonitorMapper methods.
     * @throws BCdead <em>this is a pragma</em>
     */
    protected static void initMonitorMapper( final char fastlockOffset)
    throws BCdead {
  	rewrite( MonitorMapper.ID, "releaseMonitor:()V",
            new Rewriter() {
                protected boolean rewrite() {
                    // we're unreachable, so the monitor is
                    // too. nothing to do.

		    // Umm, is this method ever supposed to be called
		    // from GC?  I would certainly hope not.  What is
		    // the purpose here?  David has a test case that
		    // verifies releaseMonitor() does something, but I
		    // don't think releaseMonitor() is ever asked do
		    // do something other than prove that it works.
		    // Nor should it be!

		    // Also note that `return true' is a broken
		    // implemenation.  At the very least, we need to
		    // pop our input!
		    cursor.addLoadConstant(0);
		    cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset);
                    return true; // delete original instruction
                }
            });

        rewrite( "getMonitor:()Lovm/services/monitors/Monitor;",
            new Rewriter() {
                protected boolean rewrite() {
                    Marker a = cursor.makeUnboundMarker();
                    Marker b = cursor.makeUnboundMarker();
	  
                                                       // oop
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset);
		    cursor.addSimpleInstruction( DUP);
                    CountBits.bf.effect( this, false, true, BfRewriter.INT);
                    cursor.addLoadConstant( INFLATED); // w cnt INFLATED
                    cursor.addIf( IF_ICMPNE, a);
                    OwnerBits.bf.effect( this, false, false, BfRewriter.INT);
                    addFiat(); // mon
                    cursor.addGoto( b);
                a:  cursor.bindMarker( a);             // w
                    cursor.addSimpleInstruction( POP); // 
		    cursor.addSimpleInstruction( ACONST_NULL);
                b:  cursor.bindMarker( b);
                    return true;
                }
            });

        rewrite( "setMonitor:(Lovm/services/monitors/Monitor;)V",
            new Rewriter() {
                protected boolean rewrite() { // oop mon
                    cursor.addLoadConstant( INFLATED); // oop mon INFLATED
                    CountBits.bf.effect( this, true, true, BfRewriter.INT);//oop w
                    cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset);
                    return true; // delete original instruction
                }
            });
    }
   
    /** Set up Oop's getReference rewriter to return the monitor reference
     *  if the object is inflated.
     * @throws BCdead <em>this is a pragma</em>
     */
    protected static void initGetReference1( final char fastlockOffset)
    throws BCdead {
        rewrite( Oop.ID, "getReference:(I)Lovm/core/domain/Oop;",
            new Rewriter() {
                protected boolean rewrite() { // oop k
                    cursor.addSimpleInstruction( POP); // oop
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // w
		    OwnerBits.bf.effect( this, false, false, BfRewriter.INT);
		    addFiat();
		    return true;
		}
	    });
    }

    /** Register rewriters for WithUpdate: updateBlueprint and updateReference(k)
     *  methods; usable where there is exactly one reference besides
     *  the blueprint, and it is in a fastlock word as encoded here.
     *  These methods are only needed for a model that will support moving MM,
     *  and B_Mf by itself will not (no place for hash). Still, I thought I'd
     *  implement the rewriters here and be done with it.
     * @param bpOffset fixed offset of the word dedicated to the blueprint
     * @param offset0 fixed offset of the fastlock word
     * @throws BCdead <em>this is a pragma</em>
     */
    protected static void initUpdate1( final char bpOffset, final char offset0)
    throws BCdead {
        rewrite( Oop.WithUpdate.ID, "updateBlueprint:(Lovm/core/domain/Blueprint;)V",
                 new Rewriter() {
                     protected boolean rewrite() {
                         cursor.addQuickOpcode(PUTFIELD_QUICK, bpOffset);
                         return true;
                     }
                 });
        rewrite( "updateReference:(ILovm/core/domain/Oop;)V",
                 new Rewriter() {
                     protected boolean rewrite() {           // oop k val
                         cursor.addSimpleInstruction( SWAP); // oop val k
                         cursor.addSimpleInstruction( POP);  // oop val
			 cursor.addSimpleInstruction( SWAP); // val oop
			 cursor.addSimpleInstruction( DUP_X1);// oop val oop
			 cursor.addQuickOpcode(GETFIELD_QUICK, offset0);
			 CountBits.bf.effect(this, false, true, BfRewriter.INT);
			 cursor.addSimpleInstruction(IADD);
                         cursor.addQuickOpcode(PUTFIELD_QUICK, offset0);
                         return true;
                     }
                 });
    }
    
    protected static void initFastLockableModel( final int maxRC) {
        rewrite( FastLockable.Model.ID, "maxRecursionCount:()I",
            new Rewriter() {
                protected boolean rewrite() {
		    cursor.addSimpleInstruction( POP);
                    cursor.addLoadConstant( maxRC);
                    return true; // delete original instruction
                }
            });
    }
}
