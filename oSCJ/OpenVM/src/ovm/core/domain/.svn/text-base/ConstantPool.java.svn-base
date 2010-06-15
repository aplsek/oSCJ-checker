package ovm.core.domain;
import ovm.core.repository.Constants;
import ovm.core.repository.ConstantsEditor;

/**
 * Unlike repository constant pools, constant pools tied to a
 * particular {@link Domain} and {@link Type.Context} are mutable.
 * Entries are added as the code is translated from standard java to
 * the Ovm IR, and entries are updated during execution.
 **/
public interface ConstantPool extends Constants, ConstantsEditor {
    boolean isStaticMethodResolved(int index);
    boolean isInstanceMethodResolved(int index);
    boolean isInterfaceMethodResolved(int index);
    boolean isStaticFieldResolved(int index);
    boolean isInstanceFieldResolved(int index);
    boolean isConstantResolved(int index);
    boolean isClassResolved(int index);
    /**
     * Resolve a constant pool CONSTANT_Methodref referred to by an
     * INVOKESTATIC.
     */
    ConstantResolvedStaticMethodref resolveStaticMethod(int index) throws LinkageException;
    /**
     * Resolve a constant pool CONSTANT_Methodref referred to by an
     * INVOKESPECIAL or INVOKEVIRTUAL.
     */
    ConstantResolvedInstanceMethodref resolveInstanceMethod(int index) throws LinkageException;
    ConstantResolvedInterfaceMethodref resolveInterfaceMethod(int index) throws LinkageException;
    /**
     * This method will resolve the interface method and add it to a
     * new constant pool slot.  (This method is used to replace
     * virtual calls with interface calls when a virtual base method
     * is does not appear in the bytecode (ie, miranda methods).)
     **/
    ConstantResolvedInterfaceMethodref resolveAndAddInterfaceMethod(int index) throws LinkageException;
    /**
     * Resolve a constant pool CONSTANT_Fieldref referred to by an
     * GETSTATIC or PUTSTATIC.
     */
    ConstantResolvedStaticFieldref resolveStaticField(int index) throws LinkageException;
    /**
     * Resolve a constant pool CONSTANT_Fieldref referred to by an
     * GETFIELD or PUTFIELD.
     */
    ConstantResolvedInstanceFieldref resolveInstanceField(int index) throws LinkageException;
    /**
     * Resolve a constant pool CONSTANT_Class entry (holding a
     * TypeName) and overwrite the entry with a Blueprint. This is
     * true of Gemeinsam TypeNames and Shared-state Blueprints,
     * too. -HY
     */
    Blueprint resolveClassAt(int index) throws LinkageException;
    /**
     * Resolve the object at the given index and return the corresponding
     * revolved instance.  This method is used to resolve java-level
     * objects (String), as well as internal objects (shared-states).
     * <p>
     * In both cases, it returns an object in the domain of the
     * containing class, rather than an executive-domain meta-object.
     */
    Oop resolveConstantAt(int index);
}
