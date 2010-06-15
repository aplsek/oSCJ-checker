package ovm.util;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.VM_Address;
import ovm.util.VM_WordArray.IRfetch;
import ovm.util.VM_WordArray.IRlength;
import ovm.util.VM_WordArray.IRnew;
import ovm.util.VM_WordArray.IRstore;
import s3.services.bootimage.Ephemeral;
import s3.util.PragmaTransformCallsiteIR.BCdead;

/**
 * Like the JikesRVM class of the same name.
 * Could be implemented various ways; this is one.
 *<p />
 * As in the current implementation of VM_Address itself, the rewriting given
 * here has 32-bit-ness built in; this code will have to be munged for 64-bit
 * worlds.
 **/
public class VM_AddressArray implements Ephemeral {
    public static final TypeName.Scalar TYPENAME =
        RepositoryUtils.makeTypeName( "ovm/util", "VM_AddressArray");
    public static VM_AddressArray create( int size)
    throws IRnew {
        return new VM_AddressArray( size);
    }
    public VM_Address get( int index) throws IRfetch {
        return backing [ index ];
    }
    public void set( int index, VM_Address v) throws IRstore {
        backing [ index ] = v;
    }
    public int length() throws IRlength {
        return backing.length;
    }
    /** This is public only so the EphemeralBureau can see it. If you use it
     *  for anything else, your bits will rot.  Returns the value that should
     *  be stored in a target field of (source) type VM_AddressArray - that is,
     *  the address of the target ("backing") array, whose contents have been
     *  copied to the image by this method (it is assumed the serializer will
     *  not otherwise encounter and copy the backing array).
     **/
    public Oop target() throws BCdead {
        VM_Address a = VM_Address.fromObject( new int [ backing.length ]);
        Oop o = a.asOop();
        Blueprint.Array bp = o.getBlueprint().asArray();
        for ( int i = 0; i < backing.length; ++ i )
            bp.addressOfElement( o, i).setAddress( backing [ i ]); // build time
        return o;
    }
    private VM_AddressArray( int size) throws BCdead {
        backing = new VM_Address [ size ];
    }
    private final VM_Address [] backing;
}
