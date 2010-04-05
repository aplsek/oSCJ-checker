package edu.purdue.scjtck.tck;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.safetycritical.MissionSequencer;

/**
 * Level
 * 
 * following JNI functions are supported
 * 
 * - GetVersion
 * 
 * - GetObjectClass - IsInstanceOf  - IsSameObject  - GetSuperclass  -
 * IsAssignableFrom
 * 
 * - GetStringLength - GetStringChars - ReleaseStringChars - GetStringUTFLength
 * - GetStringUTFChars - ReleaseStringUTFChars - GetStringRegion -
 * GetStringUTFRegion
 * 
 * - GetArrayLength - GetObjectArrayElement - SetObjectArrayElement - New <
 * PrimitiveType > Array Routines - Get < PrimitiveType > ArrayElements Routines
 * - Release < PrimitiveType > ArrayElements Routines - Get < PrimitiveType >
 * ArrayRegion Routines - Set < PrimitiveType > ArrayRegion Routines
 * 
 * - NewDirectByteBuffer - GetDirectBufferAddress - GetDirectBufferCapacity
 */
public class TestJNI900 extends TestCase {

    // static {
    // System.loadLibrary("TestJNI900");
    // }

    private native boolean testObjInfo(Object o);

    private native boolean testString(String s);

    private native boolean testArray(int[] intArray, Object[] objArray, Object o);

    private native boolean testIO(Buffer buf);

    public MissionSequencer getSequencer() {
        return new GeneralSingleMissionSequencer(new MyMission());
    }

    public class MyMission extends GeneralMission {

        private Object _obj = new Integer(0);

        private String _str = "jnitest";

        private int[] _intArray = new int[] { 1, 0, 0, 0, 0 };

        private Object[] _objArray = new Object[] { new Object(), null, null,
                null, null };

        private Buffer _buffer = ByteBuffer.allocate(1);

        @Override
        public void initialize() {
            new GeneralPeriodicEventHandler() {
                @Override
                public void handleEvent() {

                    try {
                        if (!testObjInfo(_obj))
                            fail("Failure in JNI test: Object information");
                        if (!testString(_str))
                            fail("Failure in JNI test: String");
                        if (!testArray(_intArray, _objArray, _objArray[0]))
                            fail("Failure in JNI test: Array");
                        /*
                         * Initially, _intArray = [1,0,0,0,0]; _objArray =
                         * [obj,null,null,null,null]
                         * 
                         * Finally, they should be [1,2,3,4,5];
                         * [obj,null,obj,null,null]
                         */
                        if (_objArray[2] != _objArray[0])
                            fail("Failure in JNI test: Array");
                        for (int i = 0; i < 5; i++)
                            if (_intArray[i] != i + 1)
                                fail("Failure in JNI Test: Array");

                        if (!testIO(_buffer))
                            fail("Failure in JNI test: NIO");
                    } catch (Throwable e) {
                        fail(e.getClass() + " " + e.getMessage());
                    }
                    requestSequenceTermination();
                }
            };
        }
    }
}
