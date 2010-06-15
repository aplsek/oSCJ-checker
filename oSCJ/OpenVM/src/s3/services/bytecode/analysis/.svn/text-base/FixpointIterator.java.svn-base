package s3.services.bytecode.analysis;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import s3.core.domain.S3ByteCode;
import ovm.core.repository.ExceptionHandler;
import ovm.core.domain.Method;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import org.ovmj.util.Runabout;
import ovm.services.bytecode.analysis.*;

/**
 * This class implements a fixpoint iteration for the
 * AbstractInterpreter as implemented by S3AbstractInterpreter.  The
 * Iterator iterates until all possible abstract states are
 * found. Note that the implementation of the AbstractValue determines
 * which states are considered different.<p>
 * 
 * @see S3AbstractInterpreter
 * @author Christian Grothoff, jv
 **/
public abstract class FixpointIterator
    implements Instruction.Iterator, WorkList.Initializer {

    public final S3AbstractInterpreter ai;

    /**
     * Our Worklist of Frames to be visited.
     */
    protected final WorkList worklist;

    protected final Selector.Method sel;

    /**
     * The State accumulates abstract frames seen so far to ensure convergence. Only the
     * states needed to ensure termination are recorded.
     * (Copying frames to preserve them in the state is costly, so
     * is merging. Thus we try to do this only if it should benefit convergence.)
     **/
    protected final S3State state;
    protected final Method method;

    /**
     * We use a post-processor to find the next possible frames after
     * each step in the abstract interpretation. This post-processor must
     * be added to the list of visitors *after* the S3AbstractInterpreter
     * by the Configuration. Use "getPostVisitor" to get it.
     **/
    private Runabout processor_;

    protected final InstructionBuffer buf;

    /**
     * Create a fixpoint iterator for the abstract execution of bytecode
     * with the S3AbstractInterpreter.
     **/
    public FixpointIterator(
        S3AbstractInterpreter ai,
        Method me) {
        this(ai, me, InstructionBuffer.wrap(me.getByteCode()));
    }

    public FixpointIterator(
        S3AbstractInterpreter ai,
        Method me,
        InstructionBuffer code) {
        //System.err.println("PROCESSING: " + sel);

        this.ai = ai;
        this.method = me;
        this.sel = me.getSelector();
        this.state = new S3State();
        this.worklist = new WorkList(this);
        this.buf = code;
    }

    public MergeStrategyVisitor makeStrategyVisitor() {
        return new MergeStrategyVisitor();
    }

    /**
      * Using this Fixpoint iterator requires running the post-visitor returned by
      * this method. This PostVisitor adds the next states to visit to the worklist.
      */
    public Runabout getPostVisitor() {
        if (processor_ == null)
            processor_ = makeStrategyVisitor();
        return processor_;
    }

    /**
     * Get the next Instruction and update the Frame and PC of the AbstractInterpreter.
     */
    public Instruction next() {
        Frame f = ai.getFrame();
        if (f != null)
            f.setLastPC(ai.getPC());
        WorkList.Entry curr = null;
        while (worklist.hasNext()) {
            curr = worklist.remove();
            if (curr.tryMerge == WorkList.NOMERGE)
                break;
            f =
                merge(
                    curr.frame,
                    curr.tryMerge == WorkList.MERGECLONE,
                    curr.pc);
            if (f == null) {
                curr = null; // do not simulate this one again
            } else {
                curr.frame = f;
                break;
            }
        }
        if (curr == null)
            return null;
        ai.setFrameAndPC(curr.frame, curr.pc);
        return buf.get(curr.pc);
    }

    /**
     * Initialize frame with the abstract values.
     */
    public abstract Frame makeInitialFrame();

    /**
     * See if the given frame/pc combination must be executed again
     * or if it is already covered by the current state of the
     * abstract interpreter.
     *
     * @param frame the frame that we got by abstract execution
     * @param cloneFrame do we have to clone frame before modifications?
     * @param pc the pc of the next code that would be executed for this frame
     * @return frame to simulate if we need to simulate again, otherwise null
     **/
    protected Frame merge(Frame frame, boolean cloneFrame, int pc) {
        State.Iterator it = state.iterator(pc);
        Frame old = null;
        for (old = it.currentFrame(); old != null; old = it.nextFrame())
            if (old.includes(frame)) {
                return null; // we covered this state, exit 
            }
        it = state.iterator(pc);
        old = null;
        Frame tmp = null;
        for (old = it.currentFrame(); old != null; old = it.nextFrame()) {
            tmp = old.merge(frame);
            if (tmp != null) {
                frame = tmp;
                it.replaceFrame(tmp.cloneFrame());
                break;
            }
        }
        if (tmp == null) {
            state.addFrameAt(pc, frame.cloneFrame());
            // always copy for the state!
            // always copy for the state!
            if (cloneFrame)
                frame = frame.cloneFrame();
            // and potentially copy if this was a branch!
        }
        return frame;
    }

    /**
     * Add a abstract program point to process.
     * @param pc the program counter, >= 0
     * @param f the frame, may not be null
     * @param merge should we merge?
     */
    public void add(int pc, Frame f, int merge) {
        worklist.add(pc, f, merge);
    }

    /**
     * Create the abstract value that should be pushed
     * on the stack on entry to the specified
     * execption handler.
     */
    protected abstract AbstractValue makeExceptionHandlerAV(ExceptionHandler eh);

    /**
     * Determines when to merge states so as to cut down the number of  states.
     *  Since merges are expensive, we try do them only when needed. This
     * implementation  merges at on branching intructions as well as branch targets. 
     * @author Christian Grothoff
     **/
    public class MergeStrategyVisitor extends Runabout {

        /**
         * Join an incoming frame to the existing abstract program state, and
         * update the worklist. ProgramPoints are free to evaluate exceptions.
         */
        public void visit(Instruction i) {
	    add(ai.getPC() + i.size(ai.getPosition()), ai.getFrame(), WorkList.NOMERGE);
            if (i instanceof Instruction.ExceptionThrower)
                addAllExceptionTargets((Instruction.ExceptionThrower) i);
        }

        /**
         * Join *after* we hit a flow-end (return, etc). We don't
         * really do anything.<p>
	 *
	 * FIXME: return is an ExceptionThrower
	 */
        public void visit(Instruction.FlowEnd i) {
        }

        public void visit(Instruction.ATHROW i) {
            addAllExceptionTargets(i);
        }

        protected void addAllExceptionTargets(Instruction.ExceptionThrower i) {
            Frame f = ai.getFrame();
            TypeName.Scalar[] ex = i.getThrowables();
            S3ByteCode rbcf = method.getByteCode();
            ExceptionHandler[] targets = rbcf.getExceptionHandlers();
            int pc = ai.getPC();
            for (int j = 0; j < ex.length; j++) {
                for (int k = 0; k < targets.length; k++) {
                    ExceptionHandler rh = targets[k];
                    if (rh.matchesAtPC(pc)) {
                        f = f.cloneFrame();
                        f.clearStack();
                        f.push(makeExceptionHandlerAV(rh));
                        add(rh.getHandlerPC(), f, WorkList.MERGE);
                        // we have already cloned it!
                    }
                }
            }
        }

        public void visit(Instruction.UnconditionalJump i) {
            add(ai.getPC() + i.getTarget(ai.getPosition()), ai.getFrame(), WorkList.MERGE);
        }

        public void visit(Instruction.ConditionalJump i) {
            add(
                ai.getPC() + i.getBranchTarget(ai.getPosition()),
                ai.getFrame(),
                WorkList.MERGE);
            add(ai.getPC() + i.size(ai.getPosition()), ai.getFrame(), WorkList.MERGECLONE);
        }

        public void visit(Instruction.Switch i) {
            add(
                ai.getPC() + i.getDefaultTarget(ai.getPosition()),
                ai.getFrame(),
                WorkList.MERGE);
            int[] t = i.getTargets(ai.getPosition());
            for (int j = t.length - 1; j >= 0; j--)
                add(ai.getPC() + t[j], ai.getFrame(), WorkList.MERGECLONE);
        }

        public void visit(Instruction.RET i) {
            AbstractValue av = ai.getFrame().load(i.getLocalVariableOffset(ai.getPosition()));
            add(av.getJumpTarget().getTarget(), ai.getFrame(), WorkList.MERGE);
        }

	protected void visitDefault(Object o) {
	}

    } // end of FixpointIterator.MergeStrategyVisitor

} // end of FixpointIterator
