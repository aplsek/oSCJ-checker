package ovm.core.repository;

import ovm.core.repository.Selector;

/**
 * Represent an object that is stored in a constant pool entry with
 * tag CONSTANT_Methodref.
 */
public interface ConstantMethodref extends Constant {
    Selector.Method asSelector();
}
