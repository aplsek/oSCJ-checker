package s3.services.bootimage;

import java.io.*;

import ovm.core.domain.ObjectModel;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.format.CFormat;
import ovm.util.Arrays;
import ovm.util.Comparator;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Field;
import s3.core.domain.S3MemberResolver;
import ovm.util.ArrayList;
import ovm.core.domain.Field;

import ovm.util.Iterator;
import ovm.util.Collections;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.services.memory.VM_Address;

import ovm.util.HashSet;
import ovm.core.domain.LinkageException;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;

/**
 * Declare C structures.  These include
 * <ul>
 *   <li> the image file header,
 *   <li> the object {@link ObjectModel header},
 *   <li> instance and array types in the executive domain, and
 *   <li> the vtable for {@link CoreServicesAccess CoreServicesAccess}
 *        objects (only when ImageObserver is the interprer).
 * </ul>
 *
 * We do not generate structs for user-domain types or shared-state
 * types.  Doing so would lead to identifier clashes both between the
 * ED and UD, and between field names and C macros.  {@link
 * CxxStructGenerator} mangles names to avoid these problems.<p>
 *
 * The {@code CoreServicesAccess} vtable is only used within the
 * interpreter.
 **/
public class StructGenerator extends PrintWriter {
    private HashSet dumped = new HashSet();
    private Analysis anal;
    
    /**
     * Generate the header file.
     * @param fname The output file name
     * @param eds   The executive domain
     **/
    static public void output(String fname, DomainSprout eds)
	throws IOException
    {
	StructGenerator gen = new StructGenerator(fname, eds.anal);

	BootImage.the().writeHeadersToC_h_File(gen);
	gen.println(ObjectModel.getObjectModel().toCStruct());

	// declare instance and array types
	for (Iterator it = eds.dom.getBlueprintIterator(); it.hasNext(); )
	    gen.dumpStruct((S3Blueprint) it.next());

	// declare the CSA vtable.  The declaring class name matters.
	try {
	    TypeName tn = 
	    	ReflectionSupport.typeNameForClass(CoreServicesAccess.class);
	    Type t = eds.dom.getSystemTypeContext().typeFor(tn);
	    if (ImageObserver.the() 
		instanceof s3.core.services.interpreter.ImageCompiler)
		gen.dumpVTBL((S3Blueprint) eds.dom.blueprintFor(t));
	} catch (LinkageException _) {
	    throw new MisconfiguredException(CoreServicesAccess.class.getName()
					     + " was not loaded");
	}
	gen.close();
    }

    private StructGenerator(String f, Analysis anal) throws IOException {
        super(f);
	this.anal = anal;
    }

    private void dumpVTBL(S3Blueprint bp) {
        // FIXME get right classnames for members, get overloading right

        Selector.Method[] selectors = bp.getSelectors();
        boolean[] overloaded = JNIFormat.markOverloaded(selectors);
	
        TypeName.Compound thisClass = bp.getName().asCompound();
	StringBuffer buf = new StringBuffer();
	CFormat._.format(thisClass, buf).append("_VTBL {\n");
        buf.append("    struct java_lang_Object _parent_;\n");
	buf.append("    int length;\n");
	for (int i = 0; i < selectors.length; i++) {
            buf.append("    struct ovm_core_domain_Code *");
	    JNIFormat._.format(selectors[i], overloaded[i], buf).append(";\n");
        }
        buf.append("};\n");
	w(buf.toString());
    }

    private void dumpStruct(S3Blueprint bp) {
	// Only declare valid instance types and arrays, also only
	// declare each type once.
	if (bp == null || dumped.contains(bp) || bp.isPrimitive()
	    || (bp.isScalar() && !anal.shouldCompile(bp)))
	    return;
	dumpStruct(bp.getParentBlueprint());
	dumped.add(bp);

        if (bp instanceof S3Blueprint.Array) {
            dumpStructArray(bp);
        } else {
            dumpStructPlain(bp);
        }
    }

    //----------------PRIVATE-----------------------------------

    private static final Comparator fieldByOffsetComparator = new Comparator() {
	    public int compare(Object lhs, Object rhs) {
		int lho = ((S3Field) lhs).getOffset();
		int rho = ((S3Field) rhs).getOffset();
		return lho - rho;
	    }
	};

    private void dumpStructPlain(S3Blueprint bp) {
        S3Blueprint parent = bp.getParentBlueprint();
	w(CFormat._.format(bp.getName()));
        w(" {\n");
        if (parent != null) {
            w("  ");
	    w(CFormat._.format(parent.getName()));
            w(" _parent_;\n");
        } else
            w(" struct HEADER _parent_;\n");

	Type.Scalar t = bp.getType().asScalar();
	ArrayList fields = new ArrayList();
	for (Field.Iterator it = t.localFieldIterator(); it.hasNext(); )
	    fields.add(it.next());
	Collections.sort(fields, fieldByOffsetComparator);
	for (Iterator it = fields.iterator(); it.hasNext(); ) {
	    Field f = (Field) it.next();
	    w("  ");
	    // Don't force loading of field's type
            if (f.getSelector().getDescriptor().getType().isWidePrimitive())
                w("__attribute__((aligned(8))) ");
	    w(CFormat._.format(f.getSelector().getUnboundSelector()));
	    w(";\n");
        }
        w("};\n");
    }
    
    private void dumpStructArray(S3Blueprint bp) {
        TypeName.Array tname = bp.getName().asArray();
        TypeName compname = tname.getComponentTypeName();
	w(CFormat._.format(tname));
        w(" {\n    struct java_lang_Object _parent_;\n    int length;\n    ");
        if (compname.isWidePrimitive())
            w("__attribute__((aligned(8))) ");
	w(CFormat._.format(compname));
        if (!compname.isPrimitive())
            w("*");
        w(" values[0];\n};\n");
    }

    private void w(String s) {
        write(s);
    }
} // End of StructGenerator
