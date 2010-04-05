/**
 * @file ovm/util/DirectoryScanner.java
 **/
package ovm.util;

import java.io.*;

/**
 * Simple iterator over all files in a directory.
 *
 * @author Christian Grothoff
 **/
public class DirectoryScanner 
    implements FileIterator {

    private File[] stack;
    private File next;

    /**
     * @param directory the name of the directory
     **/
    public DirectoryScanner(String directory) {
	stack = new File[0];
	push(new File(directory));
	doNext();
    }

    /**
     * @param f  the directory to scan
     **/
    public DirectoryScanner(File f) {
	stack = new File[0];
	push(f);
	doNext();
    }

    public boolean hasNext() {
	return next != null;
    }

    public File next() {
	File ret = next;
	doNext();
	return ret;
    }

    private void doNext() {
	File f = pop();
	if (f == null) {
	    next = null;
	    return;
	}
	while (f.isDirectory()) {
	    push(f.listFiles());
	    f = pop();
	    if (f == null) {
		next = null;
		return;
	    }
	}
	next = f;		
    }
    
    private File pop() {
	if (stack.length == 0)
	    return null;
	File[] ns = new File[stack.length-1];
	System.arraycopy(stack, 0,
			 ns, 0,
			 stack.length-1);
	File ret = stack[stack.length-1];
	stack = ns;
	return ret;
    }

    private void push(File[] f) {
	File[] ns = new File[stack.length+f.length];
	System.arraycopy(stack, 0,
			 ns, 0,
			 stack.length);
	System.arraycopy(f, 0,
			 ns, stack.length,
			 f.length);
	stack = ns;  	
    }

    private void push(File f) {
	File[] ns = new File[stack.length+1];
	System.arraycopy(stack, 0,
			 ns, 0,
			 stack.length);
	ns[stack.length] = f;
	stack = ns;  	
    }

} // end of DirectoryScanner
