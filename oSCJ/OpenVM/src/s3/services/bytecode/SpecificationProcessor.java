package s3.services.bytecode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import ovm.core.OVMBase;
import ovm.core.repository.TypeCodes;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionSet;
import ovm.services.bytecode.SpecificationIR.DoubleValue;
import ovm.services.bytecode.SpecificationIR.FloatValue;
import ovm.services.bytecode.SpecificationIR.IntValue;
import ovm.services.bytecode.SpecificationIR.LongValue;
import ovm.services.bytecode.SpecificationIR.PCValue;
import ovm.services.bytecode.SpecificationIR.RefValue;
import ovm.services.bytecode.SpecificationIR.Value;
import ovm.services.bytecode.SpecificationIR.ValueList;
import ovm.services.bytecode.SpecificationIR.WideValue;
import ovm.util.OVMError;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;

public abstract class SpecificationProcessor extends OVMBase { 
    
    // true generates a switch case per for each instruction, false
    // generates a label.
    boolean genSwitch = false;
    
    protected final static int OPCODE_SIZE = 1;

    protected Writer out;

    protected final InstructionSet is_;

    public SpecificationProcessor(Writer out,
				  InstructionSet is) {
	this.is_ = is;
	this.out = out;
    }

    private int indent;
    protected void writeln(String string) throws IOException {
	for (int i = 0; i < indent; i++) 
	    out.write("    ");
	
	write(string);
	out.write('\n');
    }

    protected void writeln() throws IOException, BCbootTime {
	out.write('\n');
    }
    
    protected void write(String string) throws IOException, BCbootTime {
	int length = string.length();
	for (int i = 0; i < length; i++) {
	    char c = string.charAt(i);
	    out.write(c);
	    if (c == '\n') {
		for (int j = 0; j < indent; j++) {
		    out.write("    ");
		}
	    }
	}
    }
    
    protected void indent() {
	indent ++;
    }
    protected void unindent() {
	indent --;
    }


    public static interface SpecClosure {
	public void call(Instruction spec) throws IOException;
    }

    public void iterateOverSpecs(SpecClosure closure) throws IOException {
	Instruction[] instructions = is_.set;
	for (int i = 0; i < instructions.length; i++) {
	    Instruction spec = instructions[i];
	    if (spec == null)
		continue;
	    if (shouldProcess(spec)) {
		closure.call(spec);
	    }
	}
    }
    protected boolean shouldProcess(Instruction spec) {
	return true;
    }

    
    public abstract void generate() throws IOException;


    protected static Writer buildOutputStream(String arg) throws BCbootTime {
	Writer out = null;
	if (arg != null) {
	    try {
		out = new OutputStreamWriter(new FileOutputStream(arg));
	    } catch (IOException e) {
		System.err.println("Could not open " + arg
				   + " for writing. Using standard out");
		out = new OutputStreamWriter(System.out);
	    }
	} else {
	    out = new OutputStreamWriter(System.out);
	}
	return out;
    }

    // FIXME move somewhere else ?
    public static class CSourceGenerator 
	extends SpecificationProcessor {

	protected CSourceGenerator(Writer out,
				   InstructionSet is) {
	    super(out, is);
	}

	protected String getCType(Value value) {
	    if (value instanceof ValueList) {
		return "jint*";
	    } else if (value instanceof PCValue
		       && !((PCValue)value).isRelative()) {
		return "byte*";
	    } else if (value instanceof IntValue) {
		switch (((IntValue)value).getType()) {
		case TypeCodes.UBYTE:
		    return "unsigned char";
		case TypeCodes.BYTE:
		    return "jbyte";
		case TypeCodes.CHAR:
		    return "jchar";
		case TypeCodes.USHORT:
		    return "unsigned short";
		case TypeCodes.SHORT:
		    return "jshort";
		case TypeCodes.UINT:
		    return "unsignedjint";
		case TypeCodes.INT:
		    return "jint";
		default:
		    throw new OVMError("FIXME");
		}
	    } else if (value instanceof FloatValue) {
		return "jfloat";
	    }  else if (value instanceof RefValue) {
		return "jref";
	    }  else if (value instanceof WideValue) {
		if (value instanceof DoubleValue) {
		    return "jdouble";
		} else if (value instanceof LongValue) {
		    return (((LongValue) value).getType() == TypeCodes.LONG
			    ? "jlong" : "unsignedjlong");
		} else {
		    assert(false);
		    return null;
		}
	    } else {
		return "jint";
	    }
	}
	
	protected String getUnionField(Value value) {
	    if (value instanceof PCValue) {
		return ".jref"; 
	    } else if (value instanceof IntValue) {
		switch (((IntValue)value).getType()) {
		case TypeCodes.UBYTE:
		    throw new Error();
		case TypeCodes.BYTE:
		    return ".jbyte";
		case TypeCodes.CHAR:
		    return ".jchar";
		case TypeCodes.USHORT:
		    throw new Error();
		case TypeCodes.SHORT:
		    return ".jshort";
		case TypeCodes.UINT:
		    // FIXME only works if we immediately store to a local
		case TypeCodes.INT:
		    return ".jint";
		default:
		    throw new OVMError("oops"); // FIXME useless message
		}
	    } else if (value instanceof FloatValue) {
		return ".jfloat";
	    }  else if (value instanceof RefValue) {
		return ".jref";
	    } else if (value instanceof WideValue) {
		if (value instanceof DoubleValue) {
		    return ".jdouble";
		} else if (value instanceof LongValue) {
		    // FIXME ULONG only works if we immediately store to a local
		    return ".jlong";
	    } else {
		assert false : "unrecognized stack or local type";
		return null;
	    }
	    } else {
		if (value instanceof WideValue) // how would that be true???
		    return ".jlong";
		else
		    return ".jint"; // trick with unions.  The type must be someting INSIDE the union
	    }
	}

	protected void emitHeader(Instruction spec) throws IOException {
	    if (genSwitch)
		write("case ");
	    write("INSTR_");
	    write(spec.getName());
	    write(":");
	}
	
	protected void emitBody(Instruction spec) throws IOException {}
	protected void emitWideInstruction() throws IOException {
	    writeln("DO_WIDE;");
	}
    
	public void generate() throws IOException {
	    iterateOverSpecs(new SpecClosure() {
		    public void call(Instruction spec) throws IOException {
			emitHeader(spec);
			writeln(" {");
			if (spec instanceof Instruction.WIDE) {
			    emitWideInstruction();
			} else {
			    emitBody(spec);
			}
			writeln("}");
			
		    }
		});
	    writeln("\n");
	}
    }
}
