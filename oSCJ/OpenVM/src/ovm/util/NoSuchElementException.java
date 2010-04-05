/* NoSuchElementException.java -- Attempt to access element that does not exist
   Copyright (C) 1998, 1999 Free Software Foundation, Inc.
This file is part of GNU Classpath.
It has been modified slightly to fit the OVM framework.
GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.
You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.
As a special exception, if you link this library with other files to
produce an executable, this library does not by itself cause the
resulting executable to be covered by the GNU General Public License.
This exception does not however invalidate any other reasons why the
executable file might be covered by the GNU General Public License. */
package ovm.util;

/**
 * Exception thrown when an attempt is made to access an element that does not
 * exist. This exception is thrown by the Enumeration, Iterator and ListIterator
 * classes if the nextElement, next or previous method goes beyond the end of
 * the list of elements that are being accessed.
 *
 * @author Free Software Foundation
 * @author Ben L. Titzer
 */
public class NoSuchElementException extends OVMRuntimeException
{
  private static final long serialVersionUID = 6769829250639411880L;

  /**
   * Constructs a NoSuchElementException with no detail message.
   */
  public NoSuchElementException()
  {
    super();
  }

  /**
   * Constructs a NoSuchElementException with a detail message.
   *
   * @param detail the detail message for the exception
   */
  public NoSuchElementException(String detail)
  {
    super(detail);
  }
}
