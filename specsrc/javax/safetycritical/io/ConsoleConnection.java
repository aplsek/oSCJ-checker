package javax.safetycritical.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.StreamConnection;
import javax.safetycritical.annotate.SCJAllowed;

/**
 */
@SCJAllowed
public class ConsoleConnection implements StreamConnection
{
  @SCJAllowed
  ConsoleConnection(String name) throws ConnectionNotFoundException
  {
  }

  @SCJAllowed
  public void close() throws IOException
  {
  }

  @SCJAllowed
  public InputStream openInputStream()
      throws IOException
  {
    return null;
  }

  /* (non-Javadoc)
   * @see javax.microedition.io.OutputConnection#openOutputStream()
   */
  @SCJAllowed
  public OutputStream openOutputStream()
      throws IOException
  {
    return null;
  }
}
