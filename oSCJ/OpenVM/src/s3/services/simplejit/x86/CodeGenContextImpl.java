package s3.services.simplejit.x86;

import ovm.core.domain.Type;
import s3.services.simplejit.CodeGenContext;
import s3.services.simplejit.CodeLinker;

/**
 * @author Hiroshi Yamauchi
 **/
public class CodeGenContextImpl extends CodeGenContext {

    public CodeGenContextImpl(int bytecode_len, Type.Context tcontext) {
	super(bytecode_len, tcontext);
    }

    protected CodeLinker makeCodeLinker(int[] bytecodePC2NativePC) {
	return new CodeLinkerImpl(bytecodePC2NativePC);
    }

}
