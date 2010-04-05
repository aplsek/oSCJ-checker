/**
 * OVM/S3Verifier - main class.
 * @file s3/services/bytecode/verifier/S3Verifier.java 
 **/
package s3.services.bytecode.verifier;

import ovm.core.repository.Mode;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.Driver;
import ovm.services.bytecode.verifier.Verifier;
import ovm.util.CommandLine;
import org.ovmj.util.Runabout;
import s3.services.bytecode.S3ClassProcessor;

/**
 * This class contains the S3/OVM bytecode verifier. It uses abstract interpretation 
 * to check type-safety and to generate constraints. The Verifier should internally 
 * keep track of verification constraints and ensure consistency. <p>
 * 
 * The current implementation does not generate any constraints
 * and does only a rudimentary version of type-checking.<p>
 *
 * @author Christian Grothoff
 **/
public final class S3Verifier implements Verifier {

    /**
     * The Driver used to run this Verifier.
     **/
    private final Driver driver;

    /**
     * Create a new Verifier.
     **/
    public S3Verifier() {
        this.driver = new Driver(new FactoryImpl());
    }

    /**
     * Verify the given class.
     * @param rc the class to verify
     * @throws Error in case of errors
     **/
    public final void verify(RepositoryClass rc) {
        driver.run(rc);
    }

    /**
     * This is the configuration for the S3Verifier. It glues together
     * the components of the verifier and hands them to the generic
     * abstract-execution driver. <p>
     *
     * The components are an AbstractInterpreter, an Iterator over all
     * possible states and a visitor that checks additional verification
     * constraints (this visitor is currently not implemented).<p>
     *
     * This Configuration is tied to the S3AbstractInterpreter and
     * can not be used with other visitors for abstract execution.
     * If you need a different abstract interpreter, define another
     * Configuration class!<p>
     *
     * @see s3.services.bytecode.analysis.FixpointIterator
     * @see s3.services.bytecode.analysis.S3AbstractInterpreter
     * @see ovm.util.Runabout
     * @author Christian Grothoff
     **/
    public static final class FactoryImpl 
	implements Driver.Factory {

        /**
         * Singleton for the AbstractValue.Factory (the factory
         * can recycle immutable AbstractValues).
         **/
        private final AbstractValue.Factory avf  = makeAbstractValueFactory();

        /**
         * For a given method, which set of visitors and which
         * fixpoint iteration should be used for the
         * abstract execution?
         **/
        public final Driver.VisitorsAndIterator getVisitorsAndIterator(
            RepositoryMember.Method me) {
            Mode.Method mo = me.getMode();
            if (mo.isNative() || mo.isAbstract()) {
                // for abstract or native methods, we don't do ANYTHING!
                return new Driver.VisitorsAndIterator(
                    new Runabout[0],
                    new Instruction.Iterator() {
                    public Instruction next() {
                        return null;
                    }
                });
            }
            VerificationInterpreter ai =
		new VerificationInterpreter(me, avf);
            VerificationFixpointIterator it =
                new VerificationFixpointIterator(avf, ai, me);
            Runabout[] visitors = new Runabout[] { ai, it.getPostVisitor()};
            /* System.out.println("CONFIGURATION: " + visitors[0] + 
            	       "," + visitors[1] + "," + 
            	       it);*/
            return new Driver.VisitorsAndIterator(visitors, it);
        }

        /* *************** internal mini-factories to ease extening ********** */

        /**
         * Create the AbstractValue.Factory. This factory is used
         * by the AbstractInterpreter to create AbstractValues. 
         *
         * @return the Factory, never null
         **/
        private AbstractValue.Factory makeAbstractValueFactory() {
            return new VerificationValueFactory();
        }

    } // end of VerifierConfiguration

    /**
     * Factory to create a Verifier.
     * @author Christian Grothoff
     **/
    public static final class Factory
	implements Verifier.Factory {

        public Verifier makeVerifier() {
            return new S3Verifier();
        }

    } // end of S3Verifier.Factory

    /* ******************* code for the standalone-verifier ************** */

    /**
     * The main method.
     **/
    public static void main(String[] args) throws Exception {
        CommandLine commandLine = new CommandLine(args);  	
   
        System.out.println(
            "OVM Verifier 1.9.9 (C) 2000, 2001, 2002 S3 Lab, Purdue University\n");
        if (commandLine.getBoolean("help")) {
            printHelp();
            return;
        }
        final S3Verifier verifier = new S3Verifier();
        S3ClassProcessor proc = new S3ClassProcessor() {
            public void process(RepositoryClass clz) {
                verifier.verify(clz);
            }
        };
	proc.runOnCommandLineArguments(commandLine);
    }

    private static void printHelp() {
        System.out.println("Usage: ovm-verify [options] JARS");
        System.out.println("Options:");
        System.out.println("-help         : print this help screen");
    }

} // end of S3Verifier
