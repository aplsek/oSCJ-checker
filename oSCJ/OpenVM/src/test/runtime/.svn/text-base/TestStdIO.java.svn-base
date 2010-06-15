package test.runtime;


public class TestStdIO extends TestSyncBase {
    public TestStdIO(long disabled) {
	super("StdIO");
    }
    
    public void run() {
	byte[] hello=new byte[]{' ','h', 'e', 'l', 'l', 'o', ' '};
	LibraryImports.write(1,hello,0,hello.length,true);
    }

}
