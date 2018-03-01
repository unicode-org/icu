// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.SkeletonSyntaxException;

/**
 * @author sffc
 *
 */
public class NumberSkeletonTest {

    @Test
    public void duplicateValues() {
        try {
            NumberFormatter.fromSkeleton("round-integer round-integer");
            fail();
        } catch (SkeletonSyntaxException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Duplicated setting"));
        }
    }

    @Test
    public void invalidTokens() {
        String[] cases = {
                ".00x",
                ".00##0",
                ".##+",
                ".0#+",
                "@@x",
                "@@##0",
                "@#+",
                "round-increment/xxx",
                "round-increment/0.1.2",
                "currency/dummy",
                "measure-unit/foo",
                "integer-width/xxx",
                "integer-width/0+",
                "integer-width/+0#", };

        for (String cas : cases) {
            try {
                NumberFormatter.fromSkeleton(cas);
                fail();
            } catch (SkeletonSyntaxException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Invalid"));
            }
        }
    }

    @Test
    public void unknownTokens() {
        String[] cases = { "measure-unit/foo-bar", "numbering-system/dummy" };

        for (String cas : cases) {
            try {
                NumberFormatter.fromSkeleton(cas);
                fail();
            } catch (SkeletonSyntaxException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("Unknown"));
            }
        }
    }

    @Test
    public void stemsRequiringOption() {
        String[] stems = { "round-increment", "currency", "measure-unit", "integer-width", };
        String[] suffixes = { "", "/", " scientific", "/ scientific" };

        for (String stem : stems) {
            for (String suffix : suffixes) {
                try {
                    NumberFormatter.fromSkeleton(stem + suffix);
                    fail();
                } catch (SkeletonSyntaxException expected) {
                    assertTrue(expected.getMessage(),
                            expected.getMessage().contains("requires an option"));
                }
            }
        }
    }
}
