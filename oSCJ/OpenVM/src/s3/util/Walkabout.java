package s3.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import ovm.core.OVMBase;
import ovm.util.HashMap;
import ovm.util.ArrayList;
import ovm.core.services.memory.VM_Address;
import s3.services.bootimage.Ephemeral;

/**
 * A Walkabout is a graph traversal visitor inspired by the Palsberg and Jay
 * paper of the same title and related to the Runabout.
 * 
 * The walkabout is given a set of roots and will reflectively traverse
 * the objects transitively reachable from those roots. The traversal is 
 * controlled by user-defined   <i>advices</i>.  An advice is what amounts
 * to a closure that is applied to the object about to be visited, and which
 * returns the next object to visit.<p>
 * 
 * There are four kinds of advices:
 *     <code>ObjectAdvice</code> applies to each object in the graph;
 *     <code>ClassAdvice</code> applies to each class object in the graph;
 *     <code>FieldAdvice</code> applies to each field of an object in the graph;
 *     <code>StaticFieldAdvice</code> applies to each static field of a class in
 *            the graph.
 * These advices are interfaces that must be implemented by user-supplied classes.
 * 
 * Advices are associated to Java reference types. The association is made 
 * explicit by the   <code>targetClass()</code>   method of each advice. This
 * method returns the class or interface to which this advice will apply.<p>
 * 
 * Object and Class advices implement the method 
 *      <code>Object beforeObject(Object)</code>
 * which has the following semantics. Each time the Walkabout is ready to
 * visit an object (or class), the applicable advice is called with that object
 * as argument. It then returns the object to visit. The return value of 
 * the before method is used by the Walkabout for further traversal, i.e.,
 * the Walkabout will visit all of the fields of the object returned by before.<p>
 * 
 * In many cases the <code>beforeObject()</code> method is used as a visitor and 
 * the argument is the return value. Returning  <code>null</code>  from an
 * advice has the effect of shortening the traversal. Returning another 
 * object has the effect of replacing an object by another and ignoring the 
 * original object's fields.<p>
 * 
 * We call the set of all objects returned by a  <code>beforeObject()</code>  method
 * the processed objects, the method <code>getProcessed()</code> returns that
 * set. The default advice is the identity function thus, if  nothing is specified 
 * the processed set is the entire object graph.<p>
 * 
 * Field advices must implement the method:
 *       <code>Object beforeField(Object src, Field f, Object val)</code>.
 * The semantics of the method is that the field  <code>f</code>  of object 
 * <code>src</code> has value  <code>val</code>.  The <code>targetClass()</code>
 * method of a field advice applies to the class of the source. The object
 * returned by this method will be visited.<p>
 * 
 * The Walkabout ensure that <code>beforeObejct()</code> will be called once per
 * object and that <code>beforeField()</code> will be called once per field
 * of each source object. 
 * For a given object <code>src</code>, <code>beforeField()</code> is 
 * called after <code>beforeObject()</code>.<p>
 * 
 * Advices are selected at runtime according to the following algorithm.
 * Given an object o of class C:<pre>
 *  (1) Find the set of applicable advices for which C is a subtype of the 
 *      target class.
 *  (2) Find the set of most specific applicable advices, an advice is more
 *      specific than another if it's target class is a subtype of the other
 *      advice. The <code>moreSpecificThan(a,b)</code> method can be used to
 *      override the subtype relation and ensure that the algorithm will
 *      select <code>a</code> over <code>b</code> no matter what.
 *  (3) If the set is empty use the default behavior.  
 *      If the cardinality of the set is one, apply the advice.
 *      Otherwise throw an <code>AmbiguousMatchError</code>.</pre><p>
 * 
 * The <code>Walkabout</code> public interface consists of the following
 * methods: <pre>
 *           <code>register</code> to register an advice, 
 *           <code>visit</code> to add a root
 *           <code>visitFieldsOnly</code> to add the fields of an object to the roots,
 *           <code>start</code> to start the traversal from the roots, and 
 *           <code>getProcessed</code> to get the object that were processed.
 *           <code>moreSpecificThan</code> to disambiguate conflicting advices.</pre><p>
 *
 * The order of visit is not specified. <code>Walkabout</code> maintains a
 * worklist of objects to visit. The main loop takes an object out of the
 * worklist and applies the appropriate advice, yielding a replacement object
 * (possibly identical to the target or <code>null</code>).
 * If the replacement object is not null, the Walkabout selects a field
 * advice and applies it to every field of the replacement object.
 * The results of each field advice application are added to the worklist. <p>
 *
 *
 * @author Jan Vitek
 **/

public abstract class Walkabout extends OVMBase implements Ephemeral {

    /**
     * Record the fact that this object has been seen, and return true
     * if it has not been seen before.
     **/
    protected abstract boolean markAsSeen(Object o);

    boolean traversePrimitives_;
    private static final Object NULL = new Object();

    private static Class[] addClass(Class cls, Class[] classes) {
        Class[] result = new Class[classes.length + 1];
        for (int i = 0; i < classes.length; i++) {
            Class class1 = classes[i];
            if (class1 == cls)
                throw new AmbiguousMatchError(
                    "Trying to register a duplicate advice for  " + class1);
            result[i] = class1;
        }
        result[classes.length] = cls;
        return result;
    }

    // ****************** Fields ******************************
    /** Per class user-specified actions. **/
    private final HashMap beforeClassAdvice_ = new HashMap();
    private Class[] beforeClassClasses_ = new Class[0];
    /** Per field user-specified actions. **/
    private final HashMap beforeFieldAdvice_ = new HashMap();
    private Class[] beforeFieldClasses_ = new Class[0];
    /** Per object user-specified actions. **/
    private final HashMap beforeObjectAdvice_ = new HashMap();
    private Class[] beforeObjectClasses_ = new Class[0];

    /** Per object actions that run once, before the object's fields
     ** are traversed **/
    private final HashMap afterObjectAdvice_ = new HashMap();
    private Class[] afterObjectClasses_ = new Class[0];

    /** Per static field user-specified actions. **/
    final HashMap beforeStaticFieldAdvice_ = new HashMap();
    Class[] beforeStaticFieldClasses_ = new Class[0];
    /** Existing mirrors. **/
    protected final HashMap class2mirrors_ = new HashMap();
    private HashMap resolvedClassAdvices_ = new HashMap();
    private HashMap resolvedFieldAdvices_ = new HashMap();
    private HashMap resolvedObjectAdvices_ = new HashMap();
    private HashMap resolvedAfterObjectAdvices_ = new HashMap();
    HashMap resolvedStaticFieldAdvices_ = new HashMap();
    /** flag to indicate whether to traverse static fields. **/
    final boolean traverseStatics_;
    private final int expectedSize_;

    public Walkabout(boolean staticsFlag, boolean primitiveFlag) {
        this(staticsFlag, primitiveFlag, 10000);
    }
    public Walkabout(boolean staticsFlag, boolean primitiveFlag, int sz) {
        traverseStatics_ = staticsFlag;
        traversePrimitives_ = primitiveFlag;
        expectedSize_ = sz;
    }

    /** Invoke user code.  **/
    protected Object before(Object o) {
        final boolean isClass = (o.getClass() == java.lang.Class.class);
        Advice clue =
            (isClass)
                ? getAdvice(
                    (Class) o,
                    beforeClassAdvice_,
                    beforeClassClasses_,
                    resolvedClassAdvices_)
                : getAdvice(
                    o.getClass(),
                    beforeObjectAdvice_,
                    beforeObjectClasses_,
                    resolvedObjectAdvices_);

        if (clue != null)
            o =
                (isClass)
                    ? ((ClassAdvice) clue).beforeObject(o)
                    : ((ObjectAdvice) clue).beforeObject(o);
        return o;
    }

    Advice getAdvice(
        Class cls,
        HashMap advices,
        Class[] advisees,
        HashMap resolvedAdvices) {

        Object clue = resolvedAdvices.get(cls);
        if (clue == NULL)   // N.B. NULL != null :)
            return null;
        if (clue != null)
            return (Advice) clue;

        Class mostspec = mostSpecific(cls, advisees);
        if (mostspec == null) {
            resolvedAdvices.put(cls, NULL);
            return null;
        }

        clue = advices.get(mostspec);
        assert(clue != null);
        resolvedAdvices.put(cls, clue);
        return (Advice) clue;
    }

    // ********************************************************
    // ***************** ClassMirror **************************
    // ********************************************************

    protected ClassMirror getMirror(Class tgt) {
        if (tgt == null)
            return null;
        ClassMirror mirror = (ClassMirror) class2mirrors_.get(tgt);
        if (mirror == null) {
            mirror = new ClassMirror(tgt);
            class2mirrors_.put(tgt, mirror);
            getMirror(tgt.getSuperclass());
        }
        return mirror;
    }

    private HashMap overrides = new HashMap();
    public void moreSpecificThan(Class master, Class slave) {
        overrides.put(master, slave);
    }

    /**
     * Return the most specific supertype of the target class chosen
     * amongst the set of candidates. An ambiguity occurs if there are several 
     * equally specific types. The overides hashmap can be used to disambiguate,
     * if that fails an error is reported.
     * @param target 
     * @param candidates the advices
     * @return most specific super type of target in candidates or null if none
     * @throws AmbiguousMatchError if multiple equally specific types
     */
    private final Class mostSpecific(Class target, Class[] candidates)
        throws AmbiguousMatchError {

        Class result = null;
        for (int i = 0; i < candidates.length; i++) {
            Class candidate = candidates[i];
            if (!candidate.isAssignableFrom(target) || result == candidate)
                continue;
            //if the result is undefined or less specific than the candidate
            //keep the candidate
            if (result == null || result.isAssignableFrom(candidate)) {
                result = candidate;
                continue;
            }
            // if the result is more specific than the candidate we
            // can discard the candidate and proceeed
            if (candidate.isAssignableFrom(result))
                continue;

            // ok, we have two genuine supertypes of target
            //check for overides
            if (overrides.get(result) != null)
                continue;
            else if (overrides.get(candidate) != null)
                result = candidate;
            else
                throw new AmbiguousMatchError(
                    "Ambiguous match for " + target + ": "
		    + candidate + " and " + result + " both apply");
        }
        return result;
    }
    /* Typical call sequences:
     * startWalkabout -->           // repeat for all object
     *        before  -->           // user advice -- can drop object
     *       process  -->           // for all field-defining classes
     *       traverse -->           // for all fields
     *    beforeField -->           // user advice -- can drop object
     */
    /**
    * Invoked for each object on the worklist, the process method
    * ensures that all fields of the target object are visited. The
    * order of visit is implementation specific. This implementation
    * walks up the class hierarchy visiting the fields defined at
    * each level. If the target is a Class, its static fields
    * will be visited class.
    *
    * Static fields are visited in two cases, if instance of Class
    * class are explicitly added to the worklist, or if the
    * traverseStatics_ flag is true (which causes the Class instance to
    * be added to the worklist).
    * @param    target     may be an instance, a class or null
    **/
    protected void process(Object target) {
        if (target == null)
            return;
	//System.err.println("walk " + target.getClass() + "
	//instance");
	ObjectAdvice after =
	    (ObjectAdvice) getAdvice(target.getClass(),
				     afterObjectAdvice_,
				     afterObjectClasses_,
				     resolvedAfterObjectAdvices_);
	if (after != null)
	    target = after.beforeObject(target);
	if (target == null)
	    return;
	
        Class targetClass = target.getClass();
        if (targetClass == java.lang.Class.class) {
            getMirror((Class) target).traverseClass();
        } else
            while (targetClass != null) {
                ClassMirror mirror = getMirror(targetClass);
                if (traverseStatics_)
                    addObject_(targetClass);
                mirror.traverse(
                    target,
                    getAdvice(
                        target.getClass(),
                        beforeFieldAdvice_,
                        beforeFieldClasses_,
                        resolvedFieldAdvices_));
                targetClass = targetClass.getSuperclass();
            }
    }

    /**
     * Register a handler. The handler associates a class with a visit
     * method. Duplicate are overwritten. Note: the handler should not
     * registered on an interface as the current walkabout does not visit them.
     * @param o a description of the handler
     **/
    public void register(Advice o) {
        if (o instanceof FieldAdvice) {
            beforeFieldAdvice_.put(o.targetClass(), o);
            beforeFieldClasses_ =
                addClass(o.targetClass(), beforeFieldClasses_);
        }
        if (o instanceof ObjectAdvice) {
            beforeObjectAdvice_.put(o.targetClass(), o);
            beforeObjectClasses_ =
                addClass(o.targetClass(), beforeObjectClasses_);
        }
        if (o instanceof StaticFieldAdvice) {
            beforeStaticFieldClasses_ =
                addClass(o.targetClass(), beforeStaticFieldClasses_);
            beforeStaticFieldAdvice_.put(o.targetClass(), o);
        }
        if (o instanceof ClassAdvice) {
            beforeClassClasses_ =
                addClass(o.targetClass(), beforeClassClasses_);
            beforeClassAdvice_.put(o.targetClass(), o);
        }
    }

    /**
     * Register a peice of advice that is called exactly once for each
     * object encountered.  The advice is called after the object has
     * been added to the worklist, but before it's fields are
     * traversed.  If beforeObject returns anything other than it's
     * argument, <em>o</em>, that value will be traversed in o's
     * place.  If beforeObject returns null, traversal stops at o.
     *
     * @param adv the advice.
     *
     * <!-- void -->
     *
     */
    public void registerAfter(ObjectAdvice adv) {
	afterObjectClasses_ = addClass(adv.targetClass(), afterObjectClasses_);
	afterObjectAdvice_.put(adv.targetClass(), adv);
    }

    // A linked list would be appropriate for the treadmill-style
    // breadth-first walk, but the order in which we walk data reall
    // isn't all that important, and we where doing depth-first
    // anyway, so we might as well maintain the stack in an array.
    //
    // If we could combine heap scanning and image building in a real
    // GC pass, it would make more sense to let VM_Address map
    // objects to build-time VM_Addresses that have a color, next,
    // and previous pointers.  This would make the seen and processed
    // tables, as well as the worklist obsolete.
    protected ArrayList worklist_ = new ArrayList();
    /** Starts the traversal **/
    public void start() {
        while (!worklist_.isEmpty()) {
            Object o = worklist_.remove(worklist_.size() - 1);
            process(o);
        }
    }

    /** Add the object to the worklist. **/
    public void visit(Object o) {
        addObject_(o);
    }

    void addObject_(Object o) {
	if (o != null)
	    o = before(o);
	if (markAsSeen(o))
	    worklist_.add(o);
    }

    /**
     * The fields of the target object are added to the set of root,
     * but target object isn't. The target may still end up in the
     * worklist, if transitively referenced from one of the fields.
     * The target will not be added to the set of processed objects.
     *
     * FIXME: Why do we call process instead of addObject_/visit?
     * FIXME: What is the distinction between addObject_ and visit?
     */
    public void visitFieldsOnly(Object target) {
        process(before(target));
    }

    static public class AmbiguousMatchError extends Error {
        AmbiguousMatchError(String s) {
            super(s);
        }
    }

    //	********************************************************
    // ***************** Walkabout.Advice *********************
    // ********************************************************

    public interface Advice {
        Class targetClass();
    }

    public interface ClassAdvice extends Advice {
        /** The object returned by this method will be visited by the
         * walkabout. If the method returns a different object than the
         * argument this specifies a replacement. If the method returns
         * null, the walkabout will not visit anything. This advice applies
         * to class instances only.
         * @param o object that is about to be visited.
         * @return the object that will be visited.
         */
        Object beforeObject(Object o);
    } // End of ObjectAdvice

    /**
     * A ClassMirror is data structure used to optimize graph
     * traversal by caching the reflective information for its target
     * class. Primitive fields are ignored.
     */
    protected class ClassMirror {
        protected final Field[] instanceFields_;
        protected final boolean isArray_;
        protected final Field[] staticFields_;
        protected final Class target_;

        /**
         * Depending on the on the enclosing walkabout parameters we may
         * retain static fields and primitive fields or not. 
         * @param trgt the class for which this mirror is constructed
         */
        public ClassMirror(final Class trgt) {
            this.target_ = trgt;
            if (target_.isArray()) {
                isArray_ = true;
                staticFields_ = null;
                instanceFields_ = null;
                return;
            }
            isArray_ = false;

            Field[] fields = target_.getDeclaredFields();
            Field[] referenceFields = new Field[fields.length];
            int refcnt = 0, staticCnt = 0, instanceCnt = 0;
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                field.setAccessible(true);
                if (traversePrimitives_ || !field.getType().isPrimitive()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        if (traverseStatics_) {
                            referenceFields[refcnt++] = field;
                            staticCnt++;
                        }
                    } else {
                        referenceFields[refcnt++] = field;
                        instanceCnt++;
                    }
                }
            }

            staticFields_ = new Field[staticCnt];
            instanceFields_ = new Field[instanceCnt];
            int j = 0, k = 0;
            for (int i = 0; i < refcnt; i++) {
                Field field = referenceFields[i];
                if (Modifier.isStatic(field.getModifiers()))
                    staticFields_[j++] = field;
                else
                    instanceFields_[k++] = field;
            }
        }

        private void doTraverse(Object o, Advice clue, boolean isClass) {
            if (isArray_ && isClass)
                return; // FIXME: is meant to avoid
            // traversing array classes ... not sure what good it does.

            Field[] fields = (isClass) ? staticFields_ : instanceFields_;

            for (int i = fields.length - 1; i >= 0; i--) {
                Field field = fields[i];
                Object value = null;
                try {
                    value = field.get(o);
                } catch (IllegalAccessException e) {
                    fail(e);
                }
                if (value == null)
                    continue;
                if (clue != null)
                    value =
                        (isClass)
                            ? ((StaticFieldAdvice) clue).beforeField(
                                o,
                                field,
                                value)
                            : ((FieldAdvice) clue).beforeField(o, field, value);
		// Some parts of the OVM's image build process
		// (isInOurRTL bullshit) seem to draw a distinction
		// between objects that the Walkabout has seen and
		// those it has processed.  While it might make sense
		// to run ObjectAdvice before calling addObject_, it
		// can lead to weird failures
		try {
// 		    if (o instanceof ovm.core.execution.RuntimeExports)
// 			System.out.println("walking " + field +
// 					   " of " + o + ": " + value);
		    addObject_(value);
		} catch (ArrayStoreException e) {
		    System.err.println("error adding " + o + "." + field);
		    throw e;
		}
            }
        }

        /** traverse fields of Scalars and Classes. The advice used here is
         * is a field advice. */
        protected void traverse(Object o, Advice clue) {
            if (isArray_)
                traverseArray(o);
            else
                doTraverse(o, clue, false);
        }

        public void traverseArray(Object arr) {
            Class compType = arr.getClass().getComponentType();
            /**  We used to have: 'if (compType == null  ||' because at
            	   some point 'Class.forName("[Z").getComponentType()'
            	   could return null, this is not to the case anymore. **/
            if (compType.isPrimitive())
                return;
            int length = Array.getLength(arr);
            for (int i = 0; i < length; i++)
                addObject_(Array.get(arr, i));
        }

        protected void traverseClass() {
	    doTraverse(
                    target_,
                    getAdvice(
                        target_,
                        beforeStaticFieldAdvice_,
                        beforeStaticFieldClasses_,
                        resolvedStaticFieldAdvices_),
                    true);
	}
    } // End of ClassMirror

    public interface FieldAdvice extends Advice {
        /** The object returned by this method is to be visited. 
         * If the method returns a different object than the argument this specifies 
         * a replacement. If the method returns null, the walkabout will not visit anything.
         * @param o object where we are visiting a field
         * @param f the field that we are visiting
         * @param value the value of the field (primitives are wrapped)
         * @return the value that should be visited, null for none
         **/
        Object beforeField(Object o, Field f, Object value);
    }

    public interface ObjectAdvice extends Advice {
        /** This method is called when the walkabout is about to visit an object. 
         * The return value is the object that will in fact be visited, thus a return
         * that is different than the argument means that the returned object, and not
         * the argument,  will be visited. Null means that the nothing further needs
         * to be done.        
         * @param o object that is about to be visited.
          * @return the object to visit or null
         **/
        Object beforeObject(Object o);
    }

    public interface StaticFieldAdvice extends Advice {
        /** The object returned by this method will be visited by the
         * walkabout. If the method returns a different object than
         * the argument this specifies a replacement. If the method
         * returns null, the walkabout will not visit anything. This
         * method is only invokde for static fields.
         * @param o   owner of the  field
         * @param f   the field to visit
         * @param v   value of the field (primitives are wrapped)
         * @return the value that should be further visited, null for none
         **/
        Object beforeField(Object o, Field f, Object v);
    }

    static public class IgnoreObjectAdvice implements ObjectAdvice {
        private final Class cls_;
        public IgnoreObjectAdvice(Class cls) {
            cls_ = cls;
        }
        public Class targetClass() {
            return cls_;
        }
        public Object beforeObject(Object o) {
            return null;
        }
    }
    static public class IgnoreFieldAdvice implements FieldAdvice {
        private final Class cls_;
        public IgnoreFieldAdvice(Class cls) {
            cls_ = cls;
        }
        public Class targetClass() {
            return cls_;
        }
        public Object beforeField(Object o, Field f, Object v) {
            return null;
        }
    }
    static public class IgnoreClassAdvice implements ClassAdvice {
        private final Class cls_;
        public IgnoreClassAdvice(Class cls) {
            cls_ = cls;
        }
        public Class targetClass() {
            return cls_;
        }
        public Object beforeObject(Object o) {
            return null;
        }
    }
    static public class IgnoreStaticFieldAdvice implements StaticFieldAdvice {
        private final Class cls_;
        public IgnoreStaticFieldAdvice(Class cls) {
            cls_ = cls;
        }
        public Class targetClass() {
            return cls_;
        }
        public Object beforeField(Object o, Field f, Object v) {
            return null;
        }
    }
}
