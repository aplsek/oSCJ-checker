/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_classpath/java/lang/reflect/LibraryImports.java,v 1.15 2007/06/25 20:27:20 baker29 Exp $
 */
package java.lang.reflect;
import org.ovmj.java.Opaque;

/**
 * The interface to the OVM kernel services from the Java VM personality.
 */
final class LibraryImports {

    private LibraryImports() {} // no instantiation


    static final native void initializeClass(Class clazz);

    static final native Object newInstance(Opaque constr, 
                                           Object[] args,
					   Class caller)
	throws InstantiationException, IllegalAccessException,
	       InvocationTargetException;

    static final native Object createObjectArray(Class componentClass, int length);


    static final native Object invokeMethod(Object receiver,
					    Opaque vmMethod,
					    Object[] args,
					    Class caller);

    static final native String nameForField(Opaque vmField);

    static final native Class typeForField(Opaque vmField);

    static final native void fieldSetInt(Opaque vmField, Object o, int value);
    static final native void fieldSetBoolean(Opaque vmField, Object o, boolean value);
    static final native void fieldSetShort(Opaque vmField, Object o, short value);
    static final native void fieldSetChar(Opaque vmField, Object o, char value);
    static final native void fieldSetByte(Opaque vmField, Object o, byte value);
    static final native void fieldSetFloat(Opaque vmField, Object o, float value);
    static final native void fieldSetDouble(Opaque vmField, Object o, double value);
    static final native void fieldSetLong(Opaque vmField, Object o, long value);
    static final native void fieldSetReference(Opaque vmField, Object o, Object value);

    static final native int fieldGetInt(Opaque vmField, Object o);
    static final native boolean fieldGetBoolean(Opaque vmField, Object o);
    static final native short fieldGetShort(Opaque vmField, Object o);
    static final native char fieldGetChar(Opaque vmField, Object o);
    static final native byte fieldGetByte(Opaque vmField, Object o);
    static final native float fieldGetFloat(Opaque vmField, Object o);
    static final native double fieldGetDouble(Opaque vmField, Object o);
    static final native long fieldGetLong(Opaque vmField, Object o);

    static final native Object fieldGetReference(Opaque vmField, Object o);

}
