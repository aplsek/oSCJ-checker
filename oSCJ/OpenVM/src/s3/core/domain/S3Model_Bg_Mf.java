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
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.util.PragmaTransformCallsiteIR.Rewriter;
import s3.util.PragmaNoPollcheck;
import ovm.core.services.memory.VM_Word.BfRewriter;

/** Describes an object layout with one 32word for the blueprint and
 *  two GC bits (pinned and marked), and one 32word for a fastlock.
 *  @author Filip Pizlo
 */
public class S3Model_Bg_Mf
    extends ObjectModel
    implements FastLockable.Model, GCBits.Model {
    
    private static final char BP_OFFSET = 0;
    private static final char FASTLOCK_OFFSET = 4;
    
    public VM_Address newInstance() throws BCdead { return new Proxy(); }
    
    public int headerSkipBytes() throws BCdead { return 8; }
    
    public int maxReferences() throws BCdead { return 1; }
    
    public int maxRecursionCount() throws BCdead { return alignment()-2; }

    public int identityHash(Object o) throws BCdead {
	return System.identityHashCode(o);
    }
    
    public Oop stamp(VM_Address a,Blueprint bp) {
      	a.add( BP_OFFSET).setAddress( VM_Address.fromObject( bp));
	return super.stamp(a,bp);
    }
    
    public String toCStruct() {
        return ("struct HEADER {\n"+
		"\tjint _blueprintGC_;\n"+
                "\tjint _fastlock_;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"((struct s3_core_domain_S3Blueprint*)(((struct HEADER*)(H))->_blueprintGC_&~3))\n" +
		"#define HEADER_HASHCODE(H)  ((jint) (H))\n");
    }
    
    public String toCxxStruct() {
        return ("struct HEADER {\n"+
		"\tjint blueprintGC;\n"+
                "\tjint fastlock;\n" +
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

    public int[] ppc_getBlueprint(int blueprintRegister, int objectRegister) {
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
    implements MonitorMapper, FastLockable, GCBits {
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
    
    protected static void initOop(final char bpOffset) throws BCdead {
        rewrite( Oop.ID, "getBlueprint:()Lovm/core/domain/Blueprint;",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode( REF_GETFIELD_QUICK, bpOffset);
		    cursor.addSimpleInstruction(ICONST_4);
		    cursor.addSimpleInstruction(INEG);
		    cursor.addSimpleInstruction(IAND);
		    addFiat();
                    return true; // delete original instruction
                }
            });
        rewrite( "getHash:()I",
            new Rewriter() {
                protected boolean rewrite() {
		    addFiat();
                    return true; // delete original instruction
                }
            });
    }

    protected static void initUpdate1( final char bpOffset, final char offset0)
    throws BCdead {
        rewrite( Oop.WithUpdate.ID, "updateBlueprint:(Lovm/core/domain/Blueprint;)V",
                 new Rewriter() {
                     protected boolean rewrite() {
			 // oop bp
			 cursor.addSimpleInstruction(SWAP); // bp oop
			 cursor.addSimpleInstruction(DUP); // bp oop oop
			 cursor.addQuickOpcode(GETFIELD_QUICK, bpOffset); // bp oop oldBP
			 cursor.addSimpleInstruction(ICONST_3); // bp oop oldBP 3
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
			 cursor.addSimpleInstruction( SWAP); // val oop
			 cursor.addSimpleInstruction( DUP_X1);// oop val oop
			 cursor.addQuickOpcode(GETFIELD_QUICK, offset0);
			 S3Model_B_Mf.CountBits.bf.effect(this, false, true, BfRewriter.INT);
			 cursor.addSimpleInstruction(IADD);
                         cursor.addQuickOpcode(PUTFIELD_QUICK, offset0);
                         return true;
                     }
                 });
    }

    protected static void initGCBits(final char bpOffset) throws BCdead {
	rewrite(GCBits.ID, "getGCBits:()I",
		new Rewriter() {
		    protected boolean rewrite() {
			cursor.addQuickOpcode(GETFIELD_QUICK, bpOffset);
			cursor.addSimpleInstruction(ICONST_3);
			cursor.addSimpleInstruction(IAND);
			addFiat();
			return true;
		    }
		});
	rewrite(GCBits.ID, "setGCBits:(I)V",
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
    }
    
    protected void init() throws BCdead {
	initOop(BP_OFFSET);
	S3Model_B_Mf.initFastLockable(FASTLOCK_OFFSET);
	S3Model_B_Mf.initMonitorMapper(FASTLOCK_OFFSET);
	S3Model_B_Mf.initGetReference1(FASTLOCK_OFFSET);
	S3Model_B_Mf.initFastLockableModel(maxRecursionCount());
	initGCBits(BP_OFFSET);
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

