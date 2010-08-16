package javax.microedition.io;

import javax.safetycritical.annotate.SCJAllowed;

/**
 * A Marker for Connections that can both read and write data.
 */
@SCJAllowed
public interface StreamConnection extends InputConnection, OutputConnection
{
}
