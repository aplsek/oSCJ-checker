package javax.safetycritical.io;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;

public class ConsoleConnectionFactory extends ConnectionFactory
{
  public ConsoleConnectionFactory()
  {
    super("console:");
  }

  @Override
  public Connection create(String name)
    throws ConnectionNotFoundException
  {
    return new ConsoleConnection(name);
  }
}
