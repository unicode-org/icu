// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.number.AffixUtils;
import com.ibm.icu.impl.number.AffixUtils.SymbolProvider;
import com.ibm.icu.text.UnicodeSet;

public class AffixUtilsTest {

    private static final SymbolProvider DEFAULT_SYMBOL_PROVIDER = new SymbolProvider() {
        @Override
        public CharSequence getSymbol(int type) {
            // Use interesting symbols where possible. The symbols are from ar_SA but are hard-coded
            // here to make the test independent of locale data changes.
            switch (type) {
            case AffixUtils.TYPE_MINUS_SIGN:
                return "−";
            case AffixUtils.TYPE_PLUS_SIGN:
                return "\u061C+";
            case AffixUtils.TYPE_PERCENT:
                return "٪\u061C";
            case AffixUtils.TYPE_PERMILLE:
                return "؉";
            case AffixUtils.TYPE_CURRENCY_SINGLE:
                return "$";
            case AffixUtils.TYPE_CURRENCY_DOUBLE:
                return "XXX";
            case AffixUtils.TYPE_CURRENCY_TRIPLE:
                return "long name";
            case AffixUtils.TYPE_CURRENCY_QUAD:
                return "\uFFFD";
            case AffixUtils.TYPE_CURRENCY_QUINT:
                return "@";
            case AffixUtils.TYPE_CURRENCY_OVERFLOW:
                return "\uFFFD";
            default:
                throw new AssertionError();
            }
        }
    };

    @Test
    public void testEscape() {
        Object[][] cases = {
                { "", "" },
                { "abc", "abc" },
                { "-", "'-'" },
                { "-!", "'-'!" },
                { "−", "−" },
                { "---", "'---'" },
                { "-%-", "'-%-'" },
                { "'", "''" },
                { "-'", "'-'''" },
                { "-'-", "'-''-'" },
                { "a-'-", "a'-''-'" } };

        StringBuilder sb = new StringBuilder();
        for (Object[] cas : cases) {
            String input = (String) cas[0];
            String expected = (String) cas[1];
            sb.setLength(0);
            AffixUtils.escape(input, sb);
            assertEquals(expected, sb.toString());
        }
    }

    @Test
    public void testUnescape() {
        Object[][] cases = {
                { "", false, 0, "" },
                { "abc", false, 3, "abc" },
                { "📺", false, 1, "📺" },
                { "-", false, 1, "−" },
                { "-!", false, 2, "−!" },
                { "+", false, 1, "\u061C+" },
                { "+!", false, 2, "\u061C+!" },
                { "‰", false, 1, "؉" },
                { "‰!", false, 2, "؉!" },
                { "-x", false, 2, "−x" },
                { "'-'x", false, 2, "-x" },
                { "'--''-'-x", false, 6, "--'-−x" },
                { "''", false, 1, "'" },
                { "''''", false, 2, "''" },
                { "''''''", false, 3, "'''" },
                { "''x''", false, 3, "'x'" },
                { "¤", true, 1, "$" },
                { "¤¤", true, 2, "XXX" },
                { "¤¤¤", true, 3, "long name" },
                { "¤¤¤¤", true, 4, "\uFFFD" },
                { "¤¤¤¤¤", true, 5, "@" },
                { "¤¤¤¤¤¤", true, 6, "\uFFFD" },
                { "¤¤¤a¤¤¤¤", true, 8, "long namea\uFFFD" },
                { "a¤¤¤¤b¤¤¤¤¤c", true, 12, "a\uFFFDb@c" },
                { "¤!", true, 2, "$!" },
                { "¤¤!", true, 3, "XXX!" },
                { "¤¤¤!", true, 4, "long name!" },
                { "-¤¤", true, 3, "−XXX" },
                { "¤¤-", true, 3, "XXX−" },
                { "'¤'", false, 1, "¤" },
                { "%", false, 1, "٪\u061C" },
                { "'%'", false, 1, "%" },
                { "¤'-'%", true, 3, "$-٪\u061C" },
                { "#0#@#*#;#", false, 9, "#0#@#*#;#" } };

        for (Object[] cas : cases) {
            String input = (String) cas[0];
            boolean curr = (Boolean) cas[1];
            int length = (Integer) cas[2];
            String output = (String) cas[3];

            assertEquals("Currency on <" + input + ">", curr, AffixUtils.hasCurrencySymbols(input));
            assertEquals("Length on <" + input + ">", length, AffixUtils.estimateLength(input));

            String actual = unescapeWithDefaults(input);
            assertEquals("Output on <" + input + ">", output, actual);

            int ulength = AffixUtils.unescapedCount(input, true, DEFAULT_SYMBOL_PROVIDER);
            assertEquals("Unescaped length on <" + input + ">", output.length(), ulength);

            int ucpcount = AffixUtils.unescapedCount(input, false, DEFAULT_SYMBOL_PROVIDER);
            assertEquals("Unescaped length on <" + input + ">",
                    output.codePointCount(0, output.length()),
                    ucpcount);
        }
    }

    @Test
    public void testContainsReplaceType() {
        Object[][] cases = {
                { "", false, "" },
                { "-", true, "+" },
                { "-a", true, "+a" },
                { "a-", true, "a+" },
                { "a-b", true, "a+b" },
                { "--", true, "++" },
                { "x", false, "x" } };

        for (Object[] cas : cases) {
            String input = (String) cas[0];
            boolean hasMinusSign = (Boolean) cas[1];
            String output = (String) cas[2];

            assertEquals("Contains on input " + input,
                    hasMinusSign,
                    AffixUtils.containsType(input, AffixUtils.TYPE_MINUS_SIGN));
            assertEquals("Replace on input" + input,
                    output,
                    AffixUtils.replaceType(input, AffixUtils.TYPE_MINUS_SIGN, '+'));
        }
    }

    @Test
    public void testInvalid() {
        String[] invalidExamples = { "'", "x'", "'x", "'x''", "''x'" };

        for (String str : invalidExamples) {
            try {
                AffixUtils.hasCurrencySymbols(str);
                fail("No exception was thrown on an invalid string");
            } catch (IllegalArgumentException e) {
                // OK
            }
            try {
                AffixUtils.estimateLength(str);
                fail("No exception was thrown on an invalid string");
            } catch (IllegalArgumentException e) {
                // OK
            }
            try {
                unescapeWithDefaults(str);
                fail("No exception was thrown on an invalid string");
            } catch (IllegalArgumentException e) {
                // OK
            }
        }
    }

    @Test
    public void testUnescapeWithSymbolProvider() {
        String[][] cases = {
                { "", "" },
                { "-", "1" },
                { "'-'", "-" },
                { "- + % ‰ ¤ ¤¤ ¤¤¤ ¤¤¤¤ ¤¤¤¤¤", "1 2 3 4 5 6 7 8 9" },
                { "'¤¤¤¤¤¤'", "¤¤¤¤¤¤" },
                { "¤¤¤¤¤¤", "\uFFFD" } };

        SymbolProvider provider = new SymbolProvider() {
            @Override
            public CharSequence getSymbol(int type) {
                return Integer.toString(Math.abs(type));
            }
        };

        FormattedStringBuilder sb = new FormattedStringBuilder();
        for (String[] cas : cases) {
            String input = cas[0];
            String expected = cas[1];
            sb.clear();
            AffixUtils.unescape(input, sb, 0, provider, null);
            assertEquals("With symbol provider on <" + input + ">", expected, sb.toString());
        }

        // Test insertion position
        sb.clear();
        sb.append("abcdefg", null);
        AffixUtils.unescape("-+%", sb, 4, provider, null);
        assertEquals("Symbol provider into middle", "abcd123efg", sb.toString());
    }

    @Test
    public void testWithoutSymbolsOrIgnorables() {
        Object[][] cases = {
                { "", true },
                { "-", true },
                { " ", true },
                { "'-'", false },
                { " a + b ", false },
                { "-a+b%c‰d¤e¤¤f¤¤¤g¤¤¤¤h¤¤¤¤¤i", false }, };

        UnicodeSet ignorables = new UnicodeSet("[:whitespace:]");
        for (Object[] cas : cases) {
            String input = (String) cas[0];
            boolean expected = (Boolean) cas[1];
            assertEquals("Contains only symbols and ignorables: " + input,
                    expected,
                    AffixUtils.containsOnlySymbolsAndIgnorables(input, ignorables));
        }
    }

    private static String unescapeWithDefaults(String input) {
        FormattedStringBuilder nsb = new FormattedStringBuilder();
        int length = AffixUtils.unescape(input, nsb, 0, DEFAULT_SYMBOL_PROVIDER, null);
        assertEquals("Return value of unescape", nsb.length(), length);
        return nsb.toString();
    }
}
