package ovm.services.bytecode;

import ovm.core.repository.Attribute;
import ovm.core.repository.ExceptionHandler;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Area;
import ovm.util.ByteBuffer;
import ovm.util.Vector;
import s3.core.domain.S3ByteCode;

/**
 * An S3ByteCode generator that allocates in a given area
 * @author yamauchi
 */
public class ByteCodeGen2 extends ByteCodeGen {

	private VM_Area compileArea;
    public ByteCodeGen2(S3ByteCode bc, VM_Area compileArea) {
    	super(bc);
    	this.compileArea = compileArea;
    }
    
    public void addAttribute(Attribute attr) {
    	if (compileArea != null) {
    		VM_Area prev = MemoryManager.the().setCurrentArea(compileArea);
    		try {
    			super.addAttribute(attr);
    		} finally {
    			MemoryManager.the().setCurrentArea(prev);
    		}
    	} else {
    		super.addAttribute(attr);
    	}
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
        attributes = toArray(v);
    }
    private Attribute[] toArray(Vector v) {
        Attribute[] newAttributes = null;
    	if (compileArea != null) {
    		VM_Area prev = MemoryManager.the().setCurrentArea(compileArea);
    		try {
    	        newAttributes = new Attribute[v.size()];
    		} finally {
    			MemoryManager.the().setCurrentArea(prev);
    		}
    	} else {
            newAttributes = new Attribute[v.size()];
    	}
        v.toArray(newAttributes);
        return newAttributes;
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
        attributes = toArray(v);
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
        attributes = toArray(v);
    }
    public S3ByteCode getByteCode() {
    	check();
        ByteBuffer bbuf = ByteBuffer.allocate(Integer.MAX_VALUE);
        iList.encode(bbuf);
        setMaxStack(maxStack);
        setMaxLocals(maxLocals);
        byte[] bytes = null;
        ExceptionHandler[] ehs = null;
    	if (compileArea != null) {
    		VM_Area prev = MemoryManager.the().setCurrentArea(compileArea);
    		try {
    			bytes = new byte[bbuf.position()];
    			ehs = new ExceptionHandler[handlers.length];
    	        for(int i = 0; i < ehs.length; i++) {
    	            ehs[i] = handlers[i].getExceptionHandler();
    	        }
    		} finally {
    			MemoryManager.the().setCurrentArea(prev);
    		}
    	} else {
    		bytes = new byte[bbuf.position()];
    		ehs = new ExceptionHandler[handlers.length];
            for(int i = 0; i < ehs.length; i++) {
                ehs[i] = handlers[i].getExceptionHandler();
            }
    	}
        bbuf.rewind();
        bbuf.get(bytes);
    	if (compileArea != null) {
    		VM_Area prev = MemoryManager.the().setCurrentArea(compileArea);
    		try {
    	        return new S3ByteCode.Builder(method, bytes, 
    	                maxStack, maxLocals, ehs, 
    	                attributes, method.getMode().isSynchronized()).build();
    		} finally {
    			MemoryManager.the().setCurrentArea(prev);
    		}
    	} else {
            return new S3ByteCode.Builder(method, bytes, 
                    maxStack, maxLocals, ehs, 
                    attributes, method.getMode().isSynchronized()).build();
    	}        
    }
}
