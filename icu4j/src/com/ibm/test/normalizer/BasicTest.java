/*
 * $RCSfile: BasicTest.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:48 $
 *
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.test.normalizer;

import com.ibm.test.*;
import com.ibm.text.*;

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

        { "ýffin",              "A\u0308ffin",          "ýffin"             },
        { "ý\uFB03n",           "A\u0308\uFB03n",       "ý\uFB03n"          },

        { "Henry IV",           "Henry IV",             "Henry IV"          },
        { "Henry \u2163",       "Henry \u2163",         "Henry \u2163"      },

        { "\u30AC",             "\u30AB\u3099",         "\u30AC"            }, // ga (Katakana)
        { "\u30AB\u3099",       "\u30AB\u3099",         "\u30AC"            }, // ka + ten
        { "\uFF76\uFF9E",       "\uFF76\uFF9E",         "\uFF76\uFF9E"      }, // hw_ka + hw_ten
        { "\u30AB\uFF9E",       "\u30AB\uFF9E",         "\u30AB\uFF9E"      }, // ka + hw_ten
        { "\uFF76\u3099",       "\uFF76\u3099",         "\uFF76\u3099"      }, // hw_ka + ten
    };

    String[][] compatTests = {
            // Input                Decomposed              Composed
        { "\uFB4f",             "\u05D0\u05DC",         "\u05D0\u05DC",     }, // Alef-Lamed vs. Alef, Lamed

        { "ýffin",              "A\u0308ffin",          "ýffin"             },
        { "ý\uFB03n",           "A\u0308ffin",          "ýffin"             }, // ffi ligature -> f + f + i

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


    /** The Tibetan vowel sign AA, 0f71, was messed up prior to Unicode version 2.1.9
     & Once 2.1.9 or 3.0 is released, uncomment this test
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
    */


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
            errln("Forward/reverse mismatch for input " + hex(input)
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
                errln("Forward/reverse mismatch for input " + hex(tests[i][0])
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
                errln("ERROR: case " + i
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
            assertEqual(expect, iter, "ERROR: case " + i + " ");
        }
    }

    private void assertEqual(String expected, Normalizer iter, String errPrefix)
    {
        int index = 0;
        for (char ch = iter.first(); ch != iter.DONE; ch = iter.next())
        {
            if (index >= expected.length()) {
                errln(errPrefix + "Unexpected character '" + ch + "' (" + hex(ch) + ")"
                        + " at index " + index);
                break;
            }
            char want = expected.charAt(index);
            if (ch != want) {
                errln(errPrefix + "got '" + ch + "' (" + hex(ch) + ")"
                        + " but expected '" + want + "' (" + hex(want) + ")"
                        + " at index " + index);
            }
            index++;
        }
        if (index < expected.length()) {
            errln(errPrefix + "Only got " + index + " chars, expected " + expected.length());
        }
    }
}