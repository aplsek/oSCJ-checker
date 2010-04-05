package s3.core.domain;

import ovm.core.OVMBase;
import ovm.core.repository.Selector;
import ovm.core.domain.Blueprint;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.repository.RepositoryUtils;
import ovm.core.services.memory.VM_Address;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.services.bytecode.editor.Marker;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.util.OVMError;
import s3.services.monitors.HTObject2Monitor;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import s3.util.PragmaTransformCallsiteIR.Rewriter;

/** Describes an object layout with one 32word for blueprint,
 *  monitors in an ancillary hash map, and hash==address (for nonmoving
 *  collectors only!).
 *  @author Chapman Flack
 **/
public class S3Model_B_0M extends ObjectModel {

    private static final char BP_OFFSET = 0;
    
    private static final HTObject2Monitor map = new HTObject2Monitor();
    
    // it's an identity map; identity hashes can change from host vm to ours,
    // requiring rehashing in general; here we assert map's empty just then.
    private static final void boot_() { assert( 0 == map.size()); }
    
    // the methods in this class should not be called directly (i.e. with
    // this class specified at the call site) but rather on the appropriate
    // interfaces or ObjectModel, where the pragmas giving the correct
    // runtime behavior are specified.  To catch perverse attempts to call
    // directly on this class, the methods here will all throw BCdead.
    
    public VM_Address newInstance() throws BCdead { return new Proxy(); }

    public int headerSkipBytes() throws BCdead { return 4; }
    
    // the blueprint doesn't count (it's reachable through domain).
    // in this model the monitor doesn't either; it's reachable through map, which
    // is static and therefore a root.
    public int maxReferences() throws BCdead { return 0; }
    
    public int identityHash( Object o) throws BCdead
      	{ return System.identityHashCode( o); }
    
    // no BCdead here please - I am a real method.
    public Oop stamp( VM_Address a, Blueprint bp) {
      	a.add( BP_OFFSET).setAddress( VM_Address.fromObject( bp));
	// anything else that needs initial value (hash code, etc....)
	return super.stamp( a, bp);
    }

    /**Returns a string with a description of the header data structure in C.**/
    public String toCStruct() {
        return ("struct HEADER {\n"+
		"\tstruct s3_core_domain_S3Blueprint *_blueprint_;\n" +
		"};\n" +
		"#define HEADER_BLUEPRINT(H) " +
		"(((struct HEADER*)(H))->_blueprint_)\n" +
		"#define HEADER_HASHCODE(H)  ((jint) (H))\n");
    }
    public String toCxxStruct() {
	return ("struct HEADER {\n"+
		"\tstruct e_s3_core_domain_S3Blueprint *blueprint;\n"+
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
        S3Model_B_M.initOop( BP_OFFSET);
        // register nothing for getReference(k), see comment in ObjectModel
    }

    /** Initialize monitor support, which depends on threading config. **/
    public void initialize() {
        initMonitorMapper();
    }

    /** Register rewriters for MonitorMapper, for any model that keeps monitors
     *  in an HTObject2Monitor map.  The map is static private to <em>this</em>
     *  class, which can be changed and made a parameter to this method if
     *  anyone cares; just find another way to do the assertion in boot_.
     * @throws BCdead <em>this is a pragma</em>
     */
    protected static void initMonitorMapper() throws BCdead {
        final Selector.Method mfni = // MonitorFactory.newInstance()
            RepositoryUtils.methodSelectorFor(
              	"Lovm/services/monitors/Monitor$Factory;",
        	"newInstance:()Lovm/services/monitors/Monitor;");
        final Selector.Method mapget =
            RepositoryUtils.methodSelectorFor(
              	"Ls3/services/monitors/HTObject2Monitor;",
        	"get:(Ljava/lang/Object;)Lovm/services/monitors/Monitor;");
        final Selector.Method mapput =
            RepositoryUtils.methodSelectorFor(
              	"Ls3/services/monitors/HTObject2Monitor;",
        	"put:(Ljava/lang/Object;Lovm/services/monitors/Monitor;)V");
        final Selector.Method mapremove =
            RepositoryUtils.methodSelectorFor(
              	"Ls3/services/monitors/HTObject2Monitor;",
        	"remove:(Ljava/lang/Object;)V");
        final Monitor.Factory factory =
            ((MonitorServicesFactory)ThreadServiceConfigurator.config
             .getServiceFactory(MonitorServicesFactory.name))
             .getMonitorFactory();
        	
        rewrite( MonitorMapper.ID, "releaseMonitor:()V",
            new Rewriter() {
                protected boolean rewrite() {
                    cursor.addResolvedRefLoadConstant(map);
                    cursor.addSimpleInstruction( SWAP);
                    cursor.addINVOKEVIRTUAL( mapremove);
                    return true; // delete original instruction
                }
            });
        rewrite( "getMonitor:()Lovm/services/monitors/Monitor;",
            new Rewriter() {
                protected boolean rewrite() {
                    Marker a = cursor.makeUnboundMarker();
                    Marker b = cursor.makeUnboundMarker();
                    	  		   	       // oop
                    cursor.addResolvedRefLoadConstant(map);// oop map
                    cursor.addSimpleInstruction( SWAP);// map oop
                    cursor.addINVOKEVIRTUAL( mapget);  // mon
                    return true; // delete original instruction
                }
            });
            
        rewrite( "setMonitor:(Lovm/services/monitors/Monitor;)V",
            new Rewriter() {
                protected boolean rewrite() {         // oop mon 
                    cursor.addResolvedRefLoadConstant(map);     // oop mon map
                    cursor.addRoll( (char)3, (byte)1);// map oop mon
                    cursor.addINVOKEVIRTUAL( mapput); // walla walla bing bang
                    return true; // delete original instruction
                }
            });       
    }
}
