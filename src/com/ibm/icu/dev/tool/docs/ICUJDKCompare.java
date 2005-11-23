/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.tool.docs;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Compare ICU4J and JDK APIS.
 *
 * TODO: compare protected APIs.  Reflection on Class allows you
 * to either get all inherited methods with public access, or get methods
 * on the particular class with any access, but no way to get all
 * inherited methods with any access.  Go figure.
 */
public class ICUJDKCompare {
    static final boolean DEBUG = false;

    static final Class[] pairClasses = {
        com.ibm.icu.lang.UCharacter.class, java.lang.Character.class,
        com.ibm.icu.lang.UCharacter.UnicodeBlock.class, java.lang.Character.UnicodeBlock.class,
        com.ibm.icu.text.BreakIterator.class, java.text.BreakIterator.class,
        com.ibm.icu.text.Collator.class, java.text.Collator.class,
        com.ibm.icu.text.DateFormat.class, java.text.DateFormat.class,
        com.ibm.icu.text.DateFormatSymbols.class, java.text.DateFormatSymbols.class,
        com.ibm.icu.text.DecimalFormat.class, java.text.DecimalFormat.class,
        com.ibm.icu.text.DecimalFormatSymbols.class, java.text.DecimalFormatSymbols.class,
        com.ibm.icu.text.UFormat.class, java.text.Format.class,
        com.ibm.icu.text.MessageFormat.class, java.text.MessageFormat.class,
        com.ibm.icu.text.NumberFormat.class, java.text.NumberFormat.class,
        com.ibm.icu.text.SimpleDateFormat.class, java.text.SimpleDateFormat.class,
        com.ibm.icu.util.Calendar.class, java.util.Calendar.class,
        com.ibm.icu.util.Currency.class, java.util.Currency.class,
        com.ibm.icu.util.GregorianCalendar.class, java.util.GregorianCalendar.class,
        com.ibm.icu.util.SimpleTimeZone.class, java.util.SimpleTimeZone.class,
        com.ibm.icu.util.TimeZone.class, java.util.TimeZone.class,
        com.ibm.icu.util.ULocale.class, java.util.Locale.class,
        com.ibm.icu.util.UResourceBundle.class, java.util.ResourceBundle.class
    };

    static final String[] pairs = new String[pairClasses.length];
    static {
        for (int i = 0; i < pairs.length; ++i) {
            pairs[i] = pairClasses[i].getName();
        }
    }

    public static void main(String[] args) {
        System.out.println("ICU and Java API Comparison");
    System.out.println("ICU Version " + com.ibm.icu.util.VersionInfo.ICU_VERSION);
        System.out.println("JDK Version " + System.getProperty("java.version"));

        for (int i = 0; i < pairs.length; i += 2) {
            try {
                compare(pairClasses[i], pairClasses[i+1]);
            }
            catch (Exception e) {
                System.out.println("exception: " + e);
                System.out.println("between " + pairs[i] + " and " + pairs[i+1]);
                e.printStackTrace();
            }
        }
    }

    static class MorC {
        private Method mref;
        private Constructor cref;

        MorC(Method m) {
            mref = m;
        }

        MorC(Constructor c) {
            cref = c;
        }

        int getModifiers() {
            return mref == null ? cref.getModifiers() : mref.getModifiers();
        }

        Class getReturnType() {
            return mref == null ? void.class : mref.getReturnType();
        }

        Class[] getParameterTypes() {
            return mref == null ? cref.getParameterTypes() : mref.getParameterTypes();
        }

        String getName() {
            return mref == null ? "<init>" : mref.getName();
        }
    }

    static void compare(Class class1, Class class2) throws Exception {
        String n1 = class1.getName();
        String n2 = class2.getName();

        MorC[] conss1 = getMorCArray(class1.getConstructors());
        MorC[] conss2 = getMorCArray(class2.getConstructors());

        Map cmap1 = getMethodMap(conss1);
        Map cmap2 = getMethodMap(conss2);

        MorC[] meths1 = getMorCArray(class1.getMethods());
        MorC[] meths2 = getMorCArray(class2.getMethods());

        Map map1 = getMethodMap(meths1);
        Map map2 = getMethodMap(meths2);

        Field[] fields1 = class1.getFields();
        Field[] fields2 = class2.getFields();

        Set set1 = getFieldSet(fields1);
        Set set2 = getFieldSet(fields2);

        PrintWriter pw = new PrintWriter(System.out);

    Map diffConss = diffMethodMaps(cmap2, cmap1);
    Map diffMeths = diffMethodMaps(map2, map1);
    Set diffFields = diffFieldSets(set2, set1);

    if (diffConss.size() + diffMeths.size() + diffFields.size() > 0) {
        pw.println("\n============\nAPI in " + n2 + " missing from " + n1);
        if (diffConss.size() > 0) {
        pw.println("\nCONSTRUCTORS");
        dumpMethodMap(diffConss, pw);
        }
        if (diffMeths.size() > 0) {
        pw.println("\nMETHODS");
        dumpMethodMap(diffMeths, pw);
        }
        if (diffFields.size() > 0) {
        pw.println("\nFIELDS");
        dumpFieldSet(diffFields, pw);
        }
    } else {
        pw.println("\n" + n1 + " OK");
    }
    }

    static final class MethodRecord {
        MorC[] overrides;

        MethodRecord(MorC m) {
            overrides = new MorC[] { m };
        }

        MethodRecord(MorC[] ms) {
            overrides = ms;
        }

        MethodRecord copy() {
            return new MethodRecord((MorC[])overrides.clone());
        }

        int count() {
            for (int i = 0; i < overrides.length; ++i) {
                if (overrides[i] == null) {
                    return i;
                }
            }
            return overrides.length;
        }

        void add(MorC m) {
            MorC[] temp = new MorC[overrides.length + 1];
            for (int i = 0; i < overrides.length; ++i) {
                temp[i] = overrides[i];
            }
            temp[overrides.length] = m;
            overrides = temp;
        }

        void remove(int index) {
            int i = index;
            while (overrides[i] != null && i < overrides.length-1) {
                overrides[i] = overrides[i+1];
                ++i;
            }
            overrides[i] = null;
        }

        // if a call to a method can be handled by a call to t, remove the
        // method from our list, and return true
        boolean removeOverridden(MorC t) {
            boolean result = false;
            int i = 0;
            while (i < overrides.length) {
                MorC m = overrides[i];
                if (m == null) {
                    break;
                }
                if (handles(t, m)) {
                    remove(i);
                    result = true;
                } else {
                    ++i;
                }
            }
            return result;
        }

        // remove all methods handled by any method of mr
        boolean removeOverridden(MethodRecord mr) {
            boolean result = false;
            for (int i = 0; i < mr.overrides.length; ++i) {
                MorC t = mr.overrides[i];
                if (t == null) {
                    // this shouldn't happen, as the target record should not have been modified
                    throw new InternalError();
                }
                if (removeOverridden(t)) {
                    result = true;
                }
            }
            return result;
        }

        void msg(MorC t, MorC m, String msg) {
            StringBuffer buf = new StringBuffer();
            buf.append(t.getName());
            buf.append(" ");
            buf.append(msg);
            buf.append("\n   ");
            toString(t, buf);
            buf.append("\n   ");
            toString(m, buf);
            System.out.println(buf.toString());
        }

        boolean handles(MorC t, MorC m) {
            // relevant modifiers must match
            if ((t.getModifiers() & MOD_MASK) != (m.getModifiers() & MOD_MASK)) {
                if (DEBUG) msg(t, m, "modifier mismatch");
                return false;
            }

            Class tr = pairClassEquivalent(t.getReturnType());
            Class mr = pairClassEquivalent(m.getReturnType());
            if (!assignableFrom(mr, tr)) { // t return type must be same or narrower than m
                if (DEBUG) msg(t, m, "return value mismatch");
                return false;
            }
            Class[] tts = t.getParameterTypes();
            Class[] mts = m.getParameterTypes();
            if (tts.length != mts.length) {
                if (DEBUG) msg(t, m, "param count mismatch");
                return false;
            }

            for (int i = 0; i < tts.length; ++i) {
                Class tc = pairClassEquivalent(tts[i]);
                Class mc = pairClassEquivalent(mts[i]);
                if (!assignableFrom(tc, mc)) { // m param must be same or narrower than t
                    if (DEBUG) msg(t, m, "parameter " + i + " mismatch, " +
                                   tts[i].getName() + " not assignable from " + mts[i].getName());
                    return false;
                }
            }
            return true;
        }

        public void toString(MorC m, StringBuffer buf) {
            int mod = m.getModifiers();
            if (mod != 0) {
                buf.append(Modifier.toString(mod) + " ");
            }
            buf.append(m.getReturnType().getName() + " (");
            Class[] ptypes = m.getParameterTypes();
            for (int j = 0; j < ptypes.length; ++j) {
                if (j > 0) {
                    buf.append(", ");
                }
                buf.append(ptypes[j].getName());
            }
            buf.append(')');
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(overrides[0].getName());
            for (int i = 0; i < overrides.length; ++i) {
                MorC m = overrides[i];
                if (m == null) {
                    break;
                }
                buf.append("\n   ");
                toString(m, buf);
            }
            return buf.toString();
        }
    }

    static MorC[] getMorCArray(Constructor[] cons) {
        MorC[] result = new MorC[cons.length];
        for (int i = 0 ; i < cons.length; ++i) {
            result[i] = new MorC(cons[i]);
        }
        return result;
    }

    static MorC[] getMorCArray(Method[] meths) {
        MorC[] result = new MorC[meths.length];
        for (int i = 0 ; i < meths.length; ++i) {
            result[i] = new MorC(meths[i]);
        }
        return result;
    }

    static Map getMethodMap(MorC[] meths) {
        Map result = new TreeMap();
        for (int i = 0; i < meths.length; ++i) {
            MorC m = meths[i];
            String key = m.getName();
            MethodRecord mr = (MethodRecord)result.get(key);
            if (mr == null) {
                mr = new MethodRecord(m);
                result.put(key, mr);
            } else {
                mr.add(m);
            }
        }
        return result;
    }

    static void dumpMethodMap(Map m, PrintWriter out) {
        Iterator iter = m.entrySet().iterator();
        while (iter.hasNext()) {
            MethodRecord mr = (MethodRecord)((Map.Entry)iter.next()).getValue();
            out.println(mr);
        }
        out.flush();
    }

    static Map diffMethodMaps(Map m1, Map m2) {
        // get all the methods in m1 that aren't mentioned in m2 at all
        Map result = (Map)((TreeMap)m1).clone();
        result.keySet().removeAll(m2.keySet());
        return result;
    }

    static final boolean[][] assignmentMap = {
        // bool   char   byte  short    int   long  float double   void
        {  true, false, false, false, false, false, false, false, false }, // boolean
        { false,  true,  true,  true, false, false, false, false, false }, // char
        { false, false,  true, false, false, false, false, false, false }, // byte
        { false, false,  true,  true, false, false, false, false, false }, // short
        { false,  true,  true,  true,  true, false, false, false, false }, // int
        { false,  true,  true,  true,  true,  true, false, false, false }, // long
        { false,  true,  true,  true,  true, false,  true, false, false }, // float
        { false,  true,  true,  true,  true, false,  true,  true, false }, // double
        { false, false, false, false, false, false, false, false,  true }, // void
    };

    static final Class[] prims = {
        boolean.class, char.class, byte.class, short.class,
        int.class, long.class, float.class, double.class, void.class
    };

    static int primIndex(Class cls) {
        for (int i = 0; i < prims.length; ++i) {
            if (cls == prims[i]) {
                return i;
            }
        }
        throw new InternalError("could not find primitive class: " + cls);
    }

    static boolean assignableFrom(Class lhs, Class rhs) {
        if (lhs == rhs) {
            return true;
        }
        if (lhs.isPrimitive()) {
            if (!rhs.isPrimitive()) {
                return false;
            }
            int lhsx = primIndex(lhs);
            int rhsx = primIndex(rhs);
            return assignmentMap[lhsx][rhsx];
        }
        return lhs.isAssignableFrom(rhs);
    }

    static String toString(Field f) {
        StringBuffer buf = new StringBuffer(f.getName());
        int mod = f.getModifiers() & MOD_MASK;
        if (mod != 0) {
            buf.append(" " + Modifier.toString(mod));
        }
        buf.append(" " + pairEquivalent(f.getType().getName()));
        return buf.toString();
    }

    static Set getFieldSet(Field[] fs) {
        Set set = new TreeSet();
        for (int i = 0; i < fs.length; ++i) {
            set.add(toString(fs[i]));
        }
        return set;
    }

    static Set diffFieldSets(Set s1, Set s2) {
        Set result = (Set)((TreeSet)s1).clone();
        result.removeAll(s2);
        return result;
    }

    static void dumpFieldSet(Set s, PrintWriter pw) {
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            pw.println(iter.next());
        }
        pw.flush();
    }

    // given a target string, if it matches the first of one of our pairs, return the second
    static String pairEquivalent(String target) {
        for (int i = 0; i < pairs.length; i += 2) {
            if (target.equals(pairs[i])) {
                return pairs[i+1];
            }
        }
        return target;
    }

    static Class pairClassEquivalent(Class target) {
        for (int i = 0; i < pairClasses.length; i += 2) {
            if (target.equals(pairClasses[i])) {
                return pairClasses[i+1];
            }
        }
        return target;
    }

    static final int MOD_MASK = ~(Modifier.FINAL|Modifier.SYNCHRONIZED|
                                  Modifier.VOLATILE|Modifier.TRANSIENT|Modifier.NATIVE);
}
