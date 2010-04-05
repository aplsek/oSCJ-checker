package javax.safetycritical.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * The class holding all static methods for creating all connection
 * objects.
 */
@SCJAllowed
public class Connector
{
  @SCJAllowed
  public static final int READ_WRITE = 3;

  @SCJAllowed
  public static final int READ = 1;

  @SCJAllowed
  public static final int WRITE = 2;

  public static ConnectionFactory register(ConnectionFactory factory)
  {
    return null;
  }

  @SCJAllowed
  public static Connection open(String name, int mode)
    throws IllegalArgumentException, ConnectionNotFoundException, IOException
  {
    throw new ConnectionNotFoundException("Connection is not supported:" +
					  name);
  }

  @SCJAllowed
  public static OutputStream openOutputStream(String name)
    throws IllegalArgumentException, ConnectionNotFoundException, IOException
  {
    throw new IllegalArgumentException("Connection is not an OutputConnection.");
  }
}
