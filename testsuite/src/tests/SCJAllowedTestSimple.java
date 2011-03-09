package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SCJAllowedTestSimple extends ParameterizedCheckerTest {

    public SCJAllowedTestSimple(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scjAllowed/");
        return tests;
    }
}
