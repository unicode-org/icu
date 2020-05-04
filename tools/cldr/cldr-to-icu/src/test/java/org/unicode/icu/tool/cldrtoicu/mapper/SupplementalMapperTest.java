// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.cldr.api.CldrValue.parseValue;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.util.function.Predicate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.cldr.api.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.testing.FakeDataSupplier;
import org.unicode.icu.tool.cldrtoicu.testing.FakeResult;
import org.unicode.icu.tool.cldrtoicu.testing.FakeTransformer;

// Almost all the unit-testing for SupplementalMapper is done via AbstractPathValueMapper or
// RegexTransformer (and friends). Very little is left that's special to supplemental data.
@RunWith(JUnit4.class)
public class SupplementalMapperTest {
    private final FakeTransformer transformer = new FakeTransformer();
    private final FakeDataSupplier src = new FakeDataSupplier();

    @Test
    public void testSimple() {
        addExpectedMapping(
            supplementalData("likelySubtags/likelySubtag[@from=\"Foo\"][@to=\"Bar\"]"),
            simpleResult("/Foo", "Bar"));

        IcuData icuData = SupplementalMapper.process(src, transformer, "name", p -> true);

        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/Foo", "Bar");
    }

    @Test
    public void testFifoLabel() {
        // Example:
        // //supplementalData/currencyData/region[@iso3166="(%W)"]/currency[@iso4217="(%W)"]
        //     ; /CurrencyMap/$1/<FIFO>/id ; values=$2
        //
        // Note that the order mappings are added does not affect the output, since even though the
        // "FIFO" mechanism works on encounter-order, the CldrData is sorted before being
        // transformed (and in this case, is resolved on the currency code USD < USN < USS).
        addExpectedMapping(
            supplementalData("currencyData/region[@iso3166=\"US\"]/currency[@iso4217=\"USN\"]"),
            simpleResult("/CurrencyMap/US/<FIFO>/id", "USN"));
        addExpectedMapping(
            supplementalData("currencyData/region[@iso3166=\"US\"]/currency[@iso4217=\"USS\"]"),
            simpleResult("/CurrencyMap/US/<FIFO>/id", "USS"));
        addExpectedMapping(
            supplementalData("currencyData/region[@iso3166=\"US\"]/currency[@iso4217=\"USD\"]"),
            simpleResult("/CurrencyMap/US/<FIFO>/id", "USD"));

        IcuData icuData = SupplementalMapper.process(src, transformer, "name", p -> true);

        assertThat(icuData).getPaths().hasSize(3);
        assertThat(icuData).hasValuesFor("/CurrencyMap/US/<0000>/id", "USD");
        assertThat(icuData).hasValuesFor("/CurrencyMap/US/<0001>/id", "USN");
        assertThat(icuData).hasValuesFor("/CurrencyMap/US/<0002>/id", "USS");
    }

    @Test
    public void testPathFilter() {
        addExpectedMapping(
            supplementalData("likelySubtags/likelySubtag[@from=\"Foo\"][@to=\"Bar\"]"),
            simpleResult("/Foo", "Bar"));
        addExpectedMapping(
            supplementalData("currencyData/region[@iso3166=\"US\"]/currency[@iso4217=\"USN\"]"),
            simpleResult("/CurrencyMap/US/<FIFO>/id", "USN"));

        Predicate<CldrPath> filter =
            PathMatcher.of("//supplementalData/likelySubtags")::matchesPrefixOf;
        IcuData icuData = SupplementalMapper.process(src, transformer, "name", filter);

        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/Foo", "Bar");
    }

    private void addExpectedMapping(CldrValue value, Result... results) {
        src.addSupplementalData(value);
        transformer.addResults(value, results);
    }

    private static Result simpleResult(String path, String value) {
        return FakeResult.of(path, 1, false, value);
    }

    private static CldrValue supplementalData(String path) {
        return parseValue("//supplementalData/" + path, "");
    }
}