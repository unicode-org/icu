// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.translit;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.translit.TransliteratorTest.SetAssert;
import com.ibm.icu.impl.UtilityExtensions;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import java.util.ArrayList;
import java.util.List;

public abstract class TransliteratorTestBase extends TestFmwk {

    void expect(String rules, String source, String expectedResult, Transliterator.Position pos) {
        Transliterator t = Transliterator.createFromRules("<ID>", rules, Transliterator.FORWARD);
        expect(t, source, expectedResult, pos);
    }

    void expect(String rules, String source, String expectedResult) {
        expect(rules, source, expectedResult, null);
    }

    void expect(
            Transliterator t,
            String source,
            String expectedResult,
            Transliterator reverseTransliterator) {
        expect(t, source, expectedResult);
        if (reverseTransliterator != null) {
            expect(reverseTransliterator, expectedResult, source);
        }
    }

    void expect(Transliterator t, String source, String expectedResult) {
        expect(t, source, expectedResult, (Transliterator.Position) null);
    }

    void expect(
            Transliterator t, String source, String expectedResult, Transliterator.Position pos) {
        if (pos == null) {
            String result = t.transliterate(source);
            if (!expectAux(t.getID() + ":String", source, result, expectedResult)) return;
        }

        Transliterator.Position index = null;
        if (pos == null) {
            index = new Transliterator.Position(0, source.length(), 0, source.length());
        } else {
            index =
                    new Transliterator.Position(
                            pos.contextStart, pos.contextLimit, pos.start, pos.limit);
        }

        ReplaceableString rsource = new ReplaceableString(source);

        t.finishTransliteration(rsource, index);
        // Do it all at once -- below we do it incrementally

        if (index.start != index.limit) {
            expectAux(
                    t.getID() + ":UNFINISHED",
                    source,
                    "start: " + index.start + ", limit: " + index.limit,
                    false,
                    expectedResult);
            return;
        }
        String result = rsource.toString();
        if (!expectAux(t.getID() + ":Replaceable", source, result, expectedResult)) return;

        if (pos == null) {
            index = new Transliterator.Position();
        } else {
            index =
                    new Transliterator.Position(
                            pos.contextStart, pos.contextLimit, pos.start, pos.limit);
        }

        // Test incremental transliteration -- this result
        // must be the same after we finalize (see below).
        List<String> v = new ArrayList<String>();
        v.add(source);
        rsource.replace(0, rsource.length(), "");
        if (pos != null) {
            rsource.replace(0, 0, source);
            v.add(UtilityExtensions.formatInput(rsource, index));
            t.transliterate(rsource, index);
            v.add(UtilityExtensions.formatInput(rsource, index));
        } else {
            for (int i = 0; i < source.length(); ++i) {
                // v.add(i == 0 ? "" : " + " + source.charAt(i) + "");
                // log.append(source.charAt(i)).append(" -> "));
                t.transliterate(rsource, index, source.charAt(i));
                // v.add(UtilityExtensions.formatInput(rsource, index) + source.substring(i+1));
                v.add(
                        UtilityExtensions.formatInput(rsource, index)
                                + ((i < source.length() - 1)
                                        ? (" + '" + source.charAt(i + 1) + "' ->")
                                        : " =>"));
            }
        }

        // As a final step in keyboard transliteration, we must call
        // transliterate to finish off any pending partial matches that
        // were waiting for more input.
        t.finishTransliteration(rsource, index);
        result = rsource.toString();
        // log.append(" => ").append(rsource.toString());
        v.add(result);

        String[] results = new String[v.size()];
        v.toArray(results);
        expectAux(
                t.getID() + ":Incremental", results, result.equals(expectedResult), expectedResult);
    }

    boolean expectAux(String tag, String source, String result, String expectedResult) {
        return expectAux(
                tag, new String[] {source, result}, result.equals(expectedResult), expectedResult);
    }

    boolean expectAux(
            String tag, String source, String result, boolean pass, String expectedResult) {
        return expectAux(tag, new String[] {source, result}, pass, expectedResult);
    }

    boolean expectAux(String tag, String source, boolean pass, String expectedResult) {
        return expectAux(tag, new String[] {source}, pass, expectedResult);
    }

    boolean expectAux(String tag, String[] results, boolean pass, String expectedResult) {
        if (pass) {
            logDebug("(" + tag + ")");
        } else {
            errln("FAIL: (" + tag + ")");
        }

        for (int i = 0; i < results.length; ++i) {
            String label;
            if (i == 0) {
                label = "source:   ";
            } else if (i == results.length - 1) {
                label = "result:   ";
            } else {
                if (!isVerbose() && pass) continue;
                label = "interm" + i + ":  ";
            }
            String msg = "    " + label + results[i];
            if (pass) {
                logDebug(msg);
            } else {
                errln(msg);
            }
        }

        if (!pass) {
            errln("    expected: " + expectedResult);
        }

        return pass;
    }

    void assertTransform(String message, String expected, StringTransform t, String source) {
        assertEquals(message + " " + source, expected, t.transform(source));
    }

    void assertTransform(
            String message,
            String expected,
            StringTransform t,
            StringTransform back,
            String source,
            String source2) {
        assertEquals(message + " " + source, expected, t.transform(source));
        assertEquals(message + " " + source2, expected, t.transform(source2));
        assertEquals(message + " " + expected, source, back.transform(expected));
    }

    void assertEquals(
            String message, UnicodeSet empirical, UnicodeSet actual, SetAssert setAssert) {
        boolean haveError = false;
        if (!actual.containsAll(empirical)) {
            UnicodeSet missing = new UnicodeSet(empirical).removeAll(actual);
            errln(
                    message
                            + " \tgetXSet < empirical ("
                            + missing.size()
                            + "): "
                            + toPattern(missing));
            haveError = true;
        }
        if (!empirical.containsAll(actual)) {
            UnicodeSet extra = new UnicodeSet(actual).removeAll(empirical);
            logDebug(
                    "WARNING: "
                            + message
                            + " \tgetXSet > empirical ("
                            + extra.size()
                            + "): "
                            + toPattern(extra));
            haveError = true;
        }
        if (!haveError) {
            logDebug("OK " + message + ' ' + toPattern(empirical));
        }
    }

    private static String toPattern(UnicodeSet missing) {
        String result = missing.toPattern(false);
        if (result.length() < 200) {
            return result;
        }
        return result.substring(0, CharSequences.onCharacterBoundary(result, 200) ? 200 : 199)
                + "\u2026";
    }
}
