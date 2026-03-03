// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2026, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.VersionInfo;

/**
 * Concurrency hammer tests for modernization effort (ULocale, Currency, TimeZone, MeasureUnit, ResourceCache).
 */
@RunWith(JUnit4.class)
public class ConcurrencyTest extends CoreTestFmwk {

    private static final int NUM_THREADS = 50;
    private static final int ITERATIONS = 500; // Balanced stress test (25k total ops)

    @Test
    public void testResourceCacheConcurrency() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        // Get many locales to ensure we blast past the 32-item SIMPLE_LENGTH limit
        // and trigger the Trie transition.
        final ULocale[] allLocales = ULocale.getAvailableLocales();

        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            for (int i = 0; i < NUM_THREADS; i++) {
                exec.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < ITERATIONS; j++) {
                            for (ULocale loc : allLocales) {
                                // ICUResourceBundle.getBundleInstance hits the ResourceCache
                                UResourceBundle rb = UResourceBundle.getBundleInstance(
                                        ICUData.ICU_BASE_NAME,
                                        loc);
                                if (rb == null) {
                                    throw new AssertionError("ResourceCache returned null bundle for locale: " + loc);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        exceptions.add(t);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            if (!endLatch.await(120, TimeUnit.SECONDS)) {
                errln("ResourceCache concurrency test timed out");
            }
        } finally {
            exec.shutdownNow();
        }

        for (Throwable t : exceptions) {
            errln("ResourceCache thread failed: " + t.getMessage());
        }
    }

    @Test
    public void testULocaleConcurrency() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            for (int i = 0; i < NUM_THREADS; i++) {
                exec.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < ITERATIONS; j++) {
                            ULocale.canonicalize("en_US_POSIX");
                            ULocale.canonicalize("ar_EG");
                            ULocale.canonicalize("zh_Hans_CN");
                        }
                    } catch (Throwable t) {
                        exceptions.add(t);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            if (!endLatch.await(60, TimeUnit.SECONDS)) {
                errln("ULocale concurrency test timed out");
            }
        } finally {
            exec.shutdownNow();
        }

        for (Throwable t : exceptions) {
            errln("ULocale thread failed: " + t.getMessage());
        }
    }

    @Test
    public void testCurrencyConcurrency() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            for (int i = 0; i < NUM_THREADS; i++) {
                exec.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < ITERATIONS; j++) {
                            boolean avail = Currency.isAvailable("USD", null, null);
                            String[] codes = Currency.getKeywordValuesForLocale("currency", ULocale.US, false);
                            if (!avail || codes.length == 0) {
                                throw new AssertionError("Currency data missing in thread");
                            }
                        }
                    } catch (Throwable t) {
                        exceptions.add(t);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            if (!endLatch.await(60, TimeUnit.SECONDS)) {
                errln("Currency concurrency test timed out");
            }
        } finally {
            exec.shutdownNow();
        }

        for (Throwable t : exceptions) {
            errln("Currency thread failed: " + t.getMessage());
        }
    }

    @Test
    public void testTimeZoneConcurrency() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            for (int i = 0; i < NUM_THREADS; i++) {
                exec.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < ITERATIONS; j++) {
                            TimeZone tz = TimeZone.getDefault();
                            if (tz == null || tz.getID() == null) {
                                throw new AssertionError("TimeZone.getDefault() returned null or invalid ID");
                            }
                        }
                    } catch (Throwable t) {
                        exceptions.add(t);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            if (!endLatch.await(60, TimeUnit.SECONDS)) {
                errln("TimeZone concurrency test timed out");
            }
        } finally {
            exec.shutdownNow();
        }

        for (Throwable t : exceptions) {
            errln("TimeZone thread failed: " + t.getMessage());
        }
    }

    @Test
    public void testMeasureUnitConcurrency() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        // Track every instance seen for 'meter' across all threads.
        // There should only ever be one.
        final Set<MeasureUnit> meterIdentities =
                Collections.newSetFromMap(new ConcurrentHashMap<MeasureUnit, Boolean>());

        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            for (int i = 0; i < NUM_THREADS; i++) {
                exec.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < ITERATIONS; j++) {
                            Set<MeasureUnit> units = MeasureUnit.getAvailable("length");
                            for (MeasureUnit u : units) {
                                if ("meter".equals(u.getSubtype())) {
                                    meterIdentities.add(u);
                                    break;
                                }
                            }
                        }
                    } catch (Throwable t) {
                        exceptions.add(t);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            if (!endLatch.await(60, TimeUnit.SECONDS)) {
                errln("MeasureUnit concurrency test timed out");
            }

            for (Throwable t : exceptions) {
                errln("MeasureUnit thread failed: " + t.getMessage());
            }

            // Critical Assertion: Verify Object Identity.
            // If multiple threads created their own 'meter', size will be > 1.
            assertEquals("Duplicate MeasureUnit instances created for 'meter'", 1, meterIdentities.size());

            // Verify interning: all subtypes must share the exact same type string instance
            MeasureUnit meter = meterIdentities.iterator().next();
            Set<MeasureUnit> lengthUnits = MeasureUnit.getAvailable("length");
            for (MeasureUnit u : lengthUnits) {
                if (u.getType() != meter.getType()) {
                    errln("MeasureUnit type string interning failed for unit: " + u.getSubtype());
                }
            }
        } finally {
            exec.shutdownNow();
        }
    }
}
