// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Optional.empty;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.RESOLVED;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;
import static org.unicode.cldr.api.CldrValue.parseValue;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbValue;
import org.unicode.icu.tool.cldrtoicu.testing.FakeDataSupplier;
import org.unicode.icu.tool.cldrtoicu.testing.FakeResult;
import org.unicode.icu.tool.cldrtoicu.testing.FakeTransformer;

// Almost all the unit-testing for LocaleMapper is done via AbstractPathValueMapper or
// RegexTransformer (and friends). Very little is left that's special to locale data.
@RunWith(JUnit4.class)
public class LocaleMapperTest {
    private final FakeTransformer transformer = new FakeTransformer();
    private final FakeDataSupplier src = new FakeDataSupplier();

    @Test
    public void testSimple() {
        //ldml/units/durationUnit[@type="(%A)"]/durationUnitPattern ; /durationUnits/$1
        addMapping("xx",
            ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "Bar"),
            simpleResult("/durationUnits/foo", "Bar"));

        IcuData icuData = process("xx");
        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/durationUnits/foo", "Bar");
    }

    @Test
    public void testCorrectLocaleIsUsed() {
        src.addLocaleData(
            "xx", ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "XX"));
        addMapping(
            "yy", ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "YY"),
            simpleResult("/durationUnits/foo", "YY"));
        src.addLocaleData(
            "zz", ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "ZZ"));

        IcuData icuData = process("yy");
        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/durationUnits/foo", "YY");
    }

    @Test
    public void testInheritedValuesNotIncludedByDefault() {
        //ldml/units/durationUnit[@type="(%A)"]/durationUnitPattern ; /durationUnits/$1
        addMapping("xx",
            ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "Bar"),
            simpleResult("/durationUnits/foo", "Bar"));
        //ldml/localeDisplayNames/keys/key[@type="(%A)"] ; /Keys/$1
        addRootMapping(
            ldml("localeDisplayNames/keys/key[@type=\"sometype\"]", "Value"),
            simpleResult("/Keys/sometype", "Value"));

        IcuData icuData = process("xx");

        // The 2nd mapping is not used because it does not appear in the unresolved CldrData.
        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/durationUnits/foo", "Bar");
    }

    @Test
    public void testInheritedValuesIncludedWhenSameResourceBundle() {
        //ldml/numbers/currencies/currency[@type="(%W)"]/symbol ; /Currencies/$1 ; fallback=$1
        //ldml/numbers/currencies/currency[@type="(%W)"]/displayName ; /Currencies/$1 ; fallback=$1
        addMapping("xx",
            ldml("numbers/currencies/currency[@type=\"USD\"]/symbol", "US$"),
            simpleResult("/Currencies/USD", 1, "US$"));
        // This is included because the resource bundle path is the same as above. Note that we
        // have to use the index to distinguish results here (this corresponds to the line number
        // or the real when the real regex based config is used and determines result ordering).
        addRootMapping(
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "US Dollar"),
            simpleResult("/Currencies/USD", 2, "US Dollar"));

        IcuData icuData = process("xx");

        // Now the inherited mapping is used because the path appeared for the unresolved CldrData.
        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/Currencies/USD", singletonValues("US$", "US Dollar"));
    }

    @Test
    public void testChildPathsNotIncludedByDefault() {
        // Tests that in the case that one path is the child of another path (rare) the existence
        // of the parent path will not trigger the child path to be included.
        //
        //ldml/.../dateTimeFormats/availableFormats/dateFormatItem[@id="(%A)"]
        //   ; /calendar/$1/availableFormats/$2
        //ldml/.../dateTimeFormats/availableFormats/dateFormatItem[@id="(%A)"][@count="(%A)"]
        //   ; /calendar/$1/availableFormats/$2/$3
        addMapping("xx",
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"]", "Foo"),
            simpleResult("/calendar/foo/availableFormats/bar", "Foo"));
        addRootMapping(
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"][@count=\"one\"]", "Bar"),
            simpleResult("/calendar/foo/availableFormats/bar/one", "Bar"));

        IcuData icuData = process("xx");

        // Now the inherited mapping is used because the path appeared for the unresolved CldrData.
        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/calendar/foo/availableFormats/bar", "Foo");
    }

    @Test
    public void testParentPathsNotIncludedByDefault() {
        // Same as above but swapping inherited vs explicit mappings.
        addRootMapping(
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"]", "Foo"),
            simpleResult("/calendar/foo/availableFormats/bar", "Foo"));
        addMapping("xx",
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"][@count=\"one\"]", "Bar"),
            simpleResult("/calendar/foo/availableFormats/bar/one", "Bar"));

        IcuData icuData = process("xx");

        // Now the inherited mapping is used because the path appeared for the unresolved CldrData.
        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/calendar/foo/availableFormats/bar/one", "Bar");
    }

    // This is done so that when these paths are written into the ICU data file (and the hidden
    // labels are removed) you get the "two layer" array:
    //
    // {
    //   "Parent",
    //   { "Child-1", "Child-2" }
    // }
    //
    // This needs to happen even when only one of the child elements is given explicitly.
    @Test
    public void testHiddenLabelsIncludeParentPaths() {
        // Testing that the existence of a child element using a hidden label *does* trigger the
        // parent element to be included.
        addRootMapping(
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"]", "Parent"),
            simpleResult("/calendar/foo/availableFormats/bar", "Parent"));
        addRootMapping(
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"][@count=\"one\"]", "Child-1"),
            simpleResult("/calendar/foo/availableFormats/bar/<HIDDEN>", 1, "Child-1"));

        // This is the only explicit mapping and it triggers the sibling _and_ the parent.
        addMapping("xx",
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"][@count=\"many\"]", "Child-2"),
            simpleResult("/calendar/foo/availableFormats/bar/<HIDDEN>", 2, "Child-2"));

        IcuData icuData = process("xx");

        assertThat(icuData).getPaths().hasSize(2);
        assertThat(icuData).hasValuesFor("/calendar/foo/availableFormats/bar", "Parent");
        assertThat(icuData)
            .hasValuesFor("/calendar/foo/availableFormats/bar/<HIDDEN>",
                singletonValues("Child-1", "Child-2"));
    }

    // This is strange behaviour given the test above, since it means that it's impossible to
    // use hidden labels to create a situation where the output ICU data looks like:
    //
    // {
    //   "Parent",
    //   { "Child-1", "Child-2" },
    //   "Other Parent"
    // }
    //
    // if the child elements can be inherited; since if they are not present, you just get:
    //
    // {
    //   "Parent",
    //   "Other Parent"
    // }
    //
    // Which moves the index of the following elements up by one and makes it impossible to
    // define a stable length or index mapping for the array.
    //
    // However this is relied upon in the /Currencies/XXX case where a child array exists, but
    // is optional if none of it's values are explicitly present. For example in en_150.txt:
    //
    // Currencies{
    //     EUR{
    //         "€",
    //         "Euro",
    //         {
    //             "¤#,##0.00",
    //             ".",
    //             ",",
    //         }
    //     }
    // }
    //
    // In most cases the formatting/grouping information is omitted if it can all be inherited.
    //
    // This only really works because the child array is the last element in the parent array, so
    // not having it present doesn't affect any later elements.
    //
    // The "group" instruction in the transformation configuration files is a different way to
    // allow grouping of sub-arrays which does not have this behaviour.
    @Test
    public void testHiddenLabelsAreNotIncludedAutomatically() {
        // As above, but now only the parent path is included explicitly.
        addMapping("xx",
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"]", "Parent"),
            simpleResult("/calendar/foo/availableFormats/bar", "Parent"));
        addRootMapping(
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"][@count=\"one\"]", "Child-1"),
            simpleResult("/calendar/foo/availableFormats/bar/<HIDDEN>", 1, "Child-1"));

        // This is the only explicit mapping and it triggers the sibling _and_ the parent.
        addRootMapping(
            ldml("dates/calendars/calendar[@type=\"foo\"]/dateTimeFormats"
                + "/availableFormats/dateFormatItem[@id=\"bar\"][@count=\"many\"]", "Child-2"),
            simpleResult("/calendar/foo/availableFormats/bar/<HIDDEN>", 2, "Child-2"));

        IcuData icuData = process("xx");

        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/calendar/foo/availableFormats/bar", "Parent");
    }

    @Test
    public void testDefaultCalendar() {
        IcuData icuData = process("xx", Optional.of("pastafarian"));
        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/calendar/default", "pastafarian");
    }

    @Test
    public void testDateTimeHack() {
        //calendar/$1/DateTimePatterns
        addMapping("xx",
            format("time", "full", "one"),
            simpleResult("/calendar/foo/DateTimePatterns", 1, "one"));
        addMapping("xx",
            format("time", "long", "two"),
            simpleResult("/calendar/foo/DateTimePatterns", 2, "two"));
        addMapping("xx",
            format("time", "medium", "three"),
            simpleResult("/calendar/foo/DateTimePatterns", 3, "three"));
        addMapping("xx",
            format("time", "short", "four"),
            simpleResult("/calendar/foo/DateTimePatterns", 4, "four"));
        addMapping("xx",
            format("date", "full", "five"),
            simpleResult("/calendar/foo/DateTimePatterns", 5, "five"));
        addMapping("xx",
            format("date", "long", "six"),
            simpleResult("/calendar/foo/DateTimePatterns", 6, "six"));
        addMapping("xx",
            format("date", "medium", "seven"),
            simpleResult("/calendar/foo/DateTimePatterns", 7, "seven"));
        addMapping("xx",
            format("date", "short", "eight"),
            simpleResult("/calendar/foo/DateTimePatterns", 8, "eight"));
        addMapping("xx",
            format("dateTime", "full", "nine"),
            simpleResult("/calendar/foo/DateTimePatterns", 9, "nine"));
        addMapping("xx",
            format("dateTime", "long", "ten"),
            simpleResult("/calendar/foo/DateTimePatterns", 10, "ten"));
        addMapping("xx",
            format("dateTime", "medium", "eleven"),
            simpleResult("/calendar/foo/DateTimePatterns", 11, "eleven"));
        addMapping("xx",
            format("dateTime", "short", "twelve"),
            simpleResult("/calendar/foo/DateTimePatterns", 12, "twelve"));

        IcuData icuData = process("xx");

        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/calendar/foo/DateTimePatterns",
            singletonValues(
                "one", "two", "three", "four",
                "five", "six", "seven", "eight",
                "eleven",  // <-- legacy reasons, don't ask!
                "nine", "ten", "eleven", "twelve"));
    }

    @Test
    public void testDateTimeHack_wrongNumberofElements() {
        // One missing pattern from the start.
        addMapping("xx",
            format("time", "long", "two"),
            simpleResult("/calendar/foo/DateTimePatterns", 2, "two"));
        addMapping("xx",
            format("time", "medium", "three"),
            simpleResult("/calendar/foo/DateTimePatterns", 3, "three"));
        addMapping("xx",
            format("time", "short", "four"),
            simpleResult("/calendar/foo/DateTimePatterns", 4, "four"));
        addMapping("xx",
            format("date", "full", "five"),
            simpleResult("/calendar/foo/DateTimePatterns", 5, "five"));
        addMapping("xx",
            format("date", "long", "six"),
            simpleResult("/calendar/foo/DateTimePatterns", 6, "six"));
        addMapping("xx",
            format("date", "medium", "seven"),
            simpleResult("/calendar/foo/DateTimePatterns", 7, "seven"));
        addMapping("xx",
            format("date", "short", "eight"),
            simpleResult("/calendar/foo/DateTimePatterns", 8, "eight"));
        addMapping("xx",
            format("dateTime", "full", "nine"),
            simpleResult("/calendar/foo/DateTimePatterns", 9, "nine"));
        addMapping("xx",
            format("dateTime", "long", "ten"),
            simpleResult("/calendar/foo/DateTimePatterns", 10, "ten"));
        addMapping("xx",
            format("dateTime", "medium", "eleven"),
            simpleResult("/calendar/foo/DateTimePatterns", 11, "eleven"));
        addMapping("xx",
            format("dateTime", "short", "twelve"),
            simpleResult("/calendar/foo/DateTimePatterns", 12, "twelve"));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> process("xx"));
        assertThat(e).hasMessageThat().contains("unexpected");
        assertThat(e).hasMessageThat().contains("/calendar/foo/DateTimePatterns");
    }

    private static CldrValue format(String type,String length, String pattern) {
        return ldml(String.format(
            "dates/calendars/calendar[@type=\"foo\"]"
                + "/%1$sFormats"
                + "/%1$sFormatLength[@type=\"%2$s\"]"
                + "/%1$sFormat[@type=\"standard\"]/pattern[@type=\"%3$s\"]",
            type, length, pattern));
    }

    // ---- Helper methods ----

    IcuData process(String localeId) {
        return process(localeId, empty());
    }

    IcuData process(String localeId, Optional<String> defCalendar) {
        IcuData icuData = new IcuData(localeId, true);
        LocaleMapper.process(
            icuData,
            src.getDataForLocale(localeId, UNRESOLVED),
            src.getDataForLocale(localeId, RESOLVED),
            empty(),
            transformer,
            defCalendar);
        return icuData;
    }

    private void addMapping(String locale, CldrValue value, Result... results) {
        src.addLocaleData(locale, value);
        transformer.addResults(value, results);
    }

    private void addRootMapping(CldrValue value, Result... results) {
        src.addLocaleData("root", value);
        transformer.addResults(value, results);
    }

    private static Result simpleResult(String path, String value) {
        return FakeResult.of(path, 1, false, value);
    }

    private static Result simpleResult(String path, int index, String value) {
        return FakeResult.of(path, index, false, value);
    }

    private static CldrValue ldml(String path) {
        return ldml(path, "");
    }

    private static CldrValue ldml(String path, String value) {
        return parseValue("//ldml/" + path, value);
    }

    private static RbValue[] singletonValues(String... values) {
        return Arrays.stream(values).map(RbValue::of).toArray(RbValue[]::new);
    }
}