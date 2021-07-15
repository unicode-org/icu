// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapperTest.PluralCount.FEW;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapperTest.PluralCount.MANY;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapperTest.PluralCount.ONE;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapperTest.PluralCount.OTHER;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapperTest.PluralCount.TWO;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapperTest.PluralCount.ZERO;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapperTest.PluralType.CARDINAL;
import static org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapperTest.PluralType.ORDINAL;
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

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

@RunWith(JUnit4.class)
public class PluralsMapperTest {
    enum PluralType {
        ORDINAL, CARDINAL;

        @Override public String toString() {
            return Ascii.toLowerCase(name());
        }
    }

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
            pluralRule(ORDINAL, locales, ZERO, "zero"),
            pluralRule(ORDINAL, locales, ONE, "one"),
            pluralRule(ORDINAL, locales, TWO, "two"),
            pluralRule(ORDINAL, locales, FEW, "few"),
            pluralRule(ORDINAL, locales, MANY, "many"),
            pluralRule(ORDINAL, locales, OTHER, "other"),

            pluralRule(CARDINAL, locales, ZERO, "!zero!"),
            pluralRule(CARDINAL, locales, ONE, "!one!"),
            pluralRule(CARDINAL, locales, TWO, "!two!"),
            pluralRule(CARDINAL, locales, FEW, "!few!"),
            pluralRule(CARDINAL, locales, MANY, "!many!"),
            pluralRule(CARDINAL, locales, OTHER, "!other!"));

        IcuData icuData = PluralsMapper.process(cldrData);

        assertThat(icuData).hasName("plurals");
        assertThat(icuData).hasFallback(false);

        // Cardinals are assigned first, regardless of the CLDR path order (this could change).
        assertThat(icuData).hasValuesFor("/locales/en_GB", "set0");
        assertThat(icuData).hasValuesFor("/locales/en_NZ", "set0");
        assertThat(icuData).hasValuesFor("/locales_ordinals/en_GB", "set1");
        assertThat(icuData).hasValuesFor("/locales_ordinals/en_NZ", "set1");

        assertThat(icuData).hasValuesFor("/rules/set0/zero", "!zero!");
        assertThat(icuData).hasValuesFor("/rules/set0/one", "!one!");
        assertThat(icuData).hasValuesFor("/rules/set0/two", "!two!");
        assertThat(icuData).hasValuesFor("/rules/set0/few", "!few!");
        assertThat(icuData).hasValuesFor("/rules/set0/many", "!many!");
        assertThat(icuData).hasValuesFor("/rules/set0/other", "!other!");

        assertThat(icuData).hasValuesFor("/rules/set1/zero", "zero");
        assertThat(icuData).hasValuesFor("/rules/set1/one", "one");
        assertThat(icuData).hasValuesFor("/rules/set1/two", "two");
        assertThat(icuData).hasValuesFor("/rules/set1/few", "few");
        assertThat(icuData).hasValuesFor("/rules/set1/many", "many");
        assertThat(icuData).hasValuesFor("/rules/set1/other", "other");
    }

    @Test
    public void testGroupDeduplication_subsets() {
        Set<String> locales1 = ImmutableSet.of("en_GB");
        Set<String> locales2 = ImmutableSet.of("en_NZ");
        CldrData cldrData = cldrData(
            // One group is a subset of the other, but this does not trigger deduplication.
            pluralRule(CARDINAL, locales1, ZERO, "zero"),
            pluralRule(CARDINAL, locales1, ONE, "one"),
            pluralRule(CARDINAL, locales1, TWO, "two"),

            pluralRule(CARDINAL, locales2, ZERO, "zero"),
            pluralRule(CARDINAL, locales2, ONE, "one"),
            pluralRule(CARDINAL, locales2, TWO, "two"),
            pluralRule(CARDINAL, locales2, FEW, "few"));

        IcuData icuData = PluralsMapper.process(cldrData);

        assertThat(icuData).hasValuesFor("/locales/en_GB", "set0");
        assertThat(icuData).hasValuesFor("/locales/en_NZ", "set1");

        assertThat(icuData).hasValuesFor("/rules/set0/zero", "zero");
        assertThat(icuData).hasValuesFor("/rules/set0/one", "one");
        assertThat(icuData).hasValuesFor("/rules/set0/two", "two");

        assertThat(icuData).hasValuesFor("/rules/set1/zero", "zero");
        assertThat(icuData).hasValuesFor("/rules/set1/one", "one");
        assertThat(icuData).hasValuesFor("/rules/set1/two", "two");
        assertThat(icuData).hasValuesFor("/rules/set1/few", "few");
    }

    @Test
    public void testGroupDeduplication_type() {
        Set<String> locales = ImmutableSet.of("en_GB");
        CldrData cldrData = cldrData(
            // Groups are the same, but assigned separately to different types.
            pluralRule(CARDINAL, locales, ZERO, "zero"),
            pluralRule(CARDINAL, locales, ONE, "one"),
            pluralRule(CARDINAL, locales, TWO, "two"),

            pluralRule(ORDINAL, locales, ZERO, "zero"),
            pluralRule(ORDINAL, locales, ONE, "one"),
            pluralRule(ORDINAL, locales, TWO, "two"));

        IcuData icuData = PluralsMapper.process(cldrData);

        // Group is deduplicated!
        assertThat(icuData).hasValuesFor("/locales/en_GB", "set0");
        assertThat(icuData).hasValuesFor("/locales_ordinals/en_GB", "set0");

        assertThat(icuData).hasValuesFor("/rules/set0/zero", "zero");
        assertThat(icuData).hasValuesFor("/rules/set0/one", "one");
        assertThat(icuData).hasValuesFor("/rules/set0/two", "two");
    }


    @Test
    public void testGroupDeduplication_locales() {
        Set<String> locales1 = ImmutableSet.of("en_GB");
        Set<String> locales2 = ImmutableSet.of("en_NZ");
        CldrData cldrData = cldrData(
            // Groups are the same, but assigned separately to different locales.
            pluralRule(CARDINAL, locales1, ZERO, "zero"),
            pluralRule(CARDINAL, locales1, ONE, "one"),
            pluralRule(CARDINAL, locales1, TWO, "two"),

            pluralRule(CARDINAL, locales2, ZERO, "zero"),
            pluralRule(CARDINAL, locales2, ONE, "one"),
            pluralRule(CARDINAL, locales2, TWO, "two"));

        IcuData icuData = PluralsMapper.process(cldrData);

        // Group is deduplicated!
        assertThat(icuData).hasValuesFor("/locales/en_GB", "set0");
        assertThat(icuData).hasValuesFor("/locales/en_NZ", "set0");

        assertThat(icuData).hasValuesFor("/rules/set0/zero", "zero");
        assertThat(icuData).hasValuesFor("/rules/set0/one", "one");
        assertThat(icuData).hasValuesFor("/rules/set0/two", "two");
    }

    private static CldrData cldrData(CldrValue... values) {
        return CldrDataSupplier.forValues(Arrays.asList(values));
    }

    private static CldrValue pluralRule(
        PluralType type, Set<String> locales, PluralCount count, String value) {

        StringBuilder cldrPath = new StringBuilder("//supplementalData");
        appendAttribute(cldrPath.append("/plurals"), "type", type);
        appendAttribute(cldrPath.append("/pluralRules"), "locales", Joiner.on(' ').join(locales));
        // We aren't testing sort index (#N) here, but still need to set it to something.
        appendAttribute(cldrPath.append("/pluralRule#0"), "count", count);
        return CldrValue.parseValue(cldrPath.toString(), value);
    }

    private static void appendAttribute(StringBuilder out, String k, Object v) {
        out.append(String.format("[@%s=\"%s\"]", k, v));
    }
}