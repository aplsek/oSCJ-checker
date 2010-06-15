package s3.services.bootimage;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantResolvedMethodref;
import ovm.core.domain.ConstantResolvedStaticMethodref;
import ovm.core.domain.Domain;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.Descriptor;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.MethodCodeScanner;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.services.bytecode.editor.Cursor;
import ovm.services.bytecode.editor.InstructionEditVisitor;
import ovm.services.bytecode.editor.LocalsShifter;
import ovm.util.HashSet;
import ovm.util.Iterator;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3MemberResolver;
import s3.services.bootimage.Analysis.MethodWalker;
import s3.util.PragmaForwardCallingContext;

/**
 * Allow methods in the GNU Classpath runtime library to inspect their
 * caller regardless of inlining.  We treat two classpath methods as
 * builtins:  gnu.classpath.VMStackWalker.getCallingClass() and 
 * gnu.classpath.VMStackWalker.getCallingClassLoader().  We transform
 * every method that calls one of these builtins to take the caller's
 * declaring class as a synthetic parameter, and supply these
 * parameters as needed.<p>
 *
 * The key questions here are which virtual methods should be given
 * extra parameters, and how can any of this work with dynamic
 * loading.<p>
 *
 * Consider the following:<pre>{@code
   interface I { Class m(); }
   class C implements I {
     public Class m() { return VMStackWalker.getCallingClass(); }
   }
   class D {
     public class m() { return Object.class; }
   }
   class E implements I {
     public class m() { return String.class; }
   }
 }</pre>
 *
 * Because C.m uses the calling-class parameter, we must adjust C's
 * supertypes (in particular I), and their subtypes (such as E).
 * Because E.m now takes a calling-class parameter, this parameter
 * must spread to its supertype, D.  Thus D.m will take a
 * calling-class parameter even though there is no direct relationship
 * between it and a method that actually uses stack walker builtins.<p>
 *
 * We avoid crawling the type hierarchy when a builtin is encountered
 * in a virtual method.  Rather than find the exact set of methods
 * that must take a calling-class parameter, we simply add the
 * parameter to all virtual/interface methods with the same
 * signature.<p>
 *
 * We allow exceptions where a virtual method is final (or declared in
 * a final class) and does not override any methods in its
 * supertypes.  This ensures that {@code java.lang.reflect.Field.get()} 
 * will not be confused with {@code java.util.Map.get()}.<p>
 *
 * This code handles {@link PragmaForwardCallingContext} the same
 * way as the java implementations of {@code getCallingClass} and
 * {@code getCallingClassLoader}: activations of methods with this
 * pragma are ignored.  Any method that is marked with this parameter
 * will take a calling class parameter that it simply forwards to
 * callees.<p>
 *
 * Before this transformation can be used with dynamically loaded
 * code, several issues must be addressed
 * <ul>
 *    <li> Builtins called from dynamically loaded methods may
 *         invalidate precompiled callers.  We may discover that a
 *         virtual/interface call takes the calling-class parameter at
 *         runtime, even though it was precompiled without that
 *         parameter.
 *    <li> Builtins called from dynamically loaded methods may change
 *         the signatures of precompiled methods.  A method may
 *         suddenly start taking a calling-class parameter even
 *         though it was compiled without that parameter.  This is
 *         actually harmless because the calling-class parameter will
 *         appear last.
 *    <li> Even if we do not allow builtins to be called from
 *         dynamically loaded code, we still need to correctly resolve
 *         constant pool entries for methods that take an extra
 *         parameter, and do this resolution early.
 * </ul>
 * Probably, the best approach is to run the intraprocedural part of
 * this analysis ({@link #findMagicCalls}) on the runtime library in a
 * pre-processing phase.  It can mark methods that take the class
 * parameter with a pragma, and dump the list of virtual/interface
 * signrautres to a file.
 *   
 * @see <a href="http://sss.cs.purdue.edu/index.php/StaticAnalysisForStaticCompilation#Stack_Inspection_for_Reflective_Code">the wiki</a>
 * @see PragmaForwardCallingClass
 * @see ovm.core.execution.RuntimeExports#getClassContext
 **/
public class CallingClassTransform {
    final Analysis anal;
    final Domain domain;
    /**
     * A set of UnboundSelector.Methods for all virtual/interface
     * methods that needs a calling class paramater.  Any virtual
     * method with the same signature will get the extra parameter,
     * regardless of whether they directly or indirectly share a
     * common base method in a class or interface.
     **/
    final HashSet transformedVirtuals = new HashSet();
    /** A set of Methods. **/
    final HashSet transformedNonvirtuals = new HashSet();

    /** The representation of VMStackWalker.getCallingClass() **/
    final Method getCallingClass;
    /** The representation of VMStackWalker.getCallingClassLoader() **/
    final Method getCallingClassLoader;
    /** java.lang.Class as a Blueprint **/
    final Blueprint classBP;
    /** java.lang.Class as a TypeName **/
    final TypeName classTypeName;
    /** The constant pool entry for Class.getClassLoader() **/
    final ConstantResolvedStaticMethodref getClassLoaderMethodref;

    /** The Class object for the current method's declaring class **/
    Oop currentSharedState;
    /** The local variable offset of the caller's class Object **/
    int callerParam;
    /** The blueprint of the current method **/
    Blueprint curBP;

    public CallingClassTransform(Analysis anal) {
	this.anal = anal;
	this.domain = anal.domain;
	Type.Context ctx = domain.getSystemTypeContext();
	Type.Scalar VMStackWalker = 
	    ctx.typeForKnown(JavaNames.gnu_classpath_VMStackWalker).asScalar();
	VMStackWalker = VMStackWalker.getSharedStateType();
	getCallingClass = VMStackWalker.getMethod(JavaNames.GET_CALLING_CLASS);
	getCallingClassLoader = 
	    VMStackWalker.getMethod(JavaNames.GET_CALLING_CLASS_LOADER);
	assert (getCallingClass != null && getCallingClassLoader != null);
	Type.Scalar classType = domain.getMetaClass();
	classBP = domain.blueprintFor(classType);
	classTypeName = classType.getName();
	Type.Class vmClassType = ctx.typeForKnown(JavaNames.java_lang_VMClass).asClass();
	vmClassType = vmClassType.getSharedStateType();
	Method getClassLoader = vmClassType.getMethod(JavaNames.VMClass_GET_CLASS_LOADER);
	assert(getClassLoader != null);
	int offset = S3MemberResolver.resolveNonVTableOffset
	    (domain.blueprintFor(vmClassType),
	     JavaNames.VMClass_GET_CLASS_LOADER,
	     classType);	// skip access checks
	getClassLoaderMethodref = 
	    ConstantResolvedStaticMethodref.make(getClassLoader,
						 offset,
						 vmClassType.getSingleton());
    }

    private static boolean declaredInSupertypes(Type t,
						UnboundSelector.Method sel) {
	Type.Class parent = t.getSuperclass();
	Type.Interface[] ifc = t.getInterfaces();
	if (parent != null
	    && (parent.getMethod(sel) != null
		|| declaredInSupertypes(parent, sel))) {
	    return true;
	}
	for (int i = 0; i < ifc.length; i++)
	    if (ifc[i].getMethod(sel) != null
		|| declaredInSupertypes(ifc[i], sel)) {
		return true;
	    }
	return false;
    }

    /**
     * Return true if polymorphic calls to m are impossible.  This
     * method does not assume that the whole program is known.
     **/
    private static boolean isEffectivelyNonvirtual(Method m) {
	if (!m.isVirtual())
	    return true;
	Type.Compound t = m.getDeclaringType();
	return ((m.getMode().isFinal() || t.getMode().isFinal())
		&& !declaredInSupertypes(t,
					 m.getSelector().getUnboundSelector()));
    }
	
    private void markForMagicParameter(Method client) {
	if (isEffectivelyNonvirtual(client)) {
	    transformedNonvirtuals.add(client);
// 	    System.out.println("adding class context to nonvirtual method "
// 			       + client);
	} else {
	    UnboundSelector.Method ubs =
		client.getSelector().getUnboundSelector();
	    transformedVirtuals.add(ubs);
// 	    System.out.println("adding class context to virtual method " + ubs);
	}
    }

    /**
     * Search for calls to {@link #getCallingClass} or
     * {@link #getCallingClassLoader} in a method body.  If such a
     * call is found, add the calling method to {@link #transformedVirtuals}
     * or {@link #transformedNonvirtuals} as appropriate.
     **/
    final MethodCodeScanner findMagicCalls = new MethodCodeScanner() {
	    public void visit(Instruction.INVOKESTATIC i) {
		ConstantMethodref mr = i.getConstantMethodref(buf, cp);
		try {
		    Method m = ((ConstantResolvedMethodref) mr).getMethod();
		    if (m == getCallingClass || m == getCallingClassLoader) {
			markForMagicParameter(code.getMethod());
		    }
		} catch (ClassCastException _) { }
	    }
	};

    /**
     * Return true if the given method takes a calling-class parameter.
     **/
    private boolean takesMagicParameter(Method m) {
	if (transformedNonvirtuals.contains(m))
	    return true;
	if (m.isVirtual() || m.isInterface()) {
	    UnboundSelector.Method ubs = m.getSelector().getUnboundSelector();
	    return transformedVirtuals.contains(ubs);
	}
	return false;
    }

    /**
     * Add calling-class parameter to a method call if needed
     * @param m   the called method
     * @param cfe the CodeFragmentEditor for m's caller
     * @param buf the InstructionBuffer for m's caller.  It must be
     *            positioned at m's call site
     **/
    private void pushCurrentClassIfNeeded(Method m,
					  CodeFragmentEditor cfe,
					  InstructionBuffer buf) {
	if (takesMagicParameter(m)) {
	    Cursor c = cfe.getCursorBeforeMarker(buf.getPC());
	    if (PragmaForwardCallingContext.declaredBy(buf.getSelector(),
						       curBP))
		c.addALoad((char) callerParam);
	    else
		c.addResolvedRefLoadConstant(currentSharedState);
	}
    }

    /**
     * This tranformatin is responsible for rewriting calls to
     * VMStackWalker.getCallingClass and
     * VMStackWalker.getCallingClassLoader.  It is also responsible
     * for passing the current class to other methods that may use
     * VMStackWalker builtin funcitonality.  
     **/
    private final InstructionEditVisitor transformMagicCalls = 
	new LocalsShifter(1, 0) {
	    public void beginEditing(InstructionBuffer buf,
				     CodeFragmentEditor cfe) {
		super.beginEditing(buf, cfe);
		Descriptor.Method desc = buf.getSelector().getDescriptor();
		callerParam = (desc.getArgumentCount() +
			       desc.getWideArgumentCount() +
			       1);
		fromIndex = callerParam; // shift locals after the new param
	    }

	    public void visit(Instruction.Invocation i) {
		ConstantMethodref mr = i.getConstantMethodref(buf, cp);
		if (!(mr instanceof ConstantResolvedMethodref))
		    return;
		Method m = ((ConstantResolvedMethodref) mr).getMethod();
		if (m == getCallingClass) {
		    Cursor c = cfe.replaceInstruction();
		    c.addALoad((char) callerParam);
		} else if (m == getCallingClassLoader) {
		    Cursor c = cfe.replaceInstruction();
		    // These instructions are added during quickification
		    // c.addLOAD_SHST_METHOD(getClassLoaderMethodref);
		    c.addALoad((char) callerParam);
		    c.addINVOKESTATIC(getClassLoaderMethodref);
		} else {
		    pushCurrentClassIfNeeded(m, cfe, buf);
		}
	    }
	};

    /**
     * This visitor is responsible for filling in the calling-class
     * parameter for calls that may or may not need to know their
     * calling class.  If the CallingClassTransform is used at gen-ovm
     * time, we need to run this transformation on methods that are
     * loaded at runtime.  This requires some thought.<p>
     *
     * If we allow dynamically loaded methods to use the VMStackWalker
     * builtins, we can run into problems when precompiled methods
     * suddenly start statisfying {@link #takesMagicParameter} based
     * on their signatures.<p>
     *
     * Even if we don't allow dynamically loaded methods to use
     * VMStackWalker builtins, they may still satisfy
     * {@link #takesMagicParameter} based on their signatures.
     * Runtime rewriting needs to account for this.<p>
     *
     * Also note that this code assumes that method invocations have
     * already been resolved.
     * {@link s3.services.bytecode.ovmify.IRewriter} currently
     * resolves all methods, but it really shouldn't.  Maybe there
     * should be some sort of weak probe to check whether constant
     * pool resolution can be done without classloading.  All the
     * interesting classes for IRewriter and this tranformation should
     * be part of any bootimage.
     **/
    private final InstructionEditVisitor fillMagicParameters = 
	new InstructionEditVisitor() {
	    public void visit(Instruction.Invocation i) {
		ConstantMethodref mr = i.getConstantMethodref(buf, cp);
		if (!(mr instanceof ConstantResolvedMethodref))
		    return;
		Method m = ((ConstantResolvedMethodref) mr).getMethod();
		pushCurrentClassIfNeeded(m, cfe, buf);
	    }
	};

    /**
     * Perform the calling class transformation on a program.
     **/
    public void run() {
	new MethodWalker() {
	    public void walk(Method m) {
		Blueprint bp = domain.blueprintFor(m.getDeclaringType());
		if (PragmaForwardCallingContext.declaredBy(m.getSelector(),
							   bp))
		    markForMagicParameter(m);
		else
		    findMagicCalls.run(m);
	    }
	}.walkDomain(domain);

	new MethodWalker() {
	    public void walk(Method m) {
		Type t = m.getDeclaringType();
		curBP = domain.blueprintFor(t);
		if (!t.isSharedState())
		    t = t.getSharedStateType();
		currentSharedState = ((Type.Class) t).getSingleton();
	
		InstructionEditVisitor tranform =  (takesMagicParameter(m)
						    ? transformMagicCalls
						    : fillMagicParameters);
		tranform.run(m);
	    }
	}.walkDomain(domain);

	// Be sure to add class parameters to abstract and interface
	// methods.  Otherwise, j2c will inevitably freak out at these
	// methods' call sites.
	for (Iterator bit = domain.getBlueprintIterator(); bit.hasNext(); ) {
	    Blueprint bp = (Blueprint) bit.next();
	    Type t = bp.getType();
	    for (Method.Iterator mit = t.localMethodIterator();
		 mit.hasNext(); ) {
		Method m = mit.next();
		if (takesMagicParameter(m))
		    m.appendSyntheticParameter(classTypeName);
	    }
	}
    }
}
