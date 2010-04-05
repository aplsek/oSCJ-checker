package s3.services.simplejit.bytecode;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedMethodref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.Domain;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.ConstantFieldref;
import ovm.core.repository.ConstantClass;
import ovm.core.repository.Constants;
import ovm.core.repository.ConstantsEditor;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Area;
import ovm.services.bytecode.ByteCodeGen2;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.ByteCodeGen;
import ovm.services.bytecode.CodeExceptionGen;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionHandle;
import ovm.services.bytecode.InstructionList;
import ovm.services.bytecode.InstructionTargeter;
import ovm.services.bytecode.TargetLostException;
import ovm.services.bytecode.Instruction.LocalWrite;
import ovm.services.bytecode.InstructionBuffer;
import ovm.util.ArrayList;
import ovm.util.HTint2Object;
import ovm.util.HashSet;
import ovm.util.HashMap;
import ovm.util.Iterator;
import ovm.util.NumberRanges;
import ovm.util.Vector;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Constants;
import s3.services.simplejit.SimpleJIT;
import s3.services.simplejit.bytecode.Inliner2.Visitor;
import s3.services.simplejit.SimpleJITAnalysis;

/**
 * A simple bytecode (ovm IR) inliner
 * @author Hiroshi Yamauchi
 */
public class Inliner {
    
    protected static final boolean DEBUG = false;
    protected static PrintWriter inliningLog = null;
    protected static final String inliningLogFile = "inlining.log";
    static {
	try {
	    if (DEBUG) {
		inliningLog = new PrintWriter(new BufferedOutputStream(new FileOutputStream(inliningLogFile)));
	    }
	} catch (IOException e) { throw new RuntimeException(e); }
    }

    public static final int CALLEE_MAX_SIZE = 26;
    public static final int MAX_DEPTH = 5;
    public static final int CALLER_MAX_SIZE = 2048;

    protected Method method;
    protected final int depth;
    protected SimpleJITAnalysis anal;
    protected HTint2Object work = new HTint2Object();
    protected boolean found = false;
    protected VM_Area compileArea;
    
    public Inliner(final Method method, SimpleJITAnalysis anal, VM_Area compileArea) {
        this.method = method;
	this.depth = 0;
		this.anal = anal;
		this.compileArea = compileArea;
    }

    protected Inliner(final Method method, int depth, SimpleJITAnalysis anal, VM_Area compileArea) {
        this.method = method;
	this.depth = depth;
		this.anal = anal;
		this.compileArea = compileArea;
    }

	protected S3ByteCode beforeInlining(Method method) {
	    S3ByteCode o = (S3ByteCode) SimpleJIT.beforeInlining.get(method);
	    if (o != null)
		return o;
	    S3ByteCode bc = method.getByteCode();
	    if (bc == null)
		return null;
	    SimpleJIT.beforeInlining.put(method, bc);
	    return bc;
	}

    public S3ByteCode inline() {
	if (depth >= MAX_DEPTH)
	    return null;
	if ((method.getByteCode()).getBytes().length > CALLER_MAX_SIZE) {
	    return null;
	}
	beforeInlining(method);
	S3ByteCode inlined = null;
	find();
	if (found) {
	    inlined = perform();
	}
	return inlined;
    }

    
    protected void find() {
		final Domain domain = method.getDeclaringType().getDomain();
		final Visitor visitor = new Visitor() {
			public void visit(Instruction.Invocation i) {
				ConstantMethodref mr = i.getConstantMethodref(buf, cp);
				if (mr instanceof ConstantResolvedMethodref) {
					Method sm = ((ConstantResolvedMethodref) mr).getMethod();

					Type st = sm.getDeclaringType();
					if (domain != st.getDomain()) {
						return;
					}
					Blueprint rcv = anal.blueprintFor(st);
					Method dm = (rcv != null 
							&& (i instanceof Instruction.INVOKEVIRTUAL
									|| i instanceof Instruction.INVOKEINTERFACE))
								? anal.getTarget(rcv, sm)
								: null;

					if (dm != null) { // devirtualized by the analysis
						if (method == dm) return;
						if (method.toString().indexOf("testArrayWrite") > 0) {
							return; // bug on test.TestFieldAccess.testArrayWrite
						}
						S3ByteCode calleeBC = dm.getByteCode();
						if (calleeBC == null || calleeBC.getBytes() == null
								|| !shouldInline(dm)) {
							return;
						}
						if (DEBUG) {
							inliningLog.println("Inliner.find @PC" + getPC()
									+ " " + dm);
						}
						found = true;
						//BasicIO.out.println(method + " statically calling " + sm);
						//BasicIO.out.println(method + " (devirtualized) inlining " + dm);
						work.put(getPC(), dm);
					} else { // not devirtualized by the analysis
						if (method == sm) return;
						S3ByteCode calleeBC = sm.getByteCode();
						if (calleeBC == null || calleeBC.getBytes() == null
								|| !shouldInline(sm)) {
							return;
						}
						if (sm.isNonVirtual() || sm.getMode().isFinal()
								|| sm.getDeclaringType().getMode().isFinal()
								|| (i instanceof Instruction.INVOKESPECIAL)) {
							if (DEBUG) {
								inliningLog.println("Inliner.find @PC"
										+ getPC() + " " + sm);
							}
							found = true;
							//BasicIO.out.println(method + " statically calling " + sm);
							//BasicIO.out.println(method + " inlining " + sm);
							work.put(getPC(), sm);
						}
					}
				} else {
					warnMissing(i.getSelector(buf, cp).toString());
				}
			}
		};
		visitor.run(method);
	}
    
    protected S3ByteCode perform() {
        S3ByteCode bc = (S3ByteCode) beforeInlining(method);

        if (DEBUG) {
            inliningLog.println("Inlining " + method.toString());
            bc.dumpAscii("", inliningLog);
            inliningLog.flush();
        }
        
        ByteCodeGen bcgen = new ByteCodeGen2(bc, compileArea);
    		bcgen.removeLineNumbers();
    		bcgen.removeLocalVariables();
	runVisitor(bcgen);
		bc = bcgen.getByteCode();

        if (DEBUG) {
            inliningLog.println("Inlined " + method.toString());
            bc.dumpAscii("", inliningLog);
            inliningLog.flush();
        }

	return bc;
    }

    protected void runVisitor(ByteCodeGen bcgen) {
        new InlineVisitor(bcgen).run();
    }
    
    protected boolean shouldInline(Method callee) {
	S3ByteCode bc = callee.getByteCode();
        if (bc.getBytes().length < CALLEE_MAX_SIZE && bc.getExceptionHandlers().length == 0
                && ! callee.getMode().isSynchronized()) {
            return true;
        }
        return false;
    }

    protected S3ByteCode inlineRecursively(Method method) {
	beforeInlining(method);
        S3ByteCode inlined = new Inliner(method, depth + 1, anal, compileArea).inline();
	if (inlined != null) {
	    return inlined;
	} else {
	    return (S3ByteCode) beforeInlining(method);
	}
    }

    protected class InlineVisitor extends Visitor {
        ByteCodeGen bcgen;
        InstructionList il;
        InstructionHandle ih;
        HashMap p2nCache;
        final int maxLocals;
        final int maxStack;
	int newMaxLocals;
	int newMaxStack;
        
        public InlineVisitor(ByteCodeGen bcgen) {
            this.bcgen = bcgen;
            this.il = bcgen.getInstructionList();
            this.maxLocals = bcgen.getMaxLocals();
            this.maxStack = bcgen.getMaxStack();
	    newMaxLocals = maxLocals;
	    newMaxStack = maxStack;
        }

        public void run() {
            ih = il.getStart();
            HashMap ih2Callee = new HashMap();
            while (ih != null) {
                InstructionHandle next = ih.getNext();
                Object callee = work.get(ih.getPC());
                if (callee != null) {
		    if (DEBUG) {
			inliningLog.println("@PC" + ih.getPC() + " " + callee.toString());
			S3ByteCode _bc = (S3ByteCode) beforeInlining((Method)callee);
			_bc.dumpAscii("", inliningLog);
			inliningLog.flush();
		    }
                    ih2Callee.put(ih, callee);
                }
                ih = next;
            }
            for(Iterator it = ih2Callee.keySet().iterator();
                it.hasNext(); ) {
                ih = (InstructionHandle) it.next();
                ih.accept(this);
            }
            bcgen.setMaxLocals(newMaxLocals);
            bcgen.setMaxStack(newMaxStack);
        }

        public void visit(Instruction.Invocation i) {
	    boolean isInvokeStatic = i instanceof Instruction.INVOKESTATIC;
            Method _method = (Method) work.get(ih.getPC());
	    S3ByteCode inlinedCallee = inlineRecursively(_method);
            ByteCodeGen _bcgen = new ByteCodeGen2(inlinedCallee, compileArea);
	    ConstantsEditor callerCP_W = bcgen.getConstantPoolEditor();
	    Constants callerCP_R = bcgen.getConstantPool();
            transformCallee(_bcgen, ih.getNext(), callerCP_W, maxLocals);
            InstructionList _il = _bcgen.getInstructionList();
            int _maxLocals = _bcgen.getMaxLocals();
            int _maxStack = _bcgen.getMaxStack();
            ArrayList _argTypeList = new ArrayList();
            int _argSize;
            char[] _argTypes = _bcgen.getArgumentTypes();
            _argTypeList.add(new Character(TypeCodes.REFERENCE));
            for (int j = 0; j < _argTypes.length; j++) {
                char t = toBasicType(_argTypes[j]);
                if (typeCode2Size(t) == 1) {
                    _argTypeList.add(new Character(t));
                } else if (typeCode2Size(t) == 2) {
                    _argTypeList.add(new Character(t));
                    _argTypeList.add(new Character(TypeCodes.VOID));
                } else
                    throw new Error();
            }
            _argSize = _argTypeList.size();
            
            // Insert localWrites to pop the arguments
            int localIndex = maxLocals + _argSize - 1;
            InstructionHandle firstInserted = null;
            for (int j = _argTypes.length - 1; j >= 0; j--) {
                char t = toBasicType(_argTypes[j]);
                if (t == TypeCodes.VOID)
                    continue;
		if (typeCode2Size(t) == 2)
		    localIndex--;
                if (firstInserted == null)
                    firstInserted = il.insert(ih, LocalWrite.make(t, localIndex));
                else
                    il.insert(ih, LocalWrite.make(t, localIndex));
                localIndex--;
            }
	    if (isInvokeStatic) {
		Instruction.INVOKESTATIC invokeStatic = (Instruction.INVOKESTATIC) i;
		int invokeStaticCPIndex = invokeStatic.getCPIndex();		
		Object entry = callerCP_R.getConstantAt(invokeStaticCPIndex);
		byte tag = callerCP_R.getTagAt(invokeStaticCPIndex);
		int shstCPIndex = -1;
		switch(tag) {
		case JVMConstants.CONSTANT_ResolvedStaticMethod: {
		    ConstantResolvedStaticMethodref mr =
			(ConstantResolvedStaticMethodref) entry;
		    shstCPIndex = callerCP_W.addResolvedConstant(mr.getSharedState());
		    break;
		}
		case JVMConstants.CONSTANT_Methodref:
		default:
		    throw new Error();
		}
		// Since the shared state is only used in synch methods and 
		// synch methods are not inlined for now, LDC is not needed.
		if (firstInserted == null) {
		    //firstInserted = il.insert(ih, new Instruction.LDC_REF_QUICK(shstCPIndex));
		    firstInserted = il.insert(ih, new Instruction.ACONST_NULL());
		} else {
		    //il.insert(ih, new Instruction.LDC_REF_QUICK(shstCPIndex));
		    il.insert(ih, new Instruction.ACONST_NULL());
		}
		il.insert(ih, LocalWrite.make(TypeCodes.REFERENCE, localIndex));
	    } else {
		if (firstInserted == null)
		    firstInserted = il.insert(ih, LocalWrite.make(TypeCodes.REFERENCE, localIndex));
		else
		    il.insert(ih, LocalWrite.make(TypeCodes.REFERENCE, localIndex));
		// insert REF_GETFIELD_QUICK to enforce a null check
		/* this only matters in _200_check
		il.insert(ih, Instruction.LocalRead.make(TypeCodes.REFERENCE, localIndex));
		il.insert(ih, new Instruction.REF_GETFIELD_QUICK(0));
		il.insert(ih, Instruction.POP.make());
		*/
	    }
            
            // Insert the callee instructions into caller
	    if (DEBUG) {
		//inliningLog.println("Caller code : \n" + il.toString());
		//inliningLog.println("Callee code : \n" + _il.toString());
		//inliningLog.flush();
	    }

	    il.insert(ih, _il);

            InstructionHandle last = ih.getPrev();
            // Remove the INVOKE* instruction
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int k = 0; k < targets.length; k++) {
                    InstructionTargeter[] targeters = targets[k]
                            .getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
			if (targeters[j] instanceof CodeExceptionGen) {
			    CodeExceptionGen ceg = (CodeExceptionGen) targeters[j];
			    if (ceg.getStartPC() == ih
				&& ceg.getEndPC() == ih) {
				ceg.setStartPC(firstInserted);
				ceg.setEndPC(last);
			    } else if (ceg.getStartPC() == ih) {
				targeters[j].updateTarget(targets[k],
							  firstInserted);
			    } else if (ceg.getEndPC() == ih) {
				targeters[j].updateTarget(targets[k],
							  last);
			    } else if (ceg.getHandlerPC() == ih) {
				targeters[j].updateTarget(targets[k],
							  firstInserted);
			    }
			} else {
			    targeters[j].updateTarget(targets[k],
						      firstInserted);
			}
                    }
                }
            }
            
	    if (newMaxLocals < maxLocals + _argSize + _maxLocals) {
		newMaxLocals = maxLocals + _argSize + _maxLocals;
	    }
	    if (newMaxStack < maxStack + _maxStack) {
		newMaxStack = maxStack + _maxStack;
	    }
        }
    }

    private static void transformCallee(ByteCodeGen bcgen, 
            InstructionHandle exit, ConstantsEditor callerCP, int callerMaxLocals) {
        TransformCalleeVisitor v = new TransformCalleeVisitor(bcgen, exit, callerCP, callerMaxLocals);
        v.run();
    }
    
    /**
     * Shift locals, replace returns with gotos, copy the CP entries to the caller's
     */
    protected static class TransformCalleeVisitor extends Instruction.Visitor {
        ByteCodeGen bcgen;
        Constants cp;
        InstructionList il;
        InstructionHandle ih;
        final int callerMaxLocals;
        Vector argTypeList;
        int argSize;
        InstructionHandle exit;
        ConstantsEditor callerCP;

        public TransformCalleeVisitor(ByteCodeGen bcgen, 
                InstructionHandle exit,
                ConstantsEditor callerCP,
		int callerMaxLocals) {
            this.bcgen = bcgen;
            this.il = bcgen.getInstructionList();
            this.callerMaxLocals = callerMaxLocals;
            cp = bcgen.getConstantPool();
            argTypeList = new Vector();
            this.exit = exit;
            this.callerCP = callerCP;
        }

        public void run() {
	    bcgen.removeLineNumbers();
	    bcgen.removeLocalVariables();

            char[] argTypes = bcgen.getArgumentTypes();
            argTypeList.add(new Character(TypeCodes.REFERENCE));
            for (int i = 0; i < argTypes.length; i++) {
                char t = toBasicType(argTypes[i]);
                if (typeCode2Size(t) == 1) {
                    argTypeList.add(new Character(t));
                } else if (typeCode2Size(t) == 2) {
                    argTypeList.add(new Character(t));
                    argTypeList.add(new Character(TypeCodes.VOID));
                } else
                    throw new Error();
            }
            argSize = argTypeList.size();

            ih = il.getStart();
            while (ih != null) {
                InstructionHandle next = ih.getNext();
                ih.accept(this);
                ih = next;
            }

            int maxSize = 0;
            maxSize = 1;
            for (int i = 0; i < argTypes.length; i++) {
                char t = toBasicType(argTypes[i]);
                int size = typeCode2Size(t);
                if (maxSize < size)
                    maxSize = size;
            }
        }

        public void visit(Instruction.LocalWrite o) {
            if (argSize > 0) {
                ih.setInstruction(Instruction.LocalWrite.make(o.getTypeCode(), o
                        .getLocalVariableOffset()
                        + callerMaxLocals));
            }
        }

        public void visit(Instruction.LocalRead o) {
            if (argSize > 0) {
                ih.setInstruction(Instruction.LocalRead.make(o.getTypeCode(), o
                        .getLocalVariableOffset()
                        + callerMaxLocals));
            }
        }
        
        public void visit(Instruction.IINC o) {
            ih.setInstruction(Instruction.IINC.make(o.getLocalVariableOffset() + callerMaxLocals,
                    o.getValue()));
        }
        
        public void visit(Instruction.RET o) {
            ih.setInstruction(Instruction.RET.make(NumberRanges.checkUnsignedShort(o.getLocalVariableOffset() + callerMaxLocals)));
        }
        public void visit(Instruction.WIDE_RET o) {
            ih.setInstruction(Instruction.WIDE_RET.make(NumberRanges.checkUnsignedShort(o.getLocalVariableOffset() + callerMaxLocals)));
        }
        
        public void visit(Instruction.ReturnValue o) {
	    if (ih != il.getEnd()) {
		ih.setInstruction(Instruction.GOTO.make(exit));
	    } else {
		try {
		    il.delete(ih);
		} catch (TargetLostException e) {
		    InstructionHandle[] targets = e.getTargets();
		    for (int k = 0; k < targets.length; k++) {
			InstructionTargeter[] targeters = targets[k]
			    .getTargeters();
			for (int j = 0; j < targeters.length; j++) {
			    targeters[j].updateTarget(targets[k],
						      exit);
			}
		    }
		}
	    }
        }
        
        public void visit(Instruction.RETURN o) {
	    if (ih != il.getEnd()) {
		ih.setInstruction(Instruction.GOTO.make(exit));
	    } else {
		try {
		    il.delete(ih);
		} catch (TargetLostException e) {
		    InstructionHandle[] targets = e.getTargets();
		    for (int k = 0; k < targets.length; k++) {
			InstructionTargeter[] targeters = targets[k]
			    .getTargeters();
			for (int j = 0; j < targeters.length; j++) {
			    targeters[j].updateTarget(targets[k],
						      exit);
			}
		    }
		}
	    }
        }
        
        public void visit(Instruction.ConstantPoolRead o) {
            int oldIndex = o.getCPIndex();
            Object entry = cp.getConstantAt(oldIndex);
	    byte tag = cp.getTagAt(oldIndex);
	    int newIndex = -1;
	    switch(tag) {
	    case JVMConstants.CONSTANT_ResolvedStaticMethod:
	    case JVMConstants.CONSTANT_ResolvedInstanceMethod:
	    case JVMConstants.CONSTANT_Methodref:
		newIndex = callerCP.addMethodref((ConstantMethodref)entry);
		break;
	    case JVMConstants.CONSTANT_ResolvedInterfaceMethod:
	    case JVMConstants.CONSTANT_InterfaceMethodref:
		newIndex = callerCP.addInterfaceMethodref((ConstantMethodref)entry);
		break;
	    case JVMConstants.CONSTANT_ResolvedStaticField:
	    case JVMConstants.CONSTANT_ResolvedInstanceField:
	    case JVMConstants.CONSTANT_Fieldref:
		newIndex = callerCP.addFieldref((ConstantFieldref)entry);
		break;
	    case JVMConstants.CONSTANT_Class:
	    case JVMConstants.CONSTANT_ResolvedClass:
		newIndex = callerCP.addClass((ConstantClass)entry);
		break;
	    case JVMConstants.CONSTANT_SharedState:
		newIndex = callerCP.addUnresolvedSharedState((TypeName.Gemeinsam)entry);
		break;
	    case JVMConstants.CONSTANT_String:
		newIndex = callerCP.addUnresolvedString((RepositoryString)entry);
		break;
	    default:
		newIndex = callerCP.addResolvedConstant(entry);
		break;
	    }
	    if (newIndex < 0) {
		throw new Error(o.getClass().toString());
	    }
	    /*
	    byte newTag = ((Constants)callerCP).getTagAt(newIndex);
	    Object newEntry = ((Constants)callerCP).getConstantAt(newIndex);	    
	    if ( !(newTag == tag && entry == newEntry)
		 //		 || !(tag == JVMConstants.CONSTANT_String && entry.equals(newEntry))) {
		 && !(newTag == tag && entry.equals(newEntry))) {
		throw new Error("tag=" + tag + ", newtag=" + newTag +
				", entry=" + entry.getClass() + ":" + entry + 
				", newentry=" + newEntry.getClass() + ":" + newEntry);
	    }
	    */
            o.setCPIndex(newIndex);
        }
    }

    protected static char toBasicType(char type) {
        if (type == TypeCodes.REFERENCE || type == TypeCodes.OBJECT || type == TypeCodes.ARRAY)
            return TypeCodes.REFERENCE;
        else if (type == TypeCodes.BOOLEAN || type == TypeCodes.BYTE || type == TypeCodes.CHAR
                || type == TypeCodes.SHORT)
            return TypeCodes.INT;
        else {
            return type;
        }
    }
    protected static int typeCode2Size(char t) {
        if (t == TypeCodes.LONG || t == TypeCodes.DOUBLE)
            return 2;
        else if (t == TypeCodes.INT || t == TypeCodes.FLOAT || t == TypeCodes.REFERENCE
                || t == TypeCodes.SHORT || t == TypeCodes.CHAR || t == TypeCodes.BYTE 
                || t == TypeCodes.BOOLEAN 
                || t == TypeCodes.OBJECT 
                || t == TypeCodes.ARRAY) 
            return 1;
        else
            throw new Error();
    }

    public abstract class Visitor extends Instruction.Visitor {
	protected InstructionBuffer buf;
	protected S3Constants cp;
	protected S3ByteCode code;

	protected int getPC() { return buf.getPC(); }

	public void run(Method m) {
	    code = (S3ByteCode) beforeInlining(m);
	    cp = (S3Constants) code.getConstantPool();
	    buf = InstructionBuffer.wrap(code);

	    while (buf.hasRemaining()) {
		Instruction i = buf.get();
		//BasicIO.err.println(getPC() + i.getName());
		if (i == Instruction.WIDE.singleton)
		    ((Instruction.WIDE) i).specialize(buf).accept(this);
		else
		    i.accept(this);
	    }
	}

	public void message(String s) {
	    Type.Scalar dt = code.getMethod().getDeclaringType().asScalar();
	    int dir = dt.getName().getPackageNameIndex();
	    int file = dt.getSourceFileNameIndex();
	    int line = code.getLineNumber(getPC());

	    if (file == 0)
		file = dt.getName().getShortNameIndex();

	    BasicIO.err.println(UTF8Store._.getUtf8(dir)  + "/" +
			       UTF8Store._.getUtf8(file) + ":" +
			       line + ": " + s);
	}

	public void warnMissing(String s) {
	    message("warning: " + s + " not found");
	}
    }
}
