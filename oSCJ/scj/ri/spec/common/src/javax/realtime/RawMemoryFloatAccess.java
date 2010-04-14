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
public class RawMemoryFloatAccess extends RawMemoryAccess
             implements RawRealAccess {

	public RawMemoryFloatAccess(PhysicalMemoryName type, long size) {
		super(type, size);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getDouble(long offset) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void getDoubles(long offset, double[] doubles, int low, int number) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getFloat(long offset) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void getFloats(long offset, float[] floats, int low, int number) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDouble(long offset, double value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDoubles(long offset, double[] doubles, int low, int number) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFloat(long offset, float value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFloats(long offset, float[] floats, int low, int number) {
		// TODO Auto-generated method stub
		
	}

//  public RawMemoryFloatAccess(PhysicalMemoryName type, long size)
///*         throws java.lang.SecurityException,
//                javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException,
//                javax.realtime.UnsupportedPhysicalMemoryException*/
//  {};
//
//
//  public RawMemoryFloatAccess(PhysicalMemoryName type, long base, long size)
///*         throws java.lang.SecurityException,
//                javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException,
//                javax.realtime.UnsupportedPhysicalMemoryException,
//                javax.realtime.MemoryTypeConflictException*/
//                {};
//
//  public static RawScalarAccess createFpAccessInstance(RawMemoryName type,
//                long base, long size)
// /*        throws java.lang.InstantiationException,
//                java.lang.IllegalAccessException,
//                java.lang.reflect.InvocationTargetException */
//                {};
//                
//  public double getDouble(long offset)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {return 0.0; };
//  public void getDoubles(long offset, double[] doubles, int low, int number)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//                
//  public float getFloat(long offset)
// /*        throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {return 0.0; };
//  public void getFloats(long offset, float[] floats, int low, int number)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//                
//  public void setDouble(long offset, double value)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//  public void setDoubles(long offset, double[] doubles, int low, int number)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//                
//  public void setFloat(long offset, float value)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//  public void setFloats(long offset, float[] floats, int low, int number)
///*         throws javax.realtime.OffsetOutOfBoundsException,
//                javax.realtime.SizeOutOfBoundsException*/
//                {};
//
}
