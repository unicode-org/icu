/*
 *******************************************************************************
 * Copyright (C) 2008-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.collator;
import com.ibm.icu.text.IndexCharacters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.IndexCharacters;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public class IndexCharactersTest extends TestFmwk {
    public static void main(String[] args) throws Exception{
        new IndexCharactersTest().run(args);
    }

    public void TestIndexCharactersList() {
        String[][] localeAndIndexCharactersList = new String[][] {
            {"af", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"ak", "A:B:C:D:E:\u0190:F:G:H:I:J:K:L:M:N:O:\u0186:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"ar", "\u0627:\u0628:\u062A:\u062B:\u062C:\u062D:\u062E:\u062F:\u0630:\u0631:\u0632:\u0633:\u0634:\u0635:\u0636:\u0637:\u0638:\u0639:\u063A:\u0641:\u0642:\u0643:\u0644:\u0645:\u0646:\u0647:\u0648:\u064A"},
            {"asa", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"az", "A:B:C:\u00C7:D:E:\u018F:F:G:\u011E:H:X:I:\u0130:J:K:Q:L:M:N:O:\u00D6:P:R:S:\u015E:T:U:\u00DC:V:W:Y:Z"},
            {"be", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0415:\u0416:\u0417:\u0406:\u0419:\u041A:\u041B:\u041C:\u041D:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u0428:\u042B:\u042D:\u042E:\u042F"},
            {"bem", "A:B:C:E:F:G:I:J:K:L:M:N:O:P:S:T:U:W:Y"},
            {"bez", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:Y:Z"},
            {"bg", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0415:\u0416:\u0417:\u0418:\u0419:\u041A:\u041B:\u041C:\u041D:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u0428:\u0429:\u042E:\u042F"},
            {"bm", "A:B:C:D:E:\u0190:F:G:H:I:J:K:L:M:N:\u019D:\u014A:O:\u0186:P:R:S:T:U:W:Y:Z"},
            {"bo", "\u0F40:\u0F41:\u0F42:\u0F44:\u0F45:\u0F46:\u0F47:\u0F49:\u0F4F:\u0F50:\u0F51:\u0F53:\u0F54:\u0F55:\u0F56:\u0F58:\u0F59:\u0F5A:\u0F5B:\u0F5D:\u0F5E:\u0F5F:\u0F60:\u0F61:\u0F62:\u0F63:\u0F64:\u0F66:\u0F67:\u0F68"},
            {"ca", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"cgg", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"chr", "\u13A0:\u13A6:\u13AD:\u13B3:\u13B9:\u13BE:\u13C6:\u13CC:\u13D3:\u13DC:\u13E3:\u13E9:\u13EF"},
            {"cs", "A:B:C:\u010C:D:E:F:G:H:CH:I:J:K:L:M:N:O:P:Q:R:\u0158:S:\u0160:T:U:V:W:X:Y:Z:\u017D"},
            {"cy", "A:B:C:CH:D:E:F:FF:G:H:I:J:L:LL:M:N:O:P:PH:R:RH:S:T:TH:U:W:Y"},
            {"da", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C6:\u00D8:\u00C5"},
            {"dav", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"de", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"ebu", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"ee", "A:B:C:D:\u0189:E:\u0190:F:\u0191:G:\u0194:H:I:J:K:L:M:N:\u014A:O:\u0186:P:Q:R:S:T:U:V:\u01B2:W:X:Y:Z"},
            {"el", "\u0391:\u0392:\u0393:\u0394:\u0395:\u0396:\u0397:\u0398:\u0399:\u039A:\u039B:\u039C:\u039D:\u039E:\u039F:\u03A0:\u03A1:\u03A3:\u03A4:\u03A5:\u03A6:\u03A7:\u03A8:\u03A9"},
            {"en", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"eo", "A:B:C:\u0108:D:E:F:G:\u011C:H:\u0124:I:J:\u0134:K:L:M:N:O:P:R:S:\u015C:T:U:\u016C:V:Z"},
            {"es", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:\u00D1:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"et", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:\u0160:Z:\u017D:T:U:V:\u00D5:\u00C4:\u00D6:\u00DC:X:Y"},
            {"eu", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"ff", "A:B:\u0181:C:D:\u018A:E:F:G:H:I:J:K:L:M:N:\u014A:O:P:R:S:T:U:W:Y:\u01B3"},
            {"fi", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C5:\u00C4:\u00D6"},
            {"fil", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"fo", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C6:\u00D8"},
            {"fr", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"guz", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"ha", "A:B:\u0181:C:D:\u018A:E:F:G:H:I:J:K:\u0198:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"he", "\u05D0:\u05D1:\u05D2:\u05D3:\u05D4:\u05D5:\u05D6:\u05D7:\u05D8:\u05D9:\u05DB:\u05DC:\u05DE:\u05E0:\u05E1:\u05E2:\u05E4:\u05E6:\u05E7:\u05E8:\u05E9:\u05EA"},
            {"ig", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"is", "A:\u00C1:B:C:D:\u00D0:E:\u00C9:F:G:H:I:\u00CD:J:K:L:M:N:O:\u00D3:P:Q:R:S:T:U:\u00DA:V:W:X:Y:\u00DD:Z:\u00DE:\u00C6:\u00D6"},
            {"it", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"ja", "\u3042:\u304B:\u3055:\u305F:\u306A:\u306F:\u307E:\u3084:\u3089:\u308F"},
            {"jmc", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"kab", "A:B:C:D:E:\u0190:F:G:\u0194:H:I:J:K:L:M:N:P:Q:R:S:T:U:W:X:Y:Z"},
            {"kam", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"kde", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"kea", "A:B:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:X:Z"},
            {"khq", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:\u019D:\u014A:O:P:Q:R:S:T:U:W:X:Y:Z"},
            {"ki", "A:B:C:D:E:G:H:I:J:K:M:N:O:R:T:U:W:Y"},
            {"kln", "A:B:C:D:E:G:H:I:J:K:L:M:N:O:P:R:S:T:U:W:Y"},
            {"ko", "\u1100:\u1102:\u1103:\u1105:\u1106:\u1107:\u1109:\u110B:\u110C:\u110E:\u110F:\u1110:\u1111:\u1112"},
            {"lag", "A:B:C:D:E:F:G:H:I:\u0197:J:K:L:M:N:O:P:Q:R:S:T:U:\u0244:V:W:X:Y:Z"},
            {"lg", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"lt", "A:B:C:\u010C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:\u0160:T:U:V:Z:\u017D"},
            {"luo", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y"},
            {"luy", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            // This should be the correct data.  Commented till it is fixed in CLDR collation data.
            // {"lv", "A:B:C:\u010C:D:E:F:G:\u0122:H:I:Y:J:K:\u0136:L:\u013B:M:N:\u0145:O:P:Q:R:S:\u0160:T:U:V:W:X:Z:\u017D"},
            {"lv", "A:B:C:\u010C:D:E:F:G:\u0122:H:I:J:K:\u0136:L:\u013B:M:N:\u0145:O:P:Q:R:S:\u0160:T:U:V:W:X:Y:Z:\u017D"},
            {"mas", "A:B:C:D:E:\u0190:G:H:I:\u0197:J:K:L:M:N:\u014A:O:\u0186:P:R:S:T:U:\u0244:W:Y"},
            {"mer", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"mfe", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:X:Y:Z"},
            {"mg", "A:B:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:V:Y:Z"},
            // This should be the correct data.  Commented till it is fixed in CLDR collation data.
            // {"mk", "\u0410:\u0411:\u0412:\u0413:\u0403:\u0414:\u0415:\u0416:\u0417:\u0405:\u0418:\u0408:\u041A:\u040C:\u041B:\u0409:\u041C:\u041D:\u040A:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u040F:\u0428"},
            {"mk", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0403:\u0415:\u0416:\u0417:\u0405:\u0418:\u0408:\u041A:\u041B:\u0409:\u041C:\u041D:\u040A:\u041E:\u041F:\u0420:\u0421:\u0422:\u040C:\u0423:\u0424:\u0425:\u0426:\u0427:\u040F:\u0428"},
            // This should be the correct data.  Commented till it is fixed in CLDR collation data.
            // {"mt", "A:B:C:\u010A:D:E:F:\u0120:G:G\u0126:H:\u0126:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:\u017B:Z"},
            {"mt", "A:B:\u010A:C:D:E:F:\u0120:G:G\u0126:H:\u0126:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:\u017B:Z"},
            {"naq", "A:B:C:D:E:F:G:H:I:K:M:N:O:P:Q:R:S:T:U:W:X:Y:Z"},
            {"nb", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C6:\u00D8:\u00C5"},
            {"nd", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:S:T:U:V:W:X:Y:Z"},
            {"nl", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"nn", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C6:\u00D8:\u00C5"},
            {"nyn", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"om", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"pl", "A:\u0104:B:C:\u0106:D:E:\u0118:F:G:H:I:J:K:L:\u0141:M:N:\u0143:O:\u00D3:P:Q:R:S:\u015A:T:U:V:W:X:Y:Z:\u0179:\u017B"},
            {"pt", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"rm", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"ro", "A:\u0102:\u00C2:B:C:D:E:F:G:H:I:\u00CE:J:K:L:M:N:O:P:Q:R:S:\u0218:T:\u021A:U:V:W:X:Y:Z"},
            {"rof", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"ru", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0415:\u0416:\u0417:\u0418:\u0419:\u041A:\u041B:\u041C:\u041D:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u0428:\u0429:\u042B:\u042D:\u042E:\u042F"},
            {"rw", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"rwk", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"saq", "A:B:C:D:E:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y"},
            {"seh", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"ses", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:\u019D:\u014A:O:P:Q:R:S:T:U:W:X:Y:Z"},
            {"sg", "A:B:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"shi", "A:B:C:D:E:\u0190:F:G:\u0194:H:I:J:K:L:M:N:Q:R:S:T:U:W:X:Y:Z"},
            {"shi_Tfng", "\u2D30:\u2D31:\u2D33:\u2D37:\u2D39:\u2D3B:\u2D3C:\u2D3D:\u2D40:\u2D43:\u2D44:\u2D45:\u2D47:\u2D49:\u2D4A:\u2D4D:\u2D4E:\u2D4F:\u2D53:\u2D54:\u2D55:\u2D56:\u2D59:\u2D5A:\u2D5B:\u2D5C:\u2D5F:\u2D61:\u2D62:\u2D63:\u2D65"},
            {"sk", "A:\u00C4:B:C:\u010C:D:E:F:G:H:CH:I:J:K:L:M:N:O:\u00D4:P:Q:R:S:\u0160:T:U:V:W:X:Y:Z:\u017D"},
            {"sl", "A:B:C:\u010C:\u0106:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:\u0160:T:U:V:W:X:Y:Z:\u017D"},
            {"sn", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"sr", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0402:\u0415:\u0416:\u0417:\u0418:\u0408:\u041A:\u041B:\u0409:\u041C:\u041D:\u040A:\u041E:\u041F:\u0420:\u0421:\u0422:\u040B:\u0423:\u0424:\u0425:\u0426:\u0427:\u040F:\u0428"},
            {"sv", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C5:\u00C4:\u00D6"},
            {"teo", "A:B:C:D:E:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:X:Y"},
            {"to", "A:B:C:D:E:F:G:H:\u02BB:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"tr", "A:B:C:\u00C7:D:E:F:G:\u011E:H:I:\u0130:J:K:L:M:N:O:\u00D6:P:Q:R:S:\u015E:T:U:\u00DC:V:W:X:Y:Z"},
            {"tzm", "A:B:C:D:E:\u0190:F:G:\u0194:H:I:J:K:L:M:N:Q:R:S:T:U:W:X:Y:Z"},
            {"uk", "\u0410:\u0411:\u0412:\u0413:\u0490:\u0414:\u0415:\u0404:\u0416:\u0417:\u0418:\u0406:\u0407:\u0419:\u041A:\u041B:\u041C:\u041D:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u0428:\u0429:\u042E:\u042F"},
            {"uz_Latn", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u02BF"},
            {"vi", "A:\u0102:\u00C2:B:C:D:\u0110:E:\u00CA:F:G:H:I:J:K:L:M:N:O:\u00D4:\u01A0:P:Q:R:S:T:U:\u01AF:V:W:X:Y:Z"},
            {"vun", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            {"xog", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"yo", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"zh", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            {"zh_Hant", "\u4E00:\u4E01:\u4E09:\u4E11:\u4E19:\u4E1E:\u4E32:\u4E26:\u4E9F:\u4E58:\u4E7E:\u50A2:\u4E82:\u50E7:\u5104:\u5112:\u512A:\u53E2:\u56A5:\u52F8:\u5137:\u513C:\u56CC:\u56D1:\u5EF3"},
        };
        for (String[] localeAndIndexCharacters : localeAndIndexCharactersList) {
            ULocale locale = new ULocale(localeAndIndexCharacters[0]);
            String expectedIndexCharacters = localeAndIndexCharacters[1];
            Collection<String> indexCharacters = new IndexCharacters(locale).getIndexCharacters();

            // Join the elements of the list to a string with delimiter ":"
            StringBuilder sb = new StringBuilder();
            Iterator iter = indexCharacters.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next());
                if (!iter.hasNext()) {
                    break;
                }
                sb.append(":");
            }
            String actualIndexCharacters = sb.toString();
            if (!expectedIndexCharacters.equals(actualIndexCharacters)) {
                errln("Test failed for locale " + localeAndIndexCharacters[0] +
                      "\n  Expected = |" + expectedIndexCharacters + "|\n  actual   = |" + actualIndexCharacters + "|");
             }
        }
    }

    public void TestBasics() {
        ULocale[] list = ULocale.getAvailableLocales();
        // get keywords combinations
        // don't bother with multiple combinations at this poin
        List keywords = new ArrayList();
        keywords.add("");

        String[] collationValues = Collator.getKeywordValues("collation");
        for (int j = 0; j < collationValues.length; ++j) {
            keywords.add("@collation=" + collationValues[j]);
        }

        for (int i = 0; i < list.length; ++i) {
            for (Iterator it = keywords.iterator(); it.hasNext();) {
                String collationValue = (String) it.next();
                ULocale locale = new ULocale(list[i].toString() + collationValue);
                if (collationValue.length() > 0 && !Collator.getFunctionalEquivalent("collation", locale).equals(locale)) {
                    //logln("Skipping " + locale);
                    continue;
                }

                if (locale.getCountry().length() != 0) {
                    continue;
                }
                IndexCharacters indexCharacters = new IndexCharacters(locale);
                final Collection mainChars = indexCharacters.getIndexCharacters();
                String mainCharString = mainChars.toString();
                if (mainCharString.length() > 500) {
                    mainCharString = mainCharString.substring(0,500) + "...";
                }
                logln(mainChars.size() + "\t" + locale + "\t" + locale.getDisplayName(ULocale.ENGLISH));
                logln("Index:\t" + mainCharString);
                if (mainChars.size() > 100) {
                    errln("Index character set too large");
                }
                showIfNotEmpty("A sequence sorting the same is already present", indexCharacters.getAlreadyIn());
                showIfNotEmpty("A sequence sorts the same as components", indexCharacters.getNoDistinctSorting());
                showIfNotEmpty("A sequence has only Marks or Nonalphabetics", indexCharacters.getNotAlphabetic());
            }
        }
    }
    private void showIfNotEmpty(String title, List alreadyIn) {
        if (alreadyIn.size() != 0) {
            logln("\t" + title + ":\t" + alreadyIn);
        }
    }
    private void showIfNotEmpty(String title, Map alreadyIn) {
        if (alreadyIn.size() != 0) {
            logln("\t" + title + ":\t" + alreadyIn);
        }
    }
    
    /* Test the method public ULocale getLocale() */
    public void TestGetLocale(){
        IndexCharacters ic = new IndexCharacters(new ULocale("en_US"));
        if(!ic.getLocale().equals(new ULocale("en_US"))){
            errln("IndexCharacter.getLocale() was suppose to return the same " +
                    "ULocale that was passed for the object.");
        }
        if(ic.getLocale().equals(new ULocale("jp_JP"))){
            errln("IndexCharacter.getLocale() was not suppose to return the same " +
                    "ULocale that was passed for the object.");
        }
    }
}
