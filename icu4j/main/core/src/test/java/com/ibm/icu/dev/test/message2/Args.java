// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.message2.MessageFormatter;

/**
 * Convenience class that provides the same functionality as
 * <code>Map.of</code> introduced in JDK 11, which can't be used yet for ICU4J.
 *
 * <p>The returned Map is immutable, to prove that the {@link MessageFormatter}
 * does not change it</p>
 */
@SuppressWarnings("javadoc")
public class Args {

    public static final Map<String, Object> NONE = new HashMap<>();

    public static Map<String, Object> of(
            String argName0, Object argValue0) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1,
            String argName2, Object argValue2) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        result.put(argName2, argValue2);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1,
            String argName2, Object argValue2,
            String argName3, Object argValue3) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        result.put(argName2, argValue2);
        result.put(argName3, argValue3);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1,
            String argName2, Object argValue2,
            String argName3, Object argValue3,
            String argName4, Object argValue4) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        result.put(argName2, argValue2);
        result.put(argName3, argValue3);
        result.put(argName4, argValue4);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1,
            String argName2, Object argValue2,
            String argName3, Object argValue3,
            String argName4, Object argValue4,
            String argName5, Object argValue5) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        result.put(argName2, argValue2);
        result.put(argName3, argValue3);
        result.put(argName4, argValue4);
        result.put(argName5, argValue5);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1,
            String argName2, Object argValue2,
            String argName3, Object argValue3,
            String argName4, Object argValue4,
            String argName5, Object argValue5,
            String argName6, Object argValue6) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        result.put(argName2, argValue2);
        result.put(argName3, argValue3);
        result.put(argName4, argValue4);
        result.put(argName5, argValue5);
        result.put(argName6, argValue6);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1,
            String argName2, Object argValue2,
            String argName3, Object argValue3,
            String argName4, Object argValue4,
            String argName5, Object argValue5,
            String argName6, Object argValue6,
            String argName7, Object argValue7) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        result.put(argName2, argValue2);
        result.put(argName3, argValue3);
        result.put(argName4, argValue4);
        result.put(argName5, argValue5);
        result.put(argName6, argValue6);
        result.put(argName7, argValue7);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1,
            String argName2, Object argValue2,
            String argName3, Object argValue3,
            String argName4, Object argValue4,
            String argName5, Object argValue5,
            String argName6, Object argValue6,
            String argName7, Object argValue7,
            String argName8, Object argValue8) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        result.put(argName2, argValue2);
        result.put(argName3, argValue3);
        result.put(argName4, argValue4);
        result.put(argName5, argValue5);
        result.put(argName6, argValue6);
        result.put(argName7, argValue7);
        result.put(argName8, argValue8);
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> of(
            String argName0, Object argValue0,
            String argName1, Object argValue1,
            String argName2, Object argValue2,
            String argName3, Object argValue3,
            String argName4, Object argValue4,
            String argName5, Object argValue5,
            String argName6, Object argValue6,
            String argName7, Object argValue7,
            String argName8, Object argValue8,
            String argName9, Object argValue9) {
        Map<String, Object> result = new HashMap<>();
        result.put(argName0, argValue0);
        result.put(argName1, argValue1);
        result.put(argName2, argValue2);
        result.put(argName3, argValue3);
        result.put(argName4, argValue4);
        result.put(argName5, argValue5);
        result.put(argName6, argValue6);
        result.put(argName7, argValue7);
        result.put(argName8, argValue8);
        result.put(argName9, argValue9);
        return Collections.unmodifiableMap(result);
    }
}
