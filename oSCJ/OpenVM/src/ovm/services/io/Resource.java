package ovm.services.io;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ovm.core.OVMBase;
import ovm.core.services.io.BasicIO;
import ovm.util.ByteBuffer;
import ovm.util.NumberRanges;
import ovm.util.OVMError;
import ovm.util.OVMException;
import s3.services.bootimage.Ephemeral;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;


/**
 * Resource is a data stream that can be found in a file system as a file or in
 * some bigger archive such as a jar or zip file. Classes and property
 * files are examples of resources. Java class loaders have a similar
 * notion of resources. Resources tend to be small.
 * @author Krzysztof Palacz
 **/

public abstract class Resource extends OVMBase {
    public abstract InputStream getContentsAsStream() throws IOException;

    public ByteBuffer getContents() throws OVMException.IO {
	try {
	    DataInputStream dis = new DataInputStream(getContentsAsStream());
	    byte[] bytes = new byte[NumberRanges.asInt(getSize())];
	    dis.readFully(bytes);
	    dis.close();
	    return ByteBuffer.wrap(bytes);
	} catch (IOException e) {
		throw new OVMException.IO(e);
	}
    }

    /**  @return canonical path to the class file. **/
    public abstract String getPath();
    /**  @return modification time of the class file. **/
    public abstract long getTime();
    /** @return size of the class file. **/
    public abstract long getSize();
    
    public static interface Action {
        void process(Resource resource) throws OVMException, BCbootTime;
    }


    static class HostedFile extends Resource implements Ephemeral.Void {
	File root;
	File file;
	public HostedFile(File root, File file) {
	    this.root = root;
	    this.file = file;
	}
	public InputStream getContentsAsStream() throws IOException, BCbootTime {
	    return new FileInputStream(file);
	}

	
	public String getPath() {
	    try {
		String rcannon = root.getCanonicalPath();
		String fcannon = file.getCanonicalPath();
		assert(fcannon.startsWith(rcannon));
		return fcannon.substring(rcannon.length() + 1,
					 fcannon.length());
	    } catch (IOException e) {
		throw new OVMException.IO(e).unchecked();
	    }
	}
	public long getTime() {
	    return file.lastModified();
	}
	public long getSize() {
	    return file.length();
	}
    }


    public static class StandaloneFile extends Resource {
        private String file;
	ByteBuffer contents;
        public StandaloneFile(String file) {
            assert(file != null); // failfast
            this.file = file;
        }

	public ByteBuffer getContents() throws OVMException.IO {
	    if (contents == null) {
		contents = BasicIO.contents(file);
	    }
	    return contents;
	}

	public String getPath() {
	    return file;
	}
	public InputStream getContentsAsStream() {
	    throw new OVMError.Unimplemented();
	}
	public long getTime() {
	    throw new OVMError.Unimplemented();
	}
	public long getSize() {
	    throw new OVMError.Unimplemented();
	}
    }


    static class HostedZippedFile extends Resource implements Ephemeral.Void {
        private ZipFile zip;
	private ZipEntry entry;
        HostedZippedFile(ZipFile zip, ZipEntry entry) {
	    this.zip = zip;
	    this.entry = entry;
        }
	
	public InputStream getContentsAsStream() throws IOException {
	    return zip.getInputStream(entry);
	}
	
	public String getPath() {
	    return entry.toString();
	}
	public long getTime() {
	    return entry.getTime();
	}
	public long getSize() {
	    return entry.getSize();
	}
    }
}
