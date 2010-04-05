package ovm.core.execution;

import ovm.core.Executive;
import ovm.core.domain.AttributedCode;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryPolicy;

/**
 * An activation that dispatches exceptions through
 * Code.getExceptionHandlers and relies on the dispatching code to
 * release locks for synchronized methods.
 **/
public abstract class ExceptionTableActivation extends Activation {
    static final TypeName outOfMemoryError =
	JavaNames.java_lang_OutOfMemoryError;
    static final boolean DEBUG_THROW = false;
    static final Type.Scalar wildcardExType;

    static {
	Domain d = DomainDirectory.getExecutiveDomain();
	Type.Context ctx = d.getSystemTypeContext();
	Type t = ctx.typeForKnown(JavaNames.ovm_core_domain_WildcardException);
	wildcardExType = t.asScalar();
    }

    public abstract Oop getLocal(int idx);
    public abstract void setOperand(int idx, Oop value);
    public abstract void setOperandStackDepth(int depth);
    public abstract void setPC(int pc);
    public abstract void cutTo();

    /**
     * This code (nor code it invokes) must never throw exceptions nor
     * try to catch exceptions. To detect if this occurs we set up a
     * check for recursive entry into the exception handling code. If
     * this happens then we abort.
     * <p>
     * Beware that any debugging code in here should use the most
     * primitive facilities available and avoid things like string
     * concatenation, or toString() invocations that might generate
     * nested exceptions.
     **/
    public void processThrowable(Oop throwable) {
        Blueprint bp = null;
        if (throwable == null || (bp = throwable.getBlueprint()) == null)
            throw Executive.panic("processThrowable: null throwable or blueprint");
        // watch for exceptions in the exception processing code. The one
        // "exception" to this is OOME which can reasonably be expected to
        // occur whilst generating another exception
        Context ctx = Context.getCurrentContext(); 
        Type exType = bp.getType();
        Domain exTypeDomain = exType.getDomain();

        if (ctx.flags[Context.EXCEPTION_THROW_RECURSION]
            && exType.getUnrefinedName() != outOfMemoryError) {
            throw Executive.panic("processThrowable: Nested exception occurred");
        }
        ctx.flags[Context.EXCEPTION_THROW_RECURSION] = true;

        // 'this' is always CSA.processThrowable
        ExceptionTableActivation act = this;

        // first skip to the real caller
        for (act = (ExceptionTableActivation) act.caller(act);
	     act != null;
	     act = (ExceptionTableActivation) act.caller(act)) {
            AttributedCode cf = (AttributedCode) act.getCode();
            int relPC = act.getPC();
            if (DEBUG_THROW) {
                Object ma = MemoryPolicy.the().enterExceptionSafeArea();
                try {
                    BasicIO.out.print("DEBUG: processing activation: ");
                    BasicIO.out.println(cf.getMethod().toString()); // risky call
                    BasicIO.out.print("DEBUG: relPC is: ");
                    BasicIO.out.print(relPC);
                    // try to get line number info for the throw point
                    int lineNumber = cf.getLineNumber(relPC);
		    
                    if (lineNumber != 0) {
                        lineNumber = cf.getLineNumber(relPC);
                        BasicIO.out.print(" [line: ");
                        BasicIO.out.print(lineNumber);
                        BasicIO.out.println("]");
                    }
                    else {
                        BasicIO.out.println(" ");
                    }
                }
                finally {
                    MemoryPolicy.the().leave(ma);
                }
            }

            ExceptionHandler exh[] = cf.getExceptionHandlers(); 
	    Type declaringType = cf.getMethod().getDeclaringType(); 
	    Domain curDomain = declaringType.getDomain();

            // watch for ED exceptions trying to cross into UD.
            if (exTypeDomain.isExecutive() && !curDomain.isExecutive()) {
                if (DEBUG_THROW) {
                    Native.print_string(
                        "DEBUG: executive exception trying to escape - trying translation\n");
                }
		CoreServicesAccess csa = curDomain.getCoreServicesAccess();
                throwable = csa.translateThrowable((Throwable) throwable);

                // update the type and domain for the translated throwable
                // otherwise we'll incorrectly try to translate the new
                // object based on the old one's type. - DH
                exType = throwable.getBlueprint().getType();
                exTypeDomain = exType.getDomain();

		if (DEBUG_THROW) {
                    Object ma = MemoryPolicy.the().enterExceptionSafeArea();
                    try {
                        BasicIO.out.println("DEBUG: Performed successful translateThrowable");
                        BasicIO.out.print("DEBUG: New Exception type: ");
                        BasicIO.out.println(exType.toString());
                        BasicIO.out.print("DEBUG: New Exception domain: ");
                        BasicIO.out.println(exTypeDomain.toString());
                    }
                    finally {
                        MemoryPolicy.the().leave(ma);
                    }
                }
            }

            if (DEBUG_THROW) Native.print_string("DEBUG: looking for handler\n");
            for (int i = 0; i < exh.length; i++) {
                // use local temporaries to make debugging easier
                ExceptionHandler ex = exh[i];
                int startPC = ex.getStartPC();
                int endPC = ex.getEndPC();
                Type declaredExType;
		try {
		    // FIXME is this correct? where should the declaredExType
		    // be resolved ...
		    declaredExType = 
			declaringType.getContext().typeFor(ex.getCatchTypeName());
		} catch (LinkageException e) {
		    // we'll never get here if the the recursion check is
		    // active - DH
		    throw Executive.panicOnException(e);
		}
		
                if (DEBUG_THROW) {
                    Object ma = MemoryPolicy.the().enterExceptionSafeArea();
                    try {
                        BasicIO.out.print("DEBUG: startPC = ");
                        BasicIO.out.println(startPC);
                        BasicIO.out.print("DEBUG: endPC = ");
                        BasicIO.out.println(endPC);
                        BasicIO.out.print("DEBUG: Exception type: ");
                        BasicIO.out.println(exType.toString());
                        BasicIO.out.print("DEBUG: Exception domain: ");
                        BasicIO.out.println(exTypeDomain.toString());
                        BasicIO.out.print("DEBUG: Handler type: ");
                        BasicIO.out.println(ex.getCatchTypeName().toString());
                        BasicIO.out.print("DEBUG: Handler domain: ");
                        BasicIO.out.println(curDomain.toString());
                    }
                    finally {
                        MemoryPolicy.the().leave(ma);
                    }
                }

                if ((startPC <= relPC) && (relPC < endPC) 
		    && (exType.isSubtypeOf(declaredExType)
			|| (declaredExType == wildcardExType))) {
                    if (DEBUG_THROW)
                        Native.print_string("DEBUG: Found handler\n");
                    int newPC = ex.getHandlerPC();
                    if (DEBUG_THROW) {
                        Native.print_string("DEBUG: newPC = ");
                        Native.print_int(newPC);
                        Native.print_string("\n");
                    }

                    // if we have a wildcard handler then wrap the
                    // exception unless it is already a wildcard
		    if (declaredExType == wildcardExType &&
                        exType != wildcardExType) {
                        if (DEBUG_THROW) {
                            Native.print_string("DEBUG: converting to wildcard\n");
                        }
			throwable = ctx.makeWildcardException(throwable);
		    }


		    if (DEBUG_THROW)
			Native.print_string("DEBUG: setting PC\n");
		    act.setPC(newPC);

		    // reset before we cut to the new activation 
		    // or mess with opstack
		    ctx.flags[Context.EXCEPTION_THROW_RECURSION] = false;
		    
		    if (DEBUG_THROW)
			Native.print_string("DEBUG: setting operand stack depth\n");
		    // copy before we trash 'this'
                    // NOTE: DEBUG_THROW is sometimes static and sometimes
                    // an instance field. Copy in case it's an instance field
		    boolean LOCALDEBUG = DEBUG_THROW;
		    act.setOperandStackDepth(1); // 'this' now trashed
		    if (LOCALDEBUG)
			Native.print_string("DEBUG: setting operand\n");
		    act.setOperand(0, throwable);
		    if (LOCALDEBUG)
			Native.print_string("DEBUG: cutting to new activation\n");
		    act.cutTo();
                    //throw Executive.unreachable();
		    return;
                }
            } // end for loop
	    if (DEBUG_THROW)
		Native.print_string("DEBUG: Handler not found - unwinding\n");

	    boolean is_synch = cf.getMethod().getMode().isSynchronized();

	    // works for static and instance methods
	    if (is_synch) {
                if (DEBUG_THROW)
                    Native.print_string("DEBUG: doing monitorExit for sync method\n");
		Oop this_ptr = act.getLocal(0);
		curDomain.getCoreServicesAccess().monitorExit(this_ptr);
	    }

        }
	throw Executive.panic("no catch-all frame!!!");
    }
}
