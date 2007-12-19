/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import junit.framework.TestCase;

/**
 * Implement boilerplate tests.
 * Currently there is only one method, testEHCS, which tests equals, hashCode, 
 * clone, and serialization.
 */
public abstract class ICUTestCase extends TestCase {
    private static final Object[] EMPTY_ARGS = {};
    private static final Class[] EMPTY_CLASSES = {};
        
    private static final Locale oldLocale = Locale.getDefault();
    private static final ULocale oldULocale = ULocale.getDefault();
    private static final java.util.TimeZone oldJTimeZone = java.util.TimeZone.getDefault();
    private static final TimeZone oldITimeZone = TimeZone.getDefault();
                
    // TODO: what's the best way to check this?
    public static final boolean testingWrapper = true;

    protected void setUp() throws Exception {
        super.setUp();
        Locale.setDefault(Locale.US);
        ULocale.setDefault(ULocale.US);
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("PST"));
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
    }
        
    protected void tearDown() throws Exception {
        ULocale.setDefault(oldULocale);
        Locale.setDefault(oldLocale);
        TimeZone.setDefault(oldITimeZone);
        java.util.TimeZone.setDefault(oldJTimeZone);
        super.tearDown();
    }

    private static final Object test = new Object();
        
    /**
     * Assert that two objects are _not_ equal.  Curiously missing from Assert.
     * @param lhs an object to test, may be null
     * @param rhs an object to test, may be null
     */
    public static void assertNotEqual(Object lhs, Object rhs) {
        if (lhs == null) {
            if (rhs == null) fail("null equals null");
        } else {
            if (lhs.equals(rhs)) {
                fail(lhs.toString() + " equals " + rhs);
            }
        }
    }
        
    public static void assertNotEqual(long lhs, long rhs) {
        if (lhs == rhs) {
            fail("values are equal: " + lhs);
        }
    }
        
    /**
     * Test whether equality, hashCode, clone, and serialization work as expected.  
     * Equals(Object) is assumed to return false (not throw an exception) if passed 
     * null or an object of an incompatible class.
     * Hashcodes must be equal iff the two objects compare equal.  No attempt is made to
     * evaluate the quality of the hashcode distribution, so (in particular) degenerate 
     * hashcode implementations will pass this test.
     * Clone will be tested if the method "clone" is public on the class of obj.  
     * It is assumed to return an object that compares equal to obj.
     * Serialization will be tested if object implements Serializable or Externalizable.
     * It is assumed the serialized/deserialized object compares equal to obj.
     * @param obj the object to test
     * @param eq an object that should compare equal to, but is not the same as, obj.  
     *     it should be assignable to the class of obj.
     * @param neq a non-null object that should not compare equal to obj.  
     *     it should be assignable to the class of obj.
     */
    public static void testEHCS(Object obj, Object eq, Object neq) {
        if (obj == null || eq == null || neq == null) {
            throw new NullPointerException();
        }
        Class cls = obj.getClass();
        if (!(cls.isAssignableFrom(eq.getClass()) && cls.isAssignableFrom(neq.getClass()))) {
            throw new IllegalArgumentException("unassignable classes");
        }
                
        // reflexive
        assertEquals(obj, obj);
                
        // should return false, not throw exception
        assertNotEqual(obj, test);
        assertNotEqual(obj, null);
                
        // commutative
        assertEquals(obj, eq);
        assertEquals(eq, obj);
                
        assertNotEqual(obj, neq);
        assertNotEqual(neq, obj);
                
        // equal objects MUST have equal hashes, unequal objects MAY have equal hashes
        assertEquals(obj.hashCode(), eq.hashCode());
                
        Object clone = null;
        try {
            // look for public clone method and call it if available
            Method method_clone = cls.getMethod("clone", EMPTY_CLASSES);
            clone = method_clone.invoke(obj, EMPTY_ARGS);
            assertNotNull(clone);
        }
        catch(NoSuchMethodException e) {
            // ok
        }
        catch(InvocationTargetException e) {
            // ok
        }
        catch(IllegalAccessException e) {
            // ok
        }
                
        if (clone != null) {
            assertEquals(obj, clone);
            assertEquals(clone, obj);
        }
                
        if (obj instanceof Serializable || obj instanceof Externalizable) {
            Object ser = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(clone);
                oos.close();
                                
                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bis);
                ser = ois.readObject();
                ois.close();
            }
            catch(IOException e) {
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
            catch(ClassNotFoundException e) {
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
                        
            if (ser != null) {
                assertEquals(obj, ser);
                assertEquals(ser, obj);
                assertEquals(obj.hashCode(), ser.hashCode());
            }
        }
    }

    /**
     * Fail if the arrays are not equal.  To be equal, the arrays must
     * be the same length, and each element in the left array must compare
     * equal to the corresponding element of the right array.
     * Also fails if one of the objects is not an array.
     * @param lhs the left array
     * @param rhs the right array
     */
    public static void assertArraysEqual(Object lhs, Object rhs) {
        Class lcls = lhs.getClass();
        Class rcls = rhs.getClass();
        if (!(lcls.isArray() && rcls.isArray())) {
            fail("objects are not arrays");
        }
        String result = arraysAreEqual(lhs, rhs);
        if (result != null) {
            fail(result);
        }
    }

    /**
     * Fail if the arrays are equal.  Also fails if one or the other 
     * argument is not an array.
     * @param lhs the left array
     * @param rhs the right array
     */
    public static void assertArraysNotEqual(Object lhs, Object rhs) {
        Class lcls = lhs.getClass();
        Class rcls = rhs.getClass();
        if (!(lcls.isArray() && rcls.isArray())) {
            fail("objects are not arrays");
        }
        String result = arraysAreEqual(lhs, rhs);
        if (result == null) {
            fail("arrays are equal");
        }
    }
        
    // slow but general
    private static String arraysAreEqual(Object lhsa, Object rhsa) {
        int lhsl = Array.getLength(lhsa);
        int rhsl = Array.getLength(rhsa);
        if (lhsl != rhsl) {
            return "length " + lhsl + " != " + rhsl;
        }
        boolean lhsaA = lhsa.getClass().getComponentType().isArray();
        boolean rhsaA = rhsa.getClass().getComponentType().isArray();
        if (lhsaA != rhsaA) {
            return (lhsaA ? "" : "non-") + "array != " + (rhsaA ? "" : "non-") + "array";
        }
        for (int i = 0; i < lhsl; ++i) {
            Object lhse = Array.get(lhsa, i);
            Object rhse = Array.get(rhsa, i);
            if (lhse == null) {
                if (rhse != null) {
                    return "null != " + rhse;
                }
            } else {
                if (lhsaA) {
                    String result = arraysAreEqual(lhse, rhse);
                    if (result != null) {
                        if (result.charAt(0) != '[') {
                            result = " " + result;
                        }
                        return "[" + i + "]" + result;
                    }
                } else {
                    if (!lhse.equals(rhse)) {
                        return lhse.toString() + " != " + rhse;
                    }
                }
            }
        }
        return null;
    }
        
    // much more painful and slow than it should be... partly because of the
    // oddness of clone, partly because arrays don't provide a Method for 
    // 'clone' despite the fact that they implement it and make it public.
    public static Object cloneComplex(Object obj) {
        Object result = null;
        if (obj != null) {
            Class cls = obj.getClass();
            if (cls.isArray()) {
                int len = Array.getLength(obj);
                Class typ = cls.getComponentType();
                result = Array.newInstance(typ, len);
                boolean prim = typ.isPrimitive();
                for (int i = 0; i < len; ++i) {
                    Object elem = Array.get(obj, i);
                    Array.set(result, i, prim ? elem : cloneComplex(elem));
                }
            } else {
                result = obj; // default
                try {
                    Method cloneM = cls.getMethod("clone", null);
                    result = cloneM.invoke(obj, null);
                }
                catch (NoSuchMethodException e) {
                }
                catch (IllegalAccessException e) {
                }
                catch (InvocationTargetException e) {
                }
            }
        }
        return result;
    }
}
