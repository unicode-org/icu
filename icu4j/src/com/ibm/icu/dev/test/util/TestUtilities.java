//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.TestBoilerplate;
import com.ibm.icu.dev.test.TestFmwk;
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
    
    UnicodeMap map1 = new UnicodeMap();
    Map map2 = new HashMap();
    Map map3 = new TreeMap();
    SortedSet log = new TreeSet();
    static String[] TEST_VALUES = {null, "A", "B", "C", "D", "E", "F"};
    static Random random = new Random(12345);
    
    public void TestUnicodeMap() {
        random.setSeed(12345);
        // do random change to both, then compare
        logln("Comparing against HashMap");
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
            map2.put(new Integer(start), value);
            check(counter);
        }
        checkNext(LIMIT);
        
        logln("Setting General Category");
        map1 = new UnicodeMap();
        map2 = new TreeMap();
        for (int cp = 0; cp <= SET_LIMIT; ++cp) {
              int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
              //if (enumValue <= 0) continue; // for smaller set
              String value = UCharacter.getPropertyValueName(propEnum,enumValue, UProperty.NameChoice.LONG);
              map1.put(cp, value);
              map2.put(new Integer(cp), value);
        }       
        checkNext(Integer.MAX_VALUE);


        logln("Comparing General Category");
        check(-1);
        logln("Comparing Values");
        Set values1 = (Set) map1.getAvailableValues(new TreeSet());
        Set values2 = new TreeSet(map2.values());
        if (!TestBoilerplate.verifySetsIdentical(this, values1, values2)) {
            throw new IllegalArgumentException("Halting");
        }
        logln("Comparing Sets");
        for (Iterator it = values1.iterator(); it.hasNext();) {
            Object value = it.next();
            logln(value == null ? "null" : value.toString());
            UnicodeSet set1 = map1.getSet(value);
            UnicodeSet set2 = TestBoilerplate.getSet(map2, value);
            if (!TestBoilerplate.verifySetsIdentical(this, set1, set2)) {
                throw new IllegalArgumentException("Halting");
            }
        } 
        
        logln("Getting Scripts");
        UnicodeMap scripts = ICUPropertyFactory.make().getProperty("script").getUnicodeMap_internal();
        UnicodeMap.Composer composer = new UnicodeMap.Composer() {
			public Object compose(int codePoint, Object a, Object b) {
				return a.toString() + "_" + b.toString();
			}       	
        };
        
        logln("Trying Compose");
        UnicodeMap composed = ((UnicodeMap)scripts.cloneAsThawed()).composeWith(map1, composer);
        Object last = "";
        for (int i = 0; i < 0x10FFFF; ++i) {
        	Object comp = composed.getValue(i);
        	Object gc = map1.getValue(i);
        	Object sc = scripts.getValue(i);
        	if (!comp.equals(composer.compose(i, gc, sc))) {
        		errln("Failed compose at: " + i);
        	}
        	if (!last.equals(comp)) {
        		logln(Utility.hex(i) + "\t" + comp);
        		last = comp;
        	}
        }

        // check boilerplate
        List argList = new ArrayList();
        argList.add("TestMain");
        if (params.nothrow) argList.add("-nothrow");
        if (params.verbose) argList.add("-verbose");
        String[] args = new String[argList.size()];
        argList.toArray(args);
        new UnicodeMapBoilerplate().run(args);
         // TODO: the following is not being reached
        new UnicodeSetBoilerplate().run(args);       
    }
    
    public void TestCollectionUtilitySpeed() {
        HashSet hs1 = new HashSet();
        HashSet hs2 = new HashSet();
        int size = 100000;
        int iterations = 100;
        String prefix = "abcde";
        String postfix = "abcde";
        int start1 = 0; // 1 for some, 0 for all
        for (int i = 0; i < size; i += 2) hs1.add(prefix + String.valueOf(i) + postfix);
        for (int i = start1; i < size; i += 2) hs2.add(prefix + String.valueOf(i) + postfix);
        TreeSet ts1 = new TreeSet(hs1);
        TreeSet ts2 = new TreeSet(hs2);
        CollectionUtilities.containsAll(hs1, hs2);
        CollectionUtilities.containsAll(ts1, ts2);
        long start, end;
        boolean temp = false;
        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; ++i) temp = CollectionUtilities.containsAll(hs1, hs2);
        end = System.currentTimeMillis();
        logln(temp + " " + (end - start)/1000.0);
        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; ++i) temp = CollectionUtilities.containsAll(ts1, ts2);
        end = System.currentTimeMillis();
        logln(temp + " " + (end - start)/1000.0);
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

	private void checkNext(int limit) {
        logln("Comparing nextRange");
        UnicodeMap.MapIterator mi = new UnicodeMap.MapIterator(map1);
        Map localMap = new TreeMap();
        while (mi.nextRange()) {
            logln(Utility.hex(mi.codepoint) + ".." + Utility.hex(mi.codepointEnd) + " => " + mi.value);
            for (int i = mi.codepoint; i <= mi.codepointEnd; ++i) {
                if (i >= limit) continue;
                localMap.put(new Integer(i), mi.value);
            }
        }
        checkMap(map2, localMap);
        
        logln("Comparing next");
        mi.reset();
        localMap = new TreeMap();
        Object lastValue = new Object();
        while (mi.next()) {
            if (!UnicodeMap.areEqual(lastValue, mi.value)) {
                // System.out.println("Change: " + Utility.hex(mi.codepoint) + " => " + mi.value);
                lastValue = mi.value;
            }
            if (mi.codepoint >= limit) continue;
            localMap.put(new Integer(mi.codepoint), mi.value);
        }
        checkMap(map2, localMap);
    }
    
    public void check(int counter) {
        for (int i = 0; i < LIMIT; ++i) {
            Object value1 = map1.getValue(i);
            Object value2 = map2.get(new Integer(i));
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
        StringBuffer buffer = new StringBuffer();
        Set m1entries = m1.entrySet();
        Set m2entries = m2.entrySet();
        getEntries("\r\nIn First, and not Second", m1entries, m2entries, buffer, 20);
        getEntries("\r\nIn Second, and not First", m2entries, m1entries, buffer, 20);
        errln(buffer.toString());
    }
    
    static Comparator ENTRY_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            Map.Entry a = (Map.Entry) o1;
            Map.Entry b = (Map.Entry) o2;
            int result = compare2(a.getKey(), b.getKey());
            if (result != 0) return result;
            return compare2(a.getValue(), b.getValue());
        }
        private int compare2(Object o1, Object o2) {
            if (o1 == o2) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return ((Comparable)o1).compareTo(o2);
        }
    };

    private void getEntries(String title, Set m1entries, Set m2entries, StringBuffer buffer, int limit) {
        Set m1_m2 = new TreeSet(ENTRY_COMPARATOR);
        m1_m2.addAll(m1entries);
        m1_m2.removeAll(m2entries);
        buffer.append(title + ": " + m1_m2.size() + "\r\n");
        for (Iterator it = m1_m2.iterator(); it.hasNext();) {
            if (limit-- < 0) return;
            Map.Entry entry = (Map.Entry) it.next();
            buffer.append(entry.getKey()).append(" => ")
             .append(entry.getValue()).append("\r\n");
        }
    }
    
    static final int SET_LIMIT = 0x10FFFF;
    static final int CHECK_LIMIT = 0xFFFF;
    static final NumberFormat pf = NumberFormat.getPercentInstance();
    static final NumberFormat nf = NumberFormat.getInstance();
    
    public void TestTime() {
        double hashTime, umTime, icuTime, treeTime;
        umTime = checkSetTime(20, 0);
        hashTime = checkSetTime(20, 1);
        logln("Percentage: " + pf.format(hashTime/umTime));
        treeTime = checkSetTime(20, 3);
        logln("Percentage: " + pf.format(treeTime/umTime));
        //logln(map1.toString());
        
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
        map1 = new UnicodeMap();
        map2 = new HashMap();
        System.gc();
        double start = System.currentTimeMillis();
        for (int j = 0; j < iterations; ++j)
          for (int cp = 0; cp <= SET_LIMIT; ++cp) {
            int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
            if (enumValue <= 0) continue; // for smaller set
            String value = UCharacter.getPropertyValueName(propEnum,enumValue, UProperty.NameChoice.LONG);
            switch(type) {
            case 0: map1.put(cp, value); break;
            case 1: map2.put(new Integer(cp), value); break;
            case 3: map3.put(new Integer(cp), value); break;
            }
        }
        double end = System.currentTimeMillis();
        return (end-start)/1000/iterations;
    }
    
    double checkGetTime(int iterations, int type) {
        _checkGetTime(1,type);
        double result = _checkGetTime(iterations, type);
        logln((type == 0 ? "UnicodeMap" : type == 1 ? "HashMap" : type == 2 ? "ICU" : "TreeMap") + "\t" + nf.format(result));
        return result;
    }
    double _checkGetTime(int iterations, int type) {
        System.gc();
        double start = System.currentTimeMillis();
        for (int j = 0; j < iterations; ++j)
          for (int cp = 0; cp < CHECK_LIMIT; ++cp) {
            switch (type) {
            case 0: map1.getValue(cp); break;
            case 1: map2.get(new Integer(cp)); break;
            case 2:
                int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
                //if (enumValue <= 0) continue;
                UCharacter.getPropertyValueName(propEnum,enumValue, UProperty.NameChoice.LONG);
                break;                
            case 3: map3.get(new Integer(cp)); break;
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
            StringBuffer result = new StringBuffer();
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
//#endif
