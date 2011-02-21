package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Checker Framework, using the {@link TestChecker}.
 */
public class ScopeTest extends ParameterizedCheckerTest {

    public ScopeTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", "-Anomsgtext");
    }

    //@Parameters
    //public static Collection<Object[]> data() { return testFiles("framework"); }
}
