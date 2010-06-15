package s3.core.domain;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.execution.Context;
import ovm.core.services.memory.MovingGC;
import ovm.core.services.memory.VM_Address;
import ovm.services.monitors.FastLockable;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.util.OVMError;
import ovm.util.UnsafeAccess;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.GCBits;
import s3.util.PragmaTransformCallsiteIR.Rewriter;
import s3.util.PragmaNoPollcheck;

public class S3Model_Bg_Mf_F_H extends ObjectModel
    implements FastLockable.Model, GCBits.Model
{
    private static final char BP_OFFSET = 0;
    private static final char MONITOR_OFFSET = 4;
    private static final char FORWARD_OFFSET = 8;
    private static final char HASH_OFFSET = 12;

    /**
     * If true, assign hash codes sequentially, rather than based on
     * the object's initial address.
     **/
    private static final boolean SEQ_HASH = true;
    private static int nextHash = 0;
    
    // the methods in this class should not be called directly (i.e. with
    // this class specified at the call site) but rather on the appropriate
    // interfaces or ObjectModel, where the pragmas giving the correct
    // runtime behavior are specified.  To catch perverse attempts to call
    // directly on this class, the methods here will all throw BCdead.
    
    public VM_Address newInstance() throws BCdead { return new Proxy(); }
    
    public int headerSkipBytes() throws BCdead { return 16; }
    
    public int identityHash( Object o) throws BCdead
    { return System.identityHashCode(o); 
    }
    
    // monitor counts. blueprint doesn't (reachable through domain).
    // forward address doesn't (to-space copy is not yet in danger of collection!)
    public int maxReferences() throws BCdead { return 1; }
    
    public Oop stamp( VM_Address a, Blueprint bp) {
      	a.add(BP_OFFSET).setAddress(VM_Address.fromObject( bp));
	//a.add(FORWARD_OFFSET).setInt(0); // Mark as not forwarded
	a.add(HASH_OFFSET).setInt(SEQ_HASH ? nextHash++ : a.asInt());
	return super.stamp(a, bp);
    }
    
    /**Returns a string with a description of the header data structure in C.**/
    public String toCStruct() {
        return ("struct HEADER {\n"+
		"\tjint _blueprintGC_;\n" +
                "\tjint _fastlock_;\n" +
		"\tstruct ovm_core_services_memory_MovingGC* _forward_;\n" +
		"\tjint _hashcode_;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"((struct s3_core_domain_S3Blueprint*)(((struct HEADER*)(H))->_blueprintGC_&~3))\n" +
		"#define HEADER_HASHCODE(H)  " +
		"(((struct HEADER*)(H))->_hashcode_)\n");
    }
    public String toCxxStruct() {
	return ("struct HEADER {\n"+
		"\tjint blueprintGC;\n"+
		"\tjint fastlock;\n" +
		"\tstruct e_java_lang_Object* forward;\n" +
		"\tjint hashcode;\n" +
		"};\n"+
		"#define HEADER_BLUEPRINT(H) " +
		"((struct s3_core_domain_S3Blueprint*)(((struct HEADER*)(H))->blueprintGC&~3))\n" +
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
	implements Oop.WithUpdate, MonitorMapper, MovingGC, FastLockable,
		   UnsafeAccess, GCBits
    {
        public Monitor getMonitor() {
	    throw new OVMError.Unimplemented( "getMonitor at build time");
	}

	public void setMonitor(Monitor monitor) {
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

	public int getGCBits() {
	    throw new OVMError.Unimplemented( "getGCBits at build time");
	}
	
	public void setGCBits(int bits) {
	    throw new OVMError.Unimplemented( "setGCBits at build time");
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

    protected void init() throws BCdead {
        S3Model_Bg_Mf.initOop( BP_OFFSET);
        S3Model_B_Mf.initGetReference1( MONITOR_OFFSET);
        S3Model_B_Mf.initMonitorMapper( MONITOR_OFFSET);
        S3Model_B_Mf.initFastLockable( MONITOR_OFFSET);
        S3Model_Bg_Mf.initUpdate1( BP_OFFSET, MONITOR_OFFSET);
        S3Model_B_M_F_H.initMovingGC( FORWARD_OFFSET, HASH_OFFSET);
        S3Model_B_Mf.initFastLockableModel( maxRecursionCount());
	S3Model_Bg_Mf.initGCBits(BP_OFFSET);
    }

    public int getGCBits(VM_Address slot) throws PragmaNoPollcheck {
	return slot.getInt()&3;
    }
    
    public void setGCBits(VM_Address slot,int bits) throws PragmaNoPollcheck {
	slot.setInt((slot.getInt()&~3)|bits);
    }
}    
