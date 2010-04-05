package s3.core.services.interpreter;

import java.io.FileWriter;
import java.io.IOException;

import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.ReflectiveArray;
import ovm.core.repository.TypeName;
import ovm.services.bytecode.InstructionSet;
import s3.services.bootimage.ImageObserver;
import s3.services.bytecode.interpreter.S3Generator;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;
import s3.services.bootimage.Analysis;

/**
 * This class is responsible for compiling the interpreter's C code
 * during image build.
 **/
public class ImageCompiler extends ImageObserver {

    public ImageCompiler(String OVMMakefile, String gdbinit)
	throws BCbootTime
    {
	super(OVMMakefile, gdbinit);
	try {
	    FileWriter out = new FileWriter("java_instructions_threaded.gen");
	    new S3Generator(out, InstructionSet.SINGLETON).generate();
	    out.close();
	} catch (IOException e) {
	    throw new Error("Interpeter generator failed: " + e);
	}
    }

    public void registerLoaderAdvice(Gardener gardener) {
	// Make sure that all primitive array types are included in
	// structs.h, they are needed by *aload and *astore
	String primTypes = "ZBCSIJFD";
	Domain ed = DomainDirectory.getExecutiveDomain();
	for (int i = 0; i < primTypes.length(); i++) {
	    TypeName pt = TypeName.Primitive.make(primTypes.charAt(i));
	    new ReflectiveArray(ed, pt);
	}
    }

    public boolean shouldQuickify() { return true; }

    public void compileDomain(Domain _, Analysis __) {
	// That was easy!
    }
}
