/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/Attic/PerfTest.java,v $ 
* $Date: 2002/09/19 23:00:52 $ 
* $Revision: 1.1 $
**********************************************************************
*/

package com.ibm.icu.dev.test;
import com.ibm.icu.text.NumberFormat;

/**
 * A class for doing performance testing.  To use, subclass and
 * implement the test() method.
 */
public abstract class PerfTest {

    /**
     * Subclasses must implement this method to do the action to
     * be measured.
     * @return the number of iterations run by this method, >= 1.
     */
    protected abstract int test();

    /**
     * Measure the time required by the test() method.
     * @param iterations number of times to call test().  Must be >= 1.
     * @return the time per iteration in seconds.  Iterations, in this
     * case, are counted as 'iterations' * the result of test() or
     * empty().
     */
    public double measure(int iterations, TestLog log) {
        if (iterations < 0) {
            throw new IllegalArgumentException("Invalid iterations");
        }

        int i, count;

        double start, stop, limit;

        // Call test() first
        count = 0;
        start = System.currentTimeMillis();
        for (i=0; i<iterations; ++i) {
            count += test();
        }
        stop = System.currentTimeMillis();
        
        double result = (stop - start) / count;

        if (log != null) {
            log.logln("test() elapsed " + (stop - start) +
                      " ms, count " + count +
                      ", " + nf.format(1000*result) + " us/count");
        }

        return result / 1000; // ms => s
    }

    /**
     * Convenience.
     */
    public final double measure(int iterations) {
        return measure(iterations, null);
    }

    static NumberFormat nf = NumberFormat.getInstance();
}

//eof
