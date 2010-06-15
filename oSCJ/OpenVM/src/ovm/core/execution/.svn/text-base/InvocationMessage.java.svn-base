package ovm.core.execution;
import ovm.core.OVMBase;
import ovm.core.domain.Code;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.WildcardException;
import ovm.core.services.memory.VM_Address;
import ovm.core.repository.Descriptor;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.util.OVMError;
import ovm.util.OVMError.IllegalArgument;
import s3.core.domain.S3ByteCode; 

/**
 * Reflectively invoke a <tt>Method</tt> of a given object. This is also
 * used to initialize a {@link Context} to execute the given method on
 * startup, with the given receiver.
 * <p>Note this class is not thread-safe, but can be used multiple times
 * returning a fresh return value object each time.
 *
 * <h3>Allocation Notes</h3>
 * <p>This class performs allocation during construction.
 * <p>This class performs allocation during {@link #invoke}.
 * <p>The current allocation context is always used.
 *
 * @author Krzysztof Palacz, updated: David Holmes
 */
public class InvocationMessage extends OVMBase {
    
    public static final boolean REFLECTION_DEBUGGING = false;
    
    private final Method method;
    private final ValueUnion[] inArgs;
    private Oop receiver;
 
    /**
     * Create an invocation message for the given method
     * @param method the Method
     */
    public InvocationMessage(Method method) {
	assert method != null: "null method for InvocationMessage";
	this.method = method;
	int argcount = getDesc().getArgumentCount();
	inArgs = new ValueUnion[argcount];
	for (int i = 0; i < argcount; i++) {
	    inArgs[i] = new ValueUnion(getDesc().getArgumentType(i).getTypeTag());
	}
    }

    private Descriptor.Method getDesc() {
	return method.getSelector().getDescriptor();
    }

    /**
     * Return the argument holder, in the given position, that will be, or was,
     * used for this invocation. Typically the caller retrieves this and then
     * sets the actual parameter value to be used.
     * @param index the index of the argument. Arguments are indexed from zero.
     */
    public ValueUnion getInArgAt(int index) {
	return inArgs[index];
    }

    /**
     * Return the object that this invocation will be applied to.
     */
    public Oop getReceiver() {
	return receiver;
    }
    
    /**
     * Set the object upon which this invocation message will be invoked.
     * This can also be set at invocation time.
     * @param receiver the target object
     */
    public void setReceiver(Oop receiver) {
	this.receiver = receiver;
    }
    
    /**
     * Query if the receiver has been set to a non-null value.
     * @return true of receiver is non-null, else false
    */
    public boolean isBound() {
	return receiver != null;
    }
    
    /**
     * Return the Method object that this invocation message represents
     */
    public Method getMethod() {
	return method;
    }

    /**
     * Invoke this method for the given object.
     * @param receiver_ the target object
     * @return a <tt>ReturnMessage</tt> containing the results, or exception,
     * of the invocation.
     * @throws NullPointerException if the receiver is null
     */
    public ReturnMessage invoke(Oop receiver_) {
	this.receiver = receiver_;
        return invoke((ReturnMessage)null);
    }


    /**
     * Invoke this method for the given object, using the given return message
     * @param receiver_ the target object
     * @param rmsg the return message to use thus avoiding allocation. If 
     * null a new ReturnMessage is allocated
     * @return the <tt>ReturnMessage</tt> containing the results, or exception,
     * of the invocation.
     * @throws NullPointerException if the receiver is null
     */
    public ReturnMessage invoke(Oop receiver_, ReturnMessage rmsg) {
	this.receiver = receiver_;
        return invoke(rmsg);
    }

    /**
     * Invoke this method for the set receiver.
     * @return a <tt>ReturnMessage</tt> containing the results, or exception,
     * of the invocation.
     */
    public ReturnMessage invoke() {
        return invoke((ReturnMessage)null);
    }

    /**
     * Invoke this method for the set receiver.
     * @param rmsg the ReturnMessage to use - which must have the correct type 
     * tag set. If null then a new ReturnMessage will be created.
     * @return a <tt>ReturnMessage</tt> containing the results, or exception,
     * of the invocation.
     */
    public ReturnMessage invoke(ReturnMessage rmsg) {
        try {
            if (!receiver.getBlueprint().
                isSubtypeOf(method.getDeclaringType().getContext().
                            getDomain().blueprintFor(method.getDeclaringType())) )
            throw new IllegalArgument("Wrong receiver type for method " + method);
        } catch(Exception e) {
            throw new OVMError("Failed to invoke " + method + " with error: " + e);
        }

        // in non-transactional mode, this is the identity function.
     // FIXME:::: TURNEDOFF   Method method = Transaction.the().selectReflectiveMethod(this.method);
        
        Code targetCode = method.getCode();
        // PAR: Note that it is now possible for a method to have no code.
        // This occurs because we duplicate all methods that will be logged 
        // and these may not have code to start with.
        
        if (REFLECTION_DEBUGGING) {
        
          int cid = method.getCID();
          int uid = method.getUID();
          int suffix = (cid << 16) + uid;
        
          Native.print_string("About to invoke method with suffix "+ suffix + " and signature " + method.getSelector().getDescriptor()+"\n");
          Selector.Method sel = method.getSelector();
          
  	  Native.print_string("If the invocation fails, try_to_add_method: ");
  	  Native.print_string(sel.getDefiningClass().toString()+" "+sel.getUnboundSelector().toString()+"\n");
        }

        char tag = method.getSelector().getDescriptor().getType().getTypeTag();

        if (rmsg == null) {
            rmsg = new ReturnMessage(tag);
        } else if (rmsg.getTypeTag() != tag) 
            throw new  OVMError.IllegalArgument("Wrong type tag in return message for method "+ method);        

        int value;
        try {
        
            // this will do forwarding in case of Brooks 
            InvocationMessage fixedInvocationMessage = (InvocationMessage) VM_Address.fromObject(this).asAnyObject();
            
            switch (tag) {
            case TypeCodes.VOID :
                Interpreter.invokeVoid(targetCode, fixedInvocationMessage);
                break;
            case TypeCodes.INT :
                value = Interpreter.invokeInteger(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setInt(value);
                break;
            case TypeCodes.BYTE :
                value = Interpreter.invokeInteger(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setByte((byte)value);
                break;
            case TypeCodes.CHAR :
                value = Interpreter.invokeInteger(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setChar((char)value);
                break;
            case TypeCodes.SHORT :
                value = Interpreter.invokeInteger(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setShort((short)value);
                break;
            case TypeCodes.BOOLEAN :
                // boolean handling is messy in ValueUnion
                value = Interpreter.invokeInteger(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setBoolean(value == 1);
                break;
            case TypeCodes.FLOAT :
                float fvalue = Interpreter.invokeFloat(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setFloat(fvalue);
                break;

            case TypeCodes.LONG :
                long longValue = Interpreter.invokeLong(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setLong(longValue);
                break;
            case TypeCodes.DOUBLE :
                double dValue = Interpreter.invokeDouble(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setDouble(dValue);
                break;

            case TypeCodes.GEMEINSAM : // we shouldn't be ever returning fixedInvocationMessage?
            case TypeCodes.OBJECT :
            case TypeCodes.ARRAY :
                Oop oop = Interpreter.invokeOop(targetCode, fixedInvocationMessage);
                rmsg.getReturnValue().setOop(oop);
                break;
            default:  assert false: "Unsupported return type!";
            }
        } catch (WildcardException e) { 
        
          rmsg.setException(e); 
        
          if (false) {
        
            Selector.Method sel = method.getSelector();
          
            Native.print_string("Invocation returning with exception: ");
            Native.print_string(sel.getDefiningClass().toString()+" "+sel.getUnboundSelector().toString()+"\n");
            
            return rmsg;
          }
        
        }
        
        if (false) {
        
          Selector.Method sel = method.getSelector();
          
  	  Native.print_string("Invocation returning without exception: ");
  	  Native.print_string(sel.getDefiningClass().toString()+" "+sel.getUnboundSelector().toString()+"\n");
        }
        
        return rmsg;
    }
}


