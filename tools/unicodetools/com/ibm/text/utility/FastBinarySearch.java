
/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/FastBinarySearch.java,v $
* $Date: 2002/10/01 01:19:15 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

import java.util.Random;
import java.util.Arrays;
import com.ibm.icu.text.NumberFormat;

/**
 * Quick & Dirty test program for fast (unrolled) binary search
 * Should use new PerfTest once that is done, although since there is no object
 * creation the numbers should be fairly reliable.
 */

final public class FastBinarySearch {
    
    /**
     * Testing
     */
    
    static public void test() {
        perfTest(100, 100); // warmup
        
        // try different combinations of data size and iterations
        
        perfTest(100, 200000);
        perfTest(1000, 2000);
        perfTest(100000, 200);
        
        // skip the following
        if (true) return;
        validityTest();
    }
    
    static void perfTest(int dataSize, int iterations) {
        NumberFormat percent = NumberFormat.getPercentInstance();
        percent.setMaximumFractionDigits(0);
        
        Random random = new Random(123456789L);
        int[] myData = new int[dataSize];
        FastBinarySearch fbs = new FastBinarySearch();
        
        // produce test case
        for (int i = 0; i < myData.length; ++i) {
            myData[i] = (int) (random.nextDouble() * myData.length * 3);
        }
        Arrays.sort(myData, 0, myData.length);
        fbs.setData(myData, myData.length);
        
        // produce probe data
        int[] probe = new int[myData.length*2];
        for (int i = 0; i < probe.length; ++i) {
            probe[i] = (int) (random.nextDouble() * myData.length * 3);
        }
        
        int sum = 0;
        double startTime, endTime, time, baseTime;
        
        System.out.println();
        long totalIterations = iterations * probe.length;
        System.out.println("Iterations = " + totalIterations + ", Data size = " + dataSize);
        
        startTime = System.currentTimeMillis();
        for (int testCount = 0; testCount < iterations; ++testCount) {
            for (int i = 0; i < probe.length; ++i) {
                sum += fbs.findCodePoint(i);
            }
        }
        endTime = System.currentTimeMillis();
        baseTime = time = (endTime - startTime)*1000/totalIterations;
        System.out.println("Basic; time=" + time + " microsecs/call");
        
        startTime = System.currentTimeMillis();
        for (int testCount = 0; testCount < iterations; ++testCount) {
            for (int i = 0; i < probe.length; ++i) {
                sum += fbs.highestIndexLEQ(i);
            }
        }
        endTime = System.currentTimeMillis();
        time = (endTime - startTime)*1000/totalIterations;
        System.out.println("Fast; time=" + time + " microsecs/call\t" + percent.format(time/baseTime-1));

        startTime = System.currentTimeMillis();
        for (int testCount = 0; testCount < iterations; ++testCount) {
            for (int i = 0; i < probe.length; ++i) {
                sum += fbs.highestIndexLEQ2(i);
            }
        }
        endTime = System.currentTimeMillis();
        time = (endTime - startTime)*1000/totalIterations;
        System.out.println("Compact; time=" + time + " microsecs/call\t" + percent.format(time/baseTime-1));
    }
    
    
    static void validityTest() {
        Random random = new Random(123456789L);
        int[] myData = new int[50];
        FastBinarySearch fbs = new FastBinarySearch();
        
        for (int testCount = 0; testCount < 100; ++testCount) {
            
            // produce test case
            double ran = random.nextDouble();
            //System.out.println(ran);
            int myCount = 2+ (int) (ran * (myData.length - 2));
            for (int i = 0; i < myCount; ++i) {
                ran = random.nextDouble();
                //System.out.println(ran);
                myData[i] = (int) (ran * myData.length * 3);
            }
            System.out.println("Trial " + testCount + ", len: " + myCount);
            Arrays.sort(myData, 0, myCount);
            fbs.setData(myData, myCount);
            
            // compare brute force & fast methods
            boolean ok = true;
            for (int i = -1; i < myData.length * 3 + 1; ++i) {
                int brute = fbs.bruteForce(i);
                int fast = fbs.highestIndexLEQ(i);
                if (fast != brute) {
                    if (ok) {
                        System.out.println(fbs);
                    }
                    System.out.println("Error: probe=" + i + ", brute=" + brute + ", fast=" + fast);
                    fast = fbs.highestIndexLEQ(i);  // do again with debugger
                    ok = false;
                }
            }
            if (!ok) return;
        }
    }
    
    /**
     * Set the data to be scanned. It must be in sorted order.
     */
    
    public void setData(int data[], int count) {
        
        this.data = (int[]) data.clone(); // clone for safety
        isValid = this.count == count; // isValid only depends on the count remaining the same
        this.count = count;
    }
    
    /**
     * Basic binary search
     */

    private final int findCodePoint(int c) {
        // Return the smallest i such that c < list[i].  Assume
        // list[len - 1] == HIGH and that c is legal (0..HIGH-1).
        if (c < data[0]) return 0;
        int lo = 0;
        int hi = count - 1;
        // invariant: c >= list[lo]
        // invariant: c < list[hi]
        for (;;) {
            int i = (lo + hi) >>> 1;
            if (i == lo) return hi;
            if (c < data[i]) {
                hi = i;
            } else {
                lo = i;
            }
        }
    }
    
    /**
     * @return greatest index whose value is less than or equal to the searchValue.
     * If there is no such index, then -1 is returned
     */

    public int bruteForce(int searchValue) {
        int i = count;
        while (--i >= 0 && data[i] > searchValue) {}
        return i;
    }
    
    /**
     * @return greatest index such that data[index] <= searchValue
     * If there is no such index (e.g. searchValue < data[0]), then -1 is returned
     */
     
	public int highestIndexLEQ(int searchValue) {
	    
		if (!isValid) validate();
		int temp;
		
		// set up initial range to search. Each subrange is a power of two in length
		int high = searchValue < data[topOfLow] ? topOfLow : topOfHigh;

		// Completely unrolled binary search, folhighing "Programming Pearls"
		// Each case deliberately falls through to the next
		// Logically, data[-1] < all_search_values && data[count] > all_search_values
		// although the values -1 and count are never actually touched.
		
		// The bounds at each point are low & high,
		// where low == high - delta*2
		// so high - delta is the midpoint
		
		// The invariant AFTER each line is that data[low] < searchValue <= data[high]
		
		switch (power) {
		//case 31: if (searchValue < data[temp = high-0x40000000]) high = temp; // no unsigned int in Java
		case 30: if (searchValue < data[temp = high-0x20000000]) high = temp;
		case 29: if (searchValue < data[temp = high-0x10000000]) high = temp;
		
		case 28: if (searchValue < data[temp = high- 0x8000000]) high = temp;
		case 27: if (searchValue < data[temp = high- 0x4000000]) high = temp;
		case 26: if (searchValue < data[temp = high- 0x2000000]) high = temp;
		case 25: if (searchValue < data[temp = high- 0x1000000]) high = temp;
		
		case 24: if (searchValue < data[temp = high-  0x800000]) high = temp;
		case 23: if (searchValue < data[temp = high-  0x400000]) high = temp;
		case 22: if (searchValue < data[temp = high-  0x200000]) high = temp;
		case 21: if (searchValue < data[temp = high-  0x100000]) high = temp;
		
		case 20: if (searchValue < data[temp = high-   0x80000]) high = temp;
		case 19: if (searchValue < data[temp = high-   0x40000]) high = temp;
		case 18: if (searchValue < data[temp = high-   0x20000]) high = temp;
		case 17: if (searchValue < data[temp = high-   0x10000]) high = temp;
		
		case 16: if (searchValue < data[temp = high-    0x8000]) high = temp;
		case 15: if (searchValue < data[temp = high-    0x4000]) high = temp;
		case 14: if (searchValue < data[temp = high-    0x2000]) high = temp;
		case 13: if (searchValue < data[temp = high-    0x1000]) high = temp;
		
		case 12: if (searchValue < data[temp = high-     0x800]) high = temp;
		case 11: if (searchValue < data[temp = high-     0x400]) high = temp;
		case 10: if (searchValue < data[temp = high-     0x200]) high = temp;
		case  9: if (searchValue < data[temp = high-     0x100]) high = temp;
		
		case  8: if (searchValue < data[temp = high-      0x80]) high = temp;
		case  7: if (searchValue < data[temp = high-      0x40]) high = temp;
		case  6: if (searchValue < data[temp = high-      0x20]) high = temp;
		case  5: if (searchValue < data[temp = high-      0x10]) high = temp;
		
		case  4: if (searchValue < data[temp = high-       0x8]) high = temp;
		case  3: if (searchValue < data[temp = high-       0x4]) high = temp;
		case  2: if (searchValue < data[temp = high-       0x2]) high = temp;
		case  1: if (searchValue < data[temp = high-       0x1]) high = temp;
		}
		if (high == topOfHigh && searchValue >= data[high]) return high;
		return high-1;
	}


	// NOTE: on some machines the above may not be optimal, if the size of the function
	// forces code out of the cache. For that case, it would be better for program in a loop, like the following
	
	public int highestIndexLEQ2(int searchValue) {
	    
		if (!isValid) validate();
		int temp;
		int high = searchValue < data[topOfLow] ? topOfLow : topOfHigh;
		for (int delta = deltaStart; delta != 0; delta >>= 1) {
            if (searchValue < data[temp = high-delta]) high = temp;
        }
		if (high == topOfHigh && searchValue >= data[high]) return high;
		return high-1;
	}
	
	/**
	 * For debugging
	 */
	public String toString() {
        String result = "[";
        for (int j = 0; j < count; ++j) {
            if (j != 0) result += ", ";
            result += data[j];
        }
        result += "]";
        result += ", power: " + power;
        result += ", topOfLow: " + topOfLow;
        result += ", topOfHigh: " + topOfHigh;
        return result;
    }
	
	
	// ================ Privates ================
	
	// data
	
	int data[];
	int count;
    
    // validate internal parameters
    
	private void validate() {
	    if (count <= 1) throw new IllegalArgumentException("Array must have at least 2 elements");

		// find greatest power of 2 less than or equal to count
		for (power = exp2.length-1; power > 0 && exp2[power] > count; power--) {}

		// determine the starting points
		topOfLow = exp2[power] - 1;
		topOfHigh = count - 1;
		deltaStart = exp2[power-1];
		isValid = true;
	}
    
	private boolean isValid = false;
	private int topOfLow;
	private int topOfHigh;
	private int power;
	private int deltaStart;
	
	private static final int exp2[] = {
	    0x1, 0x2, 0x4, 0x8,
	    0x10, 0x20, 0x40, 0x80,
	    0x100, 0x200, 0x400, 0x800,
	    0x1000, 0x2000, 0x4000, 0x8000,
	    0x10000, 0x20000, 0x40000, 0x80000,
	    0x100000, 0x200000, 0x400000, 0x800000,
	    0x1000000, 0x2000000, 0x4000000, 0x8000000,
	    0x10000000, 0x20000000 // , 0x40000000 // no unsigned int in Java
	};
}