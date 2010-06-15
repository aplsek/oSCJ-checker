package s3.services.bootimage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.format.JavaFormat;
import ovm.util.HashMap;
import ovm.util.HashSet;
import ovm.util.Map;
import s3.core.S3Base;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3MemberResolver;
/**
 * Put here the Java reflection related functionality that is needed in
 * the bootimage creation process. Note this class maintains state
 * and is not thread safe.
 * @author Krzysztof Palacz
 **/
public final class ReflectionSupport extends S3Base {

    /**
     * Convert the <code>java.lang.reflect.Field f</code> into the
     * corresponding OVM <code>RepositorySelector</code>. One important
     * difference between the two is that <code>Field</code> instances are
     * bound to a class whereas OVM selectors are singletons which can be
     * shared and are not bound to a class object. Moved from JDK2OVM (KP).
     * @param f field
     * @return the corresponding Selector or
     *          <code>null</code> if <code>f == null</code>.
     **/
    public static Selector.Field selectorFor(Field f) {
	Selector.Field sel = (Selector.Field)fields2selectors.get(f);
	if (sel == null) {
	    TypeName type = typeNameForClass(f.getType());
	    String uselString = f.getName() + ":" + type;
            boolean isStatic = Modifier.isStatic( f.getModifiers());
	    sel = fieldSelectorFor(f.getDeclaringClass(), uselString, isStatic);
	    fields2selectors.put(f, sel);
	}
	return sel;
    }

    public static void dumpOvmFieldStats() { // FIXME 555
        d( "Fields looked up: " + fieldsLookedUp +
           " (" + fieldsLookedUpRecklessly + " recklessly)");
        fieldsLookedUp = fieldsLookedUpRecklessly = 0;        
    }
    
    public static ovm.core.domain.Field ovmFieldFor( Field f, Blueprint bp) {
        Type.Scalar t = bp.getType().asScalar();
        Type.Context tc = t.getContext();
        if ( typeContext != tc ) {
            if ( null != typeContext ) // FIXME 555
                dumpOvmFieldStats();
            typeContext = tc;
            fields2ovmFields.clear();
         }
        ovm.core.domain.Field ovmFld =
            (ovm.core.domain.Field)fields2ovmFields.get( f);
        if ( null != ovmFld )
            return ovmFld;
        ++ fieldsLookedUp;
        Selector.Field sel = selectorFor( f);
        ovmFld = t.getField( sel);// FIXME: replace with:  ovmFld = t.getField( sel.getUnboundSelector());
        if ( null == ovmFld ) { // FIXME 555
            ++ fieldsLookedUpRecklessly;
            UnboundSelector.Field usel = sel.getUnboundSelector();
            ovmFld = t.isSharedState()
                     ? S3MemberResolver.resolveStaticField( (Type.Class)
                                                    bp.getInstanceBlueprint().getType(),
                                                    usel, null)
                     : S3MemberResolver.resolveField(((Type.Class)t), usel, null);
        }
        fields2ovmFields.put( f, ovmFld);
        return ovmFld;
    }
    
    public static Selector.Method selectorFor(Method method) {
	if (method == null)
	    return null;
	String uselString = method.getName() + ":(";
        Class[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            uselString += typeNameForClass(params[i]);
        }
        uselString += ')';
        uselString += typeNameForClass(method.getReturnType());
        boolean isStatic = Modifier.isStatic( method.getModifiers());
	return methodSelectorFor(method.getDeclaringClass(), uselString, isStatic);
    }

    private static final HashMap classes2names = new HashMap();
    private static final HashMap names2classes = new HashMap();
    private static final HashMap fields2selectors = new HashMap();
    private static final Map fields2ovmFields = new HashMap();
    private static Type.Context typeContext = null;
    private static int fieldsLookedUp = 0, fieldsLookedUpRecklessly = 0; // FIXME 555
     
    private static final HashSet problematicClasses = new HashSet();



    public static Class classForTypeName(TypeName tn) {
	Class cl = (Class)names2classes.get(tn);
	if (cl == null) {
	    try {
                if ( tn.isGemeinsam() )
                    tn = ((TypeName.Gemeinsam)tn).getInstanceTypeName();
		cl = Class.forName(JavaFormat._.format(tn));
		classes2names.put(cl, tn);
		names2classes.put(tn, cl);
	    } catch (ClassNotFoundException e) {
		System.err.println("Asked "+tn+
				   "for " + JavaFormat._.format(tn) + " got " + e);
		return null;
	    } catch (java.lang.LinkageError e) {
		if (!problematicClasses.contains(tn)) {
		    d("problem instantiating " + tn + " in host vm");
		    problematicClasses.add(tn);
		}
		System.err.println("Asked "+tn+
				   "for " + JavaFormat._.format(tn) + " got " + e);
		e.printStackTrace();
		return null;
	    } catch (RuntimeException e) {
		d("problem instantiating " + tn + " in host vm: " + e);
		e.printStackTrace();
	    }
	} 
	return cl;
    }

    public static TypeName typeNameForClass(Class cl) {
        TypeName name = (TypeName)classes2names.get(cl);
        if (name == null) {
            name = JavaFormat._.parseTypeName(cl.getName());
            classes2names.put(cl, name);
	    names2classes.put(name, cl);
        }
        return name;
    }
    
    public static String asClassFileName(TypeName.Scalar tn) {
	return JavaFormat._.format(tn).replace('.', '/') + ".class";
    }
    
    public static Selector.Method methodSelectorFor(Class declaringClass, 
					     String uselString,
                                             boolean isStatic) {
	UnboundSelector.Method usel =
	    RepositoryUtils.makeUnboundSelector(uselString).asMethod();
	
	TypeName decl = typeNameForClass(declaringClass);
        if ( isStatic )
            decl = decl.asScalar().getGemeinsamTypeName();
	return Selector.Method.make(usel, decl.asCompound());
    }

    public static Selector.Field fieldSelectorFor(Class declaringClass, 
						   String uselString,
                                                   boolean isStatic) {
	UnboundSelector.Field usel = 
	    RepositoryUtils.makeUnboundSelector(uselString).asField();

	TypeName decl = typeNameForClass(declaringClass);
        if ( isStatic )
            decl = decl.asScalar().getGemeinsamTypeName();
	return Selector.Field.make(usel, decl.asCompound());
    }

    // FIXME why does this take a Domain instead of a Type.Context?
    public static S3Blueprint blueprintFor(Class definingClass, Domain dom) {
	try {
	    TypeName.Compound name =
		ReflectionSupport.typeNameForClass(definingClass).asCompound();
	    return (S3Blueprint) dom.blueprintFor(name, 
						  dom.getApplicationTypeContext());
	} catch (LinkageException e) { throw e.unchecked(); }
    }
}
