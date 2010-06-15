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

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;

public abstract class ConnectionFactory
{
  private String prefix_;

  protected ConnectionFactory(String prefix)
  {
    prefix_ = prefix;
  }

  public boolean matches(String name)
  {
    return name.startsWith(prefix_);
  }

  @Override
  public boolean equals(Object other)
  {
    return (other instanceof ConnectionFactory) &&
            prefix_.equals(((ConnectionFactory)other).prefix_);
  }

  public abstract Connection create(String name)
    throws ConnectionNotFoundException;
}
