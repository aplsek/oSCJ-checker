package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class NoScopeCheckerTest extends ParameterizedCheckerTest {

    public NoScopeCheckerTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", BOOTCLASSPATH, "-Anomsgtext","-AnoScopeChecks");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("all/noScopeChecker");
        return tests;
    }
}
