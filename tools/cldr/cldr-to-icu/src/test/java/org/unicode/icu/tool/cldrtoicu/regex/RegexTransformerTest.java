// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.regex;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;
import static org.unicode.icu.tool.cldrtoicu.testing.ResultSubjectFactory.assertThat;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.collect.ImmutableList;

/**
 * Tests for the regex transformer class. Note that in most cases, the rules used here are taken
 * directly from one of the config files, simply because it avoids having to invent valid paths
 * for testing (and we still need "real" CLDR paths since the path parsing verifies attributes
 * against the DTD metadata). Basing tests on real rules illustrates that all of these tests are
 * asserting about relied-upon behaviour, however there is nothing inherently special about these
 * paths.
 */
@RunWith(JUnit4.class)
public class RegexTransformerTest {
    @Test
    public void testSingleResults_singleCapture() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "%W=[\\w\\-]++",
            "//ldml/numbers/defaultNumberingSystem[@alt=\"(%A)\"] ; /NumberElements/default_$1",
            "//ldml/numbers/defaultNumberingSystem                ; /NumberElements/default",
            "//ldml/numbers/otherNumberingSystems/(%W)            ; /NumberElements/$1");

        CldrValue defaultNumberingSystem =
            CldrValue.parseValue("//ldml/numbers/defaultNumberingSystem", "foobar");
        assertSingleResult(
            transformer.transform(defaultNumberingSystem), "NumberElements/default", "foobar");

        CldrValue altNumberingSystem =
            CldrValue.parseValue("//ldml/numbers/defaultNumberingSystem[@alt=\"foo\"]", "bar");
        assertSingleResult(
            transformer.transform(altNumberingSystem), "NumberElements/default_foo", "bar");

        CldrValue otherNumberingSystems =
            CldrValue.parseValue("//ldml/numbers/otherNumberingSystems/finance", "foo bar");
        assertSingleResult(
            transformer.transform(otherNumberingSystems), "NumberElements/finance", "foo bar");
    }

    @Test
    public void testSingleResults_multipleCapture() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "//ldml/characters"
                + "/parseLenients[@scope=\"(%A)\"][@level=\"(%A)\"]"
                + "/parseLenient[@sample=\"%A\"]"
                + " ; /parse/$1/$2");

        CldrValue lenient = CldrValue.parseValue(
            "//ldml/characters"
                + "/parseLenients[@scope=\"general\"][@level=\"lenient\"]"
                + "/parseLenient[@sample=\"ignored\"]",
            "foo");
        assertSingleResult(
            transformer.transform(lenient), "/parse/general/lenient", "foo");

        CldrValue stricter = CldrValue.parseValue(
            "//ldml/characters"
                + "/parseLenients[@scope=\"number\"][@level=\"stricter\"]"
                + "/parseLenient[@sample=\"ignored\"]",
            "bar");
        assertSingleResult(
            transformer.transform(stricter), "/parse/number/stricter", "bar");
    }

    @Test
    public void testMultipleResults() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "%W=[\\s\\w\\-/]++",
            "//supplementalData/numberingSystems"
                + "/numberingSystem[@type=\"numeric\"][@id=\"(%W)\"][@digits=\"(%A)\"]",
            " ; /numberingSystems/$1/algorithmic:int ; values=0",
            " ; /numberingSystems/$1/desc ; values=$2",
            " ; /numberingSystems/$1/radix:int ; values=10");

        CldrValue value = CldrValue.parseValue(
            "//supplementalData/numberingSystems"
                + "/numberingSystem[@type=\"numeric\"][@id=\"foo\"][@digits=\"bar\"]",
            "");
        ImmutableList<Result> results = transformer.transform(value);
        assertThat(results).hasSize(3);
        assertThat(results.get(0)).hasKey("/numberingSystems/foo/algorithmic:int");
        assertThat(results.get(0)).hasValues("0");
        assertThat(results.get(0)).isGrouped(false);

        assertThat(results.get(1)).hasKey("/numberingSystems/foo/desc");
        assertThat(results.get(1)).hasValues("bar");
        assertThat(results.get(1)).isGrouped(false);

        assertThat(results.get(2)).hasKey("/numberingSystems/foo/radix:int");
        assertThat(results.get(2)).hasValues("10");
        assertThat(results.get(2)).isGrouped(false);
    }

    @Test
    public void testImplicitArgumentSplitting() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "%W=[\\s\\w\\-/]++",
            "//supplementalData/gender/personList[@type=\"(%W)\"][@locales=\"(%W)\"]"
                + " ; /genderList/$2 ; values=$1",
            "//supplementalData/windowsZones/mapTimezones"
                + "/mapZone[@type=\"(%A)\"][@other=\"(%A)\"][@territory=\"(%W)\"]"
                + " ; /mapTimezones/\"$2\"/$3 ; values=\"$1\"");

        // Implicit splitting is based on the first unquoted placeholder in the output path ($2 in
        // this case) and not the first captured group of the input path.
        CldrValue personList = CldrValue.parseValue(
            "//supplementalData/gender/personList[@type=\"neutral\"][@locales=\"xx yy zz\"]", "");
        ImmutableList<Result> results = transformer.transform(personList);
        assertThat(results).hasSize(3);
        assertThat(results.get(0)).hasKey("/genderList/xx");
        assertThat(results.get(0)).hasValues("neutral");
        assertThat(results.get(1)).hasKey("/genderList/yy");
        assertThat(results.get(1)).hasValues("neutral");
        assertThat(results.get(2)).hasKey("/genderList/zz");
        assertThat(results.get(2)).hasValues("neutral");

        // Quoting prevents the first captured argument with spaces from triggering multiple
        // results (it will trigger on the first un-quoted argument in the output path). This
        // quoting must appear in the output however since spaces are "structural" in paths in
        // ICU data files.
        CldrValue mapZone = CldrValue.parseValue(
            "//supplementalData/windowsZones/mapTimezones/mapZone"
                + "[@type=\"foo\"]"
                + "[@other=\"not split\"]"
                + "[@territory=\"XX YY ZZ\"]",
            "");
        results = transformer.transform(mapZone);
        assertThat(results).hasSize(3);
        assertThat(results.get(0)).hasKey("/mapTimezones/\"not split\"/XX");
        assertThat(results.get(2)).hasValues("foo");
        assertThat(results.get(1)).hasKey("/mapTimezones/\"not split\"/YY");
        assertThat(results.get(2)).hasValues("foo");
        assertThat(results.get(2)).hasKey("/mapTimezones/\"not split\"/ZZ");
        assertThat(results.get(2)).hasValues("foo");
    }

    @Test
    public void testValueSplitting() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "%W=[\\s\\w\\-/]++",
            "//supplementalData/parentLocales/parentLocale[@parent=\"(%A)\"][@locales=\"(%A)\"]"
                + " ; /parentLocales/$1 ; values=$2",
            "//supplementalData/windowsZones/mapTimezones"
                + "/mapZone[@type=\"(%A)\"][@other=\"(%A)\"][@territory=\"(%W)\"]"
                + " ; /mapTimezones/\"$2\"/$3 ; values=\"$1\"");

        // Because the value is expressed via an explicit values instruction, it is split by space.
        CldrValue parentLocale = CldrValue.parseValue(
            "//supplementalData/parentLocales"
                + "/parentLocale[@parent=\"foo\"][@locales=\"value is split\"]",
            "");
        assertSingleResult(transformer.transform(parentLocale),
            "/parentLocales/foo", "value", "is", "split");

        // However if a placeholder is quoted in the value instruction, it is not split.
        CldrValue mapZone = CldrValue.parseValue(
            "//supplementalData/windowsZones/mapTimezones/mapZone"
                + "[@type=\"value is not split\"]"
                + "[@other=\"foo\"]"
                + "[@territory=\"XX\"]",
            "");
        assertSingleResult(transformer.transform(mapZone),
            "/mapTimezones/\"foo\"/XX", "value is not split");
    }

    @Test
    public void testResultFunctionCalling() {
        List<String> configLines = asList(
            "%A=[^\"']++",
            "%W=[\\s\\w\\-/]++",
            "//supplementalData/numberingSystems"
                + "/numberingSystem[@type=\"(%W)\"][@id=\"(%W)\"][@rules=\"(%A)\"]",
            " ; /numberingSystems/foo ; values=&swap( $1 , $2 ) $3",
            " ; /numberingSystems/bar ; values=\"&swap( $1, quux )\"",
            " ; /numberingSystems/baz ; values=\"&swap( $1-$2, $3{value} )\"");

        CldrValue numberingSystem = CldrValue.parseValue(
            "//supplementalData/numberingSystems"
                + "/numberingSystem[@type=\"foo\"][@id=\"bar\"][@rules=\"baz\"]",
            "-VALUE");

        // Note that joining with a space is rather a trivial function, but it does illustrate that
        // a function's output is still subject to value splitting unless quoted. In fact a common
        // function (&ymd) is used to split year/month/day strings using spaces exactly so they are
        // treated as separate values.
        // Note also that the spaces around the arguments to the function are ignored however.
        NamedFunction swapFn =
            NamedFunction.create("swap", 2, args -> args.get(1) + " " + args.get(0));
        PathValueTransformer transformer = RegexTransformer.fromConfigLines(configLines, swapFn);
        ImmutableList<Result> results = transformer.transform(numberingSystem);

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).hasValues("bar", "foo", "baz");
        assertThat(results.get(1)).hasValues("quux foo");
        assertThat(results.get(2)).hasValues("baz-VALUE foo-bar");
    }

    @Test
    public void testResultFunctionCalling_edgeCases() {
        List<String> configLines = asList(
            "%A=[^\"']++",
            "%W=[\\s\\w\\-/]++",
            "//supplementalData/numberingSystems"
                + "/numberingSystem[@type=\"(%W)\"][@id=\"(%W)\"][@rules=\"(%A)\"]",
            " ; /numberingSystems/foo ; values=\"&join( {value} , $1 $2 $3, {value} )\"");

        // This illustrates a fundamental problem with the way that quoting and splitting is
        // defined in this config language. Splitting is always down after value substitution,
        // which is just done as a single pass. This, if a value has a double-quote in it can
        // upset the quoting behaviour in odd ways. Here it prevents the outermost quoting from
        // working and results in multiple values where there should be one.
        //
        // To fix this, the implicit splitting should be replaced by a "split()" function and the
        // rules should be parsed into something approximating a proper expression AST.
        CldrValue badValue = CldrValue.parseValue(
            "//supplementalData/numberingSystems"
                + "/numberingSystem[@type=\"foo\"][@id=\"bar\"][@rules=\"baz\"]",
            "<< \" >>");

        NamedFunction joinFn =
            NamedFunction.create("join", 3, args -> args.get(0) + args.get(1) + args.get(2));
        PathValueTransformer transformer = RegexTransformer.fromConfigLines(configLines, joinFn);
        ImmutableList<Result> results = transformer.transform(badValue);
        // If outer quoting worked, this would be a single value, not five.
        assertSingleResult(results, "/numberingSystems/foo", "<< ", ">>foo", "bar", "baz<<", " >>");
    }

    @Test
    public void testDynamicVars() {
        PathValueTransformer transformer = transformer(
            "%W=[\\w\\-]++",
            "%D=//ldml/numbers/defaultNumberingSystem",
            "//ldml/numbers/currencyFormats[@numberSystem=\"%D\"]/currencySpacing/(%W)/(%W)",
            " ; /currencySpacing/$1/$2");
        CldrValue cldrValue = CldrValue.parseValue(
            "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]"
                + "/currencySpacing/beforeCurrency/currencyMatch",
            "format");
        // The path we expect to be resolved by the dynamic variable function.
        CldrPath expectedPath =
            CldrPath.parseDistinguishingPath("//ldml/numbers/defaultNumberingSystem");
        ImmutableList<Result> format = transformer.transform(cldrValue, p -> {
            assertThat(p).isEqualTo(expectedPath);
            return "latn";
        });
        assertSingleResult(format, "/currencySpacing/beforeCurrency/currencyMatch", "format");
    }

    @Test
    public void testFallbacks_simple() {
        PathValueTransformer transformer = transformer(
            "%W=[\\w\\-/]++",
            "//ldml/numbers/currencies/currency[@type=\"(%W)\"]/symbol"
                + " ; /Currencies/$1 ; fallback=$1",
            "//ldml/numbers/currencies/currency[@type=\"(%W)\"]/displayName"
                + " ; /Currencies/$1 ; fallback=$1");

        ImmutableList<Result> symbol = transformer.transform(
            CldrValue.parseValue(
                "//ldml/numbers/currencies/currency[@type=\"Foo\"]/symbol", "symbol"));
        assertSingleResult(symbol, "Currencies/Foo", "symbol");
        ImmutableList<Result> name = transformer.transform(
            CldrValue.parseValue(
                "//ldml/numbers/currencies/currency[@type=\"Foo\"]/displayName", "name"));
        assertSingleResult(name, "Currencies/Foo", "name");

        RbPath rbPath = RbPath.of("Currencies", "Foo");
        ImmutableList<Result> fallbacks = transformer.getFallbackResultsFor(rbPath, p -> null);
        assertThat(fallbacks).hasSize(2);

        // Both fallbacks look like they are equal, but they didn't come from the same rule...
        assertThat(fallbacks.get(0)).hasKey(rbPath);
        assertThat(fallbacks.get(0)).hasValues("Foo");
        assertThat(fallbacks.get(1)).hasKey(rbPath);
        assertThat(fallbacks.get(1)).hasValues("Foo");

        // ... so they correspond to different matched results.
        assertThat(fallbacks.get(0).isFallbackFor(symbol.get(0))).isTrue();
        assertThat(fallbacks.get(1).isFallbackFor(symbol.get(0))).isFalse();

        assertThat(fallbacks.get(0).isFallbackFor(name.get(0))).isFalse();
        assertThat(fallbacks.get(1).isFallbackFor(name.get(0))).isTrue();

        // And they are ordered by their appearance in the configuration file.
        assertThat(fallbacks.get(0)).isLessThan(fallbacks.get(1));

        // BUT (and this is important) the fallback results are "equal". This is necessary for
        // other situations where results are generated from different rules but should be
        // considered "equal" for purposes of deduplication. Deduplication doesn't affect this
        // situation though (but it's worth being explicit in this test). This is all a bit subtle
        // and should be fixed properly at some point. See also "testBaseXpath()".
        assertThat(fallbacks.get(0)).isEqualTo(fallbacks.get(1));
    }

    @Test
    public void testFallbacks_multipleArgs() {
        PathValueTransformer transformer = transformer(
            "%W=[\\s\\w\\-/]++",
            "//supplementalData/calendarData"
                + "/calendar[@type=\"(%W)\"]/eras/era[@type=\"(%W)\"][@(start|end)=\"(%A)\"]",
            " ; /fake/$2/$4/$1/$3 ; fallback=$1 $2 $3 $4 $3 $2 $1");
        // Path elements match the $N indices so it's easy to see how reordering happens.
        RbPath rbPath = RbPath.of("fake", "two", "four", "one", "three");
        // This shows that the capturing of arguments done on the resource bundle path for the
        // fallback correctly reordered the arguments. Having this many reordered arguments in a
        // fallback is not something that really happens in the actual config files currently, but
        // it's complex logic and needs to be tested. Note also how captured arguments can appear
        // multiple times in the result.
        assertSingleResult(
            transformer.getFallbackResultsFor(rbPath, p -> null),
            rbPath,
            "one", "two", "three", "four", "three", "two", "one");
    }

    @Test
    public void testFallbacks_valueSplitting() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "//supplementalData/likelySubtags/likelySubtag[@from=\"(%A)\"][@to=\"(%A)\"]",
            " ; /fake/$1/$2 ; fallback=$1 and $2");

        RbPath rbPath = RbPath.of("fake", "Foo", "Bar");
        ImmutableList<Result> fallbacks = transformer.getFallbackResultsFor(rbPath, p -> null);
        assertSingleResult(fallbacks, rbPath, "Foo", "and", "Bar");
    }

    @Test
    public void testFallbacks_missingArgs() {
        IllegalStateException e = assertThrows(
            IllegalStateException.class,
            () -> transformer(
                "%A=[^\"']++",
                "//supplementalData/likelySubtags/likelySubtag[@from=\"(%A)\"][@to=\"(%A)\"]",
                " ; /$1 ; fallback=$2"));
        // A bit brittle, but this message is important for debugging.
        assertThat(e).hasMessageThat()
            .contains("fallback values may only contain arguments from the resource bundle path");
        assertThat(e).hasMessageThat().contains("$2");
    }

    @Test
    public void testFallbacks_noValueSubstitution() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "//supplementalData/likelySubtags/likelySubtag[@from=\"(%A)\"][@to=\"(%A)\"]",
            " ; /$1 ; fallback=$1-{value}");

        RbPath rbPath = RbPath.of("Foo");
        ImmutableList<Result> fallbacks = transformer.getFallbackResultsFor(rbPath, p -> null);
        // The {value} token is not substituted in a fallback because there is not value.
        // TODO: Make this into an error (since it's only ever going to happen by mistake)!
        assertSingleResult(fallbacks, rbPath, "Foo-{value}");
    }

    @Test
    public void testFallbacks_noQuotingSupport() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "//supplementalData/likelySubtags/likelySubtag[@from=\"(%A)\"][@to=\"(%A)\"]",
            " ; /fake/$1 ; fallback=\"$1\"");

        RbPath rbPath = RbPath.of("fake", "Foo");
        ImmutableList<Result> fallbacks = transformer.getFallbackResultsFor(rbPath, p -> null);
        // Fallbacks could support quoting of placeholders, but to match legacy behaviour,
        // they don't yet. As it is you cannot prevent fallback values being split on spaces.
        assertSingleResult(fallbacks, rbPath, "\"Foo\"");
    }

    @Test
    public void testHiddenLabelsAndMetazones() {
        PathValueTransformer transformer = transformer(
            "%A=[^\"']++",
            "%W=[\\s\\w\\-/]++",
            "//supplementalData/metaZones/metazoneInfo"
                + "/timezone[@type=\"(%W)\"]/usesMetazone[@mzone=\"(%W)\"]"
                + " ; /metazoneInfo/\"$1\"/<$2> ; values=$2",
            "//supplementalData/metaZones/metazoneInfo"
                + "/timezone[@type=\"(%W)\"]/usesMetazone[@to=\"(%A)\"][@mzone=\"(%W)\"]"
                + " ; /metazoneInfo/\"$1\"/<1970-01-01 00:00> ; values=$3 \"1970-01-01 00:00\" \"$2\"");

        ImmutableList<Result> parisTz = transformPath(
            transformer,
            "//supplementalData/metaZones/metazoneInfo"
                + "/timezone[@type=\"Europe/Paris\"]/usesMetazone[@mzone=\"Europe_Central\"]");

        // The conversion from "Europe/Paris" to "Europe:Paris" is a built in special case when
        // quoting values with '/' in. It's only actually necessary for these timezone identifiers,
        // but the code is applied everywhere since that's easier. Ideally there'd be something
        // like the function calling mechanism to make this transformation explicit, but at the
        // moment, the output resource bunder paths have no way to control the transformation of
        // substituted arguments, so it has to be built in.
        assertSingleResult(
            parisTz, "/metazoneInfo/\"Europe:Paris\"/<Europe_Central>", "Europe_Central");

        ImmutableList<Result> britishTz = transformPath(
            transformer,
            "//supplementalData/metaZones/metazoneInfo"
                + "/timezone[@type=\"Europe/London\"]"
                + "/usesMetazone[@to=\"1971-10-31 02:00\"][@mzone=\"Europe_Central\"]");

        // This example demonstrates that things like ' ' or ':' (normally prohibited in resource
        // bundle path elements) are acceptable in hidden labels, since those will be stripped out
        // while writing the resulting data file. The date-time values are quoted in the rule to
        // ensure they are not split.
        assertSingleResult(
            britishTz,
            "/metazoneInfo/\"Europe:London\"/<1970-01-01 00:00>",
            "Europe_Central", "1970-01-01 00:00", "1971-10-31 02:00");
    }

    @Test
    public void testBaseXpath() {
        PathValueTransformer transformer = transformer(
            "%W=[\\s\\w\\-/]++",
            "%N=[\\d\\.]++",

            // In the real data, these rules define multiple results which reflect the actual
            // differences in the child elements, but the one tested is is only based on the
            // <territory> path prefix, which is the same for many child elements (which is all
            // that's ever actually transformed).
            //
            // So for a single path prefix you'll generate multiple identical results which need
            // to be de-duplicated, which can only happen if they are considered to have come
            // from the same source (since duplicate results happen all the time in general).
            //
            // This is what the base xpath does, it fakes a different source CLDR path which makes
            // the results "equal" (even though they came from different CLDR paths sources).
            "//supplementalData/territoryInfo"
                + "/territory[@type=\"(%W)\"][@gdp=\"(%N)\"][@literacyPercent=\"(%N)\"][@population=\"(%N)\"]"
                + "/languagePopulation[@type=\"(%W)\"][@populationPercent=\"(%N)\"]",
            " ; /territoryInfo/$1/territoryF:intvector"
                + " ; values=$2 $3 $4"
                + " ; base_xpath=//supplementalData/territoryInfo/territory[@type=\"$1\"]",

            // Same thing but with child element containing "writingPercent".
            "//supplementalData/territoryInfo"
                + "/territory[@type=\"(%W)\"][@gdp=\"(%N)\"][@literacyPercent=\"(%N)\"][@population=\"(%N)\"]"
                + "/languagePopulation[@type=\"(%W)\"][@writingPercent=\"(%N)\"][@populationPercent=\"(%N)\"]",
            " ; /territoryInfo/$1/territoryF:intvector"
                + " ; values=$2 $3 $4"
                + " ; base_xpath=//supplementalData/territoryInfo/territory[@type=\"$1\"]");

        String commonPrefix =
            "//supplementalData/territoryInfo"
                + "/territory[@type=\"CI\"][@gdp=\"97160000000\"][@literacyPercent=\"57\"][@population=\"26260600\"]";

        ImmutableList<Result> firstResult = transformPath(
            transformer,
            commonPrefix + "/languagePopulation[@type=\"kfo\"][@populationPercent=\"0.3\"]");

        ImmutableList<Result> secondResult = transformPath(
            transformer,
            commonPrefix + "/languagePopulation[@type=\"sef\"][@writingPercent=\"5\"][@populationPercent=\"4\"]");

        assertSingleResult(
            firstResult, "/territoryInfo/CI/territoryF:intvector", "97160000000", "57", "26260600");
        assertSingleResult(
            secondResult, "/territoryInfo/CI/territoryF:intvector", "97160000000", "57", "26260600");

        // Even though they come from different rules, these results are treated as interchangeably
        // equal because the base path is the same. Without the base path this would not be equal.
        assertThat(firstResult).isEqualTo(secondResult);
    }

    @Test
    public void testResultGrouping() {
        PathValueTransformer transformer = transformer(
            "%W=[\\w\\-/]++",
            "//ldml/numbers/currencies/currency[@type=\"(%W)\"]/symbol ; /Currencies/$1",
            "//ldml/numbers/currencies/currency[@type=\"(%W)\"]/decimal ; /Currencies/$1 ; group");

        Result ungrouped = transformSingleResult(
            transformer, "//ldml/numbers/currencies/currency[@type=\"USD\"]/symbol", "$");
        Result grouped = transformSingleResult(
            transformer, "//ldml/numbers/currencies/currency[@type=\"USD\"]/decimal", ".");

        // Note that grouping is important for some data, but isn't very interesting at the basic
        // transformation level (it's just a bit). It's only interesting when the converter
        // combines multiple results together.
        assertThat(ungrouped).isGrouped(false);
        assertThat(grouped).isGrouped(true);
    }

    private static PathValueTransformer transformer(String... configLines) {
        return RegexTransformer.fromConfigLines(asList(configLines));
    }

    private static ImmutableList<Result> transformPath(
        PathValueTransformer transformer, String cldrPath) {

        return transformer.transform(CldrValue.parseValue(cldrPath, ""));
    }

    private static Result transformSingleResult(
        PathValueTransformer transformer, String path, String value) {

        ImmutableList<Result> results =
            transformer.transform(CldrValue.parseValue(path, value));
        assertThat(results).hasSize(1);
        return results.get(0);
    }

    private static void assertSingleResult(List<Result> results, RbPath path, String... values) {
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isGrouped(false);
        assertThat(results.get(0)).hasKey(path);
        assertThat(results.get(0)).hasValues(values);
    }

    private static void assertSingleResult(List<Result> results, String path, String... values) {
        assertSingleResult(results, RbPath.parse(path), values);
    }
}
