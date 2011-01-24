/*
 *******************************************************************************
 * Copyright (C) 2011, Google, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public final class Timer {
    public static final long SECONDS = 100000000;

    private long startTime;
    private long duration;
    private boolean timing = false;
    private int iterations;
    private long timingPeriod = 5*SECONDS;
    {
        start();
    }

    public Timer start() {
        startTime = System.nanoTime();
        timing = true;
        duration = Long.MIN_VALUE;
        return this;
    }

    public long getDuration() {
        if (timing) {
            duration = System.nanoTime() - startTime;
            timing = false;
        }
        return duration;
    }

    public long stop() {
        return getDuration();
    }

    public int getIterations() {
        return iterations;
    }

    public long getTimingPeriod() {
        return timingPeriod;
    }

    public Timer setTimingPeriod(long timingPeriod) {
        this.timingPeriod = timingPeriod;
        return this;
    }

    public String toString() {
        return nf.format(getDuration()) + "\tns";
    }
    public String toString(Timer other) {
        return toString(1L, other.getDuration());
    }
    public String toString(long iterations) {
        return nf.format(getDuration()/iterations) + "\tns";
    }

    public String toString(long iterations, long other) {
        return nf.format(getDuration()/iterations) + "\tns\t" + pf.format((double)getDuration()/other - 1D) + "";
    }

    private static DecimalFormat nf = (DecimalFormat) NumberFormat.getNumberInstance(ULocale.ENGLISH);
    private static DecimalFormat pf = (DecimalFormat) NumberFormat.getPercentInstance(ULocale.ENGLISH);
    static {
        pf.setMaximumFractionDigits(1);
        pf.setPositivePrefix("+");
    }

    public abstract static class Loop {
        public void init(Object... params) {}
        abstract public void time(int repeat);
    }

    public long timeIterations(Loop loop, Object... params) {
        loop.init(params);
        System.gc();
        start();
        loop.time(1);
        stop();
        iterations = 2;
        while (true) {
            System.gc();
            start();
            loop.time(iterations);
            stop();
            if (duration >= timingPeriod) {
                duration /= iterations;
                return duration;
            }
            iterations <<= 1;
        }
    }
}