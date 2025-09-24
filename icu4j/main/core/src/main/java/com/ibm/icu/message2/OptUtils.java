// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class OptUtils {
    // This matches JSON number (https://www.rfc-editor.org/rfc/rfc8259#section-6)
    //
    // WARNING: this is not in the MF2 spec/message.abnf anymore.
    // Any concept of "number literal" was removed from there. 
    //
    // This is used to validate options and arguments, for example `maxDigits=|1.|`,
    // or `{|01| :number}` and by the time it gets to the checking the string literal was extracted
    // by the parser and we only see "1." and "01".
    private static final Pattern RE_NUMBER_LITERAL =
            Pattern.compile("^-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+\\-]?[0-9]+)?$");

    private OptUtils() {}

    static Number asNumber(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof CharSequence) {
            try {
                Matcher m = RE_NUMBER_LITERAL.matcher(value.toString());
                if (m.find()) {
                    return Double.parseDouble(value.toString());
                }
            } catch (NumberFormatException e) {
                /* just ignore, we continue and report */
            }
        }
        return null;
    }

    static Number asNumber(boolean reportErrors, String keyName, Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof CharSequence) {
            try {
                Matcher m = RE_NUMBER_LITERAL.matcher(value.toString());
                if (m.find()) {
                    return Double.parseDouble(value.toString());
                }
            } catch (NumberFormatException e) {
                /* just ignore, we continue and report */
            }
        }
        if (reportErrors) {
            throw new IllegalArgumentException("bad-operand: " + keyName + " must be numeric");
        }
        return null;
    }

    static Integer getInteger(Map<String, Object> options, boolean reportErrors, String key) {
        Object value = options.get(key);
        if (value == null) {
            return null;
        }
        Number nrValue = asNumber(reportErrors, key, value);
        if (nrValue != null) {
            return nrValue.intValue();
        }
        return null;
    }

    static String getString(Map<String, Object> options, String key, String defaultVal) {
        Object value = options.get(key);
        if (value instanceof CharSequence) {
            return value.toString();
        }
        return defaultVal;
    }

    static String getString(Map<String, Object> options, String key) {
        return getString(options, key, null);
    }

    static boolean reportErrors(Map<String, Object> options) {
        String reportErrors = getString(options, "icu:impl:errorPolicy");
        return Objects.equals(reportErrors, "STRICT");
    }

    static boolean reportErrors(Map<String, Object> fixedOptions, Map<String, Object> variableOptions) {
        return reportErrors(fixedOptions) || reportErrors(variableOptions);
    }

    static Locale getBestLocale(Map<String, Object> options, Locale defaultValue) {
        Locale result = null;
        String localeOverride = getString(options, "u:locale");
        if (localeOverride != null) {
            try {
                result = Locale.forLanguageTag(localeOverride.replace('_', '-'));
            } catch (Exception e) {
                if (reportErrors(options)) {
                    throw new IllegalArgumentException("bad-operand: u:locale must be a valid BCP 47 language tag");
                }
            }
        }
        if (result == null) {
            if (defaultValue == null) {
                result = Locale.getDefault();
            } else {
                result = defaultValue;
            }
        }
        return result;
    }

    static Directionality getBestDirectionality(Map<String, Object> options, Locale locale) {
        Directionality result = getDirectionality(options);
        return result == Directionality.UNKNOWN ? Directionality.of(locale) : result;
    }

    static Directionality getDirectionality(Map<String, Object> options) {
        String value = getString(options, "u:dir");
        if (value == null) {
            return Directionality.UNKNOWN;
        }
        Directionality result;
        switch (value) {
            case "rtl":
                result = Directionality.RTL;
                break;
            case "ltr":
                result = Directionality.LTR;
                break;
            case "auto":
                result = Directionality.AUTO;
                break;
            case "inherit":
                result = Directionality.INHERIT;
                break;
            default:
                result = Directionality.UNKNOWN;
                break;
        }
        return result;
    }

    static String getUId(Map<String, Object> options) {
        return getString(options, "u:id");
    }
}
