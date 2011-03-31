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

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

/**
 * Unless specified to the contrary, see JDK 1.6 documentation.
 */
@SCJAllowed
public abstract class InputStream implements Closeable {

  @SCJAllowed
  @SCJRestricted(maySelfSuspend = false)
  public InputStream()
  {
  }

  @SCJAllowed
  public abstract int read() throws IOException;

  @SCJAllowed
  public int read(byte[] b) throws IOException
  {
    return 0;
  }

  @SCJAllowed
  public int read(byte[] b, int off, int len) throws IOException
  {
    return 0;
  }

  @SCJAllowed
  public long skip(long n) throws IOException
  {
    return 0L;
  }

  @SCJAllowed
  public int available() throws IOException
  {
    return 0;
  }

  @SCJAllowed
  public void close() throws IOException
  {
  }

  @SCJAllowed
  public void mark(int readlimit)
  {
  }

  @SCJAllowed
  public void reset() throws IOException
  {
  }

  @SCJAllowed
  public boolean markSupported()
  {
    return false;
  }
  
}

