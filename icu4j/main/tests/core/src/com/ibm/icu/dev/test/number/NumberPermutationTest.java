// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.ULocale;

/**
 * @author sffc
 *
 */
public class NumberPermutationTest extends TestFmwk {

    static final String[] kSkeletonParts = {
        // Notation
        "compact-short",
        "scientific/+ee/sign-always",
        null,
        // Unit
        "percent",
        "currency/EUR",
        "measure-unit/length-furlong",
        null,
        // Unit Width
        "unit-width-narrow",
        "unit-width-full-name",
        null,
        // Precision
        "precision-integer",
        ".000",
        ".##/@@@+",
        "@@",
        null,
        // Rounding Mode
        "rounding-mode-floor",
        null,
        // Integer Width
        "integer-width/##00",
        null,
        // Scale
        "scale/0.5",
        null,
        // Grouping
        "group-on-aligned",
        null,
        // Symbols
        "latin",
        null,
        // Sign Display
        "sign-accounting-except-zero",
        null,
        // Decimal Separator Display
        "decimal-always",
        null,
    };

    static final double[] kNumbersToTest = {0, 91827.3645, -0.22222};

    static final ULocale[] kLocales = {
            new ULocale("es-MX"),
            new ULocale("zh-TW"),
            new ULocale("bn-BD")
    };

    /**
     * Test permutations of 3 orthogonal skeleton parts from the list above.
     * Compare the results against the golden data file:
     *     numberpermutationtest.txt
     * To regenerate that file, run C++ intltest with the -G option.
     */
    @Test
    public void testPermutations() throws IOException {
        boolean quick = getExhaustiveness() <= 5;

        // Convert kSkeletonParts to a more convenient data structure
        ArrayList<ArrayList<String>> skeletonParts = new ArrayList<>();
        ArrayList<String> currentSection = new ArrayList<>();
        for (int i = 0; i < kSkeletonParts.length; i++) {
            String skeletonPart = kSkeletonParts[i];
            if (skeletonPart == null) {
                skeletonParts.add(currentSection);
                currentSection = new ArrayList<>();
            } else {
                currentSection.add(skeletonPart);
            }
        }

        // Build up the golden data string as we evaluate all permutations
        ArrayList<String> resultLines = new ArrayList<>();
        resultLines.add("# © 2019 and later: Unicode, Inc. and others.");
        resultLines.add("# License & terms of use: http://www.unicode.org/copyright.html");
        resultLines.add("");

        // Take combinations of 3 orthogonal options
        outer:
        for (int i = 0; i < skeletonParts.size() - 2; i++) {
            ArrayList<String> skeletons1 = skeletonParts.get(i);
            for (int j = i + 1; j < skeletonParts.size() - 1; j++) {
                ArrayList<String> skeletons2 = skeletonParts.get(j);
                for (int k = j + 1; k < skeletonParts.size(); k++) {
                    ArrayList<String> skeletons3 = skeletonParts.get(k);

                    // Evaluate all combinations of skeletons for these options
                    for (String skel1 : skeletons1) {
                        for (String skel2 : skeletons2) {
                            for (String skel3 : skeletons3) {
                                // Compute the skeleton
                                StringBuilder skeletonBuilder = new StringBuilder();
                                skeletonBuilder
                                    .append(skel1)  //
                                    .append(' ')   //
                                    .append(skel2)  //
                                    .append(' ')   //
                                    .append(skel3);
                                String skeleton = skeletonBuilder.toString();
                                resultLines.add(skeleton);

                                // Check several locales and several numbers in each locale
                                for (ULocale locale : kLocales) {
                                    LocalizedNumberFormatter lnf =
                                            NumberFormatter.forSkeleton(skeleton).locale(locale);
                                    resultLines.add("  " + locale.toLanguageTag());
                                    for (double input : kNumbersToTest) {
                                        resultLines.add("    " + lnf.format(input).toString());
                                    }
                                }

                                resultLines.add("");
                            }
                        }
                    }
                }

                // Quick mode: test all fields at least once but stop early.
                if (quick) {
                    logln("Quick mode: stopped after " + resultLines.size() + " lines");
                    break outer;
                }
            }
        }

        // Compare it to the golden file
        String codePage = "UTF-8";
        BufferedReader f = TestUtil.getDataReader("numberpermutationtest.txt", codePage);
        int lineNumber = 1;
        for (String actualLine : resultLines) {
            String expectedLine = f.readLine();
            if (expectedLine == null) {
                errln("More lines generated than are in the data file!");
                break;
            }
            assertEquals("Line #" + lineNumber + " differs",  //
                expectedLine, actualLine);
            lineNumber++;
        }
        // Quick mode: test all fields at least once but stop early.
        if (!quick && f.readLine() != null) {
            errln("Fewer lines generated than are in the data file!");
        }
        f.close();
    }
}
