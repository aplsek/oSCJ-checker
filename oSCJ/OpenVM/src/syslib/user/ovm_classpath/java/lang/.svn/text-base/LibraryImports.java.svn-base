package java.lang;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.ovmj.java.Opaque;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * The interface to the OVM kernel services from the Java VM personality.
 */
final class LibraryImports {

    private LibraryImports() {} // no instantiation

    // initialization
    static final native void enableClassInitialization();


    // direct memory area control
    static native Opaque     getImmortalArea();
    static native Opaque     setCurrentArea(Opaque area);
    static native Opaque     areaOf(Object ref);

    // Object/Class related
    static final native int identityHashCode(Object obj);

    static final native Object clone(Object target);

    static final native String nameForClass(Class vmType);
    
 


    static final native Opaque getBootContext();
    static final native String getBootClassPath();
    static final native Opaque getSystemContext();
    static final native String getClassPath();
    static final native void setPeer(Opaque context, ClassLoader loader);
    static final native Class initiateLoading(ClassLoader loader, String name);
    static final native Class findLoadedClass(ClassLoader loader, String name);
    static final native Class defineClass(ClassLoader loader,
					  String name,
					  byte[] data, int off, int len);
    
    static final native void initializeClass(Class vmType);
    static final native ClassLoader getClassLoader(Class vmType);

    static final native Class   VMClass_getComponentType(Class vmType);
    static final native int     VMClass_getModifiers(Class vmType, boolean _);
    static final native String  VMClass_getName(Class vmType);
    static final native Class   VMClass_getSuperclass(Class vmType);
    static final native boolean VMClass_isAssignableFrom(Class c1, Class c2);
    static final native boolean VMClass_isInstance(Class c, Object o);
    static final native boolean VMClass_isArray(Class c);
    static final native boolean VMClass_isInterface(Class c);
    static final native boolean VMClass_isPrimitive(Class c);
    static final native Class VMClass_getDeclaringClass(Class vmType);    

    static final native void    VMClass_throwException(Throwable t);
    
    static final native Field[] getDeclaredFields(Class declaringClass);
    static final native Field[] getFields(Class declaringClass);
    static final native Method[] getDeclaredMethods(Class declaringClass);
    static final native Constructor[]
	getDeclaredConstructors(Class declaringClass);
    static final native Class[] VMClass_getInterfaces(Class c);


    static final native Class classFor(Object obj);

    static final native Class getPrimitiveClass(char type);

    /*
    static final native Object newInstance(Opaque constr, Object[] args)
	throws InstantiationException, IllegalAccessException,
	       InvocationTargetException;
    */

    static final native String makeStackTraceElementSourceFileName(Opaque code);
    static final native String makeStackTraceElementClassName(Opaque code);
    static final native String makeStackTraceElementMethodName(Opaque code);
    static final native int makeStackTraceElementLineNumber(Opaque code, int relativePC);

    // monitor related

    static final native boolean currentThreadOwnsMonitor(Object obj);

    static final native void monitorNotify(Object obj);

    static final native void monitorNotifyAll(Object obj);

    static final native boolean monitorWait(Object obj);

    static final native boolean monitorTimedWait(Object obj, long millis, int nanos);

    // thread dispatcher related

    // RTJVM version
    static final native Opaque createVMThread(VMThreadBase javaThread,
					      boolean noHeap);

    // Plain JVM version
    static final native Opaque createVMThread(VMThreadBase javaThread);

    static final native void bindPrimordialJavaThread(VMThreadBase javaThread);

    static final native VMThreadBase getCurrentJavaThread();

    static final native Opaque getCurrentVMThread();

    static final native void yieldCurrentThread();

    static final native boolean delayCurrentThread(long millis, int nanos);

    static final native void startThread(Opaque vmThread);

    static final native void interruptThread(Opaque vmThread);

    static final native void terminateCurrentThread();

    static final native boolean isAlive(Opaque vmThread);

    static final native boolean setPriority(Opaque vmThread, int prio);

    static final native int getPriority(Opaque vmThread);

    static final native int getMaximumPriority();

    // Low-level I/O for use during initialization 

    static final native void printString(String msg);

    // Low-level I/O with no conversion or allocation
    static final native void printInt(int i);
    static final native void printLong(long l);
    static final native void printCharAsByte(char c);

    // VM-specific properties
    static final native String VMPropertyName(int i);
    static final native String VMPropertyValue(int i);
    static final native boolean isVMProperty(String name);
    static final native String getenv(String name);

    static final native String defaultMainClass();

    // Runtime/GC related

    static final native int availableProcessors();

    static final native long freeMemory();

    static final native long totalMemory();

    static final native long maxMemory();

    static final native void gc();

    static final native void runFinalization();

    // floating-point

    static final native double parseDouble(String s);

    // Misc system

    static final native String intern(String s);

    static final native void halt(int reason);

    static final native void setIn(InputStream str);
    static final native void setOut(PrintStream str);
    static final native void setErr(PrintStream str);

    static final native long getCurrentTime();

    static final native void fillInStackTrace(VMThrowable t);
    
    static final native StackTraceElement[] getStackTrace(VMThrowable t);

    static final native String[] getCommandlineArgumentStringArray();

    static final native boolean canMonitorSignal(int sig);

    static final native int waitForSignal(int sig);

    // Memory Scopes related
    static final native Opaque enterAreaForMirror(Object o);
    static final native void leaveArea(Opaque o);
    
    // Atomic sections
    static native boolean setReschedulingEnabled(boolean enabled);

    // return the length of an arbitrary array, the right way (as
    // opposed to the way Array.getLength is implemented in classpath)
    // arr must be a instance of an array type
    static native int arrayLength(Object arr);
    
    // move bytes for arraycopy without any type, bound, or store
    // checks
    static native void copyArrayElements(Object src, int soff,
					 Object dst, int doff,
					 int nelt);

    // move bytes within a single array.  no bound check is performed
    static native void copyOverlapping(Object arr,
				       int soff, int doff, int nelt);
    
    static native void runGCThread();
    static native void setShouldPause(boolean shouldPause);
    static native boolean needsGCThread();
    static native int getGCTimerMutatorCounts();
    static native int getGCTimerCollectorCounts();
    static native int getGCThreadPriority();
    
    // some stuff
    static native int delayCurrentThreadAbsolute(long nanos);
}
