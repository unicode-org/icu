// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.RoundingMode;
import java.util.Locale;

import org.junit.Test;

import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.SkeletonSyntaxException;
import com.ibm.icu.util.ULocale;

/**
 * @author sffc
 *
 */
public class NumberSkeletonTest {

    @Test
    public void validTokens() {
        // This tests only if the tokens are valid, not their behavior.
        // Most of these are from the design doc.
        String[] cases = {
                "precision-integer",
                "precision-unlimited",
                "@@@##",
                "@@*",
                "@@+",
                "@@+/w",
                ".000##",
                ".00*",
                ".00+",
                ".",
                "./w",
                ".*",
                ".+",
                ".+/w",
                ".######",
                ".00/@@*",
                ".00/@@+",
                ".00/@##",
                ".00/@##/w",
                ".00/@",
                ".00/@r",
                ".00/@@s",
                ".00/@@#r",
                "precision-increment/3.14",
                "precision-increment/3.14/w",
                "precision-currency-standard",
                "precision-currency-standard/w",
                "precision-integer rounding-mode-half-up",
                ".00# rounding-mode-ceiling",
                ".00/@@* rounding-mode-floor",
                ".00/@@+ rounding-mode-floor",
                "scientific",
                "scientific/*ee",
                "scientific/+ee",
                "scientific/sign-always",
                "scientific/*ee/sign-always",
                "scientific/+ee/sign-always",
                "scientific/sign-always/*ee",
                "scientific/sign-always/+ee",
                "scientific/sign-except-zero",
                "engineering",
                "engineering/*eee",
                "engineering/+eee",
                "compact-short",
                "compact-long",
                "notation-simple",
                "percent",
                "permille",
                "measure-unit/length-meter",
                "measure-unit/area-square-meter",
                "measure-unit/energy-joule per-measure-unit/length-meter",
                "unit/square-meter-per-square-meter",
                "currency/XXX",
                "currency/ZZZ",
                "currency/usd",
                "group-off",
                "group-min2",
                "group-auto",
                "group-on-aligned",
                "group-thousands",
                "integer-width/00",
                "integer-width/#0",
                "integer-width/*00",
                "integer-width/+00",
                "sign-always",
                "sign-auto",
                "sign-never",
                "sign-accounting",
                "sign-accounting-always",
                "sign-except-zero",
                "sign-accounting-except-zero",
                "unit-width-narrow",
                "unit-width-short",
                "unit-width-iso-code",
                "unit-width-full-name",
                "unit-width-hidden",
                "decimal-auto",
                "decimal-always",
                "scale/5.2",
                "scale/-5.2",
                "scale/100",
                "scale/1E2",
                "scale/1",
                "latin",
                "numbering-system/arab",
                "numbering-system/latn",
                "precision-integer/@##",
                "precision-integer rounding-mode-ceiling",
                "precision-currency-cash rounding-mode-ceiling",
                "0",
                "00",
                "000",
                "E0",
                "E00",
                "E000",
                "EE0",
                "EE00",
                "EE+?0",
                "EE+?00",
                "EE+!0",
                "EE+!00",
        };

        for (String cas : cases) {
            try {
                NumberFormatter.forSkeleton(cas);
            } catch (SkeletonSyntaxException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void invalidTokens() {
        String[] cases = {
                ".00x",
                ".00i",
                ".00/x",
                ".00/ww",
                ".00##0",
                ".##*",
                ".00##*",
                ".0#*",
                "@#*",
                ".##+",
                ".00##+",
                ".0#+",
                "@#+",
                "@@x",
                "@@##0",
                ".00/@@",
                ".00/@@x",
                ".00/@@#",
                ".00/@@#*",
                ".00/floor/@@*", // wrong order
                ".00/@@#+",
                ".00/@@@+r",
                ".00/floor/@@+", // wrong order
                "precision-increment/français", // non-invariant characters for C++
                "scientific/ee",
                "precision-increment/xxx",
                "precision-increment/NaN",
                "precision-increment/Infinity",
                "precision-increment/0.1.2",
                "scale/xxx",
                "scale/NaN",
                "scale/Infinity",
                "scale/0.1.2",
                "scale/français", // non-invariant characters for C++
                "currency/dummy",
                "currency/ççç", // three characters but not ASCII
                "measure-unit/foo",
                "integer-width/xxx",
                "integer-width/0*",
                "integer-width/*0#",
                "integer-width/*#",
                "integer-width/*#0",
                "integer-width/0+",
                "integer-width/+0#",
                "integer-width/+#",
                "integer-width/+#0",
                "scientific/foo",
                "E",
                "E1",
                "E+",
                "E+?",
                "E+!",
                "E+0",
                "EE",
                "EE+",
                "EEE",
                "EEE0",
                "001",
                "00*",
                "00+",
        };

        for (String cas : cases) {
            try {
                NumberFormatter.forSkeleton(cas);
                fail(cas);
            } catch (SkeletonSyntaxException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Invalid"));
            }
        }
    }

    @Test
    public void unknownTokens() {
        String[] cases = {
                "maesure-unit",
                "measure-unit/foo-bar",
                "numbering-system/dummy",
                "français",
                "measure-unit/français-français", // non-invariant characters for C++
                "numbering-system/français", // non-invariant characters for C++
                "currency-USD" };

        for (String cas : cases) {
            try {
                NumberFormatter.forSkeleton(cas);
                fail(cas);
            } catch (SkeletonSyntaxException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Unknown"));
            }
        }
    }

    @Test
    public void unexpectedTokens() {
        String[] cases = {
                ".00/w/w",
                "group-thousands/foo",
                "precision-integer//@## group-off",
                "precision-integer//@##  group-off",
                "precision-integer/ group-off",
                "precision-integer// group-off" };

        for (String cas : cases) {
            try {
                NumberFormatter.forSkeleton(cas);
                fail(cas);
            } catch (SkeletonSyntaxException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Unexpected"));
            }
        }
    }

    @Test
    public void duplicateValues() {
        String[] cases = {
                "precision-integer precision-integer",
                "precision-integer .00+",
                "precision-integer precision-unlimited",
                "precision-integer @@@",
                "scientific engineering",
                "engineering compact-long",
                "sign-auto sign-always" };

        for (String cas : cases) {
            try {
                NumberFormatter.forSkeleton(cas);
                fail(cas);
            } catch (SkeletonSyntaxException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Duplicated"));
            }
        }
    }

    @Test
    public void stemsRequiringOption() {
        String[] stems = {
                "precision-increment",
                "measure-unit",
                "per-measure-unit",
                "currency",
                "integer-width",
                "numbering-system",
                "scale" };
        String[] suffixes = { "", "/@##", " scientific", "/@## scientific" };

        for (String stem : stems) {
            for (String suffix : suffixes) {
                String skeletonString = stem + suffix;
                try {
                    NumberFormatter.forSkeleton(skeletonString);
                    fail(skeletonString);
                } catch (SkeletonSyntaxException expected) {
                    // Success
                }
            }
        }
    }

    @Test
    public void defaultTokens() {
        String[] cases = {
                "notation-simple",
                "base-unit",
                "group-auto",
                "integer-width/+0",
                "sign-auto",
                "unit-width-short",
                "decimal-auto" };

        for (String skeleton : cases) {
            String normalized = NumberFormatter.forSkeleton(skeleton).toSkeleton();
            assertEquals("Skeleton should become empty when normalized: " + skeleton, "", normalized);
        }
    }

    @Test
    public void flexibleSeparators() {
        String[][] cases = {
                { "precision-integer group-off", "5142" },
                { "precision-integer  group-off", "5142" },
                { "precision-integer/@## group-off", "5140" },
                { "precision-integer/@##  group-off", "5140" }, };

        for (String[] cas : cases) {
            String skeleton = cas[0];
            String expected = cas[1];
            String actual = NumberFormatter.forSkeleton(skeleton).locale(ULocale.ENGLISH).format(5142.3)
                    .toString();
            assertEquals(skeleton, expected, actual);
        }
    }

    @Test
    public void wildcardCharacters() {
        String[][] cases = {
            { ".00*", ".00+" },
            { "@@*", "@@+" },
            { "scientific/*ee", "scientific/+ee" },
            { "integer-width/*00", "integer-width/+00" },
        };

        for (String[] cas : cases) {
            String star = cas[0];
            String plus = cas[1];

            String normalized = NumberFormatter.forSkeleton(plus)
                .toSkeleton();
            assertEquals("Plus should normalize to star", star, normalized);
        }
    }

    @Test
    public void roundingModeNames() {
        for (RoundingMode mode : RoundingMode.values()) {
            if (mode == RoundingMode.HALF_EVEN) {
                // This rounding mode is not printed in the skeleton since it is the default
                continue;
            }
            String skeleton = NumberFormatter.with().roundingMode(mode).toSkeleton();
            String modeString = mode.toString().toLowerCase().replace('_', '-');
            assertEquals(mode.toString(), modeString, skeleton.substring(14));
        }
    }

    @Test
    public void perUnitInArabic() {
        String[][] cases = {
                {"area", "acre"},
                {"digital", "bit"},
                {"digital", "byte"},
                {"temperature", "celsius"},
                {"length", "centimeter"},
                {"duration", "day"},
                {"angle", "degree"},
                {"temperature", "fahrenheit"},
                {"volume", "fluid-ounce"},
                {"length", "foot"},
                {"volume", "gallon"},
                {"digital", "gigabit"},
                {"digital", "gigabyte"},
                {"mass", "gram"},
                {"area", "hectare"},
                {"duration", "hour"},
                {"length", "inch"},
                {"digital", "kilobit"},
                {"digital", "kilobyte"},
                {"mass", "kilogram"},
                {"length", "kilometer"},
                {"volume", "liter"},
                {"digital", "megabit"},
                {"digital", "megabyte"},
                {"length", "meter"},
                {"length", "mile"},
                {"length", "mile-scandinavian"},
                {"volume", "milliliter"},
                {"length", "millimeter"},
                {"duration", "millisecond"},
                {"duration", "minute"},
                {"duration", "month"},
                {"mass", "ounce"},
                {"concentr", "percent"},
                {"digital", "petabyte"},
                {"mass", "pound"},
                {"duration", "second"},
                {"mass", "stone"},
                {"digital", "terabit"},
                {"digital", "terabyte"},
                {"duration", "week"},
                {"length", "yard"},
                {"duration", "year"},
        };

        ULocale arabic = new ULocale("ar");
        for (String[] cas1 : cases) {
            for (String[] cas2 : cases) {
                String skeleton = "measure-unit/";
                skeleton += cas1[0] + "-" + cas1[1] + " per-measure-unit/" + cas2[0] + "-" + cas2[1];

                @SuppressWarnings("unused")
                String actual = NumberFormatter.forSkeleton(skeleton).locale(arabic).format(5142.3)
                        .toString();
                // Just make sure it won't throw exception
            }
        }
    }

    @Test
    public void perUnitToSkeleton() {
        String[][] cases = {
            {"area", "acre"},
            {"concentr", "percent"},
            {"concentr", "permille"},
            {"concentr", "permillion"},
            {"concentr", "permyriad"},
            {"digital", "bit"},
            {"length", "yard"},
        };

        for (String[] cas1 : cases) {
            for (String[] cas2 : cases) {
                String skeleton = "measure-unit/" + cas1[0] + "-" + cas1[1] + " per-measure-unit/" +
                                  cas2[0] + "-" + cas2[1];

                if (cas1[0] != cas2[0] && cas1[1] != cas2[1]) {
                    String toSkeleton = NumberFormatter.forSkeleton(skeleton).toSkeleton();

                    // Ensure both subtype are in the toSkeleton.
                    String msg;
                    msg = toSkeleton + " should contain '" + cas1[1] + "' when constructed from " +
                          skeleton;
                    assertTrue(msg, toSkeleton.indexOf(cas1[1]) >= 0);
                    msg = toSkeleton + " should contain '" + cas2[1] + "' when constructed from " +
                          skeleton;
                    assertTrue(msg, toSkeleton.indexOf(cas2[1]) >= 0);
                }
            }
        }
    }

    @Test
    public void measurementSystemOverride() {
        // NOTE TO REVIEWERS: When the appropriate changes are made on the CLDR side, do we want to keep this
        // test or rely on additions the CLDR project makes to unitPreferencesTest.txt? --rtg 8/29/23
        String[][] testCases = {
            // Norway uses m/s for wind speed and should with or without the "ms-metric" subtag in the locale,
            // but it uses km/h for other speeds.  France uses km/h for all speeds.  And in both places, if
            // you say "ms-ussystem", you should get mph.  In the US, we use mph for all speeds, but should
            // use km/h if the locale has "ms-metric" in it.
            { "nn-NO",               "unit/kilometer-per-hour usage/wind",    "0,34 m/s" },
            { "nn-NO-u-ms-metric",   "unit/kilometer-per-hour usage/wind",    "0,34 m/s" },
            { "nn-NO-u-ms-ussystem", "unit/kilometer-per-hour usage/wind",    "0,76 mile/t" },
            { "fr-FR",               "unit/kilometer-per-hour usage/wind",    "1,2\u202Fkm/h" },
            { "fr-FR-u-ms-metric",   "unit/kilometer-per-hour usage/wind",    "1,2\u202Fkm/h" },
            { "fr-FR-u-ms-ussystem", "unit/kilometer-per-hour usage/wind",    "0,76\u202Fmi/h" },
            { "en-US",               "unit/kilometer-per-hour usage/wind",    "0.76 mph" },
            { "en-US-u-ms-metric",   "unit/kilometer-per-hour usage/wind",    "1.2 km/h" },
            { "en-US-u-ms-ussystem", "unit/kilometer-per-hour usage/wind",    "0.76 mph" },

            { "nn-NO",               "unit/kilometer-per-hour usage/default", "1,2 km/t" },
            { "nn-NO-u-ms-metric",   "unit/kilometer-per-hour usage/default", "1,2 km/t" },
            { "nn-NO-u-ms-ussystem", "unit/kilometer-per-hour usage/default", "0,76 mile/t" },
            { "fr-FR",               "unit/kilometer-per-hour usage/default", "1,2\u202Fkm/h" },
            { "fr-FR-u-ms-metric",   "unit/kilometer-per-hour usage/default", "1,2\u202Fkm/h" },
            { "fr-FR-u-ms-ussystem", "unit/kilometer-per-hour usage/default", "0,76\u202Fmi/h" },
            { "en-US",               "unit/kilometer-per-hour usage/default", "0.76 mph" },
            { "en-US-u-ms-metric",   "unit/kilometer-per-hour usage/default", "1.2 km/h" },
            { "en-US-u-ms-ussystem", "unit/kilometer-per-hour usage/default", "0.76 mph" },
        };

        for (String[] testCase : testCases) {
            String languageTag = testCase[0];
            String skeleton = testCase[1];
            String expectedResult = testCase[2];

            LocalizedNumberFormatter nf = NumberFormatter.forSkeleton(skeleton).locale(Locale.forLanguageTag(languageTag));
            String actualResult = nf.format(1.23).toString();

            assertEquals("Wrong result: " + languageTag + ":" + skeleton, expectedResult, actualResult);
        }
    }
}
