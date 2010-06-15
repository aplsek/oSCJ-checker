/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

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
    //TODO: implement
  }

  @SCJAllowed
  public static OutputStream openOutputStream(String name)
    throws IllegalArgumentException, ConnectionNotFoundException, IOException
  {
    throw new IllegalArgumentException("Connection is not an OutputConnection.");
    //TODO: implement
  }
}
