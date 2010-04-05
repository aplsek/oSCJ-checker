package s3.util;

import ovm.core.OVMBase;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.TypeName;
import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryMember;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import ovm.util.IdentityHashMap;
import ovm.util.Map;
import ovm.util.VM_AddressArray;
import ovm.util.VM_WordArray;
import s3.core.domain.S3ByteCode;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Field;
import s3.services.bootimage.Ephemeral;
import s3.services.bootimage.ImageObserver;
import s3.util.PragmaTransformCallsiteIR.BCnothing;
import s3.core.domain.S3Type;


/** This class, as it stands, is a puny proof-of-concept test bed for the
 *  evolution of the Type/Field/Method metadata in step with the object
 *  graph.  How it is ultimately done will likely bear little resemblance
 *  to this, but this may be enough to handle the existing
 *  cases for the nonce, and maybe even reveal some of the tricky bits.
 *  Populated here manually instead of automatically, for now.
 **/
public class EphemeralBureau implements Ephemeral.Void /* in more ways than one */ {
    
    private static Map map = new IdentityHashMap();

    static {
        final S3Domain exd = (S3Domain)DomainDirectory.getExecutiveDomain();

        registerForExecutive( VM_Address.typeNameVMA,
        new EphemeralBureaucrat( TypeName.Primitive.INT) {
            public Field fieldFor( RepositoryMember.Field rmf,
                                   Type.Compound declarer) {
		final Type type = exd.INT;
                // a Field.Integer that says INT if you ask its type, but
                // still returns a descriptor matching name:LVM_Address;.
                // Well, that's still tacky, but it may be sufficient for
                // present purposes.
                return new S3Field.Integer( rmf, (S3Type.Scalar) declarer) {
                    public void setUnrefined( Oop dst, Object value) {
                        VM_Address a = (VM_Address)value;
                        set( dst, (a != null ? a.asInt() : 0));
                    }
                    // comment out to return type from descriptor:
                    public Type getType() { return type; }
                };
            }
        });

        registerForExecutive( VM_Word.typeNameVMW,
        new EphemeralBureaucrat( TypeName.Primitive.INT) {
            public Field fieldFor( RepositoryMember.Field rmf,
                                   Type.Compound declarer) {
		final Type type = exd.INT;
                // a Field.Integer that says INT if you ask its type, but
                // still returns a descriptor matching name:LVM_Word;.
                // Well, that's still tacky, but it may be sufficient for
                // present purposes.
                return new S3Field.Integer( rmf, (S3Type.Scalar) declarer) {
                    public void setUnrefined( Oop dst, Object value) {
                        VM_Word w = (VM_Word)value;
                        set( dst, w.asInt());
                    }
                    // comment out to return type from descriptor:
                    public Type getType() { return type; }
                };
            }
        });

        registerForExecutive( VM_WordArray.TYPENAME,
        new EphemeralBureaucrat( JavaNames.arr_int) {
            public Field fieldFor( RepositoryMember.Field rmf,
                                   Type.Compound declarer) {
		final Type type = getType(declarer.getContext());
                return new S3Field.Reference( rmf, (S3Type.Scalar) declarer) {
                    public void setUnrefined( Oop dst, Object value) {
                        set( dst, null == value
                                ? null :  ((VM_WordArray)value).target());
                    }
                    public Type getType() { return type; }
                };
            }
        });

        registerForExecutive( VM_AddressArray.TYPENAME,
        new EphemeralBureaucrat( JavaNames.arr_int) {
            public Field fieldFor( RepositoryMember.Field rmf,
                                   Type.Compound declarer) {
		final Type type = getType(declarer.getContext());
                return new S3Field.Reference( rmf, (S3Type.Scalar) declarer) {
                    public void setUnrefined( Oop dst, Object value) {
                        set( dst, null == value
                                ? null :  ((VM_AddressArray)value).target());
                    }
                    public Type getType() { return type; }
                };
            }
        });
    }
    
    static void register( Type.Context ctx, TypeName t, EphemeralBureaucrat f)
    throws PragmaTransformCallsiteIR.BCdead {
	Map m1 = (Map) map.get(ctx);
	if (m1 == null) {
	    m1 = new IdentityHashMap();
	    map.put(ctx, m1);
	}
        m1.put( t, f);
    }
    
    static void registerForExecutive( TypeName.Compound t, EphemeralBureaucrat f)
    throws PragmaTransformCallsiteIR.BCdead {
        Domain ed = DomainDirectory.getExecutiveDomain();
        Type.Context ctx = ed.getSystemTypeContext();
        register( ctx, t, f);
    }
    
    public static Field fieldFor( RepositoryMember.Field rmf,
                                  Type.Compound declarer)
    throws PragmaTransformCallsiteIR.ReturnNull {
	TypeName tn = rmf.getDescriptor().getType();
	Map m1 = (Map) map.get(declarer.getContext());
        EphemeralBureaucrat eb = (m1 == null
				  ? null
				  : (EphemeralBureaucrat)m1.get(tn));
        return null == eb ? null : eb.fieldFor(rmf, declarer);
    }
    
    public static Type typeFor( Type t)
    throws PragmaTransformCallsiteIR.ReturnNull {
	TypeName tn = t.getUnrefinedName();
	Map m1 = (Map) map.get(t.getContext());
        EphemeralBureaucrat eb = (m1 == null
				  ? null
				  : (EphemeralBureaucrat)m1.get(tn));
        return null == eb ? null : eb.getType(t.getContext());
    }
    
    static abstract class EphemeralBureaucrat {
        EphemeralBureaucrat( TypeName tn) { type = tn; }
        public abstract
        Field fieldFor( RepositoryMember.Field rmf, Type.Compound declarer);
	Type getType(Type.Context ctx) {
	    return ctx.typeForKnown(type);
	}

        public final TypeName type;
    }
}
