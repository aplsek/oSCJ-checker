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


package s3.rawMemory;

import ovm.core.services.memory.VM_Address;

/**
 * RawMemoryAccess implementation for SCJ
 * 
 * @author plsek
 *
 */
public class RawMemoryAccess {


    /**
	 * Get the byte at the given address with an atomic load.
	 *s
	 * @param	address	address of the sbyte to read
	 * @return	The byte at the given address
	 */
	public static byte getByteAtomic(long address) {
		byte myByte = 0;
		
		VM_Address addr = VM_Address.fromInt((int) address);
		
		addr.getByte();
		//TODO : how to read/write to that location?
		
		
		//System.out.println("address is:" + addr);
		
		
		return addr.getByte();
	}

	/**
	 * Set the byte at the given address with an atomic store.
	 *
	 * @param	address	address of the byte to write
	 * @param	value	Value to write.
	 */
	public static void setByteAtomic(long address, long value) {
		VM_Address addr = VM_Address.fromInt((int) address);
		addr.setByte((byte) value);
	}

	/**
	 * Get the short at the given address with an atomic load.
	 *
	 * @param	address	address of the short to read
	 * @return	The short at the given address
	 */
	public static short getShortAtomic(long address) {
		
		return 0;
	}

	/**
	 * Set the short at the given address with an atomic store.
	 *
	 * @param	address	address of the short to write
	 * @param	value	Value to write.
	 */
	public static void setShortAtomic(long address, short value) {
		
	}

	/**
	 * Get the int at the given address with an atomic load.
	 *
	 * @param	address	address of the int to read
	 * @return	The int at the given address
	 */
	public static int getIntAtomic(long address) {
		return 0;
	}

	/**
	 * Set the int at the given address with an atomic store.
	 *
	 * @param	address	address of the int to write
	 * @param	value	Value to write.
	 */
	public static void setIntAtomic(long address, int value) { 
		
	}

	/**
	 * Get the long at the given address
	 *
	 * @param	address	address of the long to read
	 * @return	The long at the given address
	 */
	public static long getLong(long address) {
		return 0L;
	}

	/**
	 * Set the long at the given address
	 *
	 * @param	address	address of the long to write
	 * @param	value	Value to write.
	 */
	public static void setLong(long address, long value){ 
		
	}

	/**
	 * Get the Float at the given address with an atomic load.
	 *
	 * @param	address	address of the Float to read
	 * @return	The Float at the given address
	 */
	public static float getFloatAtomic(long address){
		return 0;
	}

	/**
	 * Set the Float at the given address with an atomic store.
	 *
	 * @param	address	address of the Float to write
	 * @param	value	Value to write.
	 */
	public static void setFloatAtomic(long address, float value) {
		
	}

	/**
	 * Get the Double at the given address
	 *
	 * @param	address	address of the Double to read
	 * @return	The Double at the given address
	 */
	public static double getDouble(long address) {
		return 0;
	}

	/**
	 * Set the Double at the given address
	 *
	 * @param	address	address of the Double to write
	 * @param	value	Value to write.
	 */
	public static void setDouble(long address, double value) {
		
	}
	
}
