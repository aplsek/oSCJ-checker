package s3.services.bytecode;

import java.io.IOException;
import java.io.Writer;

import ovm.core.repository.TypeCodes;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionSet;
import ovm.services.bytecode.SpecificationIR.IntValueList;
import ovm.services.bytecode.SpecificationIR.Padding;
import ovm.services.bytecode.SpecificationIR.StreamableValue;
import ovm.services.bytecode.SpecificationIR.Value;
import ovm.util.CommandLine;
import ovm.util.OVMError;

public class DisassemblerGenerator 
    extends SpecificationProcessor.CSourceGenerator {

    protected final static String STREAM_IN_PREFIX = "istream_";

    protected String istreamInName(int inslot) {
	return STREAM_IN_PREFIX + inslot;
    }

    private void declareIstreamIns(Instruction spec) throws IOException {
	int stream_offset = OPCODE_SIZE;
	boolean hasPadding = false;
	for (int i = 0; i < spec.istreamIns.length; i++) {
	    StreamableValue v = spec.istreamIns[i];
	    if (v instanceof Padding) {
		if (!hasPadding) {
		    writeln("int padding = PAD4(GETPC() + " + stream_offset 
			    + ");");
		    hasPadding = true;
		}
		continue;
	    }  
	    String macroName;
	    
	    switch (v.getType()) {
	    case TypeCodes.UBYTE:
		macroName = "ISTREAM_GET_UBYTE_AT";
		break;
	    case TypeCodes.BYTE:
		macroName = "ISTREAM_GET_SBYTE_AT";
		break;
	    case TypeCodes.CHAR:
	    case TypeCodes.USHORT:
		macroName = "ISTREAM_GET_USHORT_AT";
		break;
	    case TypeCodes.SHORT:
		macroName = "ISTREAM_GET_SSHORT_AT";
		break;
	    case TypeCodes.INT:
		if (v instanceof IntValueList) {
		    macroName = "ISTREAM_GET_SINT_ADDRESS_AT";
		} else {
		    macroName = "ISTREAM_GET_SINT_AT";
		}
		break;
	    case TypeCodes.FLOAT:
		macroName = "ISTREAM_GET_FLOAT_AT";
		break;
	    case TypeCodes.DOUBLE:
		macroName = "ISTREAM_GET_DOUBLE_AT";
		break;
	    case TypeCodes.LONG:
		macroName = "ISTREAM_GET_LONG_AT";
		break;
	    default:
		throw new OVMError("cannot deal with type " + v.getType());
	    }
	    writeln(getCType((Value)v) + ' ' + istreamInName(i) 
		    + " = " + macroName + '(' + stream_offset 
		    + (hasPadding ? " + padding" : "")
		    + ");");
	    stream_offset += v.bytestreamSize();
	}
    }


    protected void emitBody(Instruction spec) throws IOException {
	declareIstreamIns(spec);
	if (!(spec instanceof Instruction.Switch)) {
	    write("printf(\"" + spec.getName());
	    for (int i = 0; i < spec.istreamIns.length; i++) {
		switch(spec.istreamIns[i].getType()) {
		case TypeCodes.UINT:
		    write(" %u");
		    break;
		default:
		    write(" %d");
		    break;
		}
	    }

	    write("\\n\"");
	    for (int i = 0; i < spec.istreamIns.length; i++) {
		write(", " + istreamInName(i));
	    }

	    writeln(");");
	    emitPCUpdate(spec);
	} else {
	    writeln("printf(\"%s\\n\", \"" + spec.getName() + "\");");
	    // FIXME this is probably wrong
	    writeln("INCPC(padding + 4*(3 + istream_2 - istream_1 + 1));");
	}
	writeln("NEXT_INSTRUCTION;");
	    
    }
    
    protected void emitPCUpdate(Instruction spec) throws IOException {
	writeln("INCPC(" + spec.size(null) + ");"); // excepts if you call it on the "wrong" guys (K)
    }

    public DisassemblerGenerator(Writer out, 
				 InstructionSet is,
				 boolean shouldOutputDefs) {
	super(out, is);
    }

    public static void main(String[] args) throws IOException {
	CommandLine cLine = new CommandLine(args);
	Writer out = buildOutputStream(cLine.getArgument(0));
	boolean defines = (cLine.getOption("defines") != null);
	try {
	    new DisassemblerGenerator(out, 
				      InstructionSet.SINGLETON,
				      defines).generate();
	} finally {
	    out.flush();
	    out.close();
	} 
    }
}