// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2015, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.Utility;

/**
 * A collection of methods to run the data driven number format test suite.
 */
public class DataDrivenNumberFormatTestUtility extends TestFmwk {

    /**
     * Base class for code under test.
     */
    public static abstract class CodeUnderTest {

        /**
         * Returns the ID of the code under test. This ID is used to identify
         * tests that are known to fail for this particular code under test.
         * This implementation returns null which means that by default all
         * tests should work with this code under test.
         * @return 'J' means ICU4J, 'K' means JDK
         */
        public Character Id() {
            return null;
        }

        /**
         *  Runs a single formatting test. On success, returns null.
         *  On failure, returns the error. This implementation just returns null.
         *  Subclasses should override.
         *  @param tuple contains the parameters of the format test.
         */
        public String format(DataDrivenNumberFormatTestData tuple) {
            if (tuple.output != null && tuple.output.equals("fail")) return "fail";
            return null;
        }

        /**
         *  Runs a single toPattern test. On success, returns null.
         *  On failure, returns the error. This implementation just returns null.
         *  Subclasses should override.
         *  @param tuple contains the parameters of the format test.
         */
        public String toPattern(DataDrivenNumberFormatTestData tuple) {
            if (tuple.output != null && tuple.output.equals("fail")) return "fail";
            return null;
        }

        /**
         *  Runs a single parse test. On success, returns null.
         *  On failure, returns the error. This implementation just returns null.
         *  Subclasses should override.
         *  @param tuple contains the parameters of the format test.
         */
        public String parse(DataDrivenNumberFormatTestData tuple) {
            if (tuple.output != null && tuple.output.equals("fail")) return "fail";
            return null;
        }

        /**
         *  Runs a single parse currency test. On success, returns null.
         *  On failure, returns the error. This implementation just returns null.
         *  Subclasses should override.
         *  @param tuple contains the parameters of the format test.
         */
        public String parseCurrency(DataDrivenNumberFormatTestData tuple) {
            if (tuple.output != null && tuple.output.equals("fail")) return "fail";
            return null;
        }

        /**
         * Runs a single select test. On success, returns null.
         *  On failure, returns the error. This implementation just returns null.
         *  Subclasses should override.
         * @param tuple contains the parameters of the format test.
         */
        public String select(DataDrivenNumberFormatTestData tuple) {
            if (tuple.output != null && tuple.output.equals("fail")) return "fail";
            return null;
        }
    }

    private static enum RunMode {
        SKIP_KNOWN_FAILURES,
        INCLUDE_KNOWN_FAILURES,
        CHECK_FOR_UNKNOWN_IDS
    }

    private final CodeUnderTest codeUnderTest;
    private final String allowedIDs;
    private String fileLine = null;
    private int fileLineNumber = 0;
    private String fileTestName = "";
    private DataDrivenNumberFormatTestData tuple = new DataDrivenNumberFormatTestData();

    /**
     * Runs all the tests in the data driven test suite against codeUnderTest.
     * @param fileName The name of the test file. A relative file name under
     *   com/ibm/icu/dev/data such as "data.txt"
     * @param codeUnderTest the code under test
     */

    public static void runSuite(
            String fileName, CodeUnderTest codeUnderTest) {
        new DataDrivenNumberFormatTestUtility(codeUnderTest)
                .run(fileName, RunMode.SKIP_KNOWN_FAILURES);
    }

    /**
     * Runs every format test in data driven test suite including those
     * that are known to fail.  If a test is supposed to fail but actually
     * passes, an error is printed.
     *
     * @param fileName The name of the test file. A relative file name under
     *   com/ibm/icu/dev/data such as "data.txt"
     * @param codeUnderTest the code under test
     */
    public static void runFormatSuiteIncludingKnownFailures(
            String fileName, CodeUnderTest codeUnderTest) {
        new DataDrivenNumberFormatTestUtility(codeUnderTest)
                .run(fileName, RunMode.INCLUDE_KNOWN_FAILURES);
    }

    /**
     * Checks the data file for no unknown IDs in "breaks" columns.
     */
    public static void checkNoUnknownIDs(String fileName, String allowedIDs) {
        new DataDrivenNumberFormatTestUtility(allowedIDs).run(fileName, RunMode.CHECK_FOR_UNKNOWN_IDS);
    }

    private DataDrivenNumberFormatTestUtility(CodeUnderTest codeUnderTest) {
        this.codeUnderTest = codeUnderTest;
        this.allowedIDs = null;
    }

    private DataDrivenNumberFormatTestUtility(String allowedIDs) {
        this.codeUnderTest = null;
        this.allowedIDs = allowedIDs;
    }

    private void run(String fileName, RunMode runMode) {
        char codeUnderTestId = (codeUnderTest == null) ? 0 : codeUnderTest.Id();
        BufferedReader in = null;
        try {
            in = TestUtil.getDataReader("numberformattestspecification.txt", "UTF-8");
            // read first line and remove BOM if present
            readLine(in);
            if (fileLine != null && fileLine.charAt(0) == '\uFEFF') {
                fileLine = fileLine.substring(1);
            }

            int state = 0;
            List<String> columnValues;
            List<String> columnNames = null;
            while (true) {
                if (fileLine == null || fileLine.length() == 0) {
                    if (!readLine(in)) {
                        break;
                    }
                    if (fileLine.length() == 0 && state == 2) {
                        state = 0;
                    }
                    continue;
                }
                if (fileLine.startsWith("//")) {
                    fileLine = null;
                    continue;
                }
                // Initial setup of test.
                if (state == 0) {
                    if (fileLine.startsWith("test ")) {
                        fileTestName = fileLine;
                        tuple = new DataDrivenNumberFormatTestData();
                    } else if (fileLine.startsWith("set ")) {
                        if (!setTupleField()) {
                            return;
                        }
                    } else if(fileLine.startsWith("begin")) {
                        state = 1;
                    } else {
                        showError("Unrecognized verb.");
                        return;
                    }
                // column specification
                } else if (state == 1) {
                    columnNames = splitBy((char) 0x09);
                    state = 2;
                    // "breaks" column must be last!
                    if (columnNames.contains("breaks")
                            && columnNames.indexOf("breaks") != columnNames.size() - 1) {
                        showError("'breaks' column must be last!");
                    }
                // run the tests
                } else {
                    int columnNamesSize = columnNames.size();
                    columnValues = splitBy(columnNamesSize, (char) 0x09);
                    int columnValuesSize = columnValues.size();
                    for (int i = 0; i < columnValuesSize; ++i) {
                        if (!setField(columnNames.get(i), columnValues.get(i))) {
                            return;
                        }
                    }
                    for (int i = columnValuesSize; i < columnNamesSize; ++i) {
                        if (!clearField(columnNames.get(i))) {
                            return;
                        }
                    }
                    if (runMode == RunMode.CHECK_FOR_UNKNOWN_IDS) {
                        String actualIDs = tuple.breaks;
                        if (actualIDs != null) {
                            // Make sure there are no IDs in actualIDs that are not in allowedIDs.
                            // As a bonus, check that actualIDs are in alphabetical order.
                            char prevID = 0;
                            for (int i=0; i<actualIDs.length(); i++) {
                                char currID = actualIDs.charAt(i);
                                if (allowedIDs.indexOf(currID) == -1) {
                                    showError("Unknown ID: " + currID);
                                }
                                if (prevID > currID) {
                                    showError("IDs not in alphabetical order: " + actualIDs);
                                }
                                prevID = currID;
                            }
                        }
                    } else if (runMode == RunMode.INCLUDE_KNOWN_FAILURES || !breaks(codeUnderTestId)) {
                        String errorMessage;
                        Exception err = null;
                        boolean shouldFail = (tuple.output != null && tuple.output.equals("fail"))
                                ? !breaks(codeUnderTestId)
                                : breaks(codeUnderTestId);
                        try {
                            errorMessage = isPass(tuple);
                        } catch (Exception e) {
                            err = e;
                            errorMessage = "Exception: " + e + ": " + e.getCause();
                        }
                        if (shouldFail && errorMessage == null) {
                            showError("Expected failure, but passed");
                        } else if (!shouldFail && errorMessage != null) {
                            if (err != null) {
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                PrintStream ps = new PrintStream(os);
                                err.printStackTrace(ps);
                                String stackTrace = os.toString();
                                showError(errorMessage + "     Stack trace: " + stackTrace.substring(0, 500));
                            } else {
                                showError(errorMessage);
                            }
                        }
                    }
                }
                fileLine = null;
            }
        } catch (Exception e) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            e.printStackTrace(ps);
            String stackTrace = os.toString();
            showError("MAJOR ERROR: " + e.toString() + "     Stack trace: " + stackTrace.substring(0,500));
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean breaks(char code) {
       String breaks = tuple.breaks == null ? "" : tuple.breaks;
       return (breaks.toUpperCase().indexOf(code) != -1);
    }

    private static boolean isSpace(char c) {
        return (c == 0x09 || c == 0x20 || c == 0x3000);
    }

    private boolean setTupleField() {
        List<String> parts = splitBy(3, (char) 0x20);
        if (parts.size() < 3) {
            showError("Set expects 2 parameters");
            return false;
        }
        return setField(parts.get(1), parts.get(2));
    }

    private boolean setField(String name, String value) {
        try {
            tuple.setField(name,  Utility.unescape(value));
            return true;
        } catch (Exception e) {
            showError("No such field: " + name + ", or bad value: " + value + ": " + e);
            return false;
        }
    }

    private boolean clearField(String name) {
        try {
            tuple.clearField(name);
            return true;
        } catch (Exception e) {
            showError("Field cannot be cleared: " + name);
            return false;
        }
    }

    private void showError(String message) {
        TestFmwk.errln(String.format("line %d: %s\n%s\n%s", fileLineNumber, Utility.escape(message), fileTestName,fileLine));
    }

    private List<String> splitBy(char delimiter) {
        return splitBy(Integer.MAX_VALUE, delimiter);
    }

    private List<String> splitBy(int max, char delimiter) {
        ArrayList<String> result = new ArrayList<String>();
        int colIdx = 0;
        int colStart = 0;
        int len = fileLine.length();
        for (int idx = 0; colIdx < max - 1 && idx < len; ++idx) {
            char ch = fileLine.charAt(idx);
            if (ch == delimiter) {
                result.add(
                        fileLine.substring(colStart, idx));
                ++colIdx;
                colStart = idx + 1;
            }
        }
        result.add(fileLine.substring(colStart, len));
        return result;
    }

    private boolean readLine(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            fileLine = null;
            return false;
        }
        ++fileLineNumber;
        // Strip trailing comments and spaces
        int idx = line.length();
        for (; idx > 0; idx--) {
            if (!isSpace(line.charAt(idx -1))) {
                break;
            }
        }
        fileLine = idx == 0 ? "" : line;
        return true;
    }

    private String isPass(DataDrivenNumberFormatTestData tuple) {
        StringBuilder result = new StringBuilder();
        if (tuple.format != null && tuple.output != null) {
            String errorMessage = codeUnderTest.format(tuple);
            if (errorMessage != null) {
                result.append(errorMessage);
            }
        } else if (tuple.toPattern != null || tuple.toLocalizedPattern != null) {
            String errorMessage = codeUnderTest.toPattern(tuple);
            if (errorMessage != null) {
                result.append(errorMessage);
            }
        } else if (tuple.parse != null && tuple.output != null && tuple.outputCurrency != null) {
            String errorMessage = codeUnderTest.parseCurrency(tuple);
            if (errorMessage != null) {
                result.append(errorMessage);
            }
        } else if (tuple.parse != null && tuple.output != null) {
            String errorMessage = codeUnderTest.parse(tuple);
            if (errorMessage != null) {
                result.append(errorMessage);
            }
        } else if (tuple.plural != null) {
            String errorMessage = codeUnderTest.select(tuple);
            if (errorMessage != null) {
                result.append(errorMessage);
            }
        } else {
            result.append("Unrecognized test type.");
        }
        if (result.length() > 0) {
            result.append(": ");
            result.append(tuple);
            return result.toString();
        }
        return null;
    }
}
