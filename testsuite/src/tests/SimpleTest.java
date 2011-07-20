package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SimpleTest extends ParameterizedCheckerTest {

    public SimpleTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", BOOTCLASSPATH, "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scope/defineScope/simple/");
        tests.addAll(SCJRestrictedTest.data());
        tests.addAll(testFiles("scope/scopeRunsIn/simple"));
        tests.addAll(testFiles("scope/schedulable/simple"));
        tests.addAll(testFiles("scope/scope/simple/"));
        return tests;
    }
}
