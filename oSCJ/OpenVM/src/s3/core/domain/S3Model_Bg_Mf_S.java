package s3.core.domain;

import ovm.core.repository.Selector;
import ovm.core.domain.Blueprint;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.repository.RepositoryUtils;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.GCBits;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.services.bytecode.editor.Marker;
import ovm.services.monitors.FastLockable;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.util.OVMError;
import ovm.core.execution.Context;
import ovm.services.memory.scopes.ScopePointer;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.memory.VM_Word.BfRewriter;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.util.PragmaTransformCallsiteIR.Rewriter;
import s3.util.PragmaNoPollcheck;

/** Describes a retarded object layout with one 32word for the blueprint and
 *  two GC bits (pinned and marked), one 32word for a fastlock, and a 32word
 *  for a memory area pointer.
 *  @author Filip Pizlo
 */
public class S3Model_Bg_Mf_S
    extends ObjectModel
    implements FastLockable.Model, GCBits.Model, ScopePointer.Model {
    
    private static final char BP_OFFSET = 0;
    private static final char FASTLOCK_OFFSET = 4;
    private static final char SCOPE_OFFSET = 8;
    
    public VM_Address newInstance() throws BCdead { return new Proxy(); }
    
    public int headerSkipBytes() throws BCdead { return 12; }
    
    public int maxReferences() throws BCdead { return 2; }
    
    public int maxRecursionCount() throws BCdead { return alignment()-2; }

    public int identityHash(Object o) throws BCdead {
	return System.identityHashCode(o);
    }
    
    public Oop stamp(VM_Address a,Blueprint bp) {
	// WARNING WARNING WARNING: this does not set the scope pointer.
	// this means that objects allocated in the image have a null
	// scope pointer.  this means that your areaOf() machinery
	// must check for this.
      	a.add( BP_OFFSET).setAddress( VM_Address.fromObject( bp));
	return super.stamp(a,bp);
    }
    
    public String toCStruct() {
        return ("struct HEADER {\n"+
		"\tjint _blueprintGC_;\n"+
                "\tjint _fastlock_;\n" +
		"\tvoid *_scope_;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"((struct s3_core_domain_S3Blueprint*)(((struct HEADER*)(H))->_blueprintGC_&~3))\n" +
		"#define HEADER_HASHCODE(H)  ((jint) (H))\n");
    }
    
    public String toCxxStruct() {
        return ("struct HEADER {\n"+
		"\tjint blueprintGC;\n"+
                "\tjint fastlock;\n" +
		"\tvoid *scope;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"((struct s3_core_domain_S3Blueprint*)(((struct HEADER*)(H))->blueprintGC&~3))\n" +
		"#define HEADER_HASHCODE(H)  ((jint) (H))\n");		
    }
    
    public byte[] x86_getBlueprint(byte objectRegister,
				   byte blueprintRegister) {
	return new byte[] {
	    // mov 0(objectRegister) -> blueprintRegister
	    (byte)0x8B, (byte)(0 | (blueprintRegister << 3) | objectRegister ),
	    // and -4, blueprintRegister
	    (byte) 0x83, (byte) (0xe0 | blueprintRegister), (byte) -4
	};
    }
    
    public int[] ppc_getBlueprint(int objectRegister,
				  int blueprintRegister) {
	return new int[] {
	    // lwz blueprintRegister <- 0(objectRegister)
	    ((32 << 26) | (blueprintRegister << 21) | (objectRegister << 16) | (0 << 0)),
	    // rlwinm blueprintRegister, blueprintRegister, 0, 0, 29
	    ((21<<26) | (blueprintRegister<<21) | (blueprintRegister<<16) | (29 << 1))
	};
    }

    // I won't bother adding BCdeads to this private class, here
    private static class Proxy
    extends ObjectModel.Proxy
    implements MonitorMapper, FastLockable, GCBits, ScopePointer {
	public VM_Area getScopePointer() {
	    throw new OVMError.Unimplemented("getScopePointer at build time");
	}
	
	public void setScopePointer(VM_Area a) {
	    throw new OVMError.Unimplemented("setScopePointer at build time");
	}
	
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
	
	public int getGCBits() {
	    throw new OVMError.Unimplemented( "getGCBits at build time");
	}
	
	public void setGCBits(int bits) {
	    throw new OVMError.Unimplemented( "setGCBits at build time");
	}
    }
    
    protected static void initScopePointer(final char spOffset) {
        rewrite( ScopePointer.ID, "getScopePointer:()Lovm/core/services/memory/VM_Area;",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode( REF_GETFIELD_QUICK, spOffset);
		    addFiat();
                    return true; // delete original instruction
                }
            });
        rewrite( "setScopePointer:(Lovm/core/services/memory/VM_Area;)V",
            new Rewriter() {
                protected boolean rewrite() {
		    cursor.addQuickOpcode(PUTFIELD_QUICK, spOffset);
                    return true; // delete original instruction
                }
            });
    }
    
    protected static void initGetReference2(final char fastlockOffset,
					    final char scopeOffset)
	throws BCdead {
        rewrite( Oop.ID, "getReference:(I)Lovm/core/domain/Oop;",
            new Rewriter() {
                protected boolean rewrite() {
		    Marker one=cursor.makeUnboundMarker();
		    Marker done=cursor.makeUnboundMarker();
		    cursor.addIf(IFNE,one);
                    cursor.addQuickOpcode( GETFIELD_QUICK, fastlockOffset); // this can be both the Context and the Monitor
		    S3Model_B_Mf.OwnerBits.bf.effect( this, false, false, BfRewriter.INT);
		    cursor.addGoto(done);
		    cursor.bindMarker(one);
		    cursor.addQuickOpcode( GETFIELD_QUICK, scopeOffset);
		    cursor.bindMarker(done);
		    addFiat();
		    return true;
		}
	    });
    }
    
    protected void init() throws BCdead {
	S3Model_Bg_Mf.initOop(BP_OFFSET);
	S3Model_B_Mf.initFastLockable(FASTLOCK_OFFSET);
	S3Model_B_Mf.initMonitorMapper(FASTLOCK_OFFSET);
	initGetReference2(FASTLOCK_OFFSET,SCOPE_OFFSET);
	S3Model_B_Mf.initFastLockableModel(maxRecursionCount());
	S3Model_Bg_Mf.initGCBits(BP_OFFSET);
	initScopePointer(SCOPE_OFFSET);
    }
    
    public int getGCBits(VM_Address slot) throws PragmaNoPollcheck {
	return slot.getInt()&3;
    }
    
    public void setGCBits(VM_Address slot,int bits) throws PragmaNoPollcheck {
	slot.setInt((slot.getInt()&~3)|bits);
    }
    
    public VM_Address getPrev(VM_Address slot) {
	return VM_Address.fromInt(slot.getInt()&~3);
    }
    
    public void setPrev(VM_Address slot,VM_Address prev) {
	slot.setInt((slot.getInt()&3)|prev.asInt());
    }
}

