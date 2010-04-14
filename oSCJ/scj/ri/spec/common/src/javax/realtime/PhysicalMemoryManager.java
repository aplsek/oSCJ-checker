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
import static javax.safetycritical.annotate.Level.LEVEL_1;

/**
 * 
 * 
 * 
 * @author plsek
 *
 */
@SCJAllowed(LEVEL_1)
public final class PhysicalMemoryManager {
  /*
   *
   */

  public static final PhysicalMemoryName ALIGNED = null;
  
  /*
   *
   */  

  public static final PhysicalMemoryName BYTESWAP = null;

  /*
   *
   */
  @SCJAllowed(LEVEL_1)
  public static final PhysicalMemoryName DEVICE = null;

  /*
   *
   */
  @SCJAllowed(LEVEL_1)
  public static final PhysicalMemoryName DMA = null;

  /*
   *
   */
  @SCJAllowed(LEVEL_1)
  public static final PhysicalMemoryName IO_PAGE = null;

  /*
   *
   */
  @SCJAllowed(LEVEL_1)
  public static final PhysicalMemoryName SHARED = null;

}
