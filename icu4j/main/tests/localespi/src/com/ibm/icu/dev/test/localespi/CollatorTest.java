/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

public class CollatorTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new CollatorTest().run(args);
    }

    /*
     * Check if getInstance returns the ICU implementation.
     */
    public void TestGetInstance() {
        for (Locale loc : Collator.getAvailableLocales()) {
            if (TestUtil.isProblematicIBMLocale(loc)) {
                logln("Skipped " + loc);
                continue;
            }

            Collator coll = Collator.getInstance(loc);

            boolean isIcuImpl = (coll instanceof com.ibm.icu.impl.jdkadapter.CollatorICU);

            if (TestUtil.isICUExtendedLocale(loc)) {
                if (!isIcuImpl) {
                    errln("FAIL: getInstance returned JDK Collator for locale " + loc);
                }
            } else {
                if (isIcuImpl) {
                    logln("INFO: getInstance returned ICU Collator for locale " + loc);
                }
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                Collator collIcu = Collator.getInstance(iculoc);
                if (isIcuImpl) {
                    if (!coll.equals(collIcu)) {
                        errln("FAIL: getInstance returned ICU Collator for locale " + loc
                                + ", but different from the one for locale " + iculoc);
                    }
                } else {
                    if (!(collIcu instanceof com.ibm.icu.impl.jdkadapter.CollatorICU)) {
                        errln("FAIL: getInstance returned JDK Collator for locale " + iculoc);
                    }
                }
            }
        }
    }

    /*
     * Testing the behavior of text collation between ICU instance and its
     * equivalent created via the Locale SPI framework.
     */
    public void TestICUEquivalent() {
        Locale[] TEST_LOCALES = {
                new Locale("en", "US"),
                new Locale("de", "DE"),
                new Locale("ja", "JP"),
        };

        String[] TEST_DATA = {
                "Cafe",
                "cafe",
                "CAFE",
                "caf\u00e9",
                "cafe\u0301",
                "\u304b\u3075\u3047",
                "\u304c\u3075\u3047",
                "\u304b\u3075\u3048",
                "\u30ab\u30d5\u30a7",
                "\uff76\uff8c\uff6a",
        };

        for (Locale loc : TEST_LOCALES) {
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);
            Collator jdkColl = Collator.getInstance(iculoc);
            com.ibm.icu.text.Collator icuColl = com.ibm.icu.text.Collator.getInstance(loc);

            // Default strength = TERITIARY
            checkCollation(jdkColl, icuColl, TEST_DATA, "TERITIARY", loc);

            // PRIMARY
            jdkColl.setStrength(Collator.PRIMARY);
            icuColl.setStrength(com.ibm.icu.text.Collator.PRIMARY);
            checkCollation(jdkColl, icuColl, TEST_DATA, "PRIMARY", loc);

            // SECONDARY
            jdkColl.setStrength(Collator.SECONDARY);
            icuColl.setStrength(com.ibm.icu.text.Collator.SECONDARY);
            checkCollation(jdkColl, icuColl, TEST_DATA, "SECONDARY", loc);
        }
    }

    private void checkCollation(Collator jdkColl, com.ibm.icu.text.Collator icuColl,
            String[] data, String strength, Locale loc) {
        for (String text1 : data) {
            for (String text2 : data) {
                int jdkRes = jdkColl.compare(text1, text2);
                int icuRes = icuColl.compare(text1, text2);

                if (jdkRes != icuRes) {
                    errln("FAIL: Different results for [text1=" + text1 + ",text2=" + text2 + ") for locale "
                            + loc + " with strength " + strength + " - Result (jdk=" + jdkRes + ",icu=" + icuRes + ")");
                }

                // Evaluate collationKey
                CollationKey jdkKey1 = jdkColl.getCollationKey(text1);
                CollationKey jdkKey2 = jdkColl.getCollationKey(text2);

                com.ibm.icu.text.CollationKey icuKey1 = icuColl.getCollationKey(text1);
                com.ibm.icu.text.CollationKey icuKey2 = icuColl.getCollationKey(text2);

                int jdkKeyRes = jdkKey1.compareTo(jdkKey2);
                int icuKeyRes = icuKey1.compareTo(icuKey2);

                if (jdkKeyRes != icuKeyRes) {
                    errln("FAIL: Different collationKey comparison results for [text1=" + text1 + ",text2=" + text2
                            + ") for locale " + loc + " with strength " + strength
                            + " - Result (jdk=" + jdkRes + ",icu=" + icuRes + ")");
                }
            }
        }
    }
}
