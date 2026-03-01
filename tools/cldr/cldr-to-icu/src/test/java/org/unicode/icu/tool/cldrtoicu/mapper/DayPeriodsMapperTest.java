// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.icu.tool.cldrtoicu.mapper.DayPeriodsMapperTest.RuleType.AFTERNOON1;
import static org.unicode.icu.tool.cldrtoicu.mapper.DayPeriodsMapperTest.RuleType.EVENING1;
import static org.unicode.icu.tool.cldrtoicu.mapper.DayPeriodsMapperTest.RuleType.MIDNIGHT;
import static org.unicode.icu.tool.cldrtoicu.mapper.DayPeriodsMapperTest.RuleType.MORNING1;
import static org.unicode.icu.tool.cldrtoicu.mapper.DayPeriodsMapperTest.RuleType.NIGHT1;
import static org.unicode.icu.tool.cldrtoicu.mapper.DayPeriodsMapperTest.RuleType.NOON;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.util.Arrays;
import java.util.Map;
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@RunWith(JUnit4.class)
public class DayPeriodsMapperTest {
    // A subset of rule types for testing.
    enum RuleType {
        MORNING1, NOON, AFTERNOON1, EVENING1, NIGHT1, MIDNIGHT;

        @Override public String toString() {
            return Ascii.toLowerCase(name());
        }
    }

    // Possible rule names (these are the value attributes).
    enum RuleName {
        AT, BEFORE, FROM;

        @Override public String toString() {
            return Ascii.toLowerCase(name());
        }
    }

    @Test
    public void testSimple() {
        Set<String> locales = ImmutableSet.of("en_GB", "en_AU", "en_NZ");
        CldrData cldrData = cldrData(
            dayPeriodRule(locales, MORNING1, isBetween("04:00", "12:00")),
            dayPeriodRule(locales, NOON, isAt("12:00")),
            dayPeriodRule(locales, AFTERNOON1, isBetween("12:00", "18:00")),
            dayPeriodRule(locales, EVENING1, isBetween("18:00", "21:00")),
            dayPeriodRule(locales, NIGHT1, isBetween("21:00", "04:00")),
            dayPeriodRule(locales, MIDNIGHT, isAt("00:00")));

        IcuData icuData = DayPeriodsMapper.process(cldrData);

        assertThat(icuData).hasName("dayPeriods");
        assertThat(icuData).hasFallback(false);
        assertThat(icuData).hasValuesFor("/locales/en_AU", "set1");
        assertThat(icuData).hasValuesFor("/locales/en_GB", "set1");
        assertThat(icuData).hasValuesFor("/locales/en_NZ", "set1");

        assertThat(icuData).hasValuesFor("/rules/set1/morning1/from", "04:00");
        assertThat(icuData).hasValuesFor("/rules/set1/morning1/before", "12:00");
        assertThat(icuData).hasValuesFor("/rules/set1/noon/at", "12:00");
        assertThat(icuData).hasValuesFor("/rules/set1/afternoon1/from", "12:00");
        assertThat(icuData).hasValuesFor("/rules/set1/afternoon1/before", "18:00");
        assertThat(icuData).hasValuesFor("/rules/set1/evening1/from", "18:00");
        assertThat(icuData).hasValuesFor("/rules/set1/evening1/before", "21:00");
        assertThat(icuData).hasValuesFor("/rules/set1/night1/from", "21:00");
        assertThat(icuData).hasValuesFor("/rules/set1/night1/before", "04:00");
        assertThat(icuData).hasValuesFor("/rules/set1/midnight/at", "00:00");
    }

    @Test
    public void testMultipleRuleSets() {
        Set<String> locales1 = ImmutableSet.of("en_GB");
        Set<String> locales2 = ImmutableSet.of("en_AU", "en_NZ");
        CldrData cldrData = cldrData(
            dayPeriodRule(locales1, MORNING1, isBetween("04:00", "12:00")),
            dayPeriodRule(locales1, NOON, isAt("12:00")),
            dayPeriodRule(locales2, MORNING1, isBetween("06:00", "13:00")),
            dayPeriodRule(locales2, NOON, isAt("13:00")));

        IcuData icuData = DayPeriodsMapper.process(cldrData);

        // This reversal of the set ordering (as compared to the order of the input paths) is
        // because visitation requires nested path ordering, which is achieved by lexicographical
        // ordering of path strings ("en_AU" < "en_GB"). This is an implementation detail however
        // and might one day change. If this were switched to use DTD order, then it would be
        // stable (but also affect the ordering of paths in the released ICU data).
        assertThat(icuData).hasValuesFor("/locales/en_AU", "set1");
        assertThat(icuData).hasValuesFor("/locales/en_NZ", "set1");
        assertThat(icuData).hasValuesFor("/rules/set1/morning1/from", "06:00");
        assertThat(icuData).hasValuesFor("/rules/set1/morning1/before", "13:00");
        assertThat(icuData).hasValuesFor("/rules/set1/noon/at", "13:00");

        assertThat(icuData).hasValuesFor("/locales/en_GB", "set2");
        assertThat(icuData).hasValuesFor("/rules/set2/morning1/from", "04:00");
        assertThat(icuData).hasValuesFor("/rules/set2/morning1/before", "12:00");
        assertThat(icuData).hasValuesFor("/rules/set2/noon/at", "12:00");
    }

    @Test
    public void testRulesetLabels() {
        Set<String> locales = ImmutableSet.of("en_GB");
        // Note that there's an implicit assumption in the mapper that the ruleset label is the
        // same for all of the rules of any given locale (since it comes from the parent element).
        CldrData cldrData = cldrData(
            dayPeriodRule(locales, MORNING1, isBetween("04:00", "12:00"), "foo"),
            dayPeriodRule(locales, NOON, isAt("12:00"), "foo"));

        IcuData icuData = DayPeriodsMapper.process(cldrData);

        assertThat(icuData).hasValuesFor("/locales_foo/en_GB", "set1");
        assertThat(icuData).hasValuesFor("/rules/set1/morning1/from", "04:00");
        assertThat(icuData).hasValuesFor("/rules/set1/morning1/before", "12:00");
        assertThat(icuData).hasValuesFor("/rules/set1/noon/at", "12:00");
    }

    // Just demonstrating that the mapper does no data validation.
    @Test
    public void testNoDataValidation() {
        Set<String> locales = ImmutableSet.of("foo", "bar");
        CldrData cldrData = cldrData(
            dayPeriodRule(locales, MORNING1, isBetween("start", "end")),
            dayPeriodRule(locales, NOON, isAt("moment")));

        IcuData icuData = DayPeriodsMapper.process(cldrData);

        // This reversal of the set ordering (as compared to the order of the input paths) is
        // because visitation requires nested path ordering, which is achieved by lexicographical
        // ordering of path strings. This is an implementation detail however and might one day
        // change. If this were switched to use DTD order, then it would be stable (but also
        // affect the ordering of paths in the released ICU data).
        assertThat(icuData).hasValuesFor("/locales/foo", "set1");
        assertThat(icuData).hasValuesFor("/locales/bar", "set1");
        assertThat(icuData).hasValuesFor("/rules/set1/morning1/from", "start");
        assertThat(icuData).hasValuesFor("/rules/set1/morning1/before", "end");
        assertThat(icuData).hasValuesFor("/rules/set1/noon/at", "moment");
    }

    private static CldrData cldrData(CldrValue... values) {
        return CldrDataSupplier.forValues(Arrays.asList(values));
    }

    private static CldrValue dayPeriodRule(
        Set<String> locales, RuleType type, Map<RuleName, String> rules) {

        return dayPeriodRule(locales, type, rules, null);
    }

    private static CldrValue dayPeriodRule(
        Set<String> locales, RuleType type, Map<RuleName, String> rules, String label) {

        StringBuilder cldrPath = new StringBuilder("//supplementalData/dayPeriodRuleSet");
        if (label != null) {
            appendAttribute(cldrPath, "type", label);
        }
        appendAttribute(cldrPath.append("/dayPeriodRules"), "locales", Joiner.on(' ').join(locales));
        appendAttribute(cldrPath.append("/dayPeriodRule"), "type", type);
        rules.forEach((k, v) -> cldrPath.append(String.format("[@%s=\"%s\"]", k, v)));
        return CldrValue.parseValue(cldrPath.toString(), "");
    }

    private static Map<RuleName, String> isAt(String time) {
        return ImmutableMap.of(RuleName.AT, time);
    }

    private static Map<RuleName, String> isBetween(String from, String to) {
        return ImmutableMap.of(RuleName.FROM, from, RuleName.BEFORE, to);
    }

    private static void appendAttribute(StringBuilder out, String k, Object v) {
        out.append(String.format("[@%s=\"%s\"]", k, v));
    }
}