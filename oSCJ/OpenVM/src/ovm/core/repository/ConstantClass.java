package ovm.core.repository;

import ovm.core.repository.TypeName;
/**
 * Represent an object that is stored in a constant pool entry with
 * tag CONSTANT_Class.
 */
public interface ConstantClass extends Constant {
    TypeName asTypeName();
    /**
     * The class name as an OvmFormat string suitable for methods that
     * expect one.
     **/
    String asTypeNameString();
}
