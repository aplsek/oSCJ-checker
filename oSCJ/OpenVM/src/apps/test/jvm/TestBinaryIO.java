package test.jvm;

import java.io.*;
import java.lang.reflect.*;
/**
 * The binary IO test writes a set of predefined arrays of each primitive type
 * to a data output stream attached to a byte[] and then reads the data back 
 * from the byte[] using a data input stream and checks the results with the
 * original data.
 * <p>Two nested classes allow for additional functionality. 
 * The <tt>Writer</tt> class has a main method that writes all the data to a 
 * set of files.
 * The <tt>Reader</tt> class has a main method that reads the files and 
 * compares the results against the original data. By using the <tt>Reader</tt>
 * and <tt>Writer</tt> with different VM's (possibly on different platforms,
 * with the data files transferred between them) the inter-operability aspects
 * of the data streams can be tested.
 *
 * @author David Holmes
 */
public class TestBinaryIO extends TestBase {

    // the predefined data sets
    static byte[] bytes;
    static char[] chars;
    static short[] shorts;
    static int[] ints;
    static long[] longs;
    static float[] floats;
    static double[] doubles;

    // semi-random initialization. The shifts are present to ensure we
    // use more of the available bits in each type. Better coverage is
    // always possible, of course
    static {
        bytes = new byte[Byte.MAX_VALUE-Byte.MIN_VALUE+1];
        for (int i = 0, val =  Byte.MIN_VALUE; i < bytes.length; i++, val++)
            bytes[i] = (byte)val;


        chars = new char[500];
        for (int i = 0, val =  -(chars.length/2); i < chars.length; i++, val++)
            chars[i] = (char) val;

        shorts = new short[500];
        for (int i = 0, val =  -(shorts.length/2); i < shorts.length; i++, val++)
            shorts[i] = (short) val;

        ints = new int[500];
        for (int i = 0, val =  -(ints.length/2); i < ints.length; i++, val++)
            ints[i] = val << 16;

        longs = new long[500];
        for (int i = 0, val =  -(longs.length/2); i < longs.length; i++, val++)
            longs[i] = val << 32;

        doubles = new double[500];
        double seed = 3.4145;
        for (int i = 0; i < doubles.length; i++)
            doubles[i] = seed*i*i;

        floats = new float[500];
        for (int i = 0; i < doubles.length; i++)
            floats[i] = ((float)seed)*i*i;
    }

    /**
     * Holder to allow common procesing
     */
    static class Values {
        String filename;
        char tag;
        Object dataArray;
        int size;
        
        Values(Object data, String f, char t) {
            dataArray = data;
            filename = f;
            tag = t;
            size = Array.getLength(data);
        }
    }

    /** The set of data, with filename and tag information */
    static Values[] data = new Values[] {
        new Values(bytes, "byteIOTest.dat", 'B'),
        new Values(shorts, "shortIOTest.dat", 'S'),
        new Values(chars, "charIOTest.dat", 'C'),
        new Values(ints, "intIOTest.dat", 'I'),
        new Values(longs, "longIOTest.dat", 'L'),
        new Values(floats, "floatIOTest.dat", 'F'),
        new Values(doubles, "doubleIOTest.dat", 'D'),
    };


    /**
     * Write the given array out to the given stream as the type
     * specified by <tt>tag</tt>.
     * @param os the data output stream to read from
     * @param data the array of elements to write
     * @param tag the tag indicating the kind of primitive data in the array
     * (B=byte, C=char, S=short, I=int, L=long; F=float, D=double)
     */
    static void writeData(DataOutputStream os, Object data, char tag) 
        throws Exception {
        switch (tag) {
        case 'B': {
            byte[] values = (byte[]) data;
            for (int i = 0; i < values.length; i++)
                os.writeByte(values[i]);
            break;
        }

        case 'C': {
            char[] values = (char[]) data;
            for (int i = 0; i < values.length; i++)
                os.writeChar(values[i]);
            break;
        }

        case 'S': {
            short[] values = (short[]) data;
            for (int i = 0; i < values.length; i++)
                os.writeShort(values[i]);
            break;
        }

        case 'I': {
            int[] values = (int[]) data;
            for (int i = 0; i < values.length; i++)
                os.writeInt(values[i]);
            break;
        }

        case 'L': {
            long[] values = (long[]) data;
            for (int i = 0; i < values.length; i++)
                os.writeLong(values[i]);
            break;
        }

        case 'F': {
            float[] values = (float[]) data;
            for (int i = 0; i < values.length; i++)
                os.writeFloat(values[i]);
            os.close();
            break;
        }

        case 'D': {
            double[] values = (double[]) data;
            for (int i = 0; i < values.length; i++)
                os.writeDouble(values[i]);
            break;
        }
        }
        os.close();
    }


    /**
     * Read the number of primitive data elements 
     * (as identified by the tag) from the given DataInputStream.
     * @param is the data input stream to read from
     * @param elems the number of data elements to read
     * @param tag the tag indicating the kind of primitive data
     * (B=byte, C=char, S=short, I=int, L=long; F=float, D=double)
     *
     * @return an array of length <tt>elems</tt> holding the read values, of
     * a type determined by the tag
     */
    static Object readData(DataInputStream is, int elems , char tag) 
        throws Exception {
        switch (tag) {
        case 'B': {
            byte[] values = new byte[elems];
            for (int i = 0; i < values.length; i++)
                values[i] = is.readByte();
            return values;
        }

        case 'C': {
            char[] values = new char[elems];
            for (int i = 0; i < values.length; i++)
                values[i] = is.readChar();
            return values;
        }

        case 'S': {
            short[] values = new short[elems];
            for (int i = 0; i < values.length; i++)
                values[i] = is.readShort();
            return values;
        }

        case 'I': {
            int[] values = new int[elems];
            for (int i = 0; i < values.length; i++)
                values[i] = is.readInt();
            return values;
        }

        case 'L': {
            long[] values = new long[elems];
            for (int i = 0; i < values.length; i++)
                values[i] = is.readLong();
            return values;
        }

        case 'F': {
            float[] values = new float[elems];
            for (int i = 0; i < values.length; i++)
                values[i] = is.readFloat();
            return values;
        }

        case 'D': {
            double[] values = new double[elems];
            for (int i = 0; i < values.length; i++)
                values[i] = is.readDouble();
            return values;
        }

        default: throw new Error("Incorrect tag " + tag);
        }
    }


    public TestBinaryIO(Harness domain) {
        super("DataInputStream and DataOutputStream Tests", domain);
    }

    /**
     * No-arg constructor for use with standalone execution outside of
     * the test harness.
     */
    public TestBinaryIO() {
        super("DataInputStream and DataOutputStream Tests", 
              new Harness() {
                public void print(String s) {
                    System.out.print(s);
                }
                public String getDomain() {
                    return "user-domain-JVM";
                }
                public void exitOnFailure() {
                    if (failures > 0) {
                        System.exit(failures);
                    }
                }
            });

    }


    /**
     * Runs the test as a stand-alone application.
     */
    public static void main(String[] args) {
        System.out.println("TestBinaryIO started");
        TestBinaryIO tester = new TestBinaryIO();
        //        tester.verbose = true;
        tester.run();
        System.out.println("TestBinaryIO done");
    }

    public void run() {
        try {
            for (int i = 0; i < data.length; i++) {
                setModule("Data type: " + data[i].tag);
                if (verbose)
                    verbose_p("Processing tag " + data[i].tag);

                // write the known data to an array via a stream
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                DataOutputStream os = new DataOutputStream(bs);
                writeData(os, data[i].dataArray, data[i].tag);

                os.close();

                // read via a stream from that array
                byte[] buf = bs.toByteArray();
                DataInputStream is = new DataInputStream(new ByteArrayInputStream(buf));
                    
                Object tmp = readData(is, data[i].size, data[i].tag);

                is.close();

                // compare the read value with the original
                checkArrays(tmp, data[i].dataArray, data[i].tag);
            }
        }
        catch(Throwable t) {
            fail(t);
        }
    }


    void checkArrays(Object read, Object expected, char tag) {
        if (verbose)
            System.out.println("Processing tag " + tag);
        switch(tag) {
        case 'B': {
            byte[] a = (byte[]) read;
            byte[] b = (byte[]) expected;
            for (int i = 0; i < a.length; i++) {
                String msg = "read " + a[i] + " expected " + b[i];
                check_condition(a[i] == b[i],
                                "Mismatch at index " + i + ": " + msg );
                if (verbose)
                    System.out.println(msg);
            }
            break;
        }

        case 'S': {
            short[] a = (short[]) read;
            short[] b = (short[]) expected;
            for (int i = 0; i < a.length; i++) {
                String msg = "read " + a[i] + " expected " + b[i];
                check_condition(a[i] == b[i],
                                "Mismatch at index " + i + ": " + msg );
                if (verbose)
                    System.out.println(msg);
            }
            break;
        }

        case 'C': {
            char[] a = (char[]) read;
            char[] b = (char[]) expected;
            for (int i = 0; i < a.length; i++) {
                String msg = "read " + (int)a[i] + " expected " + (int)b[i];
                check_condition(a[i] == b[i],
                                "Mismatch at index " + i + ": " + msg );
                if (verbose)
                    System.out.println(msg);
            }
            break;
        }

        case 'I': {
            int[] a = (int[]) read;
            int[] b = (int[]) expected;
            for (int i = 0; i < a.length; i++) {
                String msg = "read 0x" + Integer.toHexString(a[i]) + 
                    " expected 0x" + Integer.toHexString(b[i]);
                if (verbose)
                    System.out.println(msg);
            }
            break;
        }

        case 'L': {
            long[] a = (long[]) read;
            long[] b = (long[]) expected;
            for (int i = 0; i < a.length; i++) {
                String msg = "read 0x" + Long.toHexString(a[i]) + 
                    " expected 0x" + Long.toHexString(b[i]);
                if (verbose)
                    System.out.println(msg);
            }
            break;
        }

        case 'F': {
            float[] a = (float[]) read;
            float[] b = (float[]) expected;
            for (int i = 0; i < a.length; i++) {
                String msg = "read 0x" + Integer.toHexString(Float.floatToIntBits(a[i])) + 
                    " expected 0x" + Integer.toHexString(Float.floatToIntBits(b[i]));
                if (verbose)
                    System.out.println(msg);
            }
            break;
        }

        case 'D': {
            double[] a = (double[]) read;
            double[] b = (double[]) expected;
            for (int i = 0; i < a.length; i++) {
                String msg = "read 0x" + Long.toHexString(Double.doubleToLongBits(a[i])) + 
                    " expected 0x" + Long.toHexString(Double.doubleToLongBits(b[i]));
                if (verbose)
                    System.out.println(msg);
            }
            break;
        }


        default: throw new Error("incorrect tag " + tag);
        }
    }


    /**
     * Writes the data to a set of files
     */
    static class Writer {
        public static void main(String[] args) throws Exception {
            for ( int i = 0; i < data.length; i++) {
                Values v = data[i];
                FileOutputStream fs = new FileOutputStream(v.filename);
                DataOutputStream os = new DataOutputStream(fs);
                writeData(os, v.dataArray, v.tag);
                os.close();
            }
        }
    }

    /**
     * Reads the data from the file and compares against expected values
     */
    static class Reader {
        public static void main(String[] args) throws Exception {
            TestBinaryIO tester = new TestBinaryIO();            
            //tester.verbose = true;
            for ( int i = 0; i < data.length; i++) {
                Values v = data[i];
                FileInputStream fs = new FileInputStream(v.filename);
                DataInputStream is = new DataInputStream(fs);
                Object tmp = readData(is, v.size, v.tag);
                is.close();
                tester.checkArrays(tmp, v.dataArray, v.tag);                
            }
        }
    }


}




        
