package s3.services.simplejit.bootimage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import ovm.core.domain.Code;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.ReflectiveArray;
import ovm.core.domain.Type;
import ovm.core.execution.NativeConstants;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.util.ByteBuffer;
import ovm.util.HashMap;
import ovm.util.Iterator;
import ovm.util.OVMRuntimeException;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Method;
import s3.services.bootimage.Analysis;
import s3.services.bootimage.ImageObserver;
import s3.services.simplejit.SimpleJIT;
import s3.services.simplejit.CodeGenContext;
import s3.services.simplejit.SimpleJITAnalysis;
import s3.services.simplejit.SimpleJITCode;
import s3.services.simplejit.SimpleJITBootImageHeader;
import s3.services.simplejit.SimpleJITTrampoline;
import s3.services.simplejit.dynamic.SimpleJITDynamicCompiler;
import s3.services.simplejit.dynamic.SimpleJITDynamicCompilerVMInterface;
import s3.util.Walkabout;

public class SimpleJITBootImageCompileObserver
    extends ImageObserver {
    boolean debugPrint_;
    boolean opt_;
    boolean deferUDApp_;
    boolean lazyCompilation_;
    boolean verbose_;
    SimpleJITBootImageHeader jitHeader;
    S3Domain executiveDomain;
    Method mainMethod;

    public SimpleJITBootImageCompileObserver
	(String OVMMakefile, String gdbinit,
	 String debugPrint, String simplejitopt, 
	 String simplejitDeferUDApp, String simplejitLazyCompilation, String verbose)
    {
	this(OVMMakefile, gdbinit,
	     debugPrint != null && !debugPrint.equals("false"),
	     simplejitopt != null && !simplejitopt.equals("false"),
	     simplejitDeferUDApp != null && !simplejitDeferUDApp.equals("false"),
	     simplejitLazyCompilation != null && !simplejitLazyCompilation.equals("false"),
	     verbose != null && !verbose.equals("false"));
    }

    public SimpleJITBootImageCompileObserver
	(String OVMMakefile, String gdbinit, boolean debugPrint, boolean opt,
			boolean deferUDApp, boolean lazyCompilation, boolean verbose)
    {
	super(OVMMakefile, gdbinit);
	this.debugPrint_ = debugPrint;
	this.opt_ = opt;
	this.deferUDApp_ = deferUDApp;
	this.lazyCompilation_ = lazyCompilation;
	this.verbose_ = verbose;
    }

    public void registerLoaderAdvice(Gardener gardener) {
	executiveDomain = (S3Domain) getExecutiveDomain();
	mainMethod = ((Code) getHeader("mainMethod")).getMethod();
	jitHeader = SimpleJITBootImageHeader.singleton;//new SimpleJITBootImageHeader();


	gardener.declare(jitHeader, "simpleJITBootImageHeader");
	// We don't create the actual dispatch tables until
	// defineInitialHeap, so we want to be sure that this array
	// type is live.
	new ReflectiveArray(executiveDomain,
			    RepositoryUtils.makeTypeName("s3/services/simplejit",
							 "SimpleJITCode"));
	gardener.excludePackage("s3/services/simplejit/bootimage");
	//gardener.excludePackage("s3/services/simplejit/bytecode");

    }

    public void compileDomain(Domain dom, Analysis anal) {
	new DomainWalker(anal).walkDomain(dom);
    }

    public class DomainWalker extends Analysis.MethodWalker {
	S3Domain dom;
	SimpleJITBootImageCompiler compiler; // per-domain

	// super.walkDomain will find the analysis object for us
	public DomainWalker(Analysis _) {
	}

	private void generateHeaderFile(SimpleJITBootImageCompiler compiler) {
	    try {
		PrintWriter pw =
		    new PrintWriter
		    (new FileOutputStream("runtime_functions.h"));

		String[] runtimeFunctions =
		    SimpleJITBootImageCompilerVMInterface
		    .runtimeFunctionSymbols;

		pw.println("/* Automatically generated file. Do not edit! */");
		pw.println("#ifndef RUNTIME_FUNCTION_H");
		pw.println("#define RUNTIME_FUNCTION_H");
		pw.println("void* runtimeFunctionTable[" 
			     + runtimeFunctions.length + "];");
		pw.println();
		for (int i = 0; i < runtimeFunctions.length; i++) {
		    pw.println("void* OVM_" + runtimeFunctions[i] + ";");
		}
		pw.println();
		pw.println("void installRuntimeFunctionTable() {");
		for (int i = 0; i < runtimeFunctions.length; i++) {
		    pw.println("\truntimeFunctionTable["
				 + i
				 + "] = OVM_"
				 + runtimeFunctions[i]
				 + ";");
		}
		pw.println("}");
		String stackFrameDefiningCFunctions = 
		    compiler.getStackLayoutAsCFunction();
		pw.println(stackFrameDefiningCFunctions);
        pw.println("int csa_generateThrowable_index = " + 
                compiler.getCodeGeneratorPrecomputed().csa_generateThrowable_index + ";");
		pw.println("#endif\n");
		pw.close();
	    } catch (IOException e) {
		throw new OVMRuntimeException(e);
	    }
	}

	public void walk(Method method) {
	    if (!anal.shouldCompile(method))
		return;
	    
	    ((S3Method)method).toCompile = true;
	    
	    Type t = method.getDeclaringType();
	    Blueprint bp = dom.blueprintFor(t);
	    
	    // Defer compilation for UD Application code if -simplejit-defer-ud-app=true
	    if (deferUDApp_
	    		&& dom != executiveDomain 
	    		&& t.getContext() == dom.getApplicationTypeContext()) { 
	    	return;
		}
	    
		compiler.compile(method, bp, null);
		method.removeCode(S3ByteCode.KIND);

	    //System.out.println("Compiled : " + method);
	    
	    if (method == mainMethod) {
		jitHeader.setMainMethod((SimpleJITCode) mainMethod.getCode());
	    }
	}

        /*
         * Invoke simpleJIT against the blueprint set and propagate
         * native code objects of inherited (not overriden) method
         * from parents' native vtables to children's vtables. HY
         */
	public void walkDomain(Domain _dom) {
	    dom = (S3Domain) _dom;

	    VM_Address jitHeaderAddr = VM_Address.fromObject(jitHeader);
	    UnboundSelector.Method[] csaMethods;
	    try {
		S3Blueprint bpcsa = (S3Blueprint) executiveDomain.blueprintFor
		    (JavaNames.ovm_core_execution_CoreServicesAccess,
		     executiveDomain.getSystemTypeContext());

		Selector.Method[] sels = bpcsa.getSelectors();
		csaMethods =
		    new UnboundSelector.Method[sels.length];
		for (int i = 0; i < sels.length; i++)
		    csaMethods[i] = sels[i].getUnboundSelector();
	    
	    } catch (LinkageException le) {
		throw le.fatal();
	    }
	    SimpleJITBootImageCompilerVMInterface compilerVMInterface = 
		new SimpleJITBootImageCompilerVMInterface(executiveDomain,
							  (S3Domain) dom,
							  csaMethods,
							  jitHeaderAddr);
	    compiler = new SimpleJITBootImageCompiler(debugPrint_,
						      opt_,
						      verbose_,
						      compilerVMInterface, new SimpleJITBootImageAnalysis(anal));

	    // Create the dynamic compiler here for convenience
	    SimpleJITDynamicCompilerVMInterface dCompilerVMInterface =
        	new SimpleJITDynamicCompilerVMInterface((S3Domain) executiveDomain,
							dom,
							csaMethods,
							jitHeaderAddr);
	    dom.sj = new SimpleJITDynamicCompiler(debugPrint_, opt_, verbose_,
						  lazyCompilation_,
						  dCompilerVMInterface,
						  new SimpleJITAnalysis(null));
	    SimpleJITTrampoline.init(dCompilerVMInterface);

	    super.walkDomain(dom);

	    SimpleJIT.printCompileCounter();
	    
	    // Want to run this once for a build
	    // There is no meaning with the executiveDomain
	    if (executiveDomain == dom) {
		generateHeaderFile(compiler);
	    }

        }
    }
}
