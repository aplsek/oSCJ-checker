package ovm.services.bytecode;
import ovm.core.OVMBase;
import ovm.core.domain.Code;
import ovm.core.domain.Method;
import ovm.core.repository.Bytecode;
import ovm.core.repository.Constants;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.Selector;
import ovm.util.ByteBuffer;
import ovm.util.OVMError;
import s3.core.domain.S3ByteCode;

/** 
 * Allows to view a bytecode buffer as a read-only sequence of
 * Instruction objects, supports iterating over all the instructions in the
 * buffer.
 *<p>
 * The state of an InstructionBuffer includes two distinct notions:
 * a <strong>position</strong>, which tracks (in close analogy to other buffers)
 * the next instruction that <em>is to be</em> returned in a straightforward
 * traversal of the buffer, and a <strong>PC</strong>, which is the index of
 * the opcode of the most-recently-returned instruction (and so ordinarily
 * trails behind the <em>position</em>).  The <em>PC</em> is needed by code
 * that attempts to analyze the lately-returned Instruction.
 *<p>
 * When {@link #getCode()} is used to obtain the ByteBuffer backing this
 * InstructionBuffer, the ByteBuffer's <em>position</em> corresponds to this
 * InstructionBuffer's <em>PC</em>. This is happy behavior, because
 * {@link #getCode()} is used in analysis of the lately-returned instruction.
 *<p>
 * Care is needed when retrieving multiple Instructions from an
 * InstructionBuffer and then querying the Instructions in ways that access
 * their immediate operands. See {@link #duplicate()}.
 **/

public class InstructionBuffer implements MethodInformation/* evil */, Cloneable {
    private Constants constantPool_;
    private Selector.Method selector_;
    private InstructionSet is_ = InstructionSet.SINGLETON;
    /**
     * The underlying buffer of bytecode.  Our invariant: the
     * {@link ByteBuffer#position() position} of this underlying buffer is
     * that of the opcode of the "current" instruction--the one most
     * recently returned by {@link #get()} or {@link #get(int)} and
     * which will be returned by {@link #current()} until another
     * {@link #get()} or {@link #get(int)}. This is
     * <em>counterintuitive</em> with respect to the usual behavior for
     * a Buffer; the role of our "position" (index that points
     * <em>past</em> the lately-returned instruction) is played by our
     * own instance variable {@link #next_}.
     *<p>
     * It would have been just as easy and more intuitive to identify our
     * "position" with that of the backing ByteBuffer, and use our new instance
     * variable to maintain the new concept of a current PC.  But the
     * counterintuitive approach has an advantage: client code frequently
     * queries for our backing ByteBuffer and expects its position to be the
     * current PC--which it naturally is because of the way we have set it up.
     *<p>
     * The initial state of an InstructionBuffer just after construction or
     * {@link #rewind()} is <code>next_ == 0</code> and
     * <code>code_.position() == code_.limit()</code>. By our invariant that
     * means there is no current (lately-returned) instruction, and that's the
     * right idea.
     **/
    private ByteBuffer code_;
    /**
     * The position of next Instruction to be returned by get().
     **/
    private int next_;

    protected InstructionBuffer(Bytecode cf) {
        this.constantPool_ = cf.getConstantPool();
        this.selector_ = cf.getSelector();
        this.code_ = ByteBuffer.wrap(cf.getCode());
        this.is_ = InstructionSet.SINGLETON;
        rewind(); // establish initial condition
    }

    protected InstructionBuffer(ByteBuffer code,
				Selector.Method sel,
				Constants cp) {
        this.code_ = code;
        this.selector_ = sel;
        this.constantPool_ = cp;
        this.is_ = InstructionSet.SINGLETON;
        rewind(); // establish initial condition
    }

    public String toString() {
	int nx = next_;
	StringBuffer ret = new StringBuffer();
	rewind();
	while (hasRemaining()) 
	    ret.append(position() + "\t" + get().toString(this, constantPool_) + "\n");
	next_ = nx;
	return ret.toString();
    }

    /**
     * ONLY use allocate if you really, really mean to make a copy
     * of the Bytecode/cf.  Otherwise use "wrap".
     */
    public static InstructionBuffer allocate(Bytecode cf) {
        byte[] cd = (byte[]) cf.getCode().clone();
        return new InstructionBuffer(  ByteBuffer.wrap(cd),
				     cf.getSelector(),
				     cf.getConstantPool());
    }

    public static InstructionBuffer wrap(Bytecode cf) {
        return new InstructionBuffer(cf);
    }

    public static InstructionBuffer wrap(RepositoryMember.Method me) {
        return new InstructionBuffer(me.getCodeFragment());
    }

    public static InstructionBuffer wrap(ByteBuffer code,
					 Selector.Method sel,
					 Constants cp) {
	return new InstructionBuffer(code, sel, cp);
    }
    
    /** 
     * Maybe instead Code.getCode() should just return an
     * InstructionBuffer instead of a byte array? Whatever.
     **/
    public static InstructionBuffer wrap(Code c) {
        Method m = c.getMethod();
        if (c.getKind() != S3ByteCode.KIND)
            c = m.getByteCode();
        if (!(c instanceof S3ByteCode))
            throw new OVMError.IllegalArgument("Trying to wrap non-bytecode: " + c);
        S3ByteCode s3bc = (S3ByteCode) c;
        Selector.Method sel = m.getSelector();
        Constants rcp = s3bc.getConstantPool();
        byte[] code = s3bc.getBytes();
        return new InstructionBuffer(ByteBuffer.wrap(code), sel, rcp);
    }

    //			          //
    //  Buffer-analogous methods   //
    //			         //
    /**
     * Query the <em>position</em> of this InstructionBuffer. By analogy to
     * any other Buffer, that is the index of the next instruction yet to be
     * returned.  It is <em>not</em> the PC of the lately-returned instruction;
     * use {@link #getPC()} for that.
     * @return this InstructionBuffer's position.
     **/
    public int position() {
        return next_;
    }
    /**
     * Adjust the <em>position</em> of this InstructionBuffer. By analogy to
     * any other Buffer, that is the index of the next instruction yet to be
     * returned.  It is <em>not</em> the PC of the lately-returned instruction
     * (that value only changes with {@link #get()} or {@link #get(int)}).
     * @param pos the new position
     * @return this InstructionBuffer after alteration
     * @throws IllegalArgumentException if <code>pos</code> is out of range.
     **/
    public InstructionBuffer position(int pos) {
        if (0 > pos || pos > limit())
            throw new IllegalArgumentException(
                "newPosition " + pos + " not in (0 ," + limit() + ")");
        next_ = pos;
        return this;
    }
    /**
     * Query code size.
     * @return number of bytes (not number of instructions)
     **/
    public int limit() {
        return code_.limit();
    }
    /**
     * Establish initial condition: {@link #get()} will return the first
     * instruction and, until then, there <em>is no</em> current instruction.
     **/
    public InstructionBuffer rewind() {
        code_.position(code_.limit());
        next_ = 0;
        return this;
    }
    /**
     * Query how much is left.
     * @return The number of bytes (not instructions) from this
     * InstructionBuffer's position to its limit.
     **/
    public int remaining() {
        return code_.limit() - next_;
    }
    /**
     * Is there at least one byte (not necessarily, alas, one complete
     * instruction) remaining to get?
     * @return true iff the "position" of this InstructionBuffer (not of the
     * underlying ByteBuffer) is within the underlying ByteBuffer's limit.
     **/
    public boolean hasRemaining() {
        return code_.limit() > next_;
    }
    //			         //
    //  FooBuffer-analogous methods  //
    //			         //
    /**
     * The returned InstructionBuffer shares the same sequence of instructions,
     * constant pool, selector, and instruction set, but has its own
     * independent state (position and PC). This means the backing ByteBuffer
     * is also duplicated, as the ByteBuffer's <em>position</em> is what
     * represents the InstructionBuffer's <em>PC</em>.
     *<p>
     * This method may be needed if you retrieve more than one Instruction
     * from an InstructionBuffer and then need to query their immediate
     * operands. Instruction queries require access to the InstructionBuffer
     * and assume its PC corresponds to the instruction; but an
     * InstructionBuffer has only one PC corresponding to the latest
     * Instruction retrieved. If necessary, you can duplicate the
     * InstructionBuffer before retrieving another instruction, and then you
     * can issue queries on both Instructions (provided you associate each
     * with the right InstructionBuffer).
     *<p>
     * Where possible, it's easier to just query all the information you need
     * from one instruction before retrieving another.
     * @return InstructionBuffer sharing the same content but with independent
     * position and PC.
     **/
    public InstructionBuffer duplicate() {
        try {
            InstructionBuffer ib = (InstructionBuffer) clone();
            ib.code_ = code_.duplicate();
            return ib;
        } catch (CloneNotSupportedException cnse) {
            throw OVMBase.failure(cnse);
        }
    }
    /**
     * Give me the next instruction, make it current (arrange for
     * {@link #getPC()} and {@link #getCode()}&#x2e;
     * {@link ovm.util.ByteBuffer#position() position()} to return its
     * address), and advance this InstructionBuffer's own {@link #position}
     * beyond it.
     * @return the new current Instruction
     **/
    public Instruction get() {
        code_.position(next_);
        Instruction current = current();
        next_ += current.size(this);
        return current;
    }
    /**
     * Get the instruction at a specified PC and make it current, but without
     * affecting the position of this InstructionBuffer. By analogy to the
     * absolute get methods on any Buffer, this method <em>does not</em> alter
     * the <em>position</em> of this InstructionBuffer (i.e. what instruction
     * will be returned by a subsequent call to {@link #get()})--but it
     * <em>does alter</em> the <em>PC</em> (which is also the <em>position</em>
     * of the backing ByteBuffer) to correspond to the instruction returned.
     *<p>
     * If you do want to affect the <em>position</em> (so subsequent uses of
     * {@link #get()} will return instructions following this one), use the
     * idiom {@link #position(int) position(pc)}.{@link #get()}.
     * @return Instruction at the given PC (unpredictable if this PC points
     * <em>into</em> an instruction).
     **/
    public Instruction get(int pc) {
        code_.position(pc);
        return current();
    }
    //			      	     //
    //  Evil MethodInformation methods   //
    //				     //
    /**
     * Get the code of the method that we are analyzing. This should
     * be used rarely, if at all. Or be a private interface between
     * Instruction and InstructionBuffer.
     * @return ByteBuffer whose {@link ovm.util.ByteBuffer#position()
     * position} is at 
     * the opcode of the current instruction (the one that would be returned
     * by {@link #current()}). If this method is called before there <em>is</em>
     * a current instruction, the returned ByteBuffer will be positioned at its
     * limit--get() would fail.
     **/
    public ByteBuffer getCode() {
        return code_;
    }
    /**
     * Get the constant pool of the method that we are analyzing.
     **/
    public Constants getConstantPool() {
        return constantPool_;
    }
    /**
     * Get the selector of the method that we are analyzing.
     * @return null if no selector is available
     **/
    public Selector.Method getSelector() {
        return selector_;
    }
    /**
     * Get the current PC into the method that we are analyzing.
     * The value reflects the most recent use of {@link #get()}
     * or {@link #get(int)}. This does not have side effect.
     * 
     * @return the index of the current (lately returned) instruction's
     * opcode--<em>not</em> the "position" of this InstructionBuffer (which
     * would be the index of the instruction to be returned next).
     **/
    public int getPC() {
        return code_.position();
    }
    //					 //
    //  InstructionBuffer-specific methods   //
    //					 //
    /**
     * Query the current opcode.
     * @return Opcode of the instruction lately returned by {@link #get()}
     * or {@link #get(int)}.
     **/
    public int currentOpcode() {
        return code_.get(code_.position()) & 0xFF;
    }
    /**
     * Query the current Instruction.
     * @return The current Instruction--just the one lately returned by
     * {@link #get()} or {@link #get(int)}.
     **/
    public Instruction current() {
        return is_.getInstructions()[currentOpcode()];
    }
} 
