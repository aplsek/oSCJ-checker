package ovm.core.repository;

import ovm.core.repository.Mode;
import ovm.core.OVMBase;
import ovm.core.repository.TypeName;

/**
 * Interface for a visitor pattern defined over the repository
 * data structures. 
 * <p>
 * Design note: Since at this stage the hierarchy of OVM classes is
 * flat, using visitors is not strictly necessary but it allows for a
 * painless extension path.
 *
 * @author Christian Grothoff
 * 
 * NB: christian disliked the original name so: Visitor became Processor
 **/
public class RepositoryProcessor extends OVMBase implements Mode.ModeVisitor {
    /**
     * Visits a third-party attribute object for a Class
     * @param x the third-party class attribute object to be visited
     **/
    public void visitAttrThirdParty(Attribute.ThirdParty.Class x) {}

    /**
     * Visits a third-party attribute object for a Field
     * @param x the third-party field attribute object to be visited
     **/
    public void visitAttrThirdParty(Attribute.ThirdParty.Field x) {}

    /**
     * Visits a third-party attribute object for a Method
     * @param x the third-party method attribute object to be visited
     **/
    public void visitAttrThirdParty(Attribute.ThirdParty.Method x) {}

    /**
     * Visits a third-party attribute object for a ByteCodeFragment
     * @param x the third-party bytecode fragment attribute object to be visited
     **/
    public void visitAttrThirdParty(Attribute.ThirdParty.ByteCodeFragment x) {}

    /**
     * Visits a <code>SourceFile</code> attribute object
     * @param x the source file object to be visited
     **/
    public void visitAttrSourceFile(Attribute.SourceFile x) {}

    
    /**
     * Visits a <code>Synthetic</code> attribute object for a Class
     * @param x the synthetic class attribute object to be visited
     **/
    public void visitAttrSynthetic(Attribute.Synthetic.Class x) {}

    /**
     * Visits a <code>Synthetic</code> attribute object for a Field
     * @param x the synthetic field attribute object to be visited
     **/
    public void visitAttrSynthetic(Attribute.Synthetic.Field x) {}

    /**
     * Visits a <code>Synthetic</code> attribute object for a Method
     * @param x the synthetic method attribute object to be visited
     **/
    public void visitAttrSynthetic(Attribute.Synthetic.Method x) {}

    /**
     * Visits a <code>Synthetic</code> attribute object for a Method
     * @param x the synthetic method attribute object to be visited
     **/
    public void visitAttrSynthetic(Attribute.Synthetic.Bytecode x) {}

    /**
     * Visits a <code>LocalVariableTable</code> attribute object
     * @param x the local variable table attribute object to be visited
     **/
    public void visitAttrLocalVariableTable(Attribute.LocalVariableTable x) {}

    /**
     * Visits a <code>LineNumberTable</code> attribute object
     * @param x the line number table attribute object to be visited
     **/
    public void visitAttrLineNumberTable(Attribute.LineNumberTable x) {}

    /**
     * Visits a <code>Deprecated</code> attribute object for a Class
     * @param x the deprecated class attribute object to be visited
     **/
    public void visitAttrDeprecated(Attribute.Deprecated.Class x) {}
    
	/**
	 * Visits a <code>Deprecated</code> attribute object for a Field
	 * @param x the deprecated field attribute object to be visited
	 **/
    public void visitAttrDeprecated(Attribute.Deprecated.Field x) {}
    
	/**
	 * Visits a <code>Deprecated</code> attribute object for a Method
	 * @param x the deprecated method attribute object to be visited
	 **/
    public void visitAttrDeprecated(Attribute.Deprecated.Method x) {}
    

    /**
     * Visits an <code>InnerClasses</code> attribute object
     * @param x the inner classes attribute object to be visited
     **/
    public void visitAttrInnerClasses(Attribute.InnerClasses x) {}

    /**
     * Visits a class name object
     * @param x the class name object to be visited
     **/
    public void visitClassName(TypeName.Scalar x) {}

    /**
     * Visits a super class name object
     * @param x the super class name object to be visited
     **/
    public void visitSuperName(TypeName.Scalar x) {}

    /**
     * Visits an outer-class name object
     * @param x the outer-class name object to be visited
     **/
    public void visitOuterName(TypeName.Scalar x) {}

    /**
     * Visits the major and minor versions of a class file
     * @param minor the minor version number
     * @param major the major version number
     **/
    public void visitVersions(int minor, int major) {}

    /**
     * Visits the type name of the interface specified
     * @param x the typename of the interface to be visited
     **/
    public void visitInterface(TypeName.Scalar x) {}

    /**
     * Visits a class's modifiers object
     * @param x the class modifiers object to be visited
     **/
    public void visitClassMode(Mode.Class x) {}

    /**
     * Visits a field's modifiers object
     * @param x the field modifiers object to be visited
     **/
    public void visitFieldMode(Mode.Field x) {}

    /**
     * Visits a method's modifiers object
     * @param x the method modifiers object to be visited
     **/
    public void visitMethodMode(Mode.Method x) {}

    /**
     * Visits a repository class object
     * @param x the repository class object to be visited
     **/
    public void visitClass(RepositoryClass x) {}

    /**
     * Visits the typename of the static inner class object specified
     * @param x the typename of the static inner class to be visited
     **/
    public void visitStaticInnerClass(TypeName.Scalar x) {}

    /**
     * Visits the typename of the instance inner class specified
     * Visits an instance inner class object specified by its typename
     * @param x the typename of the instance inner class to be visited
     **/
    public void visitInstanceInnerClass(TypeName.Scalar x) {}

    /**
     * Visits a field object
     * @param x the field object to be visited
     **/
    public void visitField(RepositoryMember.Field x) {}

    /**
     * Visits a method object
     * @param x the method object to be visited
     **/
    public void visitMethod(RepositoryMember.Method x) {}

    /**
     * Visits a <code>Bytecode</code> object
     * @param x the bytecode fragment object to be visited
     **/
    public void visitByteCodeFragment(Bytecode x) {}


    /**
     * Visits one of a method's <code>throws</code> declarations by name
     * @param x the name of the thrown exception to be visited
     **/
    public void visitThrowsDeclaration(TypeName.Scalar x) {}

    /**
     * Visits a <code>ExceptionHandler</code> object
     * @param x the exception object to be visited
     **/
    public void visitException(ExceptionHandler x) {}

}
