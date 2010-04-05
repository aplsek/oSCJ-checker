package s3.services.transactions;

import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.services.memory.VM_Address;
import s3.core.execution.S3CoreServicesAccess;

public class EmptyTransaction extends Transaction {
    public void logWord(Oop src, VM_Address ptr) {}
    public void par_log(VM_Address object, int offset) {}
    public void par_logw(VM_Address object, int offset) {}
    public void par_log_arr(Oop arr, int idx) {}
    public void par_log_arrw(Oop arr, int idx) {}
    public void undo() {}
    public void start(int size, boolean commit, S3CoreServicesAccess csa) {}
    public void start(S3CoreServicesAccess csa) {}
    public void commit() {}
    public void retry() {}
    public Method selectReflectiveMethod(Method method) {return method; }
    public boolean isAtomic(Method method) { return false; }

    public void initialize() { }
}
