package s3.services.bytecode.writer;

import java.io.IOException;

import ovm.core.repository.TypeName;
import ovm.core.repository.Binder;
import ovm.core.repository.RepositoryClass;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.writer.Dumper;
import ovm.util.OVMError;
import org.ovmj.util.Runabout;
import ovm.core.repository.RepositoryString;

/**
 * Write out a class in the Ovm IR format.
 * FIXME: the file should end in .ovm --jv
 * @author Christian Grothoff
 **/
public class S3OVMIRDumper extends S3Dumper {

    /**
     * Create a dumper to write cls.
     **/
    public S3OVMIRDumper(Dumper.Context ctx, RepositoryClass cls) {
        super(ctx, cls);
    }

    /**
     * @param isClean pass true here if the class has only one constant pool
     *        and does not need a cleanup-phase
     **/
    public S3OVMIRDumper(
        Dumper.Context ctx,
        RepositoryClass cls,
        boolean isClean) {
        	
        super(ctx, cls, isClean);
    }

    protected Runabout makeConstantWriter() {
        return new OVMIRConstantWriter();
    }

    /**
     * Runabout that writes the constants to the stream. 
     *
     * @author Christian 'Runabout' Grothoff
     **/
    public class OVMIRConstantWriter extends S3Dumper.ConstantWriter {

        public void visit(TypeName.Gemeinsam rss) {
            try {
                out.writeByte(JVMConstants.CONSTANT_SharedState);
                out.writeChar(addUtf8Constant(rss.toString()));
            } catch (IOException e) {
                throw new OVMError(e);
            }
        }

        public void visit(Binder rb) {
            try {
                out.writeByte(JVMConstants.CONSTANT_Binder);
                out.writeChar(addUtf8Constant(rb.getTypeName().toString()));
                RepositoryString[] s = rb.getArguments();
                out.writeChar(s.length);
                for (int i = 0; i < s.length; i++)
                    out.writeChar(addConstant(s[i].getUTF8Index()));
            } catch (IOException e) {
                throw new OVMError(e);
            }
        }

    } // end of S3Dumper.ConstantWriter
   
    static public class Factory implements Dumper.Factory {
        public Dumper makeDumper(Dumper.Context ctx, RepositoryClass cls) {
            return new S3OVMIRDumper(ctx, cls);
        }
    } // End of S3OVMIRDumper.Factory

} // end of S3OVMIRDumper
