package ovm.core.domain;

import ovm.core.Executive;
import ovm.core.services.memory.VM_Address;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.RepositoryUtils;

import s3.core.domain.S3Field;

/**
 * <code>ReflectiveField</code> objects provide a simple and somewhat
 * type-safe inteface to user-domain fields through <code>get</code>
 * and <code>set</code> methods.  The consistent use of
 * reflective wrapper objects also permits whole-program analysis.
 * Reflective wrappers must be created at image build time (before our
 * static analysis completes).  When a <code>ReflectiveField</code> is
 * created, a static analysis can account for the use of this field in
 * VM-internal code.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 **/
public class ReflectiveField {
    private Type.Context ctx;
    private Selector.Field name;
    private Oop sharedStateOop;

    protected ReflectiveField(Type.Context ctx, Selector.Field name) {
	this.ctx = ctx;
	this.name = name;
	// We only need to registerRoot for static fields in the user
	// domain.  Instance fields had better be loaded access them,
	// and ED types had better be loaded at image build time.
	ctx.getDomain().registerField(name);
    }

    public Selector.Field getName() { return name; }

    protected Field lookup()  {
	try {
	    Type.Scalar t = ctx.typeFor(name.getDefiningClass()).asScalar();
	    Field f = t.getField(name.getUnboundSelector());
	    if (t.isSharedState())
		sharedStateOop = ((Type.Class) t).getSingleton();
	    return f;
	} catch (LinkageException e) {
	    throw Executive.panicOnException(e);
	}
    }

    public Oop getStaticReceiver() {
	if (sharedStateOop == null)
	    lookup();
	return sharedStateOop;
    }

    /**
     * A wrapper for fields whole type is
     * {@link ovm.core.services.memory.VM_Address}, rather than a
     * reference or primitive type.  Because <code>VM_Address</code>
     * is a second-class type, standard get/set operations are not
     * defined on reflective fields of this type.  The only operation
     * that is defined is <code>addressWithin</code>.<p>
     *
     * FIXME: This functionality is needed, but not really supported
     * within the rest of the <code>ovm.core.domain</code> API.  See
     * bug 370.
     **/
    public static class Address extends ReflectiveField {
	private Field f;
	public Address(Domain d, TypeName.Compound ft,
		       TypeName.Scalar cls, String name) {
	    super(d.getSystemTypeContext(),
		  RepositoryUtils.selectorFor(cls, name, ft));
	}
	public VM_Address addressWithin(Oop obj) {
	    return ((S3Field) lookup()).bug370addressWithin(obj);
	}
    }

    public static class Reference extends ReflectiveField {
	private Field.Reference f;

	public Reference(Domain d, TypeName.Compound ft,
			 TypeName.Scalar cls, String name) {
	    this(d, RepositoryUtils.selectorFor(cls, name, ft));
	}
	public Reference(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Reference(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public Oop get(Oop obj) {
	    if (f == null) f = (Field.Reference) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, Oop val) {
	    if (f == null) f = (Field.Reference) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}

	public VM_Address addressWithin(Oop obj) {
	    if (f == null) f = (Field.Reference) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.addressWithin(obj);
	}
    }

    public static class Boolean extends ReflectiveField {
	private Field.Boolean f;

	public Boolean(Domain d, TypeName.Scalar cls, String name) {
	    this(d, RepositoryUtils.selectorFor(cls, name, TypeName.BOOLEAN));
	}
	public Boolean(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Boolean(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public boolean get(Oop obj) {
	    if (f == null) f = (Field.Boolean) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, boolean val) {
	    if (f == null) f = (Field.Boolean) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}
    }

    public static class Byte extends ReflectiveField {
	private Field.Byte f;

	public Byte(Domain d, TypeName.Scalar t, String name) {
	    this(d, RepositoryUtils.selectorFor(t, name, TypeName.BYTE));
	}
	public Byte(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Byte(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public byte get(Oop obj) {
	    if (f == null) f = (Field.Byte) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, byte val) {
	    if (f == null) f = (Field.Byte) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}
    }

    public static class Short extends ReflectiveField {
	private Field.Short f;

	public Short(Domain d, TypeName.Scalar t, String name) {
	    this(d, RepositoryUtils.selectorFor(t, name, TypeName.SHORT));
	}
	
	public Short(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Short(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public short get(Oop obj) {
	    if (f == null) f = (Field.Short) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, short val) {
	    if (f == null) f = (Field.Short) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}
    }

    public static class Character extends ReflectiveField {
	private Field.Character f;

	public Character(Domain d, TypeName.Scalar t, String name) {
	    this(d, RepositoryUtils.selectorFor(t, name, TypeName.CHAR));
	}
	
	public Character(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Character(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public char get(Oop obj) {
	    if (f == null) f = (Field.Character) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, char val) {
	    if (f == null) f = (Field.Character) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}
    }

    public static class Integer extends ReflectiveField {
	private Field.Integer f;
	
	public Integer(Domain d, TypeName.Scalar t, String name) {
	    this(d, RepositoryUtils.selectorFor(t, name, TypeName.INT));
	}
	
	public Integer(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Integer(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public int get(Oop obj) {
	    if (f == null) f = (Field.Integer) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, int val) {
	    if (f == null) f = (Field.Integer) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}
    }

    public static class Long extends ReflectiveField {
	private Field.Long f;
	
	public Long(Domain d, TypeName.Scalar t, String name) {
	    this(d, RepositoryUtils.selectorFor(t, name, TypeName.LONG));
	}

	public Long(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Long(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public long get(Oop obj) {
	    if (f == null) f = (Field.Long) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, long val) {
	    if (f == null) f = (Field.Long) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}
    }

    public static class Float extends ReflectiveField {
	private Field.Float f;
	
	public Float(Domain d, TypeName.Scalar t, String name) {
	    this(d, RepositoryUtils.selectorFor(t, name, TypeName.FLOAT));
	}

	public Float(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Float(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public float get(Oop obj) {
	    if (f == null) f = (Field.Float) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, float val) {
	    if (f == null) f = (Field.Float) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}
    }

    public static class Double extends ReflectiveField {
	private Field.Double f;
	
	public Double(Domain d, TypeName.Scalar t, String name) {
	    this(d, RepositoryUtils.selectorFor(t, name, TypeName.DOUBLE));
	}

	public Double(Domain d, Selector.Field sel) {
	    this(d.getSystemTypeContext(), sel);
	}

	public Double(Type.Context ctx, Selector.Field sel) {
	    super(ctx, sel);
	}

	public double get(Oop obj) {
	    if (f == null) f = (Field.Double) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    return f.get(obj);
	}

	public void set(Oop obj, double val) {
	    if (f == null) f = (Field.Double) lookup();
	    if (obj == null)
		obj = getStaticReceiver();
	    f.set(obj, val);
	}
    }
}
