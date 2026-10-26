// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.message2.FormattedPlaceholder;
import com.ibm.icu.message2.Function;
import com.ibm.icu.message2.FunctionFactory;
import com.ibm.icu.message2.MFFunctionRegistry;
import com.ibm.icu.message2.PlainStringFormattedValue;
import com.ibm.icu.util.CurrencyAmount;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Showing that a custom function shadows the implementation of the standard functions. */
@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class OverideStandardFunctionsTest extends CoreTestFmwk {

    /*
     * A custom function for formatting and selection to test that standard functions are
     * "shadowed" by custom ones.
     * This function always formats numbers to 42, and selects the first option in a .match.
     */
    private static class CustomNumberFunctionFactory implements FunctionFactory {
        @Override
        public Function create(Locale locale, Map<String, ?> fixedOptions) {
            return new CustomNumberFunctionImpl();
        }

        static class CustomNumberFunctionImpl implements Function {
            @Override
            public String formatToString(Object toFormat, Map<String, ?> variableOptions) {
                return format(toFormat, variableOptions).toString();
            }

            @Override
            public FormattedPlaceholder format(Object toFormat, Map<String, ?> variableOptions) {
                return new FormattedPlaceholder(toFormat, new PlainStringFormattedValue("42"));
            }

            @Override
            // The first key will always match, regardless of value
            public List<String> matches(
                    Object value, List<String> keys, Map<String, ?> variableOptions) {
                if (keys == null || keys.size() < 2) {
                    return List.of();
                }
                return List.of(keys.get(1));
            }
        }
    }

    private static final MFFunctionRegistry CUSTOM_FUNCTION_REGISTRY =
            MFFunctionRegistry.builder()
                    // "Hides" the standard implementation of the standard :number function
                    .setFunction("number", new CustomNumberFunctionFactory())
                    // Changes the default function for `CurrencyAmount` from the standard :currency
                    // to my new :number function, not the standard one.
                    .setDefaultFunctionNameForType(CurrencyAmount.class, "number")
                    .build();

    @Test
    public void testCustomFunctionFormat() {
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("The answer is {$count :number}!")
                        .arguments(Map.of("count", 123))
                        .expected("The answer is 42!")
                        .build());
        // Shows that the standard implementation is still there, when not masked with a registry
        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern("The answer is {$count :number}!")
                        .arguments(Map.of("count", 123))
                        .expected("The answer is 123!")
                        .build());
    }

    @Test
    public void testCustomFunctionFormatForType() {
        // Determine the function based on the argument type.
        // Check that all types that map to :number are now handled by the override.
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("The answer is {$count}!")
                        .arguments(Map.of("count", 123)) // int
                        .expected("The answer is 42!")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("The answer is {$count}!")
                        .arguments(Map.of("count", 123.456)) // double
                        .expected("The answer is 42!")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("The answer is {$count}!")
                        .arguments(Map.of("count", BigDecimal.valueOf(123)))
                        .expected("The answer is 42!")
                        .build());
        // The function is determined by the type of the argument.
        CurrencyAmount amount = new CurrencyAmount(123, Currency.getInstance("USD"));
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("The answer is {$count}!")
                        .arguments(Map.of("count", amount))
                        .expected("The answer is 42!")
                        .build());
        // Shows that the standard implementation is still there, when not masked with a registry
        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern("The answer is {$count}!")
                        .arguments(Map.of("count", amount))
                        .expected("The answer is USD 123.00!")
                        .build());
    }

    @Test
    public void testCustomFunctionSelection() {
        String message =
                ""
                        + ".input {$guestCount :number}\n"
                        + ".match $guestCount\n"
                        + "  * {{There are {$guestCount} guests at this party.}}\n"
                        + " 77 {{A weird party indeed, with {$guestCount} guests!}}\n"
                        + "  0 {{There are no guests at this party.}}\n"
                        + "  1 {{There is {$guestCount} guest at this party.}}\n";

        // The second message is always selected, and the value is formatted to "42"
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(Map.of("guestCount", 0))
                        .expected("A weird party indeed, with 42 guests!")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(Map.of("guestCount", 1))
                        .expected("A weird party indeed, with 42 guests!")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(Map.of("guestCount", 3))
                        .expected("A weird party indeed, with 42 guests!")
                        .build());

        // Check that the standard implementation works as before without a custom registry
        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(Map.of("guestCount", 0))
                        .expected("There are no guests at this party.")
                        .build());
        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(Map.of("guestCount", 1))
                        .expected("There is 1 guest at this party.")
                        .build());
        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(Map.of("guestCount", 3))
                        .expected("There are 3 guests at this party.")
                        .build());
    }
}
