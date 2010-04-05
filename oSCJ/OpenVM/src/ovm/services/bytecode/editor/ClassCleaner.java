package ovm.services.bytecode.editor;

import java.util.HashMap;
import java.util.Vector;

import ovm.core.repository.Mode;
import ovm.core.repository.Bytecode;
import ovm.core.repository.CloneClassVisitor;
import ovm.core.repository.ConstantPool;
import ovm.core.repository.ConstantPoolBuilder;
import ovm.core.repository.Constants;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionVisitor;
import ovm.services.bytecode.JVMConstants;
import ovm.util.ByteBuffer;
import ovm.util.OVMError;

/**
 * Code to clean-up a RepositoryClass, either to save memory
 * or to write it to the driver (used by S3Dumper).
 * 
 * This incarnation ensures that all methods share a constant
 * pool and that the shared CP works nicely for everybody.
 * 
 * -- there used to be an inner class here. I inlined it for the 
 * sake of get rid of one extra abstraction. Now the ClassCleaner
 * does the work itself. hope that's ok with you all. --jv
 * 
 * @author Christian Grothoff
 **/
public class ClassCleaner 
    extends CloneClassVisitor
    implements JVMConstants {

    public RepositoryClass clean(RepositoryClass original) {
        methods = new Vector();
        me2code = new HashMap();
        me2builder = new HashMap();
        newCP = null;
        currentMethod = null;
        visitClass(original);
        return commit();
    }

    private Vector methods;
    private HashMap me2code;
    private HashMap me2builder;
    private ConstantPool newCP;
    private RepositoryMember.Method currentMethod;

    /**
     * The visit method that should be called FIRST; begins visitation
     * of the entire class to be cloned. When this exits, the class
     * builder to be used to clone this class should be built and ready
     * to clone classes.
     * @param x the RepositoryClass of the class to be cloned
     **/
    public void visitClass(RepositoryClass x) {
        this.typeName = x.getName();
        rClassBuilder = repositoryFactory.getClassBuilder();
        rConstantPoolBuilder = new ConstantPoolBuilder();
        // THIS LINE IS DIFFERENT compared to super.visitClass()!
        x.visitAttributes(this);
        x.visitHeader(this);
        x.visitMembers(this);
        for (int i = 0; i < methods.size(); i++) {
            RepositoryMember.Method me =
                (RepositoryMember.Method) methods.elementAt(i);
            me2code.put(me, rewrite(me));
        }
        newCP = rConstantPoolBuilder.build();
        for (int i = 0; i < methods.size(); i++) {
            this.currentMethod = (RepositoryMember.Method) methods.elementAt(i);
            super.visitMethod(currentMethod);
        }
        rClassBuilder.declareConstantPool(newCP);
    }

    /**
     * Visits a method in the class to be cloned and declare this method
     * in the class builder.
     * @param x the cloned method to be declared in the class builder
     **/
    public void visitMethod(RepositoryMember.Method x) {
        methods.add(x);
    }

    /**
     * Visits a bytecode fragment from the current method being visited and
     * adds a cloned copy of this fragment to the method builder that will 
     * be used to declare this method in the class builder.
     * @param x the bytecode fragment to be added
     **/
    public void visitByteCodeFragment(Bytecode x) {
        if (me2builder.get(currentMethod) != null) {
            rByteCodeFragment =
                (Bytecode.Builder) me2builder.get(
                    currentMethod);
            rByteCodeFragment.setConstantPool(newCP);
            x.visitAttributes(this);
            rByteCodeFragment.declareSelector(
                rClassBuilder.getName(),
                rMethodInfoBuilder.getName(),
                rMethodInfoBuilder.getDescriptor());
            lastByteCodeFragment = rByteCodeFragment.build();
            rMethodInfoBuilder.addFragment(lastByteCodeFragment);
        } else {
            ByteBuffer newCode = (ByteBuffer) me2code.get(currentMethod);
            rByteCodeFragment = repositoryFactory.getByteCodeFragmentBuilder();
            rByteCodeFragment.setCode(newCode);
            rByteCodeFragment.setConstantPool(newCP);
            rByteCodeFragment.declareTemporaries(
                x.getMaxStack(),
                x.getMaxLocals());
            rByteCodeFragment.declareSelector(
                rClassBuilder.getName(),
                rMethodInfoBuilder.getName(),
                rMethodInfoBuilder.getDescriptor());
            x.visitComponents(this);
            lastByteCodeFragment = rByteCodeFragment.build();
            rMethodInfoBuilder.addFragment(lastByteCodeFragment);
        }
    }

    /**
     * Rewrite the CP accesses of the orig method to CP accesses of the
     * new constant pool that is being build by rConstantPoolBuilder
     **/
    private ByteBuffer rewrite(RepositoryMember.Method orig) {
        Mode.Method mo = orig.getMode();
        if (mo.isAbstract() || mo.isNative())
            return null; // nothing to do
        Bytecode cf =
            orig.getCodeFragment();
        InstructionBuffer code = InstructionBuffer.allocate(cf);
        try {
            new CleanCodeVisitor(code).run();
            return code.getCode();
        } catch (SwitchSlowPathException sspe) {
            // d("slow path method cleaning invoked for method " + orig);

            CodeFragmentEditor ed =
                new CodeFragmentEditor(cf, rConstantPoolBuilder);
            code = InstructionBuffer.allocate(cf);
            code.rewind();
            CloneInstructionVisitor civ = new CloneInstructionVisitor(code, ed);
            code.rewind();
            while (code.hasRemaining()) {
                Instruction i = code.get();
                if (i instanceof Instruction.ConstantPoolRead) {
                    int PC = code.getPC();
                    civ.setCursor(ed.getCursorAfterMarker(PC));
                    ed.removeInstruction(PC);
                    civ.visitAppropriate(i);
                    // will add instruction & update CP
                }
            }
            Bytecode.Builder builder = new Bytecode.Builder();
	    ed.commit(builder,
		      cf.getMaxStack(), 
		      cf.getMaxLocals());
            me2builder.put(orig, builder);
            return null;
        }
    }

    /**
     * Pass over a codefragment, cleaning up references to the constant
     * pool.
     * @author Christian Grothoff
     **/
    public class CleanCodeVisitor extends InstructionVisitor {

        /**
         * Create a new InstructionVisitor.
         * @param code the bytecode of the method to analyze
         **/
        public CleanCodeVisitor(InstructionBuffer code) {
            super(code);
        }

        public void run() {
            buf.rewind();
            while (buf.hasRemaining()) {
                Instruction i = buf.get();
                visitAppropriate(i);
            }
        }

        public void visit(Instruction.ConstantPoolRead cpr) {
            int cpIndex = cpr.getCPIndex(buf);
            try {
                char newIndex = copyEntry(getConstantPool(), (char) cpIndex);
                this.getCode().putChar(getPC() + 1, newIndex);
            } catch (ConstantPool.AccessException ae) {
                throw new OVMError(ae);
            }
        }

        public void visit(Instruction.LDC ldc) {
            int cpIndex = ldc.getCPIndex(buf);
            try {
                char newIndex = copyEntry(getConstantPool(), (char) cpIndex);
                if (newIndex > 255)
                    throw new SwitchSlowPathException("LDC cleaning created index > 255, not supported!");
                this.getCode().put(getPC() + 1, (byte) newIndex);
            } catch (ConstantPool.AccessException ae) {
                throw new OVMError(ae);
            }
        }

    } // end of ClassCleaner.CleanVisitor.CleanCodeVisitor

    class SwitchSlowPathException extends RuntimeException {
        SwitchSlowPathException(String s) {
            super(s);
        }
    }

    private char copyEntry(Constants cp, char index)
        throws ConstantPool.AccessException {
        switch (cp.getTagAt(index)) {
            case 0 :
                if (index == 0)
                    return 0;
                // 0 always maps to 0, and tag 0 for 0 is perfectly ok
                throw new Error("Bad CP tag (0) at index " + (int) index);
            case CONSTANT_Utf8 :
                return (char) rConstantPoolBuilder.addUtf8
                    (((ConstantPool)cp).getUtf8IndexAt(index));
            case CONSTANT_Float :
                return (char) rConstantPoolBuilder.addConstantFloat(
                    ((Float) cp.getConstantAt(index)).floatValue());
            case CONSTANT_Integer :
                return (char) rConstantPoolBuilder.addConstantInt(
                    ((Integer) cp.getConstantAt(index)).intValue());
            case CONSTANT_Long :
                return (char) rConstantPoolBuilder.addConstantLong(
                    ((Long) cp.getConstantAt(index)).longValue());
            case CONSTANT_Double :
                return (char) rConstantPoolBuilder.addConstantDouble(
                    ((Double) cp.getConstantAt(index)).doubleValue());
            case CONSTANT_Class :
                return (char) rConstantPoolBuilder.addClass(
                    cp.getClassAt(index));
            case CONSTANT_Fieldref :
                return (char) rConstantPoolBuilder.addFieldref(
                    cp.getFieldrefAt(index));
            case CONSTANT_String :
                return (char) rConstantPoolBuilder.addUnresolvedString
                    (((ConstantPool)cp).getUnresolvedStringAt(index));
            case CONSTANT_Methodref :
                return (char) rConstantPoolBuilder.addMethodref(
                    cp.getMethodrefAt(index));
            case CONSTANT_InterfaceMethodref :
                return (char) rConstantPoolBuilder.addInterfaceMethodref(
                    cp.getMethodrefAt(index));
            case CONSTANT_NameAndType :
                return (char) rConstantPoolBuilder.addUnboundSelector
                    (((ConstantPool)cp).getUnboundSelectorAt(index));
            case CONSTANT_SharedState :
                return (char) rConstantPoolBuilder.addUnresolvedSharedState
		    (((ConstantPool)cp).getUnresolvedSharedStateAt(index));
            case CONSTANT_Binder :
                return (char) rConstantPoolBuilder.addUnresolvedBinder
		    (((ConstantPool)cp).getUnresolvedBinderAt(index));
            default :
                throw new Error(
                    "unsupported CP tag "
                        + ((int) cp.getTagAt(index))
                        + " at index "
                        + (int) index);
        }
    }
} // end of ClassCleaner
