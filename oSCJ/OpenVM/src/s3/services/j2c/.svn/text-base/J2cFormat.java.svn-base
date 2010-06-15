package s3.services.j2c;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Type;
import ovm.core.repository.TypeName;
import ovm.core.services.format.CxxFormat;
import ovm.core.repository.UTF8Store;
import ovm.util.HTint2Object;
import ovm.util.SparseArrayList;
import s3.core.domain.S3Blueprint;
import ovm.core.domain.Method;
import s3.services.bootimage.JNIFormat;
import ovm.core.domain.Blueprint;
import java.util.Random;

public class J2cFormat extends CxxFormat {
    static final boolean KEEP_CACHE_STATS = J2cImageCompiler.KEEP_STATS;
    static final boolean RANDOMIZE_METHOD_NAMES = false; // debugging.. 
    
    static private HTint2Object memoUTF = new HTint2Object();
    static private int memoUTFHits;
    static private SparseArrayList[] memoMethod;
    static private int memoMethodHits;
    static private Random methodNameRandomizer;

    static String getUTF(int idx) {
	String ret = (String) memoUTF.get(idx);
	if (ret == null) {
	    ret = UTF8Store._.getUtf8(idx).toString();
	    memoUTF.put(idx, ret);
	}
	if (KEEP_CACHE_STATS)
	    memoUTFHits++;
	return ret;
    }

    static public String format(Method m) {
	if (memoMethod == null) {
	    if (RANDOMIZE_METHOD_NAMES) {
  	      methodNameRandomizer = new Random();
            }
	    memoMethod =
		new SparseArrayList[DomainDirectory.maxContextID() + 1];
	    for (int i = 0; i < memoMethod.length; i++) {
		Type.Context ctx = DomainDirectory.getContext(i);
		if (ctx != null)
		    memoMethod[i] = new SparseArrayList();
	    }
	}
	int cid = m.getCID();
	int uid = m.getUID();
	String j2cName = (String) memoMethod[cid].get(uid);
	if (j2cName == null) {
	    int suffix = (m.getCID() << 16) + m.getUID();
	    Type.Compound t = m.getDeclaringType();
	    TypeName.Scalar n = t.getName().asScalar();
	    String bpName = getUTF(n.getShortNameIndex());
	    String mName = getUTF(m.getSelector().getNameIndex());
	    
	    if (RANDOMIZE_METHOD_NAMES) {
              int rand = methodNameRandomizer.nextInt();
              if (rand<0) {
                rand = -rand;
              }
	      j2cName =  "r" + rand + "_" + JNIFormat._.encode(bpName) + '_'
		       + JNIFormat._.encode(mName) + '_'
		       + suffix;
	    } else {
  	      j2cName = (JNIFormat._.encode(bpName) + '_'
		       + JNIFormat._.encode(mName) + '_'
		       + suffix);
            }
	    memoMethod[cid].set(uid, j2cName);
	}
	if (KEEP_CACHE_STATS)
	    memoMethodHits++;
	return j2cName;
    }

    /**
     * Return the C++ name of this <b>blueprint object</b>.
     **/
    static public String formatBP(Blueprint bp) {
	if (bp instanceof Blueprint.Primitive)
	    return ("bp_" + format(bp.getType().getDomain())
		    + "_" + format(bp));
	else if (bp instanceof Blueprint.Array) {
	    // FIXME: CxxFormat uses a completely different convention
	    // for the shared-state types of arrays.  That convention
	    // should probably be used more consistently.  Strangely
	    // enough, primitive and array shared-state types do *not*
	    // appear in structs.h.  This begs the question of why I
	    // needed to change CxxFormat in the first place.
	    return "bp_" + formatCxxArray((Blueprint.Array)bp).replace('<', 'O').replace('*', 'P').replace('>','C');
	} else {
	    return "bp_" + format(bp);
          }
    }

    /**
     * Return the C++ name of the <b>shared-state object</b> for this
     * type.  bp may be either an instance or shared-state type, it
     * makes no difference.  
     **/
    static public String formatShSt(Blueprint bp) {
	if (bp.isSharedState())
	    bp = bp.getInstanceBlueprint();
	if (!bp.isScalar())
	    bp = bp.getSharedState().getBlueprint();
	return "static_" + format(bp);
    }

    static {
	if (KEEP_CACHE_STATS)
	    new J2cImageCompiler.StatPrinter() {
		public void printStats() {
		    System.err.println("\nJ2cFormat caches:\n");
		    System.err.println("utf8 -> string mapping:");
		    System.err.println("\tValues Allocated: "
				       + memoUTF.keys().length);
		    System.err.println("\tHits:             "
				       + memoUTFHits);
		    System.err.println("Method names:");
		    System.err.println("\tSlots Allocated:  "
				       + memoMethod.length);
		    System.err.println("\tValues Allocated: "
				       + countNonNull(memoMethod));
		    System.err.println("\tHits:             "
				       + memoMethodHits);
		}
	    };
    }
}

    
