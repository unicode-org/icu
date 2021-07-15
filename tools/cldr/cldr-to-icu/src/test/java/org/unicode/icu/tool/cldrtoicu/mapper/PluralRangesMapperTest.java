// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.icu.tool.cldrtoicu.mapper.PluralRangesMapperTest.PluralCount.FEW;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralRangesMapperTest.PluralCount.MANY;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralRangesMapperTest.PluralCount.ONE;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralRangesMapperTest.PluralCount.OTHER;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralRangesMapperTest.PluralCount.TWO;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralRangesMapperTest.PluralCount.ZERO;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.util.Arrays;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

@RunWith(JUnit4.class)
public class PluralRangesMapperTest {
    // Possible rule names (these are the value attributes).
    enum PluralCount {
        ZERO, ONE, TWO, FEW, MANY, OTHER;

        @Override public String toString() {
            return Ascii.toLowerCase(name());
        }
    }

    @Test
    public void testSimple() {
        Set<String> locales = ImmutableSet.of("en_GB", "en_NZ");
        CldrData cldrData = cldrData(
            pluralRange(locales, ZERO, ONE, ZERO),
            pluralRange(locales, ZERO, FEW, FEW),
            pluralRange(locales, ONE, TWO, OTHER),
            pluralRange(locales, ONE, MANY, MANY));

        IcuData icuData = PluralRangesMapper.process(cldrData);

        assertThat(icuData).hasName("pluralRanges");
        assertThat(icuData).hasFallback(false);

        assertThat(icuData).hasValuesFor("/locales/en_GB", "set00");
        assertThat(icuData).hasValuesFor("/locales/en_NZ", "set00");
        // Note that ordering is based on incoming CLDR path ordering, which is reordered by virtue
        // of being processed in "nested grouping" order.  This should probably be made to use DTD
        // order to make output more isolated once it's clear that this doesn't affect output.
        assertThat(icuData)
            .hasValuesFor("/rules/set00",
                RbValue.of("one", "many", "many"),
                RbValue.of("one", "two", "other"),
                RbValue.of("zero", "few", "few"),
                RbValue.of("zero", "one", "zero"));
    }

    @Test
    public void testMultipleSets() {
        Set<String> locales1 = ImmutableSet.of("en_GB");
        Set<String> locales2 = ImmutableSet.of("en_AU");
        CldrData cldrData = cldrData(
            pluralRange(locales1, ZERO, ONE, ZERO),
            pluralRange(locales1, ZERO, FEW, FEW),
            pluralRange(locales2, ONE, TWO, OTHER),
            pluralRange(locales2, ONE, MANY, MANY));

        IcuData icuData = PluralRangesMapper.process(cldrData);

        assertThat(icuData).hasName("pluralRanges");
        assertThat(icuData).hasFallback(false);

        assertThat(icuData).hasValuesFor("/locales/en_AU", "set00");
        assertThat(icuData)
            .hasValuesFor("/rules/set00",
                RbValue.of("one", "many", "many"),
                RbValue.of("one", "two", "other"));

        assertThat(icuData).hasValuesFor("/locales/en_GB", "set01");
        assertThat(icuData)
            .hasValuesFor("/rules/set01",
                RbValue.of("zero", "few", "few"),
                RbValue.of("zero", "one", "zero"));
    }

    private static CldrData cldrData(CldrValue... values) {
        return CldrDataSupplier.forValues(Arrays.asList(values));
    }

    private static CldrValue pluralRange(
        Set<String> locales, PluralCount start, PluralCount end, PluralCount result) {

        StringBuilder cldrPath = new StringBuilder("//supplementalData/plurals");
        appendAttribute(cldrPath.append("/pluralRanges"), "locales", Joiner.on(' ').join(locales));
        cldrPath.append("/pluralRange");
        appendAttribute(cldrPath, "start", start);
        appendAttribute(cldrPath, "end", end);
        appendAttribute(cldrPath, "result", result);
        return CldrValue.parseValue(cldrPath.toString(), "");
    }

    private static void appendAttribute(StringBuilder out, String k, Object v) {
        out.append(String.format("[@%s=\"%s\"]", k, v));
    }
}