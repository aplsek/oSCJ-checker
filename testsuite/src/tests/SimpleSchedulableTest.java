package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SimpleSchedulableTest extends ParameterizedCheckerTest {

    public SimpleSchedulableTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework",BOOTCLASSPATH, "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scope/schedulable/simple");
        return tests;
    }
}
