// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.LocaleDisplayNames.DialectHandling;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.Region;
import com.ibm.icu.util.ULocale;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Concurrency regression tests for lazy initialization patterns: volatile DCL, ConcurrentHashMap
 * caches, and AtomicReferenceArray.
 */
@RunWith(JUnit4.class)
public class LazyInitConcurrencyTest extends ConcurrencyTest {

    @Test
    public void testPluralRulesForLocaleConcurrent() throws Exception {
        ULocale[] locales = {
            ULocale.ENGLISH,
            ULocale.FRENCH,
            ULocale.GERMAN,
            ULocale.JAPANESE,
            new ULocale("ar"),
            new ULocale("ru")
        };
        runConcurrent(
                "PluralRulesForLocale",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        ULocale loc = locales[(tid + i) % locales.length];
                        PluralRules rules = PluralRules.forLocale(loc);
                        assertNotNull("PluralRules should not be null for " + loc, rules);
                    }
                });
    }

    @Test
    public void testMeasureFormatConcurrent() throws Exception {
        runConcurrent(
                "MeasureFormat",
                tid -> {
                    MeasureFormat fmt =
                            MeasureFormat.getInstance(
                                    ULocale.ENGLISH, MeasureFormat.FormatWidth.WIDE);
                    MeasureUnit[] units = {
                        MeasureUnit.METER,
                        MeasureUnit.KILOMETER,
                        MeasureUnit.KILOGRAM,
                        MeasureUnit.CELSIUS
                    };
                    for (int i = 0; i < ITERATIONS; i++) {
                        MeasureUnit unit = units[(tid + i) % units.length];
                        String result = fmt.format(new Measure(42, unit));
                        assertNotNull("MeasureFormat result should not be null", result);
                        assertFalse("MeasureFormat result should not be empty", result.isEmpty());
                    }
                });
    }

    @Test
    public void testRegionGetInstanceConcurrent() throws Exception {
        String[] codes = {"US", "DE", "JP", "CN", "FR", "GB", "BR", "IN"};
        runConcurrent(
                "RegionGetInstance",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        String code = codes[(tid + i) % codes.length];
                        Region region = Region.getInstance(code);
                        assertNotNull("Region should not be null for " + code, region);
                        assertEquals("Region code should match", code, region.toString());
                    }
                });
    }

    @Test
    public void testSimpleDateFormatDefaultPatternConcurrent() throws Exception {
        runConcurrent(
                "SimpleDateFormatDefaultPattern",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        SimpleDateFormat sdf = new SimpleDateFormat();
                        assertNotNull("SimpleDateFormat should not be null", sdf);
                        String result = sdf.format(new Date());
                        assertNotNull("format result should not be null", result);
                    }
                });
    }

    @Test
    public void testLocaleDisplayNamesConcurrent() throws Exception {
        ULocale[] locales = {ULocale.ENGLISH, ULocale.FRENCH, ULocale.GERMAN, ULocale.JAPANESE};
        runConcurrent(
                "LocaleDisplayNames",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        ULocale loc = locales[(tid + i) % locales.length];
                        LocaleDisplayNames ldn =
                                LocaleDisplayNames.getInstance(loc, DialectHandling.STANDARD_NAMES);
                        assertNotNull("LocaleDisplayNames should not be null", ldn);
                        String name = ldn.localeDisplayName(ULocale.US);
                        assertNotNull("Display name should not be null", name);
                        assertFalse("Display name should not be empty", name.isEmpty());
                    }
                });
    }
}
