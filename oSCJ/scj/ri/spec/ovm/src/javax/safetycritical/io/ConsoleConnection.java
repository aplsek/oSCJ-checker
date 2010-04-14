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
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.StreamConnection;
import javax.safetycritical.annotate.SCJAllowed;

/**
 * TODO: implement this class
 * 
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
