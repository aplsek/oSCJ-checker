/*---------------------------------------------------------------------*\
 *
 * Copyright (c) 2007-2009 Aonix, Paris, France
 *
 * This code is provided for educational purposes under the LGPL 2
 * license from GNU.  This notice must appear in all derived versions
 * of the code and the source must be made available with any binary
 * version.  
 *
\*---------------------------------------------------------------------*/
package java.io;

import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * Unless specified to the contrary, see JDK 1.6 documentation.
 */
@SCJAllowed
public abstract class OutputStream implements Closeable, Flushable {

  @BlockFree
  @SCJAllowed
  public OutputStream()
  {
  }

  @SCJAllowed
  public abstract void write(int b) throws IOException;

  @SCJAllowed
  public void write(byte[] b) throws IOException
  {
  }

  @SCJAllowed
  public void write(byte[] b, int off, int len) throws IOException
  {
  }

  @SCJAllowed
  public void flush() throws IOException
  {
  }

  @SCJAllowed
  public void close() throws IOException
  {
  }
}

