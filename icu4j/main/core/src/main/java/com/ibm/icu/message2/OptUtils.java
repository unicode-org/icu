// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Map;

class OptUtils {
    private OptUtils() {}

    static Number asNumber(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof CharSequence) {
            String strValue = value.toString();
            try {
                return Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
            }
            try {
                return Integer.decode(strValue);
            } catch (NumberFormatException e) {
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

    static String getString(Map<String, Object> options, String key) {
        Object value = options.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        return null;
    }

}
