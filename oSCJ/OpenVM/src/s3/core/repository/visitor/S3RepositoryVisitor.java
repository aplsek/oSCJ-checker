/**
 * @author Michel Pawlak
 * @author Chrislain Razafimahefa
 **/
package s3.core.repository.visitor;

import ovm.core.repository.Mode;
import ovm.core.repository.TypeName;
import ovm.core.repository.Attribute;
import ovm.core.repository.Bytecode;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.RepositoryProcessor;
import ovm.util.Debug;

public class S3RepositoryVisitor 
    extends RepositoryProcessor {

    private Debug debug;

    public S3RepositoryVisitor() {
	debug = new Debug();
    }

    /**
     * The output of the visitor will be stored in a string.
     **/
    public void bufferizeOutput(boolean flag) {
	debug.bufferizeOutput(flag);
    }

    /**
     * Retrieve the output of the visitor.
     **/
    public String getResults() {
	return debug.getResults();
    }

    public void visitClassMode(Mode.Class x) {
	debug.print(formatMode(x.toString()));
    }
    public void visitFieldMode(Mode.Field x) {
	debug.print(formatMode(x.toString()));
    }
    
    public void visitMethodMode(Mode.Method x) {
	debug.print(formatMode(x.toString()));
    }

    public void visitClass(RepositoryClass x) {
	debug.println("");
	debug.tab();
	debug.print("<class");
	debug.print(" modifiers=\"");
	x.getAccessMode().accept(this);
	debug.print("\"");
	debug.print(" name=\"" + x.getName() + "\"");
	if (x.hasSuper()) {
	    debug.print(" extends=\"" + x.getSuper() + "\""); 
	}
	debug.println(">");
	debug.indentIn();
	if (x.getInterfaces().length > 0) {	      
	    debug.tab();
	    debug.println("<implements>");
	    debug.indentIn();
	    printInterfaces(x.getInterfaces());
	    debug.indentOut();
	    debug.tab();
	    debug.println("</implements>");
	}
	debug.tab();
	debug.print("<version");
	debug.print(" major=\"" + x.getMajorVersion() + "\"");
	debug.print(" minor=\"" + x.getMinorVersion() + "\"");
	debug.println("/>");
	if (x.getAttributes().length > 0) {	
	    debug.tab();
	    debug.println("<attributes>");
	    debug.indentIn();
	    x.visitAttributes(this);
	    debug.indentOut();
	    debug.tab();
	    debug.println("</attributes>");
	}
	debug.tab();
	debug.println("<members>");
	debug.indentIn();
        x.visitHeader(this);
	x.visitMembers(this);
	debug.indentOut();
	debug.tab();
	debug.println("</members>");
	debug.indentOut();
	debug.tab();
	debug.println("</class>");
    }

    public void visitField(RepositoryMember.Field x) {
	boolean closed = false;
	debug.tab();
	debug.print("<field");
	debug.print(" modifiers=\"");
	x.getMode().accept(this);
	debug.print("\"");
	debug.print(" name=\"" + x.getUnboundSelector().getName() + "\"");
	debug.print(" selector=\"" + x.getUnboundSelector().getDescriptor() + "\"");
	debug.indentIn();
 	if (x.getConstantValue() != null) {
	    if (!closed) {
		debug.println(">");
		closed = true;
	    }
	    debug.tab();
	    debug.println("<constantValue value=\"" + x.getConstantValue() + "\"/>");
	}
	if (x.getAttributes().length > 0) {	
	    if (!closed) {
		debug.println(">");
		closed = true;
	    }
	    debug.tab();
	    debug.println("<attributes>");
	    debug.indentIn();
	    x.visitAttributes(this);
	    debug.indentOut();
	    debug.tab();
	    debug.println("</attributes>");
	}
	debug.indentOut();
	if (closed) {
	    debug.tab();
	    debug.println("</field>");
	}
	else {
	    debug.println("/>");
	}
    }
    
    public void visitMethod(RepositoryMember.Method x) {
	debug.tab();
	debug.print("<method");
	debug.print(" modifiers=\"");
	x.getMode().accept(this);
	debug.print("\"");
	debug.print(" name=\"" + x.getUnboundSelector().getName() + "\"");
	debug.print(" selector=\"" + x.getUnboundSelector().getDescriptor() + "\"");
	debug.println(">");	      
	debug.indentIn();
	if (x.getThrownExceptions() != null && 
	    x.getThrownExceptions().length != 0) {
	    debug.tab();
	    debug.println("<thrownExceptions>");
	    debug.indentIn();
	    TypeName[] xs = x.getThrownExceptions();
	    for (int i = 0; i < xs.length; i++) {
		debug.tab();
		debug.println("<thrownException type=\"" + xs[i] + "\"/>");
	    }
	    debug.indentOut();
	    debug.tab();
	    debug.println("</thrownExceptions>");
	}
	if (x.getCodeFragment() != null) 
	    x.getCodeFragment().accept(this);
	if (x.getAttributes().length > 0) {	
	    debug.tab();
	    debug.println("<attributes>");
	    debug.indentIn();
	    x.visitAttributes(this);
	    debug.indentOut();
	    debug.tab();
	    debug.println("</attributes>");
	}
	debug.indentOut();
 	debug.tab();
	debug.println("</method>");
    }

    public void visitByteCodeFragment(Bytecode x) {
	boolean closed = false;
	debug.tab();
	debug.print("<code length=\""+ x.getCode().length + "\"");
	debug.print(" maxStack=\""+ x.getMaxStack() + "\"");
 	debug.print(" maxLocal=\""+ x.getMaxLocals() + "\"");
	debug.indentIn();
	if (x.getExceptionHandlers().length > 0) {	
	    if (!closed) {
		debug.println(">");
		closed = true;
	    }
	    debug.tab();
	    debug.println("<handledExceptions>");
	    debug.indentIn();
	    // BUG WARNING : always null !!! 
	    x.visitExceptions(this);	
	    debug.indentOut();
	    debug.tab();
	    debug.println("</handledExceptions>");
	}
	if (x.getAttributes().length > 0) {	
	    if (!closed) {
		debug.println(">");
		closed = true;
	    }
	    debug.tab();
	    debug.println("<attributes>");
	    debug.indentIn();
	    x.visitAttributes(this);
	    debug.indentOut();
	    debug.tab();
	    debug.println("</attributes>");
	}
	debug.indentOut();
	if (closed) {
	    debug.tab();
	    debug.println("</code>");
	}
	else {
	    debug.println("/>");
	}
    }
    public void visitException(ExceptionHandler x) {
	debug.tab();
	debug.print("<handledException");
	debug.print(" startPC=\"" + x.getStartPC() + "\"");
	debug.print(" endPC=\"" + x.getEndPC()+ "\"");
	debug.print(" handlerPC=\"" + x.getHandlerPC() + "\"");
	debug.print(" type=\"" + x.getCatchTypeName() + "\"");
	debug.println("/>");
    }
    public void visitAttrLineNumberTable(Attribute.LineNumberTable x) {
	debug.tab();
	debug.print("<attribute name=\"" + x.getName() + "\"");
	// debug.print(" arraySize=\"" + x.getStartPCTable().length + "\"");
	debug.print(" arrayNumber=\"2\"");
	debug.println("/>");
    }
    public void visitAttrLocalVariableTable(Attribute.LocalVariableTable x) {
	debug.tab();
	debug.print("<attribute name=\"" + x.getName() + "\"");
	// debug.print(" arraySize=\"" + x.getStartPCTable().length + "\"");
	debug.print(" arrayNumber=\"5\"");
	debug.println("/>");
    }
    public void visitAttrSourceFile(Attribute.SourceFile x) {
	debug.tab();
	debug.print("<attribute name=\"" + x.getName() + "\"");
	debug.print(" sourceFileName=\"" + x.getSourceFileName() + "\"");
	debug.println("/>");
    }
    public void visitAttrThirdParty(Attribute.ThirdParty x) {
	debug.tab();
	debug.print("<attribute name=\"" + x.getName() + "\"");
	debug.print(" contentSize=\"" + x.getContent().length + "\"");
	debug.println("/>");
    }
    public void visitAttrDeprecated(Attribute.Deprecated x) {
	debug.tab();
	debug.print("<attribute name=\"" + x.getName() + "\"");
	debug.println("/>");
    }
    public void visitAttrSynthetic(Attribute.Synthetic x) {
	debug.tab();
	debug.print("<attribute name=\"" + x.getName() + "\"");
	debug.println("/>");
    }
    public void visitStaticInnerClass(TypeName.Scalar tn) {
	debug.tab();
	debug.print("<staticInnerClass name=\"" + tn + "\"/>");
    }

    public void visitInstanceInnerClass(TypeName.Scalar tn) {
	debug.tab();
	debug.print("<instanceInnerClass name=\"" + tn + "\"/>");
    }
    
    public void visitAttrInnerClasses(Attribute.InnerClasses x) {
	debug.tab();
	debug.println("<innerClass>");
	debug.indentIn();
	debug.tab();
	for (int i = 0; i < x.size(); i++) {
	    debug.println("<innerClassInfo name=\"innerClass\" value=\"" + 
			  x.getInnerClass(i) + "\"/>");
	    debug.tab();
	    debug.println("<innerClassInfo name=\"outerClass\" value=\"" + 
			  x.getOuterClass(i) + "\"/>");
	    debug.tab();
	    debug.println("<innerClassInfo name=\"innerName\" value=\"" + 
			  x.getInnerNameIndex(i) + "\"/>");
	    debug.tab();
	    debug.print("<innerClassInfo name=\"modifiers\" " +
			"value=\"");
	    x.getMode(i).accept(this);
	    debug.println("\"/>");
	}
	debug.indentOut();
	debug.tab();
	debug.println("</innerClass>");
    }

    /* PRINTING METHODS */
    
    /**
     * Formats the Mode output removing last space and replacing other 
     * spaces by commas.
     **/
    private String formatMode(String s) {
	return s.trim().replace(' ', ',');
    }

    /**
     * Print each interface name in the class declaration.
     **/
    private void printInterfaces(TypeName[] x) {
	if (x.length == 0) 
	    return;
	for (int i = 0; i < x.length; i++) {
	    debug.tab();
	    debug.print("<interface");
	    debug.print(" name=\"" + x[i].toString() + "\"");
	    debug.println("/>");
	}
    }

}
