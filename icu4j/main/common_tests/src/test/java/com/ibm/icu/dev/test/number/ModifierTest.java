// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.number.ConstantAffixModifier;
import com.ibm.icu.impl.number.ConstantMultiFieldModifier;
import com.ibm.icu.impl.number.CurrencySpacingEnabledModifier;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.SimpleModifier;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class ModifierTest {
    @Test
    public void testConstantAffixModifier() {
        assertModifierEquals(ConstantAffixModifier.EMPTY, 0, false, "|", "n");

        Modifier mod1 = new ConstantAffixModifier("a📻", "b", NumberFormat.Field.PERCENT, true);
        assertModifierEquals(mod1, 3, true, "a📻|b", "%%%n%");
    }

    @Test
    public void testConstantMultiFieldModifier() {
        FormattedStringBuilder prefix = new FormattedStringBuilder();
        FormattedStringBuilder suffix = new FormattedStringBuilder();
        Modifier mod1 = new ConstantMultiFieldModifier(prefix, suffix, false, true);
        assertModifierEquals(mod1, 0, true, "|", "n");

        prefix.append("a📻", NumberFormat.Field.PERCENT);
        suffix.append("b", NumberFormat.Field.CURRENCY);
        Modifier mod2 = new ConstantMultiFieldModifier(prefix, suffix, false, true);
        assertModifierEquals(mod2, 3, true, "a📻|b", "%%%n$");

        // Make sure the first modifier is still the same (that it stayed constant)
        assertModifierEquals(mod1, 0, true, "|", "n");
    }

    @Test
    public void testSimpleModifier() {
        String[] patterns = { "{0}", "X{0}Y", "XX{0}YYY", "{0}YY", "XX📺XX{0}" };
        Object[][] outputs = {
                { "", 0, 0 },
                { "a📻bcde", 0, 0 },
                { "a📻bcde", 4, 4 },
                { "a📻bcde", 3, 5 } };
        int[] prefixLens = { 0, 1, 2, 0, 6 };
        String[][] expectedCharFields = {
                { "|", "n" },
                { "X|Y", "%n%" },
                { "XX|YYY", "%%n%%%" },
                { "|YY", "n%%" },
                { "XX📺XX|", "%%%%%%n" } };
        String[][] expecteds = {
                { "", "XY", "XXYYY", "YY", "XX📺XX" },
                { "a📻bcde", "XYa📻bcde", "XXYYYa📻bcde", "YYa📻bcde", "XX📺XXa📻bcde" },
                { "a📻bcde", "a📻bXYcde", "a📻bXXYYYcde", "a📻bYYcde", "a📻bXX📺XXcde" },
                { "a📻bcde", "a📻XbcYde", "a📻XXbcYYYde", "a📻bcYYde", "a📻XX📺XXbcde" } };
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            String compiledPattern = SimpleFormatterImpl
                    .compileToStringMinMaxArguments(pattern, new StringBuilder(), 1, 1);
            Modifier mod = new SimpleModifier(compiledPattern, NumberFormat.Field.PERCENT, false);
            assertModifierEquals(mod,
                    prefixLens[i],
                    false,
                    expectedCharFields[i][0],
                    expectedCharFields[i][1]);

            // Test strange insertion positions
            for (int j = 0; j < outputs.length; j++) {
                FormattedStringBuilder output = new FormattedStringBuilder();
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
        FormattedStringBuilder prefix = new FormattedStringBuilder();
        FormattedStringBuilder suffix = new FormattedStringBuilder();
        Modifier mod1 = new CurrencySpacingEnabledModifier(prefix, suffix, false, true, symbols);
        assertModifierEquals(mod1, 0, true, "|", "n");

        prefix.append("USD", NumberFormat.Field.CURRENCY);
        Modifier mod2 = new CurrencySpacingEnabledModifier(prefix, suffix, false, true, symbols);
        assertModifierEquals(mod2, 3, true, "USD|", "$$$n");

        // Test the default currency spacing rules
        FormattedStringBuilder sb = new FormattedStringBuilder();
        sb.append("123", NumberFormat.Field.INTEGER);
        FormattedStringBuilder sb1 = new FormattedStringBuilder(sb);
        assertModifierEquals(mod2, sb1, 3, true, "USD\u00A0123", "$$$niii");

        // Compare with the unsafe code path
        FormattedStringBuilder sb2 = new FormattedStringBuilder(sb);
        sb2.insert(0, "USD", NumberFormat.Field.CURRENCY);
        CurrencySpacingEnabledModifier.applyCurrencySpacing(sb2, 0, 3, 6, 0, symbols);
        assertTrue(sb1.toDebugString() + " vs " + sb2.toDebugString(), sb1.contentEquals(sb2));

        // Test custom patterns
        // The following line means that the last char of the number should be a | (rather than a digit)
        symbols.setPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_SURROUNDING_MATCH,
                true,
                "[|]");
        suffix.append("XYZ", NumberFormat.Field.CURRENCY);
        Modifier mod3 = new CurrencySpacingEnabledModifier(prefix, suffix, false, true, symbols);
        assertModifierEquals(mod3, 3, true, "USD|\u00A0XYZ", "$$$nn$$$");
    }

    @Test
    public void testCurrencySpacingPatternStability() {
        // This test checks for stability of the currency spacing patterns in CLDR.
        // For efficiency, ICU caches the most common currency spacing UnicodeSets.
        // If this test starts failing, please update the method #getUnicodeSet() in
        // BOTH CurrencySpacingEnabledModifier.java AND in C++.
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(new ULocale("en-US"));
        assertEquals("[[:^S:]&[:^Z:]]",
                dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH,
                        true));
        assertEquals("[[:^S:]&[:^Z:]]",
                dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH,
                        false));
        assertEquals("[:digit:]",
                dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_SURROUNDING_MATCH,
                        true));
        assertEquals("[:digit:]",
                dfs.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_SURROUNDING_MATCH,
                        false));
    }

    private void assertModifierEquals(
            Modifier mod,
            int expectedPrefixLength,
            boolean expectedStrong,
            String expectedChars,
            String expectedFields) {
        FormattedStringBuilder sb = new FormattedStringBuilder();
        sb.appendCodePoint('|', null);
        assertModifierEquals(mod,
                sb,
                expectedPrefixLength,
                expectedStrong,
                expectedChars,
                expectedFields);
    }

    private void assertModifierEquals(
            Modifier mod,
            FormattedStringBuilder sb,
            int expectedPrefixLength,
            boolean expectedStrong,
            String expectedChars,
            String expectedFields) {
        int oldCount = sb.codePointCount();
        mod.apply(sb, 0, oldCount);
        assertEquals("Prefix length on " + sb, expectedPrefixLength, mod.getPrefixLength());
        assertEquals("Strong on " + sb, expectedStrong, mod.isStrong());
        if (!(mod instanceof CurrencySpacingEnabledModifier)) {
            assertEquals("Code point count equals actual code point count",
                    sb.codePointCount() - oldCount,
                    mod.getCodePointCount());
        }
        assertEquals("<FormattedStringBuilder [" + expectedChars + "] [" + expectedFields + "]>",
                sb.toDebugString());
    }
}
