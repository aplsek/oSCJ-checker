package s3.services.simplejit.dynamic;

import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.VM_Address;
import s3.core.domain.S3Domain;
import s3.services.simplejit.SimpleJITCompilerVMInterface;

public class SimpleJITDynamicCompilerVMInterface extends
		SimpleJITCompilerVMInterface {
    public SimpleJITDynamicCompilerVMInterface
	(S3Domain executiveDomain,
	 S3Domain targetDomain,
	 UnboundSelector.Method[] csaMethods,
	 VM_Address jitHeader) {
    	super(executiveDomain, targetDomain, csaMethods, jitHeader);
    }
}
