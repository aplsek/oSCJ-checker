package ovm.services.bytecode.editor;

import ovm.core.repository.Selector;
import ovm.services.bytecode.InstructionBuffer;
import ovm.util.ArrayList;
import ovm.util.Iterator;

/**
 * This is a controller that does a linear pass over the bytecode, invoking the 
 * appropriate visit method of an InstructionEditVisitor in each step.
 * 
 * @author Filip Pizlo and others
 * @see EditVisitor.Controller
 **/
public final class LinearPassController implements EditVisitor.Controller {
    
    /**
     * Return true if the method should be visited. 
     * @author janvitek
     */
    public interface MethodPicker {
         boolean pickMethod(Selector.Method method); 
    }
    
    private static final MethodPicker pickEveryMethod = new MethodPicker() {
	    public boolean pickMethod(Selector.Method method) { return true; }
	};
    
    private static final class VisitorStruct {
        public InstructionEditVisitor edit_visitor;
        public MethodPicker picker;
        
        public VisitorStruct(InstructionEditVisitor edit_visitor) {
            this.edit_visitor = edit_visitor;
            this.picker       = pickEveryMethod;
        }
        
        public VisitorStruct(InstructionEditVisitor edit_visitor,
                             MethodPicker picker)   {
            this.edit_visitor = edit_visitor;
            this.picker       = picker;
        }
    }
    
    /**
     * The InstructionEditVisitors that should be used.
     */
    private ArrayList _edit_visitors = new ArrayList();
    
    /**
     * Initializes itself without any edit visitors.  Use
     * the add() method.
     */
    public LinearPassController() {}
    
    /**
     * @param edit_visitor The InstructionEditVisitor that you would
     *                     like to visit each instruction.
     */
    public LinearPassController(InstructionEditVisitor edit_visitor) {
        add(edit_visitor);
    }
    
    /**
     * @param edit_visitor The InstructionEditVisitor to add.  It will
     *                     be called last.
     */
    public void add(InstructionEditVisitor edit_visitor) {
        _edit_visitors.add(new VisitorStruct(edit_visitor));
    }
    
    /**
     * @param edit_visitor The InstructionEditVisitor to add.  It will
     *                     be called last, and only if the supplied
     *                     MethodPicker returns true.
     */
    public void add(InstructionEditVisitor edit_visitor,
                    MethodPicker picker) {
        _edit_visitors.add(new VisitorStruct(edit_visitor,picker));
    }

    /**
     * This run method is invoked for each codefragment that
     * is edited. The Controller is then responsible for
     * performing the appropriate editing.
     **/
    public void run(CodeFragmentEditor cfe) {
        for (Iterator e = _edit_visitors.iterator();
             e.hasNext();) {
            VisitorStruct visitor_struct = (VisitorStruct)e.next();
            InstructionBuffer code_buf = cfe.getOriginalCode();
            code_buf.rewind();
            if (!visitor_struct.picker.pickMethod(code_buf.getSelector()))
                continue;
                
            visitor_struct.edit_visitor.beginEditing(code_buf,cfe);

            try {
                while (code_buf.hasRemaining()) 
                    visitor_struct.edit_visitor.visitAppropriate(code_buf.get()); 
            } finally {
                visitor_struct.edit_visitor.endEditing();
            }
        }
    }
}