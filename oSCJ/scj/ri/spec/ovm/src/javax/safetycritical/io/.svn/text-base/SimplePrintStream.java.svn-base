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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.safetycritical.annotate.SCJAllowed;

public class SimplePrintStream extends FilterOutputStream
{
  @SCJAllowed
  public SimplePrintStream(OutputStream stream)
  {
    super(stream);
  }

  @SCJAllowed
  public boolean checkError() { return false; }

  @SCJAllowed
  protected void setError() { }

  @SCJAllowed
  protected void clearError() {}

  /**
   * The class uses the same modified UTF-8 used by java.io.DataOuputStream.
   * There are two differences between this format and the "standard" UTF-8
   * format:
   * <ol>
   * <li>the null byte '\u0000' is encoded in two bytes rather than in one, so
   * the encoded string never has any embedded nulls; and
   * <li>only the one, two, and three byte encodings are used.
   * </ol>
   *
   * @throws IOException.
   */
  @SCJAllowed
  public synchronized void print(CharSequence sequence) {}

  @SCJAllowed
  public void println() {}

  @SCJAllowed
  public void println(CharSequence sequence) {}
}
