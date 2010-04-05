/*---------------------------------------------------------------------*\
 *
 * Copyright aicas GmbH, Karlsruhe 2009
 *
 * This code is provided to the JSR 302 group for evaluation purpose
 * under the LGPL 2 license from GNU.  This notice must appear in all
 * derived versions of the code and the source must be made available
 * with any binary version.  Viewing this code does not prejudice one
 * from writing an independent version of the classes within.
 *
 * $Source: /home/cvs/jsr302/scj/specsrc/javax/safetycritical/io/ConnectionFactory.java,v $
 * $Revision: 1.1 $
 * $Author: jjh $
 * Contents: Java source code of ConnectionFactory
 *
\*---------------------------------------------------------------------*/

package javax.safetycritical.io;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
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
  @SCJAllowed
  public boolean equals(Object other)
  {
    return (other instanceof ConnectionFactory) &&
            prefix_.equals(((ConnectionFactory)other).prefix_);
  }

  public abstract Connection create(String name)
    throws ConnectionNotFoundException;
}
