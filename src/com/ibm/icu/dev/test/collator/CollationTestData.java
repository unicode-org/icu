/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/collator/Attic/CollationTestData.java,v $
 * $Date: 2002/09/04 01:37:26 $
 * $Revision: 1.5 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.collator;

import com.ibm.icu.impl.ICUListResourceBundle;

public class CollationTestData extends ICUListResourceBundle {

    public CollationTestData() {
          super.contents = data;
    }
    static final Object[][] data = new Object[][] { 
                {
                    "Info",
                    new Object[][]{
                        {
                            "Description",
                            "This is locale based collation test for Danish",
                        },
                        {
                            "Headers",
                            new String[] { 
                                "sequence",

                            },
                        },
                        {
                            "LongDescription",
                            "The following entries are separate tests " +
                            "containing test data for various locales." +
                            "Each entry has the following fields: Test" +
                            "Locale - the locale that we should instan" +
                            "tiate collator with.ArgumentStrength - st" +
                            "rength of collatorTestData - set of test " +
                            "cases, which are sequences of strings tha" +
                            "t will be parsedSequences must not change" +
                            " the sign of relation, i.e. we can only h" +
                            "ave < and = or> and = in single sequence." +
                            " Cannot mix < and > in the same sequence." +
                            " Whitespace isis ignored unless quoted.",
                        },
                    },
                },
                {
                    "TestData",
                    new Object[][]{
                        {
                            "TestCIgnorableContraction",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "njiva=n\uD834\uDD65ji" +
                                                "va=n\uD834\uDD79jiva=" +
                                                "n\u0000\u0000\u0000ji" +
                                                "va=n\u0000jiva=n\uD800" +
                                                "jiva=n\uFFFEjiva",
                                                "ljubav=l\u0000jubav=l" +
                                                "\uD834\uDD79jubav=l\u0000" +
                                                "\u0000\u0000jubav=l\uD800" +
                                                "jubav=l\uFFFEjubav=",
                                                "Ljubav=L\u0000jubav=L" +
                                                "\uD834\uDD79jubav=L\u0000" +
                                                "\u0000\u0000jubav=L\uD800" +
                                                "jubav=L\uFFFEjubav",
                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Checks whether completely" +
                                            " ignorable code points ar" +
                                            "e skipped in contractions" +
                                            ".",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "TestLocale",
                                                "sh",
                                            },
                                        },
                                        new Object[][]{
                                            {
                                                "Rules",
                                                "& L < lj, Lj <<< LJ& " +
                                                "N < nj, Nj <<< NJ ",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            "TestCIgnorablePrefix",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "\u30A1\u30FC= \u30A1\uDB40" +
                                                "\uDC30\u30FC= \u30A1\uD800" +
                                                "\u30FC= \u30A1\uFFFE\u30FC" +
                                                "= \u30A1\uD834\uDD79\u30FC" 
                                                /*
                                                 * +
                                                "= \u30A1\u0000\u0000\u0000" +
                                                "\u30FC= \u30A1\u0000\u30FC" +
                                                "= \u30A1\u30FC= \u30A1" +
                                                "\u0000\u059A\u30FC= \u30A1" +
                                                "\u30FC",
                                                */
                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Checks whether completely" +
                                            " ignorable code points ar" +
                                            "e skipped in prefix proce" +
                                            "ssing.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "TestLocale",
                                                "ja",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            "TestNShiftedIgnorable",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "a' 'b<A' 'b<a' '\u0301" +
                                                "b<A' '\u0301b<a' '\u0300" +
                                                "b<A' '\u0300b<a_b<A_b" +
                                                "<a_\u0301b<A_\u0301b<" +
                                                "a_\u0300b<A_\u0300b<a" +
                                                "\u0301b<A\u0301b<a\u0300" +
                                                "b<A\u0300b<",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "New UCA states that prima" +
                                            "ry ignorables should be c" +
                                            "ompletely ignorable when " +
                                            "following a shifted code " +
                                            "point.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Arguments",
                                                "[alternate non-ignora" +
                                                "ble][strength 3]",
                                            },
                                            {
                                                "TestLocale",
                                                "root",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            "TestSafeSurrogates",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "a<x\uD800\uDC00b",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "It turned out that surrog" +
                                            "ates were not skipped pro" +
                                            "perly when iterating back" +
                                            "wards if they were in the" +
                                            " middle of a contraction." +
                                            " This test assures that t" +
                                            "his is fixed.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Rules",
                                                "&a < x\uD800\uDC00b",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        // this test does not pass in Java yet
                        {
                            "TestShiftedIgnorable",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "a' 'b="
                                                + "a' '\u0300b="
                                                + "a' '\u0301b<"
                                                + "a_b="
                                                + "a_\u0300b="
                                                + "a_\u0301b<"
                                                + "A' 'b="
                                                + "A' '\u0300b="
                                                + "A' '\u0301b<"
                                                + "A_b="
                                                + "A_\u0300b="
                                                + "A_\u0301b<"
                                                + "a\u0301b<"
                                                + "A\u0301b<"
                                                + "a\u0300b<"
                                                + "A\u0300b",
                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "New UCA states that prima" +
                                            "ry ignorables should be c" +
                                            "ompletely ignorable when " +
                                            "following a shifted code " +
                                            "point.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Arguments",
                                                "[alternate shifted][s" +
                                                "trength 4]",
                                            },
                                            {
                                                "TestLocale",
                                                "root",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            // "da_TestPrimary",
                            "TestPrimary",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "Lvi=Lwi",
                                                "L\u00E4vi<L\u00F6wi",
                                                "L\u00FCbeck=Lybeck",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "This test goes through pr" +
                                            "imary strength cases",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Arguments",
                                                "[strength 1]",
                                            },
                                            {
                                                "TestLocale",
                                                "da",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            // "da_TestTertiary",
                            "TestTertiary",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "Luc<luck",
                                                "luck<L\u00FCbeck",
                                                "L\u00FCbeck>lybeck",
                                                "L\u00E4vi<L\u00F6we",
                                                "L\u00F6ww<mast",
                                                "A/S<ANDRE<ANDR\u00C9<" +
                                                "ANDREAS<AS<CA<\u00C7A" +
                                                "<CB<\u00C7C<D.S.B.<DA" +
                                                "<DB<DSB<DSC<\u00D0A<\u00D0" +
                                                "C<EKSTRA_ARBEJDE<EKST" +
                                                "RABUD0<H\u00D8ST<HAAG" +
                                                "<H\u00C5NDBOG<HAANDV\u00C6" +
                                                "RKSBANKEN<karl<Karl<'" +
                                                "NIELS J\u00D8RGEN'<NI" +
                                                "ELS-J\u00D8RGEN<NIELS" +
                                                "EN<'R\u00C9E, A'<'REE" +
                                                ", B'<'R\u00C9E, L'<'R" +
                                                "EE, V'<'SCHYTT, B'<'S" +
                                                "CHYTT, H'<'SCH\u00DCT" +
                                                "T, H'<'SCHYTT, L'<'SC" +
                                                "H\u00DCTT, M'<SS<\u00DF" +
                                                "<SSA<'STORE VILDMOSE'" +
                                                "<STOREK\u00C6R0<'STOR" +
                                                "M PETERSEN'<STORMLY<T" +
                                                "HORVALD<THORVARDUR<TH" +
                                                "YGESEN<\u00FEORVAR\u00D0" +
                                                "UR<'VESTERG\u00C5RD, " +
                                                "A'<'VESTERGAARD, A'<'" +
                                                "VESTERG\u00C5RD, B'<\u00C6" +
                                                "BLE<\u00C4BLE<\u00D8B" +
                                                "ERG<\u00D6BERG",
                                                "andere<chaque<chemin<" +
                                                "cote<cot\u00E9<c\u00F4" +
                                                "te<c\u00F4t\u00E9<\u010D" +
                                                "u\u010D\u0113t<Czech<" +
                                                "hi\u0161a<irdisch<lie" +
                                                "<lire<llama<l\u00F5ug" +
                                                "<l\u00F2za<lu\u010D<l" +
                                                "uck<L\u00FCbeck<lye<l" +
                                                "\u00E4vi<L\u00F6wen<m" +
                                                "\u00E0\u0161ta<m\u00EE" +
                                                "r<myndig<M\u00E4nner<" +
                                                "m\u00F6chten<pi\u00F1" +
                                                "a<pint<pylon<\u0161\u00E0" +
                                                "ran<savoir<\u0160erb\u016B" +
                                                "ra<Sietla<\u015Blub<s" +
                                                "ubtle<symbol<s\u00E4m" +
                                                "tlich<waffle<verkehrt" +
                                                "<wood<vox<v\u00E4ga<y" +
                                                "en<yuan<yucca<\u017Ea" +
                                                "l<\u017Eena<\u017Den\u0113" +
                                                "va<zoo0<Zviedrija<Z\u00FC" +
                                                "rich<zysk0<\u00E4nder" +
                                                "e",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "This test goes through te" +
                                            "rtiary strength cases",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Arguments",
                                                "[strength 3]",
                                            },
                                            {
                                                "TestLocale",
                                                "da",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                    },
                },
    };
}
