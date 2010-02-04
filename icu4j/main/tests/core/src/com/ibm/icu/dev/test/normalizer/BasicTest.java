/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.dev.test.normalizer;

import java.text.StringCharacterIterator;
import java.util.Random;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.USerializedSet;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.*;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;


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

        { "\u00c4ffin",         "A\u0308ffin",          "\u00c4ffin"        },
        { "\u00c4\uFB03n",      "A\u0308\uFB03n",       "\u00c4\uFB03n"     },

        { "\u00fdffin",         "y\u0301ffin",          "\u00fdffin"        }, //updated with 3.0
        { "\u00fd\uFB03n",      "y\u0301\uFB03n",       "\u00fd\uFB03n"     }, //updated with 3.0

        { "Henry IV",           "Henry IV",             "Henry IV"          },
        { "Henry \u2163",       "Henry \u2163",         "Henry \u2163"      },

        { "\u30AC",             "\u30AB\u3099",         "\u30AC"            }, // ga (Katakana)
        { "\u30AB\u3099",       "\u30AB\u3099",         "\u30AC"            }, // ka + ten
        { "\uFF76\uFF9E",       "\uFF76\uFF9E",         "\uFF76\uFF9E"      }, // hw_ka + hw_ten
        { "\u30AB\uFF9E",       "\u30AB\uFF9E",         "\u30AB\uFF9E"      }, // ka + hw_ten
        { "\uFF76\u3099",       "\uFF76\u3099",         "\uFF76\u3099"      }, // hw_ka + ten

        { "A\u0300\u0316", "A\u0316\u0300", "\u00C0\u0316" },
        {"\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e","\\U0001D157\\U0001D165\\U0001D157\\U0001D165\\U0001D157\\U0001D165", "\\U0001D157\\U0001D165\\U0001D157\\U0001D165\\U0001D157\\U0001D165"},
    };

    String[][] compatTests = {
            // Input                Decomposed              Composed
        { "cat",                 "cat",                     "cat"           },
        { "\uFB4f",             "\u05D0\u05DC",         "\u05D0\u05DC",     }, // Alef-Lamed vs. Alef, Lamed

        { "\u00C4ffin",         "A\u0308ffin",          "\u00C4ffin"        },
        { "\u00C4\uFB03n",      "A\u0308ffin",          "\u00C4ffin"        }, // ffi ligature -> f + f + i

        { "\u00fdffin",         "y\u0301ffin",          "\u00fdffin"        },        //updated for 3.0
        { "\u00fd\uFB03n",      "y\u0301ffin",          "\u00fdffin"        }, // ffi ligature -> f + f + i

        { "Henry IV",           "Henry IV",             "Henry IV"          },
        { "Henry \u2163",       "Henry IV",             "Henry IV"          },

        { "\u30AC",             "\u30AB\u3099",         "\u30AC"            }, // ga (Katakana)
        { "\u30AB\u3099",       "\u30AB\u3099",         "\u30AC"            }, // ka + ten

        { "\uFF76\u3099",       "\u30AB\u3099",         "\u30AC"            }, // hw_ka + ten

        /* These two are broken in Unicode 2.1.2 but fixed in 2.1.5 and later*/
        { "\uFF76\uFF9E",       "\u30AB\u3099",         "\u30AC"            }, // hw_ka + hw_ten
        { "\u30AB\uFF9E",       "\u30AB\u3099",         "\u30AC"            }, // ka + hw_ten

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

    public void TestHangulCompose()
                throws Exception{
        // Make sure that the static composition methods work
        logln("Canonical composition...");
        staticTest(Normalizer.NFC, hangulCanon,  2);
        logln("Compatibility composition...");
        staticTest(Normalizer.NFKC, hangulCompat, 2);
        // Now try iterative composition....
        logln("Iterative composition...");
        Normalizer norm = new Normalizer("", Normalizer.NFC,0);
        iterateTest(norm, hangulCanon, 2);

        norm.setMode(Normalizer.NFKD);
        iterateTest(norm, hangulCompat, 2);

        // And finally, make sure you can do it in reverse too
        logln("Reverse iteration...");
        norm.setMode(Normalizer.NFC);
        backAndForth(norm, hangulCanon);
     }

    public void TestHangulDecomp() throws Exception{
        // Make sure that the static decomposition methods work
        logln("Canonical decomposition...");
        staticTest(Normalizer.NFD, hangulCanon,  1);
        logln("Compatibility decomposition...");
        staticTest(Normalizer.NFKD, hangulCompat, 1);

         // Now the iterative decomposition methods...
        logln("Iterative decomposition...");
        Normalizer norm = new Normalizer("", Normalizer.NFD,0);
        iterateTest(norm, hangulCanon, 1);

        norm.setMode(Normalizer.NFKD);
        iterateTest(norm, hangulCompat, 1);

        // And finally, make sure you can do it in reverse too
        logln("Reverse iteration...");
        norm.setMode(Normalizer.NFD);
        backAndForth(norm, hangulCanon);
    }
    public void TestNone() throws Exception{
        Normalizer norm = new Normalizer("", Normalizer.NONE,0);
        iterateTest(norm, canonTests, 0);
        staticTest(Normalizer.NONE, canonTests, 0);
    }
    public void TestDecomp() throws Exception{
        Normalizer norm = new Normalizer("", Normalizer.NFD,0);
        iterateTest(norm, canonTests, 1);
        staticTest(Normalizer.NFD, canonTests, 1);
        decomposeTest(Normalizer.NFD, canonTests, 1);
    }

    public void TestCompatDecomp() throws Exception{
        Normalizer norm = new Normalizer("", Normalizer.NFKD,0);
        iterateTest(norm, compatTests, 1);
        staticTest(Normalizer.NFKD,compatTests, 1);
        decomposeTest(Normalizer.NFKD,compatTests, 1);
    }

    public void TestCanonCompose() throws Exception{
        Normalizer norm = new Normalizer("", Normalizer.NFC,0);
        iterateTest(norm, canonTests, 2);
        staticTest(Normalizer.NFC, canonTests, 2);
        composeTest(Normalizer.NFC, canonTests, 2);
    }

    public void TestCompatCompose() throws Exception{
        Normalizer norm = new Normalizer("", Normalizer.NFKC,0);
        iterateTest(norm, compatTests, 2);
        staticTest(Normalizer.NFKC,compatTests, 2);
        composeTest(Normalizer.NFKC,compatTests, 2);
    }

    public void TestExplodingBase() throws Exception{
        // \u017f - Latin small letter long s
        // \u0307 - combining dot above
        // \u1e61 - Latin small letter s with dot above
        // \u1e9b - Latin small letter long s with dot above
        String[][] canon = {
            // Input                Decomposed              Composed
            { "Tschu\u017f",        "Tschu\u017f",          "Tschu\u017f"    },
            { "Tschu\u1e9b",        "Tschu\u017f\u0307",    "Tschu\u1e9b"    },
        };
        String[][] compat = {
            // Input                Decomposed              Composed
            { "\u017f",        "s",              "s"           },
            { "\u1e9b",        "s\u0307",        "\u1e61"      },
        };

        staticTest(Normalizer.NFD, canon,  1);
        staticTest(Normalizer.NFC, canon,  2);

        staticTest(Normalizer.NFKD, compat, 1);
        staticTest(Normalizer.NFKC, compat, 2);

    }

    /**
     * The Tibetan vowel sign AA, 0f71, was messed up prior to
     * Unicode version 2.1.9.
     * Once 2.1.9 or 3.0 is released, uncomment this test.
     */
    public void TestTibetan() throws Exception{
        String[][] decomp = {
            { "\u0f77", "\u0f77", "\u0fb2\u0f71\u0f80" }
        };
        String[][] compose = {
            { "\u0fb2\u0f71\u0f80", "\u0fb2\u0f71\u0f80", "\u0fb2\u0f71\u0f80" }
        };

        staticTest(Normalizer.NFD, decomp, 1);
        staticTest(Normalizer.NFKD,decomp, 2);
        staticTest(Normalizer.NFC, compose, 1);
        staticTest(Normalizer.NFKC,compose, 2);
    }

    /**
     * Make sure characters in the CompositionExclusion.txt list do not get
     * composed to.
     */
    public void TestCompositionExclusion()
                throws Exception{
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
            String b = Normalizer.normalize(a, Normalizer.NFKD);
            String c = Normalizer.normalize(b, Normalizer.NFC);
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
//          String b = Normalizer.normalize(a,Normalizer.DECOMP_COMPAT,0);
//          String c = Normalizer.normalize(b,Normalizer.COMPOSE,0);
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
    public void TestZeroIndex()
                throws Exception{
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
            String b = Normalizer.normalize(a, Normalizer.NFKC);
            String exp = DATA[i+1];
            if (b.equals(exp)) {
                logln("Ok: " + hex(a) + " x COMPOSE_COMPAT => " + hex(b));
            } else {
                errln("FAIL: " + hex(a) + " x COMPOSE_COMPAT => " + hex(b) +
                      ", expect " + hex(exp));
            }
            a = Normalizer.normalize(b, Normalizer.NFD);
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
    public void TestVerisign()
                throws Exception{
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
            String result = Normalizer.decompose(input, false);
            if (!result.equals(output)) {
                errln("FAIL input: " + hex(input));
                errln(" decompose: " + hex(result));
                errln("  expected: " + hex(output));
            }
            result = Normalizer.compose(input, false);
            if (!result.equals(output)) {
                errln("FAIL input: " + hex(input));
                errln("   compose: " + hex(result));
                errln("  expected: " + hex(output));
            }
        }

    }
    public void  TestQuickCheckResultNO()
                 throws Exception{
        final char CPNFD[] = {0x00C5, 0x0407, 0x1E00, 0x1F57, 0x220C,
                                0x30AE, 0xAC00, 0xD7A3, 0xFB36, 0xFB4E};
        final char CPNFC[] = {0x0340, 0x0F93, 0x1F77, 0x1FBB, 0x1FEB,
                                0x2000, 0x232A, 0xF900, 0xFA1E, 0xFB4E};
        final char CPNFKD[] = {0x00A0, 0x02E4, 0x1FDB, 0x24EA, 0x32FE,
                                0xAC00, 0xFB4E, 0xFA10, 0xFF3F, 0xFA2D};
        final char CPNFKC[] = {0x00A0, 0x017F, 0x2000, 0x24EA, 0x32FE,
                                0x33FE, 0xFB4E, 0xFA10, 0xFF3F, 0xFA2D};


        final int SIZE = 10;

        int count = 0;
        for (; count < SIZE; count ++)
        {
            if (Normalizer.quickCheck(String.valueOf(CPNFD[count]),
                    Normalizer.NFD,0) != Normalizer.NO)
            {
                errln("ERROR in NFD quick check at U+" +
                       Integer.toHexString(CPNFD[count]));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(CPNFC[count]),
                        Normalizer.NFC,0) !=Normalizer.NO)
            {
                errln("ERROR in NFC quick check at U+"+
                       Integer.toHexString(CPNFC[count]));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(CPNFKD[count]),
                                Normalizer.NFKD,0) != Normalizer.NO)
            {
                errln("ERROR in NFKD quick check at U+"+
                       Integer.toHexString(CPNFKD[count]));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(CPNFKC[count]),
                                         Normalizer.NFKC,0) !=Normalizer.NO)
            {
                errln("ERROR in NFKC quick check at U+"+
                       Integer.toHexString(CPNFKC[count]));
                return;
            }
            // for improving coverage
            if (Normalizer.quickCheck(String.valueOf(CPNFKC[count]),
                                         Normalizer.NFKC) !=Normalizer.NO)
            {
                errln("ERROR in NFKC quick check at U+"+
                       Integer.toHexString(CPNFKC[count]));
                return;
            }
        }
    }


    public void TestQuickCheckResultYES()
                throws Exception{
        final char CPNFD[] = {0x00C6, 0x017F, 0x0F74, 0x1000, 0x1E9A,
                                0x2261, 0x3075, 0x4000, 0x5000, 0xF000};
        final char CPNFC[] = {0x0400, 0x0540, 0x0901, 0x1000, 0x1500,
                                0x1E9A, 0x3000, 0x4000, 0x5000, 0xF000};
        final char CPNFKD[] = {0x00AB, 0x02A0, 0x1000, 0x1027, 0x2FFB,
                                0x3FFF, 0x4FFF, 0xA000, 0xF000, 0xFA27};
        final char CPNFKC[] = {0x00B0, 0x0100, 0x0200, 0x0A02, 0x1000,
                                0x2010, 0x3030, 0x4000, 0xA000, 0xFA0E};

        final int SIZE = 10;
        int count = 0;

        char cp = 0;
        while (cp < 0xA0)
        {
            if (Normalizer.quickCheck(String.valueOf(cp), Normalizer.NFD,0)
                                            != Normalizer.YES)
            {
                errln("ERROR in NFD quick check at U+"+
                                                      Integer.toHexString(cp));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(cp), Normalizer.NFC,0)
                                             != Normalizer.YES)
            {
                errln("ERROR in NFC quick check at U+"+
                                                      Integer.toHexString(cp));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(cp), Normalizer.NFKD,0)
                                             != Normalizer.YES)
            {
                errln("ERROR in NFKD quick check at U+" +
                                                      Integer.toHexString(cp));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(cp), Normalizer.NFKC,0)
                                             != Normalizer.YES)
            {
                errln("ERROR in NFKC quick check at U+"+
                                                       Integer.toHexString(cp));
                return;
            }
            // improve the coverage
            if (Normalizer.quickCheck(String.valueOf(cp), Normalizer.NFKC)
                                             != Normalizer.YES)
            {
                errln("ERROR in NFKC quick check at U+"+
                                                       Integer.toHexString(cp));
                return;
            }
            cp++;
        }

        for (; count < SIZE; count ++)
        {
            if (Normalizer.quickCheck(String.valueOf(CPNFD[count]),
                                         Normalizer.NFD,0)!=Normalizer.YES)
            {
                errln("ERROR in NFD quick check at U+"+
                                             Integer.toHexString(CPNFD[count]));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(CPNFC[count]),
                                         Normalizer.NFC,0)!=Normalizer.YES)
            {
                errln("ERROR in NFC quick check at U+"+
                                             Integer.toHexString(CPNFC[count]));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(CPNFKD[count]),
                                         Normalizer.NFKD,0)!=Normalizer.YES)
            {
                errln("ERROR in NFKD quick check at U+"+
                                    Integer.toHexString(CPNFKD[count]));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(CPNFKC[count]),
                                         Normalizer.NFKC,0)!=Normalizer.YES)
            {
                errln("ERROR in NFKC quick check at U+"+
                        Integer.toHexString(CPNFKC[count]));
                return;
            }
            // improve the coverage
            if (Normalizer.quickCheck(String.valueOf(CPNFKC[count]),
                                         Normalizer.NFKC)!=Normalizer.YES)
            {
                errln("ERROR in NFKC quick check at U+"+
                        Integer.toHexString(CPNFKC[count]));
                return;
            }
        }
    }
    public void TestBengali() throws Exception{
        String input = "\u09bc\u09be\u09cd\u09be";
        String output=Normalizer.normalize(input,Normalizer.NFC);
        if(!input.equals(output)){
             errln("ERROR in NFC of string");
        }
    }
    public void TestQuickCheckResultMAYBE()
                throws Exception{

        final char[] CPNFC = {0x0306, 0x0654, 0x0BBE, 0x102E, 0x1161,
                                0x116A, 0x1173, 0x1175, 0x3099, 0x309A};
        final char[] CPNFKC = {0x0300, 0x0654, 0x0655, 0x09D7, 0x0B3E,
                                0x0DCF, 0xDDF, 0x102E, 0x11A8, 0x3099};


        final int SIZE = 10;

        int count = 0;

        /* NFD and NFKD does not have any MAYBE codepoints */
        for (; count < SIZE; count ++)
        {
            if (Normalizer.quickCheck(String.valueOf(CPNFC[count]),
                                        Normalizer.NFC,0)!=Normalizer.MAYBE)
            {
                errln("ERROR in NFC quick check at U+"+
                                            Integer.toHexString(CPNFC[count]));
                return;
            }
            if (Normalizer.quickCheck(String.valueOf(CPNFKC[count]),
                                       Normalizer.NFKC,0)!=Normalizer.MAYBE)
            {
                errln("ERROR in NFKC quick check at U+"+
                                            Integer.toHexString(CPNFKC[count]));
                return;
            }
            if (Normalizer.quickCheck(new char[]{CPNFC[count]},
                                        Normalizer.NFC,0)!=Normalizer.MAYBE)
            {
                errln("ERROR in NFC quick check at U+"+
                                            Integer.toHexString(CPNFC[count]));
                return;
            }
            if (Normalizer.quickCheck(new char[]{CPNFKC[count]},
                                       Normalizer.NFKC,0)!=Normalizer.MAYBE)
            {
                errln("ERROR in NFKC quick check at U+"+
                                            Integer.toHexString(CPNFKC[count]));
                return;
            }
            if (Normalizer.quickCheck(new char[]{CPNFKC[count]},
                                       Normalizer.NONE,0)!=Normalizer.YES)
            {
                errln("ERROR in NONE quick check at U+"+
                                            Integer.toHexString(CPNFKC[count]));
                return;
            }
        }
    }

    public void TestQuickCheckStringResult()
                throws Exception{
        int count;
        String d;
        String c;

        for (count = 0; count < canonTests.length; count ++)
        {
            d = canonTests[count][1];
            c = canonTests[count][2];
            if (Normalizer.quickCheck(d,Normalizer.NFD,0)
                                            != Normalizer.YES)
            {
                errln("ERROR in NFD quick check for string at count " + count);
                return;
            }

            if (Normalizer.quickCheck(c, Normalizer.NFC,0)
                                            == Normalizer.NO)
            {
                errln("ERROR in NFC quick check for string at count " + count);
                return;
            }
        }

        for (count = 0; count < compatTests.length; count ++)
        {
            d = compatTests[count][1];
            c = compatTests[count][2];
            if (Normalizer.quickCheck(d, Normalizer.NFKD,0)
                                            != Normalizer.YES)
            {
                errln("ERROR in NFKD quick check for string at count " + count);
                return;
            }

            if (Normalizer.quickCheck(c,  Normalizer.NFKC,0)
                                            != Normalizer.YES)
            {
                errln("ERROR in NFKC quick check for string at count " + count);
                return;
            }
        }
    }

    static final int qcToInt(Normalizer.QuickCheckResult qc) {
        if(qc==Normalizer.NO) {
            return 0;
        } else if(qc==Normalizer.YES) {
            return 1;
        } else /* Normalizer.MAYBE */ {
            return 2;
        }
    }

    public void TestQuickCheckPerCP() {
        int c, lead, trail;
        String s, nfd;
        int lccc1, lccc2, tccc1, tccc2;
        int qc1, qc2;

        if(
            UCharacter.getIntPropertyMaxValue(UProperty.NFD_QUICK_CHECK)!=1 || // YES
            UCharacter.getIntPropertyMaxValue(UProperty.NFKD_QUICK_CHECK)!=1 ||
            UCharacter.getIntPropertyMaxValue(UProperty.NFC_QUICK_CHECK)!=2 || // MAYBE
            UCharacter.getIntPropertyMaxValue(UProperty.NFKC_QUICK_CHECK)!=2 ||
            UCharacter.getIntPropertyMaxValue(UProperty.LEAD_CANONICAL_COMBINING_CLASS)!=UCharacter.getIntPropertyMaxValue(UProperty.CANONICAL_COMBINING_CLASS) ||
            UCharacter.getIntPropertyMaxValue(UProperty.TRAIL_CANONICAL_COMBINING_CLASS)!=UCharacter.getIntPropertyMaxValue(UProperty.CANONICAL_COMBINING_CLASS)
        ) {
            errln("wrong result from one of the u_getIntPropertyMaxValue(UCHAR_NF*_QUICK_CHECK) or UCHAR_*_CANONICAL_COMBINING_CLASS");
        }

        /*
         * compare the quick check property values for some code points
         * to the quick check results for checking same-code point strings
         */
        c=0;
        while(c<0x110000) {
            s=UTF16.valueOf(c);

            qc1=UCharacter.getIntPropertyValue(c, UProperty.NFC_QUICK_CHECK);
            qc2=qcToInt(Normalizer.quickCheck(s, Normalizer.NFC));
            if(qc1!=qc2) {
                errln("getIntPropertyValue(NFC)="+qc1+" != "+qc2+"=quickCheck(NFC) for U+"+Integer.toHexString(c));
            }

            qc1=UCharacter.getIntPropertyValue(c, UProperty.NFD_QUICK_CHECK);
            qc2=qcToInt(Normalizer.quickCheck(s, Normalizer.NFD));
            if(qc1!=qc2) {
                errln("getIntPropertyValue(NFD)="+qc1+" != "+qc2+"=quickCheck(NFD) for U+"+Integer.toHexString(c));
            }

            qc1=UCharacter.getIntPropertyValue(c, UProperty.NFKC_QUICK_CHECK);
            qc2=qcToInt(Normalizer.quickCheck(s, Normalizer.NFKC));
            if(qc1!=qc2) {
                errln("getIntPropertyValue(NFKC)="+qc1+" != "+qc2+"=quickCheck(NFKC) for U+"+Integer.toHexString(c));
            }

            qc1=UCharacter.getIntPropertyValue(c, UProperty.NFKD_QUICK_CHECK);
            qc2=qcToInt(Normalizer.quickCheck(s, Normalizer.NFKD));
            if(qc1!=qc2) {
                errln("getIntPropertyValue(NFKD)="+qc1+" != "+qc2+"=quickCheck(NFKD) for U+"+Integer.toHexString(c));
            }

            nfd=Normalizer.normalize(s, Normalizer.NFD);
            lead=UTF16.charAt(nfd, 0);
            trail=UTF16.charAt(nfd, nfd.length()-1);

            lccc1=UCharacter.getIntPropertyValue(c, UProperty.LEAD_CANONICAL_COMBINING_CLASS);
            lccc2=UCharacter.getCombiningClass(lead);
            tccc1=UCharacter.getIntPropertyValue(c, UProperty.TRAIL_CANONICAL_COMBINING_CLASS);
            tccc2=UCharacter.getCombiningClass(trail);

            if(lccc1!=lccc2) {
                errln("getIntPropertyValue(lccc)="+lccc1+" != "+lccc2+"=getCombiningClass(lead) for U+"+Integer.toHexString(c));
            }
            if(tccc1!=tccc2) {
                errln("getIntPropertyValue(tccc)="+tccc1+" != "+tccc2+"=getCombiningClass(trail) for U+"+Integer.toHexString(c));
            }

            /* skip some code points */
            c=(20*c)/19+1;
        }
    }

    //------------------------------------------------------------------------
    // Internal utilities
    //
       //------------------------------------------------------------------------
    // Internal utilities
    //

/*    private void backAndForth(Normalizer iter, String input)
    {
        iter.setText(input);

        // Run through the iterator forwards and stick it into a StringBuffer
        StringBuffer forward =  new StringBuffer();
        for (int ch = iter.first(); ch != Normalizer.DONE; ch = iter.next()) {
            forward.append(ch);
        }

        // Now do it backwards
        StringBuffer reverse = new StringBuffer();
        for (int ch = iter.last(); ch != Normalizer.DONE; ch = iter.previous()) {
            reverse.insert(0, ch);
        }

        if (!forward.toString().equals(reverse.toString())) {
            errln("FAIL: Forward/reverse mismatch for input " + hex(input)
                  + ", forward: " + hex(forward) + ", backward: "+hex(reverse));
        } else if (isVerbose()) {
            logln("Ok: Forward/reverse for input " + hex(input)
                  + ", forward: " + hex(forward) + ", backward: "+hex(reverse));
        }
    }*/

    private void backAndForth(Normalizer iter, String[][] tests)
    {
        for (int i = 0; i < tests.length; i++)
        {
            iter.setText(tests[i][0]);

            // Run through the iterator forwards and stick it into a
            // StringBuffer
            StringBuffer forward =  new StringBuffer();
            for (int ch = iter.first(); ch != Normalizer.DONE; ch = iter.next()) {
                forward.append(ch);
            }

            // Now do it backwards
            StringBuffer reverse = new StringBuffer();
            for (int ch = iter.last(); ch != Normalizer.DONE; ch = iter.previous()) {
                reverse.insert(0, ch);
            }

            if (!forward.toString().equals(reverse.toString())) {
                errln("FAIL: Forward/reverse mismatch for input "
                    + hex(tests[i][0]) + ", forward: " + hex(forward)
                    + ", backward: " + hex(reverse));
            } else if (isVerbose()) {
                logln("Ok: Forward/reverse for input " + hex(tests[i][0])
                      + ", forward: " + hex(forward) + ", backward: "
                      + hex(reverse));
            }
        }
    }

    private void staticTest (Normalizer.Mode mode,
                             String[][] tests, int outCol) throws Exception{
        for (int i = 0; i < tests.length; i++)
        {
            String input = Utility.unescape(tests[i][0]);
            String expect = Utility.unescape(tests[i][outCol]);

            logln("Normalizing '" + input + "' (" + hex(input) + ")" );

            String output = Normalizer.normalize(input, mode);

            if (!output.equals(expect)) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + output + "' (" + hex(output) + ")" );
            }
        }
        char[] output = new char[1];
        for (int i = 0; i < tests.length; i++)
        {
            char[] input = Utility.unescape(tests[i][0]).toCharArray();
            String expect =Utility.unescape( tests[i][outCol]);

            logln("Normalizing '" + new String(input) + "' (" +
                        hex(new String(input)) + ")" );
            int reqLength=0;
            while(true){
                try{
                    reqLength=Normalizer.normalize(input,output, mode,0);
                    if(reqLength<=output.length    ){
                        break;
                    }
                }catch(IndexOutOfBoundsException e){
                    output= new char[Integer.parseInt(e.getMessage())];
                    continue;
                }
            }
            if (!expect.equals(new String(output,0,reqLength))) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + new String(output)
                    + "' ("  + hex(new String(output)) + ")" );
            }
        }
    }
    private void decomposeTest(Normalizer.Mode mode,
                             String[][] tests, int outCol) throws Exception{
        for (int i = 0; i < tests.length; i++)
        {
            String input = Utility.unescape(tests[i][0]);
            String expect = Utility.unescape(tests[i][outCol]);

            logln("Normalizing '" + input + "' (" + hex(input) + ")" );

            String output = Normalizer.decompose(input, mode==Normalizer.NFKD);

            if (!output.equals(expect)) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + output + "' (" + hex(output) + ")" );
            }
        }
        char[] output = new char[1];
        for (int i = 0; i < tests.length; i++)
        {
            char[] input = Utility.unescape(tests[i][0]).toCharArray();
            String expect = Utility.unescape(tests[i][outCol]);

            logln("Normalizing '" + new String(input) + "' (" +
                        hex(new String(input)) + ")" );
            int reqLength=0;
            while(true){
                try{
                    reqLength=Normalizer.decompose(input,output, mode==Normalizer.NFKD,0);
                    if(reqLength<=output.length ){
                        break;
                    }
                }catch(IndexOutOfBoundsException e){
                    output= new char[Integer.parseInt(e.getMessage())];
                    continue;
                }
            }
            if (!expect.equals(new String(output,0,reqLength))) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + new String(output)
                    + "' ("  + hex(new String(output)) + ")" );
            }
        }
        output = new char[1];
        for (int i = 0; i < tests.length; i++)
        {
           char[] input = Utility.unescape(tests[i][0]).toCharArray();
           String expect = Utility.unescape(tests[i][outCol]);
    
           logln("Normalizing '" + new String(input) + "' (" +
                       hex(new String(input)) + ")" );
           int reqLength=0;
           while(true){
               try{
                   reqLength=Normalizer.decompose(input,0,input.length,output,0,output.length, mode==Normalizer.NFKD,0);
                   if(reqLength<=output.length ){
                       break;
                   }
               }catch(IndexOutOfBoundsException e){
                   output= new char[Integer.parseInt(e.getMessage())];
                   continue;
               }
           }
           if (!expect.equals(new String(output,0,reqLength))) {
               errln("FAIL: case " + i
                   + " expected '" + expect + "' (" + hex(expect) + ")"
                   + " but got '" + new String(output)
                   + "' ("  + hex(new String(output)) + ")" );
           }
           char[] output2 = new char[reqLength * 2];
           System.arraycopy(output, 0, output2, 0, reqLength);
           int retLength = Normalizer.decompose(input,0,input.length, output2, reqLength, output2.length, mode==Normalizer.NFKC,0);
           if(retLength != reqLength){
               logln("FAIL: Normalizer.compose did not return the expected length. Expected: " +reqLength + " Got: " + retLength);
           }
        }
    }

    private void composeTest(Normalizer.Mode mode,
                             String[][] tests, int outCol) throws Exception{
        for (int i = 0; i < tests.length; i++)
        {
            String input = Utility.unescape(tests[i][0]);
            String expect = Utility.unescape(tests[i][outCol]);

            logln("Normalizing '" + input + "' (" + hex(input) + ")" );

            String output = Normalizer.compose(input, mode==Normalizer.NFKC);

            if (!output.equals(expect)) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + output + "' (" + hex(output) + ")" );
            }
        }
        char[] output = new char[1];
        for (int i = 0; i < tests.length; i++)
        {
            char[] input = Utility.unescape(tests[i][0]).toCharArray();
            String expect = Utility.unescape(tests[i][outCol]);

            logln("Normalizing '" + new String(input) + "' (" +
                        hex(new String(input)) + ")" );
            int reqLength=0;
            while(true){
                try{
                    reqLength=Normalizer.compose(input,output, mode==Normalizer.NFKC,0);
                    if(reqLength<=output.length ){
                        break;
                    }
                }catch(IndexOutOfBoundsException e){
                    output= new char[Integer.parseInt(e.getMessage())];
                    continue;
                }
            }
            if (!expect.equals(new String(output,0,reqLength))) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + new String(output)
                    + "' ("  + hex(new String(output)) + ")" );
            }
        }
        output = new char[1];
        for (int i = 0; i < tests.length; i++)
        {
            char[] input = Utility.unescape(tests[i][0]).toCharArray();
            String expect = Utility.unescape(tests[i][outCol]);

            logln("Normalizing '" + new String(input) + "' (" +
                        hex(new String(input)) + ")" );
            int reqLength=0;
            while(true){
                try{
                    reqLength=Normalizer.compose(input,0,input.length, output, 0, output.length, mode==Normalizer.NFKC,0);
                    if(reqLength<=output.length ){
                        break;
                    }
                }catch(IndexOutOfBoundsException e){
                    output= new char[Integer.parseInt(e.getMessage())];
                    continue;
                }
            }
            if (!expect.equals(new String(output,0,reqLength))) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + new String(output)
                    + "' ("  + hex(new String(output)) + ")" );
            }
            
            char[] output2 = new char[reqLength * 2];
            System.arraycopy(output, 0, output2, 0, reqLength);
            int retLength = Normalizer.compose(input,0,input.length, output2, reqLength, output2.length, mode==Normalizer.NFKC,0);
            if(retLength != reqLength){
                logln("FAIL: Normalizer.compose did not return the expected length. Expected: " +reqLength + " Got: " + retLength);
            }
        }
    }
    private void iterateTest(Normalizer iter, String[][] tests, int outCol){
        for (int i = 0; i < tests.length; i++)
        {
            String input = Utility.unescape(tests[i][0]);
            String expect = Utility.unescape(tests[i][outCol]);

            logln("Normalizing '" + input + "' (" + hex(input) + ")" );

            iter.setText(input);
            assertEqual(expect, iter, "case " + i + " ");
        }
    }

    private void assertEqual(String expected, Normalizer iter, String msg)
    {
        int index = 0;
        int ch;
        UCharacterIterator cIter =  UCharacterIterator.getInstance(expected);
        
        while ((ch=iter.next())!= Normalizer.DONE){
            if (index >= expected.length()) {
                errln("FAIL: " + msg + "Unexpected character '" + (char)ch
                        + "' (" + hex(ch) + ")"
                        + " at index " + index);
                break;
            }
            int want = UTF16.charAt(expected,index);
            if (ch != want) {
                errln("FAIL: " + msg + "got '" + (char)ch
                        + "' (" + hex(ch) + ")"
                        + " but expected '" + want + "' (" + hex(want)+ ")"
                        + " at index " + index);
            }
            index+=  UTF16.getCharCount(ch);
        }
        if (index < expected.length()) {
            errln("FAIL: " + msg + "Only got " + index + " chars, expected "
            + expected.length());
        }
        
        cIter.setToLimit();
        while((ch=iter.previous())!=Normalizer.DONE){
            int want = cIter.previousCodePoint();
            if (ch != want ) {
                errln("FAIL: " + msg + "got '" + (char)ch
                        + "' (" + hex(ch) + ")"
                        + " but expected '" + want + "' (" + hex(want) + ")"
                        + " at index " + index);
            }
        }
    }
    //--------------------------------------------------------------------------

    // NOTE: These tests are used for quick debugging so are not ported
    // to ICU4C tsnorm.cpp in intltest
    //

    public void TestDebugStatic(){
        String in = Utility.unescape("\\U0001D157\\U0001D165");
        if(!Normalizer.isNormalized(in,Normalizer.NFC,0)){
            errln("isNormalized failed");
        }

        String input  =  "\uAD8B\uAD8B\uAD8B\uAD8B"+
            "\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
            "\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
            "\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
            "\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
            "\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
            "aaaaaaaaaaaaaaaaaazzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"+
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"+
            "ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"+
            "ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"+
            "\uAD8B\uAD8B\uAD8B\uAD8B"+
            "d\u031B\u0307\u0323";
        String expect = "\u1100\u116F\u11AA\u1100\u116F\u11AA\u1100\u116F"+
                        "\u11AA\u1100\u116F\u11AA\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65\uD834\uDD57\uD834\uDD65"+
                        "\uD834\uDD57\uD834\uDD65aaaaaaaaaaaaaaaaaazzzzzz"+
                        "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"+
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"+
                        "bbbbbbbbbbbbbbbbbbbbbbbbccccccccccccccccccccccccccccc"+
                        "cccccccccccccccccccccccccccccccccccccccccccccccc"+
                        "ddddddddddddddddddddddddddddddddddddddddddddddddddddd"+
                        "dddddddddddddddddddddddd"+
                        "\u1100\u116F\u11AA\u1100\u116F\u11AA\u1100\u116F"+
                        "\u11AA\u1100\u116F\u11AA\u0064\u031B\u0323\u0307";
            String output = Normalizer.normalize(Utility.unescape(input),
                            Normalizer.NFD);
            if(!expect.equals(output)){
                errln("FAIL expected: "+hex(expect) + " got: "+hex(output));
            }



    }
    public void TestDebugIter(){
        String src = Utility.unescape("\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e");
        String expected = Utility.unescape("\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e");
        Normalizer iter = new Normalizer(new StringCharacterIterator(Utility.unescape(src)),
                                                Normalizer.NONE,0);
        int index = 0;
        int ch;
        UCharacterIterator cIter =  UCharacterIterator.getInstance(expected);
        
        while ((ch=iter.next())!= Normalizer.DONE){
            if (index >= expected.length()) {
                errln("FAIL: " +  "Unexpected character '" + (char)ch
                        + "' (" + hex(ch) + ")"
                        + " at index " + index);
                break;
            }
            int want = UTF16.charAt(expected,index);
            if (ch != want) {
                errln("FAIL: " +  "got '" + (char)ch
                        + "' (" + hex(ch) + ")"
                        + " but expected '" + want + "' (" + hex(want)+ ")"
                        + " at index " + index);
            }
            index+=  UTF16.getCharCount(ch);
        }
        if (index < expected.length()) {
            errln("FAIL: " +  "Only got " + index + " chars, expected "
            + expected.length());
        }
        
        cIter.setToLimit();
        while((ch=iter.previous())!=Normalizer.DONE){
            int want = cIter.previousCodePoint();
            if (ch != want ) {
                errln("FAIL: " + "got '" + (char)ch
                        + "' (" + hex(ch) + ")"
                        + " but expected '" + want + "' (" + hex(want) + ")"
                        + " at index " + index);
            }
        }
    }
    public void TestDebugIterOld(){
        String input = "\\U0001D15E";
        String expected = "\uD834\uDD57\uD834\uDD65";
        String expectedReverse = "\uD834\uDD65\uD834\uDD57";
        int index = 0;
        int ch;
        Normalizer iter = new Normalizer(new StringCharacterIterator(Utility.unescape(input)),
                                                Normalizer.NFKC,0);
        StringBuffer got = new StringBuffer();
        for (ch = iter.first();ch!=Normalizer.DONE;ch=iter.next())
        {
            if (index >= expected.length()) {
                errln("FAIL: " +  "Unexpected character '" + (char)ch +
                       "' (" + hex(ch) + ")" + " at index " + index);
                break;
            }
            got.append(UCharacter.toString(ch));
            index++;
        }
        if (!expected.equals(got.toString())) {
                errln("FAIL: " +  "got '" +got+ "' (" + hex(got) + ")"
                        + " but expected '" + expected + "' ("
                        + hex(expected) + ")");
        }
        if (got.length() < expected.length()) {
            errln("FAIL: " +  "Only got " + index + " chars, expected "
                           + expected.length());
        }

        logln("Reverse Iteration\n");
        iter.setIndexOnly(iter.endIndex());
        got.setLength(0);
        for(ch=iter.previous();ch!=Normalizer.DONE;ch=iter.previous()){
            if (index >= expected.length()) {
                errln("FAIL: " +  "Unexpected character '" + (char)ch
                               + "' (" + hex(ch) + ")" + " at index " + index);
                break;
            }
            got.append(UCharacter.toString(ch));
        }
        if (!expectedReverse.equals(got.toString())) {
                errln("FAIL: " +  "got '" +got+ "' (" + hex(got) + ")"
                               + " but expected '" + expected
                               + "' (" + hex(expected) + ")");
        }
        if (got.length() < expected.length()) {
            errln("FAIL: " +  "Only got " + index + " chars, expected "
                      + expected.length());
        }

    }
    //--------------------------------------------------------------------------
    // helper class for TestPreviousNext()
    // simple UTF-32 character iterator
    class UCharIterator {

       public UCharIterator(int[] src, int len, int index){

            s=src;
            length=len;
            i=index;
       }

        public int current() {
            if(i<length) {
                return s[i];
            } else {
                return -1;
            }
        }

        public int next() {
            if(i<length) {
                return s[i++];
            } else {
                return -1;
            }
        }

        public int previous() {
            if(i>0) {
                return s[--i];
            } else {
                return -1;
            }
        }

        public int getIndex() {
            return i;
        }

        private int[] s;
        private int length, i;
    }
    public void TestPreviousNext() {
        // src and expect strings
        char src[]={
            UTF16.getLeadSurrogate(0x2f999), UTF16.getTrailSurrogate(0x2f999),
            UTF16.getLeadSurrogate(0x1d15f), UTF16.getTrailSurrogate(0x1d15f),
            0xc4,
            0x1ed0
        };
        int expect[]={
            0x831d,
            0x1d158, 0x1d165,
            0x41, 0x308,
            0x4f, 0x302, 0x301
        };

        // expected src indexes corresponding to expect indexes
        int expectIndex[]={
            0,
            2, 2,
            4, 4,
            5, 5, 5,
            6 // behind last character
        };

        // initial indexes into the src and expect strings

        final int SRC_MIDDLE=4;
        final int EXPECT_MIDDLE=3;


        // movement vector
        // - for previous(), 0 for current(), + for next()
        // not const so that we can terminate it below for the error message
        String moves="0+0+0--0-0-+++0--+++++++0--------";

        // iterators
        Normalizer iter = new Normalizer(new String(src),
                                                Normalizer.NFD,0);
        UCharIterator iter32 = new UCharIterator(expect, expect.length,
                                                     EXPECT_MIDDLE);

        int c1, c2;
        char m;

        // initially set the indexes into the middle of the strings
        iter.setIndexOnly(SRC_MIDDLE);

        // move around and compare the iteration code points with
        // the expected ones
        int movesIndex =0;
        while(movesIndex<moves.length()) {
            m=moves.charAt(movesIndex++);
            if(m=='-') {
                c1=iter.previous();
                c2=iter32.previous();
            } else if(m=='0') {
                c1=iter.current();
                c2=iter32.current();
            } else /* m=='+' */ {
                c1=iter.next();
                c2=iter32.next();
            }

            // compare results
            if(c1!=c2) {
                // copy the moves until the current (m) move, and terminate
                String history = moves.substring(0,movesIndex);
                errln("error: mismatch in Normalizer iteration at "+history+": "
                      +"got c1= " + hex(c1) +" != expected c2= "+ hex(c2));
                break;
            }

            // compare indexes
            if(iter.getIndex()!=expectIndex[iter32.getIndex()]) {
                // copy the moves until the current (m) move, and terminate
                String history = moves.substring(0,movesIndex);
                errln("error: index mismatch in Normalizer iteration at "
                      +history+ " : "+ "Normalizer index " +iter.getIndex()
                      +" expected "+ expectIndex[iter32.getIndex()]);
                break;
            }
        }
    }
    // Only in ICU4j
    public void TestPreviousNextJCI() {
        // src and expect strings
        char src[]={
            UTF16.getLeadSurrogate(0x2f999), UTF16.getTrailSurrogate(0x2f999),
            UTF16.getLeadSurrogate(0x1d15f), UTF16.getTrailSurrogate(0x1d15f),
            0xc4,
            0x1ed0
        };
        int expect[]={
            0x831d,
            0x1d158, 0x1d165,
            0x41, 0x308,
            0x4f, 0x302, 0x301
        };

        // expected src indexes corresponding to expect indexes
        int expectIndex[]={
            0,
            2, 2,
            4, 4,
            5, 5, 5,
            6 // behind last character
        };

        // initial indexes into the src and expect strings

        final int SRC_MIDDLE=4;
        final int EXPECT_MIDDLE=3;


        // movement vector
        // - for previous(), 0 for current(), + for next()
        // not const so that we can terminate it below for the error message
        String moves="0+0+0--0-0-+++0--+++++++0--------";

        // iterators
        StringCharacterIterator text = new StringCharacterIterator(new String(src));
        Normalizer iter = new Normalizer(text,Normalizer.NFD,0);
        UCharIterator iter32 = new UCharIterator(expect, expect.length,
                                                     EXPECT_MIDDLE);

        int c1, c2;
        char m;

        // initially set the indexes into the middle of the strings
        iter.setIndexOnly(SRC_MIDDLE);

        // move around and compare the iteration code points with
        // the expected ones
        int movesIndex =0;
        while(movesIndex<moves.length()) {
            m=moves.charAt(movesIndex++);
            if(m=='-') {
                c1=iter.previous();
                c2=iter32.previous();
            } else if(m=='0') {
                c1=iter.current();
                c2=iter32.current();
            } else /* m=='+' */ {
                c1=iter.next();
                c2=iter32.next();
            }

            // compare results
            if(c1!=c2) {
                // copy the moves until the current (m) move, and terminate
                String history = moves.substring(0,movesIndex);
                errln("error: mismatch in Normalizer iteration at "+history+": "
                      +"got c1= " + hex(c1) +" != expected c2= "+ hex(c2));
                break;
            }

            // compare indexes
            if(iter.getIndex()!=expectIndex[iter32.getIndex()]) {
                // copy the moves until the current (m) move, and terminate
                String history = moves.substring(0,movesIndex);
                errln("error: index mismatch in Normalizer iteration at "
                      +history+ " : "+ "Normalizer index " +iter.getIndex()
                      +" expected "+ expectIndex[iter32.getIndex()]);
                break;
            }
        }
    }

    // test APIs that are not otherwise used - improve test coverage
    public void TestNormalizerAPI() throws Exception {
        try{
            // instantiate a Normalizer from a CharacterIterator
            String s=Utility.unescape("a\u0308\uac00\\U0002f800");
            // make s a bit longer and more interesting
            UCharacterIterator iter = UCharacterIterator.getInstance(s+s);
            Normalizer norm = new Normalizer(iter, Normalizer.NFC,0);
            if(norm.next()!=0xe4) {
                errln("error in Normalizer(CharacterIterator).next()");
            }   
    
            // test clone(), ==, and hashCode()
            Normalizer clone=(Normalizer)norm.clone();
            if(clone.equals(norm)) {
                errln("error in Normalizer(Normalizer(CharacterIterator)).clone()!=norm");
            }
    
            
            if(clone.getLength()!= norm.getLength()){
               errln("error in Normalizer.getBeginIndex()");
            } 
            // clone must have the same hashCode()
            //if(clone.hashCode()!=norm.hashCode()) {
            //    errln("error in Normalizer(Normalizer(CharacterIterator)).clone().hashCode()!=copy.hashCode()");
            //}
            if(clone.next()!=0xac00) {
                errln("error in Normalizer(Normalizer(CharacterIterator)).next()");
            }
            int ch = clone.next();
            if(ch!=0x4e3d) {
                errln("error in Normalizer(Normalizer(CharacterIterator)).clone().next()");
            }
            // position changed, must change hashCode()
            if(clone.hashCode()==norm.hashCode()) {
                errln("error in Normalizer(Normalizer(CharacterIterator)).clone().next().hashCode()==copy.hashCode()");
            }
    
            // test compose() and decompose()
            StringBuffer tel;
            String nfkc, nfkd;
            tel=new StringBuffer("\u2121\u2121\u2121\u2121\u2121\u2121\u2121\u2121\u2121\u2121");
            tel.insert(1,(char)0x0301);
    
            nfkc=Normalizer.compose(tel.toString(), true);
            nfkd=Normalizer.decompose(tel.toString(), true);
            if(
                !nfkc.equals(Utility.unescape("TE\u0139TELTELTELTELTELTELTELTELTEL"))||
                !nfkd.equals(Utility.unescape("TEL\u0301TELTELTELTELTELTELTELTELTEL"))
            ) {
                errln("error in Normalizer::(de)compose(): wrong result(s)");
            }
    
            // test setIndex()
//            ch=norm.setIndex(3);
//            if(ch!=0x4e3d) {
//                errln("error in Normalizer(CharacterIterator).setIndex(3)");
//            }
    
            // test setText(CharacterIterator) and getText()
            String out, out2;
            clone.setText(iter);
    
            out = clone.getText();
            out2 = iter.getText();
            if( !out.equals(out2) ||
                clone.startIndex()!=0||
                clone.endIndex()!=iter.getLength()
            ) {
                errln("error in Normalizer::setText() or Normalizer::getText()");
            }
     
            char[] fillIn1 = new char[clone.getLength()];
            char[] fillIn2 = new char[iter.getLength()];
            int len = clone.getText(fillIn1);
            iter.getText(fillIn2,0);
            if(!Utility.arrayRegionMatches(fillIn1,0,fillIn2,0,len)){
                errln("error in Normalizer.getText(). Normalizer: "+
                                Utility.hex(new String(fillIn1))+ 
                                " Iter: " + Utility.hex(new String(fillIn2)));
            }
            
            clone.setText(fillIn1);
            len = clone.getText(fillIn2);
            if(!Utility.arrayRegionMatches(fillIn1,0,fillIn2,0,len)){
                errln("error in Normalizer.setText() or Normalizer.getText()"+
                                Utility.hex(new String(fillIn1))+ 
                                " Iter: " + Utility.hex(new String(fillIn2)));
            }
    
            // test setText(UChar *), getUMode() and setMode()
            clone.setText(s);
            clone.setIndexOnly(1);
            clone.setMode(Normalizer.NFD);
            if(clone.getMode()!=Normalizer.NFD) {
                errln("error in Normalizer::setMode() or Normalizer::getMode()");
            }
            if(clone.next()!=0x308 || clone.next()!=0x1100) {
                errln("error in Normalizer::setText() or Normalizer::setMode()");
            }
    
            // test last()/previous() with an internal buffer overflow
            StringBuffer buf = new StringBuffer("aaaaaaaaaa");
            buf.setCharAt(10-1,'\u0308');
            clone.setText(buf);
            if(clone.last()!=0x308) {
                errln("error in Normalizer(10*U+0308).last()");
            }
    
            // test UNORM_NONE
            norm.setMode(Normalizer.NONE);
            if(norm.first()!=0x61 || norm.next()!=0x308 || norm.last()!=0x2f800) {
                errln("error in Normalizer(UNORM_NONE).first()/next()/last()");
            }
            out=Normalizer.normalize(s, Normalizer.NONE);
            if(!out.equals(s)) {
                errln("error in Normalizer::normalize(UNORM_NONE)");
            }
            ch = 0x1D15E;
            String exp = "\\U0001D157\\U0001D165";
            String ns = Normalizer.normalize(ch,Normalizer.NFC);
            if(!ns.equals(Utility.unescape(exp))){
                errln("error in Normalizer.normalize(int,Mode)");
            }
            ns = Normalizer.normalize(ch,Normalizer.NFC,0);
            if(!ns.equals(Utility.unescape(exp))){
                errln("error in Normalizer.normalize(int,Mode,int)");
            }
            
            
        }catch(Exception e){
            throw e;
        }
    }

    public void TestConcatenate() {

        Object[][]cases=new Object[][]{
            /* mode, left, right, result */
            {
                Normalizer.NFC,
                "re",
                "\u0301sum\u00e9",
                "r\u00e9sum\u00e9"
            },
            {
                Normalizer.NFC,
                "a\u1100",
                "\u1161bcdefghijk",
                "a\uac00bcdefghijk"
            },
            /* ### TODO: add more interesting cases */
            {
                Normalizer.NFD,
                "\u03B1\u0345",
                "\u0C4D\uD804\uDCBA\uD834\uDD69",  // 0C4D 110BA 1D169
                "\u03B1\uD834\uDD69\uD804\uDCBA\u0C4D\u0345"  // 03B1 1D169 110BA 0C4D 0345
            }
        };

        String left, right, expect, result;
        Normalizer.Mode mode;
        int i;

        /* test concatenation */
        for(i=0; i<cases.length; ++i) {
            mode = (Normalizer.Mode)cases[i][0];

            left=(String)cases[i][1];
            right=(String)cases[i][2];
            expect=(String)cases[i][3];
            {
                result=Normalizer.concatenate(left, right, mode,0);
                if(!result.equals(expect)) {
                    errln("error in Normalizer.concatenate(), cases[] failed"
                          +", result==expect: expected: "
                          + hex(expect)+" =========> got: " + hex(result));
                }
            }
            {
                result=Normalizer.concatenate(left.toCharArray(), right.toCharArray(), mode,0);
                if(!result.equals(expect)) {
                    errln("error in Normalizer.concatenate(), cases[] failed"
                          +", result==expect: expected: "
                          + hex(expect)+" =========> got: " + hex(result));
                }
            }
        }
    }
    private final int RAND_MAX = 0x7fff;

    public void TestCheckFCD()
    {
      char[] FAST = {0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
                     0x0008, 0x0009, 0x000A};

      char[] FALSE = {0x0001, 0x0002, 0x02EA, 0x03EB, 0x0300, 0x0301,
                      0x02B9, 0x0314, 0x0315, 0x0316};

      char[] TRUE = {0x0030, 0x0040, 0x0440, 0x056D, 0x064F, 0x06E7,
                     0x0050, 0x0730, 0x09EE, 0x1E10};

      char[][] datastr= { {0x0061, 0x030A, 0x1E05, 0x0302, 0},
                          {0x0061, 0x030A, 0x00E2, 0x0323, 0},
                          {0x0061, 0x0323, 0x00E2, 0x0323, 0},
                          {0x0061, 0x0323, 0x1E05, 0x0302, 0}
                        };
      Normalizer.QuickCheckResult result[] = {Normalizer.YES, Normalizer.NO, Normalizer.NO, Normalizer.YES};

      char[] datachar= {        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
                                0x6a,
                                0xe0, 0xe1, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9,
                                0xea,
                                0x0300, 0x0301, 0x0302, 0x0303, 0x0304, 0x0305, 0x0306,
                                0x0307, 0x0308, 0x0309, 0x030a,
                                0x0320, 0x0321, 0x0322, 0x0323, 0x0324, 0x0325, 0x0326,
                                0x0327, 0x0328, 0x0329, 0x032a,
                                0x1e00, 0x1e01, 0x1e02, 0x1e03, 0x1e04, 0x1e05, 0x1e06,
                                0x1e07, 0x1e08, 0x1e09, 0x1e0a
                       };

      int count = 0;

      if (Normalizer.quickCheck(FAST,0,FAST.length, Normalizer.FCD,0) != Normalizer.YES)
        errln("Normalizer.quickCheck(FCD) failed: expected value for fast Normalizer.quickCheck is Normalizer.YES\n");
      if (Normalizer.quickCheck(FALSE,0, FALSE.length,Normalizer.FCD,0) != Normalizer.NO)
        errln("Normalizer.quickCheck(FCD) failed: expected value for error Normalizer.quickCheck is Normalizer.NO\n");
      if (Normalizer.quickCheck(TRUE,0,TRUE.length,Normalizer.FCD,0) != Normalizer.YES)
        errln("Normalizer.quickCheck(FCD) failed: expected value for correct Normalizer.quickCheck is Normalizer.YES\n");


      while (count < 4)
      {
        Normalizer.QuickCheckResult fcdresult = Normalizer.quickCheck(datastr[count],0,datastr[count].length, Normalizer.FCD,0);
        if (result[count] != fcdresult) {
            errln("Normalizer.quickCheck(FCD) failed: Data set "+ count
                    + " expected value "+ result[count]);
        }
        count ++;
      }

      /* random checks of long strings */
      //srand((unsigned)time( NULL ));
      Random rand = createRandom(); // use test framework's random

      for (count = 0; count < 50; count ++)
      {
        int size = 0;
        Normalizer.QuickCheckResult testresult = Normalizer.YES;
        char[] data= new char[20];
        char[] norm= new char[100];
        char[] nfd = new char[100];
        int normStart = 0;
        int nfdsize = 0;
        while (size != 19) {
          data[size] = datachar[rand.nextInt(RAND_MAX)*50/RAND_MAX];
          logln("0x"+data[size]);
          normStart += Normalizer.normalize(data,size,size+1,
                                              norm,normStart,100,
                                              Normalizer.NFD,0);
          size ++;
        }
        logln("\n");

        nfdsize = Normalizer.normalize(data,0,size, nfd,0,nfd.length,Normalizer.NFD,0);
        //    nfdsize = unorm_normalize(data, size, UNORM_NFD, UCOL_IGNORE_HANGUL,
        //                      nfd, 100, &status);
        if (nfdsize != normStart || Utility.arrayRegionMatches(nfd,0, norm,0,nfdsize) ==false) {
          testresult = Normalizer.NO;
        }
        if (testresult == Normalizer.YES) {
          logln("result Normalizer.YES\n");
        }
        else {
          logln("result Normalizer.NO\n");
        }

        if (Normalizer.quickCheck(data,0,data.length, Normalizer.FCD,0) != testresult) {
          errln("Normalizer.quickCheck(FCD) failed: expected "+ testresult +" for random data: "+hex(new String(data)) );
        }
      }
    }


    // reference implementation of Normalizer::compare
    private int ref_norm_compare(String s1, String s2, int options) {
        String t1, t2,r1,r2;

        int normOptions=(int)(options>>Normalizer.COMPARE_NORM_OPTIONS_SHIFT);
        
        if((options&Normalizer.COMPARE_IGNORE_CASE)!=0) {
            // NFD(toCasefold(NFD(X))) = NFD(toCasefold(NFD(Y)))
            r1 = Normalizer.decompose(s1,false,normOptions);
            r2 = Normalizer.decompose(s2,false,normOptions);
            r1 = UCharacter.foldCase(r1,options);
            r2 = UCharacter.foldCase(r2,options);
        }else{
            r1 = s1;
            r2 = s2;
        }
        
        t1 = Normalizer.decompose(r1, false, normOptions);
        t2 = Normalizer.decompose(r2, false, normOptions);

        if((options&Normalizer.COMPARE_CODE_POINT_ORDER)!=0) {
            UTF16.StringComparator comp 
                    = new UTF16.StringComparator(true, false, 
                                     UTF16.StringComparator.FOLD_CASE_DEFAULT);
            return comp.compare(t1,t2);
        } else {
            return t1.compareTo(t2);
        }

    }

    // test wrapper for Normalizer::compare, sets UNORM_INPUT_IS_FCD appropriately
    private int norm_compare(String s1, String s2, int options) {
        int normOptions=(int)(options>>Normalizer.COMPARE_NORM_OPTIONS_SHIFT);

        if( Normalizer.YES==Normalizer.quickCheck(s1,Normalizer.FCD,normOptions) &&
            Normalizer.YES==Normalizer.quickCheck(s2,Normalizer.FCD,normOptions)) {
            options|=Normalizer.INPUT_IS_FCD;
        }

        return Normalizer.compare(s1, s2, options);
    }

    // reference implementation of UnicodeString::caseCompare
    private int ref_case_compare(String s1, String s2, int options) {
        String t1, t2;

        t1=s1;
        t2=s2;

        t1 = UCharacter.foldCase(t1,((options&Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I)==0));
        t2 = UCharacter.foldCase(t2,((options&Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I)==0));

        if((options&Normalizer.COMPARE_CODE_POINT_ORDER)!=0) {
            UTF16.StringComparator comp 
                    = new UTF16.StringComparator(true, false,
                                    UTF16.StringComparator.FOLD_CASE_DEFAULT);
            return comp.compare(t1,t2);
        } else {
            return t1.compareTo(t2);
        }

    }

    // reduce an integer to -1/0/1
    private static int sign(int value) {
        if(value==0) {
            return 0;
        } else {
            return (value>>31)|1;
        }
    }
    private static String signString(int value) {
        if(value<0) {
            return "<0";
        } else if(value==0) {
            return "=0";
        } else /* value>0 */ {
            return ">0";
        }
    }
    // test Normalizer::compare and unorm_compare (thinly wrapped by the former)
    // by comparing it with its semantic equivalent
    // since we trust the pieces, this is sufficient

    // test each string with itself and each other
    // each time with all options
    private  String strings[]=new String[]{
                // some cases from NormalizationTest.txt
                // 0..3
                "D\u031B\u0307\u0323",
                "\u1E0C\u031B\u0307",
                "D\u031B\u0323\u0307",
                "d\u031B\u0323\u0307",
        
                // 4..6
                "\u00E4",
                "a\u0308",
                "A\u0308",
        
                // Angstrom sign = A ring
                // 7..10
                "\u212B",
                "\u00C5",
                "A\u030A",
                "a\u030A",
        
                // 11.14
                "a\u059A\u0316\u302A\u032Fb",
                "a\u302A\u0316\u032F\u059Ab",
                "a\u302A\u0316\u032F\u059Ab",
                "A\u059A\u0316\u302A\u032Fb",
        
                // from ICU case folding tests
                // 15..20
                "A\u00df\u00b5\ufb03\\U0001040c\u0131",
                "ass\u03bcffi\\U00010434i",
                "\u0061\u0042\u0131\u03a3\u00df\ufb03\ud93f\udfff",
                "\u0041\u0062\u0069\u03c3\u0073\u0053\u0046\u0066\u0049\ud93f\udfff",
                "\u0041\u0062\u0131\u03c3\u0053\u0073\u0066\u0046\u0069\ud93f\udfff",
                "\u0041\u0062\u0069\u03c3\u0073\u0053\u0046\u0066\u0049\ud93f\udffd",
        
                //     U+d800 U+10001   see implementation comment in unorm_cmpEquivFold
                // vs. U+10000          at bottom - code point order
                // 21..22
                "\ud800\ud800\udc01",
                "\ud800\udc00",
        
                // other code point order tests from ustrtest.cpp
                // 23..31
                "\u20ac\ud801",
                "\u20ac\ud800\udc00",
                "\ud800",
                "\ud800\uff61",
                "\udfff",
                "\uff61\udfff",
                "\uff61\ud800\udc02",
                "\ud800\udc02",
                "\ud84d\udc56",
        
                // long strings, see cnormtst.c/TestNormCoverage()
                // equivalent if case-insensitive
                // 32..33
                "\uAD8B\uAD8B\uAD8B\uAD8B"+
                "\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "aaaaaaaaaaaaaaaaaazzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"+
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"+
                "ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"+
                "ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"+
                "\uAD8B\uAD8B\uAD8B\uAD8B"+
                "d\u031B\u0307\u0323",
        
                "\u1100\u116f\u11aa\uAD8B\uAD8B\u1100\u116f\u11aa"+
                "\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "\\U0001d15e\\U0001d157\\U0001d165\\U0001d15e\\U0001d15e\\U0001d15e\\U0001d15e"+
                "aaaaaaaaaaAAAAAAAAZZZZZZZZZZZZZZZZzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"+
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"+
                "ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"+
                "ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"+
                "\u1100\u116f\u11aa\uAD8B\uAD8B\u1100\u116f\u11aa"+
                "\u1E0C\u031B\u0307",
        
                // some strings that may make a difference whether the compare function
                // case-folds or decomposes first
                // 34..41
                "\u0360\u0345\u0334",
                "\u0360\u03b9\u0334",
        
                "\u0360\u1f80\u0334",
                "\u0360\u03b1\u0313\u03b9\u0334",
        
                "\u0360\u1ffc\u0334",
                "\u0360\u03c9\u03b9\u0334",
        
                "a\u0360\u0345\u0360\u0345b",
                "a\u0345\u0360\u0345\u0360b",
        
                // interesting cases for canonical caseless match with turkic i handling
                // 42..43
                "\u00cc",
                "\u0069\u0300",
        
                // strings with post-Unicode 3.2 normalization or normalization corrections
                // 44..45
                "\u00e4\u193b\\U0002f868",
                "\u0061\u193b\u0308\u36fc",


    };

    // all combinations of options
    // UNORM_INPUT_IS_FCD is set automatically if both input strings fulfill FCD conditions
    final class Temp {
        int options;
        String name;
        public Temp(int opt,String str){
            options =opt;
            name = str;
        }

    }
    // set UNORM_UNICODE_3_2 in one additional combination
  
    private Temp[] opt = new Temp[]{
                    new Temp(0,"default"),
                    new Temp(Normalizer.COMPARE_CODE_POINT_ORDER, "code point order" ),
                    new Temp(Normalizer.COMPARE_IGNORE_CASE, "ignore case" ),
                    new Temp(Normalizer.COMPARE_CODE_POINT_ORDER|Normalizer.COMPARE_IGNORE_CASE, "code point order & ignore case" ),
                    new Temp(Normalizer.COMPARE_IGNORE_CASE|Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I, "ignore case & special i"),
                    new Temp(Normalizer.COMPARE_CODE_POINT_ORDER|Normalizer.COMPARE_IGNORE_CASE|Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I, "code point order & ignore case & special i"),
                    new Temp(Normalizer.UNICODE_3_2 << Normalizer.COMPARE_NORM_OPTIONS_SHIFT, "Unicode 3.2")
            };


    public void TestCompareDebug(){

        String[] s = new String[100]; // at least as many items as in strings[] !


        int i, j, k, count=strings.length;
        int result, refResult;

        // create the UnicodeStrings
        for(i=0; i<count; ++i) {
            s[i]=Utility.unescape(strings[i]);
        }
        UTF16.StringComparator comp = new UTF16.StringComparator(true, false, 
                                     UTF16.StringComparator.FOLD_CASE_DEFAULT);
        // test them each with each other

        i = 42;
        j = 43;
        k = 2;
        // test Normalizer::compare
        result=norm_compare(s[i], s[j], opt[k].options);
        refResult=ref_norm_compare(s[i], s[j], opt[k].options);
        if(sign(result)!=sign(refResult)) {
            errln("Normalizer::compare( " + i +", "+j + ", " +k+"( " +opt[k].name+"))=" + result +" should be same sign as " + refResult);
        }

        // test UnicodeString::caseCompare - same internal implementation function
         if(0!=(opt[k].options&Normalizer.COMPARE_IGNORE_CASE)) {
        //    result=s[i]. (s[j], opt[k].options);
            if ((opt[k].options & Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I) == 0)
            {
                comp.setIgnoreCase(true, UTF16.StringComparator.FOLD_CASE_DEFAULT);
            }
            else {
                comp.setIgnoreCase(true, UTF16.StringComparator.FOLD_CASE_EXCLUDE_SPECIAL_I);
            }
            
            result=comp.compare(s[i],s[j]);
            refResult=ref_case_compare(s[i], s[j], opt[k].options);
            if(sign(result)!=sign(refResult)) {
                      errln("Normalizer::compare( " + i +", "+j + ", "+k+"( " +opt[k].name+"))=" + result +" should be same sign as " + refResult);
                            }
        }
        String value1 = "\u00dater\u00fd";
        String value2 = "\u00fater\u00fd";
        if(Normalizer.compare(value1,value2,0)!=0){
            if(Normalizer.compare(value1,value2,Normalizer.COMPARE_IGNORE_CASE)==0){

            }
        }
    }

    public void TestCompare() {

        String[] s = new String[100]; // at least as many items as in strings[] !

        int i, j, k, count=strings.length;
        int result, refResult;

        // create the UnicodeStrings
        for(i=0; i<count; ++i) {
            s[i]=Utility.unescape(strings[i]);
        }
        UTF16.StringComparator comp = new UTF16.StringComparator();
        // test them each with each other
        for(i=0; i<count; ++i) {
            for(j=i; j<count; ++j) {
                for(k=0; k<opt.length; ++k) {
                    // test Normalizer::compare
                    result=norm_compare(s[i], s[j], opt[k].options);
                    refResult=ref_norm_compare(s[i], s[j], opt[k].options);
                    if(sign(result)!=sign(refResult)) {
                        errln("Normalizer::compare( " + i +", "+j + ", " +k+"( " +opt[k].name+"))=" + result +" should be same sign as " + refResult);
                    }

                    // test UnicodeString::caseCompare - same internal implementation function
                     if(0!=(opt[k].options&Normalizer.COMPARE_IGNORE_CASE)) {
                        //    result=s[i]. (s[j], opt[k].options);
                        if ((opt[k].options & Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I) == 0)
                        {
                            comp.setIgnoreCase(true, UTF16.StringComparator.FOLD_CASE_DEFAULT);
                        }
                        else {
                            comp.setIgnoreCase(true, UTF16.StringComparator.FOLD_CASE_EXCLUDE_SPECIAL_I);
                        }
                        
                        comp.setCodePointCompare((opt[k].options & Normalizer.COMPARE_CODE_POINT_ORDER) != 0);
                        // result=comp.caseCompare(s[i],s[j], opt[k].options);
                        result=comp.compare(s[i],s[j]);
                        refResult=ref_case_compare(s[i], s[j], opt[k].options);
                        if(sign(result)!=sign(refResult)) {
                                  errln("Normalizer::compare( " + i +", "+j + ", "+k+"( " +opt[k].name+"))=" + result +" should be same sign as " + refResult);
                                         }
                    }
                }
            }
        }
        
        // test cases with i and I to make sure Turkic works
        char[] iI= new char[]{ 0x49, 0x69, 0x130, 0x131 };
        USerializedSet sset=new USerializedSet();
        UnicodeSet set = new UnicodeSet();
    
        String s1, s2;
        int start, end;
    
        // collect all sets into one for contiguous output
        int[] startEnd = new int[2];
        for(i=0; i<iI.length; ++i) {
            if(NormalizerImpl.getCanonStartSet(iI[i], sset)) {
                count=sset.countRanges();
                for(j=0; j<count; ++j) {
                    sset.getRange(j, startEnd);
                    set.add(startEnd[0], startEnd[1]);
                }
            }
        }

        // test all of these precomposed characters
        UnicodeSetIterator it = new UnicodeSetIterator(set);
        while(it.nextRange() && it.codepoint!=UnicodeSetIterator.IS_STRING) {
            start=it.codepoint;
            end=it.codepointEnd;
            while(start<=end) {
                s1 = Integer.toString(start);
                s2 = Normalizer.decompose(s1, false, 0);
//                if(U_FAILURE(errorCode)) {
//                    errln("Normalizer::decompose(U+%04x) failed: %s", start, u_errorName(errorCode));
//                    return;
//                }
                for(k=0; k<opt.length; ++k) {
                    // test Normalizer::compare

                    result= norm_compare(s1, s2, opt[k].options);
                    refResult=ref_norm_compare(s1, s2, opt[k].options);
                    if(sign(result)!=sign(refResult)) {
                        errln("Normalizer.compare(U+"+hex(start)+" with its NFD, "+opt[k].name+")" 
                              + signString(result)+" should be "+signString(refResult));
                    }
    
                    // test UnicodeString::caseCompare - same internal implementation function
                    if((opt[k].options & Normalizer.COMPARE_IGNORE_CASE)>0) {
                         if ((opt[k].options & Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I) == 0)
                        {
                            comp.setIgnoreCase(true, UTF16.StringComparator.FOLD_CASE_DEFAULT);
                        }
                        else {
                            comp.setIgnoreCase(true, UTF16.StringComparator.FOLD_CASE_EXCLUDE_SPECIAL_I);
                        }
                        
                        comp.setCodePointCompare((opt[k].options & Normalizer.COMPARE_CODE_POINT_ORDER) != 0);
         
                        result=comp.compare(s1,s2);
                        refResult=ref_case_compare(s1, s2, opt[k].options);
                        if(sign(result)!=sign(refResult)) {
                            errln("UTF16.compare(U+"+hex(start)+" with its NFD, "
                                  +opt[k].name+")"+signString(result) +" should be "+signString(refResult));
                        }
                    }
                }
    
                ++start;
            }
        }

    }

    // verify that case-folding does not un-FCD strings
    int countFoldFCDExceptions(int foldingOptions) {
        String s, d;
        int c;
        int count;
        int/*unsigned*/ cc, trailCC, foldCC, foldTrailCC;
        Normalizer.QuickCheckResult qcResult;
        int category;
        boolean isNFD;


        logln("Test if case folding may un-FCD a string (folding options 0x)"+hex(foldingOptions));

        count=0;
        for(c=0; c<=0x10ffff; ++c) {
            category=UCharacter.getType(c);
            if(category==UCharacterCategory.UNASSIGNED) {
                continue; // skip unassigned code points
            }
            if(c==0xac00) {
                c=0xd7a3; // skip Hangul - no case folding there
                continue;
            }
            // skip Han blocks - no case folding there either
            if(c==0x3400) {
                c=0x4db5;
                continue;
            }
            if(c==0x4e00) {
                c=0x9fa5;
                continue;
            }
            if(c==0x20000) {
                c=0x2a6d6;
                continue;
            }

            s= UTF16.valueOf(c);

            // get leading and trailing cc for c
            d= Normalizer.decompose(s,false);
            isNFD= s==d;
            cc=UCharacter.getCombiningClass(UTF16.charAt(d,0));
            trailCC=UCharacter.getCombiningClass(UTF16.charAt(d,d.length()-1));

            // get leading and trailing cc for the case-folding of c
            UCharacter.foldCase(s,(foldingOptions==0));
            d = Normalizer.decompose(s, false);
            foldCC=UCharacter.getCombiningClass(UTF16.charAt(d,0));
            foldTrailCC=UCharacter.getCombiningClass(UTF16.charAt(d,d.length()-1));

            qcResult=Normalizer.quickCheck(s, Normalizer.FCD,0);


            // bad:
            // - character maps to empty string: adjacent characters may then need reordering
            // - folding has different leading/trailing cc's, and they don't become just 0
            // - folding itself is not FCD
            if( qcResult!=Normalizer.YES ||
                s.length()==0 ||
                (cc!=foldCC && foldCC!=0) || (trailCC!=foldTrailCC && foldTrailCC!=0)
            ) {
                ++count;
                errln("U+"+hex(c)+": case-folding may un-FCD a string (folding options 0x"+hex(foldingOptions)+")");
                //errln("  cc %02x trailCC %02x    foldCC(U+%04lx) %02x foldTrailCC(U+%04lx) %02x   quickCheck(folded)=%d", cc, trailCC, UTF16.charAt(d,0), foldCC, UTF16.charAt(d,d.length()-1), foldTrailCC, qcResult);
                continue;
            }

            // also bad:
            // if a code point is in NFD but its case folding is not, then
            // unorm_compare will also fail
            if(isNFD && Normalizer.YES!=Normalizer.quickCheck(s, Normalizer.NFD,0)) {
                ++count;
                errln("U+"+hex(c)+": case-folding may un-FCD a string (folding options 0x"+hex(foldingOptions)+")");
            }
        }

        logln("There are "+hex(count)+" code points for which case-folding may un-FCD a string (folding options"+foldingOptions+"x)" );
        return count;
    }

    public void TestFindFoldFCDExceptions() {
        int count;

        count=countFoldFCDExceptions(0);
        count+=countFoldFCDExceptions(Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I);
        if(count>0) {
            //*
            //* If case-folding un-FCDs any strings, then unorm_compare() must be
            //* re-implemented.
            //* It currently assumes that one can check for FCD then case-fold
            //* and then still have FCD strings for raw decomposition without reordering.
            //*
            errln("error: There are "+count+" code points for which case-folding"+
                  " may un-FCD a string for all folding options.\n See comment"+
                  " in BasicNormalizerTest::FindFoldFCDExceptions()!");
        }
    }
    
    public void TestCombiningMarks(){
        String src = "\u0f71\u0f72\u0f73\u0f74\u0f75";
        String expected = "\u0F71\u0F71\u0F71\u0F72\u0F72\u0F74\u0F74";
        String result = Normalizer.decompose(src,false);
        if(!expected.equals(result)){
            errln("Reordering of combining marks failed. Expected: "+Utility.hex(expected)+" Got: "+ Utility.hex(result));
        }
    }

    /*
     * Re-enable this test when UTC fixes UAX 21
    public void TestUAX21Failure(){
        final String[][] cases = new String[][]{
                {"\u0061\u0345\u0360\u0345\u0062", "\u0061\u0360\u0345\u0345\u0062"},
                {"\u0061\u0345\u0345\u0360\u0062", "\u0061\u0360\u0345\u0345\u0062"},
                {"\u0061\u0345\u0360\u0362\u0360\u0062", "\u0061\u0362\u0360\u0360\u0345\u0062"},
                {"\u0061\u0360\u0345\u0360\u0362\u0062", "\u0061\u0362\u0360\u0360\u0345\u0062"},
                {"\u0061\u0345\u0360\u0362\u0361\u0062", "\u0061\u0362\u0360\u0361\u0345\u0062"},
                {"\u0061\u0361\u0345\u0360\u0362\u0062", "\u0061\u0362\u0361\u0360\u0345\u0062"},
        };
        for(int i = 0; i< cases.length; i++){
            String s1 =cases[0][0]; 
            String s2 = cases[0][1];
            if( (Normalizer.compare(s1,s2,Normalizer.FOLD_CASE_DEFAULT ==0)//case sensitive compare
                &&
                (Normalizer.compare(s1,s2,Normalizer.COMPARE_IGNORE_CASE)!=0)){
                errln("Normalizer.compare() failed for s1: " 
                        + Utility.hex(s1) +" s2: " + Utility.hex(s2));
            }
        }
    }
    */
    public void TestFCNFKCClosure() {
        final class TestStruct{
            int c;
            String s;
            TestStruct(int cp, String src){
                c=cp;
                s=src;
            }
        }
        
        TestStruct[] tests= new TestStruct[]{
            new TestStruct( 0x037A, "\u0020\u03B9" ),
            new TestStruct( 0x03D2, "\u03C5" ),
            new TestStruct( 0x20A8, "\u0072\u0073" ) ,
            new TestStruct( 0x210B, "\u0068" ),
            new TestStruct( 0x210C, "\u0068" ),
            new TestStruct( 0x2121, "\u0074\u0065\u006C" ),
            new TestStruct( 0x2122, "\u0074\u006D" ),
            new TestStruct( 0x2128, "\u007A" ),
            new TestStruct( 0x1D5DB,"\u0068" ),
            new TestStruct( 0x1D5ED,"\u007A" ),
            new TestStruct( 0x0061, "" )
        };
    

        for(int i = 0; i < tests.length; ++ i) {
            String result=Normalizer.getFC_NFKC_Closure(tests[i].c);
            if(!result.equals(new String(tests[i].s))) {
                errln("getFC_NFKC_Closure(U+"+Integer.toHexString(tests[i].c)+") is wrong");
            }
        }
    
        /* error handling */

        int length=Normalizer.getFC_NFKC_Closure(0x5c, null);
        if(length!=0){
            errln("getFC_NFKC_Closure did not perform error handling correctly");
        }
    }
    public void TestBugJ2324(){
       /* String[] input = new String[]{
                            //"\u30FD\u3099",
                            "\u30FA\u309A",
                            "\u30FB\u309A",
                            "\u30FC\u309A",
                            "\u30FE\u309A",
                            "\u30FD\u309A",

        };*/
        String troublesome = "\u309A";
        for(int i=0x3000; i<0x3100;i++){
            String input = ((char)i)+troublesome;
            try{                            
              /*  String result =*/ Normalizer.compose(input,false);
            }catch(IndexOutOfBoundsException e){
                errln("compose() failed for input: " + Utility.hex(input) + " Exception: " + e.toString());
            }
        }
                
    }

     static final int D = 0, C = 1, KD= 2, KC = 3, FCD=4, NONE=5;   
    private static UnicodeSet[] initSkippables(UnicodeSet[] skipSets){
        if( skipSets.length < 4 ){
            return null;
        }
        skipSets[D].applyPattern(
            "[^\\u00C0-\\u00C5\\u00C7-\\u00CF\\u00D1-\\u00D6\\u00D9-\\u00DD"
            + "\\u00E0-\\u00E5\\u00E7-\\u00EF\\u00F1-\\u00F6\\u00F9-\\u00FD"
            + "\\u00FF-\\u010F\\u0112-\\u0125\\u0128-\\u0130\\u0134-\\u0137"
            + "\\u0139-\\u013E\\u0143-\\u0148\\u014C-\\u0151\\u0154-\\u0165"
            + "\\u0168-\\u017E\\u01A0\\u01A1\\u01AF\\u01B0\\u01CD-\\u01DC"
            + "\\u01DE-\\u01E3\\u01E6-\\u01F0\\u01F4\\u01F5\\u01F8-\\u021B"
            + "\\u021E\\u021F\\u0226-\\u0233\\u0300-\\u034E\\u0350-\\u036F"
            + "\\u0374\\u037E\\u0385-\\u038A\\u038C\\u038E-\\u0390\\u03AA-"
            + "\\u03B0\\u03CA-\\u03CE\\u03D3\\u03D4\\u0400\\u0401\\u0403\\u0407"
            + "\\u040C-\\u040E\\u0419\\u0439\\u0450\\u0451\\u0453\\u0457\\u045C"
            + "-\\u045E\\u0476\\u0477\\u0483-\\u0487\\u04C1\\u04C2\\u04D0-"
            + "\\u04D3\\u04D6\\u04D7\\u04DA-\\u04DF\\u04E2-\\u04E7\\u04EA-"
            + "\\u04F5\\u04F8\\u04F9\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4"
            + "\\u05C5\\u05C7\\u0610-\\u061A\\u0622-\\u0626\\u064B-\\u065E"
            + "\\u0670\\u06C0\\u06C2\\u06D3\\u06D6-\\u06DC\\u06DF-\\u06E4"
            + "\\u06E7\\u06E8\\u06EA-\\u06ED\\u0711\\u0730-\\u074A\\u07EB-"
            + "\\u07F3\\u0816-\\u0819\\u081B-\\u0823\\u0825-\\u0827\\u0829-"
            + "\\u082D\\u0929\\u0931\\u0934\\u093C\\u094D\\u0951-\\u0954\\u0958"
            + "-\\u095F\\u09BC\\u09CB-\\u09CD\\u09DC\\u09DD\\u09DF\\u0A33"
            + "\\u0A36\\u0A3C\\u0A4D\\u0A59-\\u0A5B\\u0A5E\\u0ABC\\u0ACD\\u0B3C"
            + "\\u0B48\\u0B4B-\\u0B4D\\u0B5C\\u0B5D\\u0B94\\u0BCA-\\u0BCD"
            + "\\u0C48\\u0C4D\\u0C55\\u0C56\\u0CBC\\u0CC0\\u0CC7\\u0CC8\\u0CCA"
            + "\\u0CCB\\u0CCD\\u0D4A-\\u0D4D\\u0DCA\\u0DDA\\u0DDC-\\u0DDE"
            + "\\u0E38-\\u0E3A\\u0E48-\\u0E4B\\u0EB8\\u0EB9\\u0EC8-\\u0ECB"
            + "\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39\\u0F43\\u0F4D\\u0F52\\u0F57"
            + "\\u0F5C\\u0F69\\u0F71-\\u0F76\\u0F78\\u0F7A-\\u0F7D\\u0F80-"
            + "\\u0F84\\u0F86\\u0F87\\u0F93\\u0F9D\\u0FA2\\u0FA7\\u0FAC\\u0FB9"
            + "\\u0FC6\\u1026\\u1037\\u1039\\u103A\\u108D\\u135F\\u1714\\u1734"
            + "\\u17D2\\u17DD\\u18A9\\u1939-\\u193B\\u1A17\\u1A18\\u1A60\\u1A75"
            + "-\\u1A7C\\u1A7F\\u1B06\\u1B08\\u1B0A\\u1B0C\\u1B0E\\u1B12\\u1B34"
            + "\\u1B3B\\u1B3D\\u1B40\\u1B41\\u1B43\\u1B44\\u1B6B-\\u1B73\\u1BAA"
            + "\\u1C37\\u1CD0-\\u1CD2\\u1CD4-\\u1CE0\\u1CE2-\\u1CE8\\u1CED"
            + "\\u1DC0-\\u1DE6\\u1DFD-\\u1E99\\u1E9B\\u1EA0-\\u1EF9\\u1F00-"
            + "\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D\\u1F50-"
            + "\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F7D\\u1F80-\\u1FB4"
            + "\\u1FB6-\\u1FBC\\u1FBE\\u1FC1-\\u1FC4\\u1FC6-\\u1FD3\\u1FD6-"
            + "\\u1FDB\\u1FDD-\\u1FEF\\u1FF2-\\u1FF4\\u1FF6-\\u1FFD\\u2000"
            + "\\u2001\\u20D0-\\u20DC\\u20E1\\u20E5-\\u20F0\\u2126\\u212A"
            + "\\u212B\\u219A\\u219B\\u21AE\\u21CD-\\u21CF\\u2204\\u2209\\u220C"
            + "\\u2224\\u2226\\u2241\\u2244\\u2247\\u2249\\u2260\\u2262\\u226D-"
            + "\\u2271\\u2274\\u2275\\u2278\\u2279\\u2280\\u2281\\u2284\\u2285"
            + "\\u2288\\u2289\\u22AC-\\u22AF\\u22E0-\\u22E3\\u22EA-\\u22ED"
            + "\\u2329\\u232A\\u2ADC\\u2CEF-\\u2CF1\\u2DE0-\\u2DFF\\u302A-"
            + "\\u302F\\u304C\\u304E\\u3050\\u3052\\u3054\\u3056\\u3058\\u305A"
            + "\\u305C\\u305E\\u3060\\u3062\\u3065\\u3067\\u3069\\u3070\\u3071"
            + "\\u3073\\u3074\\u3076\\u3077\\u3079\\u307A\\u307C\\u307D\\u3094"
            + "\\u3099\\u309A\\u309E\\u30AC\\u30AE\\u30B0\\u30B2\\u30B4\\u30B6"
            + "\\u30B8\\u30BA\\u30BC\\u30BE\\u30C0\\u30C2\\u30C5\\u30C7\\u30C9"
            + "\\u30D0\\u30D1\\u30D3\\u30D4\\u30D6\\u30D7\\u30D9\\u30DA\\u30DC"
            + "\\u30DD\\u30F4\\u30F7-\\u30FA\\u30FE\\uA66F\\uA67C\\uA67D\\uA6F0"
            + "\\uA6F1\\uA806\\uA8C4\\uA8E0-\\uA8F1\\uA92B-\\uA92D\\uA953"
            + "\\uA9B3\\uA9C0\\uAAB0\\uAAB2-\\uAAB4\\uAAB7\\uAAB8\\uAABE\\uAABF"
            + "\\uAAC1\\uABED\\uAC00-\\uD7A3\\uF900-\\uFA0D\\uFA10\\uFA12"
            + "\\uFA15-\\uFA1E\\uFA20\\uFA22\\uFA25\\uFA26\\uFA2A-\\uFA2D"
            + "\\uFA30-\\uFA6D\\uFA70-\\uFAD9\\uFB1D-\\uFB1F\\uFB2A-\\uFB36"
            + "\\uFB38-\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46-"
            + "\\uFB4E\\uFE20-\\uFE26\\U000101FD\\U00010A0D\\U00010A0F\\U00010A"
            + "38-\\U00010A3A\\U00010A3F\\U0001109A\\U0001109C\\U000110AB"
            + "\\U000110B9\\U000110BA\\U0001D15E-\\U0001D169\\U0001D16D-\\U0001"
            + "D172\\U0001D17B-\\U0001D182\\U0001D185-\\U0001D18B\\U0001D1AA-"
            + "\\U0001D1AD\\U0001D1BB-\\U0001D1C0\\U0001D242-\\U0001D244\\U0002"
            + "F800-\\U0002FA1D]", false);

      skipSets[C].applyPattern(
          "[^<->A-PR-Za-pr-z\\u00A8\\u00C0-\\u00CF\\u00D1-\\u00D6\\u00D8-"
          + "\\u00DD\\u00E0-\\u00EF\\u00F1-\\u00F6\\u00F8-\\u00FD\\u00FF-"
          + "\\u0103\\u0106-\\u010F\\u0112-\\u0117\\u011A-\\u0121\\u0124"
          + "\\u0125\\u0128-\\u012D\\u0130\\u0139\\u013A\\u013D\\u013E\\u0143"
          + "\\u0144\\u0147\\u0148\\u014C-\\u0151\\u0154\\u0155\\u0158-"
          + "\\u015D\\u0160\\u0161\\u0164\\u0165\\u0168-\\u0171\\u0174-"
          + "\\u017F\\u01A0\\u01A1\\u01AF\\u01B0\\u01B7\\u01CD-\\u01DC\\u01DE"
          + "-\\u01E1\\u01E6-\\u01EB\\u01F4\\u01F5\\u01F8-\\u01FB\\u0200-"
          + "\\u021B\\u021E\\u021F\\u0226-\\u0233\\u0292\\u0300-\\u034E"
          + "\\u0350-\\u036F\\u0374\\u037E\\u0387\\u0391\\u0395\\u0397\\u0399"
          + "\\u039F\\u03A1\\u03A5\\u03A9\\u03AC\\u03AE\\u03B1\\u03B5\\u03B7"
          + "\\u03B9\\u03BF\\u03C1\\u03C5\\u03C9-\\u03CB\\u03CE\\u03D2\\u0406"
          + "\\u0410\\u0413\\u0415-\\u0418\\u041A\\u041E\\u0423\\u0427\\u042B"
          + "\\u042D\\u0430\\u0433\\u0435-\\u0438\\u043A\\u043E\\u0443\\u0447"
          + "\\u044B\\u044D\\u0456\\u0474\\u0475\\u0483-\\u0487\\u04D8\\u04D9"
          + "\\u04E8\\u04E9\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4\\u05C5"
          + "\\u05C7\\u0610-\\u061A\\u0622\\u0623\\u0627\\u0648\\u064A-"
          + "\\u065E\\u0670\\u06C1\\u06D2\\u06D5-\\u06DC\\u06DF-\\u06E4"
          + "\\u06E7\\u06E8\\u06EA-\\u06ED\\u0711\\u0730-\\u074A\\u07EB-"
          + "\\u07F3\\u0816-\\u0819\\u081B-\\u0823\\u0825-\\u0827\\u0829-"
          + "\\u082D\\u0928\\u0930\\u0933\\u093C\\u094D\\u0951-\\u0954\\u0958"
          + "-\\u095F\\u09BC\\u09BE\\u09C7\\u09CD\\u09D7\\u09DC\\u09DD\\u09DF"
          + "\\u0A33\\u0A36\\u0A3C\\u0A4D\\u0A59-\\u0A5B\\u0A5E\\u0ABC\\u0ACD"
          + "\\u0B3C\\u0B3E\\u0B47\\u0B4D\\u0B56\\u0B57\\u0B5C\\u0B5D\\u0B92"
          + "\\u0BBE\\u0BC6\\u0BC7\\u0BCD\\u0BD7\\u0C46\\u0C4D\\u0C55\\u0C56"
          + "\\u0CBC\\u0CBF\\u0CC2\\u0CC6\\u0CCA\\u0CCD\\u0CD5\\u0CD6\\u0D3E"
          + "\\u0D46\\u0D47\\u0D4D\\u0D57\\u0DCA\\u0DCF\\u0DD9\\u0DDC\\u0DDF"
          + "\\u0E38-\\u0E3A\\u0E48-\\u0E4B\\u0EB8\\u0EB9\\u0EC8-\\u0ECB"
          + "\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39\\u0F43\\u0F4D\\u0F52\\u0F57"
          + "\\u0F5C\\u0F69\\u0F71-\\u0F76\\u0F78\\u0F7A-\\u0F7D\\u0F80-"
          + "\\u0F84\\u0F86\\u0F87\\u0F93\\u0F9D\\u0FA2\\u0FA7\\u0FAC\\u0FB9"
          + "\\u0FC6\\u1025\\u102E\\u1037\\u1039\\u103A\\u108D\\u1100-\\u1112"
          + "\\u1161-\\u1175\\u11A8-\\u11C2\\u135F\\u1714\\u1734\\u17D2"
          + "\\u17DD\\u18A9\\u1939-\\u193B\\u1A17\\u1A18\\u1A60\\u1A75-"
          + "\\u1A7C\\u1A7F\\u1B05\\u1B07\\u1B09\\u1B0B\\u1B0D\\u1B11\\u1B34"
          + "\\u1B35\\u1B3A\\u1B3C\\u1B3E\\u1B3F\\u1B42\\u1B44\\u1B6B-\\u1B73"
          + "\\u1BAA\\u1C37\\u1CD0-\\u1CD2\\u1CD4-\\u1CE0\\u1CE2-\\u1CE8"
          + "\\u1CED\\u1DC0-\\u1DE6\\u1DFD-\\u1E03\\u1E0A-\\u1E0F\\u1E12-"
          + "\\u1E1B\\u1E20-\\u1E27\\u1E2A-\\u1E41\\u1E44-\\u1E53\\u1E58-"
          + "\\u1E7D\\u1E80-\\u1E87\\u1E8E-\\u1E91\\u1E96-\\u1E99\\u1EA0-"
          + "\\u1EF3\\u1EF6-\\u1EF9\\u1F00-\\u1F11\\u1F18\\u1F19\\u1F20-"
          + "\\u1F31\\u1F38\\u1F39\\u1F40\\u1F41\\u1F48\\u1F49\\u1F50\\u1F51"
          + "\\u1F59\\u1F60-\\u1F71\\u1F73-\\u1F75\\u1F77\\u1F79\\u1F7B-"
          + "\\u1F7D\\u1F80\\u1F81\\u1F88\\u1F89\\u1F90\\u1F91\\u1F98\\u1F99"
          + "\\u1FA0\\u1FA1\\u1FA8\\u1FA9\\u1FB3\\u1FB6\\u1FBB\\u1FBC\\u1FBE"
          + "\\u1FBF\\u1FC3\\u1FC6\\u1FC9\\u1FCB\\u1FCC\\u1FD3\\u1FDB\\u1FE3"
          + "\\u1FEB\\u1FEE\\u1FEF\\u1FF3\\u1FF6\\u1FF9\\u1FFB-\\u1FFE\\u2000"
          + "\\u2001\\u20D0-\\u20DC\\u20E1\\u20E5-\\u20F0\\u2126\\u212A"
          + "\\u212B\\u2190\\u2192\\u2194\\u21D0\\u21D2\\u21D4\\u2203\\u2208"
          + "\\u220B\\u2223\\u2225\\u223C\\u2243\\u2245\\u2248\\u224D\\u2261"
          + "\\u2264\\u2265\\u2272\\u2273\\u2276\\u2277\\u227A-\\u227D\\u2282"
          + "\\u2283\\u2286\\u2287\\u2291\\u2292\\u22A2\\u22A8\\u22A9\\u22AB"
          + "\\u22B2-\\u22B5\\u2329\\u232A\\u2ADC\\u2CEF-\\u2CF1\\u2DE0-"
          + "\\u2DFF\\u302A-\\u302F\\u3046\\u304B\\u304D\\u304F\\u3051\\u3053"
          + "\\u3055\\u3057\\u3059\\u305B\\u305D\\u305F\\u3061\\u3064\\u3066"
          + "\\u3068\\u306F\\u3072\\u3075\\u3078\\u307B\\u3099\\u309A\\u309D"
          + "\\u30A6\\u30AB\\u30AD\\u30AF\\u30B1\\u30B3\\u30B5\\u30B7\\u30B9"
          + "\\u30BB\\u30BD\\u30BF\\u30C1\\u30C4\\u30C6\\u30C8\\u30CF\\u30D2"
          + "\\u30D5\\u30D8\\u30DB\\u30EF-\\u30F2\\u30FD\\uA66F\\uA67C\\uA67D"
          + "\\uA6F0\\uA6F1\\uA806\\uA8C4\\uA8E0-\\uA8F1\\uA92B-\\uA92D"
          + "\\uA953\\uA9B3\\uA9C0\\uAAB0\\uAAB2-\\uAAB4\\uAAB7\\uAAB8\\uAABE"
          + "\\uAABF\\uAAC1\\uABED\\uAC00\\uAC1C\\uAC38\\uAC54\\uAC70\\uAC8C"
          + "\\uACA8\\uACC4\\uACE0\\uACFC\\uAD18\\uAD34\\uAD50\\uAD6C\\uAD88"
          + "\\uADA4\\uADC0\\uADDC\\uADF8\\uAE14\\uAE30\\uAE4C\\uAE68\\uAE84"
          + "\\uAEA0\\uAEBC\\uAED8\\uAEF4\\uAF10\\uAF2C\\uAF48\\uAF64\\uAF80"
          + "\\uAF9C\\uAFB8\\uAFD4\\uAFF0\\uB00C\\uB028\\uB044\\uB060\\uB07C"
          + "\\uB098\\uB0B4\\uB0D0\\uB0EC\\uB108\\uB124\\uB140\\uB15C\\uB178"
          + "\\uB194\\uB1B0\\uB1CC\\uB1E8\\uB204\\uB220\\uB23C\\uB258\\uB274"
          + "\\uB290\\uB2AC\\uB2C8\\uB2E4\\uB300\\uB31C\\uB338\\uB354\\uB370"
          + "\\uB38C\\uB3A8\\uB3C4\\uB3E0\\uB3FC\\uB418\\uB434\\uB450\\uB46C"
          + "\\uB488\\uB4A4\\uB4C0\\uB4DC\\uB4F8\\uB514\\uB530\\uB54C\\uB568"
          + "\\uB584\\uB5A0\\uB5BC\\uB5D8\\uB5F4\\uB610\\uB62C\\uB648\\uB664"
          + "\\uB680\\uB69C\\uB6B8\\uB6D4\\uB6F0\\uB70C\\uB728\\uB744\\uB760"
          + "\\uB77C\\uB798\\uB7B4\\uB7D0\\uB7EC\\uB808\\uB824\\uB840\\uB85C"
          + "\\uB878\\uB894\\uB8B0\\uB8CC\\uB8E8\\uB904\\uB920\\uB93C\\uB958"
          + "\\uB974\\uB990\\uB9AC\\uB9C8\\uB9E4\\uBA00\\uBA1C\\uBA38\\uBA54"
          + "\\uBA70\\uBA8C\\uBAA8\\uBAC4\\uBAE0\\uBAFC\\uBB18\\uBB34\\uBB50"
          + "\\uBB6C\\uBB88\\uBBA4\\uBBC0\\uBBDC\\uBBF8\\uBC14\\uBC30\\uBC4C"
          + "\\uBC68\\uBC84\\uBCA0\\uBCBC\\uBCD8\\uBCF4\\uBD10\\uBD2C\\uBD48"
          + "\\uBD64\\uBD80\\uBD9C\\uBDB8\\uBDD4\\uBDF0\\uBE0C\\uBE28\\uBE44"
          + "\\uBE60\\uBE7C\\uBE98\\uBEB4\\uBED0\\uBEEC\\uBF08\\uBF24\\uBF40"
          + "\\uBF5C\\uBF78\\uBF94\\uBFB0\\uBFCC\\uBFE8\\uC004\\uC020\\uC03C"
          + "\\uC058\\uC074\\uC090\\uC0AC\\uC0C8\\uC0E4\\uC100\\uC11C\\uC138"
          + "\\uC154\\uC170\\uC18C\\uC1A8\\uC1C4\\uC1E0\\uC1FC\\uC218\\uC234"
          + "\\uC250\\uC26C\\uC288\\uC2A4\\uC2C0\\uC2DC\\uC2F8\\uC314\\uC330"
          + "\\uC34C\\uC368\\uC384\\uC3A0\\uC3BC\\uC3D8\\uC3F4\\uC410\\uC42C"
          + "\\uC448\\uC464\\uC480\\uC49C\\uC4B8\\uC4D4\\uC4F0\\uC50C\\uC528"
          + "\\uC544\\uC560\\uC57C\\uC598\\uC5B4\\uC5D0\\uC5EC\\uC608\\uC624"
          + "\\uC640\\uC65C\\uC678\\uC694\\uC6B0\\uC6CC\\uC6E8\\uC704\\uC720"
          + "\\uC73C\\uC758\\uC774\\uC790\\uC7AC\\uC7C8\\uC7E4\\uC800\\uC81C"
          + "\\uC838\\uC854\\uC870\\uC88C\\uC8A8\\uC8C4\\uC8E0\\uC8FC\\uC918"
          + "\\uC934\\uC950\\uC96C\\uC988\\uC9A4\\uC9C0\\uC9DC\\uC9F8\\uCA14"
          + "\\uCA30\\uCA4C\\uCA68\\uCA84\\uCAA0\\uCABC\\uCAD8\\uCAF4\\uCB10"
          + "\\uCB2C\\uCB48\\uCB64\\uCB80\\uCB9C\\uCBB8\\uCBD4\\uCBF0\\uCC0C"
          + "\\uCC28\\uCC44\\uCC60\\uCC7C\\uCC98\\uCCB4\\uCCD0\\uCCEC\\uCD08"
          + "\\uCD24\\uCD40\\uCD5C\\uCD78\\uCD94\\uCDB0\\uCDCC\\uCDE8\\uCE04"
          + "\\uCE20\\uCE3C\\uCE58\\uCE74\\uCE90\\uCEAC\\uCEC8\\uCEE4\\uCF00"
          + "\\uCF1C\\uCF38\\uCF54\\uCF70\\uCF8C\\uCFA8\\uCFC4\\uCFE0\\uCFFC"
          + "\\uD018\\uD034\\uD050\\uD06C\\uD088\\uD0A4\\uD0C0\\uD0DC\\uD0F8"
          + "\\uD114\\uD130\\uD14C\\uD168\\uD184\\uD1A0\\uD1BC\\uD1D8\\uD1F4"
          + "\\uD210\\uD22C\\uD248\\uD264\\uD280\\uD29C\\uD2B8\\uD2D4\\uD2F0"
          + "\\uD30C\\uD328\\uD344\\uD360\\uD37C\\uD398\\uD3B4\\uD3D0\\uD3EC"
          + "\\uD408\\uD424\\uD440\\uD45C\\uD478\\uD494\\uD4B0\\uD4CC\\uD4E8"
          + "\\uD504\\uD520\\uD53C\\uD558\\uD574\\uD590\\uD5AC\\uD5C8\\uD5E4"
          + "\\uD600\\uD61C\\uD638\\uD654\\uD670\\uD68C\\uD6A8\\uD6C4\\uD6E0"
          + "\\uD6FC\\uD718\\uD734\\uD750\\uD76C\\uD788\\uF900-\\uFA0D\\uFA10"
          + "\\uFA12\\uFA15-\\uFA1E\\uFA20\\uFA22\\uFA25\\uFA26\\uFA2A-"
          + "\\uFA2D\\uFA30-\\uFA6D\\uFA70-\\uFAD9\\uFB1D-\\uFB1F\\uFB2A-"
          + "\\uFB36\\uFB38-\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46"
          + "-\\uFB4E\\uFE20-\\uFE26\\U000101FD\\U00010A0D\\U00010A0F\\U00010"
          + "A38-\\U00010A3A\\U00010A3F\\U00011099\\U0001109B\\U000110A5"
          + "\\U000110B9\\U000110BA\\U0001D15E-\\U0001D169\\U0001D16D-\\U0001"
          + "D172\\U0001D17B-\\U0001D182\\U0001D185-\\U0001D18B\\U0001D1AA-"
          + "\\U0001D1AD\\U0001D1BB-\\U0001D1C0\\U0001D242-\\U0001D244\\U0002"
          + "F800-\\U0002FA1D]", false);
   
        skipSets[KD].applyPattern(
            "[^\\u00A0\\u00A8\\u00AA\\u00AF\\u00B2-\\u00B5\\u00B8-\\u00BA"
            + "\\u00BC-\\u00BE\\u00C0-\\u00C5\\u00C7-\\u00CF\\u00D1-\\u00D6"
            + "\\u00D9-\\u00DD\\u00E0-\\u00E5\\u00E7-\\u00EF\\u00F1-\\u00F6"
            + "\\u00F9-\\u00FD\\u00FF-\\u010F\\u0112-\\u0125\\u0128-\\u0130"
            + "\\u0132-\\u0137\\u0139-\\u0140\\u0143-\\u0149\\u014C-\\u0151"
            + "\\u0154-\\u0165\\u0168-\\u017F\\u01A0\\u01A1\\u01AF\\u01B0"
            + "\\u01C4-\\u01DC\\u01DE-\\u01E3\\u01E6-\\u01F5\\u01F8-\\u021B"
            + "\\u021E\\u021F\\u0226-\\u0233\\u02B0-\\u02B8\\u02D8-\\u02DD"
            + "\\u02E0-\\u02E4\\u0300-\\u034E\\u0350-\\u036F\\u0374\\u037A"
            + "\\u037E\\u0384-\\u038A\\u038C\\u038E-\\u0390\\u03AA-\\u03B0"
            + "\\u03CA-\\u03CE\\u03D0-\\u03D6\\u03F0-\\u03F2\\u03F4\\u03F5"
            + "\\u03F9\\u0400\\u0401\\u0403\\u0407\\u040C-\\u040E\\u0419\\u0439"
            + "\\u0450\\u0451\\u0453\\u0457\\u045C-\\u045E\\u0476\\u0477\\u0483"
            + "-\\u0487\\u04C1\\u04C2\\u04D0-\\u04D3\\u04D6\\u04D7\\u04DA-"
            + "\\u04DF\\u04E2-\\u04E7\\u04EA-\\u04F5\\u04F8\\u04F9\\u0587"
            + "\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4\\u05C5\\u05C7\\u0610"
            + "-\\u061A\\u0622-\\u0626\\u064B-\\u065E\\u0670\\u0675-\\u0678"
            + "\\u06C0\\u06C2\\u06D3\\u06D6-\\u06DC\\u06DF-\\u06E4\\u06E7"
            + "\\u06E8\\u06EA-\\u06ED\\u0711\\u0730-\\u074A\\u07EB-\\u07F3"
            + "\\u0816-\\u0819\\u081B-\\u0823\\u0825-\\u0827\\u0829-\\u082D"
            + "\\u0929\\u0931\\u0934\\u093C\\u094D\\u0951-\\u0954\\u0958-"
            + "\\u095F\\u09BC\\u09CB-\\u09CD\\u09DC\\u09DD\\u09DF\\u0A33\\u0A36"
            + "\\u0A3C\\u0A4D\\u0A59-\\u0A5B\\u0A5E\\u0ABC\\u0ACD\\u0B3C\\u0B48"
            + "\\u0B4B-\\u0B4D\\u0B5C\\u0B5D\\u0B94\\u0BCA-\\u0BCD\\u0C48"
            + "\\u0C4D\\u0C55\\u0C56\\u0CBC\\u0CC0\\u0CC7\\u0CC8\\u0CCA\\u0CCB"
            + "\\u0CCD\\u0D4A-\\u0D4D\\u0DCA\\u0DDA\\u0DDC-\\u0DDE\\u0E33"
            + "\\u0E38-\\u0E3A\\u0E48-\\u0E4B\\u0EB3\\u0EB8\\u0EB9\\u0EC8-"
            + "\\u0ECB\\u0EDC\\u0EDD\\u0F0C\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39"
            + "\\u0F43\\u0F4D\\u0F52\\u0F57\\u0F5C\\u0F69\\u0F71-\\u0F7D\\u0F80"
            + "-\\u0F84\\u0F86\\u0F87\\u0F93\\u0F9D\\u0FA2\\u0FA7\\u0FAC\\u0FB9"
            + "\\u0FC6\\u1026\\u1037\\u1039\\u103A\\u108D\\u10FC\\u135F\\u1714"
            + "\\u1734\\u17D2\\u17DD\\u18A9\\u1939-\\u193B\\u1A17\\u1A18\\u1A60"
            + "\\u1A75-\\u1A7C\\u1A7F\\u1B06\\u1B08\\u1B0A\\u1B0C\\u1B0E\\u1B12"
            + "\\u1B34\\u1B3B\\u1B3D\\u1B40\\u1B41\\u1B43\\u1B44\\u1B6B-\\u1B73"
            + "\\u1BAA\\u1C37\\u1CD0-\\u1CD2\\u1CD4-\\u1CE0\\u1CE2-\\u1CE8"
            + "\\u1CED\\u1D2C-\\u1D2E\\u1D30-\\u1D3A\\u1D3C-\\u1D4D\\u1D4F-"
            + "\\u1D6A\\u1D78\\u1D9B-\\u1DE6\\u1DFD-\\u1E9B\\u1EA0-\\u1EF9"
            + "\\u1F00-\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D"
            + "\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F7D\\u1F80-"
            + "\\u1FB4\\u1FB6-\\u1FC4\\u1FC6-\\u1FD3\\u1FD6-\\u1FDB\\u1FDD-"
            + "\\u1FEF\\u1FF2-\\u1FF4\\u1FF6-\\u1FFE\\u2000-\\u200A\\u2011"
            + "\\u2017\\u2024-\\u2026\\u202F\\u2033\\u2034\\u2036\\u2037\\u203C"
            + "\\u203E\\u2047-\\u2049\\u2057\\u205F\\u2070\\u2071\\u2074-"
            + "\\u208E\\u2090-\\u2094\\u20A8\\u20D0-\\u20DC\\u20E1\\u20E5-"
            + "\\u20F0\\u2100-\\u2103\\u2105-\\u2107\\u2109-\\u2113\\u2115"
            + "\\u2116\\u2119-\\u211D\\u2120-\\u2122\\u2124\\u2126\\u2128"
            + "\\u212A-\\u212D\\u212F-\\u2131\\u2133-\\u2139\\u213B-\\u2140"
            + "\\u2145-\\u2149\\u2150-\\u217F\\u2189\\u219A\\u219B\\u21AE"
            + "\\u21CD-\\u21CF\\u2204\\u2209\\u220C\\u2224\\u2226\\u222C\\u222D"
            + "\\u222F\\u2230\\u2241\\u2244\\u2247\\u2249\\u2260\\u2262\\u226D-"
            + "\\u2271\\u2274\\u2275\\u2278\\u2279\\u2280\\u2281\\u2284\\u2285"
            + "\\u2288\\u2289\\u22AC-\\u22AF\\u22E0-\\u22E3\\u22EA-\\u22ED"
            + "\\u2329\\u232A\\u2460-\\u24EA\\u2A0C\\u2A74-\\u2A76\\u2ADC"
            + "\\u2C7C\\u2C7D\\u2CEF-\\u2CF1\\u2D6F\\u2DE0-\\u2DFF\\u2E9F"
            + "\\u2EF3\\u2F00-\\u2FD5\\u3000\\u302A-\\u302F\\u3036\\u3038-"
            + "\\u303A\\u304C\\u304E\\u3050\\u3052\\u3054\\u3056\\u3058\\u305A"
            + "\\u305C\\u305E\\u3060\\u3062\\u3065\\u3067\\u3069\\u3070\\u3071"
            + "\\u3073\\u3074\\u3076\\u3077\\u3079\\u307A\\u307C\\u307D\\u3094"
            + "\\u3099-\\u309C\\u309E\\u309F\\u30AC\\u30AE\\u30B0\\u30B2\\u30B4"
            + "\\u30B6\\u30B8\\u30BA\\u30BC\\u30BE\\u30C0\\u30C2\\u30C5\\u30C7"
            + "\\u30C9\\u30D0\\u30D1\\u30D3\\u30D4\\u30D6\\u30D7\\u30D9\\u30DA"
            + "\\u30DC\\u30DD\\u30F4\\u30F7-\\u30FA\\u30FE\\u30FF\\u3131-"
            + "\\u318E\\u3192-\\u319F\\u3200-\\u321E\\u3220-\\u3247\\u3250-"
            + "\\u327E\\u3280-\\u32FE\\u3300-\\u33FF\\uA66F\\uA67C\\uA67D"
            + "\\uA6F0\\uA6F1\\uA770\\uA806\\uA8C4\\uA8E0-\\uA8F1\\uA92B-"
            + "\\uA92D\\uA953\\uA9B3\\uA9C0\\uAAB0\\uAAB2-\\uAAB4\\uAAB7\\uAAB8"
            + "\\uAABE\\uAABF\\uAAC1\\uABED\\uAC00-\\uD7A3\\uF900-\\uFA0D"
            + "\\uFA10\\uFA12\\uFA15-\\uFA1E\\uFA20\\uFA22\\uFA25\\uFA26\\uFA2A"
            + "-\\uFA2D\\uFA30-\\uFA6D\\uFA70-\\uFAD9\\uFB00-\\uFB06\\uFB13-"
            + "\\uFB17\\uFB1D-\\uFB36\\uFB38-\\uFB3C\\uFB3E\\uFB40\\uFB41"
            + "\\uFB43\\uFB44\\uFB46-\\uFBB1\\uFBD3-\\uFD3D\\uFD50-\\uFD8F"
            + "\\uFD92-\\uFDC7\\uFDF0-\\uFDFC\\uFE10-\\uFE19\\uFE20-\\uFE26"
            + "\\uFE30-\\uFE44\\uFE47-\\uFE52\\uFE54-\\uFE66\\uFE68-\\uFE6B"
            + "\\uFE70-\\uFE72\\uFE74\\uFE76-\\uFEFC\\uFF01-\\uFFBE\\uFFC2-"
            + "\\uFFC7\\uFFCA-\\uFFCF\\uFFD2-\\uFFD7\\uFFDA-\\uFFDC\\uFFE0-"
            + "\\uFFE6\\uFFE8-\\uFFEE\\U000101FD\\U00010A0D\\U00010A0F\\U00010A"
            + "38-\\U00010A3A\\U00010A3F\\U0001109A\\U0001109C\\U000110AB"
            + "\\U000110B9\\U000110BA\\U0001D15E-\\U0001D169\\U0001D16D-\\U0001"
            + "D172\\U0001D17B-\\U0001D182\\U0001D185-\\U0001D18B\\U0001D1AA-"
            + "\\U0001D1AD\\U0001D1BB-\\U0001D1C0\\U0001D242-\\U0001D244\\U0001"
            + "D400-\\U0001D454\\U0001D456-\\U0001D49C\\U0001D49E\\U0001D49F"
            + "\\U0001D4A2\\U0001D4A5\\U0001D4A6\\U0001D4A9-\\U0001D4AC\\U0001D"
            + "4AE-\\U0001D4B9\\U0001D4BB\\U0001D4BD-\\U0001D4C3\\U0001D4C5-"
            + "\\U0001D505\\U0001D507-\\U0001D50A\\U0001D50D-\\U0001D514\\U0001"
            + "D516-\\U0001D51C\\U0001D51E-\\U0001D539\\U0001D53B-\\U0001D53E"
            + "\\U0001D540-\\U0001D544\\U0001D546\\U0001D54A-\\U0001D550\\U0001"
            + "D552-\\U0001D6A5\\U0001D6A8-\\U0001D7CB\\U0001D7CE-\\U0001D7FF"
            + "\\U0001F100-\\U0001F10A\\U0001F110-\\U0001F12E\\U0001F131\\U0001"
            + "F13D\\U0001F13F\\U0001F142\\U0001F146\\U0001F14A-\\U0001F14E"
            + "\\U0001F190\\U0001F200\\U0001F210-\\U0001F231\\U0001F240-\\U0001"
            + "F248\\U0002F800-\\U0002FA1D]", false);
   
        skipSets[KC].applyPattern(
            "[^<->A-PR-Za-pr-z\\u00A0\\u00A8\\u00AA\\u00AF\\u00B2-\\u00B5"
            + "\\u00B8-\\u00BA\\u00BC-\\u00BE\\u00C0-\\u00CF\\u00D1-\\u00D6"
            + "\\u00D8-\\u00DD\\u00E0-\\u00EF\\u00F1-\\u00F6\\u00F8-\\u00FD"
            + "\\u00FF-\\u0103\\u0106-\\u010F\\u0112-\\u0117\\u011A-\\u0121"
            + "\\u0124\\u0125\\u0128-\\u012D\\u0130\\u0132\\u0133\\u0139\\u013A"
            + "\\u013D-\\u0140\\u0143\\u0144\\u0147-\\u0149\\u014C-\\u0151"
            + "\\u0154\\u0155\\u0158-\\u015D\\u0160\\u0161\\u0164\\u0165\\u0168"
            + "-\\u0171\\u0174-\\u017F\\u01A0\\u01A1\\u01AF\\u01B0\\u01B7"
            + "\\u01C4-\\u01DC\\u01DE-\\u01E1\\u01E6-\\u01EB\\u01F1-\\u01F5"
            + "\\u01F8-\\u01FB\\u0200-\\u021B\\u021E\\u021F\\u0226-\\u0233"
            + "\\u0292\\u02B0-\\u02B8\\u02D8-\\u02DD\\u02E0-\\u02E4\\u0300-"
            + "\\u034E\\u0350-\\u036F\\u0374\\u037A\\u037E\\u0384\\u0385\\u0387"
            + "\\u0391\\u0395\\u0397\\u0399\\u039F\\u03A1\\u03A5\\u03A9\\u03AC"
            + "\\u03AE\\u03B1\\u03B5\\u03B7\\u03B9\\u03BF\\u03C1\\u03C5\\u03C9-"
            + "\\u03CB\\u03CE\\u03D0-\\u03D6\\u03F0-\\u03F2\\u03F4\\u03F5"
            + "\\u03F9\\u0406\\u0410\\u0413\\u0415-\\u0418\\u041A\\u041E\\u0423"
            + "\\u0427\\u042B\\u042D\\u0430\\u0433\\u0435-\\u0438\\u043A\\u043E"
            + "\\u0443\\u0447\\u044B\\u044D\\u0456\\u0474\\u0475\\u0483-\\u0487"
            + "\\u04D8\\u04D9\\u04E8\\u04E9\\u0587\\u0591-\\u05BD\\u05BF\\u05C1"
            + "\\u05C2\\u05C4\\u05C5\\u05C7\\u0610-\\u061A\\u0622\\u0623\\u0627"
            + "\\u0648\\u064A-\\u065E\\u0670\\u0675-\\u0678\\u06C1\\u06D2"
            + "\\u06D5-\\u06DC\\u06DF-\\u06E4\\u06E7\\u06E8\\u06EA-\\u06ED"
            + "\\u0711\\u0730-\\u074A\\u07EB-\\u07F3\\u0816-\\u0819\\u081B-"
            + "\\u0823\\u0825-\\u0827\\u0829-\\u082D\\u0928\\u0930\\u0933"
            + "\\u093C\\u094D\\u0951-\\u0954\\u0958-\\u095F\\u09BC\\u09BE"
            + "\\u09C7\\u09CD\\u09D7\\u09DC\\u09DD\\u09DF\\u0A33\\u0A36\\u0A3C"
            + "\\u0A4D\\u0A59-\\u0A5B\\u0A5E\\u0ABC\\u0ACD\\u0B3C\\u0B3E\\u0B47"
            + "\\u0B4D\\u0B56\\u0B57\\u0B5C\\u0B5D\\u0B92\\u0BBE\\u0BC6\\u0BC7"
            + "\\u0BCD\\u0BD7\\u0C46\\u0C4D\\u0C55\\u0C56\\u0CBC\\u0CBF\\u0CC2"
            + "\\u0CC6\\u0CCA\\u0CCD\\u0CD5\\u0CD6\\u0D3E\\u0D46\\u0D47\\u0D4D"
            + "\\u0D57\\u0DCA\\u0DCF\\u0DD9\\u0DDC\\u0DDF\\u0E33\\u0E38-\\u0E3A"
            + "\\u0E48-\\u0E4B\\u0EB3\\u0EB8\\u0EB9\\u0EC8-\\u0ECB\\u0EDC"
            + "\\u0EDD\\u0F0C\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39\\u0F43\\u0F4D"
            + "\\u0F52\\u0F57\\u0F5C\\u0F69\\u0F71-\\u0F7D\\u0F80-\\u0F84"
            + "\\u0F86\\u0F87\\u0F93\\u0F9D\\u0FA2\\u0FA7\\u0FAC\\u0FB9\\u0FC6"
            + "\\u1025\\u102E\\u1037\\u1039\\u103A\\u108D\\u10FC\\u1100-\\u1112"
            + "\\u1161-\\u1175\\u11A8-\\u11C2\\u135F\\u1714\\u1734\\u17D2"
            + "\\u17DD\\u18A9\\u1939-\\u193B\\u1A17\\u1A18\\u1A60\\u1A75-"
            + "\\u1A7C\\u1A7F\\u1B05\\u1B07\\u1B09\\u1B0B\\u1B0D\\u1B11\\u1B34"
            + "\\u1B35\\u1B3A\\u1B3C\\u1B3E\\u1B3F\\u1B42\\u1B44\\u1B6B-\\u1B73"
            + "\\u1BAA\\u1C37\\u1CD0-\\u1CD2\\u1CD4-\\u1CE0\\u1CE2-\\u1CE8"
            + "\\u1CED\\u1D2C-\\u1D2E\\u1D30-\\u1D3A\\u1D3C-\\u1D4D\\u1D4F-"
            + "\\u1D6A\\u1D78\\u1D9B-\\u1DE6\\u1DFD-\\u1E03\\u1E0A-\\u1E0F"
            + "\\u1E12-\\u1E1B\\u1E20-\\u1E27\\u1E2A-\\u1E41\\u1E44-\\u1E53"
            + "\\u1E58-\\u1E7D\\u1E80-\\u1E87\\u1E8E-\\u1E91\\u1E96-\\u1E9B"
            + "\\u1EA0-\\u1EF3\\u1EF6-\\u1EF9\\u1F00-\\u1F11\\u1F18\\u1F19"
            + "\\u1F20-\\u1F31\\u1F38\\u1F39\\u1F40\\u1F41\\u1F48\\u1F49\\u1F50"
            + "\\u1F51\\u1F59\\u1F60-\\u1F71\\u1F73-\\u1F75\\u1F77\\u1F79"
            + "\\u1F7B-\\u1F7D\\u1F80\\u1F81\\u1F88\\u1F89\\u1F90\\u1F91\\u1F98"
            + "\\u1F99\\u1FA0\\u1FA1\\u1FA8\\u1FA9\\u1FB3\\u1FB6\\u1FBB-\\u1FC1"
            + "\\u1FC3\\u1FC6\\u1FC9\\u1FCB-\\u1FCF\\u1FD3\\u1FDB\\u1FDD-"
            + "\\u1FDF\\u1FE3\\u1FEB\\u1FED-\\u1FEF\\u1FF3\\u1FF6\\u1FF9\\u1FFB"
            + "-\\u1FFE\\u2000-\\u200A\\u2011\\u2017\\u2024-\\u2026\\u202F"
            + "\\u2033\\u2034\\u2036\\u2037\\u203C\\u203E\\u2047-\\u2049\\u2057"
            + "\\u205F\\u2070\\u2071\\u2074-\\u208E\\u2090-\\u2094\\u20A8"
            + "\\u20D0-\\u20DC\\u20E1\\u20E5-\\u20F0\\u2100-\\u2103\\u2105-"
            + "\\u2107\\u2109-\\u2113\\u2115\\u2116\\u2119-\\u211D\\u2120-"
            + "\\u2122\\u2124\\u2126\\u2128\\u212A-\\u212D\\u212F-\\u2131"
            + "\\u2133-\\u2139\\u213B-\\u2140\\u2145-\\u2149\\u2150-\\u217F"
            + "\\u2189\\u2190\\u2192\\u2194\\u21D0\\u21D2\\u21D4\\u2203\\u2208"
            + "\\u220B\\u2223\\u2225\\u222C\\u222D\\u222F\\u2230\\u223C\\u2243"
            + "\\u2245\\u2248\\u224D\\u2261\\u2264\\u2265\\u2272\\u2273\\u2276"
            + "\\u2277\\u227A-\\u227D\\u2282\\u2283\\u2286\\u2287\\u2291\\u2292"
            + "\\u22A2\\u22A8\\u22A9\\u22AB\\u22B2-\\u22B5\\u2329\\u232A\\u2460"
            + "-\\u24EA\\u2A0C\\u2A74-\\u2A76\\u2ADC\\u2C7C\\u2C7D\\u2CEF-"
            + "\\u2CF1\\u2D6F\\u2DE0-\\u2DFF\\u2E9F\\u2EF3\\u2F00-\\u2FD5"
            + "\\u3000\\u302A-\\u302F\\u3036\\u3038-\\u303A\\u3046\\u304B"
            + "\\u304D\\u304F\\u3051\\u3053\\u3055\\u3057\\u3059\\u305B\\u305D"
            + "\\u305F\\u3061\\u3064\\u3066\\u3068\\u306F\\u3072\\u3075\\u3078"
            + "\\u307B\\u3099-\\u309D\\u309F\\u30A6\\u30AB\\u30AD\\u30AF\\u30B1"
            + "\\u30B3\\u30B5\\u30B7\\u30B9\\u30BB\\u30BD\\u30BF\\u30C1\\u30C4"
            + "\\u30C6\\u30C8\\u30CF\\u30D2\\u30D5\\u30D8\\u30DB\\u30EF-\\u30F2"
            + "\\u30FD\\u30FF\\u3131-\\u318E\\u3192-\\u319F\\u3200-\\u321E"
            + "\\u3220-\\u3247\\u3250-\\u327E\\u3280-\\u32FE\\u3300-\\u33FF"
            + "\\uA66F\\uA67C\\uA67D\\uA6F0\\uA6F1\\uA770\\uA806\\uA8C4\\uA8E0-"
            + "\\uA8F1\\uA92B-\\uA92D\\uA953\\uA9B3\\uA9C0\\uAAB0\\uAAB2-"
            + "\\uAAB4\\uAAB7\\uAAB8\\uAABE\\uAABF\\uAAC1\\uABED\\uAC00\\uAC1C"
            + "\\uAC38\\uAC54\\uAC70\\uAC8C\\uACA8\\uACC4\\uACE0\\uACFC\\uAD18"
            + "\\uAD34\\uAD50\\uAD6C\\uAD88\\uADA4\\uADC0\\uADDC\\uADF8\\uAE14"
            + "\\uAE30\\uAE4C\\uAE68\\uAE84\\uAEA0\\uAEBC\\uAED8\\uAEF4\\uAF10"
            + "\\uAF2C\\uAF48\\uAF64\\uAF80\\uAF9C\\uAFB8\\uAFD4\\uAFF0\\uB00C"
            + "\\uB028\\uB044\\uB060\\uB07C\\uB098\\uB0B4\\uB0D0\\uB0EC\\uB108"
            + "\\uB124\\uB140\\uB15C\\uB178\\uB194\\uB1B0\\uB1CC\\uB1E8\\uB204"
            + "\\uB220\\uB23C\\uB258\\uB274\\uB290\\uB2AC\\uB2C8\\uB2E4\\uB300"
            + "\\uB31C\\uB338\\uB354\\uB370\\uB38C\\uB3A8\\uB3C4\\uB3E0\\uB3FC"
            + "\\uB418\\uB434\\uB450\\uB46C\\uB488\\uB4A4\\uB4C0\\uB4DC\\uB4F8"
            + "\\uB514\\uB530\\uB54C\\uB568\\uB584\\uB5A0\\uB5BC\\uB5D8\\uB5F4"
            + "\\uB610\\uB62C\\uB648\\uB664\\uB680\\uB69C\\uB6B8\\uB6D4\\uB6F0"
            + "\\uB70C\\uB728\\uB744\\uB760\\uB77C\\uB798\\uB7B4\\uB7D0\\uB7EC"
            + "\\uB808\\uB824\\uB840\\uB85C\\uB878\\uB894\\uB8B0\\uB8CC\\uB8E8"
            + "\\uB904\\uB920\\uB93C\\uB958\\uB974\\uB990\\uB9AC\\uB9C8\\uB9E4"
            + "\\uBA00\\uBA1C\\uBA38\\uBA54\\uBA70\\uBA8C\\uBAA8\\uBAC4\\uBAE0"
            + "\\uBAFC\\uBB18\\uBB34\\uBB50\\uBB6C\\uBB88\\uBBA4\\uBBC0\\uBBDC"
            + "\\uBBF8\\uBC14\\uBC30\\uBC4C\\uBC68\\uBC84\\uBCA0\\uBCBC\\uBCD8"
            + "\\uBCF4\\uBD10\\uBD2C\\uBD48\\uBD64\\uBD80\\uBD9C\\uBDB8\\uBDD4"
            + "\\uBDF0\\uBE0C\\uBE28\\uBE44\\uBE60\\uBE7C\\uBE98\\uBEB4\\uBED0"
            + "\\uBEEC\\uBF08\\uBF24\\uBF40\\uBF5C\\uBF78\\uBF94\\uBFB0\\uBFCC"
            + "\\uBFE8\\uC004\\uC020\\uC03C\\uC058\\uC074\\uC090\\uC0AC\\uC0C8"
            + "\\uC0E4\\uC100\\uC11C\\uC138\\uC154\\uC170\\uC18C\\uC1A8\\uC1C4"
            + "\\uC1E0\\uC1FC\\uC218\\uC234\\uC250\\uC26C\\uC288\\uC2A4\\uC2C0"
            + "\\uC2DC\\uC2F8\\uC314\\uC330\\uC34C\\uC368\\uC384\\uC3A0\\uC3BC"
            + "\\uC3D8\\uC3F4\\uC410\\uC42C\\uC448\\uC464\\uC480\\uC49C\\uC4B8"
            + "\\uC4D4\\uC4F0\\uC50C\\uC528\\uC544\\uC560\\uC57C\\uC598\\uC5B4"
            + "\\uC5D0\\uC5EC\\uC608\\uC624\\uC640\\uC65C\\uC678\\uC694\\uC6B0"
            + "\\uC6CC\\uC6E8\\uC704\\uC720\\uC73C\\uC758\\uC774\\uC790\\uC7AC"
            + "\\uC7C8\\uC7E4\\uC800\\uC81C\\uC838\\uC854\\uC870\\uC88C\\uC8A8"
            + "\\uC8C4\\uC8E0\\uC8FC\\uC918\\uC934\\uC950\\uC96C\\uC988\\uC9A4"
            + "\\uC9C0\\uC9DC\\uC9F8\\uCA14\\uCA30\\uCA4C\\uCA68\\uCA84\\uCAA0"
            + "\\uCABC\\uCAD8\\uCAF4\\uCB10\\uCB2C\\uCB48\\uCB64\\uCB80\\uCB9C"
            + "\\uCBB8\\uCBD4\\uCBF0\\uCC0C\\uCC28\\uCC44\\uCC60\\uCC7C\\uCC98"
            + "\\uCCB4\\uCCD0\\uCCEC\\uCD08\\uCD24\\uCD40\\uCD5C\\uCD78\\uCD94"
            + "\\uCDB0\\uCDCC\\uCDE8\\uCE04\\uCE20\\uCE3C\\uCE58\\uCE74\\uCE90"
            + "\\uCEAC\\uCEC8\\uCEE4\\uCF00\\uCF1C\\uCF38\\uCF54\\uCF70\\uCF8C"
            + "\\uCFA8\\uCFC4\\uCFE0\\uCFFC\\uD018\\uD034\\uD050\\uD06C\\uD088"
            + "\\uD0A4\\uD0C0\\uD0DC\\uD0F8\\uD114\\uD130\\uD14C\\uD168\\uD184"
            + "\\uD1A0\\uD1BC\\uD1D8\\uD1F4\\uD210\\uD22C\\uD248\\uD264\\uD280"
            + "\\uD29C\\uD2B8\\uD2D4\\uD2F0\\uD30C\\uD328\\uD344\\uD360\\uD37C"
            + "\\uD398\\uD3B4\\uD3D0\\uD3EC\\uD408\\uD424\\uD440\\uD45C\\uD478"
            + "\\uD494\\uD4B0\\uD4CC\\uD4E8\\uD504\\uD520\\uD53C\\uD558\\uD574"
            + "\\uD590\\uD5AC\\uD5C8\\uD5E4\\uD600\\uD61C\\uD638\\uD654\\uD670"
            + "\\uD68C\\uD6A8\\uD6C4\\uD6E0\\uD6FC\\uD718\\uD734\\uD750\\uD76C"
            + "\\uD788\\uF900-\\uFA0D\\uFA10\\uFA12\\uFA15-\\uFA1E\\uFA20"
            + "\\uFA22\\uFA25\\uFA26\\uFA2A-\\uFA2D\\uFA30-\\uFA6D\\uFA70-"
            + "\\uFAD9\\uFB00-\\uFB06\\uFB13-\\uFB17\\uFB1D-\\uFB36\\uFB38-"
            + "\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46-\\uFBB1\\uFBD3"
            + "-\\uFD3D\\uFD50-\\uFD8F\\uFD92-\\uFDC7\\uFDF0-\\uFDFC\\uFE10-"
            + "\\uFE19\\uFE20-\\uFE26\\uFE30-\\uFE44\\uFE47-\\uFE52\\uFE54-"
            + "\\uFE66\\uFE68-\\uFE6B\\uFE70-\\uFE72\\uFE74\\uFE76-\\uFEFC"
            + "\\uFF01-\\uFFBE\\uFFC2-\\uFFC7\\uFFCA-\\uFFCF\\uFFD2-\\uFFD7"
            + "\\uFFDA-\\uFFDC\\uFFE0-\\uFFE6\\uFFE8-\\uFFEE\\U000101FD\\U00010"
            + "A0D\\U00010A0F\\U00010A38-\\U00010A3A\\U00010A3F\\U00011099"
            + "\\U0001109B\\U000110A5\\U000110B9\\U000110BA\\U0001D15E-\\U0001D"
            + "169\\U0001D16D-\\U0001D172\\U0001D17B-\\U0001D182\\U0001D185-"
            + "\\U0001D18B\\U0001D1AA-\\U0001D1AD\\U0001D1BB-\\U0001D1C0\\U0001"
            + "D242-\\U0001D244\\U0001D400-\\U0001D454\\U0001D456-\\U0001D49C"
            + "\\U0001D49E\\U0001D49F\\U0001D4A2\\U0001D4A5\\U0001D4A6\\U0001D4"
            + "A9-\\U0001D4AC\\U0001D4AE-\\U0001D4B9\\U0001D4BB\\U0001D4BD-"
            + "\\U0001D4C3\\U0001D4C5-\\U0001D505\\U0001D507-\\U0001D50A\\U0001"
            + "D50D-\\U0001D514\\U0001D516-\\U0001D51C\\U0001D51E-\\U0001D539"
            + "\\U0001D53B-\\U0001D53E\\U0001D540-\\U0001D544\\U0001D546\\U0001"
            + "D54A-\\U0001D550\\U0001D552-\\U0001D6A5\\U0001D6A8-\\U0001D7CB"
            + "\\U0001D7CE-\\U0001D7FF\\U0001F100-\\U0001F10A\\U0001F110-"
            + "\\U0001F12E\\U0001F131\\U0001F13D\\U0001F13F\\U0001F142\\U0001F1"
            + "46\\U0001F14A-\\U0001F14E\\U0001F190\\U0001F200\\U0001F210-"
            + "\\U0001F231\\U0001F240-\\U0001F248\\U0002F800-\\U0002FA1D]", false);
   
        return skipSets;
    }

    public void TestSkippable() {
       UnicodeSet starts;
       UnicodeSet[] skipSets = new UnicodeSet[]{
                                                    new UnicodeSet(), //NFD
                                                    new UnicodeSet(), //NFC
                                                    new UnicodeSet(), //NFKC
                                                    new UnicodeSet(), //NFKD
                                                    new UnicodeSet(), //FCD
                                                    new UnicodeSet(), //NONE
                                               };
       UnicodeSet[] expectSets = new UnicodeSet[]{
                                                    new UnicodeSet(),
                                                    new UnicodeSet(),
                                                    new UnicodeSet(),
                                                    new UnicodeSet(),
                                                    new UnicodeSet(),
                                                    new UnicodeSet(),
                                               };
       StringBuffer s, pattern;
       int start, limit, rangeEnd;
       int i, range, count;
       starts = new UnicodeSet();
       /*
       //[\u0350-\u0357\u035D-\u035F\u0610-\u0615\u0656-\u0658\u0CBC\u17DD\u1939-\u193B]
       for(int ch=0;ch<=0x10FFFF;ch++){
               if(Normalizer.isNFSkippable(ch, Normalizer.NFD)) {
                   skipSets[D].add(ch);
               }
               if(Normalizer.isNFSkippable(ch, Normalizer.NFKD)) {
                   skipSets[KD].add(ch);
               }
               if(Normalizer.isNFSkippable(ch, Normalizer.NFC)) {
                   skipSets[C].add(ch);
               }
               if(Normalizer.isNFSkippable(ch, Normalizer.NFKC)) {
                   skipSets[KC].add(ch);
               }
               if(Normalizer.isNFSkippable(ch, Normalizer.FCD)) {
                   skipSets[FCD].add(ch);
               }
               if(Normalizer.isNFSkippable(ch, Normalizer.NONE)) {
                   skipSets[NONE].add(ch);
               }
       }
       */
       // build NF*Skippable sets from runtime data 
       NormalizerImpl.addPropertyStarts(starts);
       count=starts.getRangeCount();
   
       start=limit=0;
       rangeEnd=0;
       range=0;
       for(;;) {
           if(start<limit) {
               // get properties for start and apply them to [start..limit[ 
               if(Normalizer.isNFSkippable(start, Normalizer.NFD)) {
                   skipSets[D].add(start, limit-1);
               }
               if(Normalizer.isNFSkippable(start, Normalizer.NFKD)) {
                   skipSets[KD].add(start, limit-1);
               }
               if(Normalizer.isNFSkippable(start, Normalizer.NFC)) {
                   skipSets[C].add(start, limit-1);
               }
               if(Normalizer.isNFSkippable(start, Normalizer.NFKC)) {
                   skipSets[KC].add(start, limit-1);
               }
               if(Normalizer.isNFSkippable(start, Normalizer.FCD)) {
                   skipSets[FCD].add(start, limit-1);
               }
               if(Normalizer.isNFSkippable(start, Normalizer.NONE)) {
                   skipSets[NONE].add(start, limit-1);
               }
               
           }
   
           // go to next range of same properties 
           start=limit;
           if(++limit>rangeEnd) {
               if(range<count) {
                   limit=starts.getRangeStart(range);
                   rangeEnd=starts.getRangeEnd(range);
                   ++range;
               } else if(range==count) {
                   // additional range to complete the Unicode code space 
                   limit=rangeEnd=0x110000;
                   ++range;
               } else {
                   break;
               }
           }
       }
   
       expectSets = initSkippables(expectSets);
       if(expectSets[D].contains(0x0350)){
            errln("expectSets[D] contains 0x0350");
       }
       //expectSets.length for now do not test FCD and NONE since there is no data
       for(i=0; i< 4; ++i) {

           if(!skipSets[i].equals(expectSets[i])) {
               errln("error: TestSkippable skipSets["+i+"]!=expectedSets["+i+"]\n"+
                     "May need to update hardcoded UnicodeSet patterns in com.ibm.icu.dev.test.normalizer.BasicTest.java\n"+
                     "See ICU4J - unicodetools.com.ibm.text.UCD.NFSkippable\n" +
                     "Run com.ibm.text.UCD.Main with the option NFSkippable.");
   
               s=new StringBuffer();
               
               s.append("\n\nskip=       ");
               s.append(skipSets[i].toPattern(true));
               s.append("\n\n");
               
               s.append("skip-expect=");             
               pattern = new StringBuffer(((UnicodeSet)skipSets[i].clone()).removeAll(expectSets[i]).toPattern(true));
               s.append(pattern);
   
               pattern.delete(0,pattern.length());
               s.append("\n\nexpect-skip=");
               pattern = new StringBuffer(((UnicodeSet)expectSets[i].clone()).removeAll(skipSets[i]).toPattern(true));
               s.append(pattern);
               s.append("\n\n");
               
               pattern.delete(0,pattern.length());
               s.append("\n\nintersection(expect,skip)=");
               UnicodeSet intersection  = ((UnicodeSet) expectSets[i].clone()).retainAll(skipSets[i]);
               pattern = new StringBuffer(intersection.toPattern(true));
               s.append(pattern);
               s.append("\n\n");
               

               
               errln(s.toString());
           }
       }
     }
     
     public void TestBugJ2068(){
        String sample = "The quick brown fox jumped over the lazy dog";
        UCharacterIterator text = UCharacterIterator.getInstance(sample);
        Normalizer norm = new Normalizer(text,Normalizer.NFC,0);
        text.setIndex(4);
        if(text.current() == norm.current()){
            errln("Normalizer is not cloning the UCharacterIterator");
        }
     }   
     public void TestGetCombiningClass(){
        for(int i=0;i<0x10FFFF;i++){
            int cc = UCharacter.getCombiningClass(i);
            if(0xD800<= i && i<=0xDFFF && cc >0 ){
                cc = UCharacter.getCombiningClass(i);
                errln("CC: "+ cc + " for codepoint: " +Utility.hex(i,8));
            } 
        }
    }  
    
    public void TestGetNX(){
        UnicodeSet set = NormalizerImpl.getNX(1 /*NormalizerImpl.NX_HANGUL*/);
        if(!set.contains(0xac01)){
            errln("getNX did not return correct set for NX_HANGUL");
        }
        
        set = NormalizerImpl.getNX(2/*NormalizerImpl.NX_CJK_COMPAT*/);
        if(!set.contains('\uFA20')){
            errln("getNX did not return correct set for NX_CJK_COMPAT");
        }
    }
    public void TestSerializedSet(){
        USerializedSet sset=new USerializedSet();
        UnicodeSet set = new UnicodeSet();
        int start, end;
    
        // collect all sets into one for contiguous output
        int[] startEnd = new int[2];

        if(NormalizerImpl.getCanonStartSet(0x0130, sset)) {
            int count=sset.countRanges();
            for(int j=0; j<count; ++j) {
                sset.getRange(j, startEnd);
                set.add(startEnd[0], startEnd[1]);
            }
        }
       

        // test all of these precomposed characters
        UnicodeSetIterator it = new UnicodeSetIterator(set);
        while(it.nextRange() && it.codepoint!=UnicodeSetIterator.IS_STRING) {
            start=it.codepoint;
            end=it.codepointEnd;
            while(start<=end) {
                if(!sset.contains(start)){
                    errln("USerializedSet.contains failed for "+Utility.hex(start,8));
                }
            }
        }
    }
    
    public void TestReturnFailure(){
        char[] term = {'r','\u00e9','s','u','m','\u00e9' };
        char[] decomposed_term = new char[10 + term.length + 2];
        int rc = Normalizer.decompose(term,0,term.length, decomposed_term,0,decomposed_term.length,true, 0);
        int rc1 = Normalizer.decompose(term,0,term.length, decomposed_term,10,decomposed_term.length,true, 0); 
        if(rc!=rc1){
            errln("Normalizer decompose did not return correct length");
        }
    }

    private final static class TestCompositionCase {
        public Normalizer.Mode mode;
        public int options;
        public String input, expect;
        TestCompositionCase(Normalizer.Mode mode, int options, String input, String expect) {
            this.mode=mode;
            this.options=options;
            this.input=input;
            this.expect=expect;
        }
    }

    public void TestComposition() {
        final TestCompositionCase cases[]=new TestCompositionCase[]{
            /*
             * special cases for UAX #15 bug
             * see Unicode Corrigendum #5: Normalization Idempotency
             * at http://unicode.org/versions/corrigendum5.html
             * (was Public Review Issue #29)
             */
            new TestCompositionCase(Normalizer.NFC, 0, "\u1100\u0300\u1161\u0327",      "\u1100\u0300\u1161\u0327"),
            new TestCompositionCase(Normalizer.NFC, 0, "\u1100\u0300\u1161\u0327\u11a8","\u1100\u0300\u1161\u0327\u11a8"),
            new TestCompositionCase(Normalizer.NFC, 0, "\uac00\u0300\u0327\u11a8",      "\uac00\u0327\u0300\u11a8"),
            new TestCompositionCase(Normalizer.NFC, 0, "\u0b47\u0300\u0b3e",            "\u0b47\u0300\u0b3e"),

            /* TODO: add test cases for UNORM_FCC here (j2151) */
        };

        String output;
        int i;

        for(i=0; i<cases.length; ++i) {
            output=Normalizer.normalize(cases[i].input, cases[i].mode, cases[i].options);
            if(!output.equals(cases[i].expect)) {
                errln("unexpected result for case "+i);
            }
        }
    }
}
