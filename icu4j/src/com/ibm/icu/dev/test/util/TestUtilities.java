package com.ibm.icu.dev.test.util;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
    
    public void TestUnicodeMap() {
        Random random = new Random(12345);
        String[] values = {null, "A", "B", "C", "D", "E", "F"};
        // do random change to both, then compare
        logln("Comparing against HashMap");
        for (int counter = 0; counter < ITERATIONS; ++counter) {
            int start = random.nextInt(LIMIT);
            String value = values[random.nextInt(values.length)];
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
        if (!values1.equals(values2)) {
            errln("Values differ:");
            errln("UnicodeMap - HashMap");
            temp = new TreeSet(values1);
            temp.removeAll(values2);
            errln(show(temp));
            errln("HashMap - UnicodeMap");
            temp = new TreeSet(values2);
            temp.removeAll(values1);
            errln(show(temp));
        } else {
            logln("Comparing Sets");
            for (Iterator it = values1.iterator(); it.hasNext();) {
                Object value = it.next();
                logln(value == null ? "null" : value.toString());
                UnicodeSet set1 = map1.getSet(value);
                UnicodeSet set2 = getSet(map2, value);
                if (!set1.equals(set2)) {
                    errln("Sets differ:");
                    errln("UnicodeMap - HashMap");
                    errln(new UnicodeSet(set1).removeAll(set2).toPattern(true));
                    errln("HashMap - UnicodeMap");
                    errln(new UnicodeSet(set2).removeAll(set1).toPattern(true));
                }
            }
        }
              
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
                errln("Log: " + show(log));
                errln("HashMap: " + show(map2));
                throw new IllegalArgumentException("Halting");
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
    
    String show(Collection c) {
        StringBuffer buffer = new StringBuffer();
        for (Iterator it = c.iterator(); it.hasNext();) {
            buffer.append(it.next() + "\r\n");
        }
        return buffer.toString();
    }
    
    String show(Map m) {
        StringBuffer buffer = new StringBuffer();
        for (Iterator it = m.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            buffer.append(key + "=>" + m.get(key) + "\r\n");
        }
        return buffer.toString();
    }
    
    UnicodeSet getSet(Map m, Object value) {
        UnicodeSet result = new UnicodeSet();
        for (Iterator it = m.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object val = m.get(key);
            if (!val.equals(value)) continue;
            result.add(((Integer)key).intValue());
        }
        return result;
    }
}