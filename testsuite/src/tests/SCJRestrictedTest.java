package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SCJRestrictedTest extends ParameterizedCheckerTest {

    public SCJRestrictedTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", BOOTCLASSPATH, "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = SCJRestrictedMayAllocateTest.data();
        tests.addAll(SCJRestrictedMaySelfSuspendTest.data());
        return tests;
    }
}
