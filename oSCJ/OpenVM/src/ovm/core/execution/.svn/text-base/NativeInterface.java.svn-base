/*
 * $Header: /p/sss/cvs/OpenVM/src/ovm/core/execution/NativeInterface.java,v 1.9 2006/11/14 20:55:30 baker29 Exp $
 *
 */
package ovm.core.execution;

/**
 * A marker interface identifying classes that act as proxies for a native
 * code API.
 * Classes that implement this interface are treated specially during the
 * image writing process and must obey certain conventions:
 * <ul>
 * <li>The class <b>must</b> be declared <code>final</code></li>
 * <li>All methods <b>must</b> be declared <code>static</code></li>
 * <li>The class should not be instantiatiable </li>
 * </ul>
 * <p>A class conforming to the above requirements is processed as follows:
 * <ul>
 * <li>Every invocation to a method in this class is rewritten as
 * an invocation to a C function with the same name and with a signature as
 * described below. 
 * </li>
 * <li>Any method not declared <code>native</code> may
 * be executed under the host VM during image writing. Code that executes at 
 * both image writing time and runtime must be written to this API
 * and we will get host VM functionality or native
 * functionality as appropriate at image time and runtime. Methods that are
 * declared <code>native</code> are not expected to be used at boot 
 * image creation time,
 * and if they are then an unsatisfied link error will occur.
 * </li>
 * </ul>
 * <p>There are rules regarding method parameter types:
 * <ul>
 * <li>One dimensional primitive arrays are passed as a pointer to the
 * array elements. In many cases the second argument is expected to be the
 * array length.
 * <li>Byte arrays are expected to be NUL terminated sequences 
 * of characters representing C strings. These are cast to <tt>char*</tt>
 * types.
 * <li>String parameters are expanded into two parameters for the native
 * method - a pointer to the internal byte array, and the length of the array.
 * This is is useful for methods that don't require NUL-terminated strings.
 * <li>ints can be used for int values or raw pointer values (we're assuming
 * a 32-bit system). But see below for typesafe pointer types.
 * <li>floating point types are always passed (and thus received) as double
 * (so the Java signature may say float, but the native code function must
 * take a double)
 * <li>longs are passed as an appropriate 64-bit native integer type (typically
 * <code>long long</code>)
 * <li>Any parameter that is an instance of {@link Native.Ptr} represents a
 * native pointer type. These parameters define a type name for the native
 * pointer type and a value for its value. The value is passed as the actual 
 * parameter and is passed as the type specified by the type name. For example
 * a {@link Native.FilePtr} parameter is passed 
 * as <code>(FILE*)obj->value</code>
 * <li>No other types are supported.
 * </ul>
 * <p>Return values are typically ints or pointers, and in both cases are
 * returned as int values. A <code>void</code> return type is also permitted.
 * <p>If a method requires native data structures or doesn't map directly to
 * an underlying C library or system function then we define our own helper
 * methods to support this. (See <code>native_helpers.c</code>).
 * <p>If a native API requires additional helpers at the Java level then 
 * declare a nested static class to provide those helper functions.
 * See for example, {@link Native.Utils}. 
 * Be careful, however, not to declare any of the &quot;native&quot; methods
 * as private, as this will cause the generation of a synthetic package-private
 * accessor method which will be assumed to be a native method call. If a
 * native method should only be accessed via a helper then make the method
 * package-private so that it is directly accessible.
 *
 * @see s3.services.bytecode.ovmify.NativeCallGenerator
 *
 * @author David Holmes
 * @author Krzysztof Palacz
 *
 */
public interface NativeInterface {

}
