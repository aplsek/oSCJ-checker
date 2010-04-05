package ovm.services.bytecode;

import ovm.core.domain.ConstantPool;
import ovm.core.repository.Attribute;
import ovm.core.repository.Constants;
import ovm.core.repository.ConstantsEditor;
import ovm.core.repository.Descriptor;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.services.io.BasicIO;
import ovm.util.ByteBuffer;
import ovm.util.Vector;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Method;


/**
 * An S3ByteCode generator
 * @author yamauchi
 */
public class ByteCodeGen {

    final S3Method method;
    final Selector.Method selector;
    InstructionList iList;
    char maxStack;
    char maxLocals;
    CodeExceptionGen[] handlers;
    Attribute[] attributes;
    ConstantPool constantPool;
    
    public ByteCodeGen(S3ByteCode bc) {
        method = (S3Method)bc.getMethod();
	selector = method.getSelector();
        InstructionBuffer ibuf = InstructionBuffer.wrap(bc);
        iList = new InstructionList(ibuf);
        maxStack = bc.getMaxStack();
        maxLocals = bc.getMaxLocals();
        ExceptionHandler[] ehs = bc.getExceptionHandlers();
        handlers = new CodeExceptionGen[ehs.length];
        for(int i = 0; i < ehs.length; i++) {
            int startPC = (int)ehs[i].getStartPC();
            int endPC = (int)ehs[i].getEndPC();
            int handlerPC = (int)ehs[i].getHandlerPC();
            InstructionHandle sIH = null;
            InstructionHandle eIH = null;
            InstructionHandle hIH = null;
            for(InstructionHandle ih = iList.getStart(); ih != null; ih = ih.next) {
                int pos = ih.getPosition();
                if (startPC == pos) sIH = ih;
                if (endPC == pos) eIH = ih.prev;
                if (handlerPC == pos) hIH = ih;
            }
            if (eIH == null) { // if endPC points to the end of the code stream
                eIH = iList.getEnd();
            }
            handlers[i] = new CodeExceptionGen(sIH, eIH, hIH, ehs[i].getCatchTypeName());
        }
        attributes = bc.getAttributes();
        constantPool = bc.getConstantPool();
    }

    public Selector.Method getSelector() { return selector; }
    public String toString() {
        return "ByteCodeGen of " + method.toString();
    }
    
    public boolean isStatic() {
        return method.getMode().isStatic();
    }
    
    public char[] getArgumentTypes() {
        Descriptor.Method desc = method.getSelector().getDescriptor();
        char[] args = new char[desc.getArgumentCount()];
        for(int i = 0; i < args.length; i++) {
            args[i] = desc.getArgumentType(i).getTypeTag();
        }
        return args;
    }
    
    public void addExceptionHandler(InstructionHandle startPC,
            InstructionHandle endPC,
            InstructionHandle handlerPC,
            TypeName.Scalar catchType) {
        CodeExceptionGen ceg = new CodeExceptionGen(startPC, endPC, handlerPC, catchType);
        CodeExceptionGen[] newHandlers = new CodeExceptionGen[handlers.length + 1];
        System.arraycopy(handlers, 0, newHandlers, 0, handlers.length);
        newHandlers[newHandlers.length-1] = ceg;
    }
    
    public void removeExceptionHandler(CodeExceptionGen ceg) {
	ceg.getStartPC().removeTargeter(ceg);
	ceg.getEndPC().removeTargeter(ceg);
	ceg.getHandlerPC().removeTargeter(ceg);
        Vector v = new Vector();
        for(int i = 0; i < handlers.length; i++) {
            if (ceg != handlers[i])
                v.add(handlers[i]);
        }
        handlers = new CodeExceptionGen[v.size()];
        v.toArray(handlers);
    }
    
    public Constants getConstantPool() { return constantPool; }
    public ConstantsEditor getConstantPoolEditor() { return constantPool; }
    
    public int getMaxStack() { return (int)maxStack; }
    public int getMaxLocals() { return (int)maxLocals; }

    // Recompute maxStack and maxLocals based on the current code
    public int setMaxStack() { throw new Error(); }
    public int setMaxLocals() { throw new Error(); }
    public void setMaxStack(int ms) { maxStack = (char)ms; }
    public void setMaxLocals(int ml) { maxLocals = (char)ml; }

    public void addAttribute(Attribute attr) {
        Attribute[] newAttrs = new Attribute[attributes.length + 1];
        System.arraycopy(attributes, 0, newAttrs, 0, attributes.length);
        newAttrs[newAttrs.length - 1] = attr;
	attributes = newAttrs;
    }
    public void removeAttribute(Attribute attr) {
        Vector v = new Vector();
        for(int i = 0; i < attributes.length; i++) {
            if (attributes[i] == attr) {
                //attributes[i] = null;
            } else {
                v.add(attributes[i]);
            }
        }
        Attribute[] newAttributes = new Attribute[v.size()];
        v.toArray(newAttributes);
        attributes = newAttributes;
    }
    public void removeLocalVariables() {
        Vector v = new Vector();
        for(int i = 0; i < attributes.length; i++) {
            if (attributes[i] instanceof Attribute.LocalVariableTable) {
                //attributes[i] = null;
            } else {
                v.add(attributes[i]);
            }
        }
        Attribute[] newAttributes = new Attribute[v.size()];
        v.toArray(newAttributes);
        attributes = newAttributes;
    }
    public void removeLineNumbers() { 
        Vector v = new Vector();
        for(int i = 0; i < attributes.length; i++) {
            if (attributes[i] instanceof Attribute.LineNumberTable) {
                //attributes[i] = null;
            } else {
                v.add(attributes[i]);
            }
        }
        Attribute[] newAttributes = new Attribute[v.size()];
        v.toArray(newAttributes);
        attributes = newAttributes;
    }
    
    protected void check() {
    	for(int i = 0; i < handlers.length; i++) {
    		if (! iList.contains(handlers[i].getStartPC()))
    			throw new Error();
    		if (! iList.contains(handlers[i].getEndPC())) {
    			iList.setPositions();
    			BasicIO.err.println(iList);
    			throw new Error("start = " + handlers[i].getStartPC()
    			+ " end = " + handlers[i].getEndPC()
    			+ " handler = " + handlers[i].getHandlerPC());
    		}
    		if (! iList.contains(handlers[i].getHandlerPC()))
    			throw new Error();
    		if (! handlers[i].getStartPC().containsTargeter(handlers[i]))
    			throw new Error();
    		if (! handlers[i].getEndPC().containsTargeter(handlers[i]))
    			throw new Error();
    		if (! handlers[i].getHandlerPC().containsTargeter(handlers[i]))
    			throw new Error();
    	}    	
    }
    /** Get the final S3ByteCode object */
    public S3ByteCode getByteCode() {
    	check();
        ByteBuffer bbuf = ByteBuffer.allocate(Integer.MAX_VALUE);
        iList.encode(bbuf);
        byte[] bytes = new byte[bbuf.position()];
        bbuf.rewind();
        bbuf.get(bytes);
        setMaxStack(maxStack);
        setMaxLocals(maxLocals);
        ExceptionHandler[] ehs = new ExceptionHandler[handlers.length];
        for(int i = 0; i < ehs.length; i++) {
            ehs[i] = handlers[i].getExceptionHandler();
        }
        return new S3ByteCode.Builder(method, bytes, 
                maxStack, maxLocals, ehs, 
                attributes, method.getMode().isSynchronized()).build();
    }

    public Attribute[] getAttributes() { return attributes; }
    public InstructionList getInstructionList() { return iList; }
    public CodeExceptionGen[] getExceptionHandlers() { return handlers; }
    
    
}
