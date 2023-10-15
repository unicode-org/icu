// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.tool.locale.LocaleDistanceBuilder;
import com.ibm.icu.impl.locale.LocaleDistance;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocaleMatcher.FavorSubtag;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Test the LocaleDistance.
 * TODO: Rename to LocaleDistanceTest.
 *
 * @author markdavis
 */
@RunWith(JUnit4.class)
public class LocaleDistanceTest extends CoreTestFmwk {
    private static final boolean REFORMAT = false; // set to true to get a reformatted data file listed

    private LocaleDistance localeDistance = LocaleDistance.INSTANCE;
    DataDrivenTestHelper tfh = new MyTestFileHandler()
            .setFramework(this)
            .load(LocaleDistanceTest.class, "data/localeDistanceTest.txt");

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

    @Test
    public void testLoadedDataSameAsBuiltFromScratch() {
        LocaleDistance.Data built = LocaleDistanceBuilder.build();
        LocaleDistance.Data loaded = LocaleDistance.Data.load();
        assertEquals("run LocaleDistanceBuilder and update ICU4C langInfo.txt", built, loaded);
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
                int dist1 = localeDistance.testOnlyDistance(desired, supported, 1000, FavorSubtag.LANGUAGE);
                int dist2 = localeDistance.testOnlyDistance(supported, desired, 1000, FavorSubtag.LANGUAGE);
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
        private FavorSubtag favorSubtag = FavorSubtag.LANGUAGE;
        private Integer threshold = localeDistance.getDefaultScriptDistance();

        @Override
        public void handle(int lineNumber, boolean breakpoint, String commentBase, List<String> arguments) {
            if (breakpoint) {
                breakpoint = false; // put debugger breakpoint here to break at @debug in test file
            }
            Arguments args = new Arguments(arguments);
            String desiredTag = args.desired.toLanguageTag();
            String supportedTag = args.supported.toLanguageTag();
            final String comment = commentBase.isEmpty() ? "" : "\t# " + commentBase;
            int supportedToDesiredActual = localeDistance.testOnlyDistance(args.supported, args.desired, threshold, favorSubtag);
            assertEquals("(" + lineNumber + ") " + supportedTag + " to " + desiredTag + comment,
                    args.supportedToDesired, supportedToDesiredActual);
            int desiredToSupportedActual = localeDistance.testOnlyDistance(args.desired, args.supported, threshold, favorSubtag);
            assertEquals("(" + lineNumber + ") " + desiredTag + " to " + supportedTag + comment,
                    args.desiredToSupported, desiredToSupportedActual);
        }
        @Override
        public void handleParams(String comment, List<String> arguments) {
            String switchArg = arguments.get(0);
            if (switchArg.equals("@FavorSubtag")) {
                favorSubtag = FavorSubtag.valueOf(arguments.get(1));
            } else if (switchArg.equals("@Threshold")) {
                threshold = Integer.valueOf(arguments.get(1));
            } else {
                super.handleParams(comment, arguments);
            }
            return;
        }
    }
}
