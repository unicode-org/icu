// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.translit;

import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * @test
 * @summary Disordered marks test of Transliterator
 */
@RunWith(JUnitParamsRunner.class)
public class TransliteratorDisorderedMarksTest extends TestFmwk {
    private static UnicodeSet disorderedMarks;

    @AfterClass
    public static void disorderedMarksNull() {
        disorderedMarks = null;
    }

    @BeforeClass
    public static void disorderedMarksAddAll() {
        Normalizer2 nfc = Normalizer2.getNFCInstance();
        Normalizer2 nfd = Normalizer2.getNFDInstance();

        //        Normalizer2 nfkd = Normalizer2.getInstance(null, "nfkd", Mode.DECOMPOSE);
        //        UnicodeSet nfkdSource = new UnicodeSet();
        //        UnicodeSet nfkdTarget = new UnicodeSet();
        //        for (int i = 0; i <= 0x10FFFF; ++i) {
        //            if (nfkd.isInert(i)) {
        //                continue;
        //            }
        //            nfkdSource.add(i);
        //            String t = nfkd.getDecomposition(i);
        //            if (t != null) {
        //                nfkdTarget.addAll(t);
        //            } else {
        //                nfkdTarget.add(i);
        //            }
        //        }
        //        nfkdSource.freeze();
        //        nfkdTarget.freeze();
        //        logln("NFKD Source: " + nfkdSource.toPattern(false));
        //        logln("NFKD Target: " + nfkdTarget.toPattern(false));

        UnicodeMap<UnicodeSet> leadToTrail = new UnicodeMap();
        UnicodeMap<UnicodeSet> leadToSources = new UnicodeMap();
        UnicodeSet nonStarters = new UnicodeSet("[:^ccc=0:]").freeze();
        CanonicalIterator can = new CanonicalIterator("");

        disorderedMarks = new UnicodeSet();

        for (int i = 0; i <= 0x10FFFF; ++i) {
            String s = nfd.getDecomposition(i);
            if (s == null) {
                continue;
            }

            can.setSource(s);
            for (String t = can.next(); t != null; t = can.next()) {
                disorderedMarks.add(t);
            }

            // if s has two code points, (or more), add the lead/trail information
            int first = s.codePointAt(0);
            int firstCount = Character.charCount(first);
            if (s.length() == firstCount) continue;
            String trailString = s.substring(firstCount);

            // add all the trail characters
            if (!nonStarters.containsSome(trailString)) {
                continue;
            }
            UnicodeSet trailSet = leadToTrail.get(first);
            if (trailSet == null) {
                leadToTrail.put(first, trailSet = new UnicodeSet());
            }
            trailSet.addAll(trailString); // add remaining trails

            // add the sources
            UnicodeSet sourcesSet = leadToSources.get(first);
            if (sourcesSet == null) {
                leadToSources.put(first, sourcesSet = new UnicodeSet());
            }
            sourcesSet.add(i);
        }


        for (Entry<String, UnicodeSet> x : leadToSources.entrySet()) {
            String lead = x.getKey();
            UnicodeSet sources = x.getValue();
            UnicodeSet trailSet = leadToTrail.get(lead);
            for (String source : sources) {
                for (String trail : trailSet) {
                    can.setSource(source + trail);
                    for (String t = can.next(); t != null; t = can.next()) {
                        if (t.endsWith(trail)) continue;
                        disorderedMarks.add(t);
                    }
                }
            }
        }


        for (String s : nonStarters) {
            disorderedMarks.add("\u0345" + s);
            disorderedMarks.add(s+"\u0323");
            String xx = nfc.normalize("\u01EC" + s);
            if (!xx.startsWith("\u01EC")) {
                logln("??");
            }
        }

        //        for (int i = 0; i <= 0x10FFFF; ++i) {
        //            String s = nfkd.getDecomposition(i);
        //            if (s != null) {
        //                disorderedMarks.add(s);
        //                disorderedMarks.add(nfc.normalize(s));
        //                addDerivedStrings(nfc, disorderedMarks, s);
        //            }
        //            s = nfd.getDecomposition(i);
        //            if (s != null) {
        //                disorderedMarks.add(s);
        //            }
        //            if (!nfc.isInert(i)) {
        //                if (i == 0x00C0) {
        //                    logln("\u00C0");
        //                }
        //                can.setSource(s+"\u0334");
        //                for (String t = can.next(); t != null; t = can.next()) {
        //                    addDerivedStrings(nfc, disorderedMarks, t);
        //                }
        //                can.setSource(s+"\u0345");
        //                for (String t = can.next(); t != null; t = can.next()) {
        //                    addDerivedStrings(nfc, disorderedMarks, t);
        //                }
        //                can.setSource(s+"\u0323");
        //                for (String t = can.next(); t != null; t = can.next()) {
        //                    addDerivedStrings(nfc, disorderedMarks, t);
        //                }
        //            }
        //        }
        logln("Test cases: " + disorderedMarks.size());
        disorderedMarks.addAll(0,0x10FFFF).freeze();
        logln("isInert \u0104 " + nfc.isInert('\u0104'));
    }

    @Test
    @Parameters({
            ":: [:sc=COMMON:] any-name;",

            ":: [:Greek:] hex-any/C;",
            ":: [:Greek:] any-hex/C;",

            ":: [[:Mn:][:Me:]] remove;",
            ":: [[:Mn:][:Me:]] null;",


            ":: lower;",
            ":: upper;",
            ":: title;",
            ":: CaseFold;",

            ":: NFD;",
            ":: NFC;",
            ":: NFKD;",
            ":: NFKC;",

            ":: [[:Mn:][:Me:]] NFKD;",
            ":: Latin-Greek;",
            ":: [:Latin:] NFKD;",
            ":: NFKD;",
            ":: NFKD;\n" +
                ":: [[:Mn:][:Me:]] remove;\n" +
                ":: NFC;",
    })
    public void TestSourceTargetSet2(String rule) {
        Transliterator trans = Transliterator.createFromRules("temp", rule, Transliterator.FORWARD);
        UnicodeSet actualSource = trans.getSourceSet();
        UnicodeSet actualTarget = trans.getTargetSet();
        UnicodeSet empiricalSource = new UnicodeSet();
        UnicodeSet empiricalTarget = new UnicodeSet();
        String ruleDisplay = rule.replace("\n", "\t\t");
        UnicodeSet toTest = disorderedMarks;
        Normalizer2 nfd = Normalizer2.getNFDInstance();

        String test = nfd.normalize("\u0104");
        boolean DEBUG = true;
        @SuppressWarnings("unused")
        int count = 0; // for debugging
        for (String s : toTest) {
            if (s.equals(test)) {
                logln(test);
            }
            String t = trans.transform(s);
            if (!s.equals(t)) {
                if (!TransliteratorTest.isAtomic(s, t, trans)) {
                    TransliteratorTest.isAtomic(s, t, trans);
                    continue;
                }

                // only keep the part that changed; so skip the front and end.
                //                    int start = findSharedStartLength(s,t);
                //                    int end = findSharedEndLength(s,t);
                //                    if (start != 0 || end != 0) {
                //                        s = s.substring(start, s.length() - end);
                //                        t = t.substring(start, t.length() - end);
                //                    }
                if (DEBUG) {
                    if (!actualSource.containsAll(s)) {
                        count++;
                    }
                    if (!actualTarget.containsAll(t)) {
                        count++;
                    }
                }
                TransliteratorTest.addSourceTarget(s, empiricalSource, t, empiricalTarget);
            }
        }
        if (rule.contains("title")) {
            // See the comment in TestCasing() about the iota subscript.
            empiricalSource.remove(0x345);
        }
        TransliteratorTest.assertEquals("getSource(" + ruleDisplay + ")", empiricalSource, actualSource, TransliteratorTest.SetAssert.MISSING_OK);
        TransliteratorTest.assertEquals("getTarget(" + ruleDisplay + ")", empiricalTarget, actualTarget, TransliteratorTest.SetAssert.MISSING_OK);
    }
}
