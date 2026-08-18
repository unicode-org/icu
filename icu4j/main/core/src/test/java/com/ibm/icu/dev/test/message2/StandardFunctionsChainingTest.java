// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import static org.junit.Assert.assertEquals;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.message2.FormattedPlaceholder;
import com.ibm.icu.message2.Function;
import com.ibm.icu.message2.FunctionFactory;
import com.ibm.icu.message2.MFFunctionRegistry;
import com.ibm.icu.message2.MessageFormatter;
import com.ibm.icu.message2.PlainStringFormattedValue;
import com.ibm.icu.text.FormattedValue;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Showing a custom function chaining to a standard function. Does all kind of manipulations at all
 * kind of stages, to see how flexible the approach is.
 */
@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class StandardFunctionsChainingTest extends CoreTestFmwk {

    /*
     * This custom function just chains to the standard implementation, with some exceptions.
     * 1. Replace a some locale(s) with another: `en-US` style for ALL `it` locales
     * 2. Completely replace the standard function (not chaining to it): return "a dozen" for 12
     * 3. Change the value to format and options, then chain to the standard function: "exact" PI
     * 4. Call the standard function and change the formatted result: change separators for en-CN
     * 5. The format and selection use different locales
     */
    static class CustomNumberFunctionFactory implements FunctionFactory {
        @Override
        public Function create(Locale locale, Map<String, ?> fixedOptions) {
            return new CustomNumberFunctionImpl(locale, fixedOptions);
        }

        static class CustomNumberFunctionImpl implements Function {
            Function standardNumberFormatFunction = null;
            Function standardNumberSelectFunction = null;
            final Locale formatLocale;
            final Locale selectLocale;

            CustomNumberFunctionImpl(Locale locale, Map<String, ?> fixedOptions) {
                // 1. Replace a some locale(s) with another: `en-US` style for ALL `it` locales
                if (locale != null && locale.getLanguage().equals("it")) {
                    formatLocale = Locale.US;
                    selectLocale = Locale.forLanguageTag("ro");
                } else {
                    formatLocale = locale;
                    selectLocale = locale;
                }
                // Create the original function, potentially with a different locale
                FunctionFactory standardNumberFunctionFactory =
                        MFFunctionRegistry.getStandardFunctionsRegistry().getFunction("number");
                // * 5. The format and selection use different locales
                standardNumberFormatFunction =
                        standardNumberFunctionFactory.create(formatLocale, fixedOptions);
                standardNumberSelectFunction =
                        standardNumberFunctionFactory.create(selectLocale, fixedOptions);
            }

            @Override
            public String formatToString(Object toFormat, Map<String, ?> variableOptions) {
                return format(toFormat, variableOptions).toString();
            }

            @Override
            public FormattedPlaceholder format(Object toFormat, Map<String, ?> variableOptions) {
                if (standardNumberFormatFunction == null) {
                    // We should not be able to get here
                    String result = "Chaining ERROR: " + Objects.toString(toFormat);
                    return new FormattedPlaceholder(
                            toFormat, new PlainStringFormattedValue(result));
                }
                if (toFormat instanceof Number) {
                    Number tmp = (Number) toFormat;

                    // 2. Completely replace the standard function (not chaining to it): return "a
                    // dozen" for 12
                    if (tmp.doubleValue() == 12) {
                        return new FormattedPlaceholder(
                                toFormat, new PlainStringFormattedValue("a dozen"));
                    }

                    // 3. Change the value to format and options, then chain to the standard
                    // function: "exact" PI
                    if (tmp.doubleValue() >= 3.14 && tmp.doubleValue() < 3.15) {
                        toFormat = 3.14159265358979323846;
                        // Change the options to show PI with higher precision
                        Map<String, Object> vopt = new HashMap<>(variableOptions);
                        vopt.put("maximumFractionDigits", 20);
                        variableOptions = vopt;
                    }
                }

                // 4. Call the standard function and change the formatted result: change separators
                // for en-CN
                FormattedPlaceholder standardResult =
                        standardNumberFormatFunction.format(toFormat, variableOptions);
                if (formatLocale.getLanguage().equals("en")
                        && formatLocale.getCountry().equals("CN")) {
                    FormattedValue fmtValue = standardResult.getFormattedValue();
                    // Change the thousand and decimal separators to wide versions
                    String strValue =
                            fmtValue.toString().replace('.', '\uFF0E').replace(',', '\uFF0C');
                    fmtValue = new PlainStringFormattedValue(strValue);
                    return new FormattedPlaceholder(toFormat, fmtValue);
                }

                return standardResult;
            }

            @Override
            public List<String> matches(
                    Object value, List<String> keys, Map<String, ?> variableOptions) {
                return standardNumberSelectFunction.matches(value, keys, variableOptions);
            }
        }
    }

    @Test
    public void testChainingNumberFormatter() {
        final MFFunctionRegistry registry =
                MFFunctionRegistry.builder()
                        .setFunction("number", new CustomNumberFunctionFactory())
                        .build();

        Map<String, Double> bigCount = Map.of("val", 123456789.97531);

        // Standard behavior, not registry with my custom function
        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern("it-CH number: {$val}!")
                        .locale("it-CH")
                        .arguments(bigCount)
                        .expected("it-CH number: 123'456'789.97531!")
                        .build());
        // The it-CH locale format is forced to en-US by the custom function
        TestUtils.runTestCase(
                registry,
                new TestCase.Builder()
                        .pattern("it-CH number with override: {$val}!")
                        .locale("it")
                        .arguments(bigCount)
                        .expected("it-CH number with override: 123,456,789.97531!")
                        .build());

        // Test that we managed to call the standard function then changed the standard result
        TestUtils.runTestCase(
                registry,
                new TestCase.Builder()
                        .pattern("en-CN number: {$val}!")
                        .locale("en-CN")
                        .arguments(bigCount)
                        .expected("en-CN number: 123，456，789．97531!")
                        .build());

        // With the standard function the maximumFractionDigits is pretty low
        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern("The value of PI is {$val}!")
                        .arguments(Map.of("val", 3.141592653589793))
                        .expected("The value of PI is 3.141593!")
                        .build());
        // Test that we changed the input and options before chaining to the standard function
        TestUtils.runTestCase(
                registry,
                new TestCase.Builder()
                        .pattern("The value of PI is {$val}!")
                        .arguments(Map.of("val", 3.14))
                        .expected("The value of PI is 3.141592653589793!")
                        .build());

        // We completely replaced the standard formatter
        TestUtils.runTestCase(
                registry,
                new TestCase.Builder()
                        .pattern("The farmer bought {$val} eggs!")
                        .arguments(Map.of("val", 12))
                        .expected("The farmer bought a dozen eggs!")
                        .build());
        TestUtils.runTestCase(
                registry,
                new TestCase.Builder()
                        .pattern("The farmer bought {$val} eggs!")
                        .arguments(Map.of("val", 20))
                        .expected("The farmer bought 20 eggs!")
                        .build());
        // Selector uses yet another locale (Romanian)
        String pluralPattern =
                ""
                        + ".input {$count :number}"
                        + ".match $count"
                        + "  one  {{Plural 'one' case, count = {$count}!}}"
                        + "  few  {{Plural 'few' case, count = {$count}!}}"
                        + "  *    {{Plural '*' case, count = {$count}!}}";

        MessageFormatter mf =
                MessageFormatter.builder()
                        .setFunctionRegistry(registry)
                        .setPattern(pluralPattern)
                        .setLocale(Locale.ITALIAN)
                        .build();
        assertEquals(
                "selection override, one",
                "Plural 'one' case, count = 1!",
                mf.formatToString(Map.of("count", 1)));
        assertEquals(
                "selection override, few",
                "Plural 'few' case, count = 17!",
                mf.formatToString(Map.of("count", 17)));
        assertEquals(
                "selection overridem, other",
                "Plural '*' case, count = 27!",
                mf.formatToString(Map.of("count", 27)));
    }
}
