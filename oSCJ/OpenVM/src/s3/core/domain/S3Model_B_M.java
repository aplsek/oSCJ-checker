package s3.core.domain;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.services.memory.VM_Address;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.util.OVMError;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.util.PragmaTransformCallsiteIR.Rewriter;

/** Describes an object layout with one 32word for blueprint,
 *  one 32word for monitor, and hash==address (for nonmoving
 *  collectors only!).
 *  @author Chapman Flack
 **/
public class S3Model_B_M extends ObjectModel {

    private static final char BP_OFFSET = 0;
    private static final char MONITOR_OFFSET = 4;
    private static int depth = 0;
    
    // the methods in this class should not be called directly (i.e. with
    // this class specified at the call site) but rather on the appropriate
    // interfaces or ObjectModel, where the pragmas giving the correct
    // runtime behavior are specified.  To catch perverse attempts to call
    // directly on this class, the methods here will all throw BCdead.
    
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
                "\tstruct ovm_services_monitors_Monitor *_monitor_;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"(((struct HEADER*)(H))->_blueprint_)\n" +
		"#define HEADER_HASHCODE(H)  ((jint) (H))\n");
    }
    public String toCxxStruct() {
	return ("struct HEADER {\n"+
		"\tstruct e_s3_core_domain_S3Blueprint *blueprint;\n" +
		"\t//expand interface typedefs manually\n" +
		"\tstruct e_java_lang_Object *monitor;\n" +
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
    implements MonitorMapper {
    
	public Monitor getMonitor() {
	    throw new OVMError.Unimplemented( "getMonitor at build time");
	}

	public void setMonitor(Monitor monitor) {
	    throw new OVMError.Unimplemented( "setMonitor at build time");
	}
	
	public void releaseMonitor() {
	    throw new OVMError.Unimplemented( "releaseMonitor at build time");
	}
    }
    
    protected void init() throws BCdead {
        initOop( BP_OFFSET);
        initGetReference1( MONITOR_OFFSET);
    }

    /** Initialize monitor support, which depends on threading config. **/
    public void initialize() {
        initMonitorMapper( MONITOR_OFFSET);
    }

    /** Set up rewriters for Oop methods (except getReference); usable in any
     *  model where the hash is the address and the blueprint has a word to
     *  itself at a fixed offset.
     * @param bpOffset fixed offset from object reference of word containing
     * blueprint.
     * @throws BCdead <em>this is a pragma</em>
     */
    protected static void initOop( final char bpOffset) throws BCdead {
        rewrite( Oop.ID, "getBlueprint:()Lovm/core/domain/Blueprint;",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addQuickOpcode( REF_GETFIELD_QUICK, bpOffset);
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
    
    /** Set up Oop's getReference rewriter for any model that holds exactly one
     *  reference (besides the blueprint) and keeps it in a word to itself at a
     *  fixed offset.
     * @param offset fixed offset from object reference of word containing
     * reference value.
     * @throws BCdead <em>this is a pragma</em>
     */
    protected static void initGetReference1( final char offset) throws BCdead {
        rewrite( Oop.ID, "getReference:(I)Lovm/core/domain/Oop;",
            new Rewriter() {
                protected boolean rewrite() { // oop k
                    cursor.addSimpleInstruction( POP); // oop
                    cursor.addQuickOpcode( REF_GETFIELD_QUICK, offset);
		    addFiat();
                    return true; // delete original instruction
                }
            });
    }

    /** Set up rewriters for MonitorMapper methods; usable in any model where
     *  the monitor is kept in a word to itself at a fixed offset from the
     *  object reference.
     * @param monitorOffset fixed offset from object reference of word
     * holding monitor reference.
     * @throws BCdead <em>this is a pragma</em>
     */
    protected static void initMonitorMapper( final char monitorOffset)
    throws BCdead {
        final Selector.Method mfni = // MonitorFactory.newInstance()
            RepositoryUtils.methodSelectorFor(
                "Lovm/services/monitors/Monitor$Factory;",
                "newInstance:()Lovm/services/monitors/Monitor;");
        final Monitor.Factory factory =
            ((MonitorServicesFactory)ThreadServiceConfigurator.config
                .getServiceFactory(MonitorServicesFactory.name))
                .getMonitorFactory();
	
  	rewrite( MonitorMapper.ID, "releaseMonitor:()V",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addSimpleInstruction(ACONST_NULL);
                    cursor.addQuickOpcode(PUTFIELD_QUICK, monitorOffset);
                    return true; // delete original instruction
                }
            });

        rewrite( "getMonitor:()Lovm/services/monitors/Monitor;",
            new Rewriter() {
                protected boolean rewrite() {
		    cursor.addQuickOpcode(REF_GETFIELD_QUICK, monitorOffset);
		    addFiat();
                    return true; // delete original instruction
                }
            });

        rewrite( "setMonitor:(Lovm/services/monitors/Monitor;)V",
            new Rewriter() {
                protected boolean rewrite() { // oop mon
                    cursor.addQuickOpcode(PUTFIELD_QUICK, monitorOffset);
                    return true; // delete original instruction
                }
            });
    }
}
