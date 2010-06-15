package ovm.util;

import java.io.*;

/**
 * Simple iterator over files.
 *
 * @author Christian Grothoff
 **/
public interface FileIterator {

    public boolean hasNext();
    public File next();

    /**
     * Filter that filters files that do not have the
     * right suffix.
     *
     * @author Christian Grothoff
     **/
    public static class SuffixFilter 
	implements FileIterator {
	private final String suffix;
	private File next;
	private final FileIterator iter;

	public SuffixFilter(FileIterator iter,
			    String suffix) {
	    this.iter = iter;
	    this.suffix = suffix;
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
	    next = null;
	    while (iter.hasNext()) {
		next = iter.next();
		if (next.getAbsolutePath().endsWith(suffix))
		    break;
		next = null;
	    }
	}

    } // end of FileIterator.SuffixFilter

 
    /**
     * Filter that filters files that do not have been changed after a
     * certain time.
     *
     * @author Christian Grothoff
     **/
    public class TimeFilter 
	implements FileIterator {
	private final long timestamp;
	private final FileIterator iter;
	private File next;

	public TimeFilter(FileIterator iter,
			  long timestamp) {
	    this.iter = iter;
	    this.timestamp = timestamp;
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
	    next = null;
	    while (iter.hasNext()) {
		next = iter.next();
		if (next.lastModified() > timestamp) 
		    break;
		next = null;
	    }
	}

    } // end of FileIterator.TimeFilter

} // end of FileIterator
