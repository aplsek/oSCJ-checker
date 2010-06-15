package ovm.util;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import s3.services.bootimage.Ephemeral;
import s3.util.PragmaTransformCallsiteIR;
import s3.util.PragmaTransformCallsiteIR.BCdead;

/**
 * Like the JikesRVM class of the same name.
 * Could be implemented various ways; this is one.
 *<p />
 * As in the current implementation of VM_Word itself, the rewriting given
 * here has 32-bit-ness built in; this code will have to be munged for 64-bit
 * worlds.
 **/
public class VM_WordArray implements Ephemeral {
    public static final TypeName.Scalar TYPENAME =
        RepositoryUtils.makeTypeName( "ovm/util", "VM_WordArray");
    public static VM_WordArray create( int size)
    throws IRnew {
        return new VM_WordArray( size);
    }
    public VM_Word get( int index) throws IRfetch {
        return VM_Word.fromInt( backing [ index ]);
    }
    public void set( int index, VM_Word v) throws IRstore {
        backing [ index ] = v.asInt();
    }
    public int length() throws IRlength {
        return backing.length;
    }
    /** This is public only so the EphemeralBureau can see it. If you use it
     *  for anything else, your bits will rot.  Returns the value that should
     *  be stored in a target field of (source) type VM_WordArray - that is,
     *  the address of the target ("backing") array, whose contents have been
     *  copied to the image by this method (it is assumed the serializer will
     *  not otherwise encounter and copy the backing array).
     **/
    public Oop target() throws BCdead {
        VM_Address a = VM_Address.fromObject( backing);
        Oop o = a.asOop();
        Blueprint bp = o.getBlueprint();
        VM_Word bytes = VM_Word.fromInt( bp.getVariableSize( o)); // doesn't need fix for arraylets, because 
                                                                  // "bytes" argument does not take arraylets into
                                                                  // account
                                                                  
        a.setBlock( a, bytes); // assume serializer won't otherwise see backing
        return o;
    }
    private VM_WordArray( int size) throws BCdead {
        backing = new int [ size ];
    }
    private final int [] backing;
    
    private static class IR extends PragmaTransformCallsiteIR {
        static {
            final TypeName.Compound arrtn =
                TypeName.Array.make( TypeName.INT, 1);
            r( "new", new Rewriter() {
               protected boolean rewrite() {
                   cursor.addNewArray( T_INT);
                   return true;
               } 
            });
            r( "fetch", new Rewriter() {
                protected boolean rewrite() {
                    cursor.addSimpleInstruction( SWAP);
                    cursor.addFiat( arrtn);
                    cursor.addSimpleInstruction( SWAP);
                    cursor.addSimpleInstruction( IALOAD);
                    return true;
                }
            });
            r( "store", new Rewriter() {
                protected boolean rewrite() {
                    cursor.addRoll( (char)3, (byte)-1);
                    cursor.addFiat( arrtn);
                    cursor.addRoll( (char)3, (byte)1);
                    cursor.addSimpleInstruction( IASTORE);
                    return true;
                }
            });
            r( "length", new Rewriter() {
                protected boolean rewrite() {
                    cursor.addSimpleInstruction( ARRAYLENGTH);
                    return true;
                }
            });
        }
        protected static void r( String n, Rewriter r) {
            register( "ovm.util.VM_WordArray$IR" + n, r);
        }
    }
    static class IRnew    extends IR { }
    static class IRfetch  extends IR { }
    static class IRstore  extends IR { }
    static class IRlength extends IR { }
}
