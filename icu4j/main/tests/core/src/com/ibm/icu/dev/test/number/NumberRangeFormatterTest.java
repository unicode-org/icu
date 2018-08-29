// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

import com.ibm.icu.number.LocalizedNumberRangeFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.NumberRangeFormatter;
import com.ibm.icu.number.UnlocalizedNumberRangeFormatter;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

/**
 * @author sffc
 *
 */
public class NumberRangeFormatterTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");

    @Test
    public void testSanity() {
        LocalizedNumberRangeFormatter lnrf1 = NumberRangeFormatter.withLocale(ULocale.US);
        LocalizedNumberRangeFormatter lnrf2 = NumberRangeFormatter.with().locale(ULocale.US);
        LocalizedNumberRangeFormatter lnrf3 = NumberRangeFormatter.withLocale(Locale.US);
        LocalizedNumberRangeFormatter lnrf4 = NumberRangeFormatter.with().locale(Locale.US);
        assertEquals("Formatters should be equal 1", lnrf1, lnrf2);
        assertEquals("Formatters should be equal 2", lnrf2, lnrf3);
        assertEquals("Formatters should be equal 3", lnrf3, lnrf4);
        assertEquals("Formatters should have same behavior 1", lnrf1.formatRange(4, 6), lnrf2.formatRange(4, 6));
        assertEquals("Formatters should have same behavior 2", lnrf2.formatRange(4, 6), lnrf3.formatRange(4, 6));
        assertEquals("Formatters should have same behavior 3", lnrf3.formatRange(4, 6), lnrf4.formatRange(4, 6));
    }

    @Test
    public void testBasic() {
        assertFormatRange(
                "Basic",
                NumberRangeFormatter.with(),
                ULocale.US,
                "1 --- 5",
                "5 --- 5",
                "5 --- 5",
                "0 --- 3",
                "0 --- 0",
                "3 --- 3,000",
                "3,000 --- 5,000",
                "4,999 --- 5,001",
                "5,000 --- 5,000",
                "5,000 --- 5,000,000");
    }

    @Test
    public void testNullBehavior() {
        assertFormatRange(
                "Basic",
                NumberRangeFormatter.with().numberFormatterBoth(null),
                ULocale.US,
                "1 --- 5",
                "5 --- 5",
                "5 --- 5",
                "0 --- 3",
                "0 --- 0",
                "3 --- 3,000",
                "3,000 --- 5,000",
                "4,999 --- 5,001",
                "5,000 --- 5,000",
                "5,000 --- 5,000,000");

        assertFormatRange(
                "Basic",
                NumberRangeFormatter.with().numberFormatterFirst(null),
                ULocale.US,
                "1 --- 5",
                "5 --- 5",
                "5 --- 5",
                "0 --- 3",
                "0 --- 0",
                "3 --- 3,000",
                "3,000 --- 5,000",
                "4,999 --- 5,001",
                "5,000 --- 5,000",
                "5,000 --- 5,000,000");

        assertFormatRange(
                "Basic",
                NumberRangeFormatter.with()
                    .numberFormatterFirst(NumberFormatter.with().grouping(GroupingStrategy.OFF))
                    .numberFormatterSecond(null),
                ULocale.US,
                "1 --- 5",
                "5 --- 5",
                "5 --- 5",
                "0 --- 3",
                "0 --- 0",
                "3 --- 3,000",
                "3000 --- 5,000",
                "4999 --- 5,001",
                "5000 --- 5,000",
                "5000 --- 5,000,000");

        assertFormatRange(
                "Basic",
                NumberRangeFormatter.with()
                    .numberFormatterFirst(null)
                    .numberFormatterSecond(NumberFormatter.with().grouping(GroupingStrategy.OFF)),
                ULocale.US,
                "1 --- 5",
                "5 --- 5",
                "5 --- 5",
                "0 --- 3",
                "0 --- 0",
                "3 --- 3000",
                "3,000 --- 5000",
                "4,999 --- 5001",
                "5,000 --- 5000",
                "5,000 --- 5000000");
    }

    static void assertFormatRange(
            String message,
            UnlocalizedNumberRangeFormatter f,
            ULocale locale,
            String expected_10_50,
            String expected_49_51,
            String expected_50_50,
            String expected_00_30,
            String expected_00_00,
            String expected_30_3K,
            String expected_30K_50K,
            String expected_49K_51K,
            String expected_50K_50K,
            String expected_50K_50M) {
        LocalizedNumberRangeFormatter l = f.locale(locale);
        assertFormattedRangeEquals(message, l, 1, 5, expected_10_50);
        assertFormattedRangeEquals(message, l, 4.9999999, 5.0000001, expected_49_51);
        assertFormattedRangeEquals(message, l, 5, 5, expected_50_50);
        assertFormattedRangeEquals(message, l, 0, 3, expected_00_30);
        assertFormattedRangeEquals(message, l, 0, 0, expected_00_00);
        assertFormattedRangeEquals(message, l, 3, 3000, expected_30_3K);
        assertFormattedRangeEquals(message, l, 3000, 5000, expected_30K_50K);
        assertFormattedRangeEquals(message, l, 4999, 5001, expected_49K_51K);
        assertFormattedRangeEquals(message, l, 5000, 5000, expected_50K_50K);
        assertFormattedRangeEquals(message, l, 5e3, 5e6, expected_50K_50M);
    }

    private static void assertFormattedRangeEquals(String message, LocalizedNumberRangeFormatter l, Number first,
            Number second, String expected) {
        String actual1 = l.formatRange(first, second).toString();
        assertEquals(message + ": " + first + ", " + second, expected, actual1);
    }

}
