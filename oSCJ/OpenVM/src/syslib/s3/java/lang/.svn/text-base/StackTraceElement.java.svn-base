/* StackTraceElement.java -- One function call or call stack element
   Copyright (C) 2001, 2002 Free Software Foundation, Inc.
   Adapted from GNU Classpath.
*/


package java.lang;
import ovm.core.execution.Interpreter;
import ovm.core.domain.Code;
import ovm.core.domain.ExecutiveDomain;
import ovm.core.domain.DomainDirectory;
import ovm.core.services.memory.VM_Address;

public class StackTraceElement {

    /**
     * The name of the file, null if unknown.
     */
    private String fileName;

    /**
     * The line number in the file, negative if unknown.
     */
    private int lineNumber;

    /**
     * The fully qualified class name, null if unknown.
     */
    private String className;

    /**
     * The method name in the class, null if unknown.
     */
    private String methodName;

    /** Whether the method is native. */
    private boolean isNative;

    /**
     * A package local constructor for the StackTraceElement class, to be
     * called by the Virtual Machine as part of Throwable.fillInStackTrace.
     * There are no public constructors defined for this class. Creation
     * of new elements is implementation specific.
     *
     * @param fileName the name of the file, null if unknown
     * @param lineNumber the line in the file, negative if unknown
     * @param className the fully qualified name of the class, null if unknown
     * @param methodName the name of the method, null if unknown
     * @param isNative true if native, false otherwise
     */
    StackTraceElement(String fileName, int lineNumber, String className,
		      String methodName, boolean isNative) {
	this.fileName = fileName;
	this.lineNumber = lineNumber;
	this.className = className;
	this.methodName = methodName;
	this.isNative = isNative;

	// ovm.core.services.io.BasicIO.err.println("created " + this);
    }

    /**
     * Returns the name of the file, or null if unknown. This is usually
     * obtained from the <code>SourceFile</code> attribute of the class file
     * format, if present.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the line number in the file, or a negative number if unknown.
     * This is usually obtained from the <code>LineNumberTable</code> attribute
     * of the method in the class file format, if present.
     *
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the fully qualified class name, or null if unknown.
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the method name in the class, or null if unknown. If the
     * execution point is in a constructor, the name is
     * <code>&lt;init&gt;</code>; if the execution point is in the class
     * initializer, the name is <code>&lt;clinit&gt;</code>.
     *
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns true if the method is native, or false if it is not or unknown.
     *
     * @return whether the method is native
     */
    public boolean isNativeMethod() {
        return isNative;
    }

    /**
     * Returns a string representation of this stack trace element. The
     * returned String is implementation specific. This implementation
     * returns the following String: "[class][.][method]([file][:line])".
     * If the fully qualified class name or the method is unknown it is
     * omitted including the point seperator. If the source file name is
     * unknown it is replaced by "Unknown Source" if the method is not native
     * or by "Native Method" if the method is native. If the line number
     * is unknown it and the colon are omitted.
     *
     * @return a string representation of this execution point
     */
    public String toString()  {
        StringBuffer sb = new StringBuffer();
        if (className != null) {
            sb.append(className);
            if (methodName != null)
                sb.append('.');
        }
        if (methodName != null)
            sb.append(methodName);
        sb.append(" (");
        if (fileName != null)
            sb.append(fileName);
        else
            sb.append(isNative ? "Native Method" : "Unknown Source");
        if (lineNumber >= 0)
            sb.append(':').append(lineNumber);
        sb.append(')');
        return sb.toString();
    }

    /**
     * Returns true if the given object is also a StackTraceElement and all
     * attributes, except the native flag, are equal (either the same attribute
     * between the two elments are null, or both satisfy Object.equals).
     *
     * @param o the object to compare
     * @return true if the two are equal
     */
    public boolean equals(Object o) {
        if (! (o instanceof StackTraceElement))
            return false;
        StackTraceElement e = (StackTraceElement) o;
        return equals(fileName, e.fileName)
            && lineNumber == e.lineNumber
            && equals(className, e.className)
            && equals(methodName, e.methodName);
    }
    
    /**
     * Returns the hashCode of this StackTraceElement. This implementation
     * computes the hashcode by xor-ing the hashcode of all attributes except
     * the native flag.
     *
     * @return the hashcode
     */
    public int hashCode()  {
        return hashCode(fileName) ^ lineNumber ^ hashCode(className)
            ^ hashCode(methodName);
    }
    
    /**
     * Compare two objects according to Collection semantics.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return o1 == null ? o2 == null : o1.equals(o2)
     */
    private static final boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    /**
     * Hash an object according to Collection semantics.
     *
     * @param o the object to hash
     * @return o1 == null ? 0 : o1.hashCode()
     */
    private static final int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }
}
