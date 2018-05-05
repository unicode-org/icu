// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.RoundingMode;

import org.junit.Test;

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
                "@@+",
                ".000##",
                ".00+",
                ".",
                ".+",
                ".######",
                ".00/@@+",
                ".00/@##",
                "precision-increment/3.14",
                "precision-currency-standard",
                "precision-integer rounding-mode-half-up",
                ".00# rounding-mode-ceiling",
                ".00/@@+ rounding-mode-floor",
                "scientific",
                "scientific/+ee",
                "scientific/sign-always",
                "scientific/+ee/sign-always",
                "scientific/sign-always/+ee",
                "scientific/sign-except-zero",
                "engineering",
                "engineering/+eee",
                "compact-short",
                "compact-long",
                "notation-simple",
                "percent",
                "permille",
                "measure-unit/length-meter",
                "measure-unit/area-square-meter",
                "measure-unit/energy-joule per-measure-unit/length-meter",
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
                "precision-currency-cash rounding-mode-ceiling" };

        for (String cas : cases) {
            try {
                NumberFormatter.fromSkeleton(cas);
            } catch (SkeletonSyntaxException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void invalidTokens() {
        String[] cases = {
                ".00x",
                ".00##0",
                ".##+",
                ".00##+",
                ".0#+",
                "@@x",
                "@@##0",
                "@#+",
                ".00/@",
                ".00/@@",
                ".00/@@x",
                ".00/@@#",
                ".00/@@#+",
                ".00/floor/@@+", // wrong order
                "precision-increment/français", // non-invariant characters for C++
                "scientific/ee",
                "precision-increment/xxx",
                "precision-increment/NaN",
                "precision-increment/0.1.2",
                "scale/xxx",
                "scale/NaN",
                "scale/0.1.2",
                "scale/français", // non-invariant characters for C++
                "currency/dummy",
                "currency/ççç", // three characters but not ASCII
                "measure-unit/foo",
                "integer-width/xxx",
                "integer-width/0+",
                "integer-width/+0#",
                "scientific/foo" };

        for (String cas : cases) {
            try {
                NumberFormatter.fromSkeleton(cas);
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
                NumberFormatter.fromSkeleton(cas);
                fail(cas);
            } catch (SkeletonSyntaxException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Unknown"));
            }
        }
    }

    @Test
    public void unexpectedTokens() {
        String[] cases = {
                "group-thousands/foo",
                "precision-integer//@## group-off",
                "precision-integer//@##  group-off",
                "precision-integer/ group-off",
                "precision-integer// group-off" };

        for (String cas : cases) {
            try {
                NumberFormatter.fromSkeleton(cas);
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
                NumberFormatter.fromSkeleton(cas);
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
                "per-unit",
                "currency",
                "integer-width",
                "numbering-system",
                "scale" };
        String[] suffixes = { "", "/@##", " scientific", "/@## scientific" };

        for (String stem : stems) {
            for (String suffix : suffixes) {
                String skeletonString = stem + suffix;
                try {
                    NumberFormatter.fromSkeleton(skeletonString);
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
            String normalized = NumberFormatter.fromSkeleton(skeleton).toSkeleton();
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
            String actual = NumberFormatter.fromSkeleton(skeleton).locale(ULocale.ENGLISH).format(5142.3)
                    .toString();
            assertEquals(skeleton, expected, actual);
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
}
