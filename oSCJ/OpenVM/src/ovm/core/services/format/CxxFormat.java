package ovm.core.services.format;

import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.Type;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.util.HashMap;
import s3.services.bootimage.JNIFormat;

/* 
   This file is provides formatting that is used in generated OVM C code.
   It is however C code now, not C++, as it used to be.
*/

/**
 * Move the C++ formatting conventions out of s3.services.j2c, so that
 * other code can use the same conventions.  Maybe this means that the
 * domain and blueprint counters should be exposed in the ovm
 * interfaces, but what happens to the stupid domain strings?
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 */
public class CxxFormat extends OVMBase {
    static public String format(Domain d) {
	int number = d.getUID();
	return number == 0 ? "e" : ("u" + number);
    }

    static private HashMap memoFormat = new HashMap();
    
    static public String format(Blueprint bp) {
	String ret = (String) memoFormat.get(bp);
	if (ret == null) {
	    ret = ((bp instanceof Blueprint.Primitive)
		   ? format((Blueprint.Primitive) bp)
		   : (bp instanceof Blueprint.Scalar)
		   ? format((Blueprint.Scalar) bp)
		   : format((Blueprint.Array) bp));
	    memoFormat.put(bp, ret);
	}
	return ret;
    }

    static private String format(Blueprint.Primitive bp) {
	switch(bp.getName().getTypeTag()) {
	case TypeCodes.BYTE:    return "jbyte";
	case TypeCodes.BOOLEAN: return "jboolean";
	case TypeCodes.CHAR:
	case TypeCodes.USHORT:  return "jchar";
	case TypeCodes.SHORT:   return "jshort";
	case TypeCodes.INT:     return "jint";
	case TypeCodes.UINT:    return "unsignedjint";
	case TypeCodes.LONG:    return "jlong";
	case TypeCodes.ULONG:   return "unsignedjlong";
	case TypeCodes.FLOAT:   return "jfloat";
	case TypeCodes.DOUBLE:  return "jdouble";
	case TypeCodes.VOID:    return "void";
	default:
	    throw new Error("unknown prim code:" + bp.getName().getTypeTag());
	}
    }

    static private String format(Blueprint.Scalar bp) {
	String ret = null;
	Type.Context ctx = bp.getType().getContext();
	Domain d = ctx.getDomain();
	
	String n = bp.getName().asScalar().toClassInfoString().intern();
	    
// 	    if (d.number != 0 || ctx != d.getSystemTypeContext())
// 		;		// ignore puns
// 	    else if (n == "ovm/core/services/memory/VM_Address")
// 		ret = "void";
// // 	    else if (n == "ovm.core.services.memory.VM_Word")
// // 		ret = "junsignedint";
// 	    else if (n == "ovm/core/domain/Oop")
// 		ret = "OBJECT_HEADER";
	    
	if (ret == null)
	    {
		StringBuffer b = new StringBuffer();
		
		b.append(format(d));
		b.append("_");

		if (bp.isSharedState()) {
		    Type t =  bp.getInstanceBlueprint().getType();
		    TypeName tn = t.getUnrefinedName();
		    // Encode special characters using JNI escapes.
		    if (tn.isScalar())
			n = tn.asScalar().toClassInfoString();
		    else
			// Already have G-typed classinfo string
			;
		}
		b.append(JNIFormat._.encode(n));

		// Domain may not uniquely identify this class!
		if (ctx != d.getSystemTypeContext()) {
		    b.append("__c");
		    b.append(ctx.getUID());
		}
		if (bp.isSharedState())
		    b.append("_static");
		ret = b.toString();
	    }
	return ret;
    }

    // format array for C++ (this is used even for compiling to C, to generate
    // array blueprints)
    static public String formatCxxArray(Blueprint.Array bp) {
    
    	StringBuffer b = new StringBuffer(format(bp.getType().getDomain()));
	b.append("_Array<");
	Blueprint cmp =  bp.getComponentBlueprint();
	if (cmp instanceof Blueprint.Array) {
	  b.append(formatCxxArray((Blueprint.Array)cmp));
        } else {
          b.append(format(cmp));
        }
	b.append(cmp.isReference() ? "*>" : ">");
	return b.toString();
    
    }

    // format array for C (domain_Array ...)
    static public String formatCArray(Blueprint.Array bp) {
    
    	StringBuffer b = new StringBuffer(format(bp.getType().getDomain()));
	b.append("_Array");
	Blueprint cmp =  bp.getComponentBlueprint();
	char tTag = cmp.getName().getTypeTag();
	if ((tTag==TypeCodes.LONG) || (tTag==TypeCodes.ULONG) || 
	  (tTag==TypeCodes.DOUBLE)) {
	    b.append("_8al");
        }
        return b.toString();
    
    }


    static private String format(Blueprint.Array bp) {
	return formatCArray(bp);
    }

    static private HashMap encodings = new HashMap();
    /**
     * Encode a short identifier according to JNI conventions and
     * C++ rules.
     * 
     * @param id the identifier to encode
     *
     * @return the mangled identifier
     *
     */
    static public String encode(String id) {
        String ret = (String) encodings.get(id);
        if (ret == null) {
	   ret = "ovm_" + JNIFormat._.encode(id);
           encodings.put(id, ret);
        }
        return ret;
    }
}
