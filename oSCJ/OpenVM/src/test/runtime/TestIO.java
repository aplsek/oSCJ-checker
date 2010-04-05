package test.runtime;
import ovm.services.io.Resource.StandaloneFile;
import ovm.util.ByteBuffer;
import ovm.util.OVMException;
import test.common.TestBase;
import test.common.TestSuite;

public class TestIO extends TestBase {

    boolean doThrow;

    public TestIO(long disabled) {
        super("IO");
        doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }

    public void runOn(String name, boolean shouldSucceed) {
        try {
            StandaloneFile file = new StandaloneFile(name);
            ByteBuffer bytes = file.getContents();
            check_condition(bytes.remaining() > 0);
//             BasicIO.out.print(" --- OK: sizeof ");
//             BasicIO.out.print(name);
//             BasicIO.out.print(" is ");
//             BasicIO.out.print(bytes.remaining());
//             BasicIO.out.println();
            check_condition(shouldSucceed == true);
        } catch (OVMException.IO e) {
            check_condition(shouldSucceed == false);
            //BasicIO.out.println(" --- OK: " + e.getMessage());
            return;
        }
    }
    public void run() {
        runOn("OVMMakefile", true);
        if (doThrow)
            runOn("../../ovm/core/OVMBase.class.NOT-HERE", false);
    }

}
