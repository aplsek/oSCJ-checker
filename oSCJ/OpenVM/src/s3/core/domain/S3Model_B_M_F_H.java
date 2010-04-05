package s3.core.domain;

import ovm.core.repository.Selector;
import ovm.core.domain.Blueprint;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.repository.RepositoryUtils;
import ovm.core.services.memory.MovingGC;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.editor.Marker;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.util.OVMError;
import ovm.util.UnsafeAccess;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.util.PragmaTransformCallsiteIR.Rewriter;

/** Describes an object layout with one 32word for blueprint,
 *  one 32word for monitor, one 32word for forwarding information and
 *  one 32word for a hash value
 *  @author Phil McGachey
 **/
public class S3Model_B_M_F_H extends ObjectModel {

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
	// a.add(FORWARD_OFFSET).setInt(0); // Mark as not forwarded
	a.add(HASH_OFFSET).setInt(SEQ_HASH ? nextHash++ : a.asInt());
	return super.stamp(a, bp);
    }
    
    /**Returns a string with a description of the header data structure in C.**/
    public String toCStruct() {
        return ("struct HEADER {\n"+
		"\tstruct s3_core_domain_S3Blueprint *_blueprint_;\n" +
                "\tstruct ovm_services_monitors_Monitor *_monitor_;\n" +
		"\tstruct ovm_core_services_memory_MovingGC* _forward_;\n" +
		"\tjint _hashcode_;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"(((struct HEADER*)(H))->_blueprint_)\n" +
		"#define HEADER_HASHCODE(H)  " +
		"(((struct HEADER*)(H))->_hashcode_)\n");
    }
    public String toCxxStruct() {
	return ("struct HEADER {\n"+
		"\tstruct e_s3_core_domain_S3Blueprint *blueprint;\n"+
		"\tstruct e_java_lang_Object *monitor;\n"+
		"\tstruct e_java_lang_Object* forward;\n" +
		"\tjint hashcode;\n" +
		"};\n"+
		"#define HEADER_BLUEPRINT(H) " +
		"(((struct HEADER*)(H))->blueprint)\n" +
		"#define HEADER_HASHCODE(H)  " +
		"(((struct HEADER*)(H))->hashcode)\n");    }

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
    
    private static class Proxy
    extends ObjectModel.Proxy
    implements Oop.WithUpdate, MonitorMapper, MovingGC, UnsafeAccess {
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
    }

    protected void init() throws BCdead {
        S3Model_B_M.initOop( BP_OFFSET);
        S3Model_B_M.initGetReference1( MONITOR_OFFSET);
        initMovingGC( FORWARD_OFFSET, HASH_OFFSET);
        initUpdate1( BP_OFFSET, MONITOR_OFFSET);
    }

    /** Initialize monitor support, which depends on threading config. **/
    public void initialize() {
        S3Model_B_M.initMonitorMapper( MONITOR_OFFSET);
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
        final Selector.Method vann = // VM_Address.isNonNull()
            RepositoryUtils.methodSelectorFor(
                "Lovm/core/services/memory/VM_Address;",
                "isNonNull:()Z");
        rewrite(Oop.ID, "getHash:()I",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode(GETFIELD_QUICK, hashOffset);
		    addFiat();
		    return true;
                }
            });
	rewrite(MovingGC.ID, "isForwarded:()Z",
		new Rewriter() {
		    protected boolean rewrite() {
			Marker nf = cursor.makeUnboundMarker();
			Marker end = cursor.makeUnboundMarker();
			cursor.addQuickOpcode(GETFIELD_QUICK, forwardOffset);
			cursor.addINVOKEVIRTUAL(vann);
			return true; // delete original instruction
		    }
		});
	rewrite( "markAsForwarded:(Lovm/core/services/memory/VM_Address;)V",
		 new Rewriter() {
		     protected boolean rewrite() {
			 cursor.addQuickOpcode(PUTFIELD_QUICK, forwardOffset);
			 return true;
		     }
		 });
	rewrite( "getForwardAddress:()Lovm/core/services/memory/VM_Address;",
		 new Rewriter() {
		     protected boolean rewrite() {
			 cursor.addQuickOpcode(GETFIELD_QUICK, forwardOffset);
			 return true;
		     }
		 });
    }
    
    /** Register rewriters for WithUpdate: updateBlueprint and updateReference(k)
     *  methods; usable in any model where there is exactly one reference besides
     *  the blueprint, and it and the blueprint each occupy a dedicated word at
     *  a fixed offset.
     * @param bpOffset fixed offset of the word dedicated to the blueprint
     * @param offset0 fixed offset of the word dedicated to the other reference
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
                         cursor.addQuickOpcode(PUTFIELD_QUICK, offset0);
                         return true;
                     }
                 });
    }
}
