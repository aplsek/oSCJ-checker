package ovm.core.domain;

import ovm.core.execution.Context;
import ovm.core.repository.JavaNames;
/**
 * A Java domain, is a domain that supports the type system of the Java
 * programming language. This includes the executive domain, which is
 * written in Java, and a user-domain for running a JVM.
 * The main purpose of this interface is define domain facilities that are
 * specific to Java.
 */
public interface JavaDomain extends Domain {

    /**
     * Return a utility object that holds the common system types  of the
     * Java domain.
     */
    JavaTypes commonTypes();

    // Reflection methods

    /**
     * Allocate a throwable in the domain. Make sure the stack trace
     * contains only entries from the appropriate domain.  This method
     * is used to create exceptions that are implicitly thrown during
     * the execution of a Java program.  The set of exceptions thrown
     * is defined by JVMConstants.Throwables, and the corresponding
     * type names in Java programs are defined in
     * JavaNames.throwables.
     *
     * @param throwable the error type, as defined in JVMConstants.Throwables
     * @param message A domain string to form the message when constructing
     * the throwable
     * @param throwableCause another throwable instance that is the cause of 
     * this throwable.
     */
    Oop makeThrowable(int throwable,
                       Oop message,
                       Oop throwableCause);

    /**
     * Store a lightweight representation of a context's current stack
     * trace in a domain object.  This object must be of a
     * distinguished type in the domain's core library (such as
     * VMThrowable or Throwable).
     **/
    void fillInStackTrace(Oop throwable, Context ctx);

    /**
     * Convert the stack trace captured by fillInStackTrace to an
     * array of StackTraceElement objects.
     **/
    Oop getStackTrace(Oop throwable);
    
    /**
     * Utility class that provides <tt>Type</tt> objects for a range of common
     * Java types that might be used in the {@link ExecutiveDomain} or a
     * {@link JavaUserDomain}. Types are domain-specific so an instance of
     * <tt>JavaTypes</tt> is bound to a domain at  construction time.
     * Some types in the executive domain may be null. (Once we have 1.5
     * covariant return types we could leave the common types here and
     * extend to add the domain specific ones.
     */
    class JavaTypes {
        public final Type.Scalar java_lang_Thread;
        public final Type.Scalar java_lang_Throwable;
        public final Type.Scalar java_lang_OutOfMemoryError;
        public final Type.Scalar java_lang_Error;
        public final Type.Scalar java_lang_ExceptionInInitializerError;
	public final Type.Scalar java_lang_RuntimeException;
        public final Type.Scalar java_lang_String;
        public final Type.Scalar java_lang_StackTraceElement;
        public final Type.Scalar java_lang_reflect_Constructor;
        public final Type.Scalar java_lang_reflect_Field;
        public final Type.Scalar java_lang_reflect_Method;
        public final Type.Scalar java_lang_Class;
        public final Type.Scalar java_lang_Object;
        public final Type.Scalar java_lang_Integer;
        public final Type.Scalar java_lang_Character;
        public final Type.Scalar java_lang_Short;
        public final Type.Scalar java_lang_Byte;
        public final Type.Scalar java_lang_Boolean;
        public final Type.Scalar java_lang_Float;
        public final Type.Scalar java_lang_Double;
        public final Type.Scalar java_lang_Long;
        public final Type.Array arr_byte;
        public final Type.Array arr_char;
        public final Type.Array arr_short;
        public final Type.Array arr_int;
        public final Type.Array arr_long;
        public final Type.Array arr_boolean;
        public final Type.Array arr_float;
        public final Type.Array arr_double;
        protected final Domain dom;

        /**
         * Create a JavaTypes object for the given domain
         */
        public JavaTypes(Domain d) {
            dom = d;
            Type.Context tc = dom.getSystemTypeContext();

            java_lang_Throwable = tc.
                typeForKnown(JavaNames.java_lang_Throwable).asScalar();

            java_lang_OutOfMemoryError = tc.
                typeForKnown(JavaNames.java_lang_OutOfMemoryError).asScalar();
            
            java_lang_Error = tc.
                typeForKnown(JavaNames.java_lang_Error).asScalar();

            java_lang_RuntimeException = tc.
		typeForKnown(JavaNames.java_lang_RuntimeException).asScalar();

            java_lang_String = tc. 
                typeForKnown(JavaNames.java_lang_String).asScalar();
            
            java_lang_StackTraceElement = tc.
                typeForKnown(JavaNames.java_lang_StackTraceElement).asScalar();
            
            arr_byte = tc.
                typeForKnown(JavaNames.arr_byte).asArray();
            arr_char = tc.
                typeForKnown(JavaNames.arr_char).asArray();
            arr_short = tc.
                typeForKnown(JavaNames.arr_short).asArray();
            arr_int = tc.
                typeForKnown(JavaNames.arr_int).asArray();
            arr_long = tc.
                typeForKnown(JavaNames.arr_long).asArray();
            arr_float = tc.
                typeForKnown(JavaNames.arr_float).asArray();
            arr_double = tc.
                typeForKnown(JavaNames.arr_double).asArray();
            arr_boolean = tc.
                typeForKnown(JavaNames.arr_boolean).asArray();
            
            java_lang_Class = tc.
                typeForKnown(JavaNames.java_lang_Class).asScalar();
            
            java_lang_Object = tc.
                typeForKnown(JavaNames.java_lang_Object).asScalar();
            
            java_lang_Integer = tc.
                typeForKnown(JavaNames.java_lang_Integer).asScalar();
            java_lang_Short = tc.
                typeForKnown(JavaNames.java_lang_Short).asScalar();
            java_lang_Character = tc.
                typeForKnown(JavaNames.java_lang_Character).asScalar();
            java_lang_Boolean = tc.
                typeForKnown(JavaNames.java_lang_Boolean).asScalar();
            java_lang_Byte = tc.
                typeForKnown(JavaNames.java_lang_Byte).asScalar();
            java_lang_Float = tc.
                typeForKnown(JavaNames.java_lang_Float).asScalar();
            java_lang_Double = tc.
                typeForKnown(JavaNames.java_lang_Double).asScalar();
            java_lang_Long = tc.
                typeForKnown(JavaNames.java_lang_Long).asScalar();
            
            Type.Scalar eiie = null;
            Type.Scalar cons = null;
            Type.Scalar field = null;
            Type.Scalar method = null;
            Type.Scalar thread = null;
            try {
                eiie  = tc.
                    typeFor(JavaNames.java_lang_ExceptionInInitializerError).asScalar();
            } catch (LinkageException e) {
//                 ovm.core.services.io.BasicIO.out.println("LinkageException with EIIE");
            }
            try {
                cons  = tc.
                    typeFor(JavaNames.java_lang_reflect_Constructor).asScalar();
            } catch (LinkageException e) {
//                 ovm.core.services.io.BasicIO.out.println("LinkageException with Constructor");
            }
            try {
                field = tc.
                    typeFor(JavaNames.java_lang_reflect_Field).asScalar();
            } catch (LinkageException e) {
//                 ovm.core.services.io.BasicIO.out.println("LinkageException with Field");
            }
            try {
		method = tc.
		    typeFor(JavaNames.java_lang_reflect_Method).asScalar();
	    } catch (LinkageException e) {
//                 ovm.core.services.io.BasicIO.out.println("LinkageException with Method");
	    }
            try {
                thread = tc.
                    typeFor(JavaNames.java_lang_Thread).asScalar();
	    } catch (LinkageException e) {
//                 ovm.core.services.io.BasicIO.out.println("LinkageException with Thread");
	    }
	    java_lang_ExceptionInInitializerError = eiie;
	    java_lang_reflect_Constructor = cons;
	    java_lang_reflect_Field = field;
	    java_lang_reflect_Method = method;
            java_lang_Thread = thread;

        }
    }
    
}
