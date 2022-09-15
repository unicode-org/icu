// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.icu.message2.Mf2DataModel.Expression;
import com.ibm.icu.message2.Mf2DataModel.Part;
import com.ibm.icu.message2.Mf2DataModel.Pattern;
import com.ibm.icu.message2.Mf2DataModel.SelectorKeys;
import com.ibm.icu.message2.Mf2DataModel.Text;
import com.ibm.icu.message2.Mf2DataModel.Value;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CurrencyAmount;

/**
 * Takes an {@link Mf2DataModel} and formats it to a {@link String}
 * (and later on we will also implement formatting to a {@code FormattedMessage}).
 */
// TODO: move this in the MessageFormatter
class Mf2DataModelFormatter {
    private final Locale locale;
    private final Mf2DataModel dm;

    final Mf2FunctionRegistry standardFunctions;
    final Mf2FunctionRegistry customFunctions;
    private static final Mf2FunctionRegistry EMPTY_REGISTY = Mf2FunctionRegistry.builder().build();

    Mf2DataModelFormatter(Mf2DataModel dm, Locale locale, Mf2FunctionRegistry customFunctionRegistry) {
        this.locale = locale;
        this.dm = dm;
        this.customFunctions = customFunctionRegistry == null ? EMPTY_REGISTY : customFunctionRegistry;

        standardFunctions = Mf2FunctionRegistry.builder()
                // Date/time formatting
                .setFormatter("datetime", new DateTimeFormatterFactory())
                .setDefaultFormatterNameForType(Date.class, "datetime")
                .setDefaultFormatterNameForType(Calendar.class, "datetime")

                // Number formatting
                .setFormatter("number", new NumberFormatterFactory())
                .setDefaultFormatterNameForType(Integer.class, "number")
                .setDefaultFormatterNameForType(Double.class, "number")
                .setDefaultFormatterNameForType(Number.class, "number")
                .setDefaultFormatterNameForType(CurrencyAmount.class, "number")

                // Format that returns "to string"
                .setFormatter("identity", new IdentityFormatterFactory())
                .setDefaultFormatterNameForType(String.class, "identity")
                .setDefaultFormatterNameForType(CharSequence.class, "identity")

                // Register the standard selectors
                .setSelector("plural", new PluralSelectorFactory("cardinal"))
                .setSelector("selectordinal", new PluralSelectorFactory("ordinal"))
                .setSelector("select", new TextSelectorFactory())
                .setSelector("gender", new TextSelectorFactory())

                .build();
    }

    private static Map<String, Object> mf2OptToFixedOptions(Map<String, Value> options) {
        Map<String, Object> result = new HashMap<>();
        for (Entry<String, Value> option : options.entrySet()) {
            Value value = option.getValue();
            if (value.isLiteral()) {
                result.put(option.getKey(), value.getLiteral());
            }
        }
        return result;
    }

    private Map<String, Object> mf2OptToVariableOptions(Map<String, Value> options, Map<String, Object> arguments) {
        Map<String, Object> result = new HashMap<>();
        for (Entry<String, Value> option : options.entrySet()) {
            Value value = option.getValue();
            if (value.isVariable()) {
                result.put(option.getKey(), variableToObjectEx(value, arguments));
            }
        }
        return result;
    }

    FormatterFactory getFormattingFunctionFactoryByName(Object toFormat, String functionName) {
        // Get a function name from the type of the object to format
        if (functionName == null || functionName.isEmpty()) {
            if (toFormat == null) {
                // The object to format is null, and no function provided.
                return null;
            }
            Class<?> clazz = toFormat.getClass();
            functionName = standardFunctions.getDefaultFormatterNameForType(clazz);
            if (functionName == null) {
                functionName = customFunctions.getDefaultFormatterNameForType(clazz);
            }
            if (functionName == null) {
                throw new IllegalArgumentException("Object to format without a function, and unknown type: "
                        + toFormat.getClass().getName());
            }
        }

        FormatterFactory func = standardFunctions.getFormatter(functionName);
        if (func == null) {
            func = customFunctions.getFormatter(functionName);
            if (func == null) {
                throw new IllegalArgumentException("Can't find an implementation for function: '"
                        + functionName + "'");
            }
        }
        return func;
    }

    String format(Map<String, Object> arguments) {
        List<Expression> selectors = dm.getSelectors();
        Pattern patternToRender = selectors.isEmpty()
                ? dm.getPattern()
                : findBestMatchingPattern(selectors, arguments);

        StringBuilder result = new StringBuilder();
        for (Part part : patternToRender.getParts()) {
            if (part instanceof Text) {
                result.append(part);
            } else if (part instanceof Expression) { // Placeholder is an Expression
                FormattedPlaceholder fp = formatPlaceholder((Expression) part, arguments, false);
                result.append(fp.toString());
            } else {
                throw new IllegalArgumentException("Unknown part type: " + part);
            }
        }
        return result.toString();
    }

    private Pattern findBestMatchingPattern(List<Expression> selectors, Map<String, Object> arguments) {
        Pattern patternToRender = null;

        // Collect all the selector functions in an array, to reuse
        List<Selector> selectorFunctions = new ArrayList<>(selectors.size());
        for (Expression selector : selectors) {
            String functionName = selector.getFunctionName();
            SelectorFactory funcFactory = standardFunctions.getSelector(functionName);
            if (funcFactory == null) {
                funcFactory = customFunctions.getSelector(functionName);
            }
            if (funcFactory != null) {
                Map<String, Object> opt = mf2OptToFixedOptions(selector.getOptions());
                selectorFunctions.add(funcFactory.createSelector(locale, opt));
            } else {
                throw new IllegalArgumentException("Unknown selector type: " + functionName);
            }
        }
        // This should not be possible, we added one function for each selector, or we have thrown an exception.
        // But just in case someone removes the throw above?
        if (selectorFunctions.size() != selectors.size()) {
            throw new IllegalArgumentException("Something went wrong, not enough selector functions, "
                    + selectorFunctions.size() + " vs. " + selectors.size());
        }

        // Iterate "vertically", through all variants
        for (Entry<SelectorKeys, Pattern> variant : dm.getVariants().entrySet()) {
            int maxCount = selectors.size();
            List<String> keysToCheck = variant.getKey().getKeys();
            if (selectors.size() != keysToCheck.size()) {
                throw new IllegalArgumentException("Mismatch between the number of selectors and the number of keys: "
                        + selectors.size() + " vs. " + keysToCheck.size());
            }
            boolean matches = true;
            // Iterate "horizontally", through all matching functions and keys
            for (int i = 0; i < maxCount; i++) {
                Expression selector = selectors.get(i);
                String valToCheck = keysToCheck.get(i);
                Selector func = selectorFunctions.get(i);
                Map<String, Object> options = mf2OptToVariableOptions(selector.getOptions(), arguments);
                if (!func.matches(variableToObjectEx(selector.getOperand(), arguments), valToCheck, options)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                patternToRender = variant.getValue();
                break;
            }
        }

        // TODO: check that there was an entry with all the keys set to `*`
        // And should do that only once, when building the data model.
        if (patternToRender == null) {
            // If there was a case with all entries in the keys `*` this should not happen
            throw new IllegalArgumentException("The selection went wrong, cannot select any option.");
        }

        return patternToRender;
    }

    /*
     * Pass a level to prevent local variables calling each-other recursively:
     *
     * <code><pre>
     * let $l1 = {$l4 :number}
     * let $l2 = {$l1 :number}
     * let $l3 = {$l2 :number}
     * let $l4 = {$l3 :number}
     * </pre></code>
     *
     * We can keep track of the calls (complicated and expensive).
     * Or we can forbid the use of variables before they are declared, but that is not in the spec (yet?).
     */
    private Object variableToObjectEx(Value value, Map<String, Object> arguments) {
        if (value == null) { // function only
            return null;
        }
        // We have an operand. Can be literal, local var, or argument.
        if (value.isLiteral()) {
            return value.getLiteral();
        } else if (value.isVariable()) {
            String varName = value.getVariableName();
            Expression localPh = dm.getLocalVariables().get(varName);
            if (localPh != null) {
                return formatPlaceholder(localPh, arguments, false);
            }
            return arguments.get(varName);
        } else {
            throw new IllegalArgumentException("Invalid operand type " + value);
        }
    }

    private FormattedPlaceholder formatPlaceholder(Expression ph, Map<String, Object> arguments, boolean localExpression) {
        Object toFormat;
        Value operand = ph.getOperand();
        if (operand == null) { // function only, "...{:currentOs option=value}..."
            toFormat = null;
        } else {
            // We have an operand. Can be literal, local var, or argument.
            if (operand.isLiteral()) { // "...{(1234.56) :number}..."
                // If it is a literal, return the string itself
                toFormat = operand.getLiteral();
            } else if (operand.isVariable()) {
                String varName = operand.getVariableName();
                if (!localExpression) {
                    Expression localPh = dm.getLocalVariables().get(varName);
                    if (localPh != null) {
                        // If it is a local variable, we need to format that (recursive)
                        // TODO: See if there is any danger to eval the local variables only once
                        // (on demand in case the local var is not used, for example in a select)
                        return formatPlaceholder(localPh, arguments, true);
                    }
                }
                // Return the object in the argument bag.
                toFormat = arguments.get(varName);
                // toFormat might still be null here.
            } else {
                throw new IllegalArgumentException("Invalid operand type " + ph.getOperand());
            }
        }

        if (ph.formatter == null) {
            FormatterFactory funcFactory = getFormattingFunctionFactoryByName(toFormat, ph.getFunctionName());
            if (funcFactory != null) {
                Map<String, Object> fixedOptions = mf2OptToFixedOptions(ph.getOptions());
                Formatter ff = funcFactory.createFormatter(locale, fixedOptions);
                ph.formatter = ff;
            }
        }
        if (ph.formatter != null) {
            Map<String, Object> variableOptions = mf2OptToVariableOptions(ph.getOptions(), arguments);
            try {
                return ph.formatter.format(toFormat, variableOptions);
            } catch (IllegalArgumentException e) {
                // Fall-through to the name of the placeholder without replacement.
            }
        }

        return new FormattedPlaceholder(toFormat, new PlainStringFormattedValue("{" + ph.getOperand() + "}"));
    }
}
