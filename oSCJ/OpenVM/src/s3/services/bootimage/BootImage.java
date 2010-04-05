package s3.services.bootimage;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import ovm.core.execution.NativeConstants;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.util.ByteBuffer;
import ovm.util.logging.Logger;
import s3.core.domain.MachineSizes;
import ovm.core.domain.LinkageException;

/**
 * Representation of the BootImage header structure, whose existence
 * is as much a historical oddity as a feature.<p>
 *
 * 9/10/02: Refactored BootImage to remove a number of inner classes and
 * simplify the code. Naming scheme as well as separation of duties between
 * this class and the driver have been revisited.
 * NB: the code for generating a relocation bitmap has been removed, since
 *     we assume that mmap can load the image at the proper base address.<p>
 *
 * 9/17/02: WARNING: The boot image is now a singleton.
 *
 * @author Grothoff, Vitek
 **/
public final class BootImage extends BootBase {
    /**
     * Three headers are defined here, 8 more in {@link
     * DomainSprout#DomainSprout(ExecutiveDomain)},
     * and possibly one more in {@link
     * s3.services.simplejit.bootimage.SimpleJITBootImageCompileObserver}.
     * For a long time, the header size was computed on demand.  That
     * introduced too many bootstrapping dependencies.<p>
     **/
    public static final int N_HEADER_FIELDS = 13;
    public static final int HEADER_SIZE =
	N_HEADER_FIELDS * MachineSizes.BYTES_IN_WORD;

    /** Current version number.  **/
    private static final Integer VERSION = new Integer(0x00010000);

    ///** Size of the image header in bytes (up to length field) (will need
    // * to be increased to 36 when base address is included)  **/
    //private static final int MAGICLENGTH = 4;
    //private static final int VERSIONLENGTH = 4;

    /** The magic number.  **/
    private static final Integer MAGIC = new Integer(0x494E2086);


    /** The memory that we are dumping.  **/
    final ByteBuffer memory_;

    private static BootImage thisBootimage;

    /** The HeaderObjects used to initialize the image header section.  **/
    private HeaderObject[] headers_ = new HeaderObject[N_HEADER_FIELDS];

     final HeaderObject magicHeader_ =  addHeader("OVM_MAGIC",   MAGIC);
     final HeaderObject versionHeader_ = addHeader("OVM_VERSION", VERSION);
     final HeaderObject usedMemoryHeader_ = addHeader("usedMemory",
						      new Integer(0));

    // -------------- Constructor ------------------------------

    public BootImage(ByteBuffer mem) {
        assert(thisBootimage == null);
        this.memory_ = mem;
        thisBootimage = this;
    }

    public static BootImage the() {
	assert(thisBootimage != null);
	return thisBootimage;
    }

    // --------------- instance methods ------------------------

    final HeaderObject addHeader(String name, Object target) {
        HeaderObject header = new HeaderObject(target, name);
        assert( headers_ != null );
	for (int i = 0; i < headers_.length; i++) {
	    if (headers_[i] == null) {
		headers_[i] = header;
		return header;
	    }
	}
	throw new Error(getClass().getName() + ".N_HEADER_FIELDS exceeded");
    }

    public Object getHeader(String name) {
	for (int i = 0; i < headers_.length; i++) {
	    if (headers_[i] != null && headers_[i].name.equals(name))
		return headers_[i].target;
	}
	return null;
    }

    // *********************** Inner classes **************************

    /** Class representing fields in the image header.  **/
    /**
     * Keeps track of header entries which can be either addresses or
     * integer values. In previous design we had three classes, an abstract
     * Entry and two concrete classes. But apart from the added complexity
     * there was little gain. [Yes this version has its warts.]
     **/
    private class HeaderObject extends BootBase {
        final private String name;
        private boolean isPrimitive = false;
        private Object target;

        HeaderObject(Object o, String n) {
            name = n;
            target = o; // an Int indicates a boxed integer ...
            if (o instanceof Integer) isPrimitive = true;
        }

        final boolean debug = true;

        // we are writing sequentially in the begining of the image
        // references to later objects.
        void write(ByteBuffer buf) {
            if ( ! isPrimitive ) {
                VM_Address address;
		try {
		    address = VM_Address.fromObject(target);
		} catch (LinkageException.Runtime e) {
		    System.out.println("null header value for " + name +
				       ": " + e.getMessage());
		    address = VM_Address.fromObject(null);
		}
                int addressAsInt = address.asInt();
                if ( addressAsInt == 0 )  {
		    buf.position(buf.position() + VM_Address.widthInBytes());
		    return;
		}
                buf.putInt( addressAsInt);
                if (debug) {
                    if (name.equals("mainMethod"))
                        Logger.global.finer(" Main method location: 0x"
		         + Integer.toHexString( addressAsInt));
                    if (name.equals("mainObject")) {
//                         Logger.global.finer(" Image Base Address: 0x"
//                           + Integer.toHexString(  baseAddress()));
                        Logger.global.finer(" Main object location: 0x"
                          + Integer.toHexString(addressAsInt));
                    }
                }
            } else {
                buf.putInt( ((Integer) target).intValue());
            }
        }
        String toCField() {
            return ((!isPrimitive) ? "void * " : "int ") + name + ";";
        }
        void setValue(int val) {
            assert(isPrimitive);
            target = new Integer(val);
        }
    } // End of HeaderObject

    /** Save the image to file. **/
    public void save(final String fname) 
        throws IOException {

	final OutputStream os = new FileOutputStream(fname);	
        final DataOutputStream dataOutStream =
            new DataOutputStream(new BufferedOutputStream(os));
        final int byteOrder_ = memory_.order();

        usedMemoryHeader_.setValue( getImageSize() );

	final ByteBuffer headerBuf =
	    ByteBuffer.allocate(headers_.length * MachineSizes.BYTES_IN_WORD);
	headerBuf.order(byteOrder_);
	
	for ( int i = 0; i < headers_.length ; i++ ) {
	    if (headers_[i] != null)
		headers_[i].write(headerBuf);
	    else
		headerBuf.putInt(0);
	}

	writeBufferToOutput(headerBuf, dataOutStream);
        writeBufferToOutput(memory_, dataOutStream);

        // sanity check at end of image
	ByteBuffer magic =  ByteBuffer.allocate(MachineSizes.BYTES_IN_WORD);
	magic.order(byteOrder_);
	magic.putInt(MAGIC.intValue());
	writeBufferToOutput(magic, dataOutStream);

	// Pad out to page boundary.
	int wholeSize = getWholeSize();
	while (wholeSize > 8) {
	    dataOutStream.writeLong(0);
	    wholeSize -= 8;
	}
	while (wholeSize --> 0)
	    dataOutStream.write(0);

	dataOutStream.flush();
	dataOutStream.close();
    }

    int getWholeSize() {
	int totalUsed = (MachineSizes.BYTES_IN_WORD * (headers_.length + 1)
			 + memory_.position());
	int totalSize = ((totalUsed + NativeConstants.PAGE_SIZE - 1)
			 & ~(NativeConstants.PAGE_SIZE - 1));
	int ret = totalSize - totalUsed;
	System.err.println("Padding image by " +  ret + " to " + totalSize);
	return ret;
    }
	
    /** Get the size of the image (total filesize - header size) in bytes **/
    int getImageSize() {
        return (memory_.position()
		+ MachineSizes.BYTES_IN_WORD);
    }

    /** Write the buffer up to and including the current byte to the data
     * output stream. Leaves the buffer unchanged. **/
    static private void writeBufferToOutput(ByteBuffer buf, DataOutput dos)
        throws IOException {
	dos.write(buf.array(), buf.arrayOffset(), buf.position());
    }

    public void writeHeadersToC_h_File(PrintWriter file) {
        file.print("#define IMAGE_MAGIC 0x"+
                   Integer.toHexString(MAGIC.intValue())
                   +"\n typedef struct {\n");
        for (int i = 0; i < headers_.length; i++)
	    if (headers_[i] == null)
		file.print("\tint reserved" + i + ";\n");
	    else
		file.print("\t" + headers_[i].toCField() +"\n");
        file.print("\t char data[0];\n} ImageFormat;\n");
    }
} // end of BootImage
