package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SimpleTest extends ParameterizedCheckerTest {

    public SimpleTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scope/defineScope/simple");
        tests.addAll(testFiles("scope/scopeRunsIn/simple"));
        tests.addAll(testFiles("scope/scope/simple"));
        tests.addAll(SCJAllowedTest.data());
        tests.addAll(SCJRestrictedTest.data());
        return tests;
    }
}
