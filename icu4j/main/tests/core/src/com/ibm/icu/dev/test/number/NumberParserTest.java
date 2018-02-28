// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.parse.IgnorablesMatcher;
import com.ibm.icu.impl.number.parse.MinusSignMatcher;
import com.ibm.icu.impl.number.parse.NumberParserImpl;
import com.ibm.icu.impl.number.parse.ParsedNumber;
import com.ibm.icu.impl.number.parse.ParsingUtils;
import com.ibm.icu.impl.number.parse.PercentMatcher;
import com.ibm.icu.impl.number.parse.PlusSignMatcher;
import com.ibm.icu.impl.number.parse.SeriesMatcher;
import com.ibm.icu.impl.number.parse.UnicodeSetStaticCache;
import com.ibm.icu.impl.number.parse.UnicodeSetStaticCache.Key;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.ULocale;

/**
 * @author sffc
 *
 */
public class NumberParserTest {
    @Test
    public void testBasic() {
        Object[][] cases = new Object[][] {
                // Fields:
                // a) Flags:
                // --- Bit 0x01 => Test greedy implementation
                // --- Bit 0x02 => Test slow implementation
                // --- Bit 0x04 => Test strict grouping separators
                // b) Input string
                // c) Pattern
                // d) Expected chars consumed
                // e) Expected double result
                { 3, "51423", "0", 5, 51423. },
                { 3, "51423x", "0", 5, 51423. },
                { 3, " 51423", "0", 6, 51423. },
                { 3, "51423 ", "0", 5, 51423. },
                { 3, "𝟱𝟭𝟰𝟮𝟯", "0", 10, 51423. },
                { 3, "𝟱𝟭𝟰𝟮𝟯x", "0", 10, 51423. },
                { 3, " 𝟱𝟭𝟰𝟮𝟯", "0", 11, 51423. },
                { 3, "𝟱𝟭𝟰𝟮𝟯 ", "0", 10, 51423. },
                { 7, "𝟱𝟭,𝟰𝟮𝟯", "#,##,##0", 11, 51423. },
                { 7, "𝟳,𝟴𝟵,𝟱𝟭,𝟰𝟮𝟯", "#,##,##0", 19, 78951423. },
                { 7, "𝟳𝟴,𝟵𝟱𝟭.𝟰𝟮𝟯", "#,##,##0", 18, 78951.423 },
                { 7, "𝟳𝟴,𝟬𝟬𝟬", "#,##,##0", 11, 78000. },
                { 7, "𝟳𝟴,𝟬𝟬𝟬.𝟬𝟬𝟬", "#,##,##0", 18, 78000. },
                { 7, "𝟳𝟴,𝟬𝟬𝟬.𝟬𝟮𝟯", "#,##,##0", 18, 78000.023 },
                { 7, "𝟳𝟴.𝟬𝟬𝟬.𝟬𝟮𝟯", "#,##,##0", 11, 78. },
                { 3, "-𝟱𝟭𝟰𝟮𝟯", "0", 11, -51423. },
                { 3, "-𝟱𝟭𝟰𝟮𝟯-", "0", 11, -51423. },
                { 3, "a51423US dollars", "a0¤¤¤", 16, 51423. },
                { 3, "a 51423 US dollars", "a0¤¤¤", 18, 51423. },
                { 3, "514.23 USD", "0", 10, 514.23 },
                { 3, "514.23 GBP", "0", 10, 514.23 },
                { 3, "a 𝟱𝟭𝟰𝟮𝟯 b", "a0b", 14, 51423. },
                { 3, "-a 𝟱𝟭𝟰𝟮𝟯 b", "a0b", 15, -51423. },
                { 3, "a -𝟱𝟭𝟰𝟮𝟯 b", "a0b", 15, -51423. },
                { 3, "𝟱𝟭𝟰𝟮𝟯", "[0];(0)", 10, 51423. },
                { 3, "[𝟱𝟭𝟰𝟮𝟯", "[0];(0)", 11, 51423. },
                { 3, "𝟱𝟭𝟰𝟮𝟯]", "[0];(0)", 11, 51423. },
                { 3, "[𝟱𝟭𝟰𝟮𝟯]", "[0];(0)", 12, 51423. },
                { 3, "(𝟱𝟭𝟰𝟮𝟯", "[0];(0)", 11, -51423. },
                { 3, "𝟱𝟭𝟰𝟮𝟯)", "[0];(0)", 11, -51423. },
                { 3, "(𝟱𝟭𝟰𝟮𝟯)", "[0];(0)", 12, -51423. },
                { 3, "𝟱𝟭𝟰𝟮𝟯", "{0};{0}", 10, 51423. },
                { 3, "{𝟱𝟭𝟰𝟮𝟯", "{0};{0}", 11, 51423. },
                { 3, "𝟱𝟭𝟰𝟮𝟯}", "{0};{0}", 11, 51423. },
                { 3, "{𝟱𝟭𝟰𝟮𝟯}", "{0};{0}", 12, 51423. },
                { 1, "a40b", "a0'0b'", 3, 40. }, // greedy code path thinks "40" is the number
                { 2, "a40b", "a0'0b'", 4, 4. }, // slow code path finds the suffix "0b"
                { 3, "𝟱.𝟭𝟰𝟮E𝟯", "0", 12, 5142. },
                { 3, "𝟱.𝟭𝟰𝟮E-𝟯", "0", 13, 0.005142 },
                { 3, "𝟱.𝟭𝟰𝟮e-𝟯", "0", 13, 0.005142 },
                { 7, "5,142.50 Canadian dollars", "#,##,##0", 25, 5142.5 },
                { 3, "a$ b5", "a ¤ b0", 5, 5.0 },
                { 3, "📺1.23", "📺0;📻0", 6, 1.23 },
                { 3, "📻1.23", "📺0;📻0", 6, -1.23 },
                { 3, ".00", "0", 3, 0.0 },
                { 3, "                              0", "a0", 31, 0.0 }, // should not hang
                { 3, "0", "0", 1, 0.0 } };

        for (Object[] cas : cases) {
            int flags = (Integer) cas[0];
            String input = (String) cas[1];
            String pattern = (String) cas[2];
            int expectedCharsConsumed = (Integer) cas[3];
            double resultDouble = (Double) cas[4];
            NumberParserImpl parser = NumberParserImpl
                    .createParserFromPattern(ULocale.ENGLISH, pattern, false);
            String message = "Input <" + input + "> Parser " + parser;

            if (0 != (flags & 0x01)) {
                // Test greedy code path
                ParsedNumber resultObject = new ParsedNumber();
                parser.parse(input, true, resultObject);
                assertNotNull("Greedy Parse failed: " + message, resultObject.quantity);
                assertEquals("Greedy Parse failed: " + message,
                        expectedCharsConsumed,
                        resultObject.charEnd);
                assertEquals("Greedy Parse failed: " + message,
                        resultDouble,
                        resultObject.getNumber().doubleValue(),
                        0.0);
            }

            if (0 != (flags & 0x02)) {
                // Test slow code path
                ParsedNumber resultObject = new ParsedNumber();
                parser.parse(input, false, resultObject);
                assertNotNull("Non-Greedy Parse failed: " + message, resultObject.quantity);
                assertEquals("Non-Greedy Parse failed: " + message,
                        expectedCharsConsumed,
                        resultObject.charEnd);
                assertEquals("Non-Greedy Parse failed: " + message,
                        resultDouble,
                        resultObject.getNumber().doubleValue(),
                        0.0);
            }

            if (0 != (flags & 0x04)) {
                // Test with strict separators
                parser = NumberParserImpl.createParserFromPattern(ULocale.ENGLISH, pattern, true);
                ParsedNumber resultObject = new ParsedNumber();
                parser.parse(input, true, resultObject);
                assertNotNull("Strict Parse failed: " + message, resultObject.quantity);
                assertEquals("Strict Parse failed: " + message,
                        expectedCharsConsumed,
                        resultObject.charEnd);
                assertEquals("Strict Parse failed: " + message,
                        resultDouble,
                        resultObject.getNumber().doubleValue(),
                        0.0);
            }
        }
    }

    @Test
    public void testLocaleFi() {
        // This case is interesting because locale fi has NaN starting with 'e', the same as scientific
        NumberParserImpl parser = NumberParserImpl
                .createParserFromPattern(new ULocale("fi"), "0", false);

        ParsedNumber resultObject = new ParsedNumber();
        parser.parse("epäluku", false, resultObject);
        assertTrue(resultObject.success());
        assertEquals(Double.NaN, resultObject.getNumber().doubleValue(), 0.0);

        resultObject = new ParsedNumber();
        parser.parse("1,2e3", false, resultObject);
        assertTrue(resultObject.success());
        assertEquals(1200.0, resultObject.getNumber().doubleValue(), 0.0);
    }

    @Test
    public void testSeriesMatcher() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        SeriesMatcher series = new SeriesMatcher();
        series.addMatcher(PlusSignMatcher.getInstance(symbols, false));
        series.addMatcher(MinusSignMatcher.getInstance(symbols, false));
        series.addMatcher(IgnorablesMatcher.DEFAULT);
        series.addMatcher(PercentMatcher.getInstance(symbols));
        series.addMatcher(IgnorablesMatcher.DEFAULT);
        series.freeze();

        assertEquals(UnicodeSetStaticCache.get(Key.PLUS_SIGN), series.getLeadCodePoints());

        Object[][] cases = new Object[][] {
                { "", 0, true },
                { " ", 0, false },
                { "$", 0, false },
                { "+", 0, true },
                { " +", 0, false },
                { "+-", 0, true },
                { "+ -", 0, false },
                { "+-  ", 0, true },
                { "+-  $", 0, false },
                { "+-%", 3, true },
                { "  +-  %  ", 0, false },
                { "+-  %  ", 7, true },
                { "+-%$", 3, false } };
        for (Object[] cas : cases) {
            String input = (String) cas[0];
            int expectedOffset = (Integer) cas[1];
            boolean expectedMaybeMore = (Boolean) cas[2];

            StringSegment segment = new StringSegment(input, false);
            ParsedNumber result = new ParsedNumber();
            boolean actualMaybeMore = series.match(segment, result);
            int actualOffset = segment.getOffset();

            assertEquals("'" + input + "'", expectedOffset, actualOffset);
            assertEquals("'" + input + "'", expectedMaybeMore, actualMaybeMore);
        }
    }

    @Test
    public void testGroupingDisabled() {
        DecimalFormatProperties properties = new DecimalFormatProperties();
        properties.setGroupingSize(0);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        NumberParserImpl parser = NumberParserImpl
                .createParserFromProperties(properties, symbols, false, true);
        ParsedNumber result = new ParsedNumber();
        parser.parse("12,345.678", true, result);
        assertEquals("Should not parse with grouping separator",
                12.0,
                result.getNumber().doubleValue(),
                0.0);
    }

    @Test
    public void testCaseFolding() {
        Object[][] cases = new Object[][] {
                // pattern, input string, case sensitive chars, case insensitive chars
                { "0", "JP¥3456", 7, 7 },
                { "0", "jp¥3456", 0, 0 }, // not to be accepted, even in case insensitive mode
                { "A0", "A5", 2, 2 },
                { "A0", "a5", 0, 2 },
                { "0", "NaN", 3, 3 },
                { "0", "nan", 0, 3 } };
        for (Object[] cas : cases) {
            String patternString = (String) cas[0];
            String inputString = (String) cas[1];
            int expectedCaseSensitiveChars = (Integer) cas[2];
            int expectedCaseFoldingChars = (Integer) cas[3];

            NumberParserImpl caseSensitiveParser = NumberParserImpl
                    .removeMeWhenMerged(ULocale.ENGLISH, patternString, ParsingUtils.PARSE_FLAG_OPTIMIZE);
            ParsedNumber result = new ParsedNumber();
            caseSensitiveParser.parse(inputString, true, result);
            assertEquals("Case-Sensitive: " + inputString + " on " + patternString,
                    expectedCaseSensitiveChars,
                    result.charEnd);

            NumberParserImpl caseFoldingParser = NumberParserImpl.removeMeWhenMerged(ULocale.ENGLISH,
                    patternString,
                    ParsingUtils.PARSE_FLAG_IGNORE_CASE | ParsingUtils.PARSE_FLAG_OPTIMIZE);
            result = new ParsedNumber();
            caseFoldingParser.parse(inputString, true, result);
            assertEquals("Folded: " + inputString + " on " + patternString,
                    expectedCaseFoldingChars,
                    result.charEnd);
        }
    }
}
