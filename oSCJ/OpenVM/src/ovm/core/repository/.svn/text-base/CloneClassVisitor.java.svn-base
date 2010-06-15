/**
 * @file ovm/core/repository/CloneClassVisitor.java
 **/
package ovm.core.repository;

import ovm.core.repository.Mode;
import ovm.core.repository.TypeName;
import ovm.util.ByteBuffer;
import ovm.core.domain.LinkageException;

/**
 * Visitor that walks over a class and copies it into a ClassBuilder.
 * As all repository objects are read-only, this form of a deep-copy
 * is particulary useful.  This can be used as a template for visitor
 * that performs actual modifications.<p>
 *
 * Class visitation begins by calling <code>visitClass</code>.  <p>
 *
 * To do:<br>
 * The code currently does not handle ThirdParty attributes correctly.
 *
 * @author Christian Grothoff
 **/
public class CloneClassVisitor extends RepositoryProcessor {

    /**
     * Bytecode fragment of the method currently being visited
     * (if we're visiting a method)
     **/
    protected Bytecode lastByteCodeFragment;

    /**
     * Builder used to create a bytecode fragment (when this visitor
     * is sent to a bytecode fragment)
     **/
    protected Bytecode.Builder rByteCodeFragment;

    /**
     * The builder object for the class to be cloned.
     **/
    protected RepositoryClass.Builder rClassBuilder;

    /**
     * Builder used to create a constant pool for the class (when
     * this visitor is sent to a constant pool)
     **/
    protected ConstantPoolBuilder rConstantPoolBuilder;

    /**
     * The factory object which will be used to create the builder
     * which will be used to clone the class to be visited.
     **/
    protected final ovm.core.repository.Services repositoryFactory;

    /**
     * Builder used to create a field (when this visitor is sent to
     * a field)
     **/
    protected RepositoryMember.Field.Builder rFieldInfoBuilder;

    /**
     * Builder used to create a method (when this visitor is sent to
     * a method)
     **/
    protected RepositoryMember.Method.Builder rMethodInfoBuilder;

    /**
     * The name of the currently visited class.
     **/
    protected TypeName.Scalar typeName;

    /**
     * Constructor. Initializes the factory which will be used to
     * create the class builder for this class.
     **/
    public CloneClassVisitor() {
        repositoryFactory = Services.getServices();
    }

    /**
     * RepositoryClass builder to be called when visiting is complete.
     * @return the cloned repository class
     **/
    public RepositoryClass commit() {
        return rClassBuilder.build();
    }

    public final void commitByteCodeFragment(Bytecode x) {
        rByteCodeFragment = repositoryFactory.getByteCodeFragmentBuilder();
        rByteCodeFragment.setCode(ByteBuffer.wrap(x.getCode()));
        rByteCodeFragment.setConstantPool(x.getConstantPool());
        rByteCodeFragment.declareTemporaries(x.getMaxStack(), x.getMaxLocals());
        rByteCodeFragment.declareSelector(
            rClassBuilder.getName(),
            rMethodInfoBuilder.getName(),
            rMethodInfoBuilder.getDescriptor());
        x.visitComponents(this);
        lastByteCodeFragment = rByteCodeFragment.build();
        rMethodInfoBuilder.addFragment(lastByteCodeFragment);
    }

    /**
     * Visits a <code>Deprecated</code> attribute and declare the
     * attribute in the class builder
     * @param x the <code>Deprecated</code> attribute to be declared
     *          in the class builder
     **/
    public void visitAttrDeprecated(Attribute.Deprecated.Class x) {
        rClassBuilder.declareDeprecated();
    }

    /**
     * Visits a <code>Deprecated</code> attribute and declare the
     * attribute in the field builder
     * @param x the <code>Deprecated</code> attribute to be declared
     *          in the field builder
     **/
    public void visitAttrDeprecated(Attribute.Deprecated.Field x) {
        rFieldInfoBuilder.declareDeprecated();
    }

    /**
     * Visits a <code>Deprecated</code> attribute and declare the
     * attribute in the method builder
     * @param x the <code>Deprecated</code> attribute to be declared
     *          in the method builder
     **/
    public void visitAttrDeprecated(Attribute.Deprecated.Method x) {
        rMethodInfoBuilder.declareDeprecated();
    }

    /**
     * Visits an <code>InnerClasses</code> attribute and declare the
     * attribute in the class builder
     * @param x the <code>InnerClasses</code> attribute to be declared
     *          in the class builder
     **/
    public void visitAttrInnerClasses(Attribute.InnerClasses x) {
        rClassBuilder.declareAttribute(x);
    }

    /**
     * Visits a <code>LineNumberTable</code> attribute and declare the
     * attribute in the class builder
     * @param x the <code>LineNumberTable</code> attribute to be declared
     *          in the class builder
     **/
    public void visitAttrLineNumberTable(Attribute.LineNumberTable x) {
        rByteCodeFragment.declareAttribute(x);
    }

    /**
     * Visits a <code>LocalVariableTable</code> attribute and declare the
     * attribute in the class builder
     * @param x the <code>LocalVariableTable</code> attribute to be declared
     *          in the class builder
     **/
    public void visitAttrLocalVariableTable(Attribute.LocalVariableTable x) {
        rByteCodeFragment.declareAttribute(x);
    }

    /**
     * Visits a <code>SourceFile</code> attribute and declare the
     * attribute in the class builder
     * @param x the <code>SourceFile</code> attribute to be declared
     *          in the class builder
     **/
    public void visitAttrSourceFile(Attribute.SourceFile x) {
        rClassBuilder.declareAttribute(x);
    }

    /**
     * Visits a <code>Synthetic</code> attribute and declare the
     * attribute in the class builder
     * @param x the <code>Synthetic</code> attribute to be declared
     *          in the class builder
     **/
    public void visitAttrSynthetic(Attribute.Synthetic.Class x) {
        rClassBuilder.declareSynthetic();
    }

    /**
     * Visits a <code>Synthetic</code> attribute and declare the
     * attribute in the Field builder
     * @param x the <code>Synthetic</code> attribute to be declared
     *          in the field builder
     **/
    public void visitAttrSynthetic(Attribute.Synthetic.Field x) {
        rFieldInfoBuilder.declareSynthetic();
    }

    /**
     * Visits a <code>Synthetic</code> attribute and declare the
     * attribute in the method builder
     * @param x the <code>Synthetic</code> attribute to be declared
     *          in the method builder
     **/
    public void visitAttrSynthetic(Attribute.Synthetic.Method x) {
        rMethodInfoBuilder.declareSynthetic();
    }

    /**
     * Visits a third-party attribute and declare the attribute in
     * the bytecode fragment builder
     * @param x the third-party attribute to be declared in the bytecode fragment
     *          builder
     **/
    public void visitAttrThirdParty(Attribute.ThirdParty.ByteCodeFragment x) {
        rByteCodeFragment.declareAttribute(x);
    }

    /* ******************* and now: visit methods ****************** */

    /**
     * Visits a third-party attribute and declare the attribute in
     * the class builder
     * @param x the third-party attribute to be declared in the class
     *          builder
     **/
    public void visitAttrThirdParty(Attribute.ThirdParty.Class x) {
        rClassBuilder.declareAttribute(x);
    }

    /**
     * Visits a third-party attribute and declare the attribute in
     * the field builder
     * @param x the third-party attribute to be declared in the field
     *          builder
     **/
    public void visitAttrThirdParty(Attribute.ThirdParty.Field x) {
        rFieldInfoBuilder.declareAttribute(x);
    }

    /**
     * Visits a third-party attribute and declare the attribute in
     * the method builder
     * @param x the third-party attribute to be declared in the method
     *          builder
     **/
    public void visitAttrThirdParty(Attribute.ThirdParty.Method x) {
	if (rMethodInfoBuilder != null)
	    rMethodInfoBuilder.declareAttribute(x);
	else
	    rClassBuilder.declareAttribute(x);
    }

    /**
     * Visits a bytecode fragment from the current method being visited and
     * adds a cloned copy of this fragment to the method builder that will
     * be used to declare this method in the class builder.
     * @param x the bytecode fragment to be added
     **/
    public void visitByteCodeFragment(Bytecode x) {
        commitByteCodeFragment(x);
    }

    /**
     * The visit method that should be called FIRST; begins visitation
     * of the entire class to be cloned. When this exits, the class
     * builder to be used to clone this class should be built and ready
     * to clone classes.
     * @param x the RepositoryClass of the class to be cloned
     **/
    public void visitClass(RepositoryClass x) {
        this.typeName = x.getName();
        rClassBuilder = repositoryFactory.getClassBuilder();
        rConstantPoolBuilder = (ConstantPoolBuilder) x.getConstantPool().getBuilder();
        x.visitAttributes(this);
        x.visitHeader(this);
        x.visitMembers(this);
        rClassBuilder.setSize(x.getSize());
        rClassBuilder.declareConstantPool(rConstantPoolBuilder.build());
    }

    /**
     * Visits the modifiers object of the class to be cloned and
     * declare this in the class builder
     * @param x the cloned class's modifiers to be declared in the
     *          class builder
     **/
    public void visitClassMode(Mode.Class x) {
        rClassBuilder.setAccessMode(x);
    }

    /**
     * Visits the cloned class's type name and set this name in the class
     * builder
     * @param x the type name of the class to be set in the class builder
     **/
    public void visitClassName(TypeName.Scalar x) {
        rClassBuilder.setName(x);
        rConstantPoolBuilder.addClass(x);
    }

    /**
     * Visits a code fragment from the current method being visited and
     * adds a cloned copy of this fragment to the method builder that will be
     * used to declare this method in the class builder.
     * @param x the code fragment to be added
     **/
    public void visitCodeFragment(Bytecode x) {
        throw new UnsupportedOperationException("FIXME");
        //  rMethodInfoBuilder.addFragment(x);
    }

    /**
     * Visit an entry in an exception table and declare this in the
     * current bytecode fragment of the current method being visited.
     * @param x exception table entry to be visited
     **/
    public void visitException(ExceptionHandler x) {
        rByteCodeFragment.declareExceptionHandler(x);
    }

    /**
     * Visits a field in the class to be cloned and declare this field
     * in the class builder.
     * @param x the cloned field to be declared in the class builder
     **/
    public void visitField(RepositoryMember.Field x) {
        rFieldInfoBuilder = repositoryFactory.getFieldBuilder();
	if (x.hasConstantValue())
	    rFieldInfoBuilder.setConstantValueBits(x.getConstantValueBits());
	rFieldInfoBuilder.setTypeName(typeName);
        rFieldInfoBuilder.setName(x.getUnboundSelector().getNameIndex());
        rFieldInfoBuilder.setDescriptor(x.getDescriptor());
        x.visitComponents(this);
        rClassBuilder.declareField(rFieldInfoBuilder.build());
    }

    /**
     * Visits the currently visited field's modifiers and set
     * these in the field builder that will be used to build this field
     * for the class builder
     * @param x the currently visited field's modifiers object
     **/
    public void visitFieldMode(Mode.Field x) {
        rFieldInfoBuilder.setMode(x);
    }


    /**
     * Visits an instance inner class name of the cloned class and declare
     * this inner class in the class builder
     * @param x the type name of the instance inner class to be declared
     **/
    public void visitInstanceInnerClass(TypeName.Scalar x) {
        rClassBuilder.declareInstanceInnerClass(x);
    }

    /**
     * Visits an interface name implemented by the class to be cloned and
     * declare this in the class builder
     * @param x the type name of the implemented interface to be declared
     **/
    public void visitInterface(TypeName.Scalar x) {
        rClassBuilder.declareInterface(x);
    }

    /**
     * Visits a method in the class to be cloned and declare this method
     * in the class builder.
     * @param x the cloned method to be declared in the class builder
     **/
    public void visitMethod(RepositoryMember.Method x) {
        rMethodInfoBuilder = repositoryFactory.getMethodBuilder();
	rMethodInfoBuilder.setTypeName(typeName);
	rMethodInfoBuilder.setName(x.getUnboundSelector().getNameIndex());
	rMethodInfoBuilder.setDescriptor(x.getDescriptor());
        x.visitComponents(this);
	try {
	    rClassBuilder.declareMethod(rMethodInfoBuilder.build());
	} catch (LinkageException e) {
	    // impossible!
	    throw e.unchecked();
	}
    }

    /**
     * Visits the currently visited method's modifiers and sets
     * these in the method builder that will be used to build this method
     * for the class builder
     * @param x the currently visited methods's modifiers object
     **/
    public void visitMethodMode(Mode.Method x) {
        rMethodInfoBuilder.setMode(x);
    }

    /**
     * Visits the outer class name of the class to be cloned and set this name
     * in the class builder
     * @param x the type name of the outer class to be set in the class
     *          builder
     **/
    public void visitOuterName(TypeName.Scalar x) {
	try {
	    rClassBuilder.setOuterClassName(x);
	} catch (LinkageException e) {
	    // impossible!
	    throw e.unchecked();
	}
    }

    /**
     * Visit a static inner class name of the cloned class and declare
     * this inner class in the class builder
     * @param x the type name of the static inner class to be declared
     **/
    public void visitStaticInnerClass(TypeName.Scalar x) {
        rClassBuilder.declareStaticInnerClass(x);
    }

    /**
     * Visits the cloned class's superclass name and set this name in the
     * class builder
     * @param x the type name of the superclass to be set in the class builder
     **/
    public void visitSuperName(TypeName.Scalar x) {
        rClassBuilder.setSuperClassName(x);
        if (x != null)
            rConstantPoolBuilder.addClass(x);
    }

    /**
     * Visits the type name of an exception that is declared to be thrown
     * by the current method being visited and declare this exception
     * in the method builder that will be used to declare this method
     * in the class builder.
     * @param x the type name of a declared exception thrown by the
     *          currently visited method
     **/
    public void visitThrowsDeclaration(TypeName.Scalar x) {
        rMethodInfoBuilder.declareThrownException(x);
    }

    /**
     * Visits the major and minor version of the class file corresponding
     * to the class to be cloned
     * @param minor the minor version number
     * @param major the major version number
     **/
    public void visitVersions(int minor, int major) {
        rClassBuilder.setMajorVersion(major);
        rClassBuilder.setMinorVersion(minor);
    }

} // end of CloneClassVisitor
