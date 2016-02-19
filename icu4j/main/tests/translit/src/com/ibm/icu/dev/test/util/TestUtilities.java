/*
 *******************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.TestBoilerplate;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.ICUPropertyFactory;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMapIterator;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;

public class TestUtilities extends TestFmwk {
    static final int LIMIT = 0x15; // limit to make testing more realistic in terms of collisions
    static final int ITERATIONS = 1000000;
    static final boolean SHOW_PROGRESS = false;
    static final boolean DEBUG = false;
    
    public static void main(String[] args) throws Exception {
        new TestUtilities().run(args);
    }
    
    SortedSet<String> log = new TreeSet<String>();
    static String[] TEST_VALUES = {"A", "B", "C", "D", "E", "F"};
    static Random random = new Random(12345);
    
    public void TestUnicodeMapRandom() {
        // do random change to both, then compare
        random.setSeed(12345); // reproducable results
        logln("Comparing against HashMap");
        UnicodeMap<String> map1 = new UnicodeMap();
        Map<Integer, String> map2 = new HashMap<Integer, String>();
        for (int counter = 0; counter < ITERATIONS; ++counter) {
            int start = random.nextInt(LIMIT);
            String value = TEST_VALUES[random.nextInt(TEST_VALUES.length)];
            String logline = Utility.hex(start) + "\t" + value;
            if (SHOW_PROGRESS) logln(counter + "\t" + logline);
            log.add(logline);
            if (DEBUG && counter == 144) {
                System.out.println(" debug");
            }
            map1.put(start, value);
            map2.put(start, value);
            check(map1, map2, counter);
        }
        checkNext(map1, map2, LIMIT);
    }

    public void TestUnicodeMapGeneralCategory() {
        logln("Setting General Category");
        UnicodeMap<String> map1 = new UnicodeMap();
        Map<Integer, String> map2 = new HashMap<Integer, String>();
        //Map<Integer, String> map3 = new TreeMap<Integer, String>();
        map1 = new UnicodeMap<String>();
        map2 = new TreeMap<Integer,String>();
        for (int cp = 0; cp <= SET_LIMIT; ++cp) {
              int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
              //if (enumValue <= 0) continue; // for smaller set
              String value = UCharacter.getPropertyValueName(propEnum,enumValue, UProperty.NameChoice.LONG);
              map1.put(cp, value);
              map2.put(cp, value);
        }       
        checkNext(map1, map2, Integer.MAX_VALUE);
        
        logln("Comparing General Category");
        check(map1, map2, -1);
        logln("Comparing Values");
        Set<String> values1 = map1.getAvailableValues(new TreeSet<String>());
        Set<String> values2 = new TreeSet<String>(map2.values());
        if (!TestBoilerplate.verifySetsIdentical(this, values1, values2)) {
            throw new IllegalArgumentException("Halting");
        }
        logln("Comparing Sets");
        for (Iterator<String> it = values1.iterator(); it.hasNext();) {
            String value = it.next();
            logln(value == null ? "null" : value);
            UnicodeSet set1 = map1.keySet(value);
            UnicodeSet set2 = TestBoilerplate.getSet(map2, value);
            if (!TestBoilerplate.verifySetsIdentical(this, set1, set2)) {
                throw new IllegalArgumentException("Halting");
            }
        } 
        
    }

    static final UnicodeMap<String> SCRIPTS = ICUPropertyFactory.make().getProperty("script").getUnicodeMap_internal();
    static final UnicodeMap<String> GC = ICUPropertyFactory.make().getProperty("general_category").getUnicodeMap_internal();

    public void TestUnicodeMapCompose() {
        logln("Getting Scripts");
        
        UnicodeMap.Composer<String> composer = new UnicodeMap.Composer<String>() {
            @Override
            public String compose(int codepoint, String string, String a, String b) {
                return a.toString() + "_" + b.toString();
            }
        };
        
        logln("Trying Compose");

//        Map<Integer, String> map2 = new HashMap<Integer, String>();
//        Map<Integer, String> map3 = new TreeMap<Integer, String>();
        UnicodeMap<String> composed = ((UnicodeMap)SCRIPTS.cloneAsThawed()).composeWith(GC, composer);
        String last = "";
        for (int i = 0; i < 0x10FFFF; ++i) {
//            if (i == 888) {
//                int debug = 0;
//            }
            String comp = composed.getValue(i);
            String gc = GC.getValue(i);
            String sc = SCRIPTS.getValue(i);
            if (!comp.equals(composer.compose(i, null, sc, gc))) {
                errln("Failed compose at: " + i);
                break;
            }
            if (!last.equals(comp)) {
                logln(Utility.hex(i) + "\t" + comp);
                last = comp;
            }
        }
    }

    public void testBoilerplate() {
        // check boilerplate
        List argList = new ArrayList();
        argList.add("TestMain");
        if (params.verbose) argList.add("-verbose");
        String[] args = new String[argList.size()];
        argList.toArray(args);
        new UnicodeMapBoilerplate().run(args);
         // TODO: the following is not being reached
        new UnicodeSetBoilerplate().run(args);
    }
    
    public void TestAUnicodeMap2() {
        UnicodeMap foo = new UnicodeMap();
        @SuppressWarnings("unused")
        int hash = foo.hashCode(); // make sure doesn't NPE
        @SuppressWarnings("unused")
        Set fii = foo.stringKeys(); // make sure doesn't NPE
    }
    
    public void TestAUnicodeMapInverse() {
        UnicodeMap<Character> foo1 = new UnicodeMap<Character>()
                .putAll('a', 'z', 'b')
                .put("ab", 'c')
                .put('x', 'b')
                .put("xy", 'c')
                ;
        Map<Character, UnicodeSet> target = new HashMap<Character, UnicodeSet>();
        foo1.addInverseTo(target);
        UnicodeMap<Character> reverse = new UnicodeMap().putAllInverse(target);
        assertEquals("", foo1, reverse);
    }
    
    public void TestCollectionUtilitySpeed() {
        TreeSet ts1 = new TreeSet();
        TreeSet ts2 = new TreeSet();
        int size = 1000;
        int iterations = 1000;
        String prefix =  "abc";
        String postfix = "nop";
        for (int i = 0; i < size; ++i) {
            ts1.add(prefix + String.valueOf(i) + postfix);
            ts2.add(prefix + String.valueOf(i) + postfix);
        }
        // warm up
        CollectionUtilities.containsAll(ts1, ts2);
        ts1.containsAll(ts2);

        timeAndCompare(ts1, ts2, iterations, true, .75);
        // now different sets
        ts1.add("Able");
        timeAndCompare(ts1, ts2, iterations, true, .75);
        timeAndCompare(ts2, ts1, iterations*100, false, 1.05);
    }

    private void timeAndCompare(TreeSet ts1, TreeSet ts2, int iterations, boolean expected, double factorOfStandard) {
        double utilityTimeSorted = timeUtilityContainsAll(iterations, ts1, ts2, expected)/(double)iterations;
        double standardTimeSorted = timeStandardContainsAll(iterations, ts1, ts2, expected)/(double)iterations;
        
        if (utilityTimeSorted < standardTimeSorted*factorOfStandard) {
            logln("Sorted: Utility time (" + utilityTimeSorted + ") << Standard duration (" + standardTimeSorted + "); " + 100*(utilityTimeSorted/standardTimeSorted) + "%");
        } else {
            errln("Sorted: Utility time (" + utilityTimeSorted + ") !<< Standard duration (" + standardTimeSorted + "); " + 100*(utilityTimeSorted/standardTimeSorted) + "%");
        }
    }

    private long timeStandardContainsAll(int iterations, Set hs1, Set hs2, boolean expected) {
        long standardTime;
        {
            long start, end;
            boolean temp = false;

            start = System.currentTimeMillis();
            for (int i = 0; i < iterations; ++i) {
                temp = hs1.containsAll(hs2);
                if (temp != expected) {
                    errln("Bad result");
                }
            }
            end = System.currentTimeMillis();
            standardTime = end - start;
        }
        return standardTime;
    }

    private long timeUtilityContainsAll(int iterations, Set hs1, Set hs2, boolean expected) {
        long utilityTime;
        {
            long start, end;
            boolean temp = false;
            start = System.currentTimeMillis();
            for (int i = 0; i < iterations; ++i) {
                temp = CollectionUtilities.containsAll(hs1, hs2);
                if (temp != expected) {
                    errln("Bad result");
                }
            }
            end = System.currentTimeMillis();
            utilityTime = end - start;
        }
        return utilityTime;
    }
    
    public void TestCollectionUtilities() {
        String[][] test = {{"a", "c", "e", "g", "h", "z"}, {"b", "d", "f", "h", "w"}, { "a", "b" }, { "a", "d" }, {"d"}, {}}; // 
        int resultMask = 0;
        for (int i = 0; i < test.length; ++i) {
            Collection a = new TreeSet(Arrays.asList(test[i]));
            for (int j = 0; j < test.length; ++j) {
                Collection b = new TreeSet(Arrays.asList(test[j]));
                int relation = CollectionUtilities.getContainmentRelation(a, b);
                resultMask |= (1 << relation);
                switch (relation) {
                case CollectionUtilities.ALL_EMPTY:
                    checkContainment(a.size() == 0 && b.size() == 0, a, relation, b);
                    break;
                case CollectionUtilities.NOT_A_SUPERSET_B:
                    checkContainment(a.size() == 0 && b.size() != 0, a, relation, b);
                    break;
                case CollectionUtilities.NOT_A_DISJOINT_B:
                    checkContainment(a.equals(b) && a.size() != 0, a, relation, b);
                    break;
                case CollectionUtilities.NOT_A_SUBSET_B:
                    checkContainment(a.size() != 0 && b.size() == 0, a, relation, b);
                    break;
                case CollectionUtilities.A_PROPER_SUBSET_OF_B:
                    checkContainment(b.containsAll(a) && !a.equals(b), a, relation, b);
                    break;
                case CollectionUtilities.NOT_A_EQUALS_B:
                    checkContainment(!CollectionUtilities.containsSome(a, b) && a.size() != 0 && b.size() != 0, a, relation, b);
                    break;
                case CollectionUtilities.A_PROPER_SUPERSET_B:
                    checkContainment(a.containsAll(b) && !a.equals(b), a, relation, b);
                break;
                case CollectionUtilities.A_PROPER_OVERLAPS_B:
                    checkContainment(!b.containsAll(a) && !a.containsAll(b) && CollectionUtilities.containsSome(a, b), a, relation, b);
                break;
                }
            }
        }
        if (resultMask != 0xFF) {
            String missing = "";
            for (int i = 0; i < 8; ++i) {
                if ((resultMask & (1 << i)) == 0) {
                    if (missing.length() != 0) missing += ", ";
                    missing += RelationName[i];
                }
            }
            errln("Not all ContainmentRelations checked: " + missing);
        }
    }

    static final String[] RelationName = {"ALL_EMPTY",
            "NOT_A_SUPERSET_B",
            "NOT_A_DISJOINT_B",
            "NOT_A_SUBSET_B",
            "A_PROPER_SUBSET_OF_B",
            "A_PROPER_DISJOINT_B",
            "A_PROPER_SUPERSET_B",
            "A_PROPER_OVERLAPS_B"};

    /**
     *  
     */
    private void checkContainment(boolean c, Collection a, int relation, Collection b) {
        if (!c) {
            errln("Fails relation: " + a + " \t" + RelationName[relation] + " \t" + b);
        }
    }

    private void checkNext(UnicodeMap<String> map1, Map<Integer,String> map2, int limit) {
        logln("Comparing nextRange");
        Map localMap = new TreeMap();
        UnicodeMapIterator<String> mi = new UnicodeMapIterator<String>(map1);
        while (mi.nextRange()) {
            logln(Utility.hex(mi.codepoint) + ".." + Utility.hex(mi.codepointEnd) + " => " + mi.value);
            for (int i = mi.codepoint; i <= mi.codepointEnd; ++i) {
                //if (i >= limit) continue;
                localMap.put(i, mi.value);
            }
        }
        checkMap(map2, localMap);
        
        logln("Comparing next");
        mi.reset();
        localMap = new TreeMap();
        String lastValue = null;
        while (mi.next()) {
//            if (!UnicodeMap.areEqual(lastValue, mi.value)) {
//                // System.out.println("Change: " + Utility.hex(mi.codepoint) + " => " + mi.value);
//                lastValue = mi.value;
//            }
            //if (mi.codepoint >= limit) continue;
            localMap.put(mi.codepoint, mi.value);
        }
        checkMap(map2, localMap);
    }
    
    public void check(UnicodeMap<String> map1, Map<Integer,String> map2, int counter) {
        for (int i = 0; i < LIMIT; ++i) {
            String value1 = map1.getValue(i);
            String value2 = map2.get(i);
            if (!UnicodeMap.areEqual(value1, value2)) {
                errln(counter + " Difference at " + Utility.hex(i)
                     + "\t UnicodeMap: " + value1
                     + "\t HashMap: " + value2);
                errln("UnicodeMap: " + map1);
                errln("Log: " + TestBoilerplate.show(log));
                errln("HashMap: " + TestBoilerplate.show(map2));
            }
        }
    }
    
    void checkMap(Map m1, Map m2) {
        if (m1.equals(m2)) return;
        StringBuilder buffer = new StringBuilder();
        Set m1entries = m1.entrySet();
        Set m2entries = m2.entrySet();
        getEntries("\r\nIn First, and not Second", m1entries, m2entries, buffer, 20);
        getEntries("\r\nIn Second, and not First", m2entries, m1entries, buffer, 20);
        errln(buffer.toString());
    }
    
    static Comparator<Map.Entry<Integer, String>> ENTRY_COMPARATOR = new Comparator<Map.Entry<Integer, String>>() {
        public int compare(Map.Entry<Integer, String> o1, Map.Entry<Integer, String> o2) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            Map.Entry<Integer, String> a = o1;
            Map.Entry<Integer, String> b = o2;
            int result = compare2(a.getKey(), b.getKey());
            if (result != 0) return result;
            return compare2(a.getValue(), b.getValue());
        }
        private <T extends Comparable> int compare2(T o1, T o2) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        }
    };

    private void getEntries(String title, Set<Map.Entry<Integer,String>> m1entries, Set<Map.Entry<Integer, String>> m2entries, StringBuilder buffer, int limit) {
        Set<Map.Entry<Integer, String>> m1_m2 = new TreeSet<Map.Entry<Integer, String>>(ENTRY_COMPARATOR);
        m1_m2.addAll(m1entries);
        m1_m2.removeAll(m2entries);
        buffer.append(title + ": " + m1_m2.size() + "\r\n");
        for (Entry<Integer, String> entry : m1_m2) {
            if (limit-- < 0) return;
            buffer.append(entry.getKey()).append(" => ")
             .append(entry.getValue()).append("\r\n");
        }
    }
    
    static final int SET_LIMIT = 0x10FFFF;
    static final int CHECK_LIMIT = 0xFFFF;
    static final NumberFormat pf = NumberFormat.getPercentInstance();
    static final NumberFormat nf = NumberFormat.getInstance();
    
    public void TestTime() {
        boolean shortTest = getInclusion() < 10;
        double hashTime, umTime, icuTime, treeTime;
        int warmup = shortTest ? 1 : 20;
        umTime = checkSetTime(warmup, 0);
        hashTime = checkSetTime(warmup, 1);
        logln("Percentage: " + pf.format(hashTime/umTime));
        treeTime = checkSetTime(warmup, 3);
        logln("Percentage: " + pf.format(treeTime/umTime));
        //logln(map1.toString());
        
        if (shortTest) {
            return;
        }
        
        umTime = checkGetTime(1000, 0);
        hashTime = checkGetTime(1000, 1);
        logln("Percentage: " + pf.format(hashTime/umTime));
        icuTime = checkGetTime(1000, 2);
        logln("Percentage: " + pf.format(icuTime/umTime));
        treeTime = checkGetTime(1000, 3);
        logln("Percentage: " + pf.format(treeTime/umTime));
    }
    
    int propEnum = UProperty.GENERAL_CATEGORY;
    
    double checkSetTime(int iterations, int type) {
        _checkSetTime(1,type);
        double result = _checkSetTime(iterations, type);
        logln((type == 0 ? "UnicodeMap" : type == 1 ? "HashMap" : type == 2 ? "ICU" : "TreeMap") + "\t" + nf.format(result));
        return result;
    }
    
    double _checkSetTime(int iterations, int type) {
        UnicodeMap<String> map1 = SCRIPTS;
        Map<Integer,String> map2 = map1.putAllCodepointsInto(new HashMap<Integer,String>());
        Map<Integer, String> map3 = new TreeMap<Integer, String>(map2);
        System.gc();
        double start = System.currentTimeMillis();
        for (int j = 0; j < iterations; ++j)
          for (int cp = 0; cp <= SET_LIMIT; ++cp) {
            int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
            if (enumValue <= 0) continue; // for smaller set
            String value = UCharacter.getPropertyValueName(propEnum,enumValue, UProperty.NameChoice.LONG);
            switch(type) {
            case 0: map1.put(cp, value); break;
            case 1: map2.put(cp, value); break;
            case 3: map3.put(cp, value); break;
            }
        }
        double end = System.currentTimeMillis();
        return (end-start)/1000/iterations;
    }
    
    double checkGetTime(int iterations, int type) {
        UnicodeMap<String> map1 = new UnicodeMap<String>();
        Map<Integer,String> map2 = map1.putAllCodepointsInto(new HashMap<Integer,String>());
        Map<Integer, String> map3 = new TreeMap<Integer, String>();
        _checkGetTime(map1, map2, map3, 1,type); // warmup
        double result = _checkGetTime(map1, map2, map3, iterations, type);
        logln((type == 0 ? "UnicodeMap" : type == 1 ? "HashMap" : type == 2 ? "ICU" : "TreeMap") + "\t" + nf.format(result));
        return result;
    }
    
    double _checkGetTime(UnicodeMap<String> map1, Map<Integer,String> map2, Map<Integer,String> map3, int iterations, int type) {
        System.gc();
        double start = System.currentTimeMillis();
        for (int j = 0; j < iterations; ++j)
          for (int cp = 0; cp < CHECK_LIMIT; ++cp) {
            switch (type) {
            case 0: map1.getValue(cp); break;
            case 1: map2.get(cp); break;
            case 2:
                int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
                //if (enumValue <= 0) continue;
                UCharacter.getPropertyValueName(propEnum,enumValue, UProperty.NameChoice.LONG);
                break;                
            case 3: map3.get(cp); break;
            }
        }
        double end = System.currentTimeMillis();
        return (end-start)/1000/iterations;
    }
    
    static class UnicodeMapBoilerplate extends TestBoilerplate {

        /* 
         * @see com.ibm.icu.dev.test.TestBoilerplate#_hasSameBehavior(java.lang.Object, java.lang.Object)
         */
        protected boolean _hasSameBehavior(Object a, Object b) {
            // we are pretty confident in the equals method, so won't bother with this right now.
            return true;
        }

        /*
         * @see com.ibm.icu.dev.test.TestBoilerplate#_createTestObject()
         */
        protected boolean _addTestObject(List list) {
            if (list.size() > 30) return false;
            UnicodeMap result = new UnicodeMap();
            for (int i = 0; i < 50; ++i) {
                int start = random.nextInt(25);
                String value = TEST_VALUES[random.nextInt(TEST_VALUES.length)];
                result.put(start, value);
            }
            list.add(result);
            return true;
        }
    }
    
    static class StringBoilerplate extends TestBoilerplate {

        /* 
         * @see com.ibm.icu.dev.test.TestBoilerplate#_hasSameBehavior(java.lang.Object, java.lang.Object)
         */
        protected boolean _hasSameBehavior(Object a, Object b) {
            // we are pretty confident in the equals method, so won't bother with this right now.
            return true;
        }

        /*
         * @see com.ibm.icu.dev.test.TestBoilerplate#_createTestObject()
         */
        protected boolean _addTestObject(List list) {
            if (list.size() > 31) return false;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 10; ++i) {
                result.append((char)random.nextInt(0xFF));
            }
            list.add(result.toString());
            return true;
        }
    }
    
    static class UnicodeSetBoilerplate extends TestBoilerplate {

        /* 
         * @see com.ibm.icu.dev.test.TestBoilerplate#_hasSameBehavior(java.lang.Object, java.lang.Object)
         */
        protected boolean _hasSameBehavior(Object a, Object b) {
            // we are pretty confident in the equals method, so won't bother with this right now.
            return true;
        }

        /*
         * @see com.ibm.icu.dev.test.TestBoilerplate#_createTestObject()
         */
        protected boolean _addTestObject(List list) {
            if (list.size() > 32) return false;
            UnicodeSet result = new UnicodeSet();
            for (int i = 0; i < 50; ++i) {
                result.add(random.nextInt(100));
            }
            list.add(result.toString());
            return true;
        }
    }

}
