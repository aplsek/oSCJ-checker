/**
 * Test simple field accesses.
 **/
package test;


/**
 * Tests field access
 * @author James Liang
 * @author Christian Grothoff
 **/
public class TestFieldAccess 
    extends TestBase {

    public TestFieldAccess(Harness domain) {
	super("FieldAccess", domain);
    }

    public void run() {
	testFieldRead();
	testFieldWrite();
	testArrayRead();
 	testArrayWrite();
   }

    public void testFieldRead() {
	setModule("field read");
	COREassert(i == 1, "one");
	COREassert(z,"boolean");
	COREassert(l == null, " null");
	COREassert(b == (byte)211, "byte");
	COREassert(c == (char)32210, "char");
	COREassert(s == (short)-15125, "short");
	COREassert(f == (float)5.41, "float");
	COREassert(j == (long) 0x12345678ABCDEF00L, "long");
	COREassert(d == (double)789459756.112688d, "double");
	COREassert(uninitializedInt == 0, "uninitialized");
    }

    public void testFieldWrite() {
	setModule("field write");
	i = 179; 
	COREassert(i ==179);
	z = true;
	COREassert(z);
	l = this; 
	COREassert(l==this);	
	b = 122; 
	COREassert(b==122);	
	c = 'b';  
	COREassert(c=='b');
	s = -7891; 
	COREassert(-7891==s);
	f = 17.9f; 
	COREassert(f==17.9f);
	j = 629347912201l; 
	COREassert(j==629347912201l);
    }

    public void testArrayRead() {
	setModule("array read");
	COREassert(myFloatArray[0] == 12.5f);
	COREassert(myFloatArray[1] == 15.1f);
	COREassert(myFloatArray[2] == -19.4f);
	COREassert(objectArray[0]==this);
	COREassert(objectArray[1]==myByteArray);
	COREassert(objectArray[2]==myIntArray);
	COREassert(objectArray[3]==myCharArray);
	COREassert(objectArray[4]==null);
	COREassert(myByteArray[0]==10);
	COREassert(myByteArray[1]==111);
	COREassert(myByteArray[2]==25);
	COREassert(myByteArray[4]==49);
	COREassert(myIntArray[0]==101);
	COREassert(myIntArray[1]==-1121);
	COREassert(myIntArray[2]==235);
	COREassert(myIntArray[4]==-549);
	COREassert(myCharArray[0]==610);
	COREassert(myCharArray[1]==1711);
	COREassert(myCharArray[2]==285);
	COREassert(myCharArray[4]==0xffff);
	COREassert(initializedShortArray[0]==(short)11115);
	COREassert(myDoubleArray[0] == 789456.456);
	COREassert(myDoubleArray[1] == 0);
	COREassert(myDoubleArray[2] ==  -17894.156);
	COREassert(myLongArray[0] == 1);
	COREassert(myLongArray[1] == 9461820132l);
	COREassert(myLongArray[2] == 0xffffffffffffffffl);
	COREassert(initializedShortArray[1]==(short)0);
	COREassert(initializedShortArray[3]==(short)144);
	COREassert(initializedShortArray[4]==(short)-915);
	COREassert(initializedShortArray[10]==(short)5);
	COREassert(initializedShortArray[11]==(short)1);
    }
    
    public void testArrayWrite() {
	setModule("array write");
	for (int i = 0;i<6;i++)
	    charArray[i] = (char)(12345*i);
	for (int i = 0;i<6;i++)
	    COREassert (charArray[i] ==(char)(12345*i));
	
	objArray[3] = this; 
	COREassert (objArray[3]==this);	
	boolArray[5] = true; 
	COREassert (boolArray[5]==true);	
	boolArray[6] = false; 
	COREassert (boolArray[6]==false);	
	boolArray[7] = true; 
	COREassert (boolArray[7]==true);	
	boolArray[8] = true; 
	COREassert (boolArray[8]==true);	
	boolArray[9] = false; 
	COREassert (boolArray[9]==false);	
	boolArray[0] = false; 
	COREassert (boolArray[0]==false);
	
	
	for (int i = 0;i<6;i++)
	    longArray[i] = (193567594l*i);
	for (int i = 0;i<6;i++)
	    COREassert (longArray[i] ==(193567594l*i));	
	for (int i = 0;i<6;i++)
	    byteArray[i] = (byte)(9*i);
	for (int i = 0;i<6;i++)
	    COREassert (byteArray[i] ==(byte)(9*i));
	for (int i = 0;i<6;i++)
	    intArray[i] = 219145332*i;	 
	for (int i = 0;i<6;i++)
	    COREassert (intArray[i] ==219145332*i);	
	for (int i = 0;i<6;i++)
	    doubleArray[i] = (919356.474594d*i);
	for (int i = 0;i<6;i++)
	    COREassert (doubleArray[i] ==(919356.474594d*i));	
	for (int i = 0;i<6;i++)
	    floatArray[i] = (19356.7594f*i);
	for (int i = 0;i<6;i++)
	    COREassert (floatArray[i] ==(19356.7594f*i));	
	for (int i = 1;i<6;i++)
	    shortArray[i] = (short)(4099*i);
	// reading to check.  This also checking the coupling between reading and writing
	for (int i = 0;i<6;i++)
	    COREassert (shortArray[i] ==(short)(4099*i));		
	for (int i = 0;i<6;i++)
	    shortArray[i] = (short)(-5201*i);
	for (int i = 0;i<6;i++)
	    COREassert (shortArray[i] ==(short)(-5201*i));
    }



    int uninitializedInt;
    long unitializedLong;
    boolean [] boolArray = new boolean[10];
    int i=1;
    float f=(float)5.41;
    boolean z=true;
    long j = 0x12345678ABCDEF00L;
    double d=789459756.112688; 
    Object l=null;
    byte b=(byte)211;
    char c=(char)32210;
    short s=(short)-15125;
    Object [] objArray = new Object[6];
    byte [] byteArray = new byte[6];
    char [] charArray = new char[6];
    short [] shortArray = new short[6];
    int [] intArray = new int[6];
    float [] floatArray = new float[6];
    long [] longArray = new long[6];
    double [] doubleArray = new double[6];
    short [] initializedShortArray = {(short)11115, //0
				      (short)0, //1
				      (short)-9, //2
				      (short)144, // 3
				      (short)-915, //4
				      (short)10000, //5
				      (short)-9721, // 6
				      (short)4201, //7
				      (short)0, // 8
				      (short)4, // 9
				      (short)5, // 10
				      (short)1, // 11
    };    
    long [] myLongArray = {1l,
			   9461820132l,
			   0xffffffffffffffffl,
    };
    byte [] myByteArray = {(byte)10,
			   (byte)111,
			   (byte)25,
			   (byte)3,
			   (byte)49};
    int [] myIntArray = {(int)101,
			   (int)-1121,
			   (int)235,
			   (int)43,
			   (int)-549};
    char [] myCharArray = {(char)610,
			   (char)1711,
			   (char)285,
			   (char)39,
			   (char)0xffff};
    double [] myDoubleArray = {789456.456,
			       0,
			       -17894.156};

    Object [] objectArray = {this,
			     myByteArray,
			     myIntArray,
			     myCharArray,
			     null};

    float [] myFloatArray = {12.5f,
			     15.1f,
			     -19.4f};


    
} // end of TestFieldAccess

