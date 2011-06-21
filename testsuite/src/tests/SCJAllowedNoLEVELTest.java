package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SCJAllowedNoLEVELTest extends ParameterizedCheckerTest {

    public SCJAllowedNoLEVELTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", "-Anomsgtext","-Alevel=1");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scjAllowed/noLevel");
        return tests;
    }
}
