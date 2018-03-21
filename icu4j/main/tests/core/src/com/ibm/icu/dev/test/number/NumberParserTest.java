// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.CustomSymbolCurrency;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.parse.AffixPatternMatcher;
import com.ibm.icu.impl.number.parse.AffixTokenMatcherFactory;
import com.ibm.icu.impl.number.parse.AnyMatcher;
import com.ibm.icu.impl.number.parse.IgnorablesMatcher;
import com.ibm.icu.impl.number.parse.MinusSignMatcher;
import com.ibm.icu.impl.number.parse.NumberParserImpl;
import com.ibm.icu.impl.number.parse.ParsedNumber;
import com.ibm.icu.impl.number.parse.ParsingUtils;
import com.ibm.icu.impl.number.parse.PercentMatcher;
import com.ibm.icu.impl.number.parse.PlusSignMatcher;
import com.ibm.icu.impl.number.parse.SeriesMatcher;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Currency;
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
                { 7, "51,423", "#,##,##0", 6, 51423. },
                { 7, " 51,423", "#,##,##0", 7, 51423. },
                { 7, "51,423 ", "#,##,##0", 6, 51423. },
                { 7, "𝟱𝟭,𝟰𝟮𝟯", "#,##,##0", 11, 51423. },
                { 7, "𝟳,𝟴𝟵,𝟱𝟭,𝟰𝟮𝟯", "#,##,##0", 19, 78951423. },
                { 7, "𝟳𝟴,𝟵𝟱𝟭.𝟰𝟮𝟯", "#,##,##0", 18, 78951.423 },
                { 7, "𝟳𝟴,𝟬𝟬𝟬", "#,##,##0", 11, 78000. },
                { 7, "𝟳𝟴,𝟬𝟬𝟬.𝟬𝟬𝟬", "#,##,##0", 18, 78000. },
                { 7, "𝟳𝟴,𝟬𝟬𝟬.𝟬𝟮𝟯", "#,##,##0", 18, 78000.023 },
                { 7, "𝟳𝟴.𝟬𝟬𝟬.𝟬𝟮𝟯", "#,##,##0", 11, 78. },
                { 3, "-51423", "0", 6, -51423. },
                { 3, "51423-", "0", 5, 51423. }, // plus and minus sign by default do NOT match after
                { 3, "+51423", "0", 6, 51423. },
                { 3, "51423+", "0", 5, 51423. }, // plus and minus sign by default do NOT match after
                { 3, "%51423", "0", 6, 514.23 },
                { 3, "51423%", "0", 6, 514.23 },
                { 3, "51423%%", "0", 6, 514.23 },
                { 3, "‰51423", "0", 6, 51.423 },
                { 3, "51423‰", "0", 6, 51.423 },
                { 3, "51423‰‰", "0", 6, 51.423 },
                { 3, "∞", "0", 1, Double.POSITIVE_INFINITY },
                { 3, "-∞", "0", 2, Double.NEGATIVE_INFINITY },
                { 3, "@@@123  @@", "0", 6, 123. }, // TODO: Should padding be strong instead of weak?
                { 3, "@@@123@@  ", "0", 6, 123. }, // TODO: Should padding be strong instead of weak?
                { 3, "a51423US dollars", "a0¤¤¤", 16, 51423. },
                { 3, "a 51423 US dollars", "a0¤¤¤", 18, 51423. },
                { 3, "514.23 USD", "¤0", 10, 514.23 },
                { 3, "514.23 GBP", "¤0", 10, 514.23 },
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
                { 7, "5,142.50 Canadian dollars", "#,##,##0 ¤¤¤", 25, 5142.5 },
                { 3, "a$ b5", "a ¤ b0", 5, 5.0 },
                { 3, "📺1.23", "📺0;📻0", 6, 1.23 },
                { 3, "📻1.23", "📺0;📻0", 6, -1.23 },
                { 3, ".00", "0", 3, 0.0 },
                { 3, "                              1,234", "a0", 35, 1234. }, // should not hang
                { 3, "NaN", "0", 3, Double.NaN },
                { 3, "NaN E5", "0", 3, Double.NaN },
                { 3, "0", "0", 1, 0.0 } };

        int parseFlags = ParsingUtils.PARSE_FLAG_IGNORE_CASE
                | ParsingUtils.PARSE_FLAG_INCLUDE_UNPAIRED_AFFIXES;
        for (Object[] cas : cases) {
            int flags = (Integer) cas[0];
            String inputString = (String) cas[1];
            String patternString = (String) cas[2];
            int expectedCharsConsumed = (Integer) cas[3];
            double expectedResultDouble = (Double) cas[4];
            NumberParserImpl parser = NumberParserImpl
                    .createSimpleParser(ULocale.ENGLISH, patternString, parseFlags);
            String message = "Input <" + inputString + "> Parser " + parser;

            if (0 != (flags & 0x01)) {
                // Test greedy code path
                ParsedNumber resultObject = new ParsedNumber();
                parser.parse(inputString, true, resultObject);
                assertTrue("Greedy Parse failed: " + message, resultObject.success());
                assertEquals("Greedy Parse failed: " + message,
                        expectedCharsConsumed,
                        resultObject.charEnd);
                assertEquals("Greedy Parse failed: " + message,
                        expectedResultDouble,
                        resultObject.getNumber().doubleValue(),
                        0.0);
            }

            if (0 != (flags & 0x02)) {
                // Test slow code path
                ParsedNumber resultObject = new ParsedNumber();
                parser.parse(inputString, false, resultObject);
                assertTrue("Non-Greedy Parse failed: " + message, resultObject.success());
                assertEquals("Non-Greedy Parse failed: " + message,
                        expectedCharsConsumed,
                        resultObject.charEnd);
                assertEquals("Non-Greedy Parse failed: " + message,
                        expectedResultDouble,
                        resultObject.getNumber().doubleValue(),
                        0.0);
            }

            if (0 != (flags & 0x04)) {
                // Test with strict separators
                parser = NumberParserImpl.createSimpleParser(ULocale.ENGLISH,
                        patternString,
                        parseFlags | ParsingUtils.PARSE_FLAG_STRICT_GROUPING_SIZE);
                ParsedNumber resultObject = new ParsedNumber();
                parser.parse(inputString, true, resultObject);
                assertTrue("Strict Parse failed: " + message, resultObject.success());
                assertEquals("Strict Parse failed: " + message,
                        expectedCharsConsumed,
                        resultObject.charEnd);
                assertEquals("Strict Parse failed: " + message,
                        expectedResultDouble,
                        resultObject.getNumber().doubleValue(),
                        0.0);
            }
        }
    }

    @Test
    public void testLocaleFi() {
        // This case is interesting because locale fi has NaN starting with 'e', the same as scientific
        NumberParserImpl parser = NumberParserImpl
                .createSimpleParser(new ULocale("fi"), "0", ParsingUtils.PARSE_FLAG_IGNORE_CASE);

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

        assertFalse(series.smokeTest(new StringSegment("x", false)));
        assertFalse(series.smokeTest(new StringSegment("-", false)));
        assertTrue(series.smokeTest(new StringSegment("+", false)));

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
    public void testCurrencyAnyMatcher() {
        AffixTokenMatcherFactory factory = new AffixTokenMatcherFactory();
        factory.locale = ULocale.ENGLISH;
        CustomSymbolCurrency currency = new CustomSymbolCurrency("ICU", "IU$", "ICU");
        factory.currency = currency;
        AnyMatcher matcher = factory.currency();

        Object[][] cases = new Object[][] {
                { "", null },
                { "FOO", null },
                { "USD", "USD" },
                { "$", "USD" },
                { "US dollars", "USD" },
                { "eu", null },
                { "euros", "EUR" },
                { "ICU", "ICU" },
                { "IU$", "ICU" } };
        for (Object[] cas : cases) {
            String input = (String) cas[0];
            String expectedCurrencyCode = (String) cas[1];

            StringSegment segment = new StringSegment(input, true);
            ParsedNumber result = new ParsedNumber();
            matcher.match(segment, result);
            assertEquals("Parsing " + input, expectedCurrencyCode, result.currencyCode);
            assertEquals("Whole string on " + input,
                    expectedCurrencyCode == null ? 0 : input.length(),
                    result.charEnd);
        }
    }

    @Test
    public void testAffixPatternMatcher() {
        AffixTokenMatcherFactory factory = new AffixTokenMatcherFactory();
        factory.currency = Currency.getInstance("EUR");
        factory.symbols = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        factory.ignorables = IgnorablesMatcher.DEFAULT;
        factory.locale = ULocale.ENGLISH;

        Object[][] cases = {
                { false, "-", 1, "-" },
                { false, "+-%", 5, "+-%" },
                { true, "+-%", 3, "+-%" },
                { false, "ab c", 5, "a    bc" },
                { true, "abc", 3, "abc" },
                { false, "hello-to+this%very¤long‰string", 59, "hello-to+this%very USD long‰string" } };

        for (Object[] cas : cases) {
            boolean exactMatch = (Boolean) cas[0];
            String affixPattern = (String) cas[1];
            int expectedMatcherLength = (Integer) cas[2];
            String sampleParseableString = (String) cas[3];
            int parseFlags = exactMatch ? ParsingUtils.PARSE_FLAG_EXACT_AFFIX : 0;

            AffixPatternMatcher matcher = AffixPatternMatcher
                    .fromAffixPattern(affixPattern, factory, parseFlags);

            // Check that the matcher has the expected number of children
            assertEquals(affixPattern + " " + exactMatch, expectedMatcherLength, matcher.length());

            // Check that the matcher works on a sample string
            StringSegment segment = new StringSegment(sampleParseableString, true);
            ParsedNumber result = new ParsedNumber();
            matcher.match(segment, result);
            assertEquals(affixPattern + " " + exactMatch,
                    sampleParseableString.length(),
                    result.charEnd);
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

            NumberParserImpl caseSensitiveParser = NumberParserImpl.createSimpleParser(ULocale.ENGLISH,
                    patternString,
                    ParsingUtils.PARSE_FLAG_OPTIMIZE);
            ParsedNumber result = new ParsedNumber();
            caseSensitiveParser.parse(inputString, true, result);
            assertEquals("Case-Sensitive: " + inputString + " on " + patternString,
                    expectedCaseSensitiveChars,
                    result.charEnd);

            NumberParserImpl caseFoldingParser = NumberParserImpl.createSimpleParser(ULocale.ENGLISH,
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
