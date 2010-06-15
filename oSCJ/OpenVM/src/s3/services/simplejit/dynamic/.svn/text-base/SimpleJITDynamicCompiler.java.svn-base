package s3.services.simplejit.dynamic;

import s3.services.simplejit.CompilerVMInterface;
import s3.services.simplejit.SimpleJITCompiler;
import s3.services.simplejit.SimpleJITAnalysis;

public class SimpleJITDynamicCompiler extends SimpleJITCompiler {

	private boolean lazyCompilation_;
	
	public SimpleJITDynamicCompiler(boolean debugPrint, boolean opt, boolean verbose,
			boolean lazyCompilation,
			CompilerVMInterface compilerVMInterface, SimpleJITAnalysis anal) {
		super(debugPrint, opt, verbose, compilerVMInterface, anal);
		lazyCompilation_ = lazyCompilation;
	}
	
	public boolean lazyCompilation() { return lazyCompilation_; }
}
