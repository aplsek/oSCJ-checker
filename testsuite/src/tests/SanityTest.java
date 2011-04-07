package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SanityTest extends ParameterizedCheckerTest {

    public SanityTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scope/defineScope/sanity");

        tests.addAll(testFiles("scope/scopeRunsIn/sanity"));
        tests.addAll(testFiles("scope/scope/sanity"));
        tests.addAll(testFiles("scope/schedulable/sanity"));
        tests.addAll(testFiles("scjAllowed/sanity"));

        return tests;
    }
}
