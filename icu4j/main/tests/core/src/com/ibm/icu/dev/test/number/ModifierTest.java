// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.modifiers.ConstantAffixModifier;
import com.ibm.icu.impl.number.modifiers.ConstantMultiFieldModifier;
import com.ibm.icu.impl.number.modifiers.CurrencySpacingEnabledModifier;
import com.ibm.icu.impl.number.modifiers.SimpleModifier;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class ModifierTest {
    @Test
    public void testConstantAffixModifier() {
        assertModifierEquals(ConstantAffixModifier.EMPTY, 0, false, "|", "n");

        Modifier mod1 = new ConstantAffixModifier("a", "b", NumberFormat.Field.PERCENT, true);
        assertModifierEquals(mod1, 1, true, "a|b", "%n%");
    }

    @Test
    public void testConstantMultiFieldModifier() {
        NumberStringBuilder prefix = new NumberStringBuilder();
        NumberStringBuilder suffix = new NumberStringBuilder();
        Modifier mod1 = new ConstantMultiFieldModifier(prefix, suffix, true);
        assertModifierEquals(mod1, 0, true, "|", "n");

        prefix.append("a", NumberFormat.Field.PERCENT);
        suffix.append("b", NumberFormat.Field.CURRENCY);
        Modifier mod2 = new ConstantMultiFieldModifier(prefix, suffix, true);
        assertModifierEquals(mod2, 1, true, "a|b", "%n$");

        // Make sure the first modifier is still the same (that it stayed constant)
        assertModifierEquals(mod1, 0, true, "|", "n");
    }

    @Test
    public void testSimpleModifier() {
        String[] patterns = { "{0}", "X{0}Y", "XX{0}YYY", "{0}YY", "XXXX{0}" };
        Object[][] outputs = { { "", 0, 0 }, { "abcde", 0, 0 }, { "abcde", 2, 2 }, { "abcde", 1, 3 } };
        int[] prefixLens = { 0, 1, 2, 0, 4 };
        String[][] expectedCharFields = { { "|", "n" }, { "X|Y", "%n%" }, { "XX|YYY", "%%n%%%" }, { "|YY", "n%%" },
                { "XXXX|", "%%%%n" } };
        String[][] expecteds = { { "", "XY", "XXYYY", "YY", "XXXX" },
                { "abcde", "XYabcde", "XXYYYabcde", "YYabcde", "XXXXabcde" },
                { "abcde", "abXYcde", "abXXYYYcde", "abYYcde", "abXXXXcde" },
                { "abcde", "aXbcYde", "aXXbcYYYde", "abcYYde", "aXXXXbcde" } };
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            String compiledPattern = SimpleFormatterImpl
                    .compileToStringMinMaxArguments(pattern, new StringBuilder(), 1, 1);
            Modifier mod = new SimpleModifier(compiledPattern, NumberFormat.Field.PERCENT, false);
            assertModifierEquals(mod, prefixLens[i], false, expectedCharFields[i][0], expectedCharFields[i][1]);

            // Test strange insertion positions
            for (int j = 0; j < outputs.length; j++) {
                NumberStringBuilder output = new NumberStringBuilder();
                output.append((String) outputs[j][0], null);
                mod.apply(output, (Integer) outputs[j][1], (Integer) outputs[j][2]);
                String expected = expecteds[j][i];
                String actual = output.toString();
                assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testCurrencySpacingEnabledModifier() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        NumberStringBuilder prefix = new NumberStringBuilder();
        NumberStringBuilder suffix = new NumberStringBuilder();
        Modifier mod1 = new CurrencySpacingEnabledModifier(prefix, suffix, true, symbols);
        assertModifierEquals(mod1, 0, true, "|", "n");

        prefix.append("USD", NumberFormat.Field.CURRENCY);
        Modifier mod2 = new CurrencySpacingEnabledModifier(prefix, suffix, true, symbols);
        assertModifierEquals(mod2, 3, true, "USD|", "$$$n");

        // Test the default currency spacing rules
        NumberStringBuilder sb = new NumberStringBuilder();
        sb.append("123", NumberFormat.Field.INTEGER);
        NumberStringBuilder sb1 = new NumberStringBuilder(sb);
        assertModifierEquals(mod2, sb1, 3, true, "USD\u00A0123", "$$$niii");

        // Compare with the unsafe code path
        NumberStringBuilder sb2 = new NumberStringBuilder(sb);
        sb2.insert(0, "USD", NumberFormat.Field.CURRENCY);
        CurrencySpacingEnabledModifier.applyCurrencySpacing(sb2, 0, 3, 6, 0, symbols);
        assertTrue(sb1.toDebugString() + " vs " + sb2.toDebugString(), sb1.contentEquals(sb2));

        // Test custom patterns
        // The following line means that the last char of the number should be a | (rather than a digit)
        symbols.setPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_SURROUNDING_MATCH, true, "[|]");
        suffix.append("XYZ", NumberFormat.Field.CURRENCY);
        Modifier mod3 = new CurrencySpacingEnabledModifier(prefix, suffix, true, symbols);
        assertModifierEquals(mod3, 3, true, "USD|\u00A0XYZ", "$$$nn$$$");
    }

    @Test
    public void testCurrencySpacingPatternStability() {
        // This test checks for stability of the currency spacing patterns in CLDR.
        // For efficiency, ICU caches the most common currency spacing UnicodeSets.
        // If this test starts failing, please update the method #getUnicodeSet() in
        // BOTH CurrencySpacingEnabledModifier.java AND in C++.
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(new ULocale("en-US"));
        assertEquals(
                "[:^S:]",
                dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH, true));
        assertEquals(
                "[:^S:]",
                dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH, false));
        assertEquals(
                "[:digit:]",
                dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_SURROUNDING_MATCH, true));
        assertEquals(
                "[:digit:]",
                dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_SURROUNDING_MATCH, false));
    }

    private void assertModifierEquals(
            Modifier mod,
            int expectedPrefixLength,
            boolean expectedStrong,
            String expectedChars,
            String expectedFields) {
        NumberStringBuilder sb = new NumberStringBuilder();
        sb.appendCodePoint('|', null);
        assertModifierEquals(mod, sb, expectedPrefixLength, expectedStrong, expectedChars, expectedFields);
    }

    private void assertModifierEquals(
            Modifier mod,
            NumberStringBuilder sb,
            int expectedPrefixLength,
            boolean expectedStrong,
            String expectedChars,
            String expectedFields) {
        mod.apply(sb, 0, sb.length());
        assertEquals("Prefix length on " + sb, expectedPrefixLength, mod.getPrefixLength());
        assertEquals("Strong on " + sb, expectedStrong, mod.isStrong());
        assertEquals("<NumberStringBuilder [" + expectedChars + "] [" + expectedFields + "]>", sb.toDebugString());
    }
}
