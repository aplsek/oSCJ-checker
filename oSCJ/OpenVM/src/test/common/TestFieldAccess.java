/**
 * Test simple field accesses.
 **/
package test.common;


/**
 * Tests field access
 * @author James Liang
 * @author Christian Grothoff
 **/
public class TestFieldAccess 
    extends TestBase {

    public TestFieldAccess() {
	super("FieldAccess");
    }

    public void run() {
	testFieldRead();
	testFieldWrite();
	testArrayRead();
 	testArrayWrite();
   }

    public void testFieldRead() {
	setModule("field read");
	check_condition(i_ == 1, "one");
	check_condition(z,"boolean");
	check_condition(l == null, " null");
	check_condition(b == (byte)211, "byte");
	check_condition(c == (char)32210, "char");
	check_condition(s == (short)-15125, "short");
	check_condition(f == (float)5.41, "float");
	check_condition(j == 0x12345678ABCDEF00L, "long");
	check_condition(d == 789459756.112688d, "double");
	check_condition(uninitializedInt == 0, "uninitialized");
    }

    public void testFieldWrite() {
	setModule("field write");
	i_ = 179; 
	check_condition(i_ ==179);
	z = true;
	check_condition(z);
	l = this; 
	check_condition(l==this);	
	b = 122; 
	check_condition(b==122);	
	c = 'b';  
	check_condition(c=='b');
	s = -7891; 
	check_condition(-7891==s);
	f = 17.9f; 
	check_condition(f==17.9f);
	j = 629347912201l; 
	check_condition(j==629347912201l);
    }

    public void testArrayRead() {
	setModule("array read");
	check_condition(myFloatArray[0] == 12.5f);
	check_condition(myFloatArray[1] == 15.1f);
	check_condition(myFloatArray[2] == -19.4f);
	check_condition(objectArray[0]==this);
	check_condition(objectArray[1]==myByteArray);
	check_condition(objectArray[2]==myIntArray);
	check_condition(objectArray[3]==myCharArray);
	check_condition(objectArray[4]==null);
	check_condition(myByteArray[0]==10);
	check_condition(myByteArray[1]==111);
	check_condition(myByteArray[2]==25);
	check_condition(myByteArray[4]==49);
	check_condition(myIntArray[0]==101);
	check_condition(myIntArray[1]==-1121);
	check_condition(myIntArray[2]==235);
	check_condition(myIntArray[4]==-549);
	check_condition(myCharArray[0]==610);
	check_condition(myCharArray[1]==1711);
	check_condition(myCharArray[2]==285);
	check_condition(myCharArray[4]==0xffff);
	check_condition(initializedShortArray[0]==(short)11115);
	check_condition(myDoubleArray[0] == 789456.456);
	check_condition(myDoubleArray[1] == 0);
	check_condition(myDoubleArray[2] ==  -17894.156);
	check_condition(myLongArray[0] == 1);
	check_condition(myLongArray[1] == 9461820132l);
	check_condition(myLongArray[2] == 0xffffffffffffffffl);
	check_condition(initializedShortArray[1]==(short)0);
	check_condition(initializedShortArray[3]==(short)144);
	check_condition(initializedShortArray[4]==(short)-915);
	check_condition(initializedShortArray[10]==(short)5);
	check_condition(initializedShortArray[11]==(short)1);
    }
    
    public void testArrayWrite() {
	setModule("array write");
	for (int i = 0;i<6;i++)
	    charArray[i] = (char)(12345*i);
	for (int i = 0;i<6;i++)
	    check_condition (charArray[i] ==(char)(12345*i));
	
	objArray[3] = this; 
	check_condition (objArray[3]==this);	
	boolArray[5] = true; 
	check_condition (boolArray[5]==true);	
	boolArray[6] = false; 
	check_condition (boolArray[6]==false);	
	boolArray[7] = true; 
	check_condition (boolArray[7]==true);	
	boolArray[8] = true; 
	check_condition (boolArray[8]==true);	
	boolArray[9] = false; 
	check_condition (boolArray[9]==false);	
	boolArray[0] = false; 
	check_condition (boolArray[0]==false);
	
	
	for (int i = 0;i<6;i++)
	    longArray[i] = (193567594l*i);
	for (int i = 0;i<6;i++)
	    check_condition (longArray[i] ==(193567594l*i));	
	for (int i = 0;i<6;i++)
	    byteArray[i] = (byte)(9*i);
	for (int i = 0;i<6;i++)
	    check_condition (byteArray[i] ==(byte)(9*i));
	for (int i = 0;i<6;i++)
	    intArray[i] = 219145332*i;	 
	for (int i = 0;i<6;i++)
	    check_condition (intArray[i] ==219145332*i);	
	for (int i = 0;i<6;i++)
	    doubleArray[i] = (919356.474594d*i);
	for (int i = 0;i<6;i++)
	    check_condition (doubleArray[i] ==(919356.474594d*i));	
	for (int i = 0;i<6;i++)
	    floatArray[i] = (19356.7594f*i);
	for (int i = 0;i<6;i++)
	    check_condition (floatArray[i] ==(19356.7594f*i));	
	for (int i = 1;i<6;i++)
	    shortArray[i] = (short)(4099*i);
	// reading to check.  This also checking the coupling between reading and writing
	for (int i = 0;i<6;i++)
	    check_condition (shortArray[i] ==(short)(4099*i));		
	for (int i = 0;i<6;i++)
	    shortArray[i] = (short)(-5201*i);
	for (int i = 0;i<6;i++)
	    check_condition (shortArray[i] ==(short)(-5201*i));
    }



    int uninitializedInt;
    long unitializedLong;
    boolean [] boolArray = new boolean[10];
    int i_=1;
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
    int [] myIntArray = {101,
			   -1121,
			   235,
			   43,
			   -549};
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

