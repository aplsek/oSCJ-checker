package s3.services.simplejit.x86;

import ovm.services.bytecode.InstructionSet;
import s3.core.domain.S3Method;
import s3.services.simplejit.SimpleJITAnalysis;
import s3.services.simplejit.CodeGenerator;
import s3.services.simplejit.CompilerVMInterface;
import s3.services.simplejit.SimpleJIT;

/**
 * The x86 specific compiler driver.
 * @author Hiroshi Yamauchi
 **/
public class SimpleJITImpl 
    extends SimpleJIT {

    public SimpleJITImpl(
			 CompilerVMInterface compilerVMInterface,
			 boolean debugPrint,
			 boolean opt,
			 SimpleJITAnalysis anal) {
	super( compilerVMInterface,
		debugPrint, opt, anal);
    }

    protected CodeGenerator makeCodeGenerator(S3Method method,
            InstructionSet is, CompilerVMInterface compilerVMInterface,
            CodeGenerator.Precomputed precomputed, boolean debugPrintOn) {
        return new CodeGeneratorImpl(method, InstructionSet.SINGLETON,
                compilerVMInterface, precomputed, debugPrint);
    }

    protected CodeGenerator makeCodeGenerator2(S3Method method,
            InstructionSet is, CompilerVMInterface compilerVMInterface,
            CodeGenerator.Precomputed precomputed, boolean debugPrintOn) {
        return new CodeGeneratorImpl(method, InstructionSet.SINGLETON,
                compilerVMInterface, precomputed, debugPrint);
    }
    
    public String getStackLayoutAsCFunction() {
	return StackLayoutImpl.getStackLayoutAsCFunction();
    }

}
