package test.runtime;

import ovm.core.execution.NativeConstants;

public class TestFileIO extends TestSyncBase {
    public TestFileIO(long disabled) {
	super("FileIO");
    }
    
    public void run() {
        testFileIOSemantics();
        testMissing();
    }
    
    public void testFileIOSemantics() {
        String[] tempFilename=new String[]{"/tmp/ovm_test_XXXXXXXX"};
        int fd=LibraryImports.mkstemp(tempFilename);
        check_condition(fd>=0, "Failed to open the file");
        try {
            //p("using temporary filename: "+tempFilename[0]+"\n");
            
            byte[] buf=new byte[1024];
            
            for (int i=0;i<100;++i) {
                check_condition(LibraryImports.write(fd,buf,0,buf.length,true)==buf.length);
            }
            
            check_condition(LibraryImports.length(tempFilename[0])==100*1024,
                            "File length check");
            
            check_condition(LibraryImports.rewind(fd)==0);
            
            for (int i=0;i<100;++i) {
                check_condition(LibraryImports.read(fd,buf,0,buf.length,true)==buf.length);
            }
	    LibraryImports.close(fd);
	    fd = -1;
	    long len = LibraryImports.length(tempFilename[0]);
	    check_condition(len == 100 * buf.length,
		       "LibraryImports.length() is wrong");
        } finally {
            LibraryImports.unlink(tempFilename[0]);
	    if (fd != -1)
		LibraryImports.close(fd);
        }
    }
    
    public void testMissing() {
        check_condition(LibraryImports.open("no such file",
                                       NativeConstants.O_RDONLY,
                                       0)==-1);
        check_condition(LibraryImports.getErrno()==NativeConstants.ENOENT);
    }

}
