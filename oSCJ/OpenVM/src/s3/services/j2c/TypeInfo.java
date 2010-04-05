package s3.services.j2c;
import ovm.core.OVMBase;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeName;
import ovm.util.SparseArrayList;
import s3.core.domain.S3Blueprint;
import s3.services.bootimage.Ephemeral;

abstract class TypeInfo extends OVMBase implements Ephemeral.Void {
    static final TypeName.Array objectArr
	= RepositoryUtils.makeTypeName("[Ljava/lang/Object;").asArray();
    
    static class Simple extends TypeInfo {
	S3Blueprint bp;
	boolean isExact;
	boolean canBeNull;

	// treat ovm.core.domain.Oop as a magic type only in the
	// executive domain
	boolean isOop() {
	    return bp == Context.findContext(bp.getDomain()).OopBP;
	}

	Simple(S3Blueprint bp, boolean isExact, boolean canBeNull) {
	    this.bp = bp;
	    this.isExact = isExact;
	    this.canBeNull = canBeNull;
	}

	TypeInfo merge(TypeInfo _from) {
	    if (equals(_from))
		return this;
	    else /*if (_from instanceof Simple)*/ {
		Simple from = (Simple) _from;
		if (isOop())
		    return make(from.bp, false,
				canBeNull || from.canBeNull);
		else if (from.isOop())
		    return make(bp, false,
				canBeNull || from.canBeNull);
		else if (from.bp.getDomain() != bp.getDomain())
		    throw new LinkageException
			("cross-domain merge "
			 + " of " + bp + " in " + bp.getS3Domain()
			 + " with " + from.bp + " in " + from.bp.getS3Domain()
			 ).unchecked();
		else {
		    S3Blueprint lcs;
		    try {
			lcs = bp.leastCommonSupertypes(from.bp)[0];
		    } catch (LinkageException.Runtime _) {
			// FIXME: Blueprint closure doesn't take the
			// closure of array supertypes.  I've merged
			// element types, but can't wrap the result in
			// an array blueprint.
			assert(bp instanceof S3Blueprint.Array
				  && (from.bp instanceof S3Blueprint.Array)
				  && bp.getType()
					.asArray()
					.getComponentType()
					.isCompound()
				  && from.bp.getType()
					     .asArray()
					     .getComponentType()
					     .isCompound());
			Domain d = bp.getDomain();
			Type.Context ctx = d.getSystemTypeContext();
			try {
			    lcs = (S3Blueprint) d.blueprintFor(objectArr, ctx);
			} catch (LinkageException e) {
			    throw e.unchecked();
			}
		    }
		    return make(lcs,
				(bp == from.bp && isExact && from.isExact),
				(canBeNull || from.canBeNull));
		}
	    }
	}

	TypeInfo mergeWithNull() {
	    return make(bp, isExact, true);
	}

        boolean includes(TypeInfo _from) {
	    Simple from = (Simple) _from;
	    return ((isExact ? bp == from.bp : from.bp.isSubtypeOf(bp))
		    && (isExact ? from.isExact : true)
		    && (from.canBeNull ? canBeNull : true));
	}

	S3Blueprint getBlueprint() { return bp; }
	boolean includesNull() { return canBeNull; }
	boolean exactTypeKnown() { return isExact; }

	boolean includesType(S3Blueprint _bp) {
	    return isExact ? this.bp == _bp : _bp.isSubtypeOf(this.bp);
	}

	public boolean equals(Object _other) {
	    if (getClass() == _other.getClass()) {
		Simple other = (Simple) _other;
		return (bp == other.bp && isExact == other.isExact
			&& canBeNull == other.canBeNull);
	    }
	    else return false;
	}
    }

    abstract TypeInfo merge(TypeInfo from);
    abstract TypeInfo mergeWithNull();
    abstract boolean includes(TypeInfo t);
    abstract boolean includesNull();

    abstract S3Blueprint getBlueprint();
    abstract boolean includesType(S3Blueprint bp);
    abstract boolean exactTypeKnown();

    static final boolean KEEP_STATS = J2cImageCompiler.KEEP_STATS;
    
    static private SparseArrayList[][] cache =
	new SparseArrayList[4][DomainDirectory.maxContextID() + 1];
    static {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j <= DomainDirectory.maxContextID(); j++)
                cache[i][j] = new SparseArrayList();
    }
    static private int[] alloced = new int[4];
    static private int[] returned = new int[4];

    static private String[] names = new String[] {
	"inExact, hasNull",
	"inExact, nonNull",
	"exact,   hasNull",
	"exact,   nonNull"
    };
    
    static TypeInfo make(S3Blueprint bp, boolean isExact, boolean canBeNull) {
	int kind = (isExact
		    ? (canBeNull ? 3 : 2)
		    : (canBeNull ? 1 : 0));
	SparseArrayList cache = TypeInfo.cache[kind][bp.getCID()];
	int bpNum = bp.getUID();

	TypeInfo ret = (TypeInfo) cache.get(bpNum);
	if (ret == null) {
	    ret = new Simple(bp, isExact, canBeNull);
            cache.set(bpNum, ret);
	    if (KEEP_STATS)
		alloced[kind]++;
	}
	if (KEEP_STATS)
	    returned[kind]++;
	return ret;
    }

    static {
	if (KEEP_STATS)
	    new J2cImageCompiler.StatPrinter() {
		public void printStats() {
		    System.err.println("\nTypeInfo cache:");
		    for (int i = 0; i < names.length; i++)
			System.err.println(names[i] + ": "
					   + alloced[i] + " objects served "
					   + returned[i] + " calls");
		}
	    };
    }
}
