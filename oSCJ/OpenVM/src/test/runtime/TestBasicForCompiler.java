/**
 * Test very basic functionalities (method argument passing/return
 * values, field accesses, array accesses, control flows, shift
 * operations, and so on) for the compiler and compiled code. This
 * test helps find a very fundamental bugs of the compiler.
 **/
package test.runtime;

import ovm.util.OVMError;
import test.common.TestBase;

/**
 * Test basic functionalities.
 * 
 * @author Hiroshi Yamauchi
 */
public class TestBasicForCompiler extends TestBase {

    static final class Native implements ovm.core.execution.NativeInterface {
        static native double sqrt(double d);

        /*
        static native double testArgumentPassing1(double d, int i, long l,
                float f);

        static native double testArgumentPassing2(long l, double d, float f,
                long i);
        
        static native int testArgumentPassing3(int p1, int p2);
        */
    }

    public TestBasicForCompiler() {
        super("BasicForCompiler");
    }

    public void run() {
        testCompiledCode();
    }

    public void testCompiledCode() {
        // setModule("compiled code");
        testCompare();
        testArithmetic();
        testShift();
        testLogical();
        testConversion();
        testCallingConvention();
        testField();
        testLoop();
        testSwitch();
        testArray();
        testSubtype();
        testSubroutine();
        //p("OK. Just testing string printing.\n");
        
        //ovm.core.execution.Native.exit_process(0);
    }
     
    private void testArithmetic() {
        int im1 = idI(-1);
        long lm1 = idJ(-1L);
        float fm1 = idF(-1f);
        double dm1 = idD(-1d);
        int i100 = idI(100);
        int i20 = idI(20);
        int i21 = idI(21);
        int i16 = idI(16);
        long l100 = idJ(100L);
        long l20 = idJ(20L);
        long l21 = idJ(21L);
        long l16 = idJ(16L);
        float f100 = idF(100.0F);
        float f21 = idF(21.0F);
        float f20 = idF(20.0F);
        float f16 = idF(16.0F);
        double d100 = idD(100.0);
        double d21 = idD(21.0);
        double d20 = idD(20.0);
        double d16 = idD(16.0);
        int i4 = idI(4);
        long l4 = idJ(4L);

        _assert(i100 + i20 == 120);
        _assert(l100 + l20 == 120L);
        _assert(f100 + f20 == 120f);
        _assert(d100 + d20 == 120.0);
        
        _assert(i100 + im1 == 99);
        _assert(l100 + lm1 == 99L);
        _assert(f100 + fm1 == 99f);
        _assert(d100 + dm1 == 99.0);

        _assert(i100 - i20 == 80);
        _assert(l100 - l20 == 80L);
        _assert(f100 - f20 == 80f);
        _assert(d100 - d20 == 80.0);

        _assert(i100 - im1 == 101);
        _assert(l100 - lm1 == 101L);
        _assert(f100 - fm1 == 101f);
        _assert(d100 - dm1 == 101.0);

        _assert(i100 * i20 == 2000);
        _assert(l100 * l20 == 2000L);
        _assert(f100 * f20 == 2000f);
        _assert(d100 * d20 == 2000.0);

        _assert(im1 * i20 == -20);
        _assert(lm1 * l20 == -20L);
        _assert(fm1 * f20 == -20f);
        _assert(dm1 * d20 == -20.0);

        _assert(i100 / i20 == 5);
        _assert(l100 / l20 == 5L);
        _assert(f100 / f20 == 5f);
        _assert(d100 / d20 == 5.0);

        _assert(i100 / i21 == i4);
        _assert(l100 / l21 == l4);
        
        _assert(i100 / im1 == -100);
        _assert(l100 / lm1 == -100L);
        _assert(f100 / fm1 == -100f);
        _assert(d100 / dm1 == -100.0);

        _assert(i100 % i20 == 0);
        _assert(l100 % l20 == 0L);
        _assert(f100 % f20 == 0f);
        _assert(d100 % d20 == 0.0);

        _assert(i100 % i21 == i16);
        _assert(l100 % l21 == l16);
        _assert(f100 % f21 == f16);
        _assert(d100 % d21 == d16);

        _assert(im1 % i20 == im1);
        _assert(lm1 % l20 == lm1);
        _assert(fm1 % f20 == fm1);
        _assert(dm1 % d20 == dm1);
    }
    
    private void testLogical() {
        boolean T = idZ(true);
        boolean F = idZ(false);
        int i0 = idI(0);
        int im1 = idI(-1);
        long l0 = idJ(0L);
        long lm1 = idJ(-1L);
        
        _assert((im1 & im1) == im1);
        _assert((im1 | i0) == im1);
        _assert((~im1) == i0);
        _assert((~i0) == im1);
        _assert((im1 ^ i0) == im1);
        _assert((im1 ^ im1) == i0);
        _assert((i0 ^ im1) == im1);
        _assert((i0 ^ i0) == i0);
        _assert((lm1 & lm1) == lm1);
        _assert((lm1 | l0) == lm1);
        _assert((~lm1) == l0);
        _assert((~l0) == lm1);
        _assert((lm1 ^ l0) == lm1);
        _assert((lm1 ^ lm1) == l0);
        _assert((l0 ^ lm1) == lm1);
        _assert((l0 ^ l0) == l0);

        _assert(T);
        _assert(!F);
        _assert(!!T);
        _assert(T && T);
        _assert(!(T && F));
        _assert(!(F && T));
        _assert(!(F && F));
        _assert(T || T);
        _assert(T || F);
        _assert(F || T);
        _assert(!(F || F));
        _assert(T ^ F);
        _assert(F ^ T);
        _assert(!(T ^ T));
        _assert(!(F ^ F));
        _assert(T ? T : F);
        _assert(F ? F : T);
    }
    
    private void testShift() {
        int im1 = -1;
        long lm1 = -1L;
        float fm1 = -1f;
        double dm1 = -1d;
        int i16 = 16;
        int i100 = 100;
        int i32 = 32;
        int i31 = 31;
        int i63 = 63;
        int i64 = 64;
        long l100 = 100L;
        long l3923562356 = 3923562356L;
        float f100 = 100.0F;
        double d100 = 100.0;
        
        _assert(-1L  << idI(65) == -2L);
        _assert( 3L  >> idI(65) ==  1L);
        _assert(-1L >>> idI(65) == 0x7fffffffffffffffL);
        _assert(-i100 == -100);
        _assert(-l100 == -100L);
        _assert(-l3923562356 == -3923562356L);
        _assert(-f100 == -100.0F);
        _assert(-d100 == -100.0);
        _assert(-im1 == 1);
        _assert(-lm1 == 1L);
        _assert(-fm1 == 1.0F);
        _assert(-dm1 == 1.0);
        _assert((1 << i31) == 0x80000000);
        _assert((1 << i32) == 1);
        _assert((im1 << i16) == 0xFFFF0000);
        _assert((0x80000000 >> i31) == -1);
        _assert((0x80000000 >> i32) == 0x80000000);
        _assert((im1 >> i16) == im1);
        _assert((0x80000000 >>> i31) == 1);
        _assert((0x80000000 >>> i32) == 0x80000000);
        _assert((im1 >>> i16) == 0x0000FFFF);
        _assert((1L << i63) == 0x8000000000000000L);
        _assert((1L << i64) == 1L);
        _assert((lm1 << i32) == 0xFFFFFFFF00000000L);
        _assert((0x8000000000000000L >> i63) == -1L);
        _assert((0x8000000000000000L >> i64) == 0x8000000000000000L);
        _assert((lm1 >> i32) == lm1);
        _assert((0x8000000000000000L >>> i63) == 1L);
        _assert((0x8000000000000000L >>> i64) == 0x8000000000000000L);
        _assert((lm1 >>> i32) == 0x00000000FFFFFFFFL);
    }
    
    private void testCompare() {
        int i0 = idI(0);
        int i1 = idI(1);
        int im1 = idI(-1);
        int im2 = idI(-2);
        long l0 = idJ(0L);
        long l1 = idJ(1L);
        long lm1 = idJ(-1L);
        long lm2 = idJ(-2L);
        long lm3 = idJ(-3L);
        float f0 = idF(0f);
        float f1 = idF(1f);
        float fm1 = idF(-1f);
        float fm2 = idF(-2f);
        float fm3 = idF(-3f);
        double d0 = idD(0d);
        double d1 = idD(1d);
        double dm1 = idD(-1d);
        double dm2 = idD(-2d);
        double dm3 = idD(-3d);
        long l3923562356 = idJ(3923562356L);
        long l0x4000000000000000 = idJ(0x4000000000000000L);
        long l0x8000000000000000 = idJ(0x8000000000000000L);
        long l0x4000000000000001 = idJ(0x4000000000000001L);
        long l0x8000000000000001 = idJ(0x8000000000000001L);
        
        // Compare
        _assert(i0 == i0);
        _assert(i1 != i0);
        _assert(im1 > im2);
        _assert(im2 < im1);
        _assert(l0 == l0);
        _assert(l1 != l0);
        _assert(lm1 > lm2);
        _assert(lm3 < lm2);
        _assert(f0 == f0);
        _assert(f1 != f0);
        _assert(fm1 > fm2);
        _assert(fm3 < fm2);
        _assert(d0 == d0);
        _assert(d1 != d0);
        _assert(dm1 > dm2);
        _assert(dm3 < dm2);
        _assert(l0x4000000000000000 > l1);
        _assert(l0x8000000000000000 < l0);
        _assert(l0x4000000000000000 < l0x4000000000000001);
        _assert(l0x8000000000000001 > l0x8000000000000000);
        _assert(l3923562356 > 0L);
        _assert(!(l3923562356 < 0L));
        
    }
    
    private void testConversion() {
        byte b1 = idB((byte) 1);
        byte bm1 = idB((byte) -1);
        short s1 = idS((short) 1);
        short sm1 = idS((short) -1);
        char c1 = idC((char) 1);
        char cm1 = idC((char) -1);
        int i0 = idI(0);
        int i1 = idI(1);
        int im1 = idI(-1);
        long l0 = idJ(0L);
        long l1 = idJ(1L);
        long lm1 = idJ(-1L);
        float f0 = idF(0f);
        float f1 = idF(1f);
        float fm1 = idF(-1f);
        double d0 = idD(0d);
        double d1 = idD(1d);
        double dm1 = idD(-1d);
        long l3923562356 = idJ(3923562356L);
        
        // Conversion
        _assert(i1 == (int) l1);
        _assert(im1 == (int) lm1);
        _assert(i0 == (int) l0);
        _assert(i1 == (int) f1);
        _assert(im1 == (int) fm1);
        _assert(i0 == (int) f0);
        _assert(i1 == (int) d1);
        _assert(im1 == (int) dm1);
        _assert(i0 == (int) d0);

        _assert(l1 == (long) i1);
        _assert(lm1 == (long) im1);
        _assert(l0 == (long) i0);
        _assert(l1 == (long) f1);
        _assert(lm1 == (long) fm1);
        _assert(l0 == (long) f0);
        _assert(l1 == (long) d1);
        _assert(lm1 == (long) dm1);
        _assert(l0 == (long) d0);

        _assert(f1 == (float) i1);
        _assert(f1 == (float) l1);
        _assert(f1 == (float) d1);
        _assert(fm1 == (float) im1);
        _assert(fm1 == (float) lm1);
        _assert(fm1 == (float) dm1);
        _assert(f0 == (float) i0);
        _assert(f0 == (float) l0);
        _assert(f0 == (float) d0);

        _assert(d1 == (double) i1);
        _assert(d1 == (double) l1);
        _assert(d1 == (double) f1);
        _assert(dm1 == (double) im1);
        _assert(dm1 == (double) lm1);
        _assert(dm1 == (double) fm1);
        _assert(d0 == (double) i0);
        _assert(d0 == (double) l0);
        _assert(d0 == (double) f0);

        _assert((byte) im1 == bm1);
        _assert((byte) i1 == b1);
        _assert((short) im1 == sm1);
        _assert((short) i1 == s1);
        _assert((char) im1 == cm1);
        _assert((char) i1 == c1);

        _assert((int)l3923562356 == -371404940);
    }
    
    private void testCallingConvention() {
        int i1 = idI(1);
        long l1 = idJ(1L);
        float f1 = idF(1f);
        double d1 = idD(1d);
        double d100 = idD(100.0);
        double d10 = idD(10.0);
        
        _assert(virtualMethodI(1, 2) == 3);
        _assert(staticMethodI(2, 5) == 10);
        _assert(privateMethodI(100, 90) == 10);
        _assert(virtualMethodJ(1L, 2L) == 3L);
        _assert(staticMethodJ(2L, 5L) == 10L);
        _assert(privateMethodJ(100L, 90L) == 10L);
        _assert(virtualMethodF(1.0F, 2.0F) == 3.0F);
        _assert(staticMethodF(2.0F, 5.0F) == 10.0F);
        _assert(privateMethodF(100.0F, 90.0F) == 10.0F);
        _assert(virtualMethodD(1.0, 2.0) == 3.0);
        _assert(staticMethodD(2.0, 5.0) == 10.0);
        _assert(privateMethodD(100.0, 90.0) == 10.0);
        _assert(privateMixArg(2.0D, 100, 120L, 3.0F, 5.0, 3) == 1080000.0);
        _assert(staticMixArg(2.0D, 100, 120L, 3.0F, 5.0, 3) == 1080000.0);

        testCC1();
        testCC2();
        testCC3();
        _assert(testCC4(1, 2, 3, 4, 5, 6) == 21);
        _assert(testCC5(1, 2, 3, 4, 5, 6) == 21);
        _assert(testCC6(1, 2, 3, 4, 5, 6) == 21);
        _assert(testCC7(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F,
                10.0F, 11.0F, 12.0F, 13.0F) == 91.0F);
        _assert(testCC8(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F,
                10.0F, 11.0F, 12.0F, 13.0F) == 91.0F);
        _assert(testCC9(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F,
                10.0F, 11.0F, 12.0F, 13.0F) == 91.0F);
        _assert(testCC10(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0,
                11.0, 12.0, 13.0) == 91.0);
        _assert(testCC11(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0,
                11.0, 12.0, 13.0) == 91.0);
        _assert(testCC12(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0,
                11.0, 12.0, 13.0) == 91.0);
        _assert(testCC13(1L, 2L, 3L) == 6L);
        _assert(testCC14(1L, 2L, 3L) == 6L);
        _assert(testCC15(1L, 2L, 3L) == 6L);
        _assert(testCC16(1, 2L, 3F, 4D, 5, 6L, 7F, 8D) == 36D);
        _assert(testCC17(1D, 2F, 3L, 4, 5D, 6F, 7L, 8) == 36D);
        _assert(testCC18(1, 2D, 3F, 4L, 5, 6D, 7F, 8L) == 36D);
        _assert(testCC19(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16) == 136);
        _assert(testCC20(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L) == 36L);
        _assert(testCC21(1L, 2L, 3, 4L) == 10L);
        _assert(testCC22(1, 2L, 3F, 4D, 5, 6L, 7F, 8D, 9, 10L, 11F, 12D) == 78D);
        _assert(testCC23(1D, 2F, 3L, 4, 5D, 6F, 7L, 8, 9D, 10F, 11L, 12) == 78D);
        _assert(testCC24(1, 2D, 3F, 4L, 5, 6D, 7F, 8L, 9, 10D, 11F, 12L) == 78D);
        
        _assert(Native.sqrt(d100) == d10);
        //_assert(Native.testArgumentPassing1(d1, i1, l1, f1) == 4.0);
        //_assert(Native.testArgumentPassing2(l1, d1, f1, i1) == 4.0);
        //_assert(Native.testArgumentPassing3(1, 2) == 3);
    }
    
    private void testField() {
        _assert(instanceFieldI == 35);
        instanceFieldI = 100;
        _assert(instanceFieldI == 100);
        instanceFieldI = 35;
        _assert(instanceFieldJ == 35L);
        instanceFieldJ = 100L;
        _assert(instanceFieldJ == 100L);
        instanceFieldJ = 35L;
        _assert(instanceFieldF == 35.0F);
        instanceFieldF = 100.0F;
        _assert(instanceFieldF == 100.0F);
        instanceFieldF = 35.0F;
        _assert(instanceFieldD == 35.0);
        instanceFieldD = 100.0;
        _assert(instanceFieldD == 100.0);
        instanceFieldD = 35.0;

        _assert(staticFieldI == 35);
        staticFieldI = 200;
        _assert(staticFieldI == 200);
        staticFieldI = 35;
        _assert(staticFieldJ == 35L);
        staticFieldJ = 200L;
        _assert(staticFieldJ == 200L);
        staticFieldJ = 35L;
        _assert(staticFieldF == 35.0F);
        staticFieldF = 200.0F;
        _assert(staticFieldF == 200.0F);
        staticFieldF = 35.0F;
        _assert(staticFieldD == 35.0);
        staticFieldD = 200.0;
        _assert(staticFieldD == 200.0);
        staticFieldD = 35.0;
    }
    
    private void testLoop() {
        int a = 0;
        for (int i = 0; i < 10; i++)
            a++;
        _assert(a == 10);

        a = 0;
        int i = 0;
        while (i < 10) {
            a += 2;
            i++;
        }
        _assert(a == 20);

        int x = 45;
        if (x < 30)
            x = 10;
        else
            x = 14;
        _assert(x == 14);
            
    }
    
    private void testSwitch() {
        int sum = 0;
        int k = 3;
        switch (k + 3) {
        case 3:
            sum += 1;
            break;
        case 4:
            sum += 0;
            break;
        case 5:
        case 6:
        default:
            sum += 0;
        }
        switch (k + 6) {
        case 6:
        case 7:
        case 8:
            sum += 0;
            break;
        default:
            sum += 1;
        }
        _assert(sum == 1);// , "Tableswitch is out of order");
        instanceFieldI = 4;
        staticFieldI = 35;
        switch (instanceFieldI) {
        case 1:
        case 2:
        case 3:
            _fail();
            break;
        case 4:
            break;
        default:
            _fail();
            break;
        }
        switch (staticFieldI) {
        case 1:
        case 10000:
        case -50:
            _fail();
            break;
        case 35:
            break;
        default:
            _fail();
            break;
        }
        switch (staticFieldI + 900) {
        case 1:
        case 10000:
        case -50:
            _fail();
            break;
        case 35:
            _fail();
            break;
        default:
            break;
        }
         //ovm.core.execution.Native.exit_process(0);
    }
    
    private void testArray() {
        _assert(arrI[2] == 2);
        arrI[3] = 99;
        _assert(arrI[3] == 99);
        _assert(arrI.length == 4);

        _assert(arrJ[2] == 2L);
        arrJ[3] = 99L;
        _assert(arrJ[3] == 99L);
        _assert(arrJ.length == 4);

        _assert(arrF[2] == 2.0F);
        arrF[3] = 99.0F;
        _assert(arrF[3] == 99.0F);
        _assert(arrF.length == 4);

        _assert(arrD[2] == 2.0);
        arrD[3] = 99.0;
        _assert(arrD[3] == 99.0);
        _assert(arrD.length == 4);

        _assert(arrC[2] == (char) 2);
        arrC[3] = (char) 99;
        _assert(arrC[3] == (char) 99);
        _assert(arrC.length == 4);

        _assert(arrS[2] == (short) 2);
        arrS[3] = (short) -1;
        _assert(arrS[3] == (short) -1);
        _assert(arrS.length == 4);

        _assert(arrB[2] == (byte) 2);
        arrB[3] = (byte) -1;
        _assert(arrB[3] == (byte) -1);
        _assert(arrB.length == 4);

        _assert(arrBl[2] == false);
        arrBl[3] = true;
        _assert(arrBl[3] == true);
        _assert(arrBl.length == 4);

        _assert(arrObj[2] == null);
        arrObj[3] = this;
        _assert(arrObj[3] == this);
        _assert(arrObj.length == 4);
    }
    
    private void testSubtype() {
        Object o = this; 
        Object no = null;
        _assert(o instanceof TestBasicForCompiler);
        _assert(o instanceof TestBase);
        _assert(!(o instanceof OVMError));
        _assert(!(no instanceof Object));
        _assert(o == (TestBasicForCompiler)o);
        _assert(o == (TestBase)o);
        _assert(no == (Object)no);
    }
    
    private void testSubroutine() {
         int ik = 0; 
         try { 
             ik = 1; 
         } finally { 
             ik++; 
         } 
         _assert(ik == 2);
    }
   

    int instanceFieldI = 35;

    long instanceFieldJ = 35L;

    float instanceFieldF = 35.0F;

    double instanceFieldD = 35.0;

    int[] arrI = new int[] { 0, 1, 2, 3 };

    long[] arrJ = new long[] { 0L, 1L, 2L, 3L };

    float[] arrF = new float[] { 0.0F, 1.0F, 2.0F, 3.0F };

    double[] arrD = new double[] { 0.0, 1.0, 2.0, 3.0 };

    char[] arrC = new char[] { 0, 1, 2, 3 };

    short[] arrS = new short[] { 0, 1, 2, 3 };

    byte[] arrB = new byte[] { 0, 1, 2, 3 };

    boolean[] arrBl = new boolean[] { true, true, false, false };

    Object[] arrObj = new Object[] { null, null, null, null };

    static int staticFieldI = 35;

    static long staticFieldJ = 35L;

    static float staticFieldF = 35.0F;

    static double staticFieldD = 35.0;

    private void _fail(String errormsg) {
        COREfail(errormsg);
    }

    private void _fail() {
        ovm.core.execution.Native.abort();
    }

    private void _assert(boolean condition) {
        if (!condition) {
            ovm.core.execution.Native.abort();
        }
    }

    private void _assert(boolean condition, String errormsg) {
        check_condition(condition, errormsg);
    }

    public int virtualMethodI(int a, int b) {
        return a + b;
    }

    static int staticMethodI(int a, int b) {
        return a * b;
    }

    private int privateMethodI(int a, int b) {
        return a - b;
    }

    public long virtualMethodJ(long a, long b) {
        return a + b;
    }

    static long staticMethodJ(long a, long b) {
        return a * b;
    }

    private long privateMethodJ(long a, long b) {
        return a - b;
    }

    public float virtualMethodF(float a, float b) {
        return a + b;
    }

    static float staticMethodF(float a, float b) {
        return a * b;
    }

    private float privateMethodF(float a, float b) {
        return a - b;
    }

    public double virtualMethodD(double a, double b) {
        return a + b;
    }

    static double staticMethodD(double a, double b) {
        return a * b;
    }

    private double privateMethodD(double a, double b) {
        return a - b;
    }

    private static double staticMixArg(double a1, int a2, long a3, float a4,
            double a5, int a6) {
        return a1 * a2 * a3 * a4 * a5 * a6;
    }

    private double privateMixArg(double a1, int a2, long a3, float a4,
            double a5, int a6) {
        return a1 * a2 * a3 * a4 * a5 * a6;
    }

    private void testCC1() {
    }

    private static void testCC2() {
    }

    public void testCC3() {
    }

    private int testCC4(int p1, int p2, int p3, int p4, int p5, int p6) {
        return p1 + p2 + p3 + p4 + p5 + p6;
    }

    private static int testCC5(int p1, int p2, int p3, int p4, int p5, int p6) {
        return p1 + p2 + p3 + p4 + p5 + p6;
    }

    public int testCC6(int p1, int p2, int p3, int p4, int p5, int p6) {
        return p1 + p2 + p3 + p4 + p5 + p6;
    }

    private float testCC7(float p1, float p2, float p3, float p4, float p5,
            float p6, float p7, float p8, float p9, float p10, float p11,
            float p12, float p13) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12
                + p13;
    }

    private static float testCC8(float p1, float p2, float p3, float p4,
            float p5, float p6, float p7, float p8, float p9, float p10,
            float p11, float p12, float p13) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12
                + p13;
    }

    public float testCC9(float p1, float p2, float p3, float p4, float p5,
            float p6, float p7, float p8, float p9, float p10, float p11,
            float p12, float p13) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12
                + p13;
    }

    private double testCC10(double p1, double p2, double p3, double p4,
            double p5, double p6, double p7, double p8, double p9, double p10,
            double p11, double p12, double p13) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12
                + p13;
    }

    private static double testCC11(double p1, double p2, double p3, double p4,
            double p5, double p6, double p7, double p8, double p9, double p10,
            double p11, double p12, double p13) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12
                + p13;
    }

    public double testCC12(double p1, double p2, double p3, double p4,
            double p5, double p6, double p7, double p8, double p9, double p10,
            double p11, double p12, double p13) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12
                + p13;
    }

    private long testCC13(long p1, long p2, long p3) {
        return p1 + p2 + p3;
    }

    private static long testCC14(long p1, long p2, long p3) {
        return p1 + p2 + p3;
    }

    public long testCC15(long p1, long p2, long p3) {
        return p1 + p2 + p3;
    }

    private double testCC16(int p1, long p2, float p3, double p4, int p5,
            long p6, float p7, double p8) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8;
    }

    private double testCC17(double p1, float p2, long p3, int p4, double p5,
            float p6, long p7, int p8) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8;
    }

    private double testCC18(int p1, double p2, float p3, long p4, int p5,
            double p6, float p7, long p8) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8;
    }
    
    private int testCC19(int p1, int p2, int p3, int p4, int p5, int p6, 
            int p7, int p8, int p9, int p10, int p11, int p12, int p13,
            int p14, int p15, int p16) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12 + p13 + p14 + p15 + p16;
    }
    
    private long testCC20(long p1, long p2, long p3, long p4, long p5, long p6, long p7, long p8) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8;
    }
    
    private long testCC21(long p1, long p2, int p3, long p4) {
        return p1 + p2 + p3 + p4;
    }
    
    private double testCC22(int p1, long p2, float p3, double p4, int p5,
            long p6, float p7, double p8, int p9, long p10, float p11, double p12) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12;
    }

    private double testCC23(double p1, float p2, long p3, int p4, double p5,
            float p6, long p7, int p8, double p9, float p10, long p11, int p12) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12;
    }

    private double testCC24(int p1, double p2, float p3, long p4, int p5,
            double p6, float p7, long p8, int p9, double p10, float p11, long p12) {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10 + p11 + p12;
    }
    
    private int idI(int i) { return i; }
    private long idJ(long l) { return l; }
    private float idF(float f) { return f; }
    private double idD(double d) { return d; }
    private byte idB(byte i) { return i; }
    private boolean idZ(boolean l) { return l; }
    private short idS(short f) { return f; }
    private char idC(char d) { return d; }

}
