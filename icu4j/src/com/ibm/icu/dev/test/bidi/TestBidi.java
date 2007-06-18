/*
*******************************************************************************
*   Copyright (C) 2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.test.bidi;

import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;

/**
 * Regression test for Bidi class override.
 *
 * @author Lina Kemmel, Matitiahu Allouche
 */

public class TestBidi extends BidiTest {

    private static final int MAXLEN = 256;
    private static final String levelString = "............................";

    public void testBidi() {
        Bidi bidi;
        Bidi bidiLine;

        logln("\nEntering TestBidi");
        bidi = new Bidi(MAXLEN, 0);
        bidiLine = new Bidi();

        doTests(bidi, bidiLine, false);
        doTests(bidi, bidiLine, true);
        logln("\nExiting TestBidi");
    }

    private void doTests(Bidi bidi, Bidi bidiLine, boolean countRunsFirst) {
        int testNumber;
        String string;
        int lineStart;
        byte paraLevel;
        int bidiTestCount = TestData.testCount();

        for (testNumber = 0; testNumber < bidiTestCount; ++testNumber) {
            TestData test = TestData.getTestData(testNumber);
            string = getStringFromDirProps(test.dirProps);
            paraLevel = test.paraLevel;
            try {
                bidi.setPara(string, paraLevel, null);
                logln("Bidi.setPara(tests[" + testNumber + "] OK, direction "
                        + bidi.getDirection() + " paraLevel "
                        + paraLevel);
            } catch (Exception e) {
                errln("Bidi.setPara(tests[" + testNumber + "] failed, direction "
                        + bidi.getDirection() + " paraLevel "
                        + paraLevel);
            }
            lineStart = test.lineStart;
            if (lineStart == -1) {
                doTest(bidi, testNumber, test, 0, countRunsFirst);
            } else {
                try {
                    bidiLine = bidi.setLine(lineStart, test.lineLimit);
                    logln("Bidi.setLine(" + lineStart + ", " + test.lineLimit
                            + "), in tests[" + testNumber + "] OK, direction "
                            + bidiLine.getDirection() + " paraLevel "
                            + bidiLine.getBaseLevel());
                    doTest(bidiLine, testNumber, test, lineStart, countRunsFirst);
                } catch (Exception e)  {
                    errln("Bidi.setLine(" + lineStart + ", " + test.lineLimit
                            + "), in runAll test[" + testNumber + "] failed, "
                            + "direction " + bidiLine.getDirection()
                            + " paraLevel " + bidiLine.getBaseLevel());
                }
            }
        }
    }

    private void doTest(Bidi bidi, int testNumber, TestData test,
                        int lineStart, boolean countRunsFirst) {
        short[] dirProps = test.dirProps;
        byte[] levels = test.levels;
        int[] visualMap = test.visualMap;
        int i, len = bidi.getLength(), logicalIndex = -1, runCount = 0;
        byte level, level2;

        if (countRunsFirst) {
            logln("Calling Bidi.countRuns() first.");
            try {
                runCount = bidi.countRuns();
            } catch (IllegalStateException e) {
                errln("Bidi.countRuns(test[" + testNumber + "]) failed");
            }
        } else {
            logln("Calling Bidi.getLogicalMap() first.");
        }

        _testReordering(bidi, testNumber);

        for (i = 0; i < len; ++i) {
            logln(i + "  " + bidi.getLevelAt(i) + "  " + levelString
                    + TestData.dirPropNames[dirProps[lineStart + i]] + "  "
                    + bidi.getVisualIndex(i));
        }

        log("\n-----levels:");
        for (i = 0; i < len; ++i) {
            if (i > 0) {
                log(",");
            }
            log(" " + bidi.getLevelAt(i));
        }

        log("\n--reordered:");
        for (i = 0; i < len; ++i) {
            if (i > 0) {
                log(",");
            }
            log(" " + bidi.getVisualIndex(i));
        }
        log("\n");

        assertEquals("\nFailure in Bidi.getDirection(test[" + testNumber + "])",
                     test.direction, bidi.getDirection());
        assertEquals("\nFailure in Bidi.getParaLevel(test[" + testNumber + "])",
                     test.resultLevel, bidi.getParaLevel());

        for (i = 0; i < len; ++i) {
            assertEquals("\nFailure in Bidi.getLevelAt(" + i +
                         ") in test[" + testNumber + "]",
                         levels[i], bidi.getLevelAt(i));
        }

        for (i = 0; i < len; ++i) {
            try {
                logicalIndex = bidi.getVisualIndex(i);
            } catch (Throwable th) {
                errln("Bidi.getVisualIndex(" + i + ") in test[" + testNumber
                        + "] failed");
            }
            if(visualMap[i] != logicalIndex) {
                assertEquals("\nFailure in Bidi.getVisualIndex(" + i +
                             ") in test[" + testNumber + "])",
                             visualMap[i], logicalIndex);
            }
        }

        if (!countRunsFirst) {
            try {
                runCount = bidi.countRuns();
            } catch (IllegalStateException e) {
                errln("Bidi.countRuns(test[" + testNumber + "]) failed");
            }
        }

        BidiRun run;

        for (logicalIndex = 0; logicalIndex < len; ) {
            level = bidi.getLevelAt(logicalIndex);
            run = bidi.getLogicalRun(logicalIndex);
            logicalIndex = run.getLimit();
            level2 = run.getEmbeddingLevel();
            if (level != level2) {
                assertEquals("Logical run ending at index " + logicalIndex +
                             " in test[" + testNumber + "]: wrong level",
                             level, level2);
            }
            if (--runCount < 0) {
                errln("Bidi.getLogicalRun(test[" + testNumber +
                      "]): wrong number of runs compared to " +
                      bidi.countRuns());
            }
        }
        if (runCount != 0) {
            errln("Bidi.getLogicalRun(test[" + testNumber
                    + "]): wrong number of runs compared to Bidi.countRuns() = "
                    + bidi.countRuns());
        }

        log("\n\n");
    }

    private void _testReordering(Bidi bidi, int testNumber) {
        int[] logicalMap1;
        int[] logicalMap2;
        int[] logicalMap3;
        int[] visualMap1;
        int[] visualMap2;
        int[] visualMap3;
        int[] visualMap4 = new int[MAXLEN];
        byte[] levels;
        int i, length = bidi.getLength(),
               destLength = bidi.getResultLength();
        int runCount, visualIndex, logicalIndex = -1, logicalStart, runLength;
        boolean odd;

        if(length <= 0) {
            return;
        }
        /* get the logical and visual maps from the object */
        logicalMap1 = bidi.getLogicalMap();
        if (logicalMap1 == null) {
            errln("getLogicalMap in test " + testNumber + " is null");
            logicalMap1 = new int[0];
        }

        visualMap1 = bidi.getVisualMap();

        if (visualMap1 == null) {
            errln("getVisualMap() in test " + testNumber + " is null");
            visualMap1 = new int[0];
        }

        /* invert them both */
        visualMap2 = Bidi.invertMap(logicalMap1);
        logicalMap2 = Bidi.invertMap(visualMap1);

        /* get them from the levels array, too */
        levels = bidi.getLevels();

        if (levels == null || levels.length != length) {
            errln("getLevels() in test " + testNumber + " failed");
        }

        logicalMap3 = Bidi.reorderLogical(levels);
        visualMap3 = Bidi.reorderVisual(levels);

        /* get the visual map from the runs, too */
        try {
            runCount = bidi.countRuns();
        } catch (IllegalStateException e) {
            errln("countRuns() in test " + testNumber + " failed");
            runCount = 0;
        }

        logln("\n---- " + runCount + " runs");
        visualIndex = 0;
        BidiRun run;
        for (i = 0; i < runCount; ++i) {
            run = bidi.getVisualRun(i);
            if (run == null) {
                errln("null visual run encountered at index " + i +
                      ", in test " + testNumber);
                continue;
            }
            odd = run.isOddRun();
            logicalStart = run.getStart();
            runLength = run.getLength();
            log("(" + (run.isOddRun() ? "R" : "L"));
            log(" @" + run.getStart() + '[' + run.getLength() + "])\n");
            if (!odd) {
                do {    /* LTR */
                    visualMap4[visualIndex++] = logicalStart++;
                } while (--runLength > 0);
            } else {
                logicalStart += runLength;  /* logicalLimit */
                do {    /* RTL */
                    visualMap4[visualIndex++] = --logicalStart;
                } while (--runLength > 0);
            }
        }
        log("\n");

        /* print all the maps */
        logln("logical maps:");
        for (i = 0; i < length; ++i) {
            log(logicalMap1[i] + " ");
        }
        log("\n");
        for (i = 0; i < length; ++i) {
            log(logicalMap2[i] + " ");
        }
        log("\n");
        for (i = 0; i < length; ++i) {
            log(logicalMap3[i] + " ");
        }

        log("\nvisual maps:\n");
        for (i = 0; i < destLength; ++i) {
            log(visualMap1[i] + " ");
        }
        log("\n");
        for (i = 0; i < destLength; ++i) {
            log(visualMap2[i] + " ");
        }
        log("\n");
        for (i = 0; i < length; ++i) {
            log(visualMap3[i] + " ");
        }
        log("\n");
        for (i = 0; i < length; ++i) {
            log(visualMap4[i] + " ");
        }
        log("\n");

        /* check that the indexes are the same between these and Bidi.getLogical/VisualIndex() */
        for (i = 0; i < length; ++i) {
            if (logicalMap1[i] != logicalMap2[i]) {
                errln("Error in tests[" + testNumber + "]: (logicalMap1[" + i +
                      "] == " + logicalMap1[i] + ") != (logicalMap2[" + i +
                      "] == " + logicalMap2[i] + ")");
            }
            if (logicalMap1[i] != logicalMap3[i]) {
                errln("Error in tests[" + testNumber + "]: (logicalMap1[" + i +
                      "] == " + logicalMap1[i] + ") != (logicalMap3[" + i +
                      "] == " + logicalMap3[i] + ")");
            }
            if (visualMap1[i] != visualMap2[i]) {
                errln("Error in tests[" + testNumber + "]: (visualMap1[" + i +
                      "] == " + visualMap1[i] + ") != (visualMap2[" + i +
                      "] == " + visualMap2[i] + ")");
            }
            if (visualMap1[i] != visualMap3[i]) {
                errln("Error in tests[" + testNumber + "]: (visualMap1[" + i +
                      "] == " + visualMap1[i] + ") != (visualMap3[" + i +
                      "] == " + visualMap3[i] + ")");
            }
            if (visualMap1[i] != visualMap4[i]) {
                errln("Error in tests[" + testNumber + "]: (visualMap1[" + i +
                      "] == " + visualMap1[i] + ") != (visualMap4[" + i +
                      "] == " + visualMap4[i] + ")");
            }
            try {
                visualIndex = bidi.getVisualIndex(i);
            } catch (Exception e) {
                errln("Bidi.getVisualIndex(" + i + ") failed in tests[" +
                      testNumber + "]");
            }
            if (logicalMap1[i] != visualIndex) {
                errln("Error in tests[" + testNumber + "]: (logicalMap1[" + i +
                      "] == " + logicalMap1[i] + ") != (Bidi.getVisualIndex(" + i +
                      ") == " + visualIndex + ")");
            }
            try {
                logicalIndex = bidi.getLogicalIndex(i);
            } catch (Exception e) {
                errln("Bidi.getLogicalIndex(" + i + ") failed in tests[" +
                      testNumber + "]");
            }
            if (visualMap1[i] != logicalIndex) {
                errln("Error in tests[" + testNumber + "]: (visualMap1[" + i +
                      "] == " + visualMap1[i] + ") != (Bidi.getLogicalIndex(" + i +
                      ") == " + logicalIndex + ")");
            }
        }
    }

    private String getStringFromDirProps(short[] dirProps) {
        int i;

        if (dirProps == null) {
            return null;
        }
        int length = dirProps.length;
        char[] buffer = new char[length];

        /* this part would have to be modified for UTF-x */
        for (i = 0; i < length; ++i) {
            buffer[i] = charFromDirProp[dirProps[i]];
        }
        return new String(buffer);
    }


    public static void main(String[] args) {
        try {
            new TestBidi().run(args);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
