/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/test/normalizer/Attic/BasicTest.java,v $ 
 * $Date: 2001/04/02 19:21:06 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.test.normalizer;

import com.ibm.test.*;
import com.ibm.text.*;
import com.ibm.util.Utility;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class BasicTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new BasicTest().run(args);
    }

    String[][] canonTests = {
        // Input                Decomposed              Composed
        { "cat",                "cat",                  "cat"               },
        { "\u00e0ardvark",      "a\u0300ardvark",       "\u00e0ardvark",    },

        { "\u1e0a",             "D\u0307",              "\u1e0a"            }, // D-dot_above
        { "D\u0307",            "D\u0307",              "\u1e0a"            }, // D dot_above

        { "\u1e0c\u0307",       "D\u0323\u0307",        "\u1e0c\u0307"      }, // D-dot_below dot_above
        { "\u1e0a\u0323",       "D\u0323\u0307",        "\u1e0c\u0307"      }, // D-dot_above dot_below
        { "D\u0307\u0323",      "D\u0323\u0307",        "\u1e0c\u0307"      }, // D dot_below dot_above

        { "\u1e10\u0307\u0323", "D\u0327\u0323\u0307",  "\u1e10\u0323\u0307"}, // D dot_below cedilla dot_above
        { "D\u0307\u0328\u0323","D\u0328\u0323\u0307",  "\u1e0c\u0328\u0307"}, // D dot_above ogonek dot_below

        { "\u1E14",             "E\u0304\u0300",        "\u1E14"            }, // E-macron-grave
        { "\u0112\u0300",       "E\u0304\u0300",        "\u1E14"            }, // E-macron + grave
        { "\u00c8\u0304",       "E\u0300\u0304",        "\u00c8\u0304"      }, // E-grave + macron

        { "\u212b",             "A\u030a",              "\u00c5"            }, // angstrom_sign
        { "\u00c5",             "A\u030a",              "\u00c5"            }, // A-ring

//        { "\u00fdffin",              "A\u0308ffin",          "\u00fdffin"             },
        { "\u00fdffin",              "y\u0301ffin",          "\u00fdffin"             },	//updated with 3.0
//        { "\u00fd\uFB03n",           "A\u0308\uFB03n",       "\u00fd\uFB03n"          },
        { "\u00fd\uFB03n",           "y\u0301\uFB03n",       "\u00fd\uFB03n"          },	//updated with 3.0

        { "Henry IV",           "Henry IV",             "Henry IV"          },
        { "Henry \u2163",       "Henry \u2163",         "Henry \u2163"      },

        { "\u30AC",             "\u30AB\u3099",         "\u30AC"            }, // ga (Katakana)
        { "\u30AB\u3099",       "\u30AB\u3099",         "\u30AC"            }, // ka + ten
        { "\uFF76\uFF9E",       "\uFF76\uFF9E",         "\uFF76\uFF9E"      }, // hw_ka + hw_ten
        { "\u30AB\uFF9E",       "\u30AB\uFF9E",         "\u30AB\uFF9E"      }, // ka + hw_ten
        { "\uFF76\u3099",       "\uFF76\u3099",         "\uFF76\u3099"      }, // hw_ka + ten

        { "A\u0300\u0316", "A\u0316\u0300", "\u00C0\u0316" },
    };

    String[][] compatTests = {
            // Input                Decomposed              Composed
        { "\uFB4f",             "\u05D0\u05DC",         "\u05D0\u05DC",     }, // Alef-Lamed vs. Alef, Lamed

//        { "\u00fdffin",              "A\u0308ffin",          "\u00fdffin"             },
//       { "\u00fd\uFB03n",           "A\u0308ffin",          "\u00fdffin"             }, // ffi ligature -> f + f + i
        { "\u00fdffin",              "y\u0301ffin",          "\u00fdffin"             },	//updated for 3.0
        { "\u00fd\uFB03n",           "y\u0301ffin",          "\u00fdffin"             }, // ffi ligature -> f + f + i

        { "Henry IV",           "Henry IV",             "Henry IV"          },
        { "Henry \u2163",       "Henry IV",             "Henry IV"          },

        { "\u30AC",             "\u30AB\u3099",         "\u30AC"            }, // ga (Katakana)
        { "\u30AB\u3099",       "\u30AB\u3099",         "\u30AC"            }, // ka + ten

        { "\uFF76\u3099",       "\u30AB\u3099",         "\u30AC"            }, // hw_ka + ten

        /* These two are broken in Unicode 2.1.2 but fixed in 2.1.5 and later
        { "\uFF76\uFF9E",       "\u30AB\u3099",         "\u30AC"            }, // hw_ka + hw_ten
        { "\u30AB\uFF9E",       "\u30AB\u3099",         "\u30AC"            }, // ka + hw_ten
        */
    };

    // With Canonical decomposition, Hangul syllables should get decomposed
    // into Jamo, but Jamo characters should not be decomposed into
    // conjoining Jamo
    String[][] hangulCanon = {
        // Input                Decomposed              Composed
        { "\ud4db",             "\u1111\u1171\u11b6",   "\ud4db"        },
        { "\u1111\u1171\u11b6", "\u1111\u1171\u11b6",   "\ud4db"        },
    };

    // With compatibility decomposition turned on,
    // it should go all the way down to conjoining Jamo characters.
    // THIS IS NO LONGER TRUE IN UNICODE v2.1.8, SO THIS TEST IS OBSOLETE
    String[][] hangulCompat = {
        // Input        Decomposed                          Composed
        // { "\ud4db",     "\u1111\u116e\u1175\u11af\u11c2",   "\ud478\u1175\u11af\u11c2"  },
    };

    public void TestHangulCompose() {
        // Make sure that the static composition methods work
        logln("Canonical composition...");
        staticTest(Normalizer.COMPOSE,        0, hangulCanon,  2);
        logln("Compatibility composition...");
        staticTest(Normalizer.COMPOSE_COMPAT, 0, hangulCompat, 2);

        // Now try iterative composition....
        logln("Static composition...");
        Normalizer norm = new Normalizer("", Normalizer.COMPOSE, 0);
        iterateTest(norm, hangulCanon, 2);

        norm.setMode(Normalizer.COMPOSE_COMPAT);
        iterateTest(norm, hangulCompat, 2);

        // And finally, make sure you can do it in reverse too
        logln("Reverse iteration...");
        norm.setMode(Normalizer.COMPOSE);
        backAndForth(norm, hangulCanon);
    }

    public void TestHangulDecomp() {
        // Make sure that the static decomposition methods work
        logln("Canonical decomposition...");
        staticTest(Normalizer.DECOMP,        0, hangulCanon,  1);
        logln("Compatibility decomposition...");
        staticTest(Normalizer.DECOMP_COMPAT, 0, hangulCompat, 1);

        // Now the iterative decomposition methods...
        logln("Iterative decomposition...");
        Normalizer norm = new Normalizer("", Normalizer.DECOMP, 0);
        iterateTest(norm, hangulCanon, 1);

        norm.setMode(Normalizer.DECOMP_COMPAT);
        iterateTest(norm, hangulCompat, 1);

        // And finally, make sure you can do it in reverse too
        logln("Reverse iteration...");
        norm.setMode(Normalizer.DECOMP);
        backAndForth(norm, hangulCanon);
    }

    public void TestPrevious() {
        Normalizer norm = new Normalizer("", Normalizer.DECOMP, 0);

        logln("testing decomp...");
        backAndForth(norm, canonTests);

        logln("testing compose...");
        norm.setMode(Normalizer.COMPOSE);
        backAndForth(norm, canonTests);
    }

    public void TestDecomp() {
        Normalizer norm = new Normalizer("", Normalizer.DECOMP, 0);
        iterateTest(norm, canonTests, 1);

        staticTest(Normalizer.DECOMP, 0, canonTests, 1);
    }

    public void TestCompatDecomp() {
        Normalizer norm = new Normalizer("", Normalizer.DECOMP_COMPAT, 0);
        iterateTest(norm, compatTests, 1);

        staticTest(Normalizer.DECOMP_COMPAT, 0, compatTests, 1);
    }

    public void TestCanonCompose() {
        Normalizer norm = new Normalizer("", Normalizer.COMPOSE, 0);
        iterateTest(norm, canonTests, 2);

        staticTest(Normalizer.COMPOSE, 0, canonTests, 2);
    }

    public void TestCompatCompose() {
        Normalizer norm = new Normalizer("", Normalizer.COMPOSE_COMPAT, 0);
        iterateTest(norm, compatTests, 2);

        staticTest(Normalizer.COMPOSE_COMPAT, 0, compatTests, 2);
    }

    public void TestExplodingBase() {
        // \u017f - Latin small letter long s
        // \u0307 - combining dot above
        // \u1e61 - Latin small letter s with dot above
        // \u1e9b - Latin small letter long s with dot above
        String[][] canon = {
            // Input                Decomposed              Composed
            { "Tschu\u017f",        "Tschu\u017f",          "Tschu\u017f"       },
            { "Tschu\u1e9b",        "Tschu\u017f\u0307",    "Tschu\u1e9b"       },
        };
        String[][] compat = {
            // Input                Decomposed              Composed
            { "\u017f",        "s",              "s"           },
            { "\u1e9b",        "s\u0307",        "\u1e61"      },
        };

        staticTest(Normalizer.DECOMP,           0, canon,  1);
        staticTest(Normalizer.COMPOSE,          0, canon,  2);

        staticTest(Normalizer.DECOMP_COMPAT,    0, compat, 1);
        staticTest(Normalizer.COMPOSE_COMPAT,   0, compat, 2);

        Normalizer norm = new Normalizer("", Normalizer.DECOMP_COMPAT);
        iterateTest(norm, compat, 1);
        backAndForth(norm, compat);

        norm.setMode(Normalizer.COMPOSE_COMPAT);
        iterateTest(norm, compat, 2);
        backAndForth(norm, compat);
    }

    /**
     * The Tibetan vowel sign AA, 0f71, was messed up prior to Unicode version 2.1.9.
     * Once 2.1.9 or 3.0 is released, uncomment this test.
     */
    public void TestTibetan() {
        String[][] decomp = {
            { "\u0f77", "\u0f77", "\u0fb2\u0f71\u0f80" }
        };
        String[][] compose = {
            { "\u0fb2\u0f71\u0f80", "\u0fb2\u0f71\u0f80", "\u0fb2\u0f71\u0f80" }
        };

        staticTest(Normalizer.DECOMP,           0, decomp, 1);
        staticTest(Normalizer.DECOMP_COMPAT,    0, decomp, 2);
        staticTest(Normalizer.COMPOSE,          0, compose, 1);
        staticTest(Normalizer.COMPOSE_COMPAT,   0, compose, 2);
    }

    /**
     * Make sure characters in the CompositionExclusion.txt list do not get
     * composed to.
     */
    public void TestCompositionExclusion() {
        // This list is generated from CompositionExclusion.txt.
        // Update whenever the normalizer tables are updated.  Note
        // that we test all characters listed, even those that can be
        // derived from the Unicode DB and are therefore commented
        // out.
        String EXCLUDED = 
            "\u0340\u0341\u0343\u0344\u0374\u037E\u0387\u0958" +
            "\u0959\u095A\u095B\u095C\u095D\u095E\u095F\u09DC" +
            "\u09DD\u09DF\u0A33\u0A36\u0A59\u0A5A\u0A5B\u0A5E" +
            "\u0B5C\u0B5D\u0F43\u0F4D\u0F52\u0F57\u0F5C\u0F69" +
            "\u0F73\u0F75\u0F76\u0F78\u0F81\u0F93\u0F9D\u0FA2" +
            "\u0FA7\u0FAC\u0FB9\u1F71\u1F73\u1F75\u1F77\u1F79" +
            "\u1F7B\u1F7D\u1FBB\u1FBE\u1FC9\u1FCB\u1FD3\u1FDB" +
            "\u1FE3\u1FEB\u1FEE\u1FEF\u1FF9\u1FFB\u1FFD\u2000" +
            "\u2001\u2126\u212A\u212B\u2329\u232A\uF900\uFA10" +
            "\uFA12\uFA15\uFA20\uFA22\uFA25\uFA26\uFA2A\uFB1F" +
            "\uFB2A\uFB2B\uFB2C\uFB2D\uFB2E\uFB2F\uFB30\uFB31" +
            "\uFB32\uFB33\uFB34\uFB35\uFB36\uFB38\uFB39\uFB3A" +
            "\uFB3B\uFB3C\uFB3E\uFB40\uFB41\uFB43\uFB44\uFB46" +
            "\uFB47\uFB48\uFB49\uFB4A\uFB4B\uFB4C\uFB4D\uFB4E";
        for (int i=0; i<EXCLUDED.length(); ++i) {
            String a = String.valueOf(EXCLUDED.charAt(i));
            String b = Normalizer.normalize(a, Normalizer.DECOMP_COMPAT, 0);
            String c = Normalizer.normalize(b, Normalizer.COMPOSE, 0);
            if (c.equals(a)) {
                errln("FAIL: " + hex(a) + " x DECOMP_COMPAT => " +
                      hex(b) + " x COMPOSE => " +
                      hex(c));
            } else if (isVerbose()) {
                logln("Ok: " + hex(a) + " x DECOMP_COMPAT => " +
                      hex(b) + " x COMPOSE => " +
                      hex(c));                
            }
        }
        // The following method works too, but it is somewhat
        // incestuous.  It uses UInfo, which is the same database that
        // NormalizerBuilder uses, so if something is wrong with
        // UInfo, the following test won't show it.  All it will show
        // is that NormalizerBuilder has been run with whatever the
        // current UInfo is.
        // 
        // We comment this out in favor of the test above, which
        // provides independent verification (but also requires
        // independent updating).
//      logln("---");
//      UInfo uinfo = new UInfo();
//      for (int i=0; i<=0xFFFF; ++i) {
//          if (!uinfo.isExcludedComposition((char)i) ||
//              (!uinfo.hasCanonicalDecomposition((char)i) &&
//               !uinfo.hasCompatibilityDecomposition((char)i))) continue;
//          String a = String.valueOf((char)i);
//          String b = Normalizer.normalize(a, Normalizer.DECOMP_COMPAT, 0);
//          String c = Normalizer.normalize(b, Normalizer.COMPOSE, 0);
//          if (c.equals(a)) {
//              errln("FAIL: " + hex(a) + " x DECOMP_COMPAT => " +
//                    hex(b) + " x COMPOSE => " +
//                    hex(c));
//          } else if (isVerbose()) {
//              logln("Ok: " + hex(a) + " x DECOMP_COMPAT => " +
//                    hex(b) + " x COMPOSE => " +
//                    hex(c));                
//          }
//      }
    }

    /**
     * Test for a problem that showed up just before ICU 1.6 release
     * having to do with combining characters with an index of zero.
     * Such characters do not participate in any canonical
     * decompositions.  However, having an index of zero means that
     * they all share one typeMask[] entry, that is, they all have to
     * map to the same canonical class, which is not the case, in
     * reality.
     */
    public void TestZeroIndex() {
        String[] DATA = {
            // Expect col1 x COMPOSE_COMPAT => col2
            // Expect col2 x DECOMP => col3
            "A\u0316\u0300", "\u00C0\u0316", "A\u0316\u0300",
            "A\u0300\u0316", "\u00C0\u0316", "A\u0316\u0300",
            "A\u0327\u0300", "\u00C0\u0327", "A\u0327\u0300",
            "c\u0321\u0327", "c\u0321\u0327", "c\u0321\u0327",
            "c\u0327\u0321", "\u00E7\u0321", "c\u0327\u0321",
        };

        for (int i=0; i<DATA.length; i+=3) {
            String a = DATA[i];
            String b = Normalizer.normalize(a, Normalizer.COMPOSE_COMPAT, 0);
            String exp = DATA[i+1];
            if (b.equals(exp)) {
                logln("Ok: " + hex(a) + " x COMPOSE_COMPAT => " + hex(b));
            } else {
                errln("FAIL: " + hex(a) + " x COMPOSE_COMPAT => " + hex(b) +
                      ", expect " + hex(exp));
            }
            a = Normalizer.normalize(b, Normalizer.DECOMP, 0);
            exp = DATA[i+2];
            if (a.equals(exp)) {
                logln("Ok: " + hex(b) + " x DECOMP => " + hex(a));
            } else {
                errln("FAIL: " + hex(b) + " x DECOMP => " + hex(a) +
                      ", expect " + hex(exp));
            }
        }
    }

    /**
     * Test for a problem found by Verisign.  Problem is that
     * characters at the start of a string are not put in canonical
     * order correctly by compose() if there is no starter.
     */
    public void TestVerisign() {
        String[] inputs = {
            "\u05b8\u05b9\u05b1\u0591\u05c3\u05b0\u05ac\u059f",
            "\u0592\u05b7\u05bc\u05a5\u05b0\u05c0\u05c4\u05ad"
        };
        String[] outputs = {
            "\u05b1\u05b8\u05b9\u0591\u05c3\u05b0\u05ac\u059f",
            "\u05b0\u05b7\u05bc\u05a5\u0592\u05c0\u05ad\u05c4"
        };

        for (int i = 0; i < inputs.length; ++i) {
            String input = inputs[i];
            String output = outputs[i];
            String result = Normalizer.decompose(input, false, 0);
            if (!result.equals(output)) {
                errln("FAIL input: " + Utility.escape(input));
                errln(" decompose: " + Utility.escape(result));
                errln("  expected: " + Utility.escape(output));
            }
            result = Normalizer.compose(input, false, 0);
            if (!result.equals(output)) {
                errln("FAIL input: " + Utility.escape(input));
                errln("   compose: " + Utility.escape(result));
                errln("  expected: " + Utility.escape(output));
            }
        }
    }

    //------------------------------------------------------------------------
    // Internal utilities
    //

    private void backAndForth(Normalizer iter, String input)
    {
        iter.setText(input);

        // Run through the iterator forwards and stick it into a StringBuffer
        StringBuffer forward =  new StringBuffer();
        for (char ch = iter.first(); ch != iter.DONE; ch = iter.next()) {
            forward.append(ch);
        }

        // Now do it backwards
        StringBuffer reverse = new StringBuffer();
        for (char ch = iter.last(); ch != iter.DONE; ch = iter.previous()) {
            reverse.insert(0, ch);
        }

        if (!forward.toString().equals(reverse.toString())) {
            errln("FAIL: Forward/reverse mismatch for input " + hex(input)
                  + ", forward: " + hex(forward) + ", backward: " + hex(reverse));
        } else if (isVerbose()) {
            logln("Ok: Forward/reverse for input " + hex(input)
                  + ", forward: " + hex(forward) + ", backward: " + hex(reverse));
        }
    }

    private void backAndForth(Normalizer iter, String[][] tests)
    {
        for (int i = 0; i < tests.length; i++)
        {
            iter.setText(tests[i][0]);

            // Run through the iterator forwards and stick it into a StringBuffer
            StringBuffer forward =  new StringBuffer();
            for (char ch = iter.first(); ch != iter.DONE; ch = iter.next()) {
                forward.append(ch);
            }

            // Now do it backwards
            StringBuffer reverse = new StringBuffer();
            for (char ch = iter.last(); ch != iter.DONE; ch = iter.previous()) {
                reverse.insert(0, ch);
            }

            if (!forward.toString().equals(reverse.toString())) {
                errln("FAIL: Forward/reverse mismatch for input " + hex(tests[i][0])
                    + ", forward: " + hex(forward) + ", backward: " + hex(reverse));
            } else if (isVerbose()) {
                logln("Ok: Forward/reverse for input " + hex(tests[i][0])
                      + ", forward: " + hex(forward) + ", backward: " + hex(reverse));
            }
        }
    }

    private void staticTest(Normalizer.Mode mode, int options, String[][] tests, int outCol)
    {
        for (int i = 0; i < tests.length; i++)
        {
            String input = tests[i][0];
            String expect = tests[i][outCol];

            logln("Normalizing '" + input + "' (" + hex(input) + ")" );

            String output = Normalizer.normalize(input, mode, options);

            if (!output.equals(expect)) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + output + "' (" + hex(output) + ")" );
            }
        }
    }

    private void iterateTest(Normalizer iter, String[][] tests, int outCol)
    {
        for (int i = 0; i < tests.length; i++)
        {
            String input = tests[i][0];
            String expect = tests[i][outCol];

            logln("Normalizing '" + input + "' (" + hex(input) + ")" );

            iter.setText(input);
            assertEqual(expect, iter, "case " + i + " ");
        }
    }

    private void assertEqual(String expected, Normalizer iter, String msg)
    {
        int index = 0;
        for (char ch = iter.first(); ch != iter.DONE; ch = iter.next())
        {
            if (index >= expected.length()) {
                errln("FAIL: " + msg + "Unexpected character '" + ch + "' (" + hex(ch) + ")"
                        + " at index " + index);
                break;
            }
            char want = expected.charAt(index);
            if (ch != want) {
                errln("FAIL: " + msg + "got '" + ch + "' (" + hex(ch) + ")"
                        + " but expected '" + want + "' (" + hex(want) + ")"
                        + " at index " + index);
            }
            index++;
        }
        if (index < expected.length()) {
            errln("FAIL: " + msg + "Only got " + index + " chars, expected " + expected.length());
        }
    }
}
