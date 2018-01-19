// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.number.parse.NumberParserImpl;
import com.ibm.icu.impl.number.parse.ParsedNumber;
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
                { 4, "𝟳𝟴,𝟵𝟱𝟭,𝟰𝟮𝟯", "#,##,##0", 11, 78951. },
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
                // { 3, "a$ b5", "a ¤ b0", 6, 5.0 }, // TODO: Does not work
                { 3, "📺1.23", "📺0;📻0", 6, 1.23 },
                { 3, "📻1.23", "📺0;📻0", 6, -1.23 },
                { 3, ".00", "0", 3, 0.0 },
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
                assertNotNull(message, resultObject.quantity);
                assertEquals(message, expectedCharsConsumed, resultObject.charsConsumed);
                assertEquals(message, resultDouble, resultObject.getNumber().doubleValue(), 0.0);
            }

            if (0 != (flags & 0x02)) {
                // Test slow code path
                ParsedNumber resultObject = new ParsedNumber();
                parser.parse(input, false, resultObject);
                assertNotNull(message, resultObject.quantity);
                assertEquals(message, expectedCharsConsumed, resultObject.charsConsumed);
                assertEquals(message, resultDouble, resultObject.getNumber().doubleValue(), 0.0);
            }

            if (0 != (flags & 0x04)) {
                // Test with strict separators
                parser = NumberParserImpl.createParserFromPattern(ULocale.ENGLISH, pattern, true);
                ParsedNumber resultObject = new ParsedNumber();
                parser.parse(input, true, resultObject);
                assertNotNull(message, resultObject.quantity);
                assertEquals(message, expectedCharsConsumed, resultObject.charsConsumed);
                assertEquals(message, resultDouble, resultObject.getNumber().doubleValue(), 0.0);
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
        parser.parse("1.2e3", false, resultObject);
        assertTrue(resultObject.success());
        assertEquals(12000.0, resultObject.getNumber().doubleValue(), 0.0);
    }
}
