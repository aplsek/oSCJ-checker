package ovm.core.services.memory; // SYNC PACKAGE NAME TO STRING BELOW

import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.format.CachingOvmFormat;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;
import ovm.services.bytecode.JVMConstants.WordOps;
import ovm.util.OVMRuntimeException;
import ovm.util.PragmaUnsafe;
import s3.services.bootimage.Ephemeral;
import s3.util.PragmaTransformCallsiteIR;
import s3.util.PragmaTransformCallsiteIR.BCnothing;
import s3.util.PragmaTransformCallsiteIR.Rewriter;
import ovm.core.domain.Field;
import ovm.core.Executive;

/**
 * Representation of a word in the VM.  The width in bits of this type is
 * not specified. No reference to this class is to exist in bytecode;
 * rewriting replaces all operations on VM_Word with primitive operations on
 * the appropriate primitive type. The behavior of this class's methods when
 * hosted is determined by the method bodies; the native behavior is determined
 * by substitute bytecode inlined at the call site of any method in this class.
 * <p>
 * <strong>Current design decision (revisited):</strong> VM_Words are now immutable;
 * arithmetic ops create a new object. This is of no concern running native.
 * <h3>Bitfield operations</h3>
 * Operations are provided to extract or set fixed regions of contiguous bits
 * (bitfields) of a VM_Word, a bitfield being defined by a constant width (in
 * contiguous bits) and shift (equal to the bit number in the word of the
 * field's least-significant bit, if the word's LSB is numbered 0). A bitfield
 * is defined by declaring a subclass of {@link VM_Word.Bitfield}. Bitfield
 * operations are rewritten to efficient code exploiting the constant width and
 * shift. When running hosted (writing image), the operations are less
 * efficient but still work, so the mechanism is usable in code that will run
 * both at image build and run time.
 * <p>
 * The two basic operations are to extract the contents of a bitfield so the
 * result contains only the corresponding bits ({@link #get(VM_Word.Bitfield)
 * get}), and to set the contents of a bitfield, modifying only the
 * corresponding bits to match a supplied value ({@link
 * #set(VM_Word,VM_Word.Bitfield) set}). These operations have VM_Word as the
 * return or argument type, respectively, so they are completely insulated from
 * architectural differences in word size.
 * <p>
 * Each operation also has variants with return or argument type (respectively)
 * of <code>int</code> or <code>long</code>: {@link #asInt(VM_Word.Bitfield)
 * asInt}, {@link #asLong(VM_Word.Bitfield) asLong}, {@link
 * #set(int,VM_Word.Bitfield) set}, {@link #set(long,VM_Word.Bitfield) set}.
 * All variants are supported regardless of architecture word size; the only
 * condition on correct use of, e.g., the <code>int</code> variant is that the
 * specified <em>bitfield</em> can fit in an <code>int</code>. This condition
 * depends only on the defined width of the field and not on the architecture,
 * so client code that defines and uses bitfields need not be modified when
 * word size changes. <p><em>Comment: if code that uses some bitfields, each of
 * which fits in 32 bits, is moved to a 64-bit platform, it may be convenient
 * to change some bitfield </em>declarations<em> to have new </em>shift<em>
 * values that place them differently in the 64-bit word. As long as their
 * </em>widths<em> are unchanged, the change is localized to the bitfield
 * declarations and no change is needed where the fields are accessed, even
 * accessed as int type.</em>
 * <p>
 * Every operation also has an <em>unshifted</em> variant ({@link
 * #unshiftedGet(VM_Word.Bitfield) unshiftedGet} etc., {@link
 * #unshiftedSet(VM_Word,VM_Word.Bitfield) unshiftedSet} etc.) that omits the
 * shift operation: instead of returning a result or taking an argument whose
 * LSB is the LSB of the bitfield, the bits in the result or argument are at
 * the final positions the bitfield occupies in the word (a simple mask). This
 * can be a faster operation when the field value will not be treated
 * arithmetically.
 * <p>
 * With get/set, Word/int/long variants, and unshifted variants, the total
 * complement of bitfield operations comes to twelve methods.
 * <p><em>Comment: The correctness condition for int/long variants of the
 * </em>unshifted<em> operations is more stringent, as now the bitfield must
 * fit in the desired primitive type without shifting. This is still determined
 * entirely by the bitfield declaration and independent of word size, but does
 * limit the flexibility to reposition bitfields discussed in the comment
 * above.</em>
 * @author Chapman Flack
 **/
public class VM_Word implements Ephemeral,
				java.io.Serializable // ADDED by jv
{ // SYNC CLASS NAME TO STRING BELOW

    public static TypeName.Scalar typeNameVMW =
	JavaNames.ovm_core_services_memory_VM_Word;

    /**
     * Value of the word, <em>only when hosted</em>.  During image writing,
     * references to VM_Word are <em>replaced</em> with the contents of this
     * field from the referenced VM_Word; operations on them are primitive
     * bytecode inlined at call sites.
     **/
    int value;

    private VM_Word(int v) {
        value = v;
    }

    /**
     * Return the width of a Word, in bytes.
     **/
    public static final int widthInBytes() throws BCiconst_4 {
        return 4;
    }

    //	      //
    // Casts: //
    //	      //

    /**
     * Convert an integer to a machine word.
     * If you genuinely need this, explain why.
     * @throws BCnothing <em>this is a pragma</em>
     **/
    public static final VM_Word fromInt(int i)
        throws BCnothing, // native action
    PragmaUnsafe, ovm.core.services.memory.PragmaNoBarriers {
        return new VM_Word(i);
    } // hosted action

    /**
     * integer from VM_Word.
     * If you genuinely need this, explain why.
     * @throws BCnothing <em>this is a pragma</em>
     **/
    public final int asInt() throws BCnothing, // native action
    PragmaUnsafe {
        return value;
    } // hosted action

    /**
     * Convert a long to a machine word.  If a machine word is 32 bits
     * wide, the long value will be truncated.
     **/
    public static final VM_Word fromLong(long l) throws BCl2i, PragmaUnsafe {
	return new VM_Word((int) l);
    }

    /**
     * Convert a machine word to a java long value.
     **/
    public final long asLong() throws BCi2l, PragmaUnsafe {
	return value;
    }

    /**
     * Return true if the signed value represented by l fits in a
     * machine word.  This method is not overloaded for int values
     * because a VM_Word must be at least 32 bits wide.
     **/
    public static boolean inSignedRange(long l)	throws BCsignedRange {
	return l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE;
    }

    /**
     * Return true if the unsigned 64 bit value stored in l fits in a
     * machine word.  Unsigned 64 bit values larger than 2^63 are
     * represented by negative java longs.
     **/
    public static boolean inUnsignedRange(long l) throws BCunsignedRange {
	return l >= 0 && l < 1L << 32;
    }

    /**
     * The native semantics
     * are clear enough, but what do we want it to do hosted?
     * In this implementation, just has the obvious effect of creating
     * a new VM_Address with the same value and no associated host object.
     * Anything else would be hard.
     * @throws BCnothing <em>this is a pragma</em>
     **/
    public final VM_Address asAddress() throws BCnothing {
        return VM_Address.fromInt( value);
    }

    //		    //
    // Arithmetic:  //
    //		    //

    /**
     * Twos-complement add with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws BCiadd <em>this is a pragma</em>
     **/
    public final VM_Word add(VM_Word increment) throws BCiadd {
      return add(increment.value);
    }

    /**
     * Twos-complement add with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws BCiadd <em>this is a pragma</em>
     **/
    public final VM_Word add(int increment) throws BCiadd {
      return new VM_Word (increment+value);
    }

    /**
     * Twos-complement subtract with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws BCisub <em>this is a pragma</em>
     **/
    public final VM_Word sub(VM_Word subtrahend) throws BCisub {
      return sub(subtrahend.value);
    }

    /**
     * Twos-complement subtract with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws BCisub <em>this is a pragma</em>
     **/
    public final VM_Word sub(int subtrahend) throws BCisub {
      return new VM_Word(value-subtrahend);
    }

    /**
     * Twos-complement multiply with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws BCimul <em>this is a pragma</em>
     **/
    public final VM_Word mul(VM_Word multiplier) throws BCimul {
      return mul(multiplier.value);
    }

    /**
     * Twos-complement multiply with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws BCimul <em>this is a pragma</em>
     **/
    public final VM_Word mul(int multiplier) throws BCimul {
      return new VM_Word(value*multiplier);
    }

    /**
     * Twos-complement divide with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws ArithmeticException if divisor is zero
     * @throws BCidiv <em>this is a pragma</em>
     **/
    public final VM_Word div(VM_Word divisor) throws BCidiv {
      return div(divisor.value);
    }

    /**
     * Twos-complement divide with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws ArithmeticException if divisor is zero
     * @throws BCidiv <em>this is a pragma</em>
     **/
    public final VM_Word div(int divisor) throws BCidiv {
      return new VM_Word(value/divisor);
    }

    /**
     * Twos-complement remainder with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws ArithmeticException if divisor is zero
     * @throws BCirem <em>this is a pragma</em>
     **/
    public final VM_Word rem(VM_Word divisor) throws BCirem {
      return rem(divisor.value);
    }

    /**
     * Twos-complement remainder with wraparound, no overflow detect.
     * @return This Word mutated.
     * @throws ArithmeticException if divisor is zero
     * @throws BCirem <em>this is a pragma</em>
     **/
    public final VM_Word rem(int divisor) throws BCirem {
      return new VM_Word(value%divisor);
    }

    //		    //
    // Bit access:  //
    //		    //

    // these method names must be duplicated in array below
    /**
     * Extract the specified bitfield, shifting so the LSB of the field
     * is the LSB of the result.<br>
     * <strong>Hosted:</strong> the result is a newly-allocated VM_Word; the
     * receiver is not mutated.
     * @param b Specifies the bits to extract. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return Word (newly-allocated in hosted case) consisting of only the
     * specified bits, shifted so LSB of field is LSB of word.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final VM_Word get(Bitfield b) throws BCbitfield {
        return new VM_Word((value & b.mask) >>> b.shift);
    }
    /**
     * Extract the specified bitfield, without shifting (the result bits are
     * in the same positions the bitfield occupies). Compared to
     * {@link #get(VM_Word.Bitfield) get}, this can save a shift when it is not
     * necessary to treat the result arithmetically.<br>
     * <strong>Hosted:</strong> the result is a newly-allocated VM_Word; the
     * receiver is not mutated.
     * @param b Specifies the bits to extract. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return Word (newly-allocated in hosted case) consisting of only the
     * specified bits, occupying the same positions as in the source.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final VM_Word unshiftedGet(Bitfield b) throws BCbitfield {
        return new VM_Word(value & b.mask);
    }
    /**
     * Extract the specified bitfield, shifting so the LSB of the field
     * is the LSB of the result.
     *<p>
     * This method is usable on architectures of any word size, as long as
     * the specified <em>Bitfield</em>, after shifting, fits in an
     * <code>int</code>. This correctness condition depends only on the
     * specified Bitfield, not on the architecture, so code using this
     * interface to bits of a Word is insulated from architecture changes.
     * @param b Specifies the bits to extract. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return int consisting of only the
     * specified bits, shifted so LSB of field is LSB of result.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final int asInt(Bitfield b) throws BCbitfield {
        b.check(BfRewriter.INT, true);
        return (value & b.mask) >>> b.shift;
    }
    /**
     * Extract the specified bitfield, without shifting (the result bits are in
     * the same positions the bitfield occupies). Compared to {@link
     * #asInt(VM_Word.Bitfield) asInt}, this can save a shift when it is not
     * necessary to treat the result arithmetically.
     *<p>
     * This method is usable on architectures of any word size, as long as
     * the specified <em>Bitfield</em>, <em>without</em> shifting, fits in an
     * <code>int</code>. This correctness condition depends only on the
     * specified Bitfield, not on the architecture, so code using this
     * interface to bits of a Word is insulated from architecture changes.
     * @param b Specifies the bits to extract. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return int consisting of only the
     * specified bits, occupying the same positions as in the source.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final int unshiftedAsInt(Bitfield b) throws BCbitfield {
        b.check(BfRewriter.INT, false);
        return value & b.mask;
    }
    /**
     * Extract the specified bitfield, shifting so the LSB of the field
     * is the LSB of the result.
     *<p>
     * This method is usable on architectures of any word size, as long as
     * the specified <em>Bitfield</em>, after shifting, fits in a
     * <code>long</code>. This correctness condition depends only on the
     * specified Bitfield, not on the architecture, so code using this
     * interface to bits of a Word is insulated from architecture changes.
     * @param b Specifies the bits to extract. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return long consisting of only the
     * specified bits, shifted so LSB of field is LSB of result.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final long asLong(Bitfield b) throws BCbitfield {
        b.check(BfRewriter.LONG, true);
        long result = (value & b.mask) >>> b.shift;
        return result & 0xffffffff;
    }
    /**
     * Extract the specified bitfield, without shifting (the result bits are in
     * the same positions the bitfield occupies). Compared to {@link
     * #asLong(VM_Word.Bitfield) asLong}, this can save a shift when it is not
     * necessary to treat the result arithmetically.
     *<p>
     * This method is usable on architectures of any word size, as long as
     * the specified <em>Bitfield</em>, <em>without</em> shifting, fits in a
     * <code>long</code>. This correctness condition depends only on the
     * specified Bitfield, not on the architecture, so code using this
     * interface to bits of a Word is insulated from architecture changes.
     * @param b Specifies the bits to extract. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return int consisting of only the
     * specified bits, occupying the same positions as in the source.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final long unshiftedAsLong(Bitfield b) throws BCbitfield {
        b.check(BfRewriter.LONG, false);
        long result = value & b.mask;
        return result & 0xffffffff;
    }
    /**
     * Set bits in this word, shifting the argument (field contents) to end up
     * in the right bit positions.<br>
     * <strong>Hosted:</strong> This VM_Word is returned, mutated.
     * @param w Word containing bits to be inserted into the specified field
     * of this (receiver) word. This word is assumed originally normalized
     * (its LSB is the LSB of the field) and will be shifted as needed to
     * the positions the bitfield should occupy.
     * @param b Specifies the bits to set. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return Receiver word mutated.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final VM_Word set(VM_Word w, Bitfield b) throws BCbitfield {
        value &= ~b.mask;
        value |= (w.value << b.shift) & b.mask;
        return this;
    }
    /**
     * Set bits in this word, without shifting--can save a shift operation
     * if the argument word is not used arithmetically and its bits are
     * already in the right positions.<br>
     * <strong>Hosted:</strong> This VM_Word is returned, mutated.
     * @param w Word containing bits to be inserted into the specified field
     * of this (receiver) word. This word will not be shifted; its contents
     * should already be in the positions the bitfield should occupy.
     * @param b Specifies the bits to set. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return Receiver word mutated.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final VM_Word unshiftedSet(VM_Word w, Bitfield b)
        throws BCbitfield {
        value &= ~b.mask;
        value |= w.value & b.mask;
        return this;
    }
    /**
     * Set bits in this word, shifting the argument (field contents) to end up
     * in the right bit positions.<br>
     * <strong>Hosted:</strong> This VM_Word is returned, mutated.
     *<p>
     * This method is usable on architectures of any word size, as long as
     * the specified <em>Bitfield</em>, before shifting, fits in an
     * <code>int</code>. This correctness condition depends only on the
     * specified Bitfield, not on the architecture, so code using this
     * interface to bits of a Word is insulated from architecture changes.
     * @param i int containing bits to be inserted into the specified field
     * of this (receiver) word. This int is assumed originally normalized
     * (its LSB is the LSB of the field) and will be shifted as needed to
     * the positions the bitfield should occupy.
     * @param b Specifies the bits to set. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return Receiver word mutated.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final VM_Word set(int i, Bitfield b) throws BCbitfield {
        b.check(BfRewriter.INT, true);
        value &= ~b.mask;
        value |= (i << b.shift) & b.mask;
        return this;
    }
    /**
     * Set bits in this word, without shifting--can save a shift operation
     * if the argument int is not used arithmetically and its bits are
     * already in the right positions.<br>
     * <strong>Hosted:</strong> This VM_Word is returned, mutated.
     *<p>
     * This method is usable on architectures of any word size, as long as
     * the specified <em>Bitfield</em>, <em>without</em> shifting, fits in an
     * <code>int</code>. This correctness condition depends only on the
     * specified Bitfield, not on the architecture, so code using this
     * interface to bits of a Word is insulated from architecture changes.
     * @param i int containing bits to be inserted into the specified field
     * of this (receiver) word. This int will not be shifted; its contents
     * should already be in the positions the bitfield should occupy.
     * @param b Specifies the bits to set. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return Receiver word mutated.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final VM_Word unshiftedSet(int i, Bitfield b) throws BCbitfield {
        b.check(BfRewriter.INT, false);
        value &= ~b.mask;
        value |= i & b.mask;
        return this;
    }
    /**
     * Set bits in this word, shifting the argument (field contents) to end up
     * in the right bit positions.<br>
     * <strong>Hosted:</strong> This VM_Word is returned, mutated.
     *<p>
     * This method is usable on architectures of any word size, as long as
     * the specified <em>Bitfield</em>, before shifting, fits in a
     * <code>long</code>. This correctness condition depends only on the
     * specified Bitfield, not on the architecture, so code using this
     * interface to bits of a Word is insulated from architecture changes.
     * @param l long containing bits to be inserted into the specified field
     * of this (receiver) word. This long is assumed originally normalized
     * (its LSB is the LSB of the field) and will be shifted as needed to
     * the positions the bitfield should occupy.
     * @param b Specifies the bits to set. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return Receiver word mutated.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final VM_Word set(long l, Bitfield b) throws BCbitfield {
        b.check(BfRewriter.LONG, true);
        value &= ~b.mask;
        value |= (((int) l) << b.shift) & b.mask;
        return this;
    }
    /**
     * Set bits in this word, without shifting--can save a shift operation
     * if the argument long is not used arithmetically and its bits are
     * already in the right positions.<br>
     * <strong>Hosted:</strong> This VM_Word is returned, mutated.
     *<p>
     * This method is usable on architectures of any word size, as long as
     * the specified <em>Bitfield</em>, <em>without</em> shifting, fits in a
     * <code>long</code>. This correctness condition depends only on the
     * specified Bitfield, not on the architecture, so code using this
     * interface to bits of a Word is insulated from architecture changes.
     * @param l long containing bits to be inserted into the specified field
     * of this (receiver) word. This long will not be shifted; its contents
     * should already be in the positions the bitfield should occupy.
     * @param b Specifies the bits to set. For our simpleminded rewriting
     * to work, this parameter <em>must</em> be obtained from a direct
     * invocation, at the call site, of the static method <code>bf()</code>
     * of the appropriate Bitfield class.
     * @return Receiver word mutated.
     * @throws BCbitfield <em>this is a pragma</em>
     **/
    public final VM_Word unshiftedSet(long l, Bitfield b) throws BCbitfield {
        b.check(BfRewriter.LONG, false);
        value &= ~b.mask;
        value |= ((int) l) & b.mask;
        return this;
    }

    //		    //
    // Comparisons: //
    //		    //

    /**
     * Zero predicate.
     * @return true iff this word (hosted: its value) is zero
     * @throws BCeq0 <em>this is a pragma</em>
     **/
    public final boolean isZero() throws BCeq0 {
        return value == 0;
    }

    /**
     * Nonzero predicate.
     * @return true iff this word (hosted: its value) is not zero
     * @throws BCne0 <em>this is a pragma</em>
     **/
    public final boolean isNonZero() throws BCne0 {
        return value != 0;
    }

    /**
     * Value equality.
     * @return true iff both words (hosted: their values) are equal
     * @throws BCeq <em>this is a pragma</em>
     **/
    public final boolean EQ(VM_Word other) throws BCeq {
        return value == other.value;
    }

    /**
     * Value inequality.
     * @return true iff the words (hosted: their values) are not equal
     * @throws BCne <em>this is a pragma</em>
     **/
    public final boolean NE(VM_Word other) throws BCne {
        return value != other.value;
    }

    /**
     * Value strictly less (signed).
     * @return true iff this word (hosted: value) strictly less than other
     * in signed comparison.
     * @throws BCslt <em>this is a pragma</em>
     **/
    public final boolean sLT(VM_Word other) throws BCslt {
        return value < other.value;
    }

    /**
     * Value weakly less (signed).
     * @return true iff this word (hosted: value) weakly less than other
     * in signed comparison.
     * @throws BCsle <em>this is a pragma</em>
     **/
    public final boolean sLE(VM_Word other) throws BCsle {
        return value <= other.value;
    }

    /**
     * Value weakly greater (signed).
     * @return true iff this word (hosted: value) weakly greater than other
     * in signed comparison.
     * @throws BCsge <em>this is a pragma</em>
     **/
    public final boolean sGE(VM_Word other) throws BCsge {
        return value >= other.value;
    }

    /**
     * Value strictly greater (signed).
     * @return true iff this word (hosted: value) strictly greater than other
     * in signed comparison.
     * @throws BCsgt <em>this is a pragma</em>
     **/
    public final boolean sGT(VM_Word other) throws BCsgt {
        return value > other.value;
    }

    /**
     * Signed compare ordering.
     * @return -1, 0, or 1 as this Word is less than, equal to, or greater
     * than other.
     * @throws BCscmp <em>this is a pragma</em>
     **/
    public final int sCMP(VM_Word other) throws BCscmp {
        return value == other.value ? 0 : value < other.value ? -1 : 1;
    }

    /**
     * Unsigned compare ordering.
     * @return -1, 0, or 1 as this Word is below, equal to, or above other.
     * @throws BCucmp <em>this is a pragma</em>
     **/
    public final int uCMP(VM_Word other) throws BCucmp {
        return value == other.value ? 0 : uLT(other) ? -1 : 1;
    }

    /**
     * Value strictly less (unsigned).
     * @return true iff this word (hosted: value) strictly less than other
     * in unsigned comparison.
     * @throws BCult <em>this is a pragma</em>
     **/
    public final boolean uLT(VM_Word other) throws BCult, ovm.core.services.memory.PragmaNoBarriers {
        return (value ^ other.value) >= 0
            ? value < other.value
            : value > other.value;
    }

    /**
     * Value weakly less (unsigned).
     * @return true iff this word (hosted: value) weakly less than other
     * in unsigned comparison.
     * @throws BCule <em>this is a pragma</em>
     **/
    public final boolean uLE(VM_Word other) throws BCule {
        return (value ^ other.value) >= 0
            ? value <= other.value
            : value > other.value;
    }

    /**
     * Value weakly greater (unsigned).
     * @return true iff this word (hosted: value) weakly greater than other
     * in unsigned comparison.
     * @throws BCuge <em>this is a pragma</em>
     **/
    public final boolean uGE(VM_Word other) throws BCuge {
        return (value ^ other.value) >= 0
            ? value >= other.value
            : value < other.value;
    }

    /**
     * Value strictly greater (unsigned).
     * @return true iff this word (hosted: value) strictly greater than other
     * in unsigned comparison.
     * @throws BCugt <em>this is a pragma</em>
     **/
    public final boolean uGT(VM_Word other) throws BCugt {
        return (value ^ other.value) >= 0
            ? value > other.value
            : value < other.value;
    }
    
    //        //
    // Logic: //
    //        //
    
    /** Logical AND.
     * @return New word
     * @throws BCiand <em>this is a pragma</em>
     **/
    public final VM_Word and(VM_Word other) throws BCiand {
      return new VM_Word(value&other.value);
    }
    
    /** Logical OR.
     * @return New Word
     * @throws BCior <em>this is a pragma</em>
     **/
    public final VM_Word or(VM_Word other) throws BCior {
      return new VM_Word(value|other.value);
    }
    
    /** Logical exclusive OR.
     * @return New Word
     * @throws BCixor <em>this is a pragma</em>
     **/
    public final VM_Word xor(VM_Word other) throws BCixor {
      return new VM_Word(value ^ other.value);
    }
    
    /** Logical bitwise inversion.
     * @return New Word.
     * @throws BCinvert <em>this is a pragma</em>
     **/
    public final VM_Word invert() throws BCinvert {
      return new VM_Word(~value);
    }
    
    /** Arithmetic shift left (multiply by two).
     * @param positions How many bit positions to shift. If the parameter equals
     * or exceeds 8 times {@link #widthInBytes()}, it may be reduced modulo that
     * value.
     * @return This Word.
     * @throws BCishl <em>this is a pragma</em>
     **/
    public final VM_Word shiftL(int positions) throws BCishl {
      return new VM_Word(value << positions);
    }
    
    /** Arithmetic shift right (divide by two): replicas of the high (sign) bit
     *  shift in at the left.
     * @param positions How many bit positions to shift. If the parameter equals
     * or exceeds 8 times {@link #widthInBytes()}, it may be reduced modulo that
     * value.
     * @return New Word
     * @throws BCishr <em>this is a pragma</em>
     **/
    public final VM_Word arithmeticShiftR(int positions) throws BCishr {
      return new VM_Word(value >> positions);
    }
    
    /** Logical shift right (unsigned divide by two): zeros shift in at the left.
     * @param positions How many bit positions to shift. If the parameter equals
     * or exceeds 8 times {@link #widthInBytes()}, it may be reduced modulo that
     * value.
     * @return New Word
     * @throws BCiushr <em>this is a pragma</em>
     **/
    public final VM_Word logicalShiftR(int positions) throws BCiushr {
      return new VM_Word(value>>>positions);
    }
    
    public final String toString() throws BCtostring {
        return "#" + Integer.toHexString( value);
    }

    //					//
    // Substitute bytecode definitions: //
    //					//

    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BC
        extends PragmaTransformCallsiteIR
        implements Ephemeral.Void {
        static {
            r("", null);
            r("iconst_4", ICONST_4);
            r("iadd", 
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(IADD);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("isub",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(ISUB);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("imul",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(IMUL);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("idiv",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(IDIV);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("irem",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(IREM);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r(
                "eq0",
                new Rewriter() {
                    protected boolean rewrite() {
                        addTrailingIntEq0();
                        return true; // delete original instruction
                    }
                });
            r(
                "ne0",
                new Rewriter() {
                    protected boolean rewrite() {
                        addTrailingIntNe0();
                        return true; // delete original instruction
                    }
                });
            r(
                "eq",
                new byte[] {
                    (byte) IF_ICMPEQ,
                    0,
                    7,
                    (byte) ICONST_0,
                    (byte) GOTO,
                    0,
                    4,
                    (byte) ICONST_1 });
            r(
                "ne",
                new byte[] {
                    (byte) IF_ICMPNE,
                    0,
                    7,
                    (byte) ICONST_0,
                    (byte) GOTO,
                    0,
                    4,
                    (byte) ICONST_1 });
            r(
                "slt",
                new byte[] {
                    (byte) IF_ICMPLT,
                    0,
                    7,
                    (byte) ICONST_0,
                    (byte) GOTO,
                    0,
                    4,
                    (byte) ICONST_1 });
            r(
                "sle",
                new byte[] {
                    (byte) IF_ICMPLE,
                    0,
                    7,
                    (byte) ICONST_0,
                    (byte) GOTO,
                    0,
                    4,
                    (byte) ICONST_1 });
            r(
                "sge",
                new byte[] {
                    (byte) IF_ICMPGE,
                    0,
                    7,
                    (byte) ICONST_0,
                    (byte) GOTO,
                    0,
                    4,
                    (byte) ICONST_1 });
            r(
                "sgt",
                new byte[] {
                    (byte) IF_ICMPGT,
                    0,
                    7,
                    (byte) ICONST_0,
                    (byte) GOTO,
                    0,
                    4,
                    (byte) ICONST_1 });
            r(
                "scmp",
                new byte[] {
                    (byte) INVOKE_SYSTEM,
                    InvokeSystemArguments.WORD_OP,
                    WordOps.sCMP });
            r(
                "ucmp",
                new byte[] {
                    (byte) INVOKE_SYSTEM,
                    InvokeSystemArguments.WORD_OP,
                    WordOps.uCMP });
            r(
                "ult",
                new byte[] {
                    (byte) INVOKE_SYSTEM,
                    InvokeSystemArguments.WORD_OP,
                    WordOps.uLT });
            r(
                "ule",
                new byte[] {
                    (byte) INVOKE_SYSTEM,
                    InvokeSystemArguments.WORD_OP,
                    WordOps.uLE });
            r(
                "uge",
                new byte[] {
                    (byte) INVOKE_SYSTEM,
                    InvokeSystemArguments.WORD_OP,
                    WordOps.uGE });
            r(
                "ugt",
                new byte[] {
                    (byte) INVOKE_SYSTEM,
                    InvokeSystemArguments.WORD_OP,
                    WordOps.uGT });
            r("iand",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(IAND);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("ior",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(IOR);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("ixor",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(IXOR);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("invert",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(ICONST_M1);
		      cursor.addSimpleInstruction(IXOR);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("ishl",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(ISHL);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("ishr",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(ISHR);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            r("iushr",
	      new Rewriter() {
		  protected boolean rewrite() {
		      cursor.addSimpleInstruction(IUSHR);
		      addFiat();
		      return true; // delete original instruction
		  }
	      });
            
            r("bitfield", new BfRewriter());
            r("tostring", new PragmaTransformCallsiteIR.Rewriter() {
                    final ConstantMethodref iths =
                        RepositoryUtils.selectorFor(
                            JavaNames.java_lang_Integer.getGemeinsamTypeName(),
                            JavaNames.java_lang_String,
                            "toHexString",
                            new TypeName[] { TypeName.INT });
                    final RepositoryString grid =
                        new RepositoryString( "#");
                    protected boolean rewrite() {
                        cursor.addFiat( TypeName.INT);  // i
                        cursor.addINVOKESTATIC( iths);  // hex
                        cursor.addNEW( JavaNames.       // hex [sb]
                            java_lang_StringBuffer);
                        cursor.addSimpleInstruction( DUP); // hex [sb] [sb]
                        cursor.addLoadConstant( grid);    // hex [sb] [sb] #
                        cursor.addINVOKESPECIAL( JavaNames.    // hex sb
                            java_lang_StringBuffer_STRING_Constructor);
                        cursor.addSimpleInstruction( SWAP); // sb hex
                        cursor.addINVOKEVIRTUAL( JavaNames.
                            java_lang_StringBuffer_STRING_append); // sb
                        cursor.addINVOKEVIRTUAL( JavaNames.
                            java_lang_Object_toString);
                        return true;
                    }
                });
	    r("i2l", new byte[] { (byte) I2L });
	    r("l2i", new byte[] { (byte) L2I });
	    r("signedRange",
	      new Rewriter() {
		  protected boolean rewrite() {
		      // If this value is in the range of a signed
		      // int, the top 33 bits are either all zeros, or
		      // all ones.
		      cursor.addLoadConstant(31);
		      cursor.addSimpleInstruction(LSHR);
		      cursor.addLoadConstant(1L);
		      cursor.addSimpleInstruction(LADD);
		      // If the value is in range, the top of the
		      // stack contains 0 or 1
		      cursor.addLoadConstant(1);
		      cursor.addSimpleInstruction(LUSHR);
		      // The top of the stack contains 0 if the value
		      // is in range, or a positive value otherwise
		      cursor.addLoadConstant(0L);
		      cursor.addSimpleInstruction(LCMP);
		      // Invert result
		      cursor.addLoadConstant(1);
		      cursor.addSimpleInstruction(IXOR);
		      return true;
		  }
	      });
	    r("unsignedRange",
	      new Rewriter() {
		  protected boolean rewrite() {
		      // If the value is in range, the top 32 bits are
		      // all zero
		      cursor.addLoadConstant(32);
		      cursor.addSimpleInstruction(LUSHR);
		      cursor.addLoadConstant(0L);
		      cursor.addSimpleInstruction(LCMP);
		      cursor.addLoadConstant(1);
		      cursor.addSimpleInstruction(IXOR);
		      return true;
		  }
	      });
        }
        /** OVM-specific: implementation detail: register substitute bytecode
        *  for a subpragma of this pragma. <code>bc</code> should be either a
        *  byte array or, for those tough jobs, a Rewriter.
        **/
        protected static void r(String name, Object bc) {
            // SYNCHRONIZE THIS STRING WITH CLASS/PACKAGE NAME CHANGES
            register("ovm.core.services.memory.VM_Word$BC" + name, bc);
        }
        /** OVM-specific: implementation detail: register substitute bytecode
        *  (single byte) for a subpragma of this pragma.
        **/
        protected static void r(String name, int bc) {
            r(name, new byte[] {(byte) bc });
        }
    }

    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCiconst_4 extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCiadd extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCisub extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCimul extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCidiv extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCirem extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCeq0 extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCne0 extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCeq extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCne extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCslt extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsle extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsge extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsgt extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCscmp extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCucmp extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCult extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCule extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCuge extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCugt extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCiand extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCior extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCixor extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCinvert extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCishl extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCishr extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCiushr extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCi2l extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCl2i extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCsignedRange extends BC {}
    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCunsignedRange extends BC {}
    public static class BCtostring extends BC {}

    /** OVM-specific: implementation detail: bytecode rewriting pragma. **/
    public static class BCbitfield extends BC {}

    /** Class to be extended in defining a specific bitfield. For example,
     *  a bitfield <em>Color</em> consisting of 3 bits at positions 5, 6, and 7
     *  could be defined as follows:
     * <pre>
     *  class <font color="green">Color</font> extends Bitfield {
     *    static final int WIDTH = <font color="green">3</font>, SHIFT = <font color="green">5</font>;
     *    static final Bitfield bf = bf( WIDTH, SHIFT);
     *  }
     * </pre>
     * This is ungainly but mostly unvarying syntax; the colored parts are all
     * you need to change when defining a new bitfield. A macro language or
     * preprocessor could make the syntax prettier.
     *<p>
     * The Color bitfield could then be used in code like the following:
     * <pre>
     * static final int BLACK=0, RED=1, ... INDIGO=6, VIOLET=7;
     * ...
     * VM_Word status = getMyStatus();
     * int color = status.asInt( Color.bf);
     * status = status.set( INDIGO, Color.bf);
     * setMyStatus( status);
     * </pre>
     * The last actual argument to any bitfield operation is syntactically
     * the <code>bf</code> field of the desired bitfield, but it is only
     * a key for rewriting; what really happens is the entire bitfield
     * operation is rewritten to efficient
     * inlined bytecodes and constants. When running hosted during image build,
     * the operations really do make method calls, but still
     * perform (less efficiently) the same bitfield manipulations, so this
     * mechanism is usable in code that will run both at image build and run
     * time.
     **/
    public static class Bitfield {
        final int width, shift, mask;

        private Bitfield(int width, int shift) {
            if (width < 1 || shift < 0 || width + shift > 32) // wordsize!
                throw new FantasticBitfieldException(
                    width + " bits at " + shift);
            this.width = width;
            this.shift = shift;
            this.mask = (-1 >>> (32 - width)) << shift; // wordsize!
        }
        public static Bitfield bf(int width, int shift) {
            return new Bitfield(width, shift);
        }
        public void effect( Rewriter src, boolean set, boolean _shift, int _width) {
            new BfRewriter().effect( src, this, set, _shift, _width);
        }
        void check(int wordsize, boolean shifted) {
            if (wordsize < width + (shifted ? 0 : shift))
                throw new BitfieldCastException(
                    width + " bits at " + shift + " in " + wordsize + " bits?");
        }
        protected Bitfield() {
            throw new OVMRuntimeException.IllegalAccess(
                "Avoid using the no-arg Bitfield constructor.");
        }

    }

    /**OVM-specific: implementation detail: rewriter for bitfield operations.**/
    public static class BfRewriter extends Rewriter {

        // it is safe to mutate the following. p.i.s.b.Rewriters are cloned
        // before each use.
        private int widthCheck;
        public static final int NO_CHECK = 0;
        public static final int INT = 32;
        public static final int LONG = 64;
        private boolean set;
        private boolean shifted;
        private int width;
        private int shift;
        private int mask;
        private static final UnboundSelector[] lookup = {
            // the order is significant. index%2 indicates shift/noshift
            // index>>>1 % 3 indicates word/int/long
            // index/6 indicates get/set
            rusw("get:("),
            rusw("unshiftedGet:("),
            rusi("asInt:("),
            rusi("unshiftedAsInt:("),
            rusl("asLong:("),
            rusl("unshiftedAsLong:("),
            rusw("set:(Lovm/core/services/memory/VM_Word;"),
            rusw("unshiftedSet:(Lovm/core/services/memory/VM_Word;"),
            rusw("set:(I"),
            rusw("unshiftedSet:(I"),
            rusw("set:(J"),
            rusw("unshiftedSet:(J")};
        private static final TypeName bitfieldTypeName =
            CachingOvmFormat._.parseTypeName(
                "Lovm/core/services/memory/VM_Word$Bitfield;");

        private static UnboundSelector rusi(String s) {
            return CachingOvmFormat._.parseUnboundSelector(
                s + "Lovm/core/services/memory/VM_Word$Bitfield;)I");
        }
        private static UnboundSelector rusl(String s) {
            return CachingOvmFormat._.parseUnboundSelector(
                s + "Lovm/core/services/memory/VM_Word$Bitfield;)J");
        }
        private static UnboundSelector rusw(String s) {
            return CachingOvmFormat._.parseUnboundSelector(
                s
                    + "Lovm/core/services/memory/VM_Word$Bitfield;)"
                    + "Lovm/core/services/memory/VM_Word;");
        }
        // sets set, shifted, and widthCheck
        private void determineOperation(UnboundSelector method) {
            int i;
            for (i = 0; i < lookup.length; ++i)
                if (lookup[i] == method)
                    break;
            assert(i < lookup.length);
            // hey! see order of lookup table above
            shifted = (i % 2) == 0;
            set = i > 5;
            switch ((i >>> 1) % 3) {
                case 0 :
                    widthCheck = NO_CHECK;
                    break;
                case 1 :
                    widthCheck = INT;
                    break;
                case 2 :
                    widthCheck = LONG;
                    break;
            }
        }
        private void getWidthShiftFromRC(Type t) {
            width = finalField(t.asScalar(), "WIDTH");
            shift = finalField(t.asScalar(), "SHIFT");
        }
        // width and shift already set; checks sanity, sets mask
        private void checkAndInit(String selString) {
            if (width < 1 || shift < 0 || 32 < width + shift)
                throw new FantasticBitfieldException(
                    targetBP.getName().toString());
            if (widthCheck != NO_CHECK
                && widthCheck < width + (shifted ? 0 : shift))
                throw new BitfieldCastException(
                    targetBP.getName().toString() + "." + selString);
            mask = (-1 >>> (32 - width)) << shift;
        }
        private int finalField(Type.Scalar t, String name) {
	    t = t.getSharedStateType();
	    UnboundSelector.Field ubs =
		RepositoryUtils.makeUnboundSelector(name + ":I").asField();
            Field f = t.getField(ubs);
	    if (f == null)
		throw new Error(t + " does not define constant field " + name);
	    Integer w = (Integer) f.getConstantValue();
	    if (w == null)
		throw new Error(f + " is not a compile-time constant");
	    return w.intValue();
        }
        private void addMask() { // w -- w'
            if (mask == -1)
                return;
            cursor.addLoadConstant(mask);
            cursor.addSimpleInstruction(IAND);
        }
        private void addInsertUnderMask() { // w v -- w'
            addMask(); // w v'
            cursor.addSimpleInstruction(SWAP); // v' w
            cursor.addLoadConstant(~mask); // v' w ~m
            cursor.addSimpleInstruction(IAND); // v' w&~m
            cursor.addSimpleInstruction(IOR); // w'
        }
        private void addShift() {
            if (shift == 0)
                return;
            cursor.addLoadConstant(shift);
            cursor.addSimpleInstruction(ISHL);
        }
        private void addUnShift() {
            if (shift == 0)
                return;
            cursor.addLoadConstant(shift);
            cursor.addSimpleInstruction(IUSHR);
        }
        private void addSet() {
            if (widthCheck == LONG)
                cursor.addSimpleInstruction(L2I);
            if (shifted)
                addShift();
            addInsertUnderMask();
        }
        private void addGet() {
            addMask();
            if (shifted)
                addUnShift();
            if (widthCheck == LONG)
                cursor.addINVOKESYSTEM(
                    InvokeSystemArguments.WORD_OP,
                    WordOps.uI2L);
        }
        protected boolean rewrite() {
            UnboundSelector us = targetSel.getUnboundSelector();
            determineOperation(us);

            int pc = sitePC - 3; // FIXME hackery! 3 for size of GETSTATIC
            Instruction inst = code.get(pc); // does not advance, but does ...
            if (!(inst instanceof Instruction.FieldAccess)
                || (sitePC != code.getPC() + inst.size(code)))
                throw new PuntException();

            Instruction.FieldAccess igs = (Instruction.FieldAccess) inst;
            Selector.Field fld = igs.getSelector(code, cp);
            TypeName.Compound bftn = fld.getDefiningClass();
            Type.Context tc = siteBP.getType().getContext();
            Type bft;
            try {
                bft = tc.typeFor(bftn);

		if (bftn instanceof TypeName.Scalar.Gemeinsam) {
                    bftn =
                        ((TypeName.Scalar.Gemeinsam) bftn)
			.getInstanceTypeName().asCompound();
                    bftn = tc.typeFor(bftn).getSuperclass().getName();
                } else {
                    bftn = bft.getSuperclass().getName();
                }
            } catch (LinkageException le) {
                throw (PuntException) new PuntException().initCause(le);
            }

            if (bitfieldTypeName != bftn)
                throw new PuntException();

            getWidthShiftFromRC(bft);
            checkAndInit( us.toString());

	    // remove the GETSTATIC
	    cfe.removeInstruction(pc);

            if (set)
                addSet();
            else
                addGet();

            return true;
        }
        public void effect(
            Rewriter src,
            TypeName.Scalar bitfield,
            boolean set,
            boolean shifted,
            int typeWidth) {
            Type.Context ctx = targetBP.getType().getContext();
            // ctx = ctx.getDomain().getSystemTypeContext(); // what ctx to use?
            Blueprint.Factory bpf = ctx.getDomain();
            try {
                targetBP = bpf.blueprintFor(bitfield, ctx);
            } catch (LinkageException le) {
                throw le.fatal();
            }
            BfRewriter rcv = (BfRewriter)mirror( src);
            rcv.getWidthShiftFromRC( targetBP.getType());
            rcv.effect(set, shifted, typeWidth);
        }
        public void effect(
            Rewriter src,
            Bitfield bf,
            boolean _set,
            boolean _shifted,
            int typeWidth) {
                BfRewriter rcv = (BfRewriter)mirror( src);
                rcv.width = bf.width;
                rcv.shift = bf.shift;
                rcv.effect( _set, _shifted, typeWidth);
        }
        // width and shift must already be set
        private void effect(boolean _set, boolean _shifted, int typeWidth) {
            if (typeWidth != NO_CHECK && typeWidth != INT && typeWidth != LONG)
                throw new IllegalArgumentException("typeWidth: " + typeWidth);
            this.set = _set;
            this.shifted = _shifted;
            this.widthCheck = typeWidth;
            
            checkAndInit(
                (shifted ? "" : "unshifted") + // pass useful exception
                (set ? "set" : "get") +        // message just in case
                ((typeWidth == INT) ? ":int" : "")
                + ((typeWidth == LONG) ? ":long" : ""));
            
            if (set)
                addSet();
            else
                addGet();
        }
    }

    /** Indicates attempt to use a method that accesses a bitfield as a fixed
     *  size Java type (int or long) when the bitfield won't fit.
     **/
    public static class BitfieldCastException extends OVMRuntimeException {
        public BitfieldCastException(String s) {
            super(s);
        }
    }

    /** Indicates a definition of a bitfield with outlandish values for
     *  width or shift.
     **/
    public static class FantasticBitfieldException
        extends OVMRuntimeException {
        public FantasticBitfieldException(String s) {
            super(s);
        }
    }
}
