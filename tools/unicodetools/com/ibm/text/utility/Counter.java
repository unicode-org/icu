package com.ibm.text.utility;


import java.io.IOException;
//import com.ibm.text.unicode.UInfo;
import java.util.*;
import java.io.*;
import java.text.*;

public final class Counter {
    Map map = new HashMap();

    static public final class RWInteger implements Comparable {
        static int uniqueCount;
        public int value;
        private int forceUnique = uniqueCount++;

        // public RWInteger() {
          //  forceUnique

        public int compareTo(Object other) {
            RWInteger that = (RWInteger) other;
            if (that.value < value) return -1;
            else if (that.value > value) return 1;
            else if (that.forceUnique < forceUnique) return -1;
            else if (that.forceUnique > forceUnique) return 1;
            return 0;
        }
        public String toString() {
            return String.valueOf(value);
        }
    }

    public void add(String obj) {
        RWInteger count = (RWInteger)map.get(obj);
        if (count == null) {
            count = new RWInteger();
            map.put(obj, count);
        }
        count.value += obj.length();
    }

    public Map getSortedByCount() {
        Map result = new TreeMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            Object count = map.get(key);
            result.put(count, key);
        }
        return result;
    }

    public Map getKeyToKey() {
        Map result = new HashMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            result.put(key, key);
        }
        return result;
    }


}