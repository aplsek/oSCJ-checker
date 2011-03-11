/*---------------------------------------------------------------------*\
 *
 * Copyright (c) 2007, 2008 Aonix, Paris, France
 *
 * This code is provided for educational purposes under the LGPL 2
 * license from GNU.  This notice must appear in all derived versions
 * of the code and the source must be made available with any binary
 * version.  
 *
\*---------------------------------------------------------------------*/
package java.lang;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;
import static javax.safetycritical.annotate.Allocate.Area.THIS;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed
public class BigInteger extends Number implements Comparable<BigInteger> {

	@SCJAllowed
	public static final BigInteger ONE = null;

	@SCJAllowed
	public static final BigInteger TEN = null;

	@SCJAllowed
	public static final BigInteger ZERO = null;

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ THIS })
	@SCJAllowed
	public BigInteger(byte[] val) {
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ THIS })
	@SCJAllowed
	public BigInteger(int signum, byte[] magnitude) {
	}

	/**
	 * Does not allow "this" to escape local variables.
	 * <p>
	 * Not @SCJAllowed because we don't have Random. But maybe we should...
	 * 
	 * @Allocate({THIS )
	 * @SCJRestricted(maySelfSuspend=false) public BigInteger(int bitLength, int
	 *                                      certainty, Random rnd) { }
	 */

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ THIS })
	@SCJAllowed
	public BigInteger(String val) {
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ THIS })
	@SCJAllowed
	public BigInteger(String val, int radix) {
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJAllowed
	public BigInteger abs() {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJAllowed
	public BigInteger add(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJAllowed
	public BigInteger andNot(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJAllowed
	public int bitCount() {
		return 0; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJAllowed
	public int bitLength() {
		return 0; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJAllowed
	public BigInteger clearBit(int n) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@SCJAllowed
	public int compareTo(BigInteger val) {
		return 0; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJAllowed
	public BigInteger divide(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJAllowed
	public BigInteger[] divideAndRemainder(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJAllowed
	public double doubleValue(BigInteger val) {
		return 0.0; // skeleton
	}

	/**
	 * Does not allow "this" or "x" to escape local variables.
	 */
	@SCJAllowed
	public boolean equals(Object x) {
		return true; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJAllowed
	public BigInteger flipBit(int n) {
		return null; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJAllowed
	public float floatValue(BigInteger val) {
		return (float) 0.0; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJAllowed
	public BigInteger gcd(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJAllowed
	public int getLowestSetBit() {
		return 0; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public int hashCode() {
		return 0; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public int intValue() {
		return 0; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public boolean isProbablePrime(int certainty) {
		return false; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public long longValue() {
		return 0L; // skeleton
	}

	/**
	 * Does not allow "this" or "max" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger max(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" or "val" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger min(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" or "val" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger mod(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" or "val" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger modInverse(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this", "exponent", or "m" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger modPow(BigInteger exponent, BigInteger m) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" or val to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger multiply(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger negate() {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger nextProbablePrime() {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger not() {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" or "val" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger or(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger pow(int exponent) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 * <p>
	 * Not SCJAllowed because we don't have Random, but maybe we should.
	 * 
	 * @Allocate({CURRENT )
	 * @SCJRestricted(maySelfSuspend=false) public BigInteger probablePrime(int
	 *                                      bitLength, Random rnd) { return
	 *                                      null; // skeleton }
	 */

	/**
	 * Does not allow "this" or "val" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger remainder(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger setBit(int n) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger shiftLeft(int n) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger shiftRight(int n) {
		return null; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public int signum() {
		return 0; // skeleton
	}

	/**
	 * Does not allow "this" or "val" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger subtract(BigInteger val) {
		return null; // skeleton
	}

	/**
	 * Allocates no memory. Does not allow "this" to escape local variables.
	 */
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public boolean testBit(int n) {
		return false; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger toByteArray() {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public String toString() {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public String toString(int radix) {
		return null; // skeleton
	}

	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public static BigInteger valueOf(long val) {
		return null; // skeleton
	}

	/**
	 * Does not allow "this" or "val" to escape local variables.
	 */
	@Allocate({ CURRENT })
	@SCJRestricted(maySelfSuspend=false)
	@SCJAllowed
	public BigInteger xor(BigInteger val) {
		return null; // skeleton
	}

	@Override
	public double doubleValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float floatValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short shortValue() {
		// TODO Auto-generated method stub
		return 0;
	}
}
