package ovm.services.bytecode.analysis;

import ovm.core.OVMBase;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.services.bytecode.Instruction;
import org.ovmj.util.Runabout;

/**
 * This is a generic Driver for the Bytecode Analysis Framework.
 * The Driver can run any type of bytecode Analysis on methods
 * or classes, provided a VerifierFactory. The VerifierFactory 
 * specifies completely which type of analysis to run.<p>
 * 
 * Example VerifierFactorys are bytecode verification (the s3 verifier)
 * or confined types inference (Kacheck).<p>
 *
 * @see s3.services.bytecode.verifier.VerificationValueFactory
 * @see s3.services.bytecode.verifier.VerificationInterpreter
 * @author Christian Grothoff
 **/
public final class Driver extends OVMBase {

    private final Factory conf_;

    /**
     * Create a driver to run the analysis specified by
     * the given VerifierFactory.
     **/
    public Driver(Factory conf) {
        this.conf_ = conf;
    }

    public static void run(Runabout[] visitors,
			   Instruction.Iterator it) {
        switch (visitors.length) {
            case 0 :
                break;
            case 1 :
                run1(visitors[0], it);
                break;
            case 2 :
                run1(visitors[0], visitors[1], it);
                break;
            default :
                run1(visitors, it);
                break;
        }
    }

    static int rc = 0;

    /**
     * Run the analysis specified in the configuration on
     * the given method.
     * @param me the method
     **/
    public void run(RepositoryMember.Method me) {
        VisitorsAndIterator vai =
            conf_.getVisitorsAndIterator(me);
        run(vai.getVisitors(), vai.getIterator());
    }

    /**
     * Run the analysis specified in the VerifierFactory on every
     * method in the given class.
     **/
    public void run(RepositoryClass rc_) {
        RepositoryMember.Method[] methods = rc_.getStaticMethods();
        for (int i = 0; i < methods.length; i++)
            run(methods[i]);
        methods = rc_.getInstanceMethods();
        for (int i = 0; i < methods.length; i++)
            run(methods[i]);
    }


    /* ***************** helper methods ******************* */

    /**
     * Specialized run method for only one visitor.
     **/
    static private void run1(Runabout r, Instruction.Iterator it) {
        for (Instruction i = it.next(); i != null; i = it.next()) {
            r.visitAppropriate(i);
        }
    }

    /**
     * Specialized run method for only two visitors.
     **/
    static private void run1(
        Runabout r1,
        Runabout r2,
        Instruction.Iterator it) {
        for (Instruction i = it.next(); i != null; i = it.next()) {
	    // System.err.println("X: " + i.toString());
            r1.visitAppropriate(i);
            r2.visitAppropriate(i);
        }
    }

    /**
     * Run method for any number of visitors.
     **/
    static private void run1(Runabout[] r, 
			     Instruction.Iterator it) {
        for (Instruction i = it.next(); i != null; i = it.next()) {
            for (int j = 0; j < r.length; j++)
                r[j].visitAppropriate(i);
        }
    }


    /**
     * @author Christian Grothoff
     **/
    public interface Factory {
	
	public VisitorsAndIterator 
	    getVisitorsAndIterator(RepositoryMember.Method me);

    }
    	
    public static class VisitorsAndIterator {
	
	private Runabout[] visitors;
	private Instruction.Iterator iterator;

        public VisitorsAndIterator(Runabout[] vis,
				   Instruction.Iterator it) {
	    this.visitors = vis;
	    this.iterator = it;
	}
	
	public Instruction.Iterator getIterator() {
	    return iterator;
	}
	public Runabout[] getVisitors() {
	    return visitors;
	}
	
    } // end of VisitorsAndIterator
    
} // end of Driver
