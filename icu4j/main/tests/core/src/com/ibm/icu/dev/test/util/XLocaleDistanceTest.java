// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.locale.LocaleDistance;
import com.ibm.icu.impl.locale.LocaleDistance.DistanceOption;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Test the LocaleDistance.
 * TODO: Rename to LocaleDistanceTest.
 *
 * @author markdavis
 */
@RunWith(JUnit4.class)
public class XLocaleDistanceTest extends TestFmwk {
    private static final boolean REFORMAT = false; // set to true to get a reformatted data file listed

    private LocaleDistance localeDistance = LocaleDistance.INSTANCE;
    DataDrivenTestHelper tfh = new MyTestFileHandler()
            .setFramework(this)
            .load(XLocaleDistanceTest.class, "data/localeDistanceTest.txt");

    static class Arguments {
        final ULocale desired;
        final ULocale supported;
        final int desiredToSupported;
        final int supportedToDesired;

        public Arguments(List<String> args) {
            this.desired = new ULocale.Builder().setLanguageTag(args.get(0)).build(); // use more complicated expression to check syntax
            this.supported = new ULocale.Builder().setLanguageTag(args.get(1)).build();
            this.desiredToSupported = Integer.parseInt(args.get(2));
            this.supportedToDesired = args.size() > 3 ? Integer.parseInt(args.get(3)) : this.desiredToSupported;
        }
    }

    @SuppressWarnings("unused")
    @Ignore("Disabled because of Linux; need to investigate.")
    @Test
    public void testTiming() {
        List<Arguments> testArgs = new ArrayList<>();
        for (List<String> line : tfh.getLines()) {
            if (tfh.isTestLine(line)) {
                testArgs.add(new Arguments(line));
            }
        }
        Arguments[] tests = testArgs.toArray(new Arguments[testArgs.size()]);

        final LocaleMatcher oldLocaleMatcher = new LocaleMatcher("");

        long likelyTime = 0;
        long newLikelyTime = 0;
        long newTimeMinusLikely = 0;
        //long intTime = 0;
        long oldTimeMinusLikely = 0;
        final int maxIterations = 1000;

        for (int iterations = maxIterations; iterations > 0; --iterations) {
            // int count=0;
            for (Arguments test : tests) {
                final ULocale desired = test.desired;
                final ULocale supported = test.supported;
                //final int desiredToSupported = test.desiredToSupported;
                //final int supportedToDesired = test.supportedToDesired;

                long temp = System.nanoTime();
                final ULocale desiredMax = ULocale.addLikelySubtags(desired);
                final ULocale supportedMax = ULocale.addLikelySubtags(supported);
                likelyTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                //double distOld1 = oldLocaleMatcher.match(desired, desiredMax, supported, supportedMax);
                //double distOld2 = oldLocaleMatcher.match(supported, supportedMax, desired, desiredMax);
                oldTimeMinusLikely += System.nanoTime()-temp;

                temp = System.nanoTime();
//                final LSR desiredLSR = LSR.maximizedFrom(desired);
//                final LSR supportedLSR = LSR.maximizedFrom(supported);
                newLikelyTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                int dist1 = localeDistance.testOnlyDistance(desired, supported, 1000, DistanceOption.REGION_FIRST);
                int dist2 = localeDistance.testOnlyDistance(supported, desired, 1000, DistanceOption.REGION_FIRST);
                newTimeMinusLikely += System.nanoTime()-temp;
            }
        }
        final long oldTime = oldTimeMinusLikely+likelyTime;
        final long newTime = newLikelyTime+newTimeMinusLikely;
        logln("\n");
        logln("\tlikelyTime:\t" + likelyTime/maxIterations);
        logln("\toldTime-likelyTime:\t" + oldTimeMinusLikely/maxIterations);
        logln("totalOld:\t" + oldTime/maxIterations);
        logln("\tnewLikelyTime:\t" + newLikelyTime/maxIterations);
        logln("totalNew:\t" + newTime/maxIterations);
        assertTrue("newTime < 20% of oldTime", newTime * 5 < oldTime);
        //logln("\tnewIntTime-newLikelyTime-extractTime:\t" + intTime/maxIterations);
        //logln("totalInt:\t" + (intTime)/maxIterations);
    }

    @Test
    public void testInternalTable() {
        Set<String> strings = localeDistance.testOnlyGetDistanceTable(false).keySet();
        // Check that the table has a depth of exactly 3 (desired, supported) pairs everyplace
        // by removing every prefix of a 6-subtag string from a copy of the set of strings.
        // Any remaining string is not a prefix of a full-depth string.
        Set<String> remaining = new HashSet<>(strings);
        // Check that ANY, ANY is always present.
        assertTrue("*-*", strings.contains("*-*"));
        for (String s : strings) {
            int num = countSubtags(s);
            assertTrue(s, 1 <= num && num <= 6);
            if (num > 1) {
                String oneShorter = removeLastSubtag(s);
                assertTrue(oneShorter, strings.contains(oneShorter));
            }
            if (num == 2 || num == 4) {
                String sPlusAnyAny = s + "-*-*";
                assertTrue(sPlusAnyAny, strings.contains(sPlusAnyAny));
            } else if (num == 6) {
                for (;; --num) {
                    remaining.remove(s);
                    if (num == 1) { break; }
                    s = removeLastSubtag(s);
                }
            }
        }
        assertTrue("strings that do not lead to 6-subtag matches", remaining.isEmpty());
    }

    private static final int countSubtags(String s) {
        if (s.isEmpty()) { return 0; }
        int num = 1;
        for (int pos = 0; (pos = s.indexOf('-', pos)) >= 0; ++pos) {
            ++num;
        }
        return num;
    }

    private static final String removeLastSubtag(String s) {
        int last = s.lastIndexOf('-');
        return s.substring(0, last);
    }

    @Test
    public void testShowDistanceTable() {
        if (isVerbose()) {
            localeDistance.testOnlyPrintDistanceTable();
        }
    }

    @Test
    public void testDataDriven() throws IOException {
        tfh.test();
        if (REFORMAT) {
            System.out.println(tfh.appendLines(new StringBuffer()));
        }
    }

    class MyTestFileHandler extends DataDrivenTestHelper {
        Output<ULocale> bestDesired = new Output<>();
        private DistanceOption distanceOption = DistanceOption.REGION_FIRST;
        private Integer threshold = localeDistance.getDefaultScriptDistance();

        @Override
        public void handle(int lineNumber, boolean breakpoint, String commentBase, List<String> arguments) {
            if (breakpoint) {
                breakpoint = false; // put debugger breakpoint here to break at @debug in test file
            }
            Arguments args = new Arguments(arguments);
            int supportedToDesiredActual = localeDistance.testOnlyDistance(args.supported, args.desired, threshold, distanceOption);
            int desiredToSupportedActual = localeDistance.testOnlyDistance(args.desired, args.supported, threshold, distanceOption);
            String desiredTag = args.desired.toLanguageTag();
            String supportedTag = args.supported.toLanguageTag();
            final String comment = commentBase.isEmpty() ? "" : "\t# " + commentBase;
            if (assertEquals("(" + lineNumber + ") " + desiredTag + " to " + supportedTag + comment, args.desiredToSupported, desiredToSupportedActual)) {
                assertEquals("(" + lineNumber + ") " + supportedTag + " to " + desiredTag + comment, args.supportedToDesired, supportedToDesiredActual);
            }
        }
        @Override
        public void handleParams(String comment, List<String> arguments) {
            String switchArg = arguments.get(0);
            if (switchArg.equals("@DistanceOption")) {
                distanceOption = DistanceOption.valueOf(arguments.get(1));
            } else if (switchArg.equals("@Threshold")) {
                threshold = Integer.valueOf(arguments.get(1));
            } else {
                super.handleParams(comment, arguments);
            }
            return;
        }
    }
}
