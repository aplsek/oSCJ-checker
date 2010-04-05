
// FIXME: check before using this with anything else than RTGC (TheMan)

package s3.core.domain;

import ovm.services.bytecode.editor.Marker;
import ovm.core.repository.Selector;
import ovm.core.repository.RepositoryUtils;
import ovm.core.domain.Blueprint;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.execution.Context;
import ovm.services.memory.scopes.ScopePointer;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.memory.VM_Word.Bitfield;
import ovm.core.services.memory.VM_Word.BfRewriter;
import ovm.core.services.memory.MovingGC;
import ovm.core.services.memory.VM_Address;
import ovm.services.monitors.FastLockable;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.services.monitors.MonitorMapperNB;
import ovm.util.OVMError;
import ovm.util.UnsafeAccess;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.GCData;
import s3.util.PragmaTransformCallsiteIR.Rewriter;
import s3.util.PragmaNoPollcheck;
import s3.util.PragmaInline;
import s3.util.PragmaAssertNoExceptions;
import ovm.core.services.memory.PragmaNoReadBarriers;
import ovm.core.execution.Native;
import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.MemoryManager;

public class S3Model_Bg_Mf_F_H_S extends ObjectModel
    implements FastLockable.Model, GCData.Model, ScopePointer.Model
{
    private static final char BP_OFFSET = 0;
    private static final char MONITOR_OFFSET = 4;
    private static final char FORWARD_OFFSET = 8;
    private static final char SCOPE_OFFSET = 12;
    private static final char HASH_OFFSET = 16;

      // warning !! the constant is hardcoded in TheMan
    private static final int INFLATED = 3; // 0..00011

    public int getUnforwardedSemantics() {
      return ObjectModel.FWD_SELF;
    }
    
    public int getForwardOffset() {
      return FORWARD_OFFSET;    
    }

    public int getMonitorOffset() {
      return MONITOR_OFFSET;    
    }

    /**
     * If true, assign hash codes sequentially, rather than based on
     * the object's initial address.
     **/
    private static final boolean SEQ_HASH = true;
    private static int nextHash = 0;
    
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
    
    
    
    // the methods in this class should not be called directly (i.e. with
    // this class specified at the call site) but rather on the appropriate
    // interfaces or ObjectModel, where the pragmas giving the correct
    // runtime behavior are specified.  To catch perverse attempts to call
    // directly on this class, the methods here will all throw BCdead.
    
    public VM_Address newInstance() throws BCdead { return new Proxy(); }
    
    public int headerSkipBytes() throws BCdead { return 20; }
    
    public int identityHash( Object o) throws BCdead
    { return System.identityHashCode(o); 
    }
    
    // monitor now doesn't count. blueprint doesn't (reachable through domain).
    // forward address doesn't (to-space copy is not yet in danger of collection!)
    public int maxReferences() throws BCdead { 
      //return 2; 
      return 1;
    }
    
    public Oop stamp( VM_Address a, Blueprint bp) throws PragmaNoReadBarriers {
    
      // WARNING WARNING WARNING: this does not set the scope pointer.
      // this means that objects allocated in the image have a null
      // scope pointer.  this means that your areaOf() machinery
      // must check for this.
  
        // we don't need translation here, blueprints don't move
      	a.add(BP_OFFSET).setAddress(VM_Address.fromObjectNB( bp));
	//a.add(FORWARD_OFFSET).setInt(0); // Mark as not forwarded
	
	if ( MemoryManager.the().needsReplicatingTranslatingBarrier() || MemoryManager.the().needsBrooksTranslatingBarrier()) {
  	  a.add(FORWARD_OFFSET).setAddress(a); // isn't it too late here ?
        }
	a.add(HASH_OFFSET).setInt(SEQ_HASH ? nextHash++ : a.asInt());
	return super.stamp(a, bp);
    }
    
    /**Returns a string with a description of the header data structure in C.**/
    public String toCStruct() {
        return ("struct HEADER {\n"+
		"\tjint _blueprintGC_;\n" +
                "\tjint _fastlock_;\n" +
		"\tstruct ovm_core_services_memory_MovingGC* _forward_;\n" +
		"\tvoid *_scope_;\n" +
		"\tjint _hashcode_;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"((struct s3_core_domain_S3Blueprint*)(((struct HEADER*)(H))->_blueprintGC_&~7))\n" +
		"#define HEADER_HASHCODE(H)  " +
		"(((struct HEADER*)(H))->_hashcode_)\n");
    }
    public String toCxxStruct() {
	return ("struct HEADER {\n"+
		"\tjint blueprintGC;\n"+
		"\tjint fastlock;\n" +
		"\tstruct e_java_lang_Object* forward;\n" +
		"\tvoid *scope;\n" +
		"\tjint hashcode;\n" +
		"};\n"+
		"#define HEADER_BLUEPRINT(H) " +
		"((struct s3_core_domain_S3Blueprint*)(((struct HEADER*)(H))->blueprintGC&~7))\n" +
		"#define HEADER_HASHCODE(H)  " +
		"(((struct HEADER*)(H))->hashcode)\n");    }

    public byte[] x86_getBlueprint(byte objectRegister,
				   byte blueprintRegister) {
	throw new OVMError.Unimplemented("unimplemented");
    }

    public int[] ppc_getBlueprint(int blueprintRegister, int objectRegister) {
	throw new OVMError.Unimplemented("unimplemented");
    }
    
    private static class Proxy extends ObjectModel.Proxy
	implements Oop.WithUpdate, MonitorMapper, MonitorMapperNB, MovingGC, FastLockable,
		   UnsafeAccess, GCData, ScopePointer
    {
        public VM_Area getScopePointer() {
            throw new OVMError.Unimplemented("getScopePointer at build time");
        }
                
        public void setScopePointer(VM_Area a) {
            throw new OVMError.Unimplemented("setScopePointer at build time");
        }

        public Monitor getMonitor() {
	    throw new OVMError.Unimplemented( "getMonitor at build time");
	}

	public void setMonitor(Monitor monitor) {
	    throw new OVMError.Unimplemented( "setMonitor at build time");
	}

	public void setMonitorNB(Monitor monitor) {
	    throw new OVMError.Unimplemented( "setMonitor at build time");
	}
	
	public void releaseMonitor() {
	    throw new OVMError.Unimplemented( "releaseMonitor at build time");
	}
	
	public void markAsForwarded(VM_Address fwdaddr){
	    throw new OVMError.Unimplemented( "setForwardAddress at build time");
	}
	
	public boolean isForwarded(){
	    throw new OVMError.Unimplemented( "isForwarded at build time");
	}
	
	public VM_Address getForwardAddress(){
	    throw new OVMError.Unimplemented( "getForwardAddress at build time");
	}
    
        public void updateReference( int k, Oop newValue){
            throw new OVMError.Unimplemented( "updateReference at build time");
        }
        
        public void updateBlueprint( Blueprint newValue){
            throw new OVMError.Unimplemented( "updateBlueprint at build time");
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

	public int getColor() {
	    throw new OVMError.Unimplemented( "getColor at build time");
	}
	
	public void setColor(int color) {
	    throw new OVMError.Unimplemented( "setColor at build time");
	}
	
	public int getOld() {
	    throw new OVMError.Unimplemented( "getOld at build time");	
	}
	
	public void markOld() {
	    throw new OVMError.Unimplemented( "markOld at build time");		
	}
    }

    // the methods in this class should not be called directly (i.e. with
    // this class specified at the call site) but rather on the appropriate
    // interfaces or ObjectModel, where the pragmas giving the correct
    // runtime behavior are specified.  To catch perverse attempts to call
    // directly on this class, the methods here will all throw BCdead.
    
    public int maxRecursionCount() throws BCdead {
	return alignment() - 2;
    }


    /** Register rewriters for MovingGC methods (except updateBlueprint and
     *  updateReference); usable in any model where the forwarding address gets
     *  a word of its own at a fixed offset.
     * @param forwardOffset fixed offset from object reference of word containing
     * forward address
     * @throws BCdead <em>this is a pragma</em>
     **/
    protected static void initMovingGC( final char forwardOffset,
					final char hashOffset) throws BCdead {

        final Selector.Method vane = // VM_Address.NE(VM_Address other)
            RepositoryUtils.methodSelectorFor(
                "Lovm/core/services/memory/VM_Address;",
                "NE:(Lovm/core/services/memory/VM_Address;)Z");
          


        rewrite(Oop.ID, "getHash:()I",
            new Rewriter() {
                protected boolean rewrite() { // warning !! - intentionally without barrier
                    cursor.addQuickOpcode(GETFIELD_QUICK, hashOffset);
		    addFiat();
		    return true;
                }
            });
	rewrite(MovingGC.ID, "isForwarded:()Z", // FIXME: brooks only, not used by TheMan
		new Rewriter() {
		    protected boolean rewrite() { // warning !! - intentionally without barrier
			Marker nf = cursor.makeUnboundMarker();
			Marker end = cursor.makeUnboundMarker();
			cursor.addSimpleInstruction(DUP); // oop oop 
			cursor.addQuickOpcode(GETFIELD_QUICK, forwardOffset); // oop forwarded-oop
			cursor.addINVOKEVIRTUAL(vane); // oop != forwarded-oop
			return true; // delete original instruction
		    }
		});
	rewrite( "markAsForwarded:(Lovm/core/services/memory/VM_Address;)V", // FIXME: not used by TheMan, check for other
		 new Rewriter() {
		     protected boolean rewrite() { // warning !! - intentionally without barrier
			 cursor.addQuickOpcode(PUTFIELD_QUICK, forwardOffset);
			 return true;
		     }
		 });
	rewrite( "getForwardAddress:()Lovm/core/services/memory/VM_Address;", // this _is_ used by TheMan
		 new Rewriter() {
		     protected boolean rewrite() { // warning !! - intentionally without barrier
			 cursor.addQuickOpcode(GETFIELD_QUICK, forwardOffset);
			 return true;
		     }
		 });
    }
    
    
    /** Set up rewriters for MonitorMapper methods.
     * @throws BCdead <em>this is a pragma</em>
     */
    protected static void initMonitorMapper( final char fastlockOffset)
    throws BCdead {
        rewrite( MonitorMapperNB.ID, "setMonitorNB:(Lovm/services/monitors/Monitor;)V", //FIXME: not used, it actually does not work
                                                                                        //FIXME: fix for different barriers if needed
            new Rewriter() {
                protected boolean rewrite() { // oop mon
                    cursor.addLoadConstant( INFLATED); // oop mon INFLATED  
                    CountBits.bf.effect( this, true, true, BfRewriter.INT); //oop fastlock-value                                      
                    cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset); // <empty>
                    return true;
                }
            });
    
  	rewrite( MonitorMapper.ID, "releaseMonitor:()V", //FIXME: remove, not used, not fixed for barriers
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
                    cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);
		    cursor.addLoadConstant(0);
		    cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset);
                    return true; // delete original instruction
                }
            });

        rewrite( "getMonitor:()Lovm/services/monitors/Monitor;",
            new Rewriter() {
            /* unoptimized */
            
                protected boolean rewrite() {
                    Marker a = cursor.makeUnboundMarker();
                    Marker b = cursor.makeUnboundMarker();
	  
                                                       // oop
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // oop
                    }
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // fastlock-value
		    cursor.addSimpleInstruction( DUP); // fastlock-value fastlock-value
                    CountBits.bf.effect( this, false, true, BfRewriter.INT); // fastlock-value countbits
                    cursor.addLoadConstant( INFLATED); // fastlock-value countbits INFLATED
                    cursor.addIf( IF_ICMPNE, a); // fastlock-value
                    OwnerBits.bf.effect( this, false, false, BfRewriter.INT); // owner-bits
                    addFiat(); // mon
                    cursor.addGoto( b);
                a:  cursor.bindMarker( a);             // fastlock-value
                    cursor.addSimpleInstruction( POP); // 
		    cursor.addSimpleInstruction( ACONST_NULL); // null 
                b:  cursor.bindMarker( b);
                    return true;
                }
            });
          
        rewrite( "setMonitor:(Lovm/services/monitors/Monitor;)V",
            new Rewriter() {
                protected boolean rewrite() { // oop mon
                    /*
                    cursor.addSimpleInstruction(SWAP); // mon oop
                    
                    if (MemoryManager.the().needsReplicatingTranslatingBarrier()) {
                      cursor.addSimpleInstruction(DUP_X1); // ?oop mon oop
                    }
                    
                    cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // ?oop mon translated-oop
                    cursor.addSimpleInstruction(SWAP); // ?oop t-oop mon
                    cursor.addLoadConstant( INFLATED); // ?oop t-oop mon INFLATED
                    CountBits.bf.effect( this, true, true, BfRewriter.INT); //?oop t-oop fastlock-value                                  
                    
                    if (MemoryManager.the().needsReplicatingTranslatingBarrier()) {
                      cursor.addSimpleInstruction(DUP_X1); // ?oop ?fastlock-value t-oop fastlock-value    
                      cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset); // ?oop ?fastlock-value
                    }
                    
                    cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset); // <empty>
                    
                    return true; // delete original instruction
                    */
                    
                    
                    // oop mon
                    // we don't have forwarding on mon because we know it is not moved when this is called
                    // (yes, it is hacky..)
                    cursor.addLoadConstant( INFLATED); // oop mon inflated
                    CountBits.bf.effect( this, true, true, BfRewriter.INT); // oop fastlock-value
                    
                    if (MemoryManager.the().needsReplicatingTranslatingBarrier()) {
                      cursor.addSimpleInstruction(DUP2); // oop fastlock-value oop fastlock-value
                      cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset);  // oop fastlock-value
                    }
                    
                    if (MemoryManager.the().needsReplicatingTranslatingBarrier() || MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(SWAP); 
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); 
                      cursor.addSimpleInstruction(SWAP); // oop fastlock-value
                    }
                    cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset); 
                    
                    return true; // delete original instruction
                }
            });
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
        final Selector.Field cnch = // Lovm/core/execution/Context;.nativeContextHandle_:I
            RepositoryUtils.fieldSelectorFor(
              "Lovm/core/execution/Context;",
              "nativeContextHandle_:I");

        rewrite( FastLockable.ID, "fastLock:()Z",
            new Rewriter() {
                protected boolean rewrite() {
                    Marker end = cursor.makeUnboundMarker();
		    Marker fail = cursor.makeUnboundMarker();

                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // oop
                    }
		    cursor.addSimpleInstruction(DUP); // oop oop
		    cursor.addQuickOpcode(GETFIELD_QUICK, fastlockOffset); // oop fastlock-value
		    cursor.addIf(IFNE,fail); // oop
		    
		    cursor.addLoadConstant(0); // oop 0
                    cursor.addINVOKESYSTEM((byte) SYS.GET_NATIVE_CONTEXT); // oop context

                    if (MemoryManager.the().needsReplicatingTranslatingBarrier()) {
                      // oop context
                      cursor.addSimpleInstruction(DUP2); // oop context oop context
                      cursor.addSimpleInstruction(SWAP); // oop context context oop
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // oop context context t-oop
                      cursor.addSimpleInstruction(SWAP); // oop context t-oop context
		    
                      cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset); // oop context                      
                    }
                    
                    // we don't care about the old value as it is null
                    cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset); // <empty>
		    cursor.addLoadConstant(1); // 1
		    cursor.addGoto(end);
		    
		    cursor.bindMarker( fail); // oop
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

		    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
    		      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);
                    }
		    cursor.addSimpleInstruction(DUP); // oop oop
		    cursor.addQuickOpcode(GETFIELD_QUICK, fastlockOffset); // oop fastlock-value
		      
                    // oop fastlock-value
                    CountBits.bf.effect( this, false, false, BfRewriter.INT); // oop countbits
		    cursor.addIf(IFNE,fail); // oop

                    if (MemoryManager.the().needsReplicatingTranslatingBarrier()) {
  		      cursor.addSimpleInstruction(DUP); // oop oop
  		      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // oop t-oop
  		      cursor.addLoadConstant(0); // oop t-oop 0
  		      cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset); // oop
                    }

                    cursor.addLoadConstant(0); // oop  0                      
                    cursor.addQuickOpcode(PUTFIELD_QUICK, fastlockOffset); // <empty>
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
                protected boolean rewrite() {  // oop
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // oop
                    }
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // fastlock-value
                    OwnerBits.bf.effect( this, false, false, BfRewriter.INT); // owner
                    // does the needed nullcheck... 
                    cursor.addINVOKESYSTEM((byte) SYS.NATIVE_CONTEXT_TO_CONTEXT); // owner as context
                    

                    // this is at least theoretically needed because context pointers stored in native structures
                    // are handled like stack, not heap 
                    // although the caller would not be able to use the value without forwarding, it will get unforwarded 
                    // to the caller's stack - which can break updating of references on the stack
                    //	(first a thread's stack is updated, then native contexts are fixed...)
                    // maybe it is a philosophical bug... but I've spent too many weeks looking for a bug to leave this open

                    // FIXME: this does not update with replication
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) { // context
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // context
                    }
                    
                    addFiat();
                    return true; // delete original instruction
                }
            });
        rewrite( "getRecursionCount:()I",
            new Rewriter() {
                protected boolean rewrite() {
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // oop
                    }
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // fastlock-value
                    CountBits.bf.effect( this, false, true, BfRewriter.INT);  // countbits
                    return true; // delete original instruction
                }
            });
        rewrite( "isFastLocked:()Z",
            new Rewriter() {
                // compute owner-field-nonzero AND count-field-not-INFLATED
                // this implementation does not branch
                protected boolean rewrite() {                    
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);
                    }
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // w
                    cursor.addSimpleInstruction( DUP); // w w
                    OwnerBits.bf.effect( this, false, false, BfRewriter.INT); 
                      // we don't need a barrier on owner here, because we only care if it
                      // is non-null, not about it's location if it is non-null
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
        rewrite( "isMine:()Z", //( is it my fastlock ?)
            // as long as a Context and a Monitor can't be the same object, a
            // simple comparison here suffices.
            new Rewriter() {
                protected boolean rewrite() { // oop
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // oop
                    }
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // fastlock-value
                    OwnerBits.bf.effect( this, false, false, BfRewriter.INT); // owner
		    cursor.addLoadConstant( 0); // owner 0
                    cursor.addINVOKESYSTEM((byte)SYS.GET_NATIVE_CONTEXT); // owner context
                    cursor.addSimpleInstruction( IXOR); // owner_xor_context 
                    addTrailingIntEq0();
                    return true; // delete original instruction
                }
            });

        rewrite( "isInflated:()Z",
            new Rewriter() {
                protected boolean rewrite() { // oop
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER); // oop
                    }
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // fastlock
                    CountBits.bf.effect( this, false, true, BfRewriter.INT); // countbits
                    cursor.addLoadConstant( INFLATED); // countbits inflated
                    cursor.addSimpleInstruction( IXOR); // countbits_xor_inflated
                    addTrailingIntEq0();
                    return true; // delete original instruction
                }
            });
        
        rewrite( "isFastUnlocked:()Z",
            new Rewriter() {
                protected boolean rewrite() {
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);                
                    }
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset);
                    addTrailingIntEq0();
                    return true; // delete original instruction
                }
            });
        rewrite( "setOwner:(Lovm/core/execution/Context;)V",
            new Rewriter() {
                protected boolean rewrite() { // oop ctx
                    cursor.addGETFIELD(cnch); // oop (native)ctx
                    cursor.addSimpleInstruction( SWAP); // ctx oop
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);
                    }
                    cursor.addSimpleInstruction( DUP_X1); // oop ctx oop
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // oop ctx w
                    cursor.addSimpleInstruction( SWAP); // oop w ctx
                    OwnerBits.bf.effect( this, true, false, BfRewriter.INT); // oop fastlock-value
                    
                    if (MemoryManager.the().needsReplicatingTranslatingBarrier()) {
                      cursor.addSimpleInstruction(DUP2); // oop fastlock-value oop fastlock-value
                      cursor.addSimpleInstruction(SWAP);
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);
                      cursor.addSimpleInstruction(SWAP); // oop fastlock-value t-oop fastlock-value
                      cursor.addQuickOpcode( PUTFIELD_QUICK, fastlockOffset); 
                    }
                    
                    cursor.addQuickOpcode( PUTFIELD_QUICK, fastlockOffset); 
                    return true; // delete original instruction
                }
            });
        rewrite( "setRecursionCount:(I)V",
            new Rewriter() {
                protected boolean rewrite() { // oop cnt
                    cursor.addSimpleInstruction( SWAP); // cnt oop
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);                
                    }
                    cursor.addSimpleInstruction( DUP_X1); // oop cnt oop
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // oop cnt w
                    cursor.addSimpleInstruction( SWAP); // oop w cnt
                    CountBits.bf.effect( this, true, true, BfRewriter.INT);//oop w
                    
                    if (MemoryManager.the().needsReplicatingTranslatingBarrier()) {
                      cursor.addSimpleInstruction(DUP2); //oop w oop w
                      cursor.addSimpleInstruction(SWAP);
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);
                      cursor.addSimpleInstruction(SWAP); // oop w t-oop w
                      cursor.addQuickOpcode( PUTFIELD_QUICK, fastlockOffset);
                    }
                    
                    cursor.addQuickOpcode( PUTFIELD_QUICK, fastlockOffset);
                    return true; // delete original instruction
                }
            });
    }
    
    protected static void initGetReferenceSimple( final char scopeOffset)
    throws BCdead {
        rewrite( Oop.ID, "getReference:(I)Lovm/core/domain/Oop;",
            new Rewriter() {
                protected boolean rewrite() { // oop k
                    cursor.addSimpleInstruction( POP); // oop
                    cursor.addQuickOpcode( GETFIELD_QUICK, scopeOffset); // w
		    addFiat();
		    return true;
		}
	    });
    }

    protected static void initUpdateSimple( final char bpOffset, final char scopeOffset)
    throws BCdead {
        // ever called ?
        // if so, it is called knowing that it only writes to one copy of the object ?
        
        rewrite( Oop.WithUpdate.ID, "updateBlueprint:(Lovm/core/domain/Blueprint;)V",
                 new Rewriter() {
                     protected boolean rewrite() {
			 // oop bp
			 cursor.addSimpleInstruction(SWAP); // bp oop
			 cursor.addSimpleInstruction(DUP); // bp oop oop
			 cursor.addQuickOpcode(GETFIELD_QUICK, bpOffset); // bp oop oldBP
			 cursor.addLoadConstant(7); // bp oop oldBP 7
			 cursor.addSimpleInstruction(IAND); // bp oop gcbits
			 cursor.addRoll((char)3,(byte)-1); // oop gcbits bp
			 cursor.addSimpleInstruction(IOR); // oop bpgc
                         cursor.addQuickOpcode(PUTFIELD_QUICK, bpOffset);
                         return true;
                     }
                 });
                 
                 
        rewrite( "updateReference:(ILovm/core/domain/Oop;)V",
                 new Rewriter() {
                     protected boolean rewrite() {           // oop k val
                         cursor.addSimpleInstruction( SWAP); // oop val k
                         cursor.addSimpleInstruction( POP);  // oop val
                         cursor.addQuickOpcode(PUTFIELD_QUICK, scopeOffset);
                         return true;
                     }
                 });
    }


    protected static void initOop(final char bpOffset) throws BCdead {
        rewrite( Oop.ID, "getBlueprint:()Lovm/core/domain/Blueprint;",
            new Rewriter() {
                protected boolean rewrite() {
//                    cursor.addQuickOpcode( REF_GETFIELD_QUICK, bpOffset); ??
                    cursor.addQuickOpcode( GETFIELD_QUICK, bpOffset);
		    cursor.addLoadConstant(8);
		    cursor.addSimpleInstruction(INEG);
		    cursor.addSimpleInstruction(IAND);
		    addFiat();
                    return true; // delete original instruction
                }
            });
    }

    protected static void initGCBits(final char bpOffset) throws BCdead { 
      // no barriers here, the collector must take care of that
      
	rewrite(GCData.ID, "getColor:()I",
		new Rewriter() {
		    protected boolean rewrite() {
			cursor.addQuickOpcode(GETFIELD_QUICK, bpOffset);
			cursor.addSimpleInstruction(ICONST_3);
			cursor.addSimpleInstruction(IAND);
			addFiat();
			return true;
		    }
		});
	rewrite(GCData.ID, "setColor:(I)V",
		new Rewriter() {
		    protected boolean rewrite() {
			// stack: oop, arg
			cursor.addSimpleInstruction(SWAP);
			// stack: arg, oop
			cursor.addSimpleInstruction(DUP);
			// stack: arg, oop, oop
			cursor.addQuickOpcode(GETFIELD_QUICK, bpOffset);
			// stack: arg, oop, bp|(oldgcbits)
			cursor.addSimpleInstruction(ICONST_4);
			cursor.addSimpleInstruction(INEG);
			cursor.addSimpleInstruction(IAND);
			// stack: arg, oop, bp
			cursor.addRoll((char)3,(byte)-1);
			// stack: oop, bp, arg
			cursor.addSimpleInstruction(IOR);
			// stack: oop, bp|arg
			cursor.addQuickOpcode(PUTFIELD_QUICK, bpOffset);
			return true;
		    }
		});
	rewrite(GCData.ID, "getOld:()I",
		new Rewriter() {
		    protected boolean rewrite() {
			cursor.addQuickOpcode(GETFIELD_QUICK, bpOffset);
			cursor.addSimpleInstruction(ICONST_4);
			cursor.addSimpleInstruction(IAND);
			addFiat();
			return true;
		    }
		});
	rewrite(GCData.ID, "markOld:()V",
		new Rewriter() {
		    protected boolean rewrite() {
			// stack: oop
			cursor.addSimpleInstruction(DUP);
			// stack: oop, oop
			cursor.addQuickOpcode(GETFIELD_QUICK, bpOffset);
			// stack: oop, bp|(oldgcbits)
			cursor.addSimpleInstruction(ICONST_4);
			cursor.addSimpleInstruction(IOR);
			cursor.addQuickOpcode(PUTFIELD_QUICK, bpOffset);
			return true;
		    }
		});		
    }

    protected static void initScopePointer(final char spOffset) {
        rewrite( ScopePointer.ID, "getScopePointer:()Lovm/core/services/memory/VM_Area;",
            new Rewriter() {
                protected boolean rewrite() {
                    // oop
                    if (MemoryManager.the().needsBrooksTranslatingBarrier()) {
                      cursor.addSimpleInstruction(NONCHECKING_TRANSLATING_READ_BARRIER);
                    } 
                    //cursor.addQuickOpcode( REF_GETFIELD_QUICK, spOffset);
                    cursor.addQuickOpcode( GETFIELD_QUICK, spOffset);
		    addFiat();
                    return true; // delete original instruction
                }
            });
        rewrite( "setScopePointer:(Lovm/core/services/memory/VM_Area;)V",
            new Rewriter() {
                protected boolean rewrite() {

                    // oop scope-pointer
                    cursor.addQuickOpcode(PUTFIELD_QUICK_WITH_BARRIER_REF, spOffset);
                    
                    return true; // delete original instruction
                }
            });
    }

    protected void init() throws BCdead {
        initOop( BP_OFFSET );
        initGetReferenceSimple(SCOPE_OFFSET);
        initMonitorMapper( MONITOR_OFFSET);
        initFastLockable( MONITOR_OFFSET);
        initUpdateSimple( BP_OFFSET, SCOPE_OFFSET);
        initMovingGC( FORWARD_OFFSET, HASH_OFFSET );
        S3Model_B_Mf.initFastLockableModel( maxRecursionCount());
        initGCBits(BP_OFFSET); //?? why do we have both the rewriting and the functions below ?
        
	initScopePointer(SCOPE_OFFSET);
    }

    public int getColor(VM_Address slot) throws PragmaNoPollcheck, PragmaAssertNoExceptions, PragmaInline {
	return slot.getInt()&3;
    }
    
    public void setColor(VM_Address slot,int color) throws PragmaNoPollcheck, PragmaAssertNoExceptions, PragmaInline {
	slot.setInt((slot.getInt()&~3)|color);
    }

    public int getOld(VM_Address slot) throws PragmaNoPollcheck, PragmaAssertNoExceptions, PragmaInline {
	return slot.getInt()&4;
    }

    public void markOld(VM_Address slot) throws PragmaNoPollcheck, PragmaAssertNoExceptions, PragmaInline {
      slot.setInt(slot.getInt()|4);
    }
    
    public VM_Address getPrev(VM_Address slot) throws PragmaNoPollcheck, PragmaAssertNoExceptions, PragmaInline {
	return VM_Address.fromInt(slot.getInt()&~7); // we could let it ignore the old bit, but we don't care
    }
    
    public void setPrev(VM_Address slot,VM_Address prev) throws PragmaNoPollcheck, PragmaAssertNoExceptions, PragmaInline {
	slot.setInt((slot.getInt()&7)|prev.asInt()); // we could let it clear the old bit, but we don't care
    }
}    
