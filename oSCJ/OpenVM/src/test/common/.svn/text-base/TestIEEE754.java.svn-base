/**
 * Test IEEE754 features
 **/
package test.common;


/**
 * Tests IEEE754 features
 * @author Antonio Cunei
 **/
public class TestIEEE754 extends TestBase {

  public TestIEEE754() {
    super("IEEE754");
  }

  // Must avoid constant folding in javac, the
  // constants are stored twice and compared.

  float fnan1=Float.NaN;
  float fpinf1=Float.POSITIVE_INFINITY;
  float fninf1=Float.NEGATIVE_INFINITY;
  float fzerop1=0.0f;
  float fzeron1=-0.0f;

  double dnan1=Double.NaN;
  double dpinf1=Double.POSITIVE_INFINITY;
  double dninf1=Double.NEGATIVE_INFINITY;
  double dzerop1=0.0d;
  double dzeron1=-0.0d;

  float fnan=fzerop1/fzerop1;
  float fpinf=1.0f/fzerop1;
  float fninf=1.0f/fzeron1;
  float fzerop=0.0f;
  float fzeron=-0.0f;
  float fmax=Float.MAX_VALUE;
  float fmin=Float.MIN_VALUE;

  double dnan=dzerop1/dzerop1;
  double dpinf=1.0d/dzerop1;
  double dninf=1.0d/dzeron1;
  double dzerop=0.0d;
  double dzeron=-0.0d;
  double dmax=Double.MAX_VALUE;
  double dmin=Double.MIN_VALUE;

  public void run() {

// Verify: calculated values and Float/Double constants:

    check_condition((Float.floatToIntBits(fnan1)
                     ==Float.floatToIntBits(fnan)),
                     " Float.floatToIntBits(Float.NaN) is not "+
                     "equal to Float.floatToIntBits(0.0f/0.0f)");

    check_condition((Float.floatToRawIntBits(fpinf1)
                     ==Float.floatToRawIntBits(fpinf)),
                     " Float.floatToRawIntBits(Float.POSITIVE_INFINITY) is not "+
                     "equal to Float.floatToRawIntBits(1.0f/0.0f)");

    check_condition((Float.floatToRawIntBits(fninf1)
                     ==Float.floatToRawIntBits(fninf)),
                     " Float.floatToRawIntBits(Float.NEGATIVE_INFINITY) is not "+
                     "equal to Float.floatToRawIntBits(1.0f/-0.0f)");


    check_condition((Double.doubleToLongBits(fnan1)
                     ==Double.doubleToLongBits(fnan)),
                     " Double.doubleToLongBits(Double.NaN) is not "+
                     "equal to Double.doubleToLongBits(0.0d/0.0d)");

    check_condition((Double.doubleToRawLongBits(fpinf1)
                     ==Double.doubleToRawLongBits(fpinf)),
                     " Double.doubleToRawLongBits(Double.POSITIVE_INFINITY) is not "+
                     "equal to Double.doubleToRawLongBits(1.0d/0.0d)");

    check_condition((Double.doubleToRawLongBits(fninf1)
                     ==Double.doubleToRawLongBits(fninf)),
                     " Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY) is not "+
                     "equal to Double.doubleToRawLongBits(1.0d/-0.0d)");

// Comparisons

    check_condition(!(fnan1==fnan)," float NaN == NaN must be false, but it is not");
    check_condition(!(fnan1<fnan)," float NaN < NaN must be false, but it is not");
    check_condition(!(fnan1>fnan)," float NaN > NaN must be false, but it is not");

    check_condition((fpinf1==fpinf)," float +Inf == +Inf must be true, but it is not");
    check_condition(!(fpinf1<fpinf)," float +Inf < +Inf must be false, but it is not");
    check_condition(!(fpinf1>fpinf)," float +Inf > +Inf must be false, but it is not");

    check_condition((fninf1==fninf)," float -Inf == -Inf must be true, but it is not");
    check_condition(!(fninf1<fninf)," float -Inf < -Inf must be false, but it is not");
    check_condition(!(fninf1>fninf)," float -Inf > -Inf must be false, but it is not");

    check_condition((fzerop1==fzeron)," float 0.0 == -0.0 must be true, but it is not");
    check_condition(!(fzerop1<fzeron)," float 0.0 < -0.0 must be false, but it is not");
    check_condition(!(fzerop1>fzeron)," float 0.0 > -0.0 must be false, but it is not");

    check_condition(!(fnan1==fzerop)," float NaN == 0.0 must be false, but it is not");
    check_condition(!(fnan1<fzerop)," float NaN < 0.0 must be false, but it is not");
    check_condition(!(fnan1>fzerop)," float NaN > 0.0 must be false, but it is not");


    check_condition(!(dnan1==dnan)," double NaN == NaN must be false, but it is not");
    check_condition(!(dnan1<dnan)," double NaN < NaN must be false, but it is not");
    check_condition(!(dnan1>dnan)," double NaN > NaN must be false, but it is not");

    check_condition((dpinf1==dpinf)," double +Inf == +Inf must be true, but it is not");
    check_condition(!(dpinf1<dpinf)," double +Inf < +Inf must be false, but it is not");
    check_condition(!(dpinf1>dpinf)," double +Inf > +Inf must be false, but it is not");

    check_condition((dninf1==dninf)," double -Inf == -Inf must be true, but it is not");
    check_condition(!(dninf1<dninf)," double -Inf < -Inf must be false, but it is not");
    check_condition(!(dninf1>dninf)," double -Inf > -Inf must be false, but it is not");

    check_condition((dzerop1==dzeron)," double 0.0 == -0.0 must be true, but it is not");
    check_condition(!(dzerop1<dzeron)," double 0.0 < -0.0 must be false, but it is not");
    check_condition(!(dzerop1>dzeron)," double 0.0 > -0.0 must be false, but it is not");

    check_condition(!(dnan1==dzerop)," double NaN == 0.0 must be false, but it is not");
    check_condition(!(dnan1<dzerop)," double NaN < 0.0 must be false, but it is not");
    check_condition(!(dnan1>dzerop)," double NaN > 0.0 must be false, but it is not");


// Comparisons between boxed values:

// compareTo not implemented at this point
/*
    Float fpz=new Float(fzerop);
    Float fnz=new Float(fzeron);
    Float fna=new Float(fnan);

    Double dpz=new Double(dzerop);
    Double dnz=new Double(dzeron);
    Double dna=new Double(dnan);

    check_condition(fpz.compareTo(fpz)==0," Float(0.0).compareTo(Float(0.0)) must be 0, but it is not");
    check_condition(fpz.compareTo(fnz)>0," Float(0.0).compareTo(Float(-0.0)) must be >0, but it is not");
    check_condition(fnz.compareTo(fpz)<0," Float(-0.0).compareTo(Float(0.0)) must be <0, but it is not");
    check_condition(fnz.compareTo(fnz)==0," Float(-0.0).compareTo(Float(-0.0)) must be 0, but it is not");
    check_condition(fna.compareTo(fna)==0," Float(NaN).compareTo(Float(Nan)) must be 0, but it is not");

    check_condition(dpz.compareTo(dpz)==0," Double(0.0).compareTo(Double(0.0)) must be 0, but it is not");
    check_condition(dpz.compareTo(dnz)>0," Double(0.0).compareTo(Double(-0.0)) must be >0, but it is not");
    check_condition(dnz.compareTo(dpz)<0," Double(-0.0).compareTo(Double(0.0)) must be <0, but it is not");
    check_condition(dnz.compareTo(dnz)==0," Double(-0.0).compareTo(Double(-0.0)) must be 0, but it is not");
    check_condition(dna.compareTo(dna)==0," Double(NaN).compareTo(Double(Nan)) must be 0, but it is not");
*/

// Printing

    check_condition(Float.toString(fzerop1).equals("0.0")," Float When printing 0.0 I did not get \"0.0\"");
    check_condition(Float.toString(fzeron1).equals("-0.0")," Float When printing -0.0 I did not get \"-0.0\"");
    check_condition(Float.toString(fpinf1).equals("Infinity")," Float When printing POSITIVE_INFINITY I did not get \"Infinity\"");
    check_condition(Float.toString(fninf1).equals("-Infinity")," Float When printing NEGATIVE_INFINITY I did not get \"-Infinity\"");

    check_condition(Double.toString(dzerop1).equals("0.0")," Double When printing 0.0 I did not get \"0.0\"");
    check_condition(Double.toString(dzeron1).equals("-0.0")," Double When printing -0.0 I did not get \"-0.0\"");
    check_condition(Double.toString(dpinf1).equals("Infinity")," Double When printing POSITIVE_INFINITY I did not get \"Infinity\"");
    check_condition(Double.toString(dninf1).equals("-Infinity")," Double When printing NEGATIVE_INFINITY I did not get \"-Infinity\"");


// These values are incorrectly printed by Ovm at the moment. the letter is 'e' where it should be 'E',
// too many digits are printed for MIN_VAL, and too few for MAX_VAL
//
// FIXME
//
/*
    check_condition(Float.toString(fmax).equals("3.4028235E38")," Float When printing MAX_VALUE I did not get \"3.4028235E38\"");
    check_condition(Float.toString(fmin).equals("1.4E-45")," Float When printing MIN_VALUE I did not get \"1.4E-45\"");
    check_condition(Double.toString(dmax).equals("1.7976931348623157E308")," Double When printing MAX_VALUE I did not get \"1.7976931348623157E308\"");
    check_condition(Double.toString(dmin).equals("4.9E-324")," Double When printing MIN_VALUE I did not get \"4.9E-324\"");
*/

// Conversions to integer


    check_condition(((byte)fnan1)==0," Float (byte)NaN must be 0, but it is not");
    check_condition(((short)fnan1)==0," Float (short)NaN must be 0, but it is not");
    check_condition(((int)fnan1)==0," Float (short)NaN must be 0, but it is not");
    check_condition(((long)fnan1)==0L," Float (long)NaN must be 0L, but it is not");

    check_condition(((byte)fpinf1)==-1," Float (byte)Infinity must be -1, but it is not");
    check_condition(((short)fpinf1)==-1," Float (short)Infinity must be -1, but it is not");
    check_condition(((int)fpinf1)==2147483647," Float (short)Infinity must be 2147483647, but it is not");
    check_condition(((long)fpinf1)==9223372036854775807L," Float (long)Infinity must be 9223372036854775807L, but it is not");

    check_condition(((byte)fninf1)==0," Float (byte)-Infinity must be 0, but it is not");
    check_condition(((short)fninf1)==0," Float (short)-Infinity must be 0, but it is not");
    check_condition(((int)fninf1)==-2147483648," Float (short)-Infinity must be -2147483648, but it is not");
    check_condition(((long)fninf1)==-9223372036854775808L," Float (long)-Infinity must be -9223372036854775808L, but it is not");

    check_condition(((byte)fzerop1)==0," Float (byte)0.0 must be 0, but it is not");
    check_condition(((short)fzerop1)==0," Float (short)0.0 must be 0, but it is not");
    check_condition(((int)fzerop1)==0," Float (short)0.0 must be 0, but it is not");
    check_condition(((long)fzerop1)==0L," Float (long)0.0 must be 0L, but it is not");

    check_condition(((byte)fzeron1)==0," Float (byte)-0.0 must be 0, but it is not");
    check_condition(((short)fzeron1)==0," Float (short)-0.0 must be 0, but it is not");
    check_condition(((int)fzeron1)==0," Float (short)-0.0 must be 0, but it is not");
    check_condition(((long)fzeron1)==0L," Float (long)-0.0 must be 0L, but it is not");


    check_condition(((byte)dnan1)==0," Double (byte)NaN must be 0, but it is not");
    check_condition(((short)dnan1)==0," Double (short)NaN must be 0, but it is not");
    check_condition(((int)dnan1)==0," Double (short)NaN must be 0, but it is not");
    check_condition(((long)dnan1)==0L," Double (long)NaN must be 0L, but it is not");

    check_condition(((byte)dpinf1)==-1," Double (byte)Infinity must be -1, but it is not");
    check_condition(((short)dpinf1)==-1," Double (short)Infinity must be -1, but it is not");
    check_condition(((int)dpinf1)==2147483647," Double (short)Infinity must be 2147483647, but it is not");
    check_condition(((long)dpinf1)==9223372036854775807L," Double (long)Infinity must be 9223372036854775807L, but it is not");

    check_condition(((byte)dninf1)==0," Double (byte)-Infinity must be 0, but it is not");
    check_condition(((short)dninf1)==0," Double (short)-Infinity must be 0, but it is not");
    check_condition(((int)dninf1)==-2147483648," Double (short)-Infinity must be -2147483648, but it is not");
    check_condition(((long)dninf1)==-9223372036854775808L," Double (long)-Infinity must be -9223372036854775808L, but it is not");

    check_condition(((byte)dzerop1)==0," Double (byte)0.0 must be 0, but it is not");
    check_condition(((short)dzerop1)==0," Double (short)0.0 must be 0, but it is not");
    check_condition(((int)dzerop1)==0," Double (short)0.0 must be 0, but it is not");
    check_condition(((long)dzerop1)==0L," Double (long)0.0 must be 0L, but it is not");

    check_condition(((byte)dzeron1)==0," Double (byte)-0.0 must be 0, but it is not");
    check_condition(((short)dzeron1)==0," Double (short)-0.0 must be 0, but it is not");
    check_condition(((int)dzeron1)==0," Double (short)-0.0 must be 0, but it is not");
    check_condition(((long)dzeron1)==0L," Double (long)-0.0 must be 0L, but it is not");

// Additional tricky conversion tests, just in case

    check_condition(((int)2750000.1)==2750000," ((int)2750000.1) must be 2750000, but it is not");
    check_condition(((int)2750000.9)==2750000," ((int)2750000.9) must be 2750000, but it is not");
    check_condition(((int)-2750000.1)==-2750000," ((int)-2750000.1) must be -2750000, but it is not");
    check_condition(((int)-2750000.9)==-2750000," ((int)-2750000.9) must be -2750000, but it is not");

    check_condition(((short)2750000.1)==-2512," ((short)2750000.1) must be -2512, but it is not");
    check_condition(((short)2750000.9)==-2512," ((short)2750000.9) must be -2512, but it is not");
    check_condition(((short)-2750000.1)==2512," ((short)-2750000.1) must be 2512, but it is not");
    check_condition(((short)-2750000.9)==2512," ((short)-2750000.1) must be 2512, but it is not");

    check_condition(((byte)2750000.1)==48," ((byte)2750000.1) must be 48, but it is not");
    check_condition(((byte)2750000.9)==48," ((byte)2750000.9) must be 48, but it is not");
    check_condition(((byte)-2750000.1)==-48," ((byte)-2750000.1) must be -48, but it is not");
    check_condition(((byte)-2750000.9)==-48," ((byte)-2750000.9) must be -48, but it is not");


// Some math

    check_condition(Float.isNaN(fpinf1-fpinf)," Float Infinity - Infinity should be NaN, but it is not");
    check_condition((fpinf1-fninf)==fpinf," Float Infinity -  -Infinity should be Infinity, but it is not");
    check_condition((fninf1-fpinf)==fninf," Float -Infinity - Infinity should be -Infinity, but it is not");
    check_condition(Float.isNaN(fninf1-fninf)," Float -Infinity - -Infinity should be NaN, but it is not");

    check_condition(Double.isNaN(dpinf1-dpinf)," Double Infinity - Infinity should be NaN, but it is not");
    check_condition((dpinf1-dninf)==dpinf," Double Infinity -  -Infinity should be Infinity, but it is not");
    check_condition((dninf1-dpinf)==dninf," Double -Infinity - Infinity should be -Infinity, but it is not");
    check_condition(Double.isNaN(dninf1-dninf)," Double -Infinity - -Infinity should be NaN, but it is not");

  }

}
