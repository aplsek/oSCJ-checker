package s3.services.simplejit;

import ovm.core.domain.AttributedCode;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.domain.LinkageException;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.repository.Attribute;
import ovm.core.repository.TypeName;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.Constants;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.RepositoryClass;
import ovm.core.services.memory.VM_Address;
import s3.core.domain.S3Method;

import java.io.IOException;

import java.io.ByteArrayOutputStream;
import ovm.core.repository.RepositoryUtils;

public class SimpleJITCode extends AttributedCode {
    public static Code.Kind KIND = new Code.Kind() {};

    public  Constants           constants_;
    private  boolean            synchronized_;
    private  int                maxLocals_;
    private  int                maxStack_;
    private  int                argLen_;
    public  byte[]              sourceFileName;
    public  int[]               bytecodePC2NativePC;

    private  CoreServicesAccess myCSA_;
    public  byte[]              methodname_;// For JIT debugging.
    public Attribute.LineNumberTable lnt;   // For JIT debugging.
    /** The argument length (including the receiver) in bytes. Used by
     * for SimpleJIT to do INVOKESYSTEM.INVOKE. */
    private int                 argumentLength_;

    // For not-yet-compiled methods
    public SimpleJITCode(Method method,
			 VM_Address codeEntry,
			 ExceptionHandler[] nativeExceptionHandlers,
			 Constants constants,
			 int maxLocals,  int maxStack,  int argLen,
			 int[] bytecodePC2NativePC,
			 Attribute.LineNumberTable lnt) {
	super(method,
	      lnt == null ? EMPTY_ATTRIBUTES : new Attribute[] { lnt },
	      nativeExceptionHandlers);
	foreignEntry = codeEntry;

	try {// Store the method name as zero terminated bytes for debugging 
	    ByteArrayOutputStream str = new ByteArrayOutputStream();
	    itsMethod.getSelector().write(str);
	    str.write(0);
	    methodname_ = str.toByteArray();
	} catch (IOException e) {  throw new Error(e.toString());  }
	this.lnt = lnt;
	this.synchronized_ = itsMethod.getMode().isSynchronized();
	this.constants_ = constants;
	this.myCSA_ = itsMethod.getDeclaringType().getDomain().getCoreServicesAccess();
	this.maxLocals_ = maxLocals;
	this.maxStack_ = maxStack;
	this.argLen_ = argLen;
	this.bytecodePC2NativePC = bytecodePC2NativePC;

	// Process the source file name
	Type.Compound declType = itsMethod.getDeclaringType();
	int sourceFileIdx = declType.getSourceFileNameIndex();
	String sourceFile = (sourceFileIdx != 0
			     ? RepositoryUtils.utfIndexAsString(sourceFileIdx)
			     : "[no source file]");
	byte[] raw_bytes = sourceFile.getBytes();
	byte[] zero_terminated_bytes = new byte[raw_bytes.length + 1];
	for (int i = 0; i < raw_bytes.length; i++) 
	    zero_terminated_bytes[i] = raw_bytes[i];
	zero_terminated_bytes[zero_terminated_bytes.length - 1] = 0;
	this.sourceFileName = zero_terminated_bytes;
	int receiver_sz = 4;
	this.argumentLength_ = (receiver_sz
				+ ((S3Method) itsMethod).getArgumentLength());
    }
    public Code.Kind getKind() { return KIND; }

    public void bang(Code c) { bang( (SimpleJITCode)c); }

    public void bang(SimpleJITCode c) {
	bang((AttributedCode) c);
	this.maxStack_ = c.maxStack_;
	this.maxLocals_ = c.maxLocals_;
	this.constants_ = c.constants_;
	this.synchronized_ = c.synchronized_;	
	this.argLen_ = c.argLen_;
	this.methodname_ = c.methodname_;
	this.lnt = c.lnt;
	this.sourceFileName = c.sourceFileName;
	this.bytecodePC2NativePC = c.bytecodePC2NativePC;
	this.myCSA_ = c.myCSA_;
	this.methodname_ = c.methodname_;
	this.argumentLength_ = c.argumentLength_;
    }

    public VM_Address getCodeEntry() { return foreignEntry; }
}
