package s3.services.simplejit.bootimage;
/**
 * A bootimage compiler-VM interface for SimpleJIT.
 * @author Hiroshi Yamauchi
 **/
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.VM_Address;
import s3.core.domain.S3Domain;
import s3.services.bootimage.Ephemeral;
import s3.services.simplejit.SimpleJITCompilerVMInterface;

public class SimpleJITBootImageCompilerVMInterface
	extends SimpleJITCompilerVMInterface
    implements Ephemeral.Void {

    public SimpleJITBootImageCompilerVMInterface
	(S3Domain executiveDomain,
	 S3Domain targetDomain,
	 UnboundSelector.Method[] csaMethods,
	 VM_Address jitHeader) {
    	super(executiveDomain, targetDomain, csaMethods, jitHeader);
    }
} // end of SimpleJITBootImageCompilerVMInterface

