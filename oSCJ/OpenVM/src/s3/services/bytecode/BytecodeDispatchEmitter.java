package s3.services.bytecode;

import java.io.IOException;
import java.io.Writer;

import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionSet;
import ovm.util.CommandLine;

public  class BytecodeDispatchEmitter 
    extends SpecificationProcessor.CSourceGenerator {

    public BytecodeDispatchEmitter(Writer out,
				   InstructionSet is) {
	super(out, is);
    }

    protected boolean shouldProcess(Instruction spec) {
	if ((spec instanceof Instruction.ConstantPoolRead)
	    && !(spec instanceof Instruction.Invocation)
	    && !(spec instanceof Instruction.ConstantPoolLoad)) {
	    return false;
	}
	return true;
    }

    public void generate() throws IOException {
	writeln("static void* instruction_dispatch[] = {");
	Instruction[] instructions = is_.set;
	for (int i = 0; i < instructions.length; i++) {
	    Instruction spec = instructions[i];
	    
	    if (spec == null || !shouldProcess(spec)) {
		write("\t&&INSTR_UNKNOWN");
	    } else {
		write("\t&&INSTR_" + spec.getName());
	    }
	    if (i < instructions.length - 1) {
		writeln(",");
	    } else {
		writeln();
	    }
	}
	writeln("};\n");
    }
    public static void main(String[] args) throws IOException {
	Writer out = buildOutputStream(new CommandLine(args).getArgument(0));
	try {
	    new BytecodeDispatchEmitter(out,
					InstructionSet.SINGLETON).generate();
	} finally {
	    out.flush();
	    out.close();
	}
    }
}


