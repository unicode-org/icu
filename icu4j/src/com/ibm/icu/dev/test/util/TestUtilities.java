package com.ibm.icu.dev.test.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
    UnicodeMap.Equator equator = new UnicodeMap.SimpleEquator();
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
        logln("Setting General Category");
        map1 = new UnicodeMap();
        map2 = new HashMap();
        for (int cp = 0; cp < SET_LIMIT; ++cp) {
              int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
              //if (enumValue <= 0) continue; // for smaller set
              String value = UCharacter.getPropertyValueName(propEnum,enumValue, UProperty.NameChoice.LONG);
              map1.put(cp, value);
              map2.put(new Integer(cp), value);
        }
        logln("Comparing General Category");
        check(-1);
        logln("Comparing Values");
        Set values1 = (Set) map1.getAvailableValues(new TreeSet());
        Set values2 = new TreeSet(map2.values());
        Set temp;
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
    
    public void check(int counter) {
        for (int i = 0; i < LIMIT; ++i) {
            Object value1 = map1.getValue(i);
            Object value2 = map2.get(new Integer(i));
            if (!equator.isEqual(value1, value2)) {
                errln(counter + " Difference at " + Utility.hex(i)
                     + "\t UnicodeMap: " + value1
                     + "\t HashMap: " + value2);
                errln("UnicodeMap: " + map1);
                errln("Log: " + TestBoilerplate.show(log));
                errln("HashMap: " + TestBoilerplate.show(map2));
            }
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
          for (int cp = 0; cp < SET_LIMIT; ++cp) {
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
                String value = UCharacter.getPropertyValueName(propEnum,enumValue, UProperty.NameChoice.LONG);
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