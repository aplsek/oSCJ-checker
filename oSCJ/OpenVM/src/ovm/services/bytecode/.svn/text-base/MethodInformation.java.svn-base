package ovm.services.bytecode;

import ovm.util.ByteBuffer;

/**
 * MethodInformation is an interface for an object that provides
 * information about the method that is currently analyzed.
 * KP: This interface is evil, it is used to confuse data and operations
 * @author Christian Grothoff
 **/
public interface MethodInformation {

    /**
     * Get the code of the method that we are analyzing.
     **/
    public ByteBuffer getCode();

    /**
     * Get the current PC into the method that we are analyzing.
     **/
    public int getPC();

} // end of MethodInformation
