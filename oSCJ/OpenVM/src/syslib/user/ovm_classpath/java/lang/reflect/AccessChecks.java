package java.lang.reflect;

class AccessChecks {
    private static String getPackageName(Class c) {
	String name = c.getName();
	int lastInd = name.lastIndexOf('.');
	if (lastInd == -1)
	    return "";
	return name.substring(0, lastInd);
    }

    static void checkAccessible(Class caller, Member m, Object rcv)
	throws IllegalAccessException
    {
	if (((AccessibleObject) m).isAccessible())
	    return;
	Class decl = m.getDeclaringClass();
	int modifiers = m.getModifiers();
	if (!Modifier.isPublic(modifiers)
	    || !Modifier.isPublic(decl.getModifiers())) {
	    if (caller != null && caller != decl) {
		boolean failed = Modifier.isPrivate(modifiers);
		if (!failed && Modifier.isProtected(modifiers))
		    failed = !(decl.isAssignableFrom(caller)
			       && (rcv == null || caller.isInstance(rcv)));
		if (!failed)
		    failed = (decl.getClassLoader() != caller.getClassLoader()
			      || !getPackageName(decl).equals(getPackageName(caller)));
		if (failed)
		    throw new IllegalAccessException(m + " inaccessible from "
						     + caller);
	    }
	}
    }

    static void checkReceiver(Member m, Object o) {
        if (o == null) {
            if (!Modifier.isStatic(m.getModifiers()))
                throw new NullPointerException("null receiver");
        }
        else if (!Modifier.isStatic(m.getModifiers()) &&
                 !m.getDeclaringClass().isAssignableFrom(o.getClass())) {
                throw new IllegalArgumentException("wrong receiver type");
        }
    }

    static void checkArgs(Class[] parameterTypes, Object[] args) {
        if ( (args != null && args.length != parameterTypes.length) ||
             ( args == null && parameterTypes.length != 0) )
            throw new IllegalArgumentException("wrong number of args: got " + 
                                               (args == null ? 0 : 
                                                args.length) + " expected " + 
                                               parameterTypes.length);

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].isPrimitive()) {
                // need to handle null, unwrapping and widening
                if (args[i] == null)
                    throw new IllegalArgumentException("null primitive arg");
                checkPrimitive(parameterTypes[i], args[i].getClass());
            }
            else if ( args[i] != null &&
                      !parameterTypes[i].isAssignableFrom(args[i].getClass())) {
                throw new IllegalArgumentException("wrong argument type" + 
                                                   " - expected: " 
                                                   + parameterTypes[i] 
                                                   + " got " 
                                                   + args[i].getClass());
            }
        }
    }

    static private void checkPrimitive(Class param, Class arg) {
        // ensure the arg type is the same as, or can be widened to
        // the parameter type
        if (param == Boolean.TYPE) {
            if (arg == Boolean.class) 
                return;
        } else if (param == Byte.TYPE) {
            if (arg == Byte.class) 
                return;
        } else if (param == Character.TYPE) {
            if (arg == Character.class) 
                return;
        } else if (param == Short.TYPE) {
            if (arg == Byte.class || arg == Short.class ) 
                return;
        } else if (param == Integer.TYPE) {
            if (arg == Byte.class || arg == Short.class || 
                arg == Integer.class) 
                return;
        } else if (param == Long.TYPE) {
            if (arg == Byte.class || arg == Short.class || 
                arg == Integer.class || arg == Long.class) 
                return;
        } else if (param == Float.TYPE) {
            if (arg == Byte.class || arg == Short.class || 
                arg == Integer.class || arg == Long.class || 
                arg == Float.class) 
                return;
        } else if (param == Double.TYPE) {
            if (arg == Byte.class || arg == Short.class || 
                arg == Integer.class || arg == Long.class ||
                arg == Float.class || arg == Double.class) 
                return;
        }
        throw new IllegalArgumentException("wrong type of arg: " + 
                                           arg.getName() + " instead of " + 
                                           param.getName());
    }
}