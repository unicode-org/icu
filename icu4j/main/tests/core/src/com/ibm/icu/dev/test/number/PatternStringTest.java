// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringUtils;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.ULocale;

/** @author sffc */
public class PatternStringTest {

    @Test
    public void testLocalized() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        symbols.setDecimalSeparatorString("a");
        symbols.setPercentString("b");
        symbols.setMinusSignString(".");
        symbols.setPlusSignString("'");

        String standard = "+-abcb''a''#,##0.0%'a%'";
        String localized = "’.'ab'c'b''a'''#,##0a0b'a%'";
        String toStandard = "+-'ab'c'b''a'''#,##0.0%'a%'";

        assertEquals(localized, PatternStringUtils.convertLocalized(standard, symbols, true));
        assertEquals(toStandard, PatternStringUtils.convertLocalized(localized, symbols, false));
    }

    @Test
    public void testToPatternSimple() {
        String[][] cases = {
                { "#", "0" },
                { "0", "0" },
                { "#0", "0" },
                { "###", "0" },
                { "0.##", "0.##" },
                { "0.00", "0.00" },
                { "0.00#", "0.00#" },
                { "0.05", "0.05" },
                { "#E0", "#E0" },
                { "0E0", "0E0" },
                { "#00E00", "#00E00" },
                { "#,##0", "#,##0" },
                { "#;#", "0;0" },
                { "#;-#", "0" }, // ignore a negative prefix pattern of '-' since that is the default
                { "pp#,000;(#)", "pp#,000;(#,000)" },
                { "**##0", "**##0" },
                { "*'x'##0", "*x##0" },
                { "a''b0", "a''b0" },
                { "*''##0", "*''##0" },
                { "*📺##0", "*'📺'##0" },
                { "*'நி'##0", "*'நி'##0" }, };

        for (String[] cas : cases) {
            String input = cas[0];
            String output = cas[1];

            DecimalFormatProperties properties = PatternStringParser.parseToProperties(input);
            String actual = PatternStringUtils.propertiesToPatternString(properties);
            assertEquals("Failed on input pattern '" + input + "', properties " + properties,
                    output,
                    actual);
        }
    }

    @Test
    public void testToPatternWithProperties() {
        Object[][] cases = {
                { new DecimalFormatProperties().setPositivePrefix("abc"), "abc#;-#" },
                { new DecimalFormatProperties().setPositiveSuffix("abc"), "#abc;-#" },
                { new DecimalFormatProperties().setPositivePrefixPattern("abc"), "abc#" },
                { new DecimalFormatProperties().setPositiveSuffixPattern("abc"), "#abc" },
                { new DecimalFormatProperties().setNegativePrefix("abc"), "#;abc#" },
                { new DecimalFormatProperties().setNegativeSuffix("abc"), "#;-#abc" },
                { new DecimalFormatProperties().setNegativePrefixPattern("abc"), "#;abc#" },
                { new DecimalFormatProperties().setNegativeSuffixPattern("abc"), "#;-#abc" },
                { new DecimalFormatProperties().setPositivePrefix("+"), "'+'#;-#" },
                { new DecimalFormatProperties().setPositivePrefixPattern("+"), "+#" },
                { new DecimalFormatProperties().setPositivePrefix("+'"), "'+'''#;-#" },
                { new DecimalFormatProperties().setPositivePrefix("'+"), "'''+'#;-#" },
                { new DecimalFormatProperties().setPositivePrefix("'"), "''#;-#" },
                { new DecimalFormatProperties().setPositivePrefixPattern("+''"), "+''#" }, };

        for (Object[] cas : cases) {
            DecimalFormatProperties input = (DecimalFormatProperties) cas[0];
            String output = (String) cas[1];

            String actual = PatternStringUtils.propertiesToPatternString(input);
            assertEquals("Failed on input properties " + input, output, actual);
        }
    }

    @Test
    public void testExceptionOnInvalid() {
        String[] invalidPatterns = {
                "#.#.#",
                "0#",
                "0#.",
                ".#0",
                "0#.#0",
                "@0",
                "0@",
                "0,",
                "0,,",
                "0,,0",
                "0,,0,",
                "#,##0E0" };

        for (String pattern : invalidPatterns) {
            try {
                PatternStringParser.parseToProperties(pattern);
                fail("Didn't throw IllegalArgumentException when parsing pattern: " + pattern);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    @Test
    public void testBug13117() {
        DecimalFormatProperties expected = PatternStringParser.parseToProperties("0");
        DecimalFormatProperties actual = PatternStringParser.parseToProperties("0;");
        assertEquals("Should not consume negative subpattern", expected, actual);
    }
}
