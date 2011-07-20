package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SCJRestrictedMayAllocateTest extends ParameterizedCheckerTest {

    public SCJRestrictedMayAllocateTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", BOOTCLASSPATH, "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scjRestricted/mayAllocate");
        return tests;
    }
}
