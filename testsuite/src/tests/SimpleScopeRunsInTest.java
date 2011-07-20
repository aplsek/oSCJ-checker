package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SimpleScopeRunsInTest extends ParameterizedCheckerTest {

    public SimpleScopeRunsInTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", BOOTCLASSPATH, "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scope/scopeRunsIn/simple");
        return tests;
    }
}
