/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
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
                            "These are the data driven tests",
                        },
                        {
                            "Headers",
                            new String[] { 
                                "sequence",

                            },
                        },
                        {
                            "LongDescription",
                            "The following entries are separate tests containing" +
                            " test data for various locales.Each entry has the f" +
                            "ollowing fields: Info/Description - short descriopt" +
                            "ion of the testSettings - settings for the test.Set" +
                            "tings/TestLocale - locale for the collator ORSettin" +
                            "gs/Rules - rules for the collator (can't have both)" +
                            "Settings/Arguments - arguments to be passed to the " +
                            "collator before testing. Use rule syntax.Cases - se" +
                            "t of test cases, which are sequences of strings tha" +
                            "t will be parsedSequences must not change the sign " +
                            "of relation, i.e. we can only have < and = or> and " +
                            "= in single sequence. Cannot mix < and > in the sam" +
                            "e sequence. Whitespace isis ignored unless quoted.",
                        },
                    },
                },
                {
                    "TestData",
                    new Object[][]{
                        {
                            "TestAlbanian",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "cz<\u00E7<d<dz<dh<e<ez<\u00EB<f" +
                                                "<gz<gj<h<lz<ll<m<nz<nj<o<rz<rr<" +
                                                "s<sz<sh<t<tz<th<u<xz<xh<y<zz<zh",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Albanian sort order.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "TestLocale",
                                                "sq",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            "TestCIgnorableContraction",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "njiva=n\uD834\uDD65jiva=n\uD834" +
                                                "\uDD79jiva=n\u0000\u0000\u0000j" +
                                                "iva=n\u0000jiva=n\uD800jiva=n\uFFFE" +
                                                "jiva",
                                                "ljubav=l\u0000jubav=l\uD834\uDD79" +
                                                "jubav=l\u0000\u0000\u0000jubav=" +
                                                "l\uD800jubav=l\uFFFEjubav",
                                                "Ljubav=L\u0000jubav=L\uD834\uDD79" +
                                                "jubav=L\u0000\u0000\u0000jubav=" +
                                                "L\uD800jubav=L\uFFFEjubav",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Checks whether completely ignorable" +
                                            " code points are skipped in contrac" +
                                            "tions.",
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
                                                "& L < lj, Lj <<< LJ& N < nj, Nj" +
                                                " <<< NJ ",
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
                                                "\u30A1\u30FC= \u30A1\uDB40\uDC30" +
                                                "\u30FC= \u30A1\uD800\u30FC= \u30A1" +
                                                "\uFFFE\u30FC= \u30A1\uD834\uDD79" +
                                                "\u30FC= \u30A1\u0000\u0000\u0000" +
                                                "\u30FC= \u30A1\u0000\u30FC= \u30A1" +
                                                "\u30FC= \u30A1\u0000\u059A\u30FC" +
                                                "= \u30A1\u30FC",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Checks whether completely ignorable" +
                                            " code points are skipped in prefix " +
                                            "processing.",
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
                            "TestEstonian",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "sy<\u0161<\u0161y<z<zy<\u017E<v" +
                                                "<w<va<\u00F5<\u00F5y<\u00E4<\u00E4" +
                                                "y<\u00F6<\u00F6y<\u00FC<\u00FCy" +
                                                "<x",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Estonian sort order.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "TestLocale",
                                                "et",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            "TestJavaStyleRule",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "a = equal < z < x < w < b < y"

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "java.text allows rules to start as " +
                                            "'<<<x<<<y...' we emulate this by as" +
                                            "suming a &[first tertiary ignorable" +
                                            "] in this case.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Rules",
                                                "=equal<<<z<<x<<<w<y &[first ter" +
                                                "tiary ignorable]=a &[first prim" +
                                                "ary ignorable]=b",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            "TestLatvian",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "cz<\u010D<d<gz<\u0123<h<iz<y<j<" +
                                                "kz<\u0137<l<lz<\u013C<m<nz<\u0146" +
                                                "<o<rz<\u0157<s<sz<\u0161<t<zz<\u017E",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Latvian sort order.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "TestLocale",
                                                "lv",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            "TestLithuanian",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "cz<\u010D<d<iz<y<j<sz<\u0161<t<" +
                                                "zz<\u017E",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Lithuanian sort order.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "TestLocale",
                                                "lt",
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
                                                "a' 'b<A' 'b<a' '\u0301b<A' '\u0301" +
                                                "b<a' '\u0300b<A' '\u0300b<a_b<A" +
                                                "_b<a_\u0301b<A_\u0301b<a_\u0300" +
                                                "b<A_\u0300b<a\u0301b<A\u0301b<a" +
                                                "\u0300b<A\u0300b<",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "New UCA states that primary ignorab" +
                                            "les should be completely ignorable " +
                                            "when following a shifted code point" +
                                            ".",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Arguments",
                                                "[alternate non-ignorable][stren" +
                                                "gth 3]",
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
                                            "It turned out that surrogates were " +
                                            "not skipped properly when iterating" +
                                            " backwards if they were in the midd" +
                                            "le of a contraction. This test assu" +
                                            "res that this is fixed.",
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
                        {
                            "TestShiftedIgnorable",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "a' 'b=a' '\u0300b=a' '\u0301b<a" +
                                                "_b=a_\u0300b=a_\u0301b<A' 'b=A'" +
                                                " '\u0300b=A' '\u0301b<A_b=A_\u0300" +
                                                "b=A_\u0301b<a\u0301b<A\u0301b<a" +
                                                "\u0300b<A\u0300b",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "New UCA states that primary ignorab" +
                                            "les should be completely ignorable " +
                                            "when following a shifted code point" +
                                            ".",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Arguments",
                                                "[alternate shifted][strength 4]",
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
                            "TestSimplifiedChineseOrder",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "\u5F20<\u5F20\u4E00\u8E3F",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "Sorted file has different order.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "Arguments",
                                                "[normalization on]",
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
                            "TestThaiPartialSortKeyProblems",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "\u0E01\u0E01\u0E38\u0E18\u0E20\u0E31" +
                                                "\u0E13\u0E11\u0E4C<\u0E01\u0E01" +
                                                "\u0E38\u0E2A\u0E31\u0E19\u0E42\u0E18",
                                                "\u0E01\u0E07\u0E01\u0E32\u0E23<" +
                                                "\u0E01\u0E07\u0E42\u0E01\u0E49",
                                                "\u0E01\u0E23\u0E19\u0E17\u0E32<" +
                                                "\u0E01\u0E23\u0E19\u0E19\u0E40\u0E0A" +
                                                "\u0E49\u0E32",
                                                "\u0E01\u0E23\u0E30\u0E40\u0E08\u0E35" +
                                                "\u0E22\u0E27<\u0E01\u0E23\u0E30" +
                                                "\u0E40\u0E08\u0E35\u0E4A\u0E22\u0E27",
                                                "\u0E01\u0E23\u0E23\u0E40\u0E0A\u0E2D" +
                                                "<\u0E01\u0E23\u0E23\u0E40\u0E0A" +
                                                "\u0E49\u0E32",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "These are examples of strings that " +
                                            "caused trouble in partial sort key " +
                                            "testing.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "TestLocale",
                                                "th_TH",
                                            },
                                        },

                                    },
                                },
                            },
                        },
                        {
                            "TestTibetanNormalizedIterativeCrash",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "\u0F71\u0F72\u0F80\u0F71\u0F72<" +
                                                "\u0F80",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "This pretty much crashes.",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
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
                            "TestDanishPrimary",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "Lvi<Lwi",
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
                                            "This test goes through primary stre" +
                                            "ngth cases",
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
                            "TestDanishTertiary",
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
                                                "A/S<ANDRE<ANDR\u00C9<ANDREAS<AS" +
                                                "<CA<\u00C7A<CB<\u00C7C<D.S.B.<D" +
                                                "A<\u00D0A<DB<\u00D0C<DSB<DSC<EK" +
                                                "STRA_ARBEJDE<EKSTRABUD0<H\u00D8" +
                                                "ST<HAAG<H\u00C5NDBOG<HAANDV\u00C6" +
                                                "RKSBANKEN<Karl<karl<'NIELS J\u00D8" +
                                                "RGEN'<NIELS-J\u00D8RGEN<NIELSEN" +
                                                "<'R\u00C9E, A'<'REE, B'<'R\u00C9" +
                                                "E, L'<'REE, V'<'SCHYTT, B'<'SCH" +
                                                "YTT, H'<'SCH\u00DCTT, H'<'SCHYT" +
                                                "T, L'<'SCH\u00DCTT, M'<SS<\u00DF" +
                                                "<SSA<'STORE VILDMOSE'<STOREK\u00C6" +
                                                "R0<'STORM PETERSEN'<STORMLY<THO" +
                                                "RVALD<THORVARDUR<\u00FEORVAR\u00D0" +
                                                "UR<THYGESEN<'VESTERG\u00C5RD, A" +
                                                "'<'VESTERGAARD, A'<'VESTERG\u00C5" +
                                                "RD, B'<\u00C6BLE<\u00C4BLE<\u00D8" +
                                                "BERG<\u00D6BERG",
                                                "andere<chaque<chemin<cote<cot\u00E9" +
                                                "<c\u00F4te<c\u00F4t\u00E9<\u010D" +
                                                "u\u010D\u0113t<Czech<hi\u0161a<" +
                                                "irdisch<lie<lire<llama<l\u00F5u" +
                                                "g<l\u00F2za<lu\u010D<luck<L\u00FC" +
                                                "beck<lye<l\u00E4vi<L\u00F6wen<m" +
                                                "\u00E0\u0161ta<m\u00EEr<myndig<" +
                                                "M\u00E4nner<m\u00F6chten<pi\u00F1" +
                                                "a<pint<pylon<\u0161\u00E0ran<sa" +
                                                "voir<\u0160erb\u016Bra<Sietla<\u015B" +
                                                "lub<subtle<symbol<s\u00E4mtlich" +
                                                "<verkehrt<vox<v\u00E4ga<waffle<" +
                                                "wood<yen<yuan<yucca<\u017Eal<\u017E" +
                                                "ena<\u017Den\u0113va<zoo0<Zvied" +
                                                "rija<Z\u00FCrich<zysk0<\u00E4nd" +
                                                "ere",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "This test goes through tertiary str" +
                                            "ength cases",
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
                        {
                            "TestNewHindiRules",
                            new Object[][]{
                                {
                                    "Cases",
                                    new Object[]{
                                        new Object[]{
                                            new String[] { 
                                                "\u0950<\u0964<\u0965<\u0970<\u0966" +
                                                "<\u0967<\u0968<\u0969<\u096A<\u096B" +
                                                "<\u096C<\u096D<\u096E<\u096F<\u0905" +
                                                "<\u0906<\u0907<\u0908<\u0909<\u090A" +
                                                "<\u090B<\u0960<\u090C<\u0961<\u090D" +
                                                "<\u090E<\u090F<\u0910<\u0911<\u0912" +
                                                "<\u0913<\u0914<\u0915<\u0915\u093C" +
                                                "=\u0958<\u0915\u0901<\u0915\u0902" +
                                                "<\u0915\u0903<\u0915\u0951<\u0915" +
                                                "\u0952<\u0915\u0953<\u0915\u0954" +
                                                "<\u0915\u093D<\u0915\u094D<\u0915" +
                                                "\u093E<\u0915\u093F<\u0915\u0940" +
                                                "<\u0915\u0941<\u0915\u0942<\u0915" +
                                                "\u0943<\u0915\u0944<\u0915\u0962" +
                                                "<\u0915\u0963<\u0915\u0945<\u0915" +
                                                "\u0946<\u0915\u0947<\u0915\u0948" +
                                                "<\u0915\u0949<\u0915\u094A<\u0915" +
                                                "\u094B<\u0915\u094C<\u0916<\u0916" +
                                                "\u093C =\u0959<\u0916\u0901<\u0916" +
                                                "\u0902<\u0916\u0903<\u0916\u0951" +
                                                "<\u0916\u0952<\u0916\u0953<\u0916" +
                                                "\u0954<\u0916\u093D<\u0916\u094D" +
                                                "<\u0916\u093E<\u0916\u093F<\u0916" +
                                                "\u0940<\u0916\u0941<\u0916\u0942" +
                                                "<\u0916\u0943<\u0916\u0944<\u0916" +
                                                "\u0962<\u0916\u0963<\u0916\u0945" +
                                                "<\u0916\u0946<\u0916\u0947<\u0916" +
                                                "\u0948<\u0916\u0949<\u0916\u094A" +
                                                "<\u0916\u094B<\u0916\u094C<\u0917" +
                                                "<\u0917\u093C=\u095A<\u0917\u0901" +
                                                "<\u0917\u0902<\u0917\u0903<\u0917" +
                                                "\u0951<\u0917\u0952<\u0917\u0953" +
                                                "<\u0917\u0954<\u0917\u093D<\u0917" +
                                                "\u094D<\u0917\u093E<\u0917\u093F" +
                                                "<\u0917\u0940<\u0917\u0941<\u0917" +
                                                "\u0942<\u0917\u0943<\u0917\u0944" +
                                                "<\u0917\u0962<\u0917\u0963<\u0917" +
                                                "\u0945<\u0917\u0946<\u0917\u0947" +
                                                "<\u0917\u0948<\u0917\u0949<\u0917" +
                                                "\u094A<\u0917\u094B<\u0917\u094C" +
                                                "<\u0918<\u0919<\u091A<\u091B<\u091C" +
                                                "<\u091C\u093C =\u095B<\u091C\u0901" +
                                                "<\u091C\u0902<\u091C\u0903<\u091C" +
                                                "\u0951<\u091C\u0952<\u091C\u0953" +
                                                "<\u091C\u0954<\u091C\u093D<\u091C" +
                                                "\u094D<\u091C\u093E<\u091C\u093F" +
                                                "<\u091C\u0940<\u091C\u0941<\u091C" +
                                                "\u0942<\u091C\u0943<\u091C\u0944" +
                                                "<\u091C\u0962<\u091C\u0963<\u091C" +
                                                "\u0945<\u091C\u0946<\u091C\u0947" +
                                                "<\u091C\u0948<\u091C\u0949<\u091C" +
                                                "\u094A<\u091C\u094B<\u091C\u094C" +
                                                "<\u091D<\u091E<\u091F<\u0920<\u0921" +
                                                "<\u0921\u093C=\u095C<\u0921\u0901" +
                                                "<\u0921\u0902<\u0921\u0903<\u0921" +
                                                "\u0951<\u0921\u0952<\u0921\u0953" +
                                                "<\u0921\u0954<\u0921\u093D<\u0921" +
                                                "\u094D<\u0921\u093E<\u0921\u093F" +
                                                "<\u0921\u0940<\u0921\u0941<\u0921" +
                                                "\u0942<\u0921\u0943<\u0921\u0944" +
                                                "<\u0921\u0962<\u0921\u0963<\u0921" +
                                                "\u0945<\u0921\u0946<\u0921\u0947" +
                                                "<\u0921\u0948<\u0921\u0949<\u0921" +
                                                "\u094A<\u0921\u094B<\u0921\u094C" +
                                                "<\u0922<\u0922\u093C=\u095D<\u0922" +
                                                "\u0901<\u0922\u0902<\u0922\u0903" +
                                                "<\u0922\u0951<\u0922\u0952<\u0922" +
                                                "\u0953<\u0922\u0954<\u0922\u093D" +
                                                "<\u0922\u094D<\u0922\u093E<\u0922" +
                                                "\u093F<\u0922\u0940<\u0922\u0941" +
                                                "<\u0922\u0942<\u0922\u0943<\u0922" +
                                                "\u0944<\u0922\u0962<\u0922\u0963" +
                                                "<\u0922\u0945<\u0922\u0946<\u0922" +
                                                "\u0947<\u0922\u0948<\u0922\u0949" +
                                                "<\u0922\u094A<\u0922\u094B<\u0922" +
                                                "\u094C<\u0923<\u0924<\u0925<\u0926" +
                                                "<\u0927<\u0928<\u0928\u093C =\u0929" +
                                                "< \u0928\u0901<\u0928\u0902< \u0928" +
                                                "\u0903<\u0928\u0951<\u0928\u0952" +
                                                "<\u0928\u0953<\u0928\u0954<\u0928" +
                                                "\u093D<\u0928\u094D<\u0928\u093E" +
                                                "<\u0928\u093F<\u0928\u0940<\u0928" +
                                                "\u0941<\u0928\u0942<\u0928\u0943" +
                                                "<\u0928\u0944<\u0928\u0962<\u0928" +
                                                "\u0963<\u0928\u0945<\u0928\u0946" +
                                                "<\u0928\u0947<\u0928\u0948<\u0928" +
                                                "\u0949<\u0928\u094A<\u0928\u094B" +
                                                "<\u0928\u094C<\u092A<\u092B<\u092B" +
                                                "\u093C=\u095E<\u092B\u0901<\u092B" +
                                                "\u0902<\u092B\u0903<\u092B\u0951" +
                                                "<\u092B\u0952<\u092B\u0953<\u092B" +
                                                "\u0954<\u092B\u093D<\u092B\u094D" +
                                                "<\u092B\u093E<\u092B\u093F<\u092B" +
                                                "\u0940<\u092B\u0941<\u092B\u0942" +
                                                "<\u092B\u0943<\u092B\u0944<\u092B" +
                                                "\u0962<\u092B\u0963<\u092B\u0945" +
                                                "<\u092B\u0946<\u092B\u0947<\u092B" +
                                                "\u0948<\u092B\u0949<\u092B\u094A" +
                                                "<\u092B\u094B<\u092B\u094C<\u092C" +
                                                "<\u092D<\u092E<\u092F<\u092F\u093C" +
                                                "=\u095F <\u092F\u0901<\u092F\u0902" +
                                                "<\u092F\u0903<\u092F\u0951<\u092F" +
                                                "\u0952<\u092F\u0953<\u092F\u0954" +
                                                "<\u092F\u093D<\u092F\u094D<\u092F" +
                                                "\u093E<\u092F\u093F<\u092F\u0940" +
                                                "<\u092F\u0941<\u092F\u0942<\u092F" +
                                                "\u0943<\u092F\u0944<\u092F\u0962" +
                                                "<\u092F\u0963<\u092F\u0945<\u092F" +
                                                "\u0946<\u092F\u0947<\u092F\u0948" +
                                                "<\u092F\u0949<\u092F\u094A<\u092F" +
                                                "\u094B<\u092F\u094C<\u0930<\u0930" +
                                                "\u093C=\u0931<\u0930\u0901<\u0930" +
                                                "\u0902<\u0930\u0903<\u0930\u0951" +
                                                "<\u0930\u0952<\u0930\u0953<\u0930" +
                                                "\u0954<\u0930\u093D<\u0930\u094D" +
                                                "<\u0930\u093E<\u0930\u093F<\u0930" +
                                                "\u0940<\u0930\u0941<\u0930\u0942" +
                                                "<\u0930\u0943<\u0930\u0944<\u0930" +
                                                "\u0962<\u0930\u0963<\u0930\u0945" +
                                                "<\u0930\u0946<\u0930\u0947<\u0930" +
                                                "\u0948<\u0930\u0949<\u0930\u094A" +
                                                "<\u0930\u094B<\u0930\u094C<\u0932" +
                                                "<\u0933<\u0933\u093C=\u0934<\u0933" +
                                                "\u0901<\u0933\u0902<\u0933\u0903" +
                                                "<\u0933\u0951<\u0933\u0952<\u0933" +
                                                "\u0953<\u0933\u0954<\u0933\u093D" +
                                                "<\u0933\u094D<\u0933\u093E<\u0933" +
                                                "\u093F<\u0933\u0940<\u0933\u0941" +
                                                "<\u0933\u0942<\u0933\u0943<\u0933" +
                                                "\u0944<\u0933\u0962<\u0933\u0963" +
                                                "<\u0933\u0945<\u0933\u0946<\u0933" +
                                                "\u0947<\u0933\u0948<\u0933\u0949" +
                                                "<\u0933\u094A<\u0933\u094B<\u0933" +
                                                "\u094C<\u0935<\u0936<\u0937<\u0938" +
                                                "<\u0939<\u093C<\u0901<\u0902<\u0903" +
                                                "<\u0951<\u0952<\u0953<\u0954<\u093D" +
                                                "<\u094D<\u093E<\u093F<\u0940<\u0941" +
                                                "<\u0942<\u0943<\u0944<\u0962<\u0963" +
                                                "<\u0945<\u0946<\u0947<\u0948<\u0949" +
                                                "<\u094A<\u094B<\u094C",

                                            },

                                        },

                                    },
                                },
                                {
                                    "Info",
                                    new Object[][]{
                                        {
                                            "Description",
                                            "This test goes through new rules an" +
                                            "d tests against old rules",
                                        },
                                    },
                                },
                                {
                                    "Settings",
                                    new Object[]{
                                        new Object[][]{
                                            {
                                                "TestLocale",
                                                "hi",
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
