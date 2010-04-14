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

@SCJAllowed(LEVEL_0)
public class RawMemoryAccess implements RawIntegralAccess {

  @SCJAllowed(LEVEL_0)
  public static final RawMemoryName IO_ACCESS = null;
  
  @SCJAllowed(LEVEL_0)
  public static final RawMemoryName MEM_ACCESS = null;
  
  @SCJAllowed(LEVEL_0)
  public RawMemoryAccess(PhysicalMemoryName type, long size)
  /*       throws java.lang.SecurityException,
                javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException,
                javax.realtime.UnsupportedPhysicalMemoryException */
                {
                };
       
  @SCJAllowed(LEVEL_0)         
  public RawMemoryAccess(PhysicalMemoryName type, long base, long size)
   /*      throws java.lang.SecurityException,
                javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException,
                javax.realtime.UnsupportedPhysicalMemoryException */
                {
                };
    
  @SCJAllowed(LEVEL_0)            
  public static RawIntegralAccess createRmaInstance(RawMemoryName type,
                long base, long size)
   /*      throws java.lang.InstantiationException,
                java.lang.IllegalAccessException,
                java.lang.reflect.InvocationTargetException */
                { return null; }
   
  @SCJAllowed(LEVEL_0)             
  public byte getByte(long offset)
    /*     throws javax.realtime.OffsetOutOfBoundsException,
               javax.realtime.SizeOutOfBoundsException*/
                { return 0; };
  
  @SCJAllowed(LEVEL_0)
  public void getBytes(long offset, byte[] bytes, int low, int number)
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
         
  @SCJAllowed(LEVEL_0)
  public int getInt(long offset)
  /*       throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException*/
                { return 1; };
                
  @SCJAllowed(LEVEL_0)
  public void getInts(long offset, int[] ints, int low, int number)
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
         
  @SCJAllowed(LEVEL_0)
  public long getLong(long offset)
  /*       throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException*/
                { return 0L; };
  
  @SCJAllowed(LEVEL_0)
  public void getLongs(long offset, long[] longs, int low, int number)
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};

  @SCJAllowed(LEVEL_0)
  public short getShort(long offset)
 /*        throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException*/
                { return 0; };
  
  @SCJAllowed(LEVEL_0)
  public void getShorts(long offset, short[] shorts, int low, int number)
/*         throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
         
  @SCJAllowed(LEVEL_0)   
  public void setByte(long offset, byte value)
/*         throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
         
  @SCJAllowed(LEVEL_0)
  public void setBytes(long offset, byte[] bytes, int low, int number)
/*         throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};

  @SCJAllowed(LEVEL_0)
  public void setInt(long offset, int value)
/*         throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
   
  @SCJAllowed(LEVEL_0)      
  public void setInts(long offset, int[] its, int low, int number)
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
         
  @SCJAllowed(LEVEL_0)
  public void setByte(long offset, long value)
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
  
  @SCJAllowed(LEVEL_0)   
  public void setLongs(long offset, long[] longs, int low, int number)
 /*        throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
         
  @SCJAllowed(LEVEL_0)
  public void setShort(long offset, short value)
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException*/
         {};
         
  @SCJAllowed(LEVEL_0)
  public void setShorts(long offset, short[] shorts, int low, int number)
 /*        throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException */
         {};
}
