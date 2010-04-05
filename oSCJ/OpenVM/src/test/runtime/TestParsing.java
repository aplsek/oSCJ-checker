package test.runtime;
import ovm.core.repository.TypeName;
import ovm.core.repository.Bundle;
import ovm.core.repository.Repository;
import ovm.core.repository.RepositoryClass;
import ovm.core.services.format.OvmFormat;
import ovm.core.services.io.BasicIO;
import ovm.util.OVMException;
import test.common.TestBase;
import ovm.services.io.ResourcePath;

public class TestParsing extends TestBase {

    public TestParsing() {
        super("Parsing");
    }

    public void run() {
        parse("Lovm/core/OVMBase;");
        parse("Ls3/core/domain/S3Domain;");
        parse("Lovm/core/services/memory/VM_Address;");

    }

    public void parse(String stringName) {
        Bundle bundle = Repository._.makeBundle(new ResourcePath("../../src"));
        TypeName.Scalar tn = OvmFormat._.parseTypeName(stringName).asScalar();
        try {
            RepositoryClass cls = bundle.lookupClass(tn);
            BasicIO.err.println("parsed class  " + cls.getName());
        } catch (OVMException e) {
            BasicIO.err.println("failed " + e);
        }
    }
}
