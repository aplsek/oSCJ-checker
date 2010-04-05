package s3.services.transactions;

import ovm.core.OVMBase;
import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type.Class;
import ovm.core.execution.RuntimeExports;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.repository.TypeName.Scalar;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.CoreComponent;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.services.bytecode.SpecificationIR.ValueSource;
import ovm.util.BitSet;
import ovm.util.OVMError;
import s3.core.domain.S3Type;
import s3.core.execution.S3CoreServicesAccess;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import s3.services.bytecode.ovmify.IRewriter;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Blueprint;

/**
 * The interface to Preemptible Atomic Region support.  This interface
 * can be divided into compile-time components (which insert
 * transaction-logging code), and run-time componets (which start,
 * commit and roll back transactions).  Our compilation strategy is to
 * introduce a set of duplicate method names into the program.
 * These new methods (whose names end in {@link #TRANS_SUFFIX}) are
 * only called within transactions.  When static analysis (or
 * just-in-time compilation) discovers a call to a transactional
 * method, the bytecode rewriter expands the method's body into an
 * unshared bytecode object that contains all the required transaction
 * logging code.<p>
 *
 * {@link #initMethods} and {@link #rewriteTransactional} form the
 * core of Transaction's compile-time interface.  {@link
 * #initMethods}, unfortunately, must be called before the type for
 * {@code java.lang.Object} can be defined.  This means that
 * Transaction is needed during {@code gen-ovm} bootstrapping, and
 * must be declared as an {@link CoreComponent}.
 **/
public abstract class Transaction extends OVMBase 
    implements CoreComponent
{
    // FIXME: space causes trouble in reflection lists.  Maybe "-T"
    // would be better?
    public static final String TRANS_SUFFIX = " T";

    static final String PACKAGE_NAME = "org/ovmj/transact";
    static final int PACKAGE = RepositoryUtils.asUTF(PACKAGE_NAME);       
    public static final Scalar transaction = Scalar.make(PACKAGE, RepositoryUtils.asUTF("Transaction"));
    public static final Selector.Method Transaction_start = RepositoryUtils.
    	selectorFor(transaction, TypeName.VOID, "start", new TypeName[] {});
    public static final Selector.Method Transaction_commit = RepositoryUtils.
    	selectorFor(transaction, TypeName.VOID, "commit", new TypeName[] {});


    static final int ovm_core_execution = RepositoryUtils.asUTF("ovm/core/execution");
    static final Scalar kernel_transaction = Scalar.make(ovm_core_execution, RepositoryUtils.asUTF("RuntimeExports"));
    public static final Selector.Method kernel_start = 
	RepositoryUtils.selectorFor(kernel_transaction, TypeName.VOID, "start", new TypeName[] {});
    public static final Selector.Method kernel_commit = 
	RepositoryUtils.selectorFor(kernel_transaction, TypeName.VOID, "commit", new TypeName[] {});

    public static final Scalar NATIVE_EXCEPTION = 
	(Scalar) RepositoryUtils.makeTypeName(PACKAGE_NAME, 
					      "NativeCallException");

    public static final Scalar  ABORTED_EXCEPTION = (Scalar) RepositoryUtils.makeTypeName(PACKAGE_NAME, "AbortedException");     
    static final Scalar ATOMIC = (Scalar) RepositoryUtils.makeTypeName(PACKAGE_NAME, "Atomic");
    
     public static final Scalar s3transaction = Scalar.make(PACKAGE,RepositoryUtils.asUTF("S3Transaction"));
     public static final Selector.Method S3Transaction_doLog = RepositoryUtils.selectorFor(s3transaction, TypeName.VOID, "doLog", new TypeName[] {});

     static final String ED_PACKAGE_NAME = "s3/services/transactions";
     public static final Scalar ED_ABORTED_EXCEPTION = (Scalar) RepositoryUtils.makeTypeName(ED_PACKAGE_NAME, "EDAbortedException");
     
     static final boolean DEBUG = true;
     
     /**
      * Method used to stich the singleton Transaction object. The config/s3common
      * file tells us which one of S3Transaction or EmptyTransaction it will be. 
      */
     static public Transaction the() throws PragmaStitchSingleton {
 	return (Transaction) InvisibleStitcher.singletonFor(Transaction.class.getName());
     }
    public Transaction() {
	RuntimeExports.defineVMProperty("org.ovmj.supportsTransactions",
					transactionalMode());
    }


    // RUNTIME METHODS
    public void  boot() {};
    public boolean inTransaction() { return false; }    
    public abstract void par_log(VM_Address object, int offset);   
    public abstract void par_logw(VM_Address object, int offset);    
    public abstract void par_log_arr(Oop arr, int idx);    
    public abstract void par_log_arrw(Oop arr, int idx);    
    public void par_read() {}
    public abstract void undo();    
    public abstract void retry();
    public abstract void start(int size, boolean commit, S3CoreServicesAccess csa);    
    public abstract void start(S3CoreServicesAccess csa);
    public abstract void commit();    
    public int logSize() { return -1; }
    public void abort() {}
    public void postRunThreadHook(boolean aborting) {};    
    public boolean preRunThreadHook(OVMThread currentThread, OVMThread nextThread) { return false;}
    public boolean PARenabled() { return false; }
    public Oop getUDException() { return null; }

    public EDAbortedException getEDA() throws EDAbortedException { return null; }

    public void dumpStats() { }

    public static void static_commit() throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { 
	Transaction.the().commit(); 
    }
    public static void static_start(S3CoreServicesAccess csa) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { 
	Transaction.the().start(csa); 
    }

    
    // BUILD TIME METHODS
    public void setExceptionConstructors(Domain _) {}
    public void throwNativeCallException(String _) {}

    // public void setAbortedExceptionConstructor(ReflectiveConstructor _) {}    
    public abstract Method selectReflectiveMethod(Method method);    
    public void setReflectiveCalls(BitSet[] reflectiveCalls, Domain d, Method[] meth, int i) {}    
    public UnboundSelector.Method translateTransactionalSelector(UnboundSelector.Method usel) { return usel;  }    
    public boolean isTransMethod(Method m) { return false; }    
    public boolean transactionalMode() { return false; }    
    public RepositoryMember.Method[] initMethods(S3Type t, RepositoryMember.Method[] r, RepositoryMember.Method[] c) {
	throw new OVMError.Configuration("Unimplemented");     
    }
    public boolean isAtomic(Method method) {
	int cnt = method.getThrownTypeCount();
	for (int i = 0; i < cnt; i++) 
	    try {
		Class exception = method.getThrownType(i);
		if (exception.getName().equals(ATOMIC)) return true;
	    } catch (LinkageException e) { throw new OVMError.Internal(e); }	
         return false;
    }

    public boolean ignoreLogCall(ValueSource _exp) { return false;  }

    public boolean gatherStatistics() {return false; }

    public  boolean rewriteTransactional(int x, IRewriter ir, Method method, 
					 S3ByteCode.Builder builder, S3Blueprint curBp,
					 int phase) {
	throw new Error("unsupported");
    }
}
