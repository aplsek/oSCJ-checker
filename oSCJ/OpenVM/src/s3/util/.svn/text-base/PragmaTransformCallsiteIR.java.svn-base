package s3.util;

import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.repository.Constants;
import ovm.core.repository.Descriptor;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.services.bytecode.editor.Cursor;
import ovm.services.bytecode.editor.Marker;
import ovm.util.HashMap;
import ovm.util.OVMError;
import ovm.util.OVMRuntimeException;
import ovm.util.logging.Logger;
import s3.services.bootimage.Ephemeral;

/**
 * Hierarchical pragma (see {@link PragmaHierarchy}) to indicate a method
 * whose call sites are to be replaced by inlined bytecode (specified by the
 * pragma, not by inlining the bytecode compiled from the method body).  Each
 * subclass of this pragma registers either an array of bytes, which is used
 * literally in the rewriter to replace the instruction invoking an affected
 * method, or a {@link Rewriter}, whose {@link Rewriter#rewrite() rewrite()}
 * method will rewrite the call site appropriately.
 * @author Chapman Flack
 **/
public class PragmaTransformCallsiteIR /* MUST MATCH STRING! */
    extends PragmaException
    implements JVMConstants.Opcodes, ovm.util.PragmaLiveClass {
  
  private static final TypeName.Scalar me = // UPDATE IF CLASS NAME CHANGED!
    register( PragmaTransformCallsiteIR.class.getName(),
      	      null); // force complaint if a throws declares THIS class directly

  /**
   * @param ms Selector of the method referenced at a call site
   * @param bp Blueprint corresponding to class declaring the method
   * 
   * @return null if the method does <em>not</em> declare (a subclass of)
   * this pragma; otherwise, either a byte array containing fixed bytecode
   * to be substituted for the method invocation, or a {@link Rewriter} whose
   * {@link Rewriter#rewrite() rewrite()} method will rewrite the call site
   * appropriately.
   * 
   * @throws UnregisteredPragmaException if the
   * method declares a subclass of this pragma that explicitly registered
   * <code>null</code> for its byte array; simple way to handle not-yet-
   * implemented special methods.
   **/
  public static Object descendantDeclaredBy( Selector.Method ms,
					     Blueprint bp) {
    return descendantDeclaredBy( me, ms, bp);
  }
  
  public static void failDeadCallsite( String s) {
    throw new DeadCallsiteReachedError( s);
  }
  
  public static class DeadCallsiteReachedError extends OVMError {
    DeadCallsiteReachedError( String s) { super( s); }
  }
  
  /**
   * Thrown by any method whose call sites are to be rewritten to no
   * bytecode at all.
   **/
  public static class BCnothing extends PragmaTransformCallsiteIR {
    static {
      register( "s3.util.PragmaTransformCallsiteIR$BCnothing",
                new byte [ 0 ]);
    }
  }
  
  public static class BCdead extends PragmaTransformCallsiteIR {
    static {
      register( "s3.util.PragmaTransformCallsiteIR$BCdead",
		DeadRewriter.make("method declared dead: "));
    }
  }
  /**
   * Methods which can not be called from the kernel. (May be the wrong name
   * as they could be called from a UserDomain.
   * @author janvitek
   */
  public static class BCbootTime extends BCdead {
    static {
      register( "s3.util.PragmaTransformCallsiteIR$BCbootTime",
        DeadRewriter.make("method declared bootime only: "));
    }
  }
  /**
   * BCbadalloc marks methods that allocate potentially large amounts of
   * temporary storage.  Calls to these methods in a scope would be leaks,
   * but I couldn't see a good way to fix things.
   * @author baker
   */
  public static class BCbadalloc extends PragmaTransformCallsiteIR {
    static {
      register( "s3.util.PragmaTransformCallsiteIR$BCbadalloc",
                DeadRewriter.make("method allocates: "));
    }
  }

  /**
   * Rewritten to a method that returns a null reference.
   * 
   */
  public static class ReturnNull extends PragmaTransformCallsiteIR {
    static {
      register( "s3.util.PragmaTransformCallsiteIR$ReturnNull",
      new Rewriter() {
        protected boolean rewrite() {
            addPopAll();
            cursor.addSimpleInstruction( ACONST_NULL);
            return true;
        }
      });
    }
  }

  public static class Return0 extends PragmaTransformCallsiteIR {
    static {
	register(Return0.class.getName(),
		 new Rewriter() {
		     protected boolean rewrite() {
			 addPopAll();
			 cursor.addSimpleInstruction( ICONST_0);
			 return true;
		     }
      });
    }
  }

    public static class Ignore extends PragmaTransformCallsiteIR {
	static {
	    register(Ignore.class.getName(),
		     new Rewriter() {
			 protected boolean rewrite() {
			     addPopAll();
			     return true;
			 }
		     });
	}
    }

  /**
   * An instance of this class can be registered, instead of a fixed bytecode
   * array, when an InlineSubstituteBytecode pragma needs to do something more
   * interesting than substituting a fixed instruction sequence.
   **/
  public static abstract class Rewriter extends OVMBase
  implements JVMConstants.Opcodes, JVMConstants.PrimitiveArrayTypes,
             Cloneable {
    /**
     * Method that any subclass must implement: given the <em>cursor</em> and
     * any of the other instance variables, all of which are initialized to
     * reflect the call site and the target class and method, rewrite the call
     * site as appropriate. The most common form will likely be some sequence
     * of <code>cursor.add<em>Foo</em>(...)</code>. Return <code>true</code>
     * if the original instruction should be deleted.
     *<p>
     * Things are arranged so that if this method throws a
     * {@link PuntException}
     * (before it has done any editing), a warning will be logged and
     * the call site replaced with an invocation of
     * {@link #failDeadCallsite(String) failDeadCallsite}. What will
     * happen if this method has done some editing and <em>then</em> throws
     * the exception, I would rather not guess.
     * @return <code>true</code> if the original instruction should be deleted.
     **/
    protected abstract boolean rewrite();
    
    /** Blueprint of declaring class of method containing callsite
     * being rewritten **/
    protected Blueprint siteBP;
    /** PC of invocation instruction being rewritten: cache of
     * siteMI.getPC() **/
    protected int sitePC;
    /** Invocation instruction being rewritten **/
    protected Instruction.Invocation siteInst;
    /** Blueprint of declaring class of target method **/
    protected Blueprint targetBP;
    /** Selector of target method: cache of
     * siteInst.getSelector( siteMI) **/
    protected Selector.Method targetSel;
    /** CodeFragmentEditor initialized on the site method **/
    protected CodeFragmentEditor cfe;
    /** Cursor initially positioned on the call site: cache of
     * cfe.getCursorAfterMarker( sitePC) **/
    protected Cursor cursor;

    /** Cache of the code obtained from siteMI **/
    protected InstructionBuffer code;
    /** Cache of the constant pool for siteMI **/
    protected Constants cp;

    /**
     * This is called while rewriting a method when a call site is found that
     * requires the services of this Rewriter. The parameter list is long and
     * unwieldy, but that's ok because this method has only one call site (in
     * the main rewriter) and it makes sense to pass the stuff that's in scope
     * there and might be needed here. Subclasses do not override this method
     * (they'd have to replicate that bulky parameter list, which could occupy
     * more lines than their interesting code). Instead, they just override
     * {@link #rewrite()}, which has access to all the same parameters in the
     * instance variables. The cloning gyrations that make this work are a
     * little unfortunate (Java macros might be nice) but I think it is worth
     * it so subclasses can be written concisely.
     * @param siteBP_ Blueprint of declaring class of method containing callsite
     * being rewritten
     * @param siteInst_ Invocation instruction being rewritten
     * @param targetBP_ Blueprint of declaring class of target method
     * @param targetSel_ Selector of target method: shorthand for
     * siteInst.getSelector( cfe.getOriginalCode())
     * @param cfe_ CodeFragmentEditor initialized on the site method
     **/
    public void rewrite(Blueprint siteBP_,
			Instruction.Invocation siteInst_,
			Blueprint targetBP_,
			Selector.Method targetSel_,
			CodeFragmentEditor cfe_) {
      Rewriter r;
      try {
        r = (Rewriter)clone();
      } catch ( CloneNotSupportedException cnse ) {
        throw new OVMError( cnse);
      }
      r.code = cfe_.getOriginalCode();
      r.cp = r.code.getConstantPool();
      int savedSitePC = cfe_.getOriginalCode().getPC();
      r.siteBP = siteBP_; r.sitePC = savedSitePC;
      r.siteInst = siteInst_; r.targetBP = targetBP_; r.targetSel = targetSel_;
      r.cfe = cfe_; r.cursor = cfe_.getCursorAfterMarker( r.sitePC);
      r.targetBP = targetBP_;
      r.targetSel = targetSel_;
      try {
      	if ( r.rewrite() )
	  cfe_.removeInstruction( savedSitePC);
      } catch ( PuntException pe ) {
        r.code.get( savedSitePC); // first restore PC in case r moved it
      	Logger.global.warning( "Failed to rewrite callsite of " + targetSel_ +
                               ": " + pe);
	pe.printStackTrace();
      	DeadRewriter.make("method could not be rewritten").rewrite
	    (siteBP_, siteInst_, targetBP_,
	     targetSel_, cfe_);
      }
      finally {
        r.code.get( savedSitePC); // restore PC in case r moved it
      }
    }
    /** 
     * Return a clone of this receiver object, whose state is filled in from
     * the argument object. The receiver of an 
     * {@link PragmaTransformCallsiteIR.Rewriter#effect(PragmaTransformCallsiteIR.Rewriter) effect}
     *  call will first clone itself and mirror the caller's state by passing
     *  its argument to this method, and then call {@link #rewrite() rewrite} on
     *  the clone.
     *  @param src reference to the caller's receiver (caller passes
     *  <code>this</code>), whose state will be copied into the clone.
     **/
    protected final Rewriter mirror( Rewriter src) {
      Rewriter rcv;
      try {
      	rcv = (Rewriter)clone();
      } catch ( CloneNotSupportedException cnse ) {
        throw new OVMError( cnse);
      }
      rcv.siteBP = src.siteBP; rcv.sitePC = src.sitePC;
      rcv.siteInst = src.siteInst; rcv.targetBP = src.targetBP;
      rcv.targetSel = src.targetSel; rcv.cfe = src.cfe; rcv.cursor = src.cursor;
      rcv.code = src.code; 
      return rcv;
    }
    /** Effect the action of this (receiver) rewriter at the current cursor
     *  position within the effect of the calling (src) rewriter, allowing
     *  rewriters to be reusable.
     * <p>
     *  It may be appropriate for a calling rewriter to pass some additional
     *  information to a called one; in that case the rewriter to be called
     *  should overload this method with a version taking additional arguments.
     *  @param src reference to the caller's receiver (caller passes
     *  <code>this</code>), whose state will be copied.
     **/
    public void effect( Rewriter src) {
      Rewriter rcv = mirror( src);
      rcv.rewrite(); // discard return value: subcall, can't remove inst again!
    }
    /** Convenience method to add a FIAT to the declared return type. **/
    protected void addFiat() {
        cursor.addFiat( targetSel.getDescriptor().getType());
    }
    
    /** Add a quick branch-free sequence to convert an int on ToS to a proper
     *  boolean (0 or 1, 1 if the int was nonzero).
     **/
    protected void addIntNe0() {
	cursor.addSimpleInstruction(IFIAT);
        cursor.addSimpleInstruction( DUP);
        cursor.addSimpleInstruction( INEG);
        cursor.addSimpleInstruction( IOR); // v|-v has sign bit iff v != 0
        cursor.addLoadConstant( -1);
        cursor.addSimpleInstruction( IUSHR);
    }
    /** Add a quick branch-free sequence to convert an int on ToS to a proper
     *  boolean (0 or 1, 1 if the int was zero).
     **/
    protected void addIntEq0() {
        addIntNe0();
        cursor.addLoadConstant( 1);
        cursor.addSimpleInstruction( IXOR);
    }
    /** Add a sequence to treat an int on ToS as a proper boolean, for use when
     *  this is the last thing emitted by rewriting.  Will not bother to emit the
     *  sequence to convert to proper boolean if following instruction (after any
     *  number of NOP and !) is IFEQ or IFNE, but just jump to the right targets
     *  instead.
     **/
    protected void addTrailingIntNe0() {
        if ( ifEqOrNeFollows() ) {
            cursor.addIf( IFNE, nzTarget);
            cursor.addGoto( zTarget);
        }
        else
            addIntNe0();
    }
    /** Add a sequence to treat an int on ToS as a proper boolean, for use when
     *  this is the last thing emitted by rewriting.  Will not bother to emit the
     *  sequence to convert to proper boolean if following instruction (after any
     *  number of NOP and !) is IFEQ or IFNE, but just jump to the right targets
     *  instead.
     **/
    protected void addTrailingIntEq0() {
        if ( ifEqOrNeFollows() ) {
            cursor.addIf( IFEQ, nzTarget);
            cursor.addGoto( zTarget);
        }
        else
            addIntEq0();
    }
    /**
     * If the immediately (disregarding NOPs) following Instruction is a
     * checkcast, return it.  If not, return <code>null</code>.
     * On return, <code>code.getPC()</code> will be the PC of the returned
     * checkcast, or of the first non-NOP non-checkcast, respectively.
     * <code>code.position()</code> is not affected.
     */
    protected Instruction.CHECKCAST followingCheckCast() {
        int pc = code.position(); // instr following invoke
        Instruction inst = code.get( pc); // does not advance...
                        
        while (inst instanceof Instruction.NOP) {
            pc++;
            inst = code.get( pc);
        }
        if ( inst instanceof Instruction.CHECKCAST )
            return (Instruction.CHECKCAST)inst;
        return null;
    }
    
    /** Consult the target method's descriptor and emit enough POP2s and POPs
     *  to clear off all of its arguments, avoiding to pop halves of wide
     *  arguments.
     **/
    protected void addPopAll() {
        Descriptor.Method md = targetSel.getDescriptor();
        // when does a receiver (this-pointer) have to be included in the
        // pops? when the method is NOT on a sharedstate. otherwise, the
        // shst reference pushed earlier has already been popped by
        // IRewriter. clear as mud? the whole early static conversion
        // mess must die.
        boolean receiver = ! targetBP.isSharedState();
        for ( int k = md.getArgumentCount(); k --> 0; ) {
            byte opcode = POP;
            if ( md.isArgumentWidePrimitive( k) )
                opcode = POP2;
            else if ( 0 < k  &&  ! md.isArgumentWidePrimitive( k-1) ) {
                opcode = POP2;
                -- k;
            }
            else if ( 0 == k  &&  receiver ) {
                opcode = POP2;
                receiver = false;
            }
            cursor.addSimpleInstruction( opcode);
        }
        if ( receiver )
            cursor.addSimpleInstruction( POP);
    }
    
    private Marker zTarget;
    private Marker nzTarget;
    /** Determine if the instruction being rewritten is followed by an IFEQ or
     *  IFNE, possibly preceded by any sequence of NOP and ! (i.e. 1 IXOR).
     *  If so, set zTarget and nzTarget to the targets that would be reached with
     *  zero or nonzero, respectively, on top of stack, accounting for the number
     *  of intervening ! and whether the branch is IFEQ or IFNE.
     * @return true if the instruction being rewritten is followed by such a
     * sequence.
     */
    private boolean ifEqOrNeFollows() {
        boolean invert = false;
        Instruction inst;
        int pc;
        for ( pc = code.position() ;; pc += inst.size( code) ) {
            inst = code.get( pc); // does not bump position
            if ( inst instanceof Instruction.NOP )
                continue;
            if ( inst instanceof Instruction.ICONST_1 ) {
                pc += inst.size( code);
                inst = code.get( pc);
                if ( inst instanceof Instruction.IXOR ) {
                    invert = !invert;
                    continue;
                }
                return false;
            }
            if ( inst instanceof Instruction.IFEQ )
                break;
            if ( inst instanceof Instruction.IFNE ) {
                invert = !invert;
                break;
            }
            return false;
        }
        Instruction.ConditionalJump icj = (Instruction.ConditionalJump)inst;
        int branch = icj.getBranchTarget( code) + pc;
        int fall   = pc + icj.size( code);
        if ( invert ) {
            int t = fall; fall = branch; branch = t;
        }
        zTarget = cfe.getMarkerAtPC( branch);
        nzTarget = cfe.getMarkerAtPC( fall);
        return true;
    }
    /** Exception a rewriter can throw in order to punt. **/
    public static class PuntException extends OVMRuntimeException {
	public PuntException(String s, Throwable cause) { super(s, cause); }
	public PuntException(Throwable cause) { super(cause); }
	public PuntException(String s) { super(s); }
	public PuntException() {}
    }
  }

  public static class DeadRewriter extends Rewriter {
      //public static final DeadRewriter SINGLETON = new DeadRewriter();

    private static final TypeName.Compound className =
	(RepositoryUtils.makeTypeName("Ls3/util/PragmaTransformCallsiteIR;")
	 .asCompound());
    private static final Selector.Method method =
      RepositoryUtils.methodSelectorFor(
      	"Gs3/util/PragmaTransformCallsiteIR;",
	"failDeadCallsite:(Ljava/lang/String;)V");

      static HashMap rewriters = new HashMap();
      String reason;

      public static DeadRewriter make(String reason) {
	  Object ret = rewriters.get(reason);
	  if (ret == null) {
	      ret = new DeadRewriter(reason);
	      rewriters.put(reason, ret);
	  }
	  return (DeadRewriter) ret;
      }
      private DeadRewriter(String reason) { this.reason = reason; }

      protected boolean rewrite() {
      int pops = siteInst.getArgumentLengthInWords( code, cp);

      // pop dead method's arguments
      while ( pops-- > 0 )
      	  cursor.addSimpleInstruction( POP);

      cursor.addLoadConstant(new RepositoryString(reason +  targetSel));
      cursor.addINVOKESTATIC( method);

      // push a dummy value on the stack so that analysis of the
      // calling method is possible
      TypeName ret = targetSel.getDescriptor().getType();
      switch (ret.getTypeTag()) {
      case TypeCodes.VOID:
	  break;
      case TypeCodes.ARRAY:
      case TypeCodes.OBJECT:
      case TypeCodes.GEMEINSAM:
	  cursor.addSimpleInstruction(ACONST_NULL);
	  break;
      case TypeCodes.DOUBLE:
	  cursor.addLoadConstant(0.0);
	  break;
      case TypeCodes.FLOAT:
	  cursor.addLoadConstant(0.0f);
	  break;
      case TypeCodes.LONG:
	  cursor.addLoadConstant(0L);
	  break;
      default:
	  cursor.addLoadConstant(0);
	  break;
      }
      return true;
    }
  }
}
