/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/TransliteratorTest.java,v $ 
 * $Date: 2001/07/02 20:54:51 $ 
 * $Revision: 1.39 $
 *
 *****************************************************************************************
 */
package com.ibm.test.translit;
import com.ibm.text.*;
import com.ibm.test.*;
import com.ibm.util.Utility;
import java.text.*;
import java.util.*;

/**
 * @test
 * @summary General test of Transliterator
 */
public class TransliteratorTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TransliteratorTest().run(args);
    }

    public void TestInstantiation() {
        long ms = System.currentTimeMillis();
        String ID;
        for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements(); ) {
            ID = (String) e.nextElement();
            try {
                Transliterator t = Transliterator.getInstance(ID);
                // We should get a new instance if we try again
                Transliterator t2 = Transliterator.getInstance(ID);
                if (t != t2) {
                    logln(ID + ":" + t);
                } else {
                    errln("FAIL: " + ID + " returned identical instances");
                }
            } catch (IllegalArgumentException ex) {
                errln("FAIL: " + ID);
                throw ex;
            }
        }

        // Now test the failure path
        try {
            ID = "<Not a valid Transliterator ID>";
            Transliterator t = Transliterator.getInstance(ID);
            errln("FAIL: " + ID + " returned " + t);
        } catch (IllegalArgumentException ex) {
            logln("OK: Bogus ID handled properly");
        }
        
        ms = System.currentTimeMillis() - ms;
        logln("Elapsed time: " + ms + " ms");
    }

    public void TestDisplayName() {
        String ID;
        for (Enumeration e = Transliterator.getAvailableIDs(); e.hasMoreElements(); ) {
            ID = (String) e.nextElement();
            logln(ID + " -> " + Transliterator.getDisplayName(ID));
        }
    }

    public void TestSimpleRules() {
        /* Example: rules 1. ab>x|y
         *                2. yc>z
         *
         * []|eabcd  start - no match, copy e to tranlated buffer
         * [e]|abcd  match rule 1 - copy output & adjust cursor
         * [ex|y]cd  match rule 2 - copy output & adjust cursor
         * [exz]|d   no match, copy d to transliterated buffer
         * [exzd]|   done
         */
        expect("ab>x|y;" +
               "yc>z",
               "eabcd", "exzd");

        /* Another set of rules:
         *    1. ab>x|yzacw
         *    2. za>q
         *    3. qc>r
         *    4. cw>n
         *
         * []|ab       Rule 1
         * [x|yzacw]   No match
         * [xy|zacw]   Rule 2
         * [xyq|cw]    Rule 4
         * [xyqn]|     Done
         */
        expect("ab>x|yzacw;" +
               "za>q;" +
               "qc>r;" +
               "cw>n",
               "ab", "xyqn");

        /* Test categories
         */
        Transliterator t = new RuleBasedTransliterator("<ID>",
                                                       "$dummy=\uE100;" +
                                                       "$vowel=[aeiouAEIOU];" +
                                                       "$lu=[:Lu:];" +
                                                       "$vowel } $lu > '!';" +
                                                       "$vowel > '&';" +
                                                       "'!' { $lu > '^';" +
                                                       "$lu > '*';" +
                                                       "a>ERROR");
        expect(t, "abcdefgABCDEFGU", "&bcd&fg!^**!^*&");
    }

    /**
     * Test undefined variable.
     */
    public void TestUndefinedVariable() {
        String rule = "$initial } a <> \u1161;";
        try {
            Transliterator t = new RuleBasedTransliterator("<ID>", rule);
        } catch (IllegalArgumentException e) {
            logln("OK: Got exception for " + rule + ", as expected: " +
                  e.getMessage());
            return;
        }
        errln("Fail: bogus rule " + rule + " compiled without error");
    }

    /**
     * Test empty context.
     */
    public void TestEmptyContext() {
        expect(" { a } > b;", "xay a ", "xby b ");
    }

    /**
     * Test inline set syntax and set variable syntax.
     */
    public void TestInlineSet() {
        expect("{ [:Ll:] } x > y; [:Ll:] > z;", "aAbxq", "zAyzz");
        expect("a[0-9]b > qrs", "1a7b9", "1qrs9");

        expect("$digit = [0-9];" +
               "$alpha = [a-zA-Z];" +
               "$alphanumeric = [$digit $alpha];" + // ***
               "$special = [^$alphanumeric];" +     // ***
               "$alphanumeric > '-';" +
               "$special > '*';",

               "thx-1138", "---*----");
    }

    /**
     * Create some inverses and confirm that they work.  We have to be
     * careful how we do this, since the inverses will not be true
     * inverses -- we can't throw any random string at the composition
     * of the transliterators and expect the identity function.  F x
     * F' != I.  However, if we are careful about the input, we will
     * get the expected results.
     */
    public void TestRuleBasedInverse() {
        String RULES =
            "abc>zyx;" +
            "ab>yz;" +
            "bc>zx;" +
            "ca>xy;" +
            "a>x;" +
            "b>y;" +
            "c>z;" +

            "abc<zyx;" +
            "ab<yz;" +
            "bc<zx;" +
            "ca<xy;" +
            "a<x;" +
            "b<y;" +
            "c<z;" +

            "";

        String[] DATA = {
            // Careful here -- random strings will not work.  If we keep
            // the left side to the domain and the right side to the range
            // we will be okay though (left, abc; right xyz).
            "a", "x",
            "abcacab", "zyxxxyy",
            "caccb", "xyzzy",
        };

        Transliterator fwd = new RuleBasedTransliterator("<ID>", RULES);
        Transliterator rev = new RuleBasedTransliterator("<ID>", RULES,
                                     RuleBasedTransliterator.REVERSE, null);
        for (int i=0; i<DATA.length; i+=2) {
            expect(fwd, DATA[i], DATA[i+1]);
            expect(rev, DATA[i+1], DATA[i]);
        }
    }

    /**
     * Basic test of keyboard.
     */
    public void TestKeyboard() {
        Transliterator t = new RuleBasedTransliterator("<ID>", 
                                                       "psch>Y;"
                                                       +"ps>y;"
                                                       +"ch>x;"
                                                       +"a>A;");
        String DATA[] = {
            // insertion, buffer
            "a", "A",
            "p", "Ap",
            "s", "Aps",
            "c", "Apsc",
            "a", "AycA",
            "psch", "AycAY",
            null, "AycAY", // null means finishKeyboardTransliteration
        };

        keyboardAux(t, DATA);
    }

    /**
     * Basic test of keyboard with cursor.
     */
    public void TestKeyboard2() {
        Transliterator t = new RuleBasedTransliterator("<ID>", 
                                                       "ych>Y;"
                                                       +"ps>|y;"
                                                       +"ch>x;"
                                                       +"a>A;");
        String DATA[] = {
            // insertion, buffer
            "a", "A",
            "p", "Ap",
            "s", "Ay",
            "c", "Ayc",
            "a", "AycA",
            "p", "AycAp",
            "s", "AycAy",
            "c", "AycAyc",
            "h", "AycAY",
            null, "AycAY", // null means finishKeyboardTransliteration
        };

        keyboardAux(t, DATA);
    }

    /**
     * Test keyboard transliteration with back-replacement.
     */
    public void TestKeyboard3() {
        // We want th>z but t>y.  Furthermore, during keyboard
        // transliteration we want t>y then yh>z if t, then h are
        // typed.
        String RULES =
            "t>|y;" +
            "yh>z;" +
            "";

        String[] DATA = {
            // Column 1: characters to add to buffer (as if typed)
            // Column 2: expected appearance of buffer after
            //           keyboard xliteration.
            "a", "a",
            "b", "ab",
            "t", "aby",
            "c", "abyc",
            "t", "abycy",
            "h", "abycz",
            null, "abycz", // null means finishKeyboardTransliteration
        };

        Transliterator t = new RuleBasedTransliterator("<ID>", RULES);
        keyboardAux(t, DATA);
    }

    private void keyboardAux(Transliterator t, String[] DATA) {
        Transliterator.Position index = new Transliterator.Position();
        ReplaceableString s = new ReplaceableString();
        for (int i=0; i<DATA.length; i+=2) {
            StringBuffer log;
            if (DATA[i] != null) {
                log = new StringBuffer(s.toString() + " + "
                                       + DATA[i]
                                       + " -> ");
                t.transliterate(s, index, DATA[i]);
            } else {
                log = new StringBuffer(s.toString() + " => ");
                t.finishTransliteration(s, index);
            }
            String str = s.toString();
            // Show the start index '{' and the cursor '|'
            log.append(str.substring(0, index.contextStart)).
                append('{').
                append(str.substring(index.contextStart,
                                     index.start)).
                append('|').
                append(str.substring(index.start));
            if (str.equals(DATA[i+1])) {
                logln(log.toString());
            } else {
                errln("FAIL: " + log.toString() + ", expected " + DATA[i+1]);
            }
        }
    }

    public void TestArabic() {
        String DATA[] = {
            "Arabic",
                "\u062a\u062a\u0645\u062a\u0639 "+
                "\u0627\u0644\u0644\u063a\u0629 "+
                "\u0627\u0644\u0639\u0631\u0628\u0628\u064a\u0629 "+
                "\u0628\u0628\u0646\u0638\u0645 "+
                "\u0643\u062a\u0627\u0628\u0628\u064a\u0629 "+
                "\u062c\u0645\u064a\u0644\u0629"
        };

        Transliterator t = Transliterator.getInstance("Latin-Arabic");
        for (int i=0; i<DATA.length; i+=2) {
            expect(t, DATA[i], DATA[i+1]);
        }
    }

    /**
     * Compose the Kana transliterator forward and reverse and try
     * some strings that should come out unchanged.
     */
    public void TestCompoundKana() {
        Transliterator t = new CompoundTransliterator("Latin-Kana;Kana-Latin");
        expect(t, "aaaaa", "aaaaa");
    }

    /**
     * Compose the hex transliterators forward and reverse.
     */
    public void TestCompoundHex() {
        Transliterator a = Transliterator.getInstance("Unicode-Hex");
        Transliterator b = Transliterator.getInstance("Hex-Unicode");
        Transliterator[] trans = { a, b };
        Transliterator ab = new CompoundTransliterator(trans);

        // Do some basic tests of b
        expect(b, "\\u0030\\u0031", "01");

        String s = "abcde";
        expect(ab, s, s);

        trans = new Transliterator[] { b, a };
        Transliterator ba = new CompoundTransliterator(trans);
        ReplaceableString str = new ReplaceableString(s);
        a.transliterate(str);
        expect(ba, str.toString(), str.toString());
    }

    /**
     * Do some basic tests of filtering.
     */
    public void TestFiltering() {
        Transliterator hex = Transliterator.getInstance("Unicode-Hex");
        hex.setFilter(new UnicodeFilter() {
            public boolean contains(char c) {
                return c != 'c';
            }
        });
        String s = "abcde";
        String out = hex.transliterate(s);
        String exp = "\\u0061\\u0062c\\u0064\\u0065";
        if (out.equals(exp)) {
            logln("Ok:   \"" + exp + "\"");
        } else {
            logln("FAIL: \"" + out + "\", wanted \"" + exp + "\"");
        }
    }

    /**
     * Test anchors
     */
    public void TestAnchors() { 
        expect("^ab  > 01 ;" +
               " ab  > |8 ;" +
               "  b  > k ;" +
               " 8x$ > 45 ;" +
               " 8x  > 77 ;",

               "ababbabxabx",
               "018k7745");  
        expect("$s = [z$] ;" +
               "$s{ab    > 01 ;" +
               "   ab    > |8 ;" +
               "    b    > k ;" +
               "   8x}$s > 45 ;" +
               "   8x    > 77 ;",

               "abzababbabxzabxabx",
               "01z018k45z01x45");
    }

    /**
     * Test pattern quoting and escape mechanisms.
     */
    public void TestPatternQuoting() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "\u4E01>'[male adult]'", "\u4E01", "[male adult]",
        };

        for (int i=0; i<DATA.length; i+=3) {
            logln("Pattern: " + Utility.escape(DATA[i]));
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Regression test for bugs found in Greek transliteration.
     */
    public void TestJ277() {
        Transliterator gl = Transliterator.getInstance("Greek-Latin");
        
        char sigma = (char)0x3C3;
        char upsilon = (char)0x3C5;
        char nu = (char)0x3BD;
        char PHI = (char)0x3A6;
        char alpha = (char)0x3B1;
        char omega = (char)0x3C9;
        char omicron = (char)0x3BF;
        char epsilon = (char)0x3B5;

        // sigma upsilon nu -> syn
        StringBuffer buf = new StringBuffer();
        buf.append(sigma).append(upsilon).append(nu);
        String syn = buf.toString();
        expect(gl, syn, "syn");

        // sigma alpha upsilon nu -> saun
        buf.setLength(0);
        buf.append(sigma).append(alpha).append(upsilon).append(nu);
        String sayn = buf.toString();
        expect(gl, sayn, "saun");

        // Again, using a smaller rule set
        String rules =
                    "$alpha   = \u03B1;" +
                    "$nu      = \u03BD;" +
                    "$sigma   = \u03C3;" +
                    "$ypsilon = \u03C5;" +
                    "$vowel   = [aeiouAEIOU$alpha$ypsilon];" +
                    "s <>           $sigma;" +
                    "a <>           $alpha;" +
                    "u <>  $vowel { $ypsilon;" +
                    "y <>           $ypsilon;" +
                    "n <>           $nu;";
        RuleBasedTransliterator mini = new RuleBasedTransliterator
            ("mini", rules, Transliterator.REVERSE, null);
        expect(mini, syn, "syn");
        expect(mini, sayn, "saun");

//|    // Transliterate the Greek locale data
//|    Locale el("el");
//|    DateFormatSymbols syms(el, status);
//|    if (U_FAILURE(status)) { errln("FAIL: Transliterator constructor failed"); return; }
//|    int32_t i, count;
//|    const UnicodeString* data = syms.getMonths(count);
//|    for (i=0; i<count; ++i) {
//|        if (data[i].length() == 0) {
//|            continue;
//|        }
//|        UnicodeString out(data[i]);
//|        gl->transliterate(out);
//|        bool_t ok = TRUE;
//|        if (data[i].length() >= 2 && out.length() >= 2 &&
//|            u_isupper(data[i].charAt(0)) && u_islower(data[i].charAt(1))) {
//|            if (!(u_isupper(out.charAt(0)) && u_islower(out.charAt(1)))) {
//|                ok = FALSE;
//|            }
//|        }
//|        if (ok) {
//|            logln(prettify(data[i] + " -> " + out));
//|        } else {
//|            errln(UnicodeString("FAIL: ") + prettify(data[i] + " -> " + out));
//|        }
//|    }
    }

    /**
     * Prefix, suffix support in hex transliterators
     */
    public void TestJ243() {
        // Test default Hex-Unicode, which should handle
        // \\u, \\U, u+, and U+
        HexToUnicodeTransliterator hex = new HexToUnicodeTransliterator();
        expect(hex, "\\u0041+\\U0042,u+0043uu+0044z", "A+B,CuDz");

        // Try a custom Hex-Unicode
        // \\uXXXX and &#xXXXX;
        HexToUnicodeTransliterator hex2 = new HexToUnicodeTransliterator("\\\\u###0;&\\#x###0\\;"); 
        expect(hex2, "\\u61\\u062\\u0063\\u00645\\u66x&#x30;&#x031;&#x0032;&#x00033;",
               "abcd5fx012&#x00033;");

        // Try custom Unicode-Hex (default is tested elsewhere)
        UnicodeToHexTransliterator hex3 = new UnicodeToHexTransliterator("&\\#x###0;");
        expect(hex3, "012", "&#x30;&#x31;&#x32;");
    }

    /**
     * Test segments and segment references.
     */
    public void TestSegments() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "([a-z]) '.' ([0-9]) > $2 '-' $1",
            "abc.123.xyz.456",
            "ab1-c23.xy4-z56",
        };

        for (int i=0; i<DATA.length; i+=3) {
            logln("Pattern: " + Utility.escape(DATA[i]));
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Test cursor positioning outside of the key
     */
    public void TestCursorOffset() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "pre {alpha} post > | @ ALPHA ;" +
            "eALPHA > beta ;" +
            "pre {beta} post > BETA @@ | ;" +
            "post > xyz",

            "prealphapost prebetapost",
            "prbetaxyz preBETApost",
        };

        for (int i=0; i<DATA.length; i+=3) {
            logln("Pattern: " + Utility.escape(DATA[i]));
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Test zero length and > 1 char length variable values.  Test
     * use of variable refs in UnicodeSets.
     */
    public void TestArbitraryVariableValues() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "$abe = ab;" +
            "$pat = x[yY]z;" +
            "$ll  = 'a-z';" +
            "$llZ = [$ll];" +
            "$llY = [$ll$pat];" +
            "$emp = ;" +

            "$abe > ABE;" +
            "$pat > END;" +
            "$llZ > 1;" +
            "$llY > 2;" +
            "7$emp 8 > 9;" +
            "",

            "ab xYzxyz stY78",
            "ABE ENDEND 1129",
        };

        for (int i=0; i<DATA.length; i+=3) {
            logln("Pattern: " + Utility.escape(DATA[i]));
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[i]);
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }

    /**
     * Confirm that the contextStart, contextLimit, start, and limit
     * behave correctly.
     */
    public void TestPositionHandling() {
        // Array of 3n items
        // Each item is <rules>, <input>, <expected output>
        String[] DATA = {
            "a{t} > SS ; {t}b > UU ; {t} > TT ;",
            "xtat txtb", // pos 0,9,0,9
            "xTTaSS TTxUUb",

            "a{t} > SS ; {t}b > UU ; {t} > TT ;",
            "xtat txtb", // pos 2,9,3,8
            "xtaSS TTxUUb",

            "a{t} > SS ; {t}b > UU ; {t} > TT ;",
            "xtat txtb", // pos 3,8,3,8
            "xtaTT TTxTTb",
        };

        // Array of 4n positions -- these go with the DATA array
        // They are: contextStart, contextLimit, start, limit
        int[] POS = {
            0, 9, 0, 9,
            2, 9, 3, 8,
            3, 8, 3, 8,
        };

        int n = DATA.length/3;
        for (int i=0; i<n; i++) {
            Transliterator t = new RuleBasedTransliterator("<ID>", DATA[3*i]);
            Transliterator.Position pos = new Transliterator.Position(
                POS[4*i], POS[4*i+1], POS[4*i+2], POS[4*i+3]);
            ReplaceableString rsource = new ReplaceableString(DATA[3*i+1]);
            t.transliterate(rsource, pos);
            t.finishTransliteration(rsource, pos);
            String result = rsource.toString();
            String exp = DATA[3*i+2];
            expectAux(Utility.escape(DATA[3*i]),
                      DATA[3*i+1] + " -> " + result,
                      result.equals(exp),
                      exp);
        }
    }

    /**
     * Test the Hiragana-Katakana transliterator.
     */
    public void TestHiraganaKatakana() {
        Transliterator hk = Transliterator.getInstance("Hiragana-Katakana");
        Transliterator kh = Transliterator.getInstance("Katakana-Hiragana");

        // Array of 3n items
        // Each item is "hk"|"kh"|"both", <Hiragana>, <Katakana>
        String[] DATA = {
            "both",
            "\u3042\u3090\u3099\u3092\u3050",
            "\u30A2\u30F8\u30F2\u30B0",

            "kh",
            "\u307C\u3051\u3060\u3042\u3093\u30FC",
            "\u30DC\u30F6\u30C0\u30FC\u30F3\u30FC",
        };

        for (int i=0; i<DATA.length; i+=3) {
            switch (DATA[i].charAt(0)) {
            case 'h': // Hiragana-Katakana
                expect(hk, DATA[i+1], DATA[i+2]);
                break;
            case 'k': // Katakana-Hiragana
                expect(kh, DATA[i+2], DATA[i+1]);
                break;
            case 'b': // both
                expect(hk, DATA[i+1], DATA[i+2]);
                expect(kh, DATA[i+2], DATA[i+1]);
                break;
            }
        }

    }

    /**
     * Test inter-Indic transliterators.  These are composed.
     */
    public void TestInterIndic() {
        String ID = "Devanagari-Gujarati";
        Transliterator dg = Transliterator.getInstance(ID);
        if (dg == null) {
            errln("FAIL: getInstance(" + ID + ") returned null");
            return;
        }
        String id = dg.getID();
        if (!id.equals(ID)) {
            errln("FAIL: getInstance(" + ID + ").getID() => " + id);
        }
        String dev = "\u0901\u090B\u0925";
        String guj = "\u0A81\u0A8B\u0AA5";
        expect(dg, dev, guj);
    }

    /**
     * Test IDs of inverses of compound transliterators. (J20)
     */
    public void TestCompoundInverseID() {
        String ID = "Latin-Jamo;Jamo-Hangul";
        Transliterator t = Transliterator.getInstance(ID);
        Transliterator u = t.getInverse();
        String exp = "Hangul-Jamo;Jamo-Latin";
        String got = u.getID();
        if (!got.equals(exp)) {
            errln("FAIL: Inverse of " + ID + " is " + got +
                  ", expected " + exp);
        }
    }

    /**
     * Inverse of "Null" should be "Null". (J21)
     */
    public void TestNullInverse() {
        Transliterator t = Transliterator.getInstance("Null");
        Transliterator u = t.getInverse();
        if (!u.getID().equals("Null")) {
            errln("FAIL: Inverse of Null should be Null");
        }
    }

    /**
     * Check ID of inverse of alias. (J22)
     */
    public void TestAliasInverseID() {
        String ID = "Latin-Hangul"; // This should be any alias ID with an inverse
        Transliterator t = Transliterator.getInstance(ID);
        Transliterator u = t.getInverse();
        String exp = "Hangul-Latin";
        String got = u.getID();
        if (!got.equals(exp)) {
            errln("FAIL: Inverse of " + ID + " is " + got +
                  ", expected " + exp);
        }        
    }

    /**
     * Test filter syntax in IDs. (J23)
     */
    public void TestFilterIDs() {
        String[] DATA = {
            "Unicode-Hex[aeiou]",
            "Hex-Unicode[aeiou]",
            "quizzical",
            "q\\u0075\\u0069zz\\u0069c\\u0061l",
            
            "Unicode-Hex[aeiou];Hex-Unicode[^5]",
            "Unicode-Hex[^5];Hex-Unicode[aeiou]",
            "quizzical",
            "q\\u0075izzical",
            
            "Null[abc]",
            "Null[abc]",
            "xyz",
            "xyz",
        };
        
        for (int i=0; i<DATA.length; i+=4) {
            String ID = DATA[i];
            Transliterator t = Transliterator.getInstance(ID);
            expect(t, DATA[i+2], DATA[i+3]);

            // Check the ID
            if (!ID.equals(t.getID())) {
                errln("FAIL: getInstance(" + ID + ").getID() => " +
                      t.getID());
            }

            // Check the inverse
            String uID = DATA[i+1];
            Transliterator u = t.getInverse();
            if (u == null) {
                errln("FAIL: " + ID + ".getInverse() returned NULL");
            } else if (!u.getID().equals(uID)) {
                errln("FAIL: " + ID + ".getInverse().getID() => " +
                      u.getID() + ", expected " + uID);
            }
        }
    }

    /**
     * Test the "Remove" transliterator.
     */
    public void TestRemove() {
        Transliterator t = Transliterator.getInstance("Remove[aeiou]");
        expect(t, "The quick brown fox.",
               "Th qck brwn fx.");
    }

    /**
     * Test the case mapping transliterators.
     */
    public void TestCaseMap() {
        Transliterator toUpper =
            Transliterator.getInstance("Any-Upper[^xyzXYZ]");
        Transliterator toLower =
            Transliterator.getInstance("Any-Lower[^xyzXYZ]");
        Transliterator toTitle =
            Transliterator.getInstance("Any-Title[^xyzXYZ]");
        
        expect(toUpper, "The quick brown fox jumped over the lazy dogs.",
               "THE QUICK BROWN FOx JUMPED OVER THE LAzy DOGS.");
        expect(toLower, "The quIck brown fOX jUMPED OVER THE LAzY dogs.",
               "the quick brown foX jumped over the lazY dogs.");
        expect(toTitle, "the quick brown foX caN'T jump over the laZy dogs.",
               "The Quick Brown FoX Can't Jump Over The LaZy Dogs.");
    }

    /**
     * Test the name mapping transliterators.
     */
    public void TestNameMap() {
        Transliterator uni2name =
            Transliterator.getInstance("Any-Name[^abc]");
        Transliterator name2uni =
            Transliterator.getInstance("Name-Any");
        
        /// NOTE NOTE NOTE NOTE NOTE NOTE NOTE
        // The results in icu4j and icu4c are different:
        // icu4c: CJK UNIFIED IDEOGRAPH-4E01
        // icu4j: CJK UNIFIED IDEOGRAPH-4e01

        expect(uni2name, "\u00A0abc\u4E01\u00B5\u0A81\uFFFD\uFFFF",
               "{NO-BREAK SPACE}abc{CJK UNIFIED IDEOGRAPH-4e01}{MICRO SIGN}{GUJARATI SIGN CANDRABINDU}{REPLACEMENT CHARACTER}\uFFFF");
        expect(name2uni, "{ NO-BREAK SPACE}abc{  CJK UNIFIED  IDEOGRAPH-4E01  }{x{MICRO SIGN}{GUJARATI SIGN CANDRABINDU}{REPLACEMENT CHARACTER}{",
               "\u00A0abc\u4E01{x\u00B5\u0A81\uFFFD{");
    }

    /**
     * Test the normalization transliterator.
     */
    public void TestNormalizationTransliterator() {
        // THE FOLLOWING TWO TABLES ARE COPIED FROM com.ibm.test.normalizer.BasicTest
        // PLEASE KEEP THEM IN SYNC WITH BasicTest.
        String[][] CANON = {
            // Input               Decomposed            Composed
            {"cat",                "cat",                "cat"               },
            {"\u00e0ardvark",      "a\u0300ardvark",     "\u00e0ardvark",    },
                                                         
            {"\u1e0a",             "D\u0307",            "\u1e0a"            }, // D-dot_above
            {"D\u0307",            "D\u0307",            "\u1e0a"            }, // D dot_above
                                                         
            {"\u1e0c\u0307",       "D\u0323\u0307",      "\u1e0c\u0307"      }, // D-dot_below dot_above
            {"\u1e0a\u0323",       "D\u0323\u0307",      "\u1e0c\u0307"      }, // D-dot_above dot_below
            {"D\u0307\u0323",      "D\u0323\u0307",      "\u1e0c\u0307"      }, // D dot_below dot_above
                                                         
            {"\u1e10\u0307\u0323", "D\u0327\u0323\u0307","\u1e10\u0323\u0307"}, // D dot_below cedilla dot_above
            {"D\u0307\u0328\u0323","D\u0328\u0323\u0307","\u1e0c\u0328\u0307"}, // D dot_above ogonek dot_below
                                                         
            {"\u1E14",             "E\u0304\u0300",      "\u1E14"            }, // E-macron-grave
            {"\u0112\u0300",       "E\u0304\u0300",      "\u1E14"            }, // E-macron + grave
            {"\u00c8\u0304",       "E\u0300\u0304",      "\u00c8\u0304"      }, // E-grave + macron
                                                         
            {"\u212b",             "A\u030a",            "\u00c5"            }, // angstrom_sign
            {"\u00c5",             "A\u030a",            "\u00c5"            }, // A-ring
                                                         
            {"\u00fdffin",         "y\u0301ffin",        "\u00fdffin"        },	//updated with 3.0
            {"\u00fd\uFB03n",      "y\u0301\uFB03n",     "\u00fd\uFB03n"     },	//updated with 3.0
                                                         
            {"Henry IV",           "Henry IV",           "Henry IV"          },
            {"Henry \u2163",       "Henry \u2163",       "Henry \u2163"      },
                                                         
            {"\u30AC",             "\u30AB\u3099",       "\u30AC"            }, // ga (Katakana)
            {"\u30AB\u3099",       "\u30AB\u3099",       "\u30AC"            }, // ka + ten
            {"\uFF76\uFF9E",       "\uFF76\uFF9E",       "\uFF76\uFF9E"      }, // hw_ka + hw_ten
            {"\u30AB\uFF9E",       "\u30AB\uFF9E",       "\u30AB\uFF9E"      }, // ka + hw_ten
            {"\uFF76\u3099",       "\uFF76\u3099",       "\uFF76\u3099"      }, // hw_ka + ten
                                                         
            {"A\u0300\u0316",      "A\u0316\u0300",      "\u00C0\u0316"      },
        };                                                
                                                          
        String[][] COMPAT = {                        
            // Input               Decomposed            Composed
            {"\uFB4f",             "\u05D0\u05DC",       "\u05D0\u05DC",     }, // Alef-Lamed vs. Alef, Lamed
                                                         
            {"\u00fdffin",         "y\u0301ffin",        "\u00fdffin"        },	//updated for 3.0
            {"\u00fd\uFB03n",      "y\u0301ffin",        "\u00fdffin"        }, // ffi ligature -> f + f + i
                                                         
            {"Henry IV",           "Henry IV",           "Henry IV"          },
            {"Henry \u2163",       "Henry IV",           "Henry IV"          },
                                                         
            {"\u30AC",             "\u30AB\u3099",       "\u30AC"            }, // ga (Katakana)
            {"\u30AB\u3099",       "\u30AB\u3099",       "\u30AC"            }, // ka + ten
                                                         
            {"\uFF76\u3099",       "\u30AB\u3099",       "\u30AC"            }, // hw_ka + ten
        };

        Transliterator NFD = Transliterator.getInstance("NFD");
        Transliterator NFC = Transliterator.getInstance("NFC");
        for (int i=0; i<CANON.length; ++i) {
            String in = CANON[i][0];
            String expd = CANON[i][1];
            String expc = CANON[i][2];
            expect(NFD, in, expd);
            expect(NFC, in, expc);
        }

        Transliterator NFKD = Transliterator.getInstance("NFKD");
        Transliterator NFKC = Transliterator.getInstance("NFKC");
        for (int i=0; i<COMPAT.length; ++i) {
            String in = COMPAT[i][0];
            String expkd = COMPAT[i][1];
            String expkc = COMPAT[i][2];
            expect(NFKD, in, expkd);
            expect(NFKC, in, expkc);
        }
    }
    
    //======================================================================
    // Support methods
    //======================================================================

    void expect(String rules, String source, String expectedResult) {
        expect(new RuleBasedTransliterator("<ID>", rules), source, expectedResult);
    }

    void expect(Transliterator t, String source, String expectedResult,
                Transliterator reverseTransliterator) {
        expect(t, source, expectedResult);
        if (reverseTransliterator != null) {
            expect(reverseTransliterator, expectedResult, source);
        }
    }

    void expect(Transliterator t, String source, String expectedResult) {
        String result = t.transliterate(source);
        expectAux(t.getID() + ":String", source, result, expectedResult);

        ReplaceableString rsource = new ReplaceableString(source);
        t.transliterate(rsource);
        result = rsource.toString();
        expectAux(t.getID() + ":Replaceable", source, result, expectedResult);

        // Test keyboard (incremental) transliteration -- this result
        // must be the same after we finalize (see below).
        rsource.replace(0, rsource.length(), "");
        Transliterator.Position index = new Transliterator.Position();
        StringBuffer log = new StringBuffer();

        for (int i=0; i<source.length(); ++i) {
            if (i != 0) {
                log.append(" + ");
            }
            log.append(source.charAt(i)).append(" -> ");
            t.transliterate(rsource, index,
                            String.valueOf(source.charAt(i)));
            // Append the string buffer with a vertical bar '|' where
            // the committed index is.
            String s = rsource.toString();
            log.append(s.substring(0, index.start)).
                append('|').
                append(s.substring(index.start));
        }
        
        // As a final step in keyboard transliteration, we must call
        // transliterate to finish off any pending partial matches that
        // were waiting for more input.
        t.finishTransliteration(rsource, index);
        result = rsource.toString();
        log.append(" => ").append(rsource.toString());

        expectAux(t.getID() + ":Keyboard", log.toString(),
                  result.equals(expectedResult),
                  expectedResult);
    }

    void expectAux(String tag, String source,
                   String result, String expectedResult) {
        expectAux(tag, source + " -> " + result,
                  result.equals(expectedResult),
                  expectedResult);
    }
    
    void expectAux(String tag, String summary, boolean pass,
                   String expectedResult) {
        if (pass) {
            logln("("+tag+") " + Utility.escape(summary));
        } else {
            errln("FAIL: ("+tag+") "
                  + Utility.escape(summary)
                  + ", expected " + Utility.escape(expectedResult));
        }
    }
}
