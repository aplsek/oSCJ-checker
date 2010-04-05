package s3.services.bootimage;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedInstanceFieldref;
import ovm.core.domain.ConstantResolvedMethodref;
import ovm.core.domain.ConstantResolvedStaticFieldref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.domain.UserDomain;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.MethodCodeScanner;
import ovm.services.io.Resource;
import ovm.services.io.ResourcePath;
import ovm.util.BitSet;
import ovm.util.OVMException;
import ovm.util.UnicodeBuffer;
import ovm.services.bytecode.JVMConstants;

public class ClassHierarchyAnalysis extends Analysis {
    boolean once;
    String forcePath;
    
    protected ClassHierarchyAnalysis(Domain d, 
				     String forcePath) {
        super(d);
	this.forcePath = forcePath;
        seen = new BitSet[DomainDirectory.maxContextID() + 1];
        for (int i = 0; i < seen.length; i++) {
            Type.Context tc = DomainDirectory.getContext(i);
            if (tc != null && tc.getDomain() == d)
                seen[i] = new BitSet();
        }
    }

    BitSet[] seen;

    private void addClass(Type t) {
	t = t.getInstanceType();
	addClass(t, domain.blueprintFor(t));
    }
    private void addClass(Blueprint bp) {
	addClass(bp.getType(), bp);
    }

    private void addClass(Type t, Blueprint bp) {
	if (shouldCompile(bp) && !seen[bp.getCID()].get(bp.getUID())) {
	    seen[bp.getCID()].set(bp.getUID());
	    if (t.getSuperclass() != null)
		addClass(t.getSuperclass());
	    Type[] ifs = t.getInterfaces();
	    for (int i = 0; i < ifs.length; i++)
		addClass(ifs[i]);
	    memberPusher.walkBlueprint(bp);
	    // System.err.println("add class " + t);
	}
    }

    public class Visitor extends MethodCodeScanner {
	void visitMethodref(ConstantResolvedMethodref r) {
	    Type t = r.getMethod().getDeclaringType();
	    if (t.getDomain() == domain)
		addClass(t);
	}
	public void visit(Instruction.INVOKEINTERFACE i) {
	    try {
		visitMethodref(cp.resolveInterfaceMethod(i.getCPIndex(buf)));
	    }
	    catch (LinkageException _) { }
	}
	public void visit(Instruction.INVOKEVIRTUAL i) {
	    try {
		visitMethodref(cp.resolveInstanceMethod(i.getCPIndex(buf)));
	    } catch (LinkageException _) { }
	}
	void addSharedState(Oop oop) {
	    addClass(oop.getBlueprint().getInstanceBlueprint());
	}
	public void visit(Instruction.INVOKESTATIC i) {
	    try {
		ConstantResolvedStaticMethodref r =
		    cp.resolveStaticMethod(i.getCPIndex(buf));
		addSharedState(r.getSharedState());
	    }
	    catch (LinkageException _) { }
	}

	void visitStaticField(Instruction.FieldAccess i) {
	    try {
		ConstantResolvedStaticFieldref r =
		    cp.resolveStaticField(i.getCPIndex(buf));
		addSharedState(r.getSharedState());
	    }
	    catch (LinkageException _) { }
	}
	public void visit(Instruction.GETSTATIC i) {
	    visitStaticField(i);
	}
	public void visit(Instruction.PUTSTATIC i) {
	    visitStaticField(i);
	}

	void visitInstanceField(Instruction.FieldAccess i) {
	    try {
		ConstantResolvedInstanceFieldref r =
		    cp.resolveInstanceField(i.getCPIndex(buf));
		addClass(r.getField().getType());
	    } catch (LinkageException _) { }
	}
	public void visit(Instruction.GETFIELD i) {
	    visitInstanceField(i);
	}
	public void visit(Instruction.PUTFIELD i) {
	    visitInstanceField(i);
	}

	void visitClassRef(Instruction.Resolution i) {
	    try {
		addClass(cp.resolveClassAt(i.getCPIndex(buf)));
	    }
	    catch (LinkageException _) { }
	}
	public void visit(Instruction.NEW i) {
	    visitClassRef(i);
	}
	public void visit(Instruction.SINGLEANEWARRAY i) {
	    visitClassRef(i);
	}
	public void visit(Instruction.MULTIANEWARRAY i) {
	    visitClassRef(i);
	}
	public void visit(Instruction.CHECKCAST i) {
	    visitClassRef(i);
	}
	public void visit(Instruction.INSTANCEOF i) {
	    visitClassRef(i);
	}

	public void visit(Instruction.ConstantPoolLoad i) {
	    int idx = i.getCPIndex(buf);
	    if (cp.getTagAt(idx) == JVMConstants.CONSTANT_SharedState) {
		TypeName.Gemeinsam gtn =
		    (TypeName.Gemeinsam) cp.getConstantAt(idx);
		Type.Context ctx =
		    code.getMethod().getDeclaringType().getContext();
		try {
		    addClass(ctx.typeFor(gtn.getInstanceTypeName()));
		} catch (LinkageException e) {
		    warnMissing("Can't resolve class constant "  + gtn
				+ ": " + e.getMessage());
		}
	    }
	}
    }

    final Visitor visitor = new  Visitor();

    public void analyzeMethod(Method meth) {
	super.analyzeMethod(meth);
	visitor.run(meth);
    }

    private final MethodWalker memberPusher = new MethodWalker() {
	    public void walkBlueprint(Blueprint bp) {
		Type t = bp.getType();
		for (Field.Iterator it = t.localFieldIterator();
		     it.hasNext();
		     )
		    try { addClass(it.next().getType()); }
		    catch (LinkageException _) { }
		if (!t.isSharedState()) 
		    walkBlueprint(domain.blueprintFor(t.getSharedStateType()));
		super.walkBlueprint(bp);
	    }
	    public void walk(Method meth) {
		//System.err.println("walking " + meth.getSelector());
		try {
		    for (int i = 0; i < meth.getArgumentCount(); i++)
			try { addClass(meth.getArgumentType(i)); }
			catch (LinkageException _) { return; }
		    try { addClass(meth.getReturnType()); }
		    catch (LinkageException _) { return; }
		    addMethod(meth);
		} catch (Exception e) {
		    System.err.println("\n" + meth + " failed");
		    e.printStackTrace(System.err);
		}
	    }
        
            public void walkDead(Method meth) {
	    }

	    protected boolean shouldWalk(Method m) {
		return shouldAnalyze(m);
	    }
	};

    /**
     * Force loading of all classes on the given path.
     **/
    private void forceLoading() {
	if (forcePath == null || once)
	    return;
	once = true;
	final Type.Context ctx = domain.getApplicationTypeContext();
	try {
	    new ResourcePath(forcePath).forAll
		(".class",
		 new Resource.Action() {
		     public void process(Resource r) {
			 String name = r.getPath();
			 try {
			     UnicodeBuffer buf =
				UnicodeBuffer.factory().wrap(name);
			    int dotPos = buf.lastPositionOf('.');
			    TypeName tn =
				TypeName.Scalar.parseUntagged(buf, dotPos);
			    addClass(ctx.typeFor(tn));
			} catch (Exception e) {
			    System.err.println("could not load "
					       + name + " from "
					       + forcePath + ": "
					       + e.getMessage());
			}
		    }
		 });
	} catch (OVMException e) {
	    System.err.println("error iterating over -forcepath "
			       + forcePath + ": " + e.getMessage());
	}
    }

    public void analyzeCode() {
	forceLoading();
	super.analyzeCode();
    }

    public boolean analyzePartialHeap(java.util.Iterator it) {
	forceLoading();
	while (it.hasNext()){
	    Blueprint bp = ((Oop) it.next()).getBlueprint();
	    if (!bp.isSharedState())
		addClass(bp);
	}
	return analyze();
    }

    public void addNonvirtualCall(Method m) {
	addClass(m.getDeclaringType());
		 
    }
    public void addVirtualCall(Method m) {
	addClass(m.getDeclaringType());
		 
    }
    public void addFieldRead(Field f) {
	addClass(f.getDeclaringType());
    }
    public void addFieldWrite(Field f) {
	addClass(f.getDeclaringType());
    }
    public void addArrayAccess(Type.Array _) { }
    public void addAllocation(Type t) {
	addClass(t);
    }

    public static class Factory extends Analysis.Factory {
	String forcePath;
	
	public Analysis make(UserDomain d) {
	    return new ClassHierarchyAnalysis(d, forcePath);
	}

	public Factory(String forcePath) {
	    this.forcePath = forcePath;
	}
    }
}
