// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Ignore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.message2.MFFunctionRegistry;
import com.ibm.icu.message2.MessageFormatter;

/** Utility class, has no test methods. */
@Ignore("Utility class, has no test methods.")
public class TestUtils {

    static final Gson GSON = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .registerTypeAdapter(Sources.class, new StringToListAdapter())
        .create();

    // ======= Legacy TestCase utilities, no json-compatible ========

    static void runTestCase(TestCase testCase) {
        runTestCase(null, testCase);
    }

    static void runTestCase(MFFunctionRegistry customFunctionsRegistry, TestCase testCase) {
        if (testCase.ignore) {
            return;
        }

        // We can call the "complete" constructor with null values, but we want to test that
        // all constructors work properly.
        MessageFormatter.Builder mfBuilder = MessageFormatter.builder()
                .setPattern(testCase.message)
                .setLocale(testCase.locale);
        if (customFunctionsRegistry != null) {
            mfBuilder.setFunctionRegistry(customFunctionsRegistry);
        }
        try { // TODO: expected error
            MessageFormatter mf = mfBuilder.build();
            String result = mf.formatToString(testCase.arguments);
            if (!testCase.errors.isEmpty()) {
                fail(reportCase(testCase) + "\nExpected error, but it didn't happen.\n"
                        + "Result: '" + result + "'");
            } else {
                assertEquals(reportCase(testCase), testCase.expected, result);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            if (testCase.errors.isEmpty()) {
                fail(reportCase(testCase) + "\nNo error was expected here, but it happened:\n"
                        + e.getMessage());
            }
        }
    }

    private static String reportCase(TestCase testCase) {
        return testCase.toString();
    }

    // ======= Same functionality with Unit, usable with JSON ========

    static void rewriteDates(Param[] params) {
        // For each value in `params` that's a map with the single key
        // `date` and a double value d,
        // return a map with that value changed to Date(d)
        // In JSON this looks like:
        //    "params": [{"name": "exp"}, { "value": { "date": 1722746637000 } }]
        for (int i = 0; i < params.length; i++) {
            Param pair = params[i];
            if (pair.value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> innerMap = (Map<String, Object>) pair.value;
                if (innerMap.size() == 1 && innerMap.containsKey("date") && innerMap.get("date") instanceof Double) {
                    Long dateValue = Double.valueOf((Double) innerMap.get("date")).longValue();
                    params[i] = new Param(pair.name, new Date(dateValue));
                }
            }
        }
    }

    static void rewriteDecimals(Param[] params) {
        // For each value in `params` that's a map with the single key
        // `decimal` and a string value s
        // return a map with that value changed to Decimal(s)
        // In JSON this looks like:
        //    "params": [{"name": "val"}, {"value": {"decimal": "1234567890123456789.987654321"}}]
        for (int i = 0; i < params.length; i++) {
            Param pair = params[i];
            if (pair.value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> innerMap = (Map<String, Object>) pair.value;
                if (innerMap.size() == 1 && innerMap.containsKey("decimal")
                    && innerMap.get("decimal") instanceof String) {
                    String decimalValue = (String) innerMap.get("decimal");
                    params[i] = new Param(pair.name, new com.ibm.icu.math.BigDecimal(decimalValue));
                }
            }
        }
    }

    static Map<String, Object> paramsToMap(Param[] params) {
        if (params == null) {
            return null;
        }
        TreeMap<String, Object> result = new TreeMap<String, Object>();
        for (Param pair : params) {
            result.put(pair.name, pair.value);
        }
        return result;
    }

    static boolean expectsErrors(DefaultTestProperties defaults, Unit unit) {
        return (unit.expErrors != null && !unit.expErrors.isEmpty())
            || (defaults.getExpErrors().length > 0);
    }

    static void runTestCase(DefaultTestProperties defaults, Unit unit) {
        runTestCase(defaults, unit, null);
    }

    static void runTestCase(DefaultTestProperties defaults, Unit unit, Param[] params) {
        if (unit.ignoreJava != null) {
            return;
        }

        StringBuilder pattern = new StringBuilder();
        if (unit.src != null) {
            for (String src : unit.src.sources) {
                pattern.append(src);
            }
        }

        // We can call the "complete" constructor with null values, but we want to test that
        // all constructors work properly.
        MessageFormatter.Builder mfBuilder =
                MessageFormatter.builder().setPattern(pattern.toString());
        if (unit.locale != null && !unit.locale.isEmpty()) {
            mfBuilder.setLocale(Locale.forLanguageTag(unit.locale));
        } else if (defaults.getLocale() != null) {
            mfBuilder.setLocale(Locale.forLanguageTag(defaults.getLocale()));
        } else {
            mfBuilder.setLocale(Locale.US);
        }

        try {
            MessageFormatter mf = mfBuilder.build();
            if (unit.params != null) {
                params = unit.params;
                rewriteDates(params);
                rewriteDecimals(params);
            }
            String result = mf.formatToString(paramsToMap(params));
            if (expectsErrors(defaults, unit)) {
                fail(reportCase(unit)
                        + "\nExpected error, but it didn't happen.\n"
                        + "Result: '" + result + "'");
            } else {
                if (unit.exp != null) {
                    assertEquals(reportCase(unit), unit.exp, result);
                }
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            if (!expectsErrors(defaults, unit)) {
                fail(reportCase(unit)
                        + "\nNo error was expected here, but it happened:\n"
                        + e.getMessage());
            }
        }
    }

    private static String reportCase(Unit unit) {
        return unit.toString();
    }

    static Reader jsonReader(String jsonFileName) throws URISyntaxException, IOException {
        Path json = getTestFile(TestUtils.class, jsonFileName);
        return Files.newBufferedReader(json, StandardCharsets.UTF_8);
    }

    private static Path getTestFile(Class<?> cls, String fileName) throws URISyntaxException, IOException {
        String packageName = cls.getPackage().getName().replace('.', '/');
        URI getPath = cls.getClassLoader().getResource(packageName).toURI();
        Path filePath = Paths.get(getPath);
        Path json = Paths.get(fileName);
        return filePath.resolve(json);
    }}
