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

package javax.realtime;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_0;

/**
 * Description An interface that describes factory classes
 *  that create classes that imple- ment RawIntegralAccess.
 * @author plsek
 *
 */
@SCJAllowed(LEVEL_0)
public interface RawIntegralAccessFactory {

  @SCJAllowed(LEVEL_0)
  public RawMemoryName getName();

  @SCJAllowed(LEVEL_0)
  public RawIntegralAccess newIntegralAccess(long base, long size)/*
         throws java.lang.SecurityException,
                javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException,
                javax.realtime.MemoryTypeConflictException,
                java.lang.OutOfMemoryError*/;

}
