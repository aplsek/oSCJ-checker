package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public class StorageConfigurationParameters {

    long _totalBackingStore;
    long _nativeStackSize;
    long _javaStackSize;
    long _messageLength;
    long _stackTraceLength;

    /**
     * Stack sizes for schedulable objects and sequencers. Passed as parameter
     * to the constructor of mission sequencers and schedulable objects.
     * 
     * @param totalBackingStore
     *            size of the backing store reservation for worst-case scope
     *            usage in bytes
     * @param nativeStack
     *            size of native stack in bytes (vendor specific)
     * @param javaStack
     *            size of Java execution stack in bytes (vendor specific)
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public StorageConfigurationParameters(long totalBackingStore,
            long nativeStackSize, long javaStackSize) {
        // TODO: default for the last two parameters?
        this(totalBackingStore, nativeStackSize, javaStackSize, 0, 0);
    }

    /**
     * Stack sizes for schedulable objects and sequencers. Passed as parameter
     * to the constructor of mission sequencers and schedulable objects.
     * 
     * @param totalBackingStore
     *            size of the backing store reservation for worst-case scope
     *            usage in bytes
     * @param nativeStack
     *            size of native stack in bytes (vendor specific)
     * @param javaStack
     *            size of Java execution stack in bytes (vendor specific)
     * @param messageLength
     *            length of the space in bytes dedicated to message associated
     *            with this Schedulable object's ThrowBoundaryError exception
     *            plus all the method names/identifiers in the stack backtrace
     * @param stackTraceLength
     *            the number of byte for the StackTraceElement array dedicated
     *            to stack backtrace associated with this Schedulable object's
     *            ThrowBoundaryError exception.
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public StorageConfigurationParameters(long totalBackingStore,
            long nativeStackSize, long javaStackSize, long messageLength,
            long stackTraceLength) {
        // TODO: add runtime check of parameters
        _totalBackingStore = totalBackingStore;
        _nativeStackSize = nativeStackSize;
        _javaStackSize = javaStackSize;
        _messageLength = messageLength;
        _stackTraceLength = stackTraceLength;
    }

    /**
     * 
     * @return the size of the total backing store available for scoped memory
     *         areas created by the assocated SO.
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public long getTotalBackingStoreSize() {
        return _totalBackingStore;
    }

    /**
     * 
     * @return the size of the native method stack available to the assocated
     *         SO.
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public long getNativeStackSize() {
        return _nativeStackSize;
    }

    /**
     * 
     * @return the size of the Java stack available to the assocated SO.
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public long getJavaStackSize() {
        return _javaStackSize;
    }

    /**
     * 
     * return the length of the message buffer
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public long getMessageLength() {
        return _messageLength;
    }

    /**
     * 
     * return the length of the stack trace buffer
     */
// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public long getStackTraceLength() {
        return _stackTraceLength;
    }
}
