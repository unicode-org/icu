// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2005-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.tool.docs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

    // set up defaults
    private static final String kSrcPrefix = "java.";
    private static final String kTrgPrefix = "com.ibm.icu.";
    private static final String[] kPairInfo = {
        "lang.Character/UCharacter",
        "lang.Character$UnicodeBlock/UCharacter$UnicodeBlock",
        "text.BreakIterator",
        "text.Collator",
        "text.DateFormat",
        "text.DateFormatSymbols",
        "text.DecimalFormat",
        "text.DecimalFormatSymbols",
        "text.Format/UFormat",
        "text.MessageFormat",
        "text.NumberFormat",
        "text.SimpleDateFormat",
        "util.Calendar",
        "util.Currency",
        "util.GregorianCalendar",
        "util.SimpleTimeZone",
        "util.TimeZone",
        "util.Locale/ULocale",
        "util.ResourceBundle/UResourceBundle",
    };

    private static final String[] kIgnore = new String[] {
        "lang.Character <init> charValue compareTo MAX_VALUE MIN_VALUE TYPE",
        "lang.Character$UnicodeBlock SURROGATES_AREA",
        "util.Calendar FIELD_COUNT",
        "util.GregorianCalendar FIELD_COUNT",
        "util.SimpleTimeZone STANDARD_TIME UTC_TIME WALL_TIME",
    };

    private PrintWriter pw;
    private String srcPrefix;
    private String trgPrefix;
    private Class[] classPairs;
    private String[] namePairs;
    private String[] ignore;
    private boolean swap;
    //private boolean signature;

    // call System.exit with non-zero if there were some missing APIs
    public static void main(String[] args) {
        System.exit(doMain(args));
    }

    // return non-zero if there were some missing APIs
    public static int doMain(String[] args) {
        ICUJDKCompare p = new ICUJDKCompare();
        p.setOutputWriter(new PrintWriter(System.out));
        p.setup(args);
        return p.process();
    }

    // setters
    public ICUJDKCompare setOutputWriter(PrintWriter pw) {
        this.pw = pw;
        return this;
    }

    public ICUJDKCompare setSrcPrefix(String srcPrefix) {
        this.srcPrefix = srcPrefix;
        return this;
    }

    public ICUJDKCompare setTrgPrefix(String trgPrefix) {
        this.trgPrefix = trgPrefix;
        return this;
    }

    public ICUJDKCompare setClassPairs(Class[] classPairs) {
        this.classPairs = classPairs;
        return this;
    }

    public ICUJDKCompare setNamePairs(String[] namePairs) {
        this.namePairs = namePairs;
        return this;
    }

    public ICUJDKCompare setIgnore(String[] ignore) {
        this.ignore = ignore;
        return this;
    }

    public ICUJDKCompare setSwap(boolean swap) {
        this.swap = swap;
        return this;
    }

    public ICUJDKCompare setup(String[] args) {
        String namelist = null;
        String ignorelist = null;
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.equals("-swap")) {
                swap = true;
            } else if (arg.equals("-srcPrefix:")) {
                srcPrefix = args[++i];
                if (!srcPrefix.endsWith(".")) {
                    srcPrefix += '.';
                }
            } else if (arg.equals("-trgPrefix:")) {
                trgPrefix = args[++i];
                if (!trgPrefix.endsWith(".")) {
                    trgPrefix += '.';
                }
            } else if (arg.equals("-names:")) {
                namelist = args[++i];
            } else if (arg.equals("-ignore:")) {
                ignorelist = args[++i];
            } else {
                System.err.println("unrecognized argument: " + arg);
                throw new IllegalStateException();
            }
        }

        if (ignorelist != null) {
            if (ignorelist.charAt(0) == '@') { // a file containing ignoreinfo
                BufferedReader br = null;
                try {
                    ArrayList nl = new ArrayList();
                    File f = new File(namelist.substring(1));
                    FileInputStream fis = new FileInputStream(f);
                    InputStreamReader isr = new InputStreamReader(fis);
                    br = new BufferedReader(isr);
                    String line = null;
                    while (null != (line = br.readLine())) {
                        nl.add(line);
                    }
                    ignore = (String[])nl.toArray(new String[nl.size()]);
                }
                catch (Exception e) {
                    System.err.println(e);
                    throw new IllegalStateException();
                }
                finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            } else { // a list of ignoreinfo separated by semicolons
                ignore = ignorelist.split("\\s*;\\s*");
            }
        }

        if (namelist != null) {
            String[] names = null;
            if (namelist.charAt(0) == '@') { // a file
                BufferedReader br = null;
                try {
                    ArrayList nl = new ArrayList();
                    File f = new File(namelist.substring(1));
                    FileInputStream fis = new FileInputStream(f);
                    InputStreamReader isr = new InputStreamReader(fis);
                    br = new BufferedReader(isr);
                    String line = null;
                    while (null != (line = br.readLine())) {
                        nl.add(line);
                    }
                    names = (String[])nl.toArray(new String[nl.size()]);
                }
                catch (Exception e) {
                    System.err.println(e);
                    throw new IllegalStateException();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }

            } else { // a list of names separated by semicolons
                names = namelist.split("\\s*;\\s*");
            }
            processPairInfo(names);
        }

        pw.flush();

        return this;
    }

    private void processPairInfo(String[] names) {
        ArrayList cl = new ArrayList();
        ArrayList nl = new ArrayList();
        for (int i = 0; i < names.length; ++i) {
            String name = names[i];
            String srcName = srcPrefix;
            String trgName = trgPrefix;

            int n = name.indexOf('/');
            if (n == -1) {
                srcName += name;
                trgName += name;
            } else {
                String srcSuffix = name.substring(0, n).trim();
                String trgSuffix = name.substring(n+1).trim();
                int jx = srcSuffix.length()+1;
                int ix = trgSuffix.length()+1;
                while (ix != -1) {
                    jx = srcSuffix.lastIndexOf('.', jx-1);
                    ix = trgSuffix.lastIndexOf('.', ix-1);
                }
                srcName += srcSuffix;
                trgName += srcSuffix.substring(0, jx+1) + trgSuffix;
            }

            try {
                Class jc = Class.forName(srcName);
                Class ic = Class.forName(trgName);
                cl.add(ic);
                cl.add(jc);
                nl.add(ic.getName());
                nl.add(jc.getName());
            }
            catch (Exception e) {
                if (DEBUG) System.err.println("can't load class: " + e.getMessage());
            }
        }
        classPairs = (Class[])cl.toArray(new Class[cl.size()]);
        namePairs = (String[])nl.toArray(new String[nl.size()]);
    }

    private void println(String s) {
        if (pw != null) pw.println(s);
    }

    private void flush() {
        if (pw != null) pw.flush();
    }

    public int process() {
        // set defaults
        if (srcPrefix == null) {
            srcPrefix = kSrcPrefix;
        }

        if (trgPrefix == null) {
            trgPrefix = kTrgPrefix;
        }

        if (classPairs == null) {
            processPairInfo(kPairInfo);
        }

        if (ignore == null) {
            ignore = kIgnore;
        }

        println("ICU and Java API Comparison");
        String ICU_VERSION = "unknown";
        try {
            Class cls = Class.forName("com.ibm.icu.util.VersionInfo");
            Field fld = cls.getField("ICU_VERSION");
            ICU_VERSION = fld.get(null).toString();
        }
        catch (Exception e) {
            if (DEBUG) System.err.println("can't get VersionInfo: " + e.getMessage());
        }
        println("ICU Version " + ICU_VERSION);
        println("JDK Version " + System.getProperty("java.version"));

        int errorCount = 0;
        for (int i = 0; i < classPairs.length; i += 2) {
            try {
                if (swap) {
                    errorCount += compare(classPairs[i+1], classPairs[i]);
                } else {
                    errorCount += compare(classPairs[i], classPairs[i+1]);
                }
            }
            catch (Exception e) {
                System.err.println("exception: " + e);
                System.err.println("between " + namePairs[i] + " and " + namePairs[i+1]);
                e.printStackTrace();
                errorCount += 1;
            }
        }
        return errorCount;
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

        String getSignature() {
            return mref == null ? cref.toString() : mref.toString();
        }
    }

    private int compare(Class class1, Class class2) throws Exception {
        String n1 = class1.getName();
        String n2 = class2.getName();

        println("\ncompare " + n1 + " <> " + n2);

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

        if (n1.indexOf("DecimalFormatSymbols") != -1) {
          pw.format("fields in %s: %s%n", n1, set1);
          pw.format("fields in %s: %s%n", n2, set2);
        }

        Map diffConss = diffMethodMaps(cmap2, cmap1);
        Map diffMeths = diffMethodMaps(map2, map1);
        Set diffFields = diffFieldSets(set2, set1);

        diffConss = removeIgnored(n2, diffConss);
        diffMeths = removeIgnored(n2, diffMeths);
        diffFields = removeIgnored(n2, diffFields);

        int result = diffConss.size() + diffMeths.size() + diffFields.size();
        if (result > 0 && pw != null) {
            pw.println("Public API in " + n2 + " but not in " + n1);
            if (diffConss.size() > 0) {
                pw.println("CONSTRUCTORS");
                dumpMethodMap(diffConss, pw);
            }
            if (diffMeths.size() > 0) {
                pw.println("METHODS");
                dumpMethodMap(diffMeths, pw);
            }
            if (diffFields.size() > 0) {
                pw.println("FIELDS");
                dumpFieldSet(diffFields, pw);
            }
        }

        flush();

        return result;
    }

    final class MethodRecord {
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
                    throw new IllegalStateException();
                }
                if (removeOverridden(t)) {
                    result = true;
                }
            }
            return result;
        }

        void debugmsg(MorC t, MorC m, String msg) {
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
                if (DEBUG) debugmsg(t, m, "modifier mismatch");
                return false;
            }

            Class tr = pairClassEquivalent(t.getReturnType());
            Class mr = pairClassEquivalent(m.getReturnType());
            if (!assignableFrom(mr, tr)) { // t return type must be same or narrower than m
                if (DEBUG) debugmsg(t, m, "return value mismatch");
                return false;
            }
            Class[] tts = t.getParameterTypes();
            Class[] mts = m.getParameterTypes();
            if (tts.length != mts.length) {
                if (DEBUG) debugmsg(t, m, "param count mismatch");
                return false;
            }

            for (int i = 0; i < tts.length; ++i) {
                Class tc = pairClassEquivalent(tts[i]);
                Class mc = pairClassEquivalent(mts[i]);
                if (!assignableFrom(tc, mc)) { // m param must be same or narrower than t
                    if (DEBUG) debugmsg(t, m, "parameter " + i + " mismatch, " +
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
            buf.append(nameOf(m.getReturnType()));
            buf.append(" ");
            buf.append(m.getName());
            buf.append("(");
            Class[] ptypes = m.getParameterTypes();
            for (int j = 0; j < ptypes.length; ++j) {
                if (j > 0) {
                    buf.append(", ");
                }
                buf.append(nameOf(ptypes[j]));
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

    public static String nameOf(Class c) {
        if (c.isArray()) {
            return nameOf(c.getComponentType()) + "[]";
        }
        String name = c.getName();
        return name.substring(name.lastIndexOf('.') + 1);
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

    private Map getMethodMap(MorC[] meths) {
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

    private void dumpMethodMap(Map m, PrintWriter pw) {
        Iterator iter = m.entrySet().iterator();
        while (iter.hasNext()) {
            dumpMethodRecord((MethodRecord)((Map.Entry)iter.next()).getValue());
        }
        pw.flush();
    }

    private void dumpMethodRecord(MethodRecord mr) {
        pw.println(mr.toString());
    }

    static Map diffMethodMaps(Map m1, Map m2) {
        // get all the methods in m1 that aren't mentioned in m2 at all
        Map result = (Map)((TreeMap)m1).clone();
        result.keySet().removeAll(m2.keySet());
        return result;
    }

    private Map removeIgnored(String name, Map m1) {
        if (ignore == null) {
            return m1;
        }
        if (name.startsWith(srcPrefix)) {
            name = name.substring(srcPrefix.length());
        }
        name += " "; // to avoid accidental prefix of nested class name

        // prune ignore list to relevant items
        ArrayList il = null;
        for (int i = 0; i < ignore.length; ++i) {
            String s = ignore[i];
            if (s.startsWith(name)) {
                if (il == null) {
                    il = new ArrayList();
                }
                il.add(s);
            }
        }
        if (il == null) {
            return m1;
        }

        Map result = new TreeMap(((TreeMap)m1).comparator());
        result.putAll(m1);
        Iterator iter = result.entrySet().iterator();
        loop: while (iter.hasNext()) {
            Map.Entry e = (Map.Entry)iter.next();
            String key = (String)e.getKey();
            for (int i = 0; i < il.size(); ++i) {
                String ig = (String)il.get(i);
                if (ig.indexOf(" " + key) != 0) {
                    iter.remove();
                    continue loop;
                }
            }
        }
        return result;
    }

    private Set removeIgnored(String name, Set s1) {
        if (ignore == null) {
            return s1;
        }
        if (name.startsWith(srcPrefix)) {
            name = name.substring(srcPrefix.length());
        }
        name += " "; // to avoid accidental prefix of nested class name

        // prune ignore list to relevant items
        ArrayList il = null;
        for (int i = 0; i < ignore.length; ++i) {
            String s = ignore[i];
            if (s.startsWith(name)) {
                if (il == null) {
                    il = new ArrayList();
                }
                il.add(s);
            }
        }
        if (il == null) {
            return s1;
        }

        Set result = (Set)((TreeSet)s1).clone();
        Iterator iter = result.iterator();
        loop: while (iter.hasNext()) {
            String key = (String)iter.next();
            String fieldname = key.substring(0, key.indexOf(' '));
            for (int i = 0; i < il.size(); ++i) {
                String ig = (String)il.get(i);
                if (ig.indexOf(" " + fieldname) != 0) {
                    iter.remove();
                    continue loop;
                }
            }
        }
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
        throw new IllegalStateException("could not find primitive class: " + cls);
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

    private String toString(Field f) {
        StringBuffer buf = new StringBuffer(f.getName());
        int mod = f.getModifiers() & MOD_MASK;
        if (mod != 0) {
            buf.append(" " + Modifier.toString(mod));
        }
        buf.append(" ");
        String n = pairEquivalent(f.getType().getName());
        n = n.substring(n.lastIndexOf('.') + 1);
        buf.append(n);
        return buf.toString();
    }

    private Set getFieldSet(Field[] fs) {
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

    private void dumpFieldSet(Set s, PrintWriter pw) {
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            pw.println(iter.next());
        }
        pw.flush();
    }

    // given a target string, if it matches the first of one of our pairs, return the second
    // or vice-versa if swap is true
    private String pairEquivalent(String target) {
        for (int i = 0; i < namePairs.length; i += 2) {
            if (swap) {
                if (target.equals(namePairs[i+1])) {
                    return namePairs[i];
                }
            } else {
                if (target.equals(namePairs[i])) {
                    return namePairs[i+1];
                }
            }
        }
        return target;
    }

    private Class pairClassEquivalent(Class target) {
        for (int i = 0; i < classPairs.length; i += 2) {
            if (target.equals(classPairs[i])) {
                return classPairs[i+1];
            }
        }
        return target;
    }

    static final int MOD_MASK = ~(Modifier.FINAL|Modifier.SYNCHRONIZED|
                                  Modifier.VOLATILE|Modifier.TRANSIENT|Modifier.NATIVE);
}
