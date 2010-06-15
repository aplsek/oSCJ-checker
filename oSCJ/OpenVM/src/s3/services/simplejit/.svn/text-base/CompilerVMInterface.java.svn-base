package s3.services.simplejit;

import ovm.core.domain.Domain;
import ovm.core.domain.ExecutiveDomain;
import ovm.core.domain.JavaDomain;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.VM_Address;

/**
 * The compiler-VM interface.
 * @author Hiroshi Yamauchi
 **/
public interface CompilerVMInterface {

    /********** Domain **********/

    /** 
     * Return the compile target domain. This could be the executive
     * domain.
     */
    public JavaDomain getTargetDomain();

    /** 
     * Return the executive domain.
     */
    public ExecutiveDomain getExecutiveDomain();

    /*********** Code Generation **********/

    /**
     * Tells whether the compiler has to emit position independent
     * code (that is, whether or not the compiled code will move). If
     * false, some optimization may become possible.
     */
    public boolean positionIndependentCode(Domain domain);

    /**
     * Tells whether objects move at all (that is, whether or not key
     * object addresses (vtable, blueprint, etc) be embedded (or
     * hardcoded) in compiled machine code). If false, some
     * optimization may become possible.
     */
    public boolean movingGC(Domain domain);

    /**
     * Tells whether dynamic class loading will occur (that is,
     * whether or not a closed-world assumption can be made. If false,
     * some optimization may become possible.
     */
    public boolean dynamicClassLoading(Domain domain);

    /********** Object layout & Object model **********/

    public ObjectLayout getObjectLayout(Domain domain);

    /* Field offsets and header sizes */
    public interface ObjectLayout {
	public int getFieldOffset(TypeName.Scalar classTypeName, 
				  UnboundSelector.Field selector);
	public int getFieldSize(char typeTag);
	public int getArrayHeaderSize(char typeTag);
	public int getArrayElementSize(char typeTag);
	public int getArrayLengthFieldOffset();
	public int getArrayLengthFieldSize();
    }
    /* Method indices ... */

    /**
     * Return the machine code that performs Oop.getBlueprint()
     * @param objReg the register that contains the object
     * @param bpReg the register that will contain the blueprint of
     * the object as a result
     */
    public byte[] getGetBlueprintCode_X86(byte objReg, byte bpReg);
    public int[] getGetBlueprintCode_PPC(int bpReg, int objReg);
    
    /********** VM calls ***********/

    /**
     * Return the CSA method vtable index for the CSA method of the
     * given selector.
     */
    public int getCSAMethodIndex(UnboundSelector.Method method);

    /**
     * Return the exception type ID given an exception class. The
     * result will be used for CSA.generateThrowable calls.
     */
    public int getExceptionID(Domain domain, Class c);

    /********** Runtime functions & Native methods **********/

    /**
     * Return the list of native (INVOKE_NATIVE) methods.
     */
    public UnboundSelector.Method[] getNativeMethodList();

    /**
     * Return the handle to the native runtime function table. Here,
     * we expect the VM prepare such a table.
     */
    public VM_Address getRuntimeFunctionTableHandle();

    /**
     * Return the runtime function table index for the C runtime
     * function of the given name.
     */
    public int getRuntimeFunctionIndex(String symbol);


}
