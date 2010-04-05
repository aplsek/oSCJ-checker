package s3.services.bytecode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ovm.core.repository.Bundle;
import ovm.core.repository.Repository;
import ovm.core.repository.RepositoryClass;
import ovm.services.bytecode.reader.Parser;
import ovm.util.ByteBuffer;
import ovm.util.CommandLine;
import ovm.util.DirectoryScanner;
import ovm.util.FileIterator;
import ovm.util.OVMError;
import ovm.core.OVMBase;
import ovm.services.io.ResourcePath;
import ovm.core.services.format.JavaFormat;
import ovm.core.repository.TypeName;
import ovm.core.domain.LinkageException;

/**
 * @author KP
 * @author Christian Grothoff
 **/
public abstract class S3ClassProcessor extends OVMBase  {
    
    final protected Parser parser;

    private WorkQueue queue;
    private ParseWorkQueue pqueue;
    private Bundle bundle;
    
    ovm.services.bytecode.reader.Services bS;

    public S3ClassProcessor(String fileReaderPath) {
	this.bS = ovm.services.bytecode.reader.Services.getServices();
	bundle = Repository._.makeBundle(new ResourcePath(fileReaderPath));
	this.parser = createParser(bS);
    }   

    public S3ClassProcessor() {
	this(".");
    }

    protected Parser createParser(ovm.services.bytecode.reader.Services bs) {
	return bs.getParserFactory().makeParser();
    }

    public void runOnCommandLineArguments(CommandLine cLine) 
	throws Exception {
	for (int i = cLine.argumentCount() - 1; i >= 0; i--) {
	    TypeName tn = JavaFormat._.parseTypeName(cLine.getArgument(i));
	    process(bundle.lookupClass(tn.asScalar()));
	}
    }


    /**
     * Run on a Directory
     * @param threadLevel number of additional threads to use for 
     *        parallelization; use -1 to explicitly separate parsing
     *        from processing.
     * @param pthreadLevel number of additional threads to use for
     *        parallelization of the parsing
     * @return if threadlevel==-1, the time the processing took
     *        will be returned (in milli-seconds)
     **/
    public long runOnDirectory(String directory,
			       boolean ignoreMalformed,
			       int threadLevel,
			       int pthreadLevel) throws Exception {
	//System.out.println("Running with " + threadLevel + " threads\n");
	Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	if (pthreadLevel < 1) 
	    return runOnDirectory(directory, ignoreMalformed, threadLevel);
	pqueue = new ParseWorkQueue();
	for (int i = 0; i < pthreadLevel; i++)
	    pqueue.createProcessThread();
	long _ = runOnDirectory(directory, ignoreMalformed, threadLevel);
	pqueue = null;
	return 0;
    }

    /**
     * Run on a Zip or Jar file
     * @param threadLevel number of additional threads to use for 
     *        parallelization; use -1 to explicitly separate parsing
     *        from processing.
     * @param pthreadLevel number of additional threads to use for
     *        parallelization of the parsing
     * @return if threadlevel==-1, the time the processing took
     *        will be returned (in milli-seconds)
     **/
    public long runOnZipFile(ZipFile zipFile,
			     boolean ignoreMalformed,
			     int threadLevel,
			     int pthreadLevel) throws Exception {
	//System.out.println("Running with " + threadLevel + " threads\n");
	Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	if (pthreadLevel < 1) 
	    return runOnZipFile(zipFile, ignoreMalformed, threadLevel);
	pqueue = new ParseWorkQueue();
	for (int i = 0; i < pthreadLevel; i++)
	    pqueue.createProcessThread();
	long _ = runOnZipFile(zipFile, ignoreMalformed, threadLevel);
	pqueue = null;
	return 0;
    }
    
    /**
     * Run on a directory.
     * @param threadLevel number of additional threads to use for 
     *        parallelization; use -1 to explicitly separate parsing
     *        from processing.
     * @return if threadlevel==-1, the time the processing took
     *        will be returned (in milli-seconds)
     **/
    public long runOnDirectory(String directory,
			       boolean ignoreMalformed,
			       int threadLevel) throws Exception {
	//System.out.println("Running with " + threadLevel + " threads\n");
	if (threadLevel == 0) {
	    runOnDirectory(directory, ignoreMalformed);
	    if (pqueue != null)
		pqueue.join();
	    return 0;
	}
	queue = new WorkQueue();
	for (int i=0;i<threadLevel;i++)
	    queue.createProcessThread();
	runOnDirectory(directory, ignoreMalformed);
	if (threadLevel == -1) {
	    long start = System.currentTimeMillis();
	    queue.createProcessThread();
	    if (pqueue != null)
		pqueue.join();
	    queue.join();	    
	    queue = null;
	    return System.currentTimeMillis() - start;
	} else {
	    if (pqueue != null)
		pqueue.join();
	    queue.join();
	    queue = null;
	    return 0; // not measured
	}
    }
   
    /**
     * Run on a Zip or Jar file
     * @param threadLevel number of additional threads to use for 
     *        parallelization; use -1 to explicitly separate parsing
     *        from processing.
     * @return if threadlevel==-1, the time the processing took
     *        will be returned (in milli-seconds)
     **/
    public long runOnZipFile(ZipFile zipFile,
			     boolean ignoreMalformed,
			     int threadLevel) throws Exception {
	//System.out.println("Running with " + threadLevel + " threads\n");
	if (threadLevel == 0) {
	    runOnZipFile(zipFile, ignoreMalformed);
	    if (pqueue != null)
		pqueue.join();
	    return 0;
	}
	queue = new WorkQueue();
	for (int i=0;i<threadLevel;i++)
	    queue.createProcessThread();
	runOnZipFile(zipFile, ignoreMalformed);
	if (threadLevel == -1) {
	    long start = System.currentTimeMillis();
	    queue.createProcessThread();
	    if (pqueue != null)
		pqueue.join();
	    queue.join();	    
	    queue = null;
	    return System.currentTimeMillis() - start;
	} else {
	    if (pqueue != null)
		pqueue.join();
	    queue.join();
	    queue = null;
	    return 0; // not measured
	}
    }
    
    protected long getTimeLimit() {
	return 0;
    }

    /**
     * Run on a directory
     **/
    public void runOnDirectory(String directory, 
			       boolean ignoreMalformed) throws Exception {
	FileIterator it = new DirectoryScanner(directory);
	FileIterator cit = new FileIterator.SuffixFilter(it, ".class");
	if (getTimeLimit() > 0)
	    it = new FileIterator.TimeFilter(cit,
					     getTimeLimit());
	else
	    it = cit;
	while (it.hasNext()) {
	    File f = it.next();
	    InputStream stream = new FileInputStream(f);
	    byte[] data = new byte[(int)f.length()];
	    // d("zip entry size " + data.length + " for " + entry);
	    
	    int read = 0;
	    do {
		read += stream.read(data, read, data.length - read);
	    } while (read > 0 && read < data.length);
	    
	    stream.close();
	    try {
		parseInternal(ByteBuffer.wrap(data));
	    } catch (LinkageException.ClassFormat e) {
		if (ignoreMalformed) {
		    d("malformed entry " + f + 
		      ". Reason: " + e.getMessage());
		    e.printStackTrace();
		    try {
			dumpFile(f.getAbsolutePath().replace('/', '.'), data);
		    } catch (IOException ioe) {
			throw new OVMError("oops " + ioe);
		    }
		    continue;
		} else {
		    throw e;
		}
	    }
	}
    }
        
    /**
     * Run on a Zip or Jar file     
     **/
    public void runOnZipFile(ZipFile zipFile, 
			     boolean ignoreMalformed) throws Exception {
	Enumeration i = zipFile.entries();
	while (i.hasMoreElements()) {
	    ZipEntry entry = (ZipEntry)i.nextElement();
	    if (entry.isDirectory() || !entry.getName().endsWith(".class"))
		continue;
	    if (entry.getTime() < getTimeLimit())
		continue;
	    InputStream stream = zipFile.getInputStream(entry);
	    byte[] data = new byte[(int)entry.getSize()];
	    // d("zip entry size " + data.length + " for " + entry);
	    
	    int read = 0;
	    do {
		read += stream.read(data, read, data.length - read);
	    } while (read > 0 && read < data.length);
	    
	    stream.close();
	    try {
		parseInternal(ByteBuffer.wrap(data));
	    } catch (LinkageException.ClassFormat e) {

		if (ignoreMalformed) {
		    d("malformed entry " + entry + 
		      ". Reason: " + e.getMessage());
		    e.printStackTrace();
		    try {
			dumpFile(entry.getName().replace('/', '.'), data);
		    } catch (IOException ioe) {
			throw new OVMError("oops " + ioe);
		    }
		    continue;
		} else {
		    throw e;
		}
	    }
	}
    }
    
    private void dumpFile(String name, byte[] data) throws IOException {
	File dumpFile = new File(name);
	FileOutputStream fos = new FileOutputStream(dumpFile);
	fos.write(data);
	fos.flush();
	fos.close();
    }
     
    /**
     * Run on a Zip or Jar file
     **/
    public void runOnZipFile(String zipFileName,
			     boolean ignoreMalformed) throws Exception {
	runOnZipFile(new ZipFile(zipFileName), ignoreMalformed);
    }
    
    /**
     * Run on a Zip or Jar file
     **/
    public long runOnZipFile(String zipFileName,
			     boolean ignoreMalformed,
			     int threadlevel) throws Exception {
	return runOnZipFile(new ZipFile(zipFileName),
			    ignoreMalformed,
			    threadlevel);
    }
    
    /**
     * Run on a Zip or Jar file
     **/
    public long runOnZipFile(String zipFileName,
			     boolean ignoreMalformed,
			     int threadlevel,
			     int pthreadlevel) throws Exception {
	return runOnZipFile(new ZipFile(zipFileName),
			    ignoreMalformed,
			    threadlevel,
			    pthreadlevel);
    }

   
    /**
     * Run on a Classpath
     **/
    public void runOnClasspath(String classpath,			       
			       boolean ignoreMalformed) throws Exception {
	String[] tokens = classpath.split(":");
	for (int i=0;i<tokens.length;i++) {
	    if (tokens[i].endsWith(".jar") ||
		tokens[i].endsWith(".zip") )
		runOnZipFile(new ZipFile(tokens[i]), ignoreMalformed);
	    else
		runOnDirectory(tokens[i], ignoreMalformed);
	}
    }
    
    public void runOnResourcePath(ResourcePath rpath, 
				  boolean ignoreMalformed) throws Exception {
	throw new UnsupportedOperationException();
    }


    /**
     * Run on a Classpath
     **/
    public long runOnClasspath(String classpath,
			       boolean ignoreMalformed,
			       int threadlevel) throws Exception {
	String[] tokens = classpath.split(":");
	long ret = 0;
	for (int i=0;i<tokens.length;i++) {
	    if (tokens[i].endsWith(".jar") ||
		tokens[i].endsWith(".zip") )
		ret += runOnZipFile(new ZipFile(tokens[i]), 
			     ignoreMalformed,
			     threadlevel);
	    else
		ret += runOnDirectory(tokens[i],
				    ignoreMalformed,
				    threadlevel);
	}
	return ret;
    }
    
    /**
     * Run on a Classpath
     **/
    public long runOnClasspath(String classpath,
			       boolean ignoreMalformed,
			       int threadlevel,
			       int pthreadlevel) throws Exception {
	String[] tokens = classpath.split(":");
	long ret = 0;
	for (int i=0;i<tokens.length;i++) {
	    if (tokens[i].endsWith(".jar") ||
		tokens[i].endsWith(".zip") )
		ret += runOnZipFile(new ZipFile(tokens[i]), 
				    ignoreMalformed,
				    threadlevel,
				    pthreadlevel);
	    else
		ret += runOnDirectory(tokens[i],
				       ignoreMalformed,
				       threadlevel,
				       pthreadlevel);
	}
	return ret;
    }

    private void processInternal(RepositoryClass cls) throws Exception {
	if (queue == null)
	    process(cls); // no threading
	else
	    queue.add(cls);
    }
    private void parseInternal(ByteBuffer bb) throws Exception {
	if (pqueue == null)
	    parse(bb);
	else
	    pqueue.add(bb);
    }
    private void parse(ByteBuffer bb) throws Exception {
	processInternal(parser.parse(bb, bb.remaining()));
    }
    
    /**
     * Override this to do whatever you want wit the class.
     **/
    public abstract void process(RepositoryClass cls) throws Exception;

    /**
     * Work queue for parallel processing of RepositoryClasses.
     * Beware, crazy multi-threading code ahead!
     *
     * @author Christian Grothoff
     **/
    class WorkQueue {
	
	boolean joining;
	int threadCount;	
	LL first;

	/**
	 * Add a class to the list of classes that need to be processed.
	 **/
	synchronized void add(RepositoryClass cls) {
	    first = new LL(first, cls);
	    notifyAll();
	}

	/**
	 * Get the next class from the worklist.
	 **/
	private RepositoryClass getNext() {
	    if (first == null)
		return null;
	    else {
		RepositoryClass ret = first.rc;
		first = first.next;
		return ret;
	    }
	}

	/**
	 * Block until the reader read another class or until
	 * we know that there are no more for sure.
	 **/
	private synchronized RepositoryClass next() {
	    RepositoryClass next = getNext();
	    while ( (false == joining) &&
		    (next == null) ) {
		try {
		    wait();
		} catch (InterruptedException ie) {
		    return null; // this should not happen...
		}
		next = getNext();
	    }
	    return next;
	}

	/**
	 * Create an additional processing thread
	 **/
	void createProcessThread() {
	    threadCount++;
	    new Thread() {
		public void run() {
		    while (true) {
			RepositoryClass rc = next();
			if (rc != null) {
			    try {
				process(rc);
			    } catch (Exception e) {
				threadDone(e);
				return;
			    }
			} else {
			    threadDone();
			    return;
			}
		    }
		}
	    }.start();
	}

	private Exception ex;
	private synchronized void threadDone(Exception e) {
	    if (this.ex != null) 
		this.ex = new DoubleException(e, ex);	    
	    else
		this.ex = e;
	    threadCount--;
	    notifyAll();
	}
	
	private synchronized void threadDone() {
	    threadCount--;
	    notifyAll();
	}

	/**
	 * Block until the queue is empty.
	 **/
	synchronized void join() throws Exception {	
	    joining = true;
	    notifyAll();
	    while (true) {
		if (threadCount == 0)
		    break;
		try {
		    wait();
		} catch (InterruptedException ie) {
		    if (this.ex != null)
			this.ex = new DoubleException(ie, ex);
		    else
			this.ex = ie;
		    break;
		}
	    }
	    if (ex != null) {
		Exception e = ex;
		this.ex = null; // clear flag!
		throw e;
	    }
	}

    } // end of WorkQueue


    /**
     * Work queue for parallel processing of ByteBuffers
     * Beware, crazy multi-threading code ahead!
     *
     * @author Christian Grothoff
     **/
    class ParseWorkQueue {
	
	boolean joining;
	int threadCount;	
	LL2 first;

	/**
	 * Add a class to the list of classes that need to be processed.
	 **/
	synchronized void add(ByteBuffer cls) {
	    first = new LL2(first, cls);
	    notifyAll();
	}

	/**
	 * Get the next class from the worklist.
	 **/
	private ByteBuffer getNext() {
	    if (first == null)
		return null;
	    else {
		ByteBuffer ret = first.rc;
		first = first.next;
		return ret;
	    }
	}

	/**
	 * Block until the reader read another class or until
	 * we know that there are no more for sure.
	 **/
	private synchronized ByteBuffer next() {
	    ByteBuffer next = getNext();
	    while ( (false == joining) &&
		    (next == null) ) {
		try {
		    wait();
		} catch (InterruptedException ie) {
		    return null; // this should not happen...
		}
		next = getNext();
	    }
	    return next;
	}

	/**
	 * Create an additional processing thread
	 **/
	void createProcessThread() {
	    threadCount++;
	    new Thread() {
		public void run() {
		    Parser px = createParser(bS);

		    while (true) {
			ByteBuffer rc = next();
			if (rc != null) {
			    try {
				processInternal(px.parse(rc, rc.remaining()));
			    } catch (Exception e) {
				threadDone(e);
				return;
			    }
			} else {
			    threadDone();
			    return;
			}
		    }
		}
	    }.start();
	}

	private Exception ex;
	private synchronized void threadDone(Exception e) {
	    if (this.ex != null) 
		this.ex = new DoubleException(e, ex);	    
	    else
		this.ex = e;
	    threadCount--;
	    notifyAll();
	}
	
	private synchronized void threadDone() {
	    threadCount--;
	    notifyAll();
	}

	/**
	 * Block until the queue is empty.
	 **/
	synchronized void join() throws Exception {	
	    joining = true;
	    notifyAll();
	    while (true) {
		if (threadCount == 0)
		    break;
		try {
		    wait();
		} catch (InterruptedException ie) {
		    if (this.ex != null)
			this.ex = new DoubleException(ie, ex);
		    else
			this.ex = ie;
		    break;
		}
	    }
	    if (ex != null) {
		Exception e = ex;
		this.ex = null; // clear flag!
		throw e;
	    }
	}

    } // end of ParseWorkQueue


    static class DoubleException extends Exception {
	Exception e1;
	Exception e2;
	DoubleException(Exception e1, Exception e2) {
	    this.e1 = e1;
	    this.e2 = e2;
	}
	public String toString() {
	    return e1.toString() + " AND " + e2.toString();
	}
    }

    static class LL {
	RepositoryClass rc;
	LL next;
	LL(LL next, RepositoryClass rc) {
	    this.rc = rc;
	    this.next = next;
	}
    }
    static class LL2 {
	ByteBuffer rc;
	LL2 next;
	LL2(LL2 next, ByteBuffer rc) {
	    this.rc = rc;
	    this.next = next;
	}
    }

} // end of S3ClassProcessor
