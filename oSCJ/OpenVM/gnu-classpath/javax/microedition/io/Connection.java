package javax.microedition.io;

import java.io.IOException;



/** A generic connection that just provides the ability to be closed. */

public abstract interface Connection
{
  /**
   * Clean up all resources for this connection and make it unusable.
   */

  public void close() throws IOException;
}
