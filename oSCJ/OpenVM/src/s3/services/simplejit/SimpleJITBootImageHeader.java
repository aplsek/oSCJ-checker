package s3.services.simplejit;
/**
 * An object of this type will be included in the image header if the
 * jit is on.  This class is here in this package because the
 * s3.services.simplejit.bootimage package will not be in the boot
 * image.
 */

import ovm.core.services.memory.VM_Address;

public final class SimpleJITBootImageHeader {
    public VM_Address runtimeFunctionTableHandle;
    public SimpleJITCode mainMethod;
    public static final SimpleJITBootImageHeader singleton = new SimpleJITBootImageHeader();
    private SimpleJITBootImageHeader() { }
    public void setMainMethod(SimpleJITCode nativeMainMethod) {
	this.mainMethod = nativeMainMethod;
    }
}

