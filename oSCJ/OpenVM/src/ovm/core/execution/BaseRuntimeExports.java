package ovm.core.execution;

import ovm.core.OVMBase;
import ovm.core.domain.JavaDomain;

/**
 * This class exists to define an order on field initialization.
 **/
abstract class BaseRuntimeExports extends OVMBase {
    protected JavaDomain dom;
    protected CoreServicesAccess csa;

    /**
     * Because this constructor executes before field initializers in
     * the concrete RuntimeExports implementation, field initializer
     * can freely refer to dom and csa.
     **/
    BaseRuntimeExports(JavaDomain dom) {
	this.dom = dom;
	csa = dom.getCoreServicesAccess();
    }
}
    
