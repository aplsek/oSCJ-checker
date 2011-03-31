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

import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

/**
 * Unless specified to the contrary, see JDK 1.6 documentation.
 */
@SCJAllowed
public class FilterOutputStream extends OutputStream
{
  protected OutputStream out;

  @SCJAllowed
  @MemoryAreaEncloses(inner = {"this"}, outer = {"out"})
  @SCJRestricted(maySelfSuspend = false)
  public FilterOutputStream(OutputStream out) {}

  @SCJAllowed
  public void write(int b) throws IOException {}

  @SCJAllowed
  public void write(byte[] b) throws IOException {}

  @SCJAllowed
  public void write(byte[] b, int off, int len) throws IOException {}

  @SCJAllowed
  public void flush() throws IOException {}

  @SCJAllowed
  public void close() throws IOException {}
}

