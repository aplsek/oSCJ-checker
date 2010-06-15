// $Header: /p/sss/cvs/OpenVM/src/ovm/core/domain/Oop.java,v 1.40 2007/06/03 01:25:48 baker29 Exp $

package ovm.core.domain;
import ovm.core.domain.ObjectModel.PragmaModelOp;
import ovm.core.services.memory.VM_Address;
/**
 * Every object in the language being implemented is visible to the
 * implementing language as an <code>Oop</code>. Each <code>Oop</code> has a
 * {@link Blueprint} which contains information about the object's layout,
 * and a {@link Type}, which contains information about the object's class.
 **/
public interface  Oop
//  Perhaps Oop should not implement Ephemeral ... or, once Ephemeral is in
//  fact implemented (and the mechanism has been settled for how to specify
//  an Ephemeral's precise behavior), the ephemerality of Oop should not be
//  the same as that of VM_Address.  Oop references should be left as reference
//  types and tagged accordingly; because they are known to have Blueprints,
//  they are fair game for the collector to see, and that will simplify a lot
//  of code that would otherwise have to be uninterruptible or risk references
//  going bad when the collector moves things (all the pesky risks of dealing
//  with VM_Addresses).
    extends s3.services.bootimage.Ephemeral // FIXME ovm->s3 dependency... this is to be changed -- jv 25.9.02
{
    /** Disinvite error-prone duplication
     *  in ObjectModel implementations
     **/
    String ID = "Lovm/core/domain/Oop;";
    
    /** Get the <code>Blueprint</code> of this object
     *  @throws PragmaModelOp <em>this is a pragma</em>
     **/
    Blueprint getBlueprint() throws PragmaModelOp;
    
    /** Return the <em>identityHashCode</em> of this object: a cheaply computed
     *  hash that is independent of whether the object overrides
     *  {@link java.lang.Object#hashCode() hashCode} but must not change once
     *  an object is created. This is what <code>hashCode()</code>, if not
     *  overridden, should return. If objects cannot move after creation, the
     *  address will do fine. If objects can move (e.g. copying GC) the
     *  requirement for unchanging hashcode precludes using the address.
     *  Different objects are allowed to have the same hash; in fact a trivial
     *  implementation could return a fixed value--that would degenerate hash
     *  maps into lists and change the complexity order of some algorithms, but
     *  not affect correctness. A common solution is to use a reasonable number
     *  of object header bits to save a hash - the more bits, the better hash
     *  maps work. Because this method, therefore, <em>may</em> depend on
     *  the object model, Oop is a sensible place to define it.
     * <p>
     *  Jan was concerned that defining the method on Oop could interfere with
     *  using different hash strategies in different domains, but that isn't
     *  a problem; the Oop is linked to a domain via blueprint/type/context
     *  and the hash method would be able to defer to the domain if necessary.
     * <p>
     *  <strong>Note:</strong> In code that may run hosted, use
     *  {@link ObjectModel#identityHash(Object)} rather than
     *  {@link VM_Address#fromObject(Object)}.{@link VM_Address#asOop()
     *  asOop()}.{@link Oop#getHash() getHash()} because in the hosted setting
     *  {@link VM_Address#fromObject(Object) fromObject} allocates image space!
     *  (All this to avoid calling the method
     *  java.lang.System.identityHashCode() so we can avoid having a
     *  java.lang.System.)
     * <p>
     *  With some implementations of {@link ObjectModel}, the object's
     *  address is simply returned as the hash. <strong>Those implementations
     *  are usable only with nonmoving GC.</strong>
     * @return a hash value
     * @throws PragmaModelOp <em>this is a pragma</em>
     **/
    int getHash() throws PragmaModelOp;
    
    /** Advance past the header.
     *  @return VM_Address of the beginning of object data, after the header.
     *  @throws PragmaModelOp <em>this is a pragma</em>
     **/
    VM_Address headerSkip() throws PragmaModelOp;
    
    /**Get a reference associated with this Oop (but not a user-visible reference
     * field) by the object model. {@link ObjectModel#maxReferences()} returns the
     * number of such references there may be; for a given Oop, any of these may be
     * <code>null</code>.
     * @param k Which reference attribute to return. <strong>If k is less than zero,
     * or not less than {@link ObjectModel#maxReferences() maxReferences()}, this
     * method is not required to fail in any predictable or desirable way.</strong>
     * @return The indexed reference attribute, or <code>null</code>.
     * @throws PragmaModelOp
     */
    Oop getReference( int k) throws PragmaModelOp;
    
    /**
     *  Unchecked (magic) narrowing cast of Oop to any particular interface
     *  that extends Oop
     *  ({@link ovm.services.monitors.MonitorMapper MonitorMapper}, for example)
     *  so the (equally magic) instance methods of that interface can be used.
     *  @throws BCeatcast <em>this is a pragma</em>
     **/
    Oop asAnyOop() throws VM_Address.BCfiatcast;
    
    /**
     * Return a representation of the object's type and address.
     * There are two reasons this can't be called <code>toString</code>.
     * 1. <code>javac</code> will not emit <code>invokeinterface</code>
     * for an interface method that overrides an <code>Object</code> method.
     * 2. There are two different <code>toString</code>-related tricks that
     * <code>Oop</code> should be able to do.  This one returns a
     * meta-description&emdash;the object's type and address.  The other
     * should be <code>reflectiveToString</code> and have the effect of
     * reflectively invoking <code>toString</code> when the <code>Oop</code>
     * is in another domain, and translating the result.
     **/
    String metaToString() throws PragmaModelOp;

    interface WithUpdate extends Oop {
        String ID = "Lovm/core/domain/Oop$WithUpdate;";

        /**Update the blueprint reference associated with this Oop by the object
         * model.
         * By convention the blueprint is not included among an object model's
         * numbered references; to update them, see {@link #updateReference(int,Oop)}.
         *<p/>
         * If the object model represents a particular reference in a way that does
         * not need updating when the target moves, the corresponding update may be
         * a no-op. For example, the reference might be found by indexing into an
         * array; the index does not change, and the array entry will be updated
         * when the array is scanned. It follows that this method is only reliable
         * for updating an existing reference when the target moves, but not for
         * storing an arbitrary new reference in the object model.
         * @param newValue the new value
         **/    
        void updateBlueprint( Blueprint newValue) throws PragmaModelOp;

        /**Update a reference associated with this Oop (but not a user-visible
         * reference field) by the object model. {@link ObjectModel#maxReferences()}
         * returns the number of such references there may be; they need not all be
         * non-<code>null</code>.
         * By convention the blueprint is not included among an object model's
         * numbered references; to update that, see {@link #updateBlueprint}.
         *<p/>
         * If the object model represents a particular reference in a way that does
         * not need updating when the target moves, the corresponding update may be
         * a no-op. For example, the reference might be found by indexing into an
         * array; the index does not change, and the array entry will be updated
         * when the array is scanned. It follows that this method is only reliable
         * for updating an existing reference when the target moves, but not for
         * storing an arbitrary new reference in the object model.
         * @param k Which reference attribute to update. <strong>If k is less than
         * zero, or not less than {@link ObjectModel#maxReferences() maxReferences()},
         * this method is not required to fail in any predictable or desirable
         * way.</strong>
         * @param newValue the new value
         **/
        void updateReference( int k, Oop newValue) throws PragmaModelOp;
    }
} // End of Oop
