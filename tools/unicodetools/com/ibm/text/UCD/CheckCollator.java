/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/CheckCollator.java,v $
* $Date: 2002/08/08 15:35:01 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

// http://java.sun.com/j2se/1.3/docs/guide/intl/encoding.doc.html

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
import java.text.NumberFormat;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * This is a quick and dirty program to get some idea of collation performance, comparing old Java to new stuff.
 */
abstract public class CheckCollator {
    static final String PREFIX = "C:\\ICUInternal\\icu4c\\collation-perf-data\\TestNames_";
    static final boolean DO_RAW = false;
    
    static final NumberFormat nf = NumberFormat.getInstance();
    static final NumberFormat percent = NumberFormat.getPercentInstance();
    static {
        nf.setMaximumFractionDigits(2);
    }
    
    public static void main(String[] args) throws IOException {
        
        // later, drive off of args
        
        // choices are: Asian, Chinese, Japanese, Japanese_h, Japanese_k, Korean, Latin, Russian, Thai
        test(Locale.KOREAN, "Korean");
        test(Locale.ENGLISH, "Latin");
        test(Locale.FRENCH, "Latin");
        test(Locale.JAPANESE, "Japanese");
    }
    
    public static void test(Locale loc, String name) throws IOException {
        
        System.out.println();
        System.out.println("Testing " + loc.getDisplayName() + ", file: " + name);
        System.out.println();

        // get test data
        
        String fileName = PREFIX + name + ".txt";
        
        FileInputStream fis = new FileInputStream(fileName);
        InputStreamReader isr = new InputStreamReader(fis, "UnicodeLittle");
        BufferedReader br = new BufferedReader(isr, 32*1024);

        int counter = 0;
        
        ArrayList list = new ArrayList();
        while (true) {
            String line = Utility.readDataLine(br);
            if (line == null) break;
            if (line.length() == 0) continue;
            Utility.dot(counter++);
            list.add(line);
        }
        System.out.println("Read " + counter + " lines in file");
        
        int limit = 800; // put a limit on it to save time
        
        // pump it up if there aren't very many
        while (list.size() < limit) {
            list.addAll(list);
        }
        
        int size = list.size();
        
        
        // later, adjust these so we always get a reasonble number of tries
        
        int extraIterations = 200;
        if (size > limit) size = limit;
        
        String[] tests = new String [size];
        
        for (int i = 0; i < size; ++i) {
            tests[i] = (String) list.get(i);
        }
        
        // get collators
        
        com.ibm.icu.text.Collator newCol = com.ibm.icu.text.Collator.getInstance(loc);
        java.text.Collator oldCol = java.text.Collator.getInstance(loc);
        
        
        double startTime, endTime;
        double delta, oldDelta;
        String probe;
        
        
        // load classes at least once before starting
        
        newCol.compare("a", "b");
        oldCol.compare("a", "b");
        
        // ================================================
        // check sort key size
        
        int stringSize = 0, newSize = 0, oldSize = 0;
        
        for (int i = 0; i < size; ++i) {
            stringSize += tests[i].length() * 2;
            byte[] newKey = newCol.getCollationKey(tests[i]).toByteArray();
            newSize += newKey.length;
            byte[] oldKey = oldCol.getCollationKey(tests[i]).toByteArray();
            oldSize += oldKey.length;
        }
        delta = stringSize/(size + 0.0);
        System.out.println("string size: " + nf.format(delta) + " bytes per key");
        System.out.println();

        delta = oldDelta = (oldSize/(size + 0.0));
        System.out.println("old sortkey size: " + nf.format(delta) + " bytes per key ");
        delta = (newSize/(size + 0.0));
        System.out.println("new sortkey size: " + nf.format(delta) + " bytes per key " + percent.format(delta/oldDelta));
        System.out.println();
       
        // ================================================
        // Sort Key: old time
        
        // get overhead time
        counter = 0;
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                counter++;
            }
        }
        endTime = System.currentTimeMillis();
        double overhead = (1000*(endTime - startTime) / counter);
        System.out.println("overhead: " + nf.format((endTime - startTime) / counter) + " micros");
        
        counter = 0;
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < size; ++i) {
            probe = tests[i];
            for (int k = 0; k < extraIterations; ++k) {
                oldCol.getCollationKey(probe);
                counter++;
            }
        }
        endTime = System.currentTimeMillis();
        oldDelta = delta = (1000*(endTime - startTime) / counter) - overhead;
        System.out.println("Old sort key time: " + nf.format(delta)
            + " micros (" + counter + " iterations)");
  
        // Sort Key: new time
        
        counter = 0;
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < size; ++i) {
            probe = tests[i];
            for (int k = 0; k < extraIterations; ++k) {
                newCol.getCollationKey(probe);
                counter++;
            }
        }
        endTime = System.currentTimeMillis();
        delta = (1000*(endTime - startTime) / counter) - overhead;
        System.out.println("New sort key time: " + nf.format(delta)
            + " micros (" + counter + " iterations) " + percent.format(delta/oldDelta));
        System.out.println();
        
        // ================================================
        // Raw Compare
        
        if (DO_RAW) {
            // get overhead time
            counter = 0;
            startTime = System.currentTimeMillis();
            int opt = 0; // to keep the compiler from optimizing out
            
            for (int i = 0; i < size; ++i) {
                probe = tests[i];
                for (int j = 0; j < size; ++j) {
                    opt ^= probe.compareTo(tests[j]);
                    counter++;
                }
            }
            endTime = System.currentTimeMillis();
            overhead = (1000*(endTime - startTime) / counter);
            System.out.println("overhead: " + nf.format((endTime - startTime) / counter) + " micros");
            
            // Raw Compare: old time
            
            counter = 0;
            startTime = System.currentTimeMillis();
            
            for (int i = 0; i < size; ++i) {
                probe = tests[i];
                for (int j = 0; j < size; ++j) {
                    opt ^= oldCol.compare(probe, tests[j]);
                    counter++;
                }
            }
            endTime = System.currentTimeMillis();
            oldDelta = delta = (1000*(endTime - startTime) / counter) - overhead;
            System.out.println("Old raw compare time: " + nf.format(delta)
                + " micros (" + counter + " iterations)");
            
            // Raw Compare: new time
            
            counter = 0;
            startTime = System.currentTimeMillis();
            
            for (int i = 0; i < size; ++i) {
                probe = tests[i];
                for (int j = 0; j < size; ++j) {
                    opt ^= newCol.compare(probe, tests[j]);
                    counter++;
                }
            }
            endTime = System.currentTimeMillis();
            delta = (1000*(endTime - startTime) / counter) - overhead;
            System.out.println("New raw compare time: " + nf.format(delta)
                + " micros (" + counter + " iterations) " + percent.format(delta/oldDelta));
            System.out.println();
        }
        
        // ================================================
        // Binary Search
        // note: I don't worry about getting the binary search precisely right, since I just want to
        // see which strings would get compared.
        
        // overhead
        
        int iterations = (size * extraIterations);
        startTime = System.currentTimeMillis();
        Arrays.sort(tests);
        int opt2 = 0; // keep from optimizing out
        
        for (int i = 0; i < size; ++i) {
            probe = tests[i];
            for (int k = 0; k < extraIterations; ++k) {
                opt2 ^= Arrays.binarySearch(tests, probe);
            }
        }
        endTime = System.currentTimeMillis();
        overhead = delta = (1000*(endTime - startTime) / iterations);
        System.out.println("Overhead: " + nf.format(delta)
            + " micros (" + iterations + " iterations)");
        
        // old time
        
        startTime = System.currentTimeMillis();
        Arrays.sort(tests, oldCol);
        
        for (int i = 0; i < size; ++i) {
            probe = tests[i];
            for (int k = 0; k < extraIterations; ++k) {
                opt2 ^= Arrays.binarySearch(tests, probe, oldCol);
            }
        }
        endTime = System.currentTimeMillis();
        oldDelta = delta = (1000*(endTime - startTime) / iterations) - overhead;
        System.out.println("Old binary search time: " + nf.format(delta)
            + " micros (" + iterations + " iterations)");
        
        
        // new time
        
        Arrays.sort(tests, newCol);
        
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < size; ++i) {
            probe = tests[i];
            for (int k = 0; k < extraIterations; ++k) {
                opt2 ^= Arrays.binarySearch(tests, probe, newCol);
            }
        }
        endTime = System.currentTimeMillis();
        delta = (1000*(endTime - startTime) / iterations) - overhead;
        System.out.println("New binary search time: " + nf.format(delta)
            + " micros (" + iterations + " iterations) " + percent.format(delta/oldDelta));
        System.out.println();
        
        // ================================================
        // Sort
        
        String[] sortTests = (String[]) tests.clone();
        extraIterations = 5;
        iterations = (size * extraIterations);
        
        // overhead
        
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < size; ++i) {
            for (int k = 0; k < extraIterations; ++k) {
                System.arraycopy(tests, 0, sortTests, 0, tests.length); // copy array
                Arrays.sort(sortTests);
            }
        }
        endTime = System.currentTimeMillis();
        overhead = delta = (1000*(endTime - startTime) / iterations);
        System.out.println("overhead: " + nf.format(delta)
            + " micros (" + iterations + " iterations)");
        
        // old time
        
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < size; ++i) {
            for (int k = 0; k < extraIterations; ++k) {
                System.arraycopy(tests, 0, sortTests, 0, tests.length); // copy array
                Arrays.sort(sortTests, oldCol);
            }
        }
        endTime = System.currentTimeMillis();
        oldDelta = delta = (1000*(endTime - startTime) / iterations) - overhead;
        System.out.println("Old sort time: " + nf.format(delta)
            + " micros (" + iterations + " iterations)");
        
        // new time
        
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < size; ++i) {
            for (int k = 0; k < extraIterations; ++k) {
                System.arraycopy(tests, 0, sortTests, 0, tests.length); // copy array
                Arrays.sort(sortTests, newCol);
            }
        }
        endTime = System.currentTimeMillis();
        delta = (1000*(endTime - startTime) / iterations) - overhead;
        System.out.println("New sort time: " + nf.format(delta)
            + " micros (" + iterations + " iterations) " + percent.format(delta/oldDelta));
 
    }
}