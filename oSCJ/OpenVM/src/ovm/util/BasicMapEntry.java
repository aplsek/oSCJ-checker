/* BasicMapEntry.java -- a class providing a plain-vanilla implementation of
   the Map.Entry interface; could be used anywhere in java.util
   Copyright (C) 1998 Free Software Foundation, Inc.
This file is part of GNU Classpath.
It has been modified slightly to fit the OVM framework.
*/
package ovm.util;

import ovm.core.OVMBase;


/**
 * a class which implements Map.Entry
 *
 * @author      Jon Zeppieri
 * @author      Ben L. Titzer
 * @version     $Revision: 1.3 $
 * @modified    $Id: BasicMapEntry.java,v 1.3 2003/03/31 19:56:28 jv Exp $
 */
class BasicMapEntry extends OVMBase implements Map.Entry
{
    /** the key */
    Object key;
    /** the value */
    Object value;

  /**
   * construct a new BasicMapEntry with the given key and value
   *
   * @param     newKey       the key of this Entry
   * @param     newValue     the value of this Entry
   */
    BasicMapEntry(Object newKey, Object newValue)
    {
        key = newKey;
        value = newValue;
    }

    /**
   * returns true if <pre>o</pre> is a Map.Entry and
   * <pre>
   * (((o.getKey == null) ? (key == null) :
   * o.getKey().equals(key)) &&
   * ((o.getValue() == null) ? (value == null) :
   * o.getValue().equals(value)))
   * </pre>
   *
   * NOTE: the calls to getKey() and getValue() in this implementation
   * are <i>NOT</i> superfluous and should not be removed.  They insure
   * that subclasses such as HashMapEntry work correctly
   *
   * @param      o        the Object being tested for equality
   */
    public boolean equals(Object o)
    {
        Map.Entry tester;
        Object oTestingKey, oTestingValue;
        Object oKey, oValue;
        if (o instanceof Map.Entry)
            {
                tester = (Map.Entry) o;
                oKey = getKey();
                oValue = getValue();
                oTestingKey = tester.getKey();
                oTestingValue = tester.getValue();
                return (((oTestingKey == null) ? (oKey == null) :
                         oTestingKey.equals(oKey)) &&
                        ((oTestingValue == null) ? (oValue == null) :
                         oTestingValue.equals(oValue)));
            }
        return false;
    }

    /** returns the key */
    public Object getKey()
    {
        return key;
    }

    /** returns the value */
    public Object getValue()
    {
        return value;
    }

    /** the hashCode() for a Map.Entry is
   * <pre>
   * ((getKey() == null) ? 0 : getKey().hashCode()) ^
   * ((getValue() == null) ? 0 : getValue().hashCode());
   * </pre>
   *
   * NOTE: the calls to getKey() and getValue() in this implementation
   * are <i>NOT</i> superfluous and should not be removed.  They insure
   * that subclasses such as HashMapEntry work correctly
   */
    public int hashCode()
    {
        Object oKey = getKey();
        Object oValue = getValue();
        return ((oKey == null) ? 0 : oKey.hashCode()) ^
            ((oValue == null) ? 0 : oValue.hashCode());
    }

    /**
   * sets the value of this Map.Entry
   *
   * @param     newValue         the new value of this Map.Entry
   */
    public Object setValue(Object newValue)
        throws UnsupportedOperationException, ClassCastException,
               IllegalArgumentException, NullPointerException
    {
        Object oVal = value;
        value = newValue;
        return oVal;
    }
}

