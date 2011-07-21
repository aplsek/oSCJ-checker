package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SCJAllowedTest extends ParameterizedCheckerTest {

    public SCJAllowedTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework",BOOTCLASSPATH, "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scjAllowed/simple");
        tests.addAll(testFiles("javax/safetycritical"));
        return tests;
    }
}
