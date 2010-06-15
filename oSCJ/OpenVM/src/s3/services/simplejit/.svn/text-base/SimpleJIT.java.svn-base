package s3.services.simplejit;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import ovm.core.domain.Method;
import ovm.core.execution.NativeConstants;
import ovm.core.repository.UTF8Store;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.VM_Area;
import ovm.services.bytecode.InstructionSet;
import ovm.util.HashMap;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Method;
import s3.services.simplejit.bytecode.Inliner;
import s3.services.simplejit.bytecode.Inliner2;
import s3.services.simplejit.bytecode.Translator;

/**
 * The compiler driver.
 * @author Hiroshi Yamauchi
 **/
public abstract class SimpleJIT {

	private static final boolean PRINT_STATUS_EACH_METHOD = false;
	private static final boolean PRINT_STATUS = true;
	private static final int PRINT_STATUS_INTERVAL = 500;
	private static int compileCounter = 0;

	public static void printCompileCounter() {
		BasicIO.out.println("[SimpleJIT] " + compileCounter + " methods have been compiled...");
	}
	public static final boolean dumpOvmIRSJ = false;
	public static PrintWriter img_ovmir_ascii_sj = null;
	static {
		try {
			if (dumpOvmIRSJ)
				img_ovmir_ascii_sj = new PrintWriter(new BufferedOutputStream(new FileOutputStream("img.ovmir.ascii.sj")));
		} catch (IOException e) { throw new RuntimeException(e); }
	}

	public static final int SCENARIO_OFR  = 1;
	public static final int SCENARIO_ONR  = 2;
	public static final int SCENARIO_ONRI = 3;
	public static final int SCENARIO_OFRI = 4;
	public static final int compileScenario = SCENARIO_OFRI;

	public static final boolean doCompileTimeStat = true;

	public static CompileTimeStat compileTime;
	public static CompileTimeStat specCompileTime;
	public static CompileTimeStat verificationTime;
	public static CompileTimeStat specVerificationTime;

	static {
		if (doCompileTimeStat) {
			compileTime = new CompileTimeStat("Total compile time");
			specCompileTime = new CompileTimeStat("Spec compile time");
			verificationTime = new CompileTimeStat("Total verification time");
			specVerificationTime = new CompileTimeStat("Spec verification time");
		}
	}

	private static boolean isSpecMethod(Method method) {
		String name = method.toString();
		return name.startsWith("Lspec/")
		|| name.startsWith("Gspec/")
		|| name.startsWith("LSpecApplication/")
		|| name.startsWith("GSpecApplication/");
	}

	public static class CompileTimeStat {
		int nMethod = 0;
		long totalCompileTime = 0L; // milliseconds
		long startTime;
		final int STATE_OFF = 0;
		final int STATE_ON = 1;
		final int STATE_PAUSED = 2;
		int state = STATE_OFF;
		Method currMethod;
		String name;

		public CompileTimeStat(String name) {
			this.name = name;
		}
		public Method currentMethod() { return currMethod; }
		public void countMethod(Method method) {
			nMethod++;
		}
		public void start(Method method) {
			if (state != STATE_OFF)
				throw new Error(name + " : Tried to start the timer without before stopping it");
			state = STATE_ON;
			startTime = 0;//System.currentTimeMillis();
			currMethod = method;
		}
		public void stop(Method method) {
			if (state == STATE_OFF)
				throw new Error(name + " : Tried to stop the timer without before starting it");
			long endTime = 0;//System.currentTimeMillis();
			long duration = endTime - startTime;
			totalCompileTime += duration;
			state = STATE_OFF;
		}
		public void restart() {
			if (state != STATE_PAUSED)
				throw new Error(name + " : Tried to restart the timer without before paused it");
			state = STATE_ON;
			startTime = 0;//System.currentTimeMillis();
		}
		public void pause() {
			long endTime = 0;//System.currentTimeMillis();
			long duration = endTime - startTime;
			totalCompileTime += duration;
			state = STATE_PAUSED;
		}
		public void restartIfPaused() {
			if (state == STATE_PAUSED)
				restart();
		}
		public void pauseIfOn() {
			if (state == STATE_ON)
				pause();
		}
		public String toString() {
			return "SJ " + name + " : # method " + nMethod + 
			", compile time " + totalCompileTime + 
			" (millisec) ";
		}
	}

	public static void countCompiledMethod(Method method) {
		if (doCompileTimeStat) {
			if (isSpecMethod(method)) {
				specCompileTime.countMethod(method);
			}
			compileTime.countMethod(method);
		}
	}
	public static void countVerifiedMethod(Method method) {
		if (doCompileTimeStat) {
			if (isSpecMethod(method)) {
				specVerificationTime.countMethod(method);
			}
			verificationTime.countMethod(method);
		}
	}
	public static void startCompileTimer(Method method) {
		if (doCompileTimeStat) {
			if (isSpecMethod(method)) {
				specCompileTime.start(method);
			}
			compileTime.start(method);
		}
	}
	public static void stopCompileTimer(Method method) {
		if (doCompileTimeStat) {
			if (isSpecMethod(method)) {
				specCompileTime.stop(method);
			}
			compileTime.stop(method);
		}
	}
	public static void restartCompileTimer() {
		if (doCompileTimeStat) {
			specCompileTime.restartIfPaused();
			compileTime.restart();
		}
	}
	public static void pauseCompileTimer() {
		if (doCompileTimeStat) {
			specCompileTime.pauseIfOn();
			compileTime.pause();
		}
	}

	public static void startVerificationTimer(Method method) {
		if (doCompileTimeStat) {
			if (isSpecMethod(method)) {
				specVerificationTime.start(method);
			}
			verificationTime.start(method);
		}
	} 
	public static void stopVerificationTimer(Method method) {
		if (doCompileTimeStat) {
			if (isSpecMethod(method)) {
				specVerificationTime.stop(method);
			}
			verificationTime.stop(method);
		}
	}

	public static void printCompileStat() {
		BasicIO.out.println(compileTime.toString());
		BasicIO.out.println(specCompileTime.toString());
		BasicIO.out.println(verificationTime.toString());
		BasicIO.out.println(specVerificationTime.toString());
	}

	public static final HashMap beforeInlining = new HashMap();
	public static final int clinitUtf8Index = UTF8Store._.findUtf8(UnicodeBuffer.factory().wrap("<clinit>"));

	protected CompilerVMInterface compilerVMInterface;
	protected boolean debugPrint;
	protected boolean opt;
	protected CodeGenerator.Precomputed precomputed;
	protected SimpleJITAnalysis anal;

	protected SimpleJIT(
			CompilerVMInterface compilerVMInterface,
			boolean debugPrint,
			boolean opt,
			SimpleJITAnalysis anal) {
		this.compilerVMInterface = compilerVMInterface;
		this.debugPrint = debugPrint;
		this.opt = opt;
		this.precomputed =
			new CodeGenerator.Precomputed(compilerVMInterface);
		this.anal = anal;
	}

	public CodeGenerator.Precomputed getCodeGeneratorPrecomputed() {
		return precomputed;
	}

	/**
	 * Compile a method.
	 **/
	public void compile(Method method, VM_Area compileArea) {
		if (method.getByteCode() == null) {
			throw new OVMError.Internal("[SimpleJIT] No S3ByteCode found in "
					+ method.getSelector());
		}
		if (method.getByteCode().getBytes() == null) {
			throw new OVMError.Internal(
					"[SimpleJIT] No bytecode (byte[]) found in "
					+ method.getSelector());
		}

		if (PRINT_STATUS_EACH_METHOD) {
			BasicIO.out.println("Compiling " + method.toString());
		}

		if (PRINT_STATUS) {
			if (compileCounter > 0
					&& (compileCounter % PRINT_STATUS_INTERVAL) == 0) {
				printCompileCounter();
			}
			compileCounter++;
		}

		if (doCompileTimeStat) {
			countCompiledMethod(method);
		}

		if (opt && method.getSelector().getNameIndex() != clinitUtf8Index) {
			switch (compileScenario) {
			case SCENARIO_ONR: {
				// BasicIO.out.println("Translating " + method.toString());
				S3ByteCode translatedCode = Translator.translate(
						method.getByteCode(), method,
						compileArea);
				method.addCode(translatedCode);
				break;
			}
			case SCENARIO_ONRI: {
				beforeInlining.put(method, method.getByteCode());
				SimpleJIT.startCompileTimer(method);
				try {
					S3ByteCode inlined = new Inliner(method, anal, compileArea)
					.inline();
					if (inlined != null) {
						method.addCode(inlined);
					}
				} finally {
					SimpleJIT.stopCompileTimer(method);
				}
				// BasicIO.out.println("Translating " + method.toString());
				S3ByteCode translatedCode = Translator.translate(
						method.getByteCode(), method,
						compileArea);
				method.addCode(translatedCode);
				break;
			}
			case SCENARIO_OFR: {
				// BasicIO.out.println("Translating " + method.toString());
				S3ByteCode translatedCode = Translator.translate(
						method.getByteCode(), method,
						compileArea);
				method.addCode(translatedCode);
				break;
			}
			case SCENARIO_OFRI: {
				if (beforeInlining.get(method) == null) {
					// BasicIO.out.println("Translating " + method.toString());
					S3ByteCode translatedCode = Translator.translate(
							method.getByteCode(),
							method, compileArea);
					beforeInlining.put(method, translatedCode);
					method.addCode(translatedCode);
				}
				S3ByteCode inlined = null;
				startCompileTimer(method);
				try {
					inlined = new Inliner2(method, anal, compileArea).inline();
				} finally {
					stopCompileTimer(method);
				}
				if (inlined != null) {
					startCompileTimer(method);
					try {
						inlined = Translator.performDataflowOpt(inlined,
								Inliner.MAX_DEPTH, compileArea);
					} finally {
						stopCompileTimer(method);
					}
					inlined = Translator.addLivenessAttribute(inlined,
							compileArea);
					method.addCode(inlined);
				}
				break;
			}
			default:
				throw new Error();
			}

			if (dumpOvmIRSJ) {
				S3ByteCode bc = method.getByteCode();
				bc.dumpAscii(method.getDeclaringType().getDomain().toString(),
						img_ovmir_ascii_sj);
				img_ovmir_ascii_sj.flush();
			}

			if (NativeConstants.OVM_PPC && NativeConstants.OSX_BUILD) {
				CodeGenerator codegen = makeCodeGenerator2((S3Method) method,
						InstructionSet.SINGLETON, compilerVMInterface,
						precomputed, debugPrint);
				startCompileTimer(method);
				try {
					codegen.compile(compileArea);
				} finally {
					stopCompileTimer(method);
				}
			} else if (NativeConstants.OVM_X86) {
				CodeGenerator codegen = makeCodeGenerator((S3Method) method,
						InstructionSet.SINGLETON, compilerVMInterface,
						precomputed, debugPrint);
				startCompileTimer(method);
				try {
					codegen.compile(compileArea);
				} finally {
					stopCompileTimer(method);
				}
			} else {
				throw new Error();
			}
		} else {
			if (dumpOvmIRSJ) {
				(method.getByteCode()).dumpAscii(method
						.getDeclaringType().getDomain().toString(),
						img_ovmir_ascii_sj);
				img_ovmir_ascii_sj.flush();
			}

			CodeGenerator codegen = makeCodeGenerator((S3Method) method,
					InstructionSet.SINGLETON, compilerVMInterface, precomputed,
					debugPrint);
			startCompileTimer(method);
			try {
				codegen.compile(compileArea);
			} finally {
				stopCompileTimer(method);
			}
		}

	}

	protected abstract CodeGenerator makeCodeGenerator
	(S3Method method,
			InstructionSet is,
			CompilerVMInterface compilerVMInterface,
			CodeGenerator.Precomputed precomputed,
			boolean debugPrintOn);

	protected abstract CodeGenerator makeCodeGenerator2
	(S3Method method,
			InstructionSet is,
			CompilerVMInterface compilerVMInterface,
			CodeGenerator.Precomputed precomputed,
			boolean debugPrintOn);

	public abstract String getStackLayoutAsCFunction();

} // end of SimpleJIT
