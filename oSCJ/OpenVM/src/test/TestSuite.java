package test;

import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;

public abstract class TestSuite {
    public abstract void run();
    
    static public TestSuite the() throws PragmaStitchSingleton {
	return (TestSuite) InvisibleStitcher.singletonFor(TestSuite.class);
    }

    static public class Disabled extends TestSuite {
	public void run() {}
    }
}