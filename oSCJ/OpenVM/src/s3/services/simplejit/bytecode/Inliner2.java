package s3.services.simplejit.bytecode;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.ConstantResolvedMethodref;
import ovm.core.domain.Method;
import ovm.core.domain.Domain;
import ovm.core.domain.Type;
import ovm.core.repository.Attribute;
import ovm.core.repository.ConstantClass;
import ovm.core.repository.ConstantFieldref;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.Constants;
import ovm.core.repository.ConstantsEditor;
import ovm.core.repository.Descriptor;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Area;
import ovm.services.bytecode.ByteCodeGen;
import ovm.services.bytecode.ByteCodeGen2;
import ovm.services.bytecode.CodeExceptionGen;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionHandle;
import ovm.services.bytecode.InstructionList;
import ovm.services.bytecode.InstructionTargeter;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.TargetLostException;
import ovm.util.ArrayList;
import ovm.util.HashMap;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.NumberRanges;
import ovm.util.Vector;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Constants;
import s3.services.simplejit.SimpleJITAnalysis;
import s3.services.simplejit.SimpleJIT;
import s3.services.simplejit.bytecode.Translator.Liveness;
import s3.services.simplejit.bytecode.Translator.RegisterTable;

/**
 * A bytecode (ovm IR) inliner that respects and combines
 * the register allocation results from Translator.
 * @author Hiroshi Yamauchi
 */
public class Inliner2 extends Inliner {

	public Inliner2(final Method method, SimpleJITAnalysis anal, VM_Area compileArea) {
		super(method, anal, compileArea);
	}

	private Inliner2(final Method method, int depth, SimpleJITAnalysis anal, VM_Area compileArea) {
		super(method, depth, anal, compileArea);
	}

	protected S3ByteCode beforeInlining(Method method) {
	    S3ByteCode o = (S3ByteCode) SimpleJIT.beforeInlining.get(method);
	    if (o != null)
		return o;
	    S3ByteCode bc = method.getByteCode();	    
	    if (bc == null || bc.getBytes() == null)
		return null;
	    if (getRegisterTableAttribute(bc) == null) {
		SimpleJIT.pauseCompileTimer();

		S3ByteCode translatedCode = Translator.translate(bc, method, compileArea);

		SimpleJIT.restartCompileTimer();
		SimpleJIT.beforeInlining.put(method, translatedCode);
		method.addCode(translatedCode);

		return translatedCode;
	    } else {
		//throw new Error();
	    	return bc;
	    }
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




    protected S3ByteCode perform() {
	S3ByteCode bc = (S3ByteCode) beforeInlining(method);

        if (DEBUG) {
            inliningLog.println("Inlining " + method.toString());
            bc.dumpAscii("", inliningLog);
            inliningLog.flush();
        }

	S3ByteCode inlinedBC = null;
	inlinedBC = new InlineVisitor2(bc).run();

	if (DEBUG) {
            inliningLog.println("Inlined " + method.toString());
            inlinedBC.dumpAscii("", inliningLog);
            inliningLog.flush();
	}

	return inlinedBC;
    }

	protected S3ByteCode inlineRecursively(Method method) {
	    beforeInlining(method);
	    S3ByteCode inlined = new Inliner2(method, depth + 1, anal, compileArea).inline();
	    if (inlined != null) {
		return inlined;
	    } else {
		return (S3ByteCode) beforeInlining(method);
	    }
	}

	protected class InlineVisitor2 extends Visitor {
		S3ByteCode bc;
		ByteCodeGen bcgen;
		InstructionList il;
		InstructionHandle ih;
		HashMap p2nCache;
		final int maxLocals;
		final int maxStack;
		int newMaxLocals;
		int newMaxStack;
		HashMap callerIH2Liveness;
	    HashMap callerIH2LoopDepth;
		HashMap ih2Callee;
	    ArrayList loopStart;
	    ArrayList loopEnd;

		public InlineVisitor2(S3ByteCode bc) {
			this.bc = bc;
			this.bcgen = new ByteCodeGen2(bc, compileArea);
			this.il = bcgen.getInstructionList();
			this.maxLocals = bcgen.getMaxLocals();
			this.maxStack = bcgen.getMaxStack();
			newMaxLocals = maxLocals;
			newMaxStack = maxStack;
			callerIH2Liveness = new HashMap();
			callerIH2LoopDepth = new HashMap();
			ih2Callee = new HashMap();
			loopStart = new ArrayList();
			loopEnd = new ArrayList();
		}

		private void build_callerIH2Liveness(ByteCodeGen bcgen) {
			Liveness.Entry[] livenessEntries = getLivenessAttribute(bcgen)
					.getEntries();
			int k = 0;
			for (InstructionHandle ih = il.getStart(); ih != null; ih = ih
					.getNext()) {
				if (ih.getPC() != livenessEntries[k].position()) {
					throw new Error();
				}
				callerIH2Liveness.put(ih, livenessEntries[k]);
				k++;
			}
		}

	    // fast approximation of loop detection
		private void build_callerLoopDepthMap() {
		    ArrayList loopStart = new ArrayList();
		    ArrayList loopEnd = new ArrayList();
			InstructionHandle _ih = il.getStart();
			while (_ih != null) {
				InstructionHandle next = _ih.getNext();
				Instruction inst = _ih.getInstruction();
				if (inst instanceof Instruction.BranchInstruction) {
				    Instruction.BranchInstruction binst = (Instruction.BranchInstruction) inst;
				    InstructionHandle target = binst.getTargetHandle();
				    if (target.getPC() < _ih.getPC()) { // backward
					loopStart.add(new Integer(target.getPC()));
					loopEnd.add(new Integer(_ih.getPC()));
				    }
				}
				_ih = next;
			}
			_ih = il.getStart();
			while (_ih != null) {
				InstructionHandle next = _ih.getNext();
				int loopCount = loopStart.size();
				int pc = _ih.getPC();
				int depth = 0;
				for(int i = 0; i < loopCount; i++) {
				    int startPC = ((Integer) loopStart.get(i)).intValue();
				    int endPC = ((Integer) loopEnd.get(i)).intValue();
				    if (startPC <= pc && pc <= endPC) {
					depth++;
				    }
				}
				callerIH2LoopDepth.put(_ih, new Integer(depth));
				_ih = next;
			}
		}
		
		private void build_ih2Callee() {
			InstructionHandle _ih = il.getStart();
			while (_ih != null) {
				InstructionHandle next = _ih.getNext();
				Object callee = work.get(_ih.getPC());
				if (callee != null) {
					ih2Callee.put(_ih, callee);
				}
				_ih = next;
			}
		}
		
		public S3ByteCode run() {
			bcgen.removeLineNumbers();
			bcgen.removeLocalVariables();

			build_ih2Callee();
			build_callerIH2Liveness(bcgen);
			build_callerLoopDepthMap();
			
			for(Iterator it = ih2Callee.keySet().iterator();
				it.hasNext(); ) {
				ih = (InstructionHandle) it.next();
				Method callee = (Method) ih2Callee.get(ih);
				if (DEBUG) {
					inliningLog.println("@PC" + ih.getPC() + " "
							+ callee.toString());	
					S3ByteCode _bc = (S3ByteCode) beforeInlining(callee);
					_bc.dumpAscii("", inliningLog);
					inliningLog.flush();
				}
				ih.accept(this);
			}
			
			// Fix up Liveness
			bcgen.removeAttribute(getLivenessAttribute(bcgen));
			return bcgen.getByteCode();
		}

	    private void updateCallerLiveness(int[] localMap, int newMaxLocals) {
		for(Iterator it = callerIH2Liveness.keySet().iterator();
		    it.hasNext(); ) {
		    InstructionHandle ih = (InstructionHandle) it.next();
		    Liveness.Entry le = (Liveness.Entry) callerIH2Liveness.get(ih);
		    boolean[] liveness = le.liveness();
		    boolean[] newLiveness = new boolean[newMaxLocals];
		    for(int i = 0; i < liveness.length; i++) {
			if (liveness[i]) {
			    newLiveness[localMap[i]] = true;
			}
		    }
		    Liveness.Entry newLE = new Liveness.Entry(le.position(),
							      newLiveness,
							      newMaxLocals,
							      le.isBlockStart());
		    callerIH2Liveness.put(ih, newLE);
		}
	    }

		public void visit(Instruction.Invocation i) {
			ConstantsEditor callerCP_W = bcgen.getConstantPoolEditor();
			Constants callerCP_R = bcgen.getConstantPool();
			boolean isInvokeStatic = i instanceof Instruction.INVOKESTATIC;
			Method _method = (Method) ih2Callee.get(ih);
			S3ByteCode inlinedCallee = inlineRecursively(_method);
			ByteCodeGen _bcgen = new ByteCodeGen2(inlinedCallee, compileArea);
			int[] callerLocalMap = new int[bcgen.getMaxLocals()];
			int[] calleeLocalMap = new int[_bcgen.getMaxLocals()];

			// Merge RegisterTable
			InstructionHandle precallSite = ih;
			final int calleeArgSize = _bcgen.getSelector().getDescriptor().getArgumentCount() + 1;
			for (int j = 0; j < calleeArgSize; j++) {
				precallSite = precallSite.getPrev();
			}
			if (isInvokeStatic) {
				precallSite = precallSite.getNext();
			}
			RegisterTable mergedRT = mergeRegisterTables(ih,
					precallSite, bcgen, _bcgen, callerLocalMap,
					calleeLocalMap);
			bcgen.removeAttribute(getRegisterTableAttribute(bcgen));
			bcgen.addAttribute(mergedRT);

			updateCallerLiveness(callerLocalMap, mergedRT.getEntries().length);

			transformCaller(bcgen, callerLocalMap);
			transformCallee(_bcgen, ih.getNext(), callerCP_W, calleeLocalMap);
			InstructionList _il = _bcgen.getInstructionList();
			int _maxLocals = _bcgen.getMaxLocals();
			int _maxStack = _bcgen.getMaxStack();

			// Interleave the argument loads with the callee's prologue
			InstructionHandle prologue = _il.getStart();
			InstructionHandle actual = precallSite;
			InstructionHandle formal = _il.getStart();
			if (isInvokeStatic) {
			    // Since the shared state is only used in synch methods and 
			    // synch methods are not inlined for now, LDC is not needed.
			    /*
				Instruction.INVOKESTATIC invokeStatic = (Instruction.INVOKESTATIC) i;
				int invokeStaticCPIndex = invokeStatic.getCPIndex();
				Object entry = callerCP_R.getConstantAt(invokeStaticCPIndex);
				byte tag = callerCP_R.getTagAt(invokeStaticCPIndex);
				int shstCPIndex = -1;
				switch (tag) {
				case JVMConstants.CONSTANT_ResolvedStaticMethod: {
					ConstantResolvedStaticMethodref mr = (ConstantResolvedStaticMethodref) entry;
					shstCPIndex = callerCP_W.addResolvedConstant(mr
							.getSharedState());
					break;
				}
				case JVMConstants.CONSTANT_Methodref:
				default:
					throw new Error();
				}
				formal.setInstruction(new Instruction.LDC_REF_QUICK(shstCPIndex));
			    */
			    formal.setInstruction(new Instruction.ACONST_NULL());
			} else {
				formal.setInstruction(actual.getInstruction());
				actual = actual.getNext();
			}
			formal = formal.getNext().getNext();
			int _argSize = _bcgen.getSelector().getDescriptor().getArgumentCount();
			for(int j = 0; j < _argSize; j++) {
				formal.setInstruction(actual.getInstruction());
				formal = formal.getNext().getNext();
				actual = actual.getNext();
			}

			InstructionHandle pollcheck = _il.getStart();
			pollcheck = pollcheck.getNext().getNext();
			for(int j = 0; j < _argSize; j++) { 
			    pollcheck = pollcheck.getNext().getNext();
			}
			if (pollcheck != null 
			    && pollcheck.getInstruction() instanceof Instruction.POLLCHECK) {
			    pollcheck.setInstruction(Instruction.NOP.make());
			}

			InstructionHandle callRangeBegin = null;
			if (!isInvokeStatic) {
				// insert REF_GETFIELD_QUICK to enforce a null check
			    // This only matters in _200_check
			    /*
			    callRangeBegin = il.insert(ih, precallSite.getInstruction());
				il.insert(ih, new Instruction.REF_GETFIELD_QUICK(0));
				il.insert(ih, Instruction.POP.make());
			    */
			    callRangeBegin = prologue;
			} else {
			    callRangeBegin = prologue;
			}
			// Insert the callee instructions into caller
			if (DEBUG) {
				//		inliningLog.println("Caller code : \n" + il.toString());
				//		inliningLog.println("Callee code : \n" + _il.toString());
				//		inliningLog.flush();
			}

			il.insert(ih, _il);

			// Remove the argment loads
			InstructionHandle loads = precallSite;
			int ndelete = isInvokeStatic ? calleeArgSize - 1 : calleeArgSize;
			for(int l = 0; l < ndelete; l++) {
				InstructionHandle next = loads.getNext();
				InstructionHandle prev = loads.getPrev();
				try {
				    if (loads.getInstruction() instanceof Instruction.Invocation)
					throw new Error();
				    il.delete(loads);
				} catch (TargetLostException e) {
					InstructionHandle[] targets = e.getTargets();
					for (int k = 0; k < targets.length; k++) {
						InstructionTargeter[] targeters = targets[k].getTargeters();
						for (int j = 0; j < targeters.length; j++) {
							if (targeters[j] instanceof CodeExceptionGen) {
								CodeExceptionGen ceg = (CodeExceptionGen) targeters[j];
								if (ceg.getStartPC() == loads) {
									targeters[j].updateTarget(targets[k],
											next);
								} else if (ceg.getEndPC() == loads) {
									targeters[j].updateTarget(targets[k], prev);
								} else if (ceg.getHandlerPC() == loads) {
									targeters[j].updateTarget(targets[k],
											next);
								//} else {
								//	throw new Error(loads + " : " + ceg.toString());
								}
							} else {
								targeters[j].updateTarget(targets[k], next);
							}
						}
					}
				}
				loads = next;
			}

			InstructionHandle callRangeEnd = ih.getPrev();
			 // Remove the INVOKE* instruction
			try {
				il.delete(ih);
			} catch (TargetLostException e) {
				InstructionHandle[] targets = e.getTargets();
				for (int k = 0; k < targets.length; k++) {
					InstructionTargeter[] targeters = targets[k].getTargeters();
					for (int j = 0; j < targeters.length; j++) {
						if (targeters[j] instanceof CodeExceptionGen) {
							CodeExceptionGen ceg = (CodeExceptionGen) targeters[j];
							if (ceg.getStartPC() == ih
							    && ceg.getEndPC() == ih) {
							    ceg.setStartPC(callRangeBegin);
							    ceg.setEndPC(callRangeEnd);
							} else if (ceg.getStartPC() == ih) {
								targeters[j].updateTarget(targets[k],
										callRangeBegin);
							} else if (ceg.getEndPC() == ih) {
								targeters[j].updateTarget(targets[k], 
											  callRangeEnd);
							} else if (ceg.getHandlerPC() == ih) {
								targeters[j].updateTarget(targets[k],
										callRangeBegin);
							//} else {
							//	throw new Error(ih + " : " + ceg);
							}
						} else {
							targeters[j]
							    .updateTarget(targets[k], callRangeBegin);
						}
					}
				}
			}

			if (newMaxLocals < mergedRT.getEntries().length) {
				newMaxLocals = mergedRT.getEntries().length;
			}
			if (newMaxStack < maxStack + _maxStack) {
				newMaxStack = maxStack + _maxStack;
			}
			
			bcgen.setMaxLocals(newMaxLocals);
			bcgen.setMaxStack(newMaxStack);
		}

	private RegisterTable mergeRegisterTables(InstructionHandle callSiteIH,
			InstructionHandle precallSiteIH, ByteCodeGen caller, ByteCodeGen callee,
			int[] callerLocalMap, int[] calleeLocalMap) {
		final int callerArgSize = caller.getSelector().getDescriptor()
				.getArgumentLength() / 4 + 1;
		final int calleeArgSize = callee.getSelector().getDescriptor()
				.getArgumentLength() / 4 + 1;

		int loopDepth = ((Integer)callerIH2LoopDepth.get(callSiteIH)).intValue();
		boolean[] livenessAtCallSite = ((Liveness.Entry)callerIH2Liveness.get(precallSiteIH)).liveness();
		RegisterTable callerRT = getRegisterTableAttribute(caller);
		RegisterTable calleeRT = getRegisterTableAttribute(callee);
		RegisterTable.Entry[] callerRTEntries = callerRT.getEntries();
		RegisterTable.Entry[] calleeRTEntries = calleeRT.getEntries();
		HashMap callerRTEntry2Index = new HashMap();
		HashMap calleeRTEntry2Index = new HashMap();
		for (int i = 0; i < callerArgSize; i++) {
			if (callerRTEntries[i].type() == TypeCodes.VOID) {
				callerLocalMap[i] = -1;
			} else {
				callerLocalMap[i] = i;
			}
		}
		for (int i = 0; i < callerRTEntries.length; i++) {
			if (callerRTEntries[i].type() != TypeCodes.VOID) {
				callerRTEntry2Index.put(callerRTEntries[i], new Integer(i));
			}
		}
		for (int i = calleeArgSize; i < calleeRTEntries.length; i++) {
			if (calleeRTEntries[i].type() != TypeCodes.VOID) {
			    RegisterTable.Entry e = calleeRTEntries[i];
			    e.setScore(e.score() * tenToThePowerOf(loopDepth));
			    calleeRTEntry2Index.put(e, new Integer(i));
			}
		}
		HashSet liveCallerLocals = new HashSet();
		for (int i = 0; i < callerRTEntries.length; i++) {
			if (livenessAtCallSite[i]) {
				liveCallerLocals.add(callerRTEntries[i]);
			}
		}
		ArrayList callerTableA = new ArrayList();
		ArrayList callerTableI = new ArrayList();
		ArrayList callerTableJ = new ArrayList();
		ArrayList callerTableF = new ArrayList();
		ArrayList callerTableD = new ArrayList();
		ArrayList calleeTableA = new ArrayList();
		ArrayList calleeTableI = new ArrayList();
		ArrayList calleeTableJ = new ArrayList();
		ArrayList calleeTableF = new ArrayList();
		ArrayList calleeTableD = new ArrayList();
		ArrayList mergedTableA = new ArrayList();
		ArrayList mergedTableI = new ArrayList();
		ArrayList mergedTableJ = new ArrayList();
		ArrayList mergedTableF = new ArrayList();
		ArrayList mergedTableD = new ArrayList();

		int ptr = callerArgSize;
		//A
		while (ptr < callerRTEntries.length
				&& callerRTEntries[ptr].type() == TypeCodes.REFERENCE) {
			callerTableA.add(callerRTEntries[ptr]);
			ptr++;
		}
		//I
		while (ptr < callerRTEntries.length
				&& callerRTEntries[ptr].type() == TypeCodes.INT) {
			callerTableI.add(callerRTEntries[ptr]);
			ptr++;
		}
		//J
		while (ptr < callerRTEntries.length
				&& callerRTEntries[ptr].type() == TypeCodes.LONG) {
			callerTableJ.add(callerRTEntries[ptr]);
			ptr += 2;
		}
		//F
		while (ptr < callerRTEntries.length
				&& callerRTEntries[ptr].type() == TypeCodes.FLOAT) {
			callerTableF.add(callerRTEntries[ptr]);
			ptr++;
		}
		//D
		while (ptr < callerRTEntries.length
				&& callerRTEntries[ptr].type() == TypeCodes.DOUBLE) {
			callerTableD.add(callerRTEntries[ptr]);
			ptr += 2;
		}

		ptr = calleeArgSize;
		//A
		while (ptr < calleeRTEntries.length
				&& calleeRTEntries[ptr].type() == TypeCodes.REFERENCE) {
			calleeTableA.add(calleeRTEntries[ptr]);
			ptr++;
		}
		//I
		while (ptr < calleeRTEntries.length
				&& calleeRTEntries[ptr].type() == TypeCodes.INT) {
			calleeTableI.add(calleeRTEntries[ptr]);
			ptr++;
		}
		//J
		while (ptr < calleeRTEntries.length
				&& calleeRTEntries[ptr].type() == TypeCodes.LONG) {
			calleeTableJ.add(calleeRTEntries[ptr]);
			ptr += 2;
		}
		//F
		while (ptr < calleeRTEntries.length
				&& calleeRTEntries[ptr].type() == TypeCodes.FLOAT) {
			calleeTableF.add(calleeRTEntries[ptr]);
			ptr++;
		}
		//D
		while (ptr < calleeRTEntries.length
				&& calleeRTEntries[ptr].type() == TypeCodes.DOUBLE) {
			calleeTableD.add(calleeRTEntries[ptr]);
			ptr += 2;
		}

		// Merge
		HashMap callerNew2Old = new HashMap();
		HashMap calleeNew2Old = new HashMap();

		merge(callerTableA, calleeTableA, mergedTableA);
		merge(callerTableI, calleeTableI, mergedTableI);
		merge(callerTableJ, calleeTableJ, mergedTableJ);
		merge(callerTableF, calleeTableF, mergedTableF);
		merge(callerTableD, calleeTableD, mergedTableD);

// 		// A
// 		int callerIndex = 0;
// 		int calleeIndex = 0;
// 		while (callerIndex < callerTableA.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) callerTableA
// 					.get(callerIndex);
// 			if (true && liveCallerLocals.contains(e)) { // Do not merge
// 				insertSorted(mergedTableA, e);
// 			} else { // Merge
// 				if (calleeIndex < calleeTableA.size()) {
// 					RegisterTable.Entry _e = (RegisterTable.Entry) calleeTableA
// 							.get(calleeIndex);
// 					RegisterTable.Entry ne = new RegisterTable.Entry(e.type(),
// 							e.score() + _e.score());
// 					callerNew2Old.put(ne, e);
// 					calleeNew2Old.put(ne, _e);
// 					insertSorted(mergedTableA, ne);
// 					calleeIndex++;
// 				} else {
// 					insertSorted(mergedTableA, e);
// 				}
// 			}
// 			callerIndex++;
// 		}
// 		while (calleeIndex < calleeTableA.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) calleeTableA
// 					.get(calleeIndex);
// 			insertSorted(mergedTableA, e);
// 			calleeIndex++;
// 		}
// 		// I
// 		callerIndex = 0;
// 		calleeIndex = 0;
// 		while (callerIndex < callerTableI.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) callerTableI
// 					.get(callerIndex);
// 			if (true && liveCallerLocals.contains(e)) { // Do not merge
// 				insertSorted(mergedTableI, e);
// 			} else { // Merge
// 				if (calleeIndex < calleeTableI.size()) {
// 					RegisterTable.Entry _e = (RegisterTable.Entry) calleeTableI
// 							.get(calleeIndex);
// 					RegisterTable.Entry ne = new RegisterTable.Entry(e.type(),
// 							e.score() + _e.score());
// 					callerNew2Old.put(ne, e);
// 					calleeNew2Old.put(ne, _e);
// 					insertSorted(mergedTableI, ne);
// 					calleeIndex++;
// 				} else {
// 					insertSorted(mergedTableI, e);
// 				}
// 			}
// 			callerIndex++;
// 		}
// 		while (calleeIndex < calleeTableI.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) calleeTableI
// 					.get(calleeIndex);
// 			insertSorted(mergedTableI, e);
// 			calleeIndex++;
// 		}
// 		// J
// 		callerIndex = 0;
// 		calleeIndex = 0;
// 		while (callerIndex < callerTableJ.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) callerTableJ
// 					.get(callerIndex);
// 			if (true && liveCallerLocals.contains(e)) { // Do not merge
// 				insertSorted(mergedTableJ, e);
// 			} else { // Merge
// 				if (calleeIndex < calleeTableJ.size()) {
// 					RegisterTable.Entry _e = (RegisterTable.Entry) calleeTableJ
// 							.get(calleeIndex);
// 					RegisterTable.Entry ne = new RegisterTable.Entry(e.type(),
// 							e.score() + _e.score());
// 					callerNew2Old.put(ne, e);
// 					calleeNew2Old.put(ne, _e);
// 					insertSorted(mergedTableJ, ne);
// 					calleeIndex++;
// 				} else {
// 					insertSorted(mergedTableJ, e);
// 				}
// 			}
// 			callerIndex++;
// 		}
// 		while (calleeIndex < calleeTableJ.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) calleeTableJ
// 					.get(calleeIndex);
// 			insertSorted(mergedTableJ, e);
// 			calleeIndex++;
// 		}
// 		// F
// 		callerIndex = 0;
// 		calleeIndex = 0;
// 		while (callerIndex < callerTableF.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) callerTableF
// 					.get(callerIndex);
// 			if (true && liveCallerLocals.contains(e)) { // Do not merge
// 				insertSorted(mergedTableF, e);
// 			} else { // Merge
// 				if (calleeIndex < calleeTableF.size()) {
// 					RegisterTable.Entry _e = (RegisterTable.Entry) calleeTableF
// 							.get(calleeIndex);
// 					RegisterTable.Entry ne = new RegisterTable.Entry(e.type(),
// 							e.score() + _e.score());
// 					callerNew2Old.put(ne, e);
// 					calleeNew2Old.put(ne, _e);
// 					insertSorted(mergedTableF, ne);
// 					calleeIndex++;
// 				} else {
// 					insertSorted(mergedTableF, e);
// 				}
// 			}
// 			callerIndex++;
// 		}
// 		while (calleeIndex < calleeTableF.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) calleeTableF
// 					.get(calleeIndex);
// 			insertSorted(mergedTableF, e);
// 			calleeIndex++;
// 		}
// 		// D
// 		callerIndex = 0;
// 		calleeIndex = 0;
// 		while (callerIndex < callerTableD.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) callerTableD
// 					.get(callerIndex);
// 			if (true && liveCallerLocals.contains(e)) { // Do not merge
// 				insertSorted(mergedTableD, e);
// 			} else { // Merge
// 				if (calleeIndex < calleeTableD.size()) {
// 					RegisterTable.Entry _e = (RegisterTable.Entry) calleeTableD
// 							.get(calleeIndex);
// 					RegisterTable.Entry ne = new RegisterTable.Entry(e.type(),
// 							e.score() + _e.score());
// 					callerNew2Old.put(ne, e);
// 					calleeNew2Old.put(ne, _e);
// 					insertSorted(mergedTableD, ne);
// 					calleeIndex++;
// 				} else {
// 					insertSorted(mergedTableD, e);
// 				}
// 			}
// 			callerIndex++;
// 		}
// 		while (calleeIndex < calleeTableD.size()) {
// 			RegisterTable.Entry e = (RegisterTable.Entry) calleeTableD
// 					.get(calleeIndex);
// 			insertSorted(mergedTableD, e);
// 			calleeIndex++;
// 		}

		ArrayList combinedRTEntryList = new ArrayList();
		for (int i = 0; i < callerArgSize; i++) {
			combinedRTEntryList.add(callerRTEntries[i]);
		}
		for (int i = 0; i < mergedTableA.size(); i++) {
			combinedRTEntryList.add(mergedTableA.get(i));
		}
		for (int i = 0; i < mergedTableI.size(); i++) {
			combinedRTEntryList.add(mergedTableI.get(i));
		}
		for (int i = 0; i < mergedTableJ.size(); i++) {
			combinedRTEntryList.add(mergedTableJ.get(i));
			combinedRTEntryList
					.add(new RegisterTable.Entry(TypeCodes.VOID, -1));
		}
		for (int i = 0; i < mergedTableF.size(); i++) {
			combinedRTEntryList.add(mergedTableF.get(i));
		}
		for (int i = 0; i < mergedTableD.size(); i++) {
			combinedRTEntryList.add(mergedTableD.get(i));
			combinedRTEntryList
					.add(new RegisterTable.Entry(TypeCodes.VOID, -1));
		}
		RegisterTable.Entry[] combinedRTEntries = new RegisterTable.Entry[combinedRTEntryList
				.size()];
		combinedRTEntryList.toArray(combinedRTEntries);

		for (int i = callerArgSize; i < combinedRTEntries.length; i++) {
			RegisterTable.Entry e = combinedRTEntries[i];
			if (callerNew2Old.get(e) != null) {
				RegisterTable.Entry old = (RegisterTable.Entry) callerNew2Old
						.get(e);
				int oldIndex = ((Integer) callerRTEntry2Index.get(old))
						.intValue();
				callerLocalMap[oldIndex] = i;
			} else {
				Object _oldIndex = callerRTEntry2Index.get(e);
				if (_oldIndex != null) {
					int oldIndex = ((Integer) _oldIndex).intValue();
					callerLocalMap[oldIndex] = i;
				}
			}
			if (calleeNew2Old.get(e) != null) {
				RegisterTable.Entry old = (RegisterTable.Entry) calleeNew2Old
						.get(e);
				int oldIndex = ((Integer) calleeRTEntry2Index.get(old))
						.intValue();
				calleeLocalMap[oldIndex] = i;
			} else {
				Object _oldIndex = calleeRTEntry2Index.get(e);
				if (_oldIndex != null) {
					int oldIndex = ((Integer) _oldIndex).intValue();
					calleeLocalMap[oldIndex] = i;
				}
			}
		}

		return RegisterTable.make(combinedRTEntries);
	}

	}

	private static RegisterTable getRegisterTableAttribute(S3ByteCode bc) {
		Attribute[] attrs = bc.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i] instanceof RegisterTable) {
				return (RegisterTable) attrs[i];
			}
		}
		return null;
	}

    private static int[] tenToThePowerOf = new int[] {1, 10, 100, 1000, 10000, 100000, 1000000};
    private static int tenToThePowerOf(int e) {
	if (e < tenToThePowerOf.length) {
	    return tenToThePowerOf[e];
	} else {
	    return power(10, e);
	}
    }
    private static int power(int a, int b) {
	int r = 1;
	for(int i = 0; i < b; i++) {
	    r = r * a;
	}
	return r;
    }

	private static RegisterTable getRegisterTableAttribute(ByteCodeGen bcgen) {
		Attribute[] attrs = bcgen.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i] instanceof RegisterTable) {
				return (RegisterTable) attrs[i];
			}
		}
		return null;
	}

	private static Liveness getLivenessAttribute(S3ByteCode bc) {
		Attribute[] attrs = bc.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i] instanceof Liveness) {
				return (Liveness) attrs[i];
			}
		}
		return null;
	}

	private static Liveness getLivenessAttribute(ByteCodeGen bcgen) {
		Attribute[] attrs = bcgen.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i] instanceof Liveness) {
				return (Liveness) attrs[i];
			}
		}
		return null;
	}

	private static char[] getArgumentTypes(S3ByteCode bc) {
		Descriptor.Method desc = bc.getSelector().getDescriptor();
		char[] args = new char[desc.getArgumentCount()];
		for (int i = 0; i < args.length; i++) {
			args[i] = desc.getArgumentType(i).getTypeTag();
		}
		return args;
	}

    private static void merge(ArrayList callerTable, ArrayList calleeTable, ArrayList mergedTable) {
		int callerIndex = 0;
		int calleeIndex = 0;
		int callerTableSize = callerTable.size();
		int calleeTableSize = calleeTable.size();
		while (callerIndex < callerTableSize && calleeIndex < calleeTableSize) {
			RegisterTable.Entry r = (RegisterTable.Entry) callerTable.get(callerIndex);
			RegisterTable.Entry e = (RegisterTable.Entry) calleeTable.get(calleeIndex);
			if (r.score() >= e.score()) {
			    mergedTable.add(r);
			    callerIndex++;
			} else {
			    mergedTable.add(e);
			    calleeIndex++;
			}
		}
		while (callerIndex < callerTableSize) {
			RegisterTable.Entry e = (RegisterTable.Entry) callerTable.get(callerIndex);
			mergedTable.add(e);
			callerIndex++;
		}
		while (calleeIndex < calleeTableSize) {
			RegisterTable.Entry e = (RegisterTable.Entry) calleeTable.get(calleeIndex);
			mergedTable.add(e);
			calleeIndex++;
		}
    }

	private static void insertSorted(ArrayList table, RegisterTable.Entry e) {
		for (int i = 0; i < table.size(); i++) {
			RegisterTable.Entry _e = (RegisterTable.Entry) table.get(i);
			if (e.score() >= _e.score()) {
				table.add(i, e);
				return;
			}
		}
		table.add(e);
	}

	protected static void transformCallee(ByteCodeGen bcgen,
			InstructionHandle exit, ConstantsEditor callerCP,
			int[] calleeLocalMap) {
		TransformCalleeVisitor2 v = new TransformCalleeVisitor2(bcgen, exit,
				callerCP, calleeLocalMap);
		v.run();
	}

	/**
	 * Update locals, replace returns with gotos, copy the CP entries to the caller's
	 */
	protected static class TransformCalleeVisitor2 extends
			TransformCalleeVisitor {
		int[] localMap;

		public TransformCalleeVisitor2(ByteCodeGen bcgen,
				InstructionHandle exit, ConstantsEditor callerCP, int[] localMap) {
			super(bcgen, exit, callerCP, -1);
			this.localMap = localMap;
		}

		public void run() {
			bcgen.removeLineNumbers();
			bcgen.removeLocalVariables();

			ih = il.getStart();
			while (ih != null) {
				InstructionHandle next = ih.getNext();
				ih.accept(this);
				ih = next;
			}

			int maxSize = 0;
			maxSize = 1;
			char[] argTypes = bcgen.getArgumentTypes();
			for (int i = 0; i < argTypes.length; i++) {
				char t = toBasicType(argTypes[i]);
				int size = typeCode2Size(t);
				if (maxSize < size)
					maxSize = size;
			}
		}

		private int localMap(int old) {
			return localMap[old];
		}

		public void visit(Instruction.LocalWrite o) {
			ih.setInstruction(Instruction.LocalWrite.make(o.getTypeCode(),
					localMap(o.getLocalVariableOffset())));
		}

		public void visit(Instruction.LocalRead o) {
			ih.setInstruction(Instruction.LocalRead.make(o.getTypeCode(),
					localMap(o.getLocalVariableOffset())));
		}

		public void visit(Instruction.IINC o) {
			ih.setInstruction(Instruction.IINC.make(localMap(o
					.getLocalVariableOffset()), o.getValue()));
		}

		public void visit(Instruction.RET o) {
			ih.setInstruction(Instruction.RET.make(NumberRanges
					.checkUnsignedShort(localMap(o.getLocalVariableOffset()))));
		}

		public void visit(Instruction.WIDE_RET o) {
			ih.setInstruction(Instruction.WIDE_RET.make(NumberRanges
					.checkUnsignedShort(localMap(o.getLocalVariableOffset()))));
		}

		public void visit(Instruction.ReturnValue o) {
			if (ih != il.getEnd()) {
			    if (! bcgen.getSelector().getDescriptor().isReturnValueVoid()) {
				InstructionHandle store_or_pop = exit;
				Instruction ins = store_or_pop.getInstruction();
				if (! (ins instanceof Instruction.POP ||
				       ins instanceof Instruction.POP2 ||
				       ins instanceof Instruction.LocalWrite)) {
				    throw new Error("Should be POP, POP2 or LocalWrite : " + ins);
				}
				il.insert(ih, store_or_pop.getInstruction());
				//store_or_pop.setInstruction(Instruction.NOP.make());
				ih.setInstruction(Instruction.GOTO.make(store_or_pop.getNext()));
			    } else {
				ih.setInstruction(Instruction.GOTO.make(exit));
			    }
			} else {
				try {
					il.delete(ih);
				} catch (TargetLostException e) {
					InstructionHandle[] targets = e.getTargets();
					for (int k = 0; k < targets.length; k++) {
						InstructionTargeter[] targeters = targets[k]
								.getTargeters();
						for (int j = 0; j < targeters.length; j++) {
							targeters[j].updateTarget(targets[k], exit);
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
							targeters[j].updateTarget(targets[k], exit);
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
			switch (tag) {
			case JVMConstants.CONSTANT_ResolvedStaticMethod:
			case JVMConstants.CONSTANT_ResolvedInstanceMethod:
			case JVMConstants.CONSTANT_Methodref:
				newIndex = callerCP.addMethodref((ConstantMethodref) entry);
				break;
			case JVMConstants.CONSTANT_ResolvedInterfaceMethod:
			case JVMConstants.CONSTANT_InterfaceMethodref:
				newIndex = callerCP
						.addInterfaceMethodref((ConstantMethodref) entry);
				break;
			case JVMConstants.CONSTANT_ResolvedStaticField:
			case JVMConstants.CONSTANT_ResolvedInstanceField:
			case JVMConstants.CONSTANT_Fieldref:
				newIndex = callerCP.addFieldref((ConstantFieldref) entry);
				break;
			case JVMConstants.CONSTANT_Class:
			case JVMConstants.CONSTANT_ResolvedClass:
				newIndex = callerCP.addClass((ConstantClass) entry);
				break;
			case JVMConstants.CONSTANT_SharedState:
				newIndex = callerCP
						.addUnresolvedSharedState((TypeName.Gemeinsam) entry);
				break;
			case JVMConstants.CONSTANT_String:
				newIndex = callerCP
						.addUnresolvedString((RepositoryString) entry);
				break;
			default:
				newIndex = callerCP.addResolvedConstant(entry);
				break;
			}
			if (newIndex < 0) {
				throw new Error(o.getClass().toString());
			}
			/*
			byte newTag = ((Constants) callerCP).getTagAt(newIndex);
			Object newEntry = ((Constants) callerCP).getConstantAt(newIndex);
			if (!(newTag == tag && entry == newEntry)
			//		 || !(tag == JVMConstants.CONSTANT_String && entry.equals(newEntry))) {
					&& !(newTag == tag && entry.equals(newEntry))) {
				throw new Error("tag=" + tag + ", newtag=" + newTag
						+ ", entry=" + entry.getClass() + ":" + entry
						+ ", newentry=" + newEntry.getClass() + ":" + newEntry);
			}
			*/
			o.setCPIndex(newIndex);
		}
	}

	protected static void transformCaller(ByteCodeGen bcgen,
			int[] callerLocalMap) {
		TransformCallerVisitor v = new TransformCallerVisitor(bcgen,
				callerLocalMap);
		v.run();
	}

	/**
	 * Update locals
	 */
	protected static class TransformCallerVisitor extends Instruction.Visitor {
		ByteCodeGen bcgen;

		Constants cp;

		InstructionList il;

		InstructionHandle ih;

		Vector argTypeList;

		int argSize;

		int[] localMap;

		public TransformCallerVisitor(ByteCodeGen bcgen, int[] localMap) {
			this.bcgen = bcgen;
			this.il = bcgen.getInstructionList();
			cp = bcgen.getConstantPool();
			argTypeList = new Vector();
			this.localMap = localMap;
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

		private int localMap(int old) {
			return localMap[old];
		}

		public void visit(Instruction.LocalWrite o) {
			ih.setInstruction(Instruction.LocalWrite.make(o.getTypeCode(),
					localMap(o.getLocalVariableOffset())));
		}

		public void visit(Instruction.LocalRead o) {
			ih.setInstruction(Instruction.LocalRead.make(o.getTypeCode(),
					localMap(o.getLocalVariableOffset())));
		}

		public void visit(Instruction.IINC o) {
			ih.setInstruction(Instruction.IINC.make(localMap(o
					.getLocalVariableOffset()), o.getValue()));
		}

		public void visit(Instruction.RET o) {
			ih.setInstruction(Instruction.RET.make(NumberRanges
					.checkUnsignedShort(localMap(o.getLocalVariableOffset()))));
		}

		public void visit(Instruction.WIDE_RET o) {
			ih.setInstruction(Instruction.WIDE_RET.make(NumberRanges
					.checkUnsignedShort(localMap(o.getLocalVariableOffset()))));
		}

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
