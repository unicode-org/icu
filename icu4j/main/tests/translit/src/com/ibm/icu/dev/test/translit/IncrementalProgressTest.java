// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.translit;

import java.util.Enumeration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.UtilityExtensions;
import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.Transliterator;

// Check to see that incremental gets at least part way through a reasonable string.
@RunWith(Parameterized.class)
public class IncrementalProgressTest extends TestFmwk {
    private String lang;
    private String text;

    public IncrementalProgressTest(String lang, String text){
        this.lang = lang;
        this.text = text;
    }

    @Parameterized.Parameters
    public static String[][] testData(){
        String latinTest = "The Quick Brown Fox.";
        String devaTest = Transliterator.getInstance("Latin-Devanagari").transliterate(latinTest);
        String kataTest = Transliterator.getInstance("Latin-Katakana").transliterate(latinTest);
        // Labels have to be valid transliterator source names.
        String[][] tests = {
                {"Any", latinTest},
                {"Latin", latinTest},
                {"Halfwidth", latinTest},
                {"Devanagari", devaTest},
                {"Katakana", kataTest},
        };
        return tests;
    }

    public void CheckIncrementalAux(Transliterator t, String input) {
        Replaceable test = new ReplaceableString(input);
        Transliterator.Position pos = new Transliterator.Position(0, test.length(), 0, test.length());
        t.transliterate(test, pos);
        boolean gotError = false;

        // we have a few special cases. Any-Remove (pos.start = 0, but also = limit) and U+XXXXX?X?
        if (pos.start == 0 && pos.limit != 0 && !t.getID().equals("Hex-Any/Unicode")) {
            errln("No Progress, " + t.getID() + ": " + UtilityExtensions.formatInput(test, pos));
            gotError = true;
        } else {
            logln("PASS Progress, " + t.getID() + ": " + UtilityExtensions.formatInput(test, pos));
        }
        t.finishTransliteration(test, pos);
        if (pos.start != pos.limit) {
            errln("Incomplete, " + t.getID() + ":  " + UtilityExtensions.formatInput(test, pos));
            gotError = true;
        }
        if(!gotError){
            //errln("FAIL: Did not get expected error");
        }
    }

    @Test
    public void TestIncrementalProgress() {
        Enumeration targets = Transliterator.getAvailableTargets(this.lang);
        while(targets.hasMoreElements()) {
            String target = (String) targets.nextElement();
            Enumeration variants = Transliterator.getAvailableVariants(this.lang, target);
            while(variants.hasMoreElements()) {
                String variant = (String) variants.nextElement();
                String id = this.lang + "-" + target + "/" + variant;
                if ((id.contains("Geminate") || id.contains("geminate")) &&
                        logKnownIssue("CLDR-16408", "Transliterator instantiation fails for Ethiopic-Latin /Geminate[d] transforms")) {
                    continue;
                }
                logln("id: " + id);

                Transliterator t = Transliterator.getInstance(id);
                CheckIncrementalAux(t, text);

                String rev = t.transliterate(text);

                // Special treatment: This transliterator has no registered inverse, skip for now.
                if (id.equals("Devanagari-Arabic/"))
                    continue;

                Transliterator inv = t.getInverse();
                CheckIncrementalAux(inv, rev);
            }
        }
    }

}
