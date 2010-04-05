package ovm.core.execution;

import ovm.core.domain.Code;
import ovm.core.services.memory.VM_Address;
import s3.core.domain.S3ByteCode;

public class NullTrampoline extends Trampoline {
    public VM_Address compileAndRestart(S3ByteCode bc, Code _, Object __) {
	return null;
    }
}
