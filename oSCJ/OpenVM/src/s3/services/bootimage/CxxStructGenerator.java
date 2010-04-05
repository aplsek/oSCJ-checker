package s3.services.bootimage;
import java.io.PrintWriter;

import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Type;
import ovm.core.repository.Selector;
import ovm.core.services.format.CxxFormat;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.Set;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import java.io.IOException;

/**
 * Generate C++ struct declarations.  These include
 * <ul>
 *   <li> the image file header,
 *   <li> the object {@link ObjectModel header}, and
 *   <li> all valid class types initially loaded into the VM
 *   <li> per-domain templates for all possible array types
 * </ul>
 *
 * Array templates are per-domain because every array is an instance
 * of {@code java.lang.Object}.
 **/
public class CxxStructGenerator extends CxxFormat {
    private PrintWriter w;
    private int headerSize;

    /**
     * Generate the header file.
     * @param fname output file name
     * @param ds    the set of domains in the virtual machine
     **/
    static public void output(String fname, DomainSprout[] ds)
	throws IOException
    {
	CxxStructGenerator gen = new CxxStructGenerator(new PrintWriter(fname));

	BootImage.the().writeHeadersToC_h_File(gen.w);
	gen.genHeaderPrologue();

	// define object types for every domain
	for (int i = 0; i < ds.length; i++)
	    gen.genStructs((S3Domain) ds[i].dom, ds[i].anal);

	gen.w.close();
    }

    private void genHeaderPrologue() {
        ObjectModel m = ObjectModel.getObjectModel();
        w.println(m.toCxxStruct());
        headerSize = m.headerSkipBytes();
    }

    private void genStructDecls(S3Domain dom, Analysis anal) {
      //  S3Blueprint root = null;  unused
        for (Iterator it = dom.getBlueprintIterator(); it.hasNext();) {
            S3Blueprint bp = (S3Blueprint) it.next();
            if (bp.getType().isClass() && !bp.getType().isSharedState()) {
		if (anal.shouldCompile(bp)) {
		    w.print("struct ");
		    w.print(format(bp));
		    w.println(";");
		    // if (bp.getParentBlueprint() == null)
		    //    root = bp;
		} else {
		    w.print("typedef HEADER ");
		    w.print(format(bp));
		    w.println(";");
		}
	    }
        }
        w.println("");

        // See FIXME in genStructDefs for some thoughts on this
        // maneuver. Be careful to typedef interfaces after
        // <d>_java_lang_Object has been defined.
        for (Iterator it = dom.getBlueprintIterator(); it.hasNext();) {
            S3Blueprint bp = (S3Blueprint) it.next();
            if (bp.getType().isInterface()) {
                S3Blueprint zuper = bp.getParentBlueprint();
                w.println(
                    "typedef struct " + format(zuper) + " " + format(bp) + ";");
            }
        }
        w.println("");
    }

    private void dump(S3Domain dom, Analysis anal, Set seen, S3Blueprint bp) {
	S3Blueprint.Scalar zuper =   bp.getParentBlueprint();
	if (zuper != null)
	    genStructDef(dom, anal, seen, zuper);

	// FIXME: Interfaces are typedeffed above. This pretty
	// much ties gcc's hands for type-based alias analysis.
	// Should use virtual inheritence? How would that affect
	// object layout?
	if (bp.getType() instanceof Type.Class) {
	    w.print("struct ");
	    w.print(format(bp));
	    w.println(":");
	    w.print("    public ");
	    w.println(zuper == null ? "HEADER" : format(zuper));
	    w.println("{");

	    //     Type.Class t = (Type.Class) bp.getType();
	    Field[] fsel = bp.getLayout();
	    for (int i =
		     (zuper == null ? headerSize : zuper.getUnpaddedFixedSize()) / 4;
		 i < fsel.length;
		 i++) {
		if (fsel[i] == null)
		    // prev (exclusive) or next wide
		    continue;
		Field f = fsel[i];
		S3Blueprint fbp;
		try {
		    try {
			fbp = (S3Blueprint) dom.blueprintFor(f.getType());
		    } catch (LinkageException e) {
			// the LinkageException.Runtime we
			// care about is thrown by f.getType
			throw e.unchecked();
		    }
		} catch (LinkageException.Runtime e) {
		    String name = fsel[i].getSelector().getName();
		    BootBase.d(
			       BootBase.LOG_INCOMPLETE_TYPES,
			       "can't find type of "
			       + format(bp)
			       + "."
			       + name
			       + "(descriptor = "
			       + fsel[i].getSelector().getDescriptor()
			       + ") using Oop");
		    w.print("    HEADER *");
		    w.print(encode(name));
		    w.print(";\t// Really ");
		    w.println(fsel[i].getSelector().getDescriptor());
		    continue;
		}
		String name = encode(fsel[i].getSelector().getName());
		w.print("    ");
		if (fbp instanceof S3Blueprint.Primitive &&
		    fbp.getUnpaddedFixedSize() > s3.core.domain.MachineSizes.BYTES_IN_WORD)
		    w.print("__attribute__((aligned(8))) ");
		w.print(format(fbp.promote()));
		w.print(fbp.isReference() ? " *" : " ");
		w.print(name);
		w.println(";");
	    }
	    w.println("};");
	}

	if (bp == dom.ROOT_BLUEPRINT) {
	    // As soon as Object is defined, we can define array
	    // types for our domain
	    w.print("struct " + format(dom) + "_ARRAY: ");
	    w.println("public " + format(bp) + " {");
	    w.println("    jint length;");
	    w.println("};");
	    w.println("");

	    w.println("// why doesn't g++ allow zero-length arrays?");
	    w.print(
                    "template <class T, int sz=1> struct "
		    + format(dom)
		    + "_Array: ");
	    w.println("public " + format(dom) + "_ARRAY {");
	    w.println("    T values[sz];");
	    w.println("};");
	    w.println("");

            specializeArrayTemplate(dom,"jlong");
            specializeArrayTemplate(dom,"unsignedjlong");
            specializeArrayTemplate(dom,"jdouble");

	}
    }

    private void specializeArrayTemplate(S3Domain dom,String s) {
         w.print("template <int sz> struct "+ format(dom)
             + "_Array<"+s+",sz>: ");
         w.println("public " + format(dom) + "_ARRAY {");
         w.println("    __attribute__((aligned(8))) "+s+" values[sz];");
         w.println("};");
         w.println("");
    }

    private void genStructDef(S3Domain dom, Analysis anal,
			      Set seen, S3Blueprint bp) {
        if (seen.contains(bp))
            return;
        seen.add(bp);
        if (anal.shouldCompile(bp)) {
	    dump(dom, anal, seen, bp);
	    dump(dom, anal, seen,
		 (S3Blueprint) bp.getSharedState().getBlueprint());
	}
    }

    private void genStructDefs(S3Domain dom, Analysis anal) {
        Set seen = new HashSet();
        for (Iterator it = dom.getBlueprintIterator(); it.hasNext();) {
            S3Blueprint bp = (S3Blueprint) it.next();
            if (!bp.isSharedState())
                genStructDef(dom, anal, seen, bp);
        }
    }

    private void genStructs(S3Domain dom, Analysis anal) {
        w.println("// Domain " + format(dom));
        genStructDecls(dom, anal);
        w.println("\n");
        genStructDefs(dom, anal);
    }

    public CxxStructGenerator(PrintWriter w) {
        this.w = w;
    }
}
