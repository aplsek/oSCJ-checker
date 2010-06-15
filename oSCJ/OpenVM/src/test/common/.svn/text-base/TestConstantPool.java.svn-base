/**
 **/
package test.common;

import test.common.TestBase;
/**
 * @author Hiroshi Yamauchi
 * @author Gergana Markova
 */
public class TestConstantPool extends TestBase {

    public TestConstantPool() {
        super("ConstantPool");
    }

    /**
     * This runs a series of tests to see if constant pool works.
     */
    public void run() {
        testInt();
        testLong();
        testFloat();
        testDouble();
        testString();
        testChar();
    }

    /* booleans, shorts and bytes don't come from constant pools */
    public void testChar() {
        setModule("char");
        char c0 = (char) 9;
        char c2 = (char) 100;
        char c3 = 'A'; // 65
        char c4 = (char) 50000;
        check_condition((c0 + c2) == (char) 109);
        check_condition((c0 + c3) == (char) 74);
        check_condition((c2 - c0) == (char) 91);
        check_condition((c4 - c2) == (char) 49900);
    }

    public void testInt() {
        setModule("int");
        int int0 = 0x12345678;
        byte byte0 = (byte) (int0 & 0xFF);
        byte byte1 = (byte) ((int0 >> 8) & 0xFF);
        byte byte2 = (byte) ((int0 >> 16) & 0xFF);
        byte byte3 = (byte) ((int0 >> 24) & 0xFF);
        check_condition(byte0 == 0x78);
        check_condition(byte1 == 0x56);
        check_condition(byte2 == 0x34);
        check_condition(byte3 == 0x12);
    }

    public void testLong() {
        setModule("long");
        long long0 = 0L;
        int low0 = 0;
        int high0 = 0;
        long long1 = 1L;
        int low1 = 1;
        int high1 = 0;
        long long2 = -1L;
        int low2 = 0xFFFFFFFF;
        int high2 = 0xFFFFFFFF;
        long long3 = Long.MAX_VALUE;
        int low3 = -1;
        int high3 = Integer.MAX_VALUE;
        long long4 = Long.MIN_VALUE;
        int low4 = 0;
        int high4 = Integer.MIN_VALUE;
        long long5 = 0x00FFFFFFFFFFFFFFL;
        int low5 = -1;
        int high5 = 0x00FFFFFF;
        long long6 = 0xCCFFFFFFFFFFFFFFL;
        int low6 = -1;
        int high6 = 0xCCFFFFFF;

        check_condition(
            ((int) (long0 & 0xFFFFFFFF) == low0
                && ((int) ((long0 >> 32) & 0xFFFFFFFF)) == high0));
        check_condition(
            ((int) (long1 & 0xFFFFFFFF) == low1
                && ((int) ((long1 >> 32) & 0xFFFFFFFF)) == high1));
        check_condition(
            ((int) (long2 & 0xFFFFFFFF) == low2
                && ((int) ((long2 >> 32) & 0xFFFFFFFF)) == high2));
        check_condition(
            ((int) (long3 & 0xFFFFFFFF) == low3
                && ((int) ((long3 >> 32) & 0xFFFFFFFF)) == high3));
        check_condition(
            ((int) (long4 & 0xFFFFFFFF) == low4
                && ((int) ((long4 >> 32) & 0xFFFFFFFF)) == high4));
        check_condition(
            ((int) (long5 & 0xFFFFFFFF) == low5
                && ((int) ((long5 >> 32) & 0xFFFFFFFF)) == high5));
        check_condition(
            ((int) (long6 & 0xFFFFFFFF) == low6
                && ((int) ((long6 >> 32) & 0xFFFFFFFF)) == high6));
    }

    public void testFloat() {
        setModule("float");
        float f5 = 0.1F;
        float f6 = 2.0F + f5;
        float f7 = 4.0F;
        float f8 = 150.0F;
        float f9 = 154.0F;
        check_condition((2.1F == f6));
        check_condition(((f7 + f8) == f9));
    }

    public void testDouble() {
        setModule("double");
        double d5 = 0.1;
        double d6 = 2.0 + d5;
        double d7 = 4.5;
        double d8 = 150.5;
        double d9 = 155.0;
        check_condition(2.1 == d6);
        check_condition((d7 + d8) == d9);
    }

    public void testString() {
        setModule("string");
        String s = "hello";
        String s2 = " world";
        check_condition(s.length() == 5);
        check_condition(s2.charAt(1) == 'w');
        check_condition("".length() == 0);
    }

} // end of TestConstantPool
