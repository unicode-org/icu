// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.message2.FormattedPlaceholder;
import com.ibm.icu.message2.Formatter;
import com.ibm.icu.message2.FormatterFactory;
import com.ibm.icu.message2.MFDataModel.CatchallKey;
import com.ibm.icu.message2.PlainStringFormattedValue;
import com.ibm.icu.message2.Selector;
import com.ibm.icu.message2.SelectorFactory;
import com.ibm.icu.text.FormattedValue;

/**
 * Locale-independent functions for formatting and selection.
 * Implements the functionality required by `:test:function`, `:test:format`, and `:test:select`.  
 * Used only for testing (see test/README.md in the MF2 repository).
 */
public class TestFunctionFactory implements FormatterFactory, SelectorFactory {
    private final String kind;

    public TestFunctionFactory(String kind) {
        this.kind = kind;
    }

    @Override
    public Formatter createFormatter(Locale locale, Map<String, Object> fixedOptions) {
        return new TestFormatterImpl(kind, fixedOptions);
    }

    @Override
    public Selector createSelector(Locale locale, Map<String, Object> fixedOptions) {
        return new TestSelectorImpl(kind, fixedOptions);
    }

    private static class TestFormatterImpl implements Formatter {
        private final String kind;
        private final ParsedOptions parsedOptions;

        public TestFormatterImpl(String kind, Map<String, Object> fixedOptions) {
            this.kind = kind;
            this.parsedOptions = ParsedOptions.of(fixedOptions);
        }

        @Override
        public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
            if (!"select".equals(kind) && parsedOptions.failsFormat) {
                throw new InvalidParameterException("ALWAYS FAIL");
            }
            return format(toFormat, variableOptions).toString();
        }

        @Override
        public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
            return TestFunctionFactory.formatImpl(toFormat, parsedOptions);
        }
    }

    private static class TestSelectorImpl implements Selector {
        private static final String NO_MATCH = "\uFFFDNO_MATCH\uFFFE"; // Unlikely to show in a key
        private final String kind;
        private final ParsedOptions parsedOptions;

        public TestSelectorImpl(String kind, Map<String, Object> fixedOptions) {
            this.kind = kind;
            this.parsedOptions = ParsedOptions.of(fixedOptions);
        }

        @Override
        public List<String> matches(Object value, List<String> keys, Map<String, Object> variableOptions) {
//            ParsedOptions parsedOptions = ParsedOptions.of(variableOptions);
            if (parsedOptions.failsSelect) {
                throw new InvalidParameterException("Expected the test to always fail.");
            }

            FormattedPlaceholder foo = TestFunctionFactory.formatImpl(value, parsedOptions);
            List<String> result = new ArrayList<>();
            for (String key : keys) {
                if (CatchallKey.isCatchAll(key) || key.equals(foo.getFormattedValue().toString())) {
                    result.add(key);
                } else {
                    result.add(NO_MATCH);
                }
            }

            result.sort(TestSelectorImpl::testComparator);
            return result;
        }

        private static int testComparator(String o1, String o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            if (NO_MATCH.equals(o1)) {
                return 1;
            }
            if (NO_MATCH.equals(o2)) {
                return -1;
            }
            // * sorts last
            if ("*".equals(o1)) {
                return 1;
            }
            if ("*".equals(o2)) {
                return -1;
            }
            // At this point they are both strings
            // We should never get here, so the order does not really matter
            return o1.compareTo(o2);
        }
    }

    private static class ParsedOptions {
        final boolean reportErrors;
        final boolean failsFormat;
        final boolean failsSelect;
        final int decimalPlaces;

        ParsedOptions(boolean reportErrors, boolean failsFormat, boolean failsSelect, int decimalPlaces) {
            this.failsFormat = failsFormat;
            this.failsSelect = failsSelect;
            this.decimalPlaces = decimalPlaces;
            this.reportErrors = reportErrors;
        }

        static ParsedOptions of(Map<String, Object> options) {
            boolean reportErrors = false;
            // fail = "never" (default)
            boolean failsFormat = false;
            boolean failsSelect = false;
            int decimalPlaces = 0;
            if (options == null) {
                return new ParsedOptions(reportErrors, failsFormat, failsSelect, decimalPlaces);
            }

            String option = getStringOption(options, "icu:impl:errorPolicy", null);
            reportErrors= "STRICT".equals(option);

            option = getStringOption(options, "fails", "never");
            if (option == null) {
                System.out.println("WTF?");
            }
            switch (option) {
                case "never":
                    // both options are already set to false, all good
                    break;
                case "select":
                    failsSelect = true;
                    break;
                case "format":
                    failsFormat = true;
                    break;
                case "always":
                    failsFormat = true;
                    failsSelect = true;
                    break;
                default:
                    // "All other _options_ and their values are ignored." (spec, `test/README.md`)
                    // 1. Emit "bad-option" _Resolution Error_.
                    if (reportErrors) {
                        throw new InvalidParameterException("bad-option-value");
                    }
            }
            option = getStringOption(options, "decimalPlaces", "0");
            switch (option) {
                case "0":
                    decimalPlaces = 0;
                    break;
                case "1":
                    decimalPlaces = 1;
                    break;
                default:
                    // "All other _options_ and their values are ignored." (spec, `test/README.md`)
                    // 1. Emit "bad-option" _Resolution Error_.
                    // 1. Use a _fallback value_ as the _resolved value_ of the _expression_.
                    if (reportErrors) {
                        throw new InvalidParameterException("bad-option");
                    }
            }
            return new ParsedOptions(reportErrors, failsFormat, failsSelect, decimalPlaces);
        }
    }

    private static String getStringOption(Map<String, Object> options, String key, String defaultVal) {
        if (options == null) {
            return defaultVal;
        }
        Object value = options.get(key);
        if (value == null) {
            return defaultVal;
        }
        return value.toString();
    }

    static FormattedPlaceholder formatImpl(Object toFormat, ParsedOptions parsedOptions) {
        FormattedValue result = null;
        Double dblToFormat = null;
        if (toFormat == null) {
            // This is also what MessageFormat does.
            throw new NullPointerException("Argument to format can't be null");
        } else if (toFormat instanceof Double) {
            dblToFormat = (Double) toFormat;
        } else if (toFormat instanceof CharSequence) {
            try {
                dblToFormat = Double.valueOf(toFormat.toString());
            } catch (NumberFormatException e) {
                if (parsedOptions.reportErrors) {
                    throw new IllegalArgumentException("bad-operand: argument must be numeric");
                }
            }
        }

        if (parsedOptions.failsFormat) {
            if (parsedOptions.reportErrors) {
                throw new InvalidParameterException("Expected the test to always fail.");
            }
        }
        if (dblToFormat == null) {
            if (parsedOptions.reportErrors) {
                throw new NullPointerException("unresolved-variable: argument to format can't be null");
            }
            result = new PlainStringFormattedValue("{|" + toFormat + "|}");
        } else {
            StringBuffer buffer = new StringBuffer();
            if (dblToFormat < 0) {
                // 1. If `Input` is less than 0, the character `-` U+002D Hyphen-Minus.
                buffer.append('-');
                dblToFormat = -dblToFormat;
            }
            // 1. The truncated absolute integer value of `Input`, i.e. floor(abs(`Input`)),
            //    formatted as a sequence of decimal digit characters (U+0030...U+0039).
            buffer.append(dblToFormat.intValue());
            if (parsedOptions.decimalPlaces == 1) {
                // 1. If `DecimalPlaces` is 1, then
                //   1. The character `.` U+002E Full Stop.
                buffer.append('.');
                //   1. The single decimal digit character representing the value floor((abs(`Input`) - floor(abs(`Input`))) \* 10)
                buffer.append((int) ((dblToFormat - dblToFormat.intValue()) * 10));
            }
            result = new PlainStringFormattedValue(buffer.toString());
        }

        return new FormattedPlaceholder(toFormat, result);
    }
}
