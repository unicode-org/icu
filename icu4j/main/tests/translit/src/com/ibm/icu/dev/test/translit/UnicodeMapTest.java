/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * @test
 * @summary General test of UnicodeSet
 */
public class UnicodeMapTest extends TestFmwk {

    static final int MODIFY_TEST_LIMIT = 32;
    static final int MODIFY_TEST_ITERATIONS = 100000;

    public static void main(String[] args) throws Exception {
        new UnicodeMapTest().run(args);
    }

    public void TestAMonkey() {
        SortedMap<String,Integer> stayWithMe = new TreeMap<String,Integer>(OneFirstComparator);

        UnicodeMap<Integer> me = new UnicodeMap<Integer>().putAll(stayWithMe);
        // check one special case, removal near end
        me.putAll(0x10FFFE, 0x10FFFF, 666);
        me.remove(0x10FFFF);
        
        int iterations = 100000;
        SortedMap<String,Integer> test = new TreeMap();

        Random rand = new Random(0);
        String other;
        Integer value;
        // try modifications
        for (int i = 0; i < iterations ; ++i) {
            switch(rand.nextInt(20)) {
            case 0:
                logln("clear");
                stayWithMe.clear();
                me.clear();
                break;
            case 1:
                fillRandomMap(rand, 5, test);
                logln("putAll\t" + test);
                stayWithMe.putAll(test);
                me.putAll(test);
                break;
            case 2: case 3: case 4: case 5: case 6: case 7: case 8:
                other = getRandomKey(rand);
//                if (other.equals("\uDBFF\uDFFF") && me.containsKey(0x10FFFF) && me.get(0x10FFFF).equals(me.get(0x10FFFE))) {
//                    System.out.println("Remove\t" + other + "\n" + me);
//                }
                logln("remove\t" + other);
                stayWithMe.remove(other);
                try {
                    me.remove(other);
                } catch (IllegalArgumentException e) {
                    errln("remove\t" + other + "\tfailed: " + e.getMessage() + "\n" + me);
                    me.clear();
                    stayWithMe.clear();
                }
                break;
            default:
                other = getRandomKey(rand);
                value = rand.nextInt(50)+50;
                logln("put\t" + other + " = " + value);
                stayWithMe.put(other, value);
                me.put(other,value);
                break;
            }
            checkEquals(me, stayWithMe);
        }
    }

    /**
     * @param rand 
     * @param nextInt
     * @param test 
     * @return
     */
    private SortedMap<String, Integer> fillRandomMap(Random rand, int max, SortedMap<String, Integer> test) {
        test.clear();
        max = rand.nextInt(max);
        for (int i = 0; i < max; ++i) {
            test.put(getRandomKey(rand), rand.nextInt(50)+50);
        }
        return test;
    }

    Set temp = new HashSet();
    /**
     * @param me
     * @param stayWithMe
     */
    private void checkEquals(UnicodeMap<Integer> me, SortedMap<String, Integer> stayWithMe) {
        temp.clear();
        for (Entry<String, Integer> e : me.entrySet()) {
            temp.add(e);
        }
        Set<Entry<String, Integer>> entrySet = stayWithMe.entrySet();
        if (!entrySet.equals(temp)) {
            logln(me.entrySet().toString());
            logln(me.toString());
            assertEquals("are in parallel", entrySet, temp);
            // we failed. Reset and start again
            entrySet.clear();
            temp.clear();
            return;
        }
        for (String key : stayWithMe.keySet()) {
            assertEquals("containsKey", stayWithMe.containsKey(key), me.containsKey(key));
            Integer value = stayWithMe.get(key);
            assertEquals("get", value, me.get(key));
            assertEquals("containsValue", stayWithMe.containsValue(value), me.containsValue(value));
            int cp = UnicodeSet.getSingleCodePoint(key);
            if (cp != Integer.MAX_VALUE) {
                assertEquals("get", value, me.get(cp));
            }
        }
        Set<String> nonCodePointStrings = stayWithMe.tailMap("").keySet();
        if (nonCodePointStrings.size() == 0) nonCodePointStrings = null; // for parallel api
        assertEquals("getNonRangeStrings", nonCodePointStrings, me.getNonRangeStrings());
        
        TreeSet<Integer> values = new TreeSet<Integer>(stayWithMe.values());
        TreeSet<Integer> myValues = new TreeSet<Integer>(me.values());
        assertEquals("values", myValues, values);

        for (String key : stayWithMe.keySet()) {
            assertEquals("containsKey", stayWithMe.containsKey(key), me.containsKey(key));
        }
    }
    
    static Comparator<String> OneFirstComparator = new Comparator<String>() {
        public int compare(String o1, String o2) {
            int cp1 = UnicodeSet.getSingleCodePoint(o1);
            int cp2 = UnicodeSet.getSingleCodePoint(o2);
            int result = cp1 - cp2;
            if (result != 0) {
                return result;
            }
            if (cp1 == Integer.MAX_VALUE) {
                return o1.compareTo(o2);
            }
            return 0;
        }
        
    };

    /**
     * @param rand 
     * @param others
     * @return
     */
    private String getRandomKey(Random rand) {
        int r = rand.nextInt(30);
        if (r == 0) {
            return UTF16.valueOf(r); 
        } else if (r < 10) {
            return UTF16.valueOf('A'-1+r);
        } else if (r < 20) {
            return UTF16.valueOf(0x10FFFF - (r-10));
//        } else if (r == 20) {
//            return "";
        }
        return "a" + UTF16.valueOf(r + 'a'-1);
    }

    public void TestModify() {
        Random random = new Random(0);
        UnicodeMap unicodeMap = new UnicodeMap();
        HashMap hashMap = new HashMap();
        String[] values = {null, "the", "quick", "brown", "fox"};
        for (int count = 1; count <= MODIFY_TEST_ITERATIONS; ++count) {
            String value = values[random.nextInt(values.length)];
            int start = random.nextInt(MODIFY_TEST_LIMIT); // test limited range
            int end = random.nextInt(MODIFY_TEST_LIMIT);
            if (start > end) {
                int temp = start;
                start = end;
                end = temp;
            }
            int modCount = count & 0xFF;
            if (modCount == 0 && isVerbose()) {
                logln("***"+count);
                logln(unicodeMap.toString());
            }
            unicodeMap.putAll(start, end, value);
            if (modCount == 1 && isVerbose()) {
                logln(">>>\t" + Utility.hex(start) + ".." + Utility.hex(end) + "\t" + value);
                logln(unicodeMap.toString());
            }
            for (int i = start; i <= end; ++i) {
                hashMap.put(new Integer(i), value);
            }
            if (!hasSameValues(unicodeMap, hashMap)) {
                errln("Failed at " + count);
            }
        }
    }

    private boolean hasSameValues(UnicodeMap unicodeMap, HashMap hashMap) {
        for (int i = 0; i < MODIFY_TEST_LIMIT; ++i) {
            Object unicodeMapValue = unicodeMap.getValue(i);
            Object hashMapValue = hashMap.get(new Integer(i));
            if (unicodeMapValue != hashMapValue) {
                return false;
            }
        }
        return true;
    }
}
