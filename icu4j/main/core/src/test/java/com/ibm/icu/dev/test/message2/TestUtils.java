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

import org.junit.Ignore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.message2.MFFunctionRegistry;
import com.ibm.icu.message2.MessageFormatter;

/** Utility class, has no test methods. */
@Ignore("Utility class, has no test methods.")
public class TestUtils {
    static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

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

    static void rewriteDates(Map<String, Object> params) {
        // For each value in `params` that's a map with the single key
        // `date` and a double value d,
        // return a map with that value changed to Date(d)
        // In JSON this looks like:
        //    "params": {"exp": { "date": 1722746637000 } }
        for (Map.Entry<String, Object> pair : params.entrySet()) {
            if (pair.getValue() instanceof Map) {
                Map innerMap = (Map) pair.getValue();
                if (innerMap.size() == 1 && innerMap.containsKey("date") && innerMap.get("date") instanceof Double) {
                    Long dateValue = Double.valueOf((Double) innerMap.get("date")).longValue();
                    params.put(pair.getKey(), new Date(dateValue));
                }
            }
        }
    }

    static void rewriteDecimals(Map<String, Object> params) {
        // For each value in `params` that's a map with the single key
        // `decimal` and a string value s
        // return a map with that value changed to Decimal(s)
        // In JSON this looks like:
        //    "params": {"val": {"decimal": "1234567890123456789.987654321"}},
        for (Map.Entry<String, Object> pair : params.entrySet()) {
            if (pair.getValue() instanceof Map) {
                Map innerMap = (Map) pair.getValue();
                if (innerMap.size() == 1 && innerMap.containsKey("decimal")
                    && innerMap.get("decimal") instanceof String) {
                    String decimalValue = (String) innerMap.get("decimal");
                    params.put(pair.getKey(), new com.ibm.icu.math.BigDecimal(decimalValue));
                }
            }
        }
    }


    static boolean expectsErrors(Unit unit) {
        return unit.errors != null && !unit.errors.isEmpty();
    }

    static void runTestCase(Unit unit) {
        runTestCase(unit, null);
    }

    static void runTestCase(Unit unit, Map<String, Object> params) {
        if (unit.ignoreJava != null) {
            return;
        }

        StringBuilder pattern = new StringBuilder();
        if (unit.srcs != null) {
            for (String src : unit.srcs) {
                pattern.append(src);
            }
        } else if (unit.src != null) {
            pattern.append(unit.src);
        }

        // We can call the "complete" constructor with null values, but we want to test that
        // all constructors work properly.
        MessageFormatter.Builder mfBuilder =
                MessageFormatter.builder().setPattern(pattern.toString());
        if (unit.locale != null && !unit.locale.isEmpty()) {
            mfBuilder.setLocale(Locale.forLanguageTag(unit.locale));
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
            String result = mf.formatToString(params);
            if (expectsErrors(unit)) {
                fail(reportCase(unit)
                        + "\nExpected error, but it didn't happen.\n"
                        + "Result: '" + result + "'");
            } else {
                assertEquals(reportCase(unit), unit.exp, result);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            if (!expectsErrors(unit)) {
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
        // First, check the top level of the source directory,
        // in case we're in a source tarball
        Path icuTestdataInSourceDir = filePath.resolve("../../../../../../../../../../../testdata/message2/").normalize();
        Path icuTestdataDir = icuTestdataInSourceDir;
        if (!Files.isDirectory(icuTestdataInSourceDir)) {
            // If that doesn't exist, check one directory higher, in case we're
            // in a checked-out repo
            Path icuTestdataInRepo = Paths.get("../").resolve(icuTestdataInSourceDir).normalize();
            if (!Files.isDirectory(icuTestdataInRepo)) {
                throw new java.io.FileNotFoundException("Test data directory does not exist: tried "
                                                        + icuTestdataInSourceDir + " and "
                                                        + icuTestdataInRepo);
            }
            icuTestdataDir = icuTestdataInSourceDir;
        }
        return icuTestdataDir.resolve(json);
    }}
