// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Map;

class OptUtils {
    private OptUtils() {}

    static Number asNumber(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof CharSequence) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                /* just ignore, we want to try more */
            }
        }
        return null;
    }

    static Integer getInteger(Map<String, Object> options, String key) {
        Object value = options.get(key);
        if (value == null) {
            return null;
        }
        Number nrValue = asNumber(value);
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
}
