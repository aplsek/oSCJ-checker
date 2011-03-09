package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;



public class BigTest extends ParameterizedCheckerTest {

    public BigTest(File testFile) {
        super(testFile, "checkers.SCJChecker", "framework", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> tests = testFiles("scope/miniCDx");

        //tests.addAll(testFiles("scope/level0"));
        //tests.addAll(testFiles("scope/level1"));
        //tests.addAll(testFiles("scope/level2"));
       // tests.addAll(testFiles("scope/perReleaseAlloc"));
       // tests.addAll(testFiles("scope/staticAlloc"));
       // tests.addAll(testFiles("scope/illegalStateEx"));
        //tests.addAll(testFiles("scope/error"));
        //tests.addAll(testFiles("scope/advancedMM"));

        return tests;
    }
}
