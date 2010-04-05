package ovm.services.bytecode;

import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.TypeName;

/**
 * @author yamauchi
 */
public class CodeExceptionGen implements InstructionTargeter {

    public static final CodeExceptionGen[] EMPTY_ARRAY = new CodeExceptionGen[0];
    private InstructionHandle startPC;
    /**
     * Note that the endPC is inclusive unlike the JVMS. 
     * This is because if the end of the exception range is the end of the code, 
     * there is no InstructionHandle to point to.
     */
    private InstructionHandle endPC;
    private InstructionHandle handlerPC;
    private final TypeName.Scalar catchTypeName;
    
    CodeExceptionGen(InstructionHandle startPC,
            InstructionHandle endPC, 
            InstructionHandle handlerPC,
            TypeName.Scalar catchTypeName) {
        this.startPC = startPC;
        this.endPC = endPC;
        this.handlerPC = handlerPC;
        this.catchTypeName = catchTypeName;
	startPC.addTargeter(this);
	endPC.addTargeter(this);
	handlerPC.addTargeter(this);
    }

    public String toString() { return "CodeExceptionGen(" + startPC + ", " + endPC + ", " + handlerPC + ", " + catchTypeName; }    
    public InstructionHandle getStartPC() { return startPC; }
    public InstructionHandle getEndPC() { return endPC; }
    public InstructionHandle getHandlerPC() { return handlerPC; }
    public TypeName.Scalar getCatchType() { return catchTypeName; }
    
    public void setStartPC(InstructionHandle ih) { 
	if (! (startPC == endPC || startPC == handlerPC))
	    startPC.removeTargeter(this);
	ih.addTargeter(this);
	startPC = ih; 
    }
    public void setEndPC(InstructionHandle ih) { 
	if (! (endPC == startPC || endPC == handlerPC))
	    endPC.removeTargeter(this);
	ih.addTargeter(this);
	endPC = ih; 
    }
    public void setHandlerPC(InstructionHandle ih) { 
	if (! (handlerPC == startPC || handlerPC == endPC))
	    handlerPC.removeTargeter(this);
	ih.addTargeter(this);
	handlerPC = ih; 
    }
    
    public ExceptionHandler getExceptionHandler() {
        return new ExceptionHandler((char)startPC.getPosition(),
                    endPC.next != null 
                            ? (char)endPC.next.getPosition() 
                            : (char)(endPC.getPosition() + endPC.getInstruction().size(endPC.getPosition())),
                    (char)handlerPC.getPosition(),
                    catchTypeName);
        
    }
    
    public boolean containsTarget(InstructionHandle ih) {
        return ih == startPC || ih == endPC || ih == handlerPC;
    }
    public void updateTarget(InstructionHandle old_ih,  InstructionHandle new_ih) {
//	if (! containsTarget(old_ih))
//	    throw new Error("Did not contain the target " + old_ih);	
        if (old_ih == startPC)
            startPC = new_ih;
        if (old_ih == endPC)
            endPC = new_ih;
        if (old_ih == handlerPC)
            handlerPC = new_ih;
	old_ih.removeTargeter(this);
	new_ih.addTargeter(this);
    }
}
