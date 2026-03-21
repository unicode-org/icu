// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.TimeZoneFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Concurrency regression tests for TimeZone, ZoneMeta, and TimeZoneFormat. */
@RunWith(JUnit4.class)
public class TimeZoneConcurrencyTest extends ConcurrencyTest {

    @Test
    public void testTimeZoneDefaultTypeConcurrent() throws Exception {
        runConcurrent(
                "TimeZoneDefaultType",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        int type = TimeZone.getDefaultTimeZoneType();
                        assertTrue(
                                "TimeZone type should be ICU or JDK",
                                type == TimeZone.TIMEZONE_ICU || type == TimeZone.TIMEZONE_JDK);
                        TimeZone tz = TimeZone.getDefault();
                        assertNotNull("Default TimeZone should not be null", tz);
                    }
                });
    }

    /** Write-contention test: concurrent setDefault() calls interleaved with getDefault() reads. */
    @Test
    public void testTimeZoneSetDefaultConcurrent() throws Exception {
        TimeZone original = TimeZone.getDefault();
        String[] zoneIds = {"America/New_York", "Europe/Berlin", "Asia/Tokyo", "Pacific/Auckland"};
        try {
            runConcurrent(
                    "TimeZoneSetDefault",
                    tid -> {
                        for (int i = 0; i < ITERATIONS / 10; i++) {
                            if (tid % 4 == 0) {
                                TimeZone tz =
                                        TimeZone.getTimeZone(zoneIds[(tid + i) % zoneIds.length]);
                                TimeZone.setDefault(tz);
                            } else {
                                TimeZone tz = TimeZone.getDefault();
                                assertNotNull(
                                        "Default TimeZone should never be null during writes", tz);
                                String id = tz.getID();
                                assertNotNull("TimeZone ID should never be null", id);
                            }
                        }
                    });
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /** ZoneMeta SoftReference caches under contention. */
    @Test
    public void testZoneMetaConcurrent() throws Exception {
        runConcurrent(
                "ZoneMeta",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        Set<String> ids =
                                TimeZone.getAvailableIDs(
                                        TimeZone.SystemTimeZoneType.ANY, null, null);
                        assertFalse("Zone IDs should not be empty", ids == null || ids.isEmpty());
                    }
                });
    }

    /** TimeZoneFormat volatile DCL for timezone name cache. */
    @Test
    public void testTimeZoneFormatConcurrent() throws Exception {
        ULocale[] locales = {ULocale.US, ULocale.GERMANY, ULocale.JAPAN, ULocale.FRANCE};
        runConcurrent(
                "TimeZoneFormat",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        ULocale loc = locales[(tid + i) % locales.length];
                        TimeZoneFormat tzf = TimeZoneFormat.getInstance(loc);
                        assertNotNull("TimeZoneFormat should not be null", tzf);
                        String formatted =
                                tzf.format(
                                        TimeZoneFormat.Style.GENERIC_LONG,
                                        TimeZone.getTimeZone("America/New_York"),
                                        System.currentTimeMillis());
                        assertNotNull("TimeZone format result should not be null", formatted);
                    }
                });
    }
}
