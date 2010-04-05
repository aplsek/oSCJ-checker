package ovm.core.repository;

import ovm.core.OVMBase;
import ovm.util.Arrays;
import ovm.util.HashMap;
import ovm.util.List;
import ovm.util.UnicodeBuffer;
import s3.services.bootimage.Ephemeral;
import s3.util.PragmaTransformCallsiteIR.BCdead;

/**
 * Static utility methods for repository manipulation. This allows
 * applications using a repository to manipulate it without knowing
 * repository-specific or implementation-specific details.
 *
 **/
public final class RepositoryUtils extends OVMBase {

    /**
     * This cache is quite useful during bootimage generation... but
     * may not be such a good thing at run time... the internal hash
     * table can be set to null to turn off caching.
     *
     * note that we do assume that there are no collisions between the
     * different namespaces that are stored in the cache. See the uses
     * of put for more detail.
     **/
    //  private 
    public static final class Cache extends OVMBase implements Ephemeral.Void {
        static HashMap names2selectors = new HashMap();
        private static final Cache probe = new Cache("", "");

        // FIX ME FIX ME: This cache is neither scope nor thread safe. So
        // it has been disabled in clear() - DH July 9, 2004
        static Object probe(String d, String s)  {
            if (names2selectors == null)
                return null;
            probe.definingClass = d;
            probe.selector = s;
            return names2selectors.get(probe);
        }

        /**
         * NB: comment written by jt trying to understand cg's code ; don't trust this comment :)
         * @param d the fully qualified name of the defining class in a slash '/' separated form
         * @param s the name of a selector for a member of this class 
         * @param o the associated Selector object
         * 
         * Other uses of this function:
         * 1/ used with <code>d</code> being a package name, <code>s<code> being a classname,
         *    and <code>o<code> being the associated TypeName object
         * 2/ used with <code>d</code> being an empty string, when storing unbound selectors
         */
        static void put(String d, String s, Object o) {
            if (names2selectors == null)
                return;
            names2selectors.put(new Cache(d, s), o);
        }

        static public void clear() { names2selectors = null; /*new HashMap();*/ }
        
        String definingClass;
        final int hashCode;
        String selector;
        
        Cache(String d, String s) {
            definingClass = d;
            selector = s;
            hashCode = d.hashCode() ^ s.hashCode();
        }
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            try {
                Cache entry = (Cache) o;
                return selector.equals(entry.selector)
                    && definingClass.equals(entry.definingClass);
            } catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            return hashCode;
        }
    }

    public static int asUTF(String string) {
	UnicodeBuffer buf = UnicodeBuffer.factory().wrap(string);
        return UTF8Store._.installUtf8(buf);
    }

    /**
     * Given a repository and <code>String</code> objects representing an
     * unbound field selector and its defining class, return a repository bound
     * field selector object for this field.
     *
     * <p>See {@link UnboundSelector}
     * for unbound selector format; class names should be the fully qualified
     * name representation of the type name to be created (for example,
     * Ljava/lang/Object;). (See JVM Spec $2.7.5)
     *
     * @param definingClassStr the defining class of the field
     * @param selStr the unbound field selector, represented as a string
     * @return the bound field selector object
     *
     * FIXME: no caching here?
     **/
    public static Selector.Field fieldSelectorFor(String definingClassStr,
						  String selStr) {
        UnboundSelector.Field usel = makeUnboundSelector(selStr).asField();
	TypeName.Compound tn = makeTypeName(definingClassStr).asCompound();
	return Selector.Field.make(usel, tn);
    }

    /**
     * @param fullClassName the fully qualified name of a class, as found internally,
     * i.e. Ljava/lang/String;, [Ljava/lang/Objecct;, [I, I.
     * @return an associated TypeName object
     */
    public static TypeName makeTypeName(String fullClassName)
	throws BCdead
    {
        Object object = Cache.probe(fullClassName, "");
        if (object != null)
            return (TypeName) object;

	UnicodeBuffer buf = UnicodeBuffer.factory().wrap(fullClassName);
        TypeName name = TypeName.parse(buf);
        Cache.put(fullClassName, "", name);
        return name;
    }

    /** @param packageName delimited with /
     **/
    public static TypeName.Scalar makeTypeName(String packageName,
					       String className)
	throws BCdead
    {

        Object object = Cache.probe(packageName, className);
        if (object != null)
            return (TypeName.Scalar) object;

        TypeName.Scalar name = TypeName.Scalar.make(asUTF(packageName),
						    asUTF(className));
        Cache.put(packageName, className, name);
        return name;
    }

    /**
     * Make an unbound selector for a field or method from a
     * <code>String</code> according to the following
     * format: <code> &lt;name&gt; ':' &lt;descriptor&gt; </code>.
     *
     * Descriptor format is described in
     * {@link ovm.core.repository.Descriptor}
     * @param str the selector string in the above format
     * @return the <code>UnboundSelector</code> for this
     *         field or method
     **/
    public static UnboundSelector makeUnboundSelector(String str)
	throws BCdead
    {
        Object object = Cache.probe("", str);
        if (object != null)
            return (UnboundSelector) object;

	UnicodeBuffer buf = UnicodeBuffer.factory().wrap(str);
        UnboundSelector selector = UnboundSelector.parse(buf);
        Cache.put("", str, selector);
        return selector;
    }

    /**
     * Given a <code>String</code> objects representing an
     * unbound method selector and its defining class, return a repository
     * bound method selector object for this method.
     *
     * <p>See {@link UnboundSelector}
     * for unbound selector format; class names should be the fully qualified
     * name representation of the type name to be created (for example,
     * Ljava/lang/Object;). (See JVM Spec $2.7.5)
     *
     * @param definingClassStr the defining class of the method
     * @param selStr the unbound method selector, represented as a string
     * @return the bound method selector object
     **/
    public static Selector.Method methodSelectorFor(String definingClassStr,
						    String selStr)
	throws BCdead
    {
        Object object = Cache.probe(definingClassStr, selStr);
        if (object != null)
            return (Selector.Method) object;

        UnboundSelector.Method sel = makeUnboundSelector(selStr).asMethod();
	TypeName.Compound tn = makeTypeName(definingClassStr).asCompound();
        Selector.Method method = Selector.Method.make(sel, tn);
        Cache.put(definingClassStr, selStr, method);
        return method;
    }

    /**
     * Given a <code>String</code> objects representing an
     * unbound method or field selector and its defining class, return a
     * repository bound selector object for this method or field.
     *
     * <p>See {@link UnboundSelector}
     * for unbound selector format; class names should be the fully qualified
     * name representation of the type name to be created (for example,
     * Ljava/lang/Object;). (See JVM Spec $2.7.5)
     *
     * @param definingClassStr the defining class of the member (method or
     *                         field)
     * @param selStr the unbound selector, represented as a string
     * @return the bound selector object
     **/
    public static Selector selectorFor(String definingClassStr,
				       String selStr)
	throws BCdead
    {
        Object object = Cache.probe(definingClassStr, selStr);
        if (object != null)
            return (Selector) object;

        UnboundSelector unboundsel = makeUnboundSelector(selStr);
	TypeName.Compound tn = makeTypeName(definingClassStr).asCompound();
        Selector selector = null;
        if (unboundsel.isMethod())
            selector = Selector.Method.make(unboundsel.asMethod(), tn);
        else
            selector = Selector.Field.make(unboundsel.asField(), tn);
        Cache.put(definingClassStr, selStr, selector);
        return selector;
    }

    /**
     * This is the canonical verion of selectorFor for fields.
     * Return the selector for field <i>name</i> of type <i>type</i>
     * declared in <i>declaringType</i>
     **/
    public static Selector.Field selectorFor(TypeName.Compound declaringType,
					     String name,
					     TypeName type) {
        Descriptor.Field desc = Descriptor.Field.make(type);
        UnboundSelector.Field usel = UnboundSelector.Field.make(asUTF(name),
								desc);
        return Selector.Field.make(usel, declaringType);
    }

    /**
     * This is the canonical verion of selectorFor for methods.
     * Return the selector for field <i>name</i> with signature
     * <i>arguments</i> returning <i>returnType</i> declared in
     * <i>declaringType</i>.
     **/
    public static Selector.Method selectorFor(TypeName.Compound declaringType,
					      TypeName returnType,
					      String name,
					      TypeName[] arguments) {
        List list = arguments==null ? null : Arrays.asList(arguments);
        Descriptor.Method desc = Descriptor.Method.make(list, returnType);
        UnboundSelector.Method usel =
	    UnboundSelector.Method.make(asUTF(name), desc);
        return Selector.Method.make(usel, declaringType);
    }

    public static String utfIndexAsString(int idx) {
        return UTF8Store._.getUtf8(idx).toString();
    }

} // End of RepositoryUtils
