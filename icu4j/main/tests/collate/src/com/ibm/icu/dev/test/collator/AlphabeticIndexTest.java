/*
 *******************************************************************************
 * Copyright (C) 2008-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.AlphabeticIndex;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public class AlphabeticIndexTest extends TestFmwk {
    public static Set<String> KEY_LOCALES = new LinkedHashSet(Arrays.asList(
            "en", "es", "de", "fr", "ja", "it", "tr", "pt", "zh", "nl", 
            "pl", "ar", "ru", "zh_Hant", "ko", "th", "sv", "fi", "da", 
            "he", "nb", "el", "hr", "bg", "sk", "lt", "vi", "lv", "sr", 
            "pt_PT", "ro", "hu", "cs", "id", "sl", "fil", "fa", "uk", 
            "ca", "hi", "et", "eu", "is", "sw", "ms", "bn", "am", "ta", 
            "te", "mr", "ur", "ml", "kn", "gu", "or"));
    private String[][] localeAndIndexCharactersLists = new String[][] {
            /* Arabic*/ {"ar", "\u0627:\u0628:\u062A:\u062B:\u062C:\u062D:\u062E:\u062F:\u0630:\u0631:\u0632:\u0633:\u0634:\u0635:\u0636:\u0637:\u0638:\u0639:\u063A:\u0641:\u0642:\u0643:\u0644:\u0645:\u0646:\u0647:\u0648:\u064A"},
            /* Bulgarian*/  {"bg", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0415:\u0416:\u0417:\u0418:\u0419:\u041A:\u041B:\u041C:\u041D:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u0428:\u0429:\u042E:\u042F"},
            /* Catalan*/    {"ca", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Czech*/  {"cs", "A:B:C:\u010C:D:E:F:G:H:CH:I:J:K:L:M:N:O:P:Q:R:\u0158:S:\u0160:T:U:V:W:X:Y:Z:\u017D"},
            /* Danish*/ {"da", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C6:\u00D8:\u00C5"},
            /* German*/ {"de", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Greek*/  {"el", "\u0391:\u0392:\u0393:\u0394:\u0395:\u0396:\u0397:\u0398:\u0399:\u039A:\u039B:\u039C:\u039D:\u039E:\u039F:\u03A0:\u03A1:\u03A3:\u03A4:\u03A5:\u03A6:\u03A7:\u03A8:\u03A9"},
            /* English*/    {"en", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Spanish*/    {"es", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:\u00D1:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Estonian*/   {"et", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:\u0160:Z:\u017D:T:U:V:\u00D5:\u00C4:\u00D6:\u00DC:X:Y"},
            /* Basque*/ {"eu", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Finnish*/    {"fi", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C5:\u00C4:\u00D6"},
            /* Filipino*/   {"fil", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* French*/ {"fr", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Hebrew*/ {"he", "\u05D0:\u05D1:\u05D2:\u05D3:\u05D4:\u05D5:\u05D6:\u05D7:\u05D8:\u05D9:\u05DB:\u05DC:\u05DE:\u05E0:\u05E1:\u05E2:\u05E4:\u05E6:\u05E7:\u05E8:\u05E9:\u05EA"},
            /* Icelandic*/  {"is", "A:\u00C1:B:C:D:\u00D0:E:\u00C9:F:G:H:I:\u00CD:J:K:L:M:N:O:\u00D3:P:Q:R:S:T:U:\u00DA:V:W:X:Y:\u00DD:Z:\u00DE:\u00C6:\u00D6"},
            /* Italian*/    {"it", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Japanese*/   {"ja", "\u3042:\u304B:\u3055:\u305F:\u306A:\u306F:\u307E:\u3084:\u3089:\u308F"},
            /* Korean*/ {"ko", "\u1100:\u1102:\u1103:\u1105:\u1106:\u1107:\u1109:\u110B:\u110C:\u110E:\u110F:\u1110:\u1111:\u1112"},
            /* Lithuanian*/ {"lt", "A:B:C:\u010C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:\u0160:T:U:V:Z:\u017D"},
            // This should be the correct data.  Commented till it is fixed in CLDR collation data.
            // {"lv", "A:B:C:\u010C:D:E:F:G:\u0122:H:I:Y:J:K:\u0136:L:\u013B:M:N:\u0145:O:P:Q:R:S:\u0160:T:U:V:W:X:Z:\u017D"},
            /* Latvian*/    {"lv", "A:B:C:\u010C:D:E:F:G:\u0122:H:I:J:K:\u0136:L:\u013B:M:N:\u0145:O:P:Q:R:S:\u0160:T:U:V:W:X:Y:Z:\u017D"},
            /* Norwegian Bokm\u00E5l*/  {"nb", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C6:\u00D8:\u00C5"},
            /* Dutch*/  {"nl", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Polish*/ {"pl", "A:\u0104:B:C:\u0106:D:E:\u0118:F:G:H:I:J:K:L:\u0141:M:N:\u0143:O:\u00D3:P:Q:R:S:\u015A:T:U:V:W:X:Y:Z:\u0179:\u017B"},
            /* Portuguese*/ {"pt", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Romanian*/   {"ro", "A:\u0102:\u00C2:B:C:D:E:F:G:H:I:\u00CE:J:K:L:M:N:O:P:Q:R:S:\u0218:T:\u021A:U:V:W:X:Y:Z"},
            /* Russian*/    {"ru", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0415:\u0416:\u0417:\u0418:\u0419:\u041A:\u041B:\u041C:\u041D:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u0428:\u0429:\u042B:\u042D:\u042E:\u042F"},
            /* Slovak*/ {"sk", "A:\u00C4:B:C:\u010C:D:E:F:G:H:CH:I:J:K:L:M:N:O:\u00D4:P:Q:R:S:\u0160:T:U:V:W:X:Y:Z:\u017D"},
            /* Slovenian*/  {"sl", "A:B:C:\u010C:\u0106:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:\u0160:T:U:V:W:X:Y:Z:\u017D"},
            /* Serbian*/    {"sr", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0402:\u0415:\u0416:\u0417:\u0418:\u0408:\u041A:\u041B:\u0409:\u041C:\u041D:\u040A:\u041E:\u041F:\u0420:\u0421:\u0422:\u040B:\u0423:\u0424:\u0425:\u0426:\u0427:\u040F:\u0428"},
            /* Swedish*/    {"sv", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C5:\u00C4:\u00D6"},
            /* Turkish*/    {"tr", "A:B:C:\u00C7:D:E:F:G:\u011E:H:I:\u0130:J:K:L:M:N:O:\u00D6:P:Q:R:S:\u015E:T:U:\u00DC:V:W:X:Y:Z"},
            /* Ukrainian*/  {"uk", "\u0410:\u0411:\u0412:\u0413:\u0490:\u0414:\u0415:\u0404:\u0416:\u0417:\u0418:\u0406:\u0407:\u0419:\u041A:\u041B:\u041C:\u041D:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u0428:\u0429:\u042E:\u042F"},
            /* Vietnamese*/ {"vi", "A:\u0102:\u00C2:B:C:D:\u0110:E:\u00CA:F:G:H:I:J:K:L:M:N:O:\u00D4:\u01A0:P:Q:R:S:T:U:\u01AF:V:W:X:Y:Z"},
            /* Chinese*/    {"zh", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            /* Chinese (Traditional Han)*/  {"zh_Hant", "\u4E00:\u4E01:\u4E09:\u4E11:\u4E19:\u4E1E:\u4E32:\u4E26:\u4E9F:\u4E58:\u4E7E:\u50A2:\u4E82:\u50E7:\u5104:\u5112:\u512A:\u53E2:\u56A5:\u52F8:\u5137:\u513C:\u56CC:\u56D1:\u5EF3"},

            // Comment these out to make the test run faster. Later, make these run under extended

            //            /* Afrikaans*/  {"af", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Akan*/   {"ak", "A:B:C:D:E:\u0190:F:G:H:I:J:K:L:M:N:O:\u0186:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Asu*/    {"asa", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Azerbaijani*/    {"az", "A:B:C:\u00C7:D:E:\u018F:F:G:\u011E:H:X:I:\u0130:J:K:Q:L:M:N:O:\u00D6:P:R:S:\u015E:T:U:\u00DC:V:W:Y:Z"},
            //            /* Belarusian*/ {"be", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0415:\u0416:\u0417:\u0406:\u0419:\u041A:\u041B:\u041C:\u041D:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u0428:\u042B:\u042D:\u042E:\u042F"},
            //            /* Bemba*/  {"bem", "A:B:C:E:F:G:I:J:K:L:M:N:O:P:S:T:U:W:Y"},
            //            /* Bena*/   {"bez", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:Y:Z"},
            //            /* Bambara*/    {"bm", "A:B:C:D:E:\u0190:F:G:H:I:J:K:L:M:N:\u019D:\u014A:O:\u0186:P:R:S:T:U:W:Y:Z"},
            //            /* Tibetan*/    {"bo", "\u0F40:\u0F41:\u0F42:\u0F44:\u0F45:\u0F46:\u0F47:\u0F49:\u0F4F:\u0F50:\u0F51:\u0F53:\u0F54:\u0F55:\u0F56:\u0F58:\u0F59:\u0F5A:\u0F5B:\u0F5D:\u0F5E:\u0F5F:\u0F60:\u0F61:\u0F62:\u0F63:\u0F64:\u0F66:\u0F67:\u0F68"},
            //            /* Chiga*/  {"cgg", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Cherokee*/   {"chr", "\u13A0:\u13A6:\u13AD:\u13B3:\u13B9:\u13BE:\u13C6:\u13CC:\u13D3:\u13DC:\u13E3:\u13E9:\u13EF"},
            //            /* Welsh*/  {"cy", "A:B:C:CH:D:E:F:FF:G:H:I:J:L:LL:M:N:O:P:PH:R:RH:S:T:TH:U:W:Y"},
            //            /* Taita*/  {"dav", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Embu*/   {"ebu", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Ewe*/    {"ee", "A:B:C:D:\u0189:E:\u0190:F:\u0191:G:\u0194:H:I:J:K:L:M:N:\u014A:O:\u0186:P:Q:R:S:T:U:V:\u01B2:W:X:Y:Z"},
            //            /* Esperanto*/  {"eo", "A:B:C:\u0108:D:E:F:G:\u011C:H:\u0124:I:J:\u0134:K:L:M:N:O:P:R:S:\u015C:T:U:\u016C:V:Z"},
            //            /* Fulah*/  {"ff", "A:B:\u0181:C:D:\u018A:E:F:G:H:I:J:K:L:M:N:\u014A:O:P:R:S:T:U:W:Y:\u01B3"},
            //            /* Faroese*/    {"fo", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C6:\u00D8"},
            //            /* Gusii*/  {"guz", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Hausa*/  {"ha", "A:B:\u0181:C:D:\u018A:E:F:G:H:I:J:K:\u0198:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Igbo*/   {"ig", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Machame*/    {"jmc", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Kabyle*/ {"kab", "A:B:C:D:E:\u0190:F:G:\u0194:H:I:J:K:L:M:N:P:Q:R:S:T:U:W:X:Y:Z"},
            //            /* Kamba*/  {"kam", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Makonde*/    {"kde", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Kabuverdianu*/   {"kea", "A:B:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:X:Z"},
            //            /* Koyra Chiini*/   {"khq", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:\u019D:\u014A:O:P:Q:R:S:T:U:W:X:Y:Z"},
            //            /* Kikuyu*/ {"ki", "A:B:C:D:E:G:H:I:J:K:M:N:O:R:T:U:W:Y"},
            //            /* Kalenjin*/   {"kln", "A:B:C:D:E:G:H:I:J:K:L:M:N:O:P:R:S:T:U:W:Y"},
            //            /* Langi*/  {"lag", "A:B:C:D:E:F:G:H:I:\u0197:J:K:L:M:N:O:P:Q:R:S:T:U:\u0244:V:W:X:Y:Z"},
            //            /* Ganda*/  {"lg", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Luo*/    {"luo", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y"},
            //            /* Luyia*/  {"luy", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Masai*/  {"mas", "A:B:C:D:E:\u0190:G:H:I:\u0197:J:K:L:M:N:\u014A:O:\u0186:P:R:S:T:U:\u0244:W:Y"},
            //            /* Meru*/   {"mer", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Morisyen*/   {"mfe", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:X:Y:Z"},
            //            /* Malagasy*/   {"mg", "A:B:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:V:Y:Z"},
            // This should be the correct data.  Commented till it is fixed in CLDR collation data.
            // {"mk", "\u0410:\u0411:\u0412:\u0413:\u0403:\u0414:\u0415:\u0416:\u0417:\u0405:\u0418:\u0408:\u041A:\u040C:\u041B:\u0409:\u041C:\u041D:\u040A:\u041E:\u041F:\u0420:\u0421:\u0422:\u0423:\u0424:\u0425:\u0426:\u0427:\u040F:\u0428"},
            //            /* Macedonian*/ {"mk", "\u0410:\u0411:\u0412:\u0413:\u0414:\u0403:\u0415:\u0416:\u0417:\u0405:\u0418:\u0408:\u041A:\u041B:\u0409:\u041C:\u041D:\u040A:\u041E:\u041F:\u0420:\u0421:\u0422:\u040C:\u0423:\u0424:\u0425:\u0426:\u0427:\u040F:\u0428"},
            // This should be the correct data.  Commented till it is fixed in CLDR collation data.
            // {"mt", "A:B:C:\u010A:D:E:F:\u0120:G:G\u0126:H:\u0126:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:\u017B:Z"},
            //            /* Maltese*/    {"mt", "A:B:\u010A:C:D:E:F:\u0120:G:G\u0126:H:\u0126:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:\u017B:Z"},
            //            /* Nama*/   {"naq", "A:B:C:D:E:F:G:H:I:K:M:N:O:P:Q:R:S:T:U:W:X:Y:Z"},
            //            /* North Ndebele*/  {"nd", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:S:T:U:V:W:X:Y:Z"},
            //            /* Norwegian Nynorsk*/  {"nn", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u00C6:\u00D8:\u00C5"},
            //            /* Nyankole*/   {"nyn", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Oromo*/  {"om", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Romansh*/    {"rm", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Rombo*/  {"rof", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Kinyarwanda*/    {"rw", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Rwa*/    {"rwk", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Samburu*/    {"saq", "A:B:C:D:E:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y"},
            //            /* Sena*/   {"seh", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Koyraboro Senni*/    {"ses", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:\u019D:\u014A:O:P:Q:R:S:T:U:W:X:Y:Z"},
            //            /* Sango*/  {"sg", "A:B:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Tachelhit*/  {"shi", "A:B:C:D:E:\u0190:F:G:\u0194:H:I:J:K:L:M:N:Q:R:S:T:U:W:X:Y:Z"},
            //            /* Tachelhit (Tifinagh)*/   {"shi_Tfng", "\u2D30:\u2D31:\u2D33:\u2D37:\u2D39:\u2D3B:\u2D3C:\u2D3D:\u2D40:\u2D43:\u2D44:\u2D45:\u2D47:\u2D49:\u2D4A:\u2D4D:\u2D4E:\u2D4F:\u2D53:\u2D54:\u2D55:\u2D56:\u2D59:\u2D5A:\u2D5B:\u2D5C:\u2D5F:\u2D61:\u2D62:\u2D63:\u2D65"},
            //            /* Shona*/  {"sn", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Teso*/   {"teo", "A:B:C:D:E:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:X:Y"},
            //            /* Tonga*/  {"to", "A:B:C:D:E:F:G:H:\u02BB:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Central Morocco Tamazight*/  {"tzm", "A:B:C:D:E:\u0190:F:G:\u0194:H:I:J:K:L:M:N:Q:R:S:T:U:W:X:Y:Z"},
            //            /* Uzbek (Latin)*/  {"uz_Latn", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z:\u02BF"},
            //            /* Vunjo*/  {"vun", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:R:S:T:U:V:W:Y:Z"},
            //            /* Soga*/   {"xog", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},
            //            /* Yoruba*/ {"yo", "A:B:C:D:E:F:G:H:I:J:K:L:M:N:O:P:Q:R:S:T:U:V:W:X:Y:Z"},

    };
    public static void main(String[] args) throws Exception{
        new AlphabeticIndexTest().run(args);
    }

    public void TestFirstCharacters() {
        AlphabeticIndex indexCharacters = new AlphabeticIndex(ULocale.ENGLISH);
        RuleBasedCollator collator = indexCharacters.getCollator();
        collator.setStrength(Collator.IDENTICAL);
        List<String> firsts = indexCharacters.getFirstScriptCharacters();
        // Verify that they are all in order, and that each script is represented exactly once.
        UnicodeSet missingScripts = new UnicodeSet("[^[:sc=inherited:][:sc=unknown:][:sc=common:][:Script=Braille:]]");
        String last = "";
        for (String index : firsts) {
            if (collator.compare(last,index) >= 0) {
                errln("Characters not in order: " + last + " !< " + index);
            }
            int script = UScript.getScript(index.codePointAt(0)); // we actually look at just the first char
            UnicodeSet s = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, script);
            if (missingScripts.containsNone(s)) {
                errln("2nd character in script: " + index + "\t" + new UnicodeSet(missingScripts).retainAll(s).toPattern(false));
            }
            missingScripts.removeAll(s);
        }
        if (missingScripts.size() != 0) {
            errln("Missing character from: " + missingScripts);
        }
    }

    public void TestBuckets() {
        String[] test = { "$", "£", "12", "2", 
                "Edgar", "edgar", "Abbot", "Effron", "Zach", "Effron", "Ƶ", "İstanbul", "Istanbul", "istanbul", "ıstanbul",
                "Þor", "Åberg", "Östlund",
                "Ἥρα", "Ἀθηνᾶ", "Ζεύς", "Ποσειδὣν", "Ἅιδης", "Δημήτηρ", "Ἑστιά", 
                //"Ἀπόλλων", "Ἄρτεμις", "Ἑρμἣς", "Ἄρης", "Ἀφροδίτη", "Ἥφαιστος", "Διόνυσος",
                "斉藤", "佐藤", "鈴木", "高橋", "田中", "渡辺", "伊藤", "山本", "中村", "小林", "斎藤", "加藤",
                //"吉田", "山田", "佐々木", "山口", "松本", "井上", "木村", "林", "清水"
        };
        ULocale additionalLocale = ULocale.ENGLISH;            

        for (String[] pair : localeAndIndexCharactersLists) {
            checkBuckets(pair[0], test, additionalLocale, "E", "edgar", "Effron", "Effron");
        }
    }

    private void checkBuckets(String localeString, String[] test, ULocale additionalLocale, String testBucket, String... items) {
        StringBuilder UI = new StringBuilder();
        ULocale desiredLocale = new ULocale(localeString);

        // Create a simple index where the values for the strings are Integers, and add the strings
        AlphabeticIndex<Integer> index = new AlphabeticIndex<Integer>(desiredLocale).addLabels(additionalLocale);
        int counter = 0;
        Counter<String> itemCount = new Counter();
        for (String item : test) {
            index.addRecord(item, counter++); 
            itemCount.add(item, 1);
        }

        logln(desiredLocale + "\t" + desiredLocale.getDisplayName(ULocale.ENGLISH) + " - " + desiredLocale.getDisplayName(desiredLocale) + "\t"
                + index.getCollator().getLocale(ULocale.ACTUAL_LOCALE));
        UI.setLength(0);
        UI.append(desiredLocale + "\t");
        boolean showAll = true;

        // Show index at top. We could skip or gray out empty buckets
        for (AlphabeticIndex.Bucket<Integer> bucket : index) {
            if (showAll || bucket.size() != 0) {
                showLabelAtTop(UI, bucket.getLabel());
            }
        }
        logln(UI.toString());

        // Show the buckets with their contents, skipping empty buckets
        for (AlphabeticIndex.Bucket<Integer> bucket : index) {
            if (bucket.getLabel().equals(testBucket)) {
                Counter<String> keys = getKeys(bucket);
                for (String item : items) {
                    long globalCount = itemCount.get(item);
                    long localeCount = keys.get(item);
                    if (globalCount != localeCount) {
                        errln("Error: in " + "'" + testBucket + "', '" + item + "' should have count "
                                + globalCount + " but has count " + localeCount);
                    }

                }
            }

            if (bucket.size() != 0) {
                showLabelInList(UI, bucket.getLabel());
                for (AlphabeticIndex.Record<Integer> item : bucket) {
                    showIndexedItem(UI, item.getKey(), item.getValue());
                }
                logln(UI.toString());
            }
        }
    }


    private void showLabelAtTop(StringBuilder buffer, String label) {
        buffer.append(label + " ");
    }

    private void showIndexedItem(StringBuilder buffer, CharSequence key, Integer value) {
        buffer.append("\t " + key + "→" + value);
    }

    private void showLabelInList(StringBuilder buffer, String label) {
        buffer.setLength(0);
        buffer.append(label);
    }

    private Counter<String> getKeys(AlphabeticIndex.Bucket<Integer> entry) {
        Counter<String> keys = new Counter<String>();
        for (AlphabeticIndex.Record x : entry) {
            String key = x.getKey().toString();
            keys.add(key, 1);
        }
        return keys;
    }

    public void TestIndexCharactersList() {
        for (String[] localeAndIndexCharacters : localeAndIndexCharactersLists) {
            ULocale locale = new ULocale(localeAndIndexCharacters[0]);
            String expectedIndexCharacters = localeAndIndexCharacters[1];
            Collection<String> indexCharacters = new AlphabeticIndex(locale).getLabels();

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
                String localeString = list[i].toString();
                if (!KEY_LOCALES.contains(localeString)) continue; // TODO change in exhaustive
                ULocale locale = new ULocale(localeString + collationValue);
                if (collationValue.length() > 0 && !Collator.getFunctionalEquivalent("collation", locale).equals(locale)) {
                    //logln("Skipping " + locale);
                    continue;
                }

                if (locale.getCountry().length() != 0) {
                    continue;
                }
                AlphabeticIndex indexCharacters = new AlphabeticIndex(locale);
                final Collection mainChars = indexCharacters.getLabels();
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

    //    public void TestFilter() {
    //        displayPairs(true);
    //        logln("");
    //        displayPairs(false);
    //    }

    private void displayPairs(boolean in) {
        for (String[] pair : localeAndIndexCharactersLists) {
            if (KEY_LOCALES.contains(pair[0]) == in) {
                logln("\t"
                        + "/* " + ULocale.getDisplayName(pair[0], "en") + "*/\t"
                        + "{\"" + pair[0] + "\", \"" + pair[1] + "\"},");
            }
        }
    }
    public void TestZZZ() {
        // stub
    }

    public void TestSimplified() {
        checkBuckets("zh", simplifiedNames, ULocale.ENGLISH, "W", "西");
    }
    public void TestTraditional() {
        checkBuckets("zh_Hant", traditionalNames, ULocale.ENGLISH, "亟", "南門");
    }

    static final String[] hackPinyin = { "吖", "墺", "八", "b", "拔", "蔀", "嚓", "c", "礸", "鹾", "咑", "d", "迏", "陊", "妸", "e",
            "鋨", "荋", "发", "f", "醗", "馥", "猤", "g", "釓", "腂", "妎", "h", "鉿", "夻", "丌", "j", "枅", "鵘", "咔", "k", "開",
            "穒", "垃", "l", "拉", "鮥", "嘸", "m", "麻", "旀", "拿", "n", "肭", "桛", "噢", "o", "毮", "讴", "妑", "p", "耙", "谱",
            "七", "q", "恓", "罖", "呥", "r", "犪", "渃", "仨", "s", "钑", "鏁", "他", "t", "鉈", "柝", "屲", "w", "啘", "婺", "夕",
            "x", "吸", "殾", "丫", "y", "芽", "蕴", "帀", "z", "災", "尊"};
    
    static final String[] simplifiedNames = { "Abbot", "Morton", "Zachary", "Williams", "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "楮", "卫", "蒋", "沈",
        "韩", "杨", "朱", "秦", "尤", "许", "何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏", "陶", "姜", "戚", "谢", "邹",
        "喻", "柏", "水", "窦", "章", "云", "苏", "潘", "葛", "奚", "范", "彭", "郎", "鲁", "韦", "昌", "马", "苗", "凤", "花", "方",
        "俞", "任", "袁", "柳", "酆", "鲍", "史", "唐", "费", "廉", "岑", "薛", "雷", "贺", "倪", "汤", "滕", "殷", "罗", "毕", "郝",
        "邬", "安", "常", "乐", "于", "时", "傅", "皮", "卞", "齐", "康", "伍", "余", "元", "卜", "顾", "孟", "平", "黄", "和", "穆",
        "萧", "尹", "姚", "邵", "湛", "汪", "祁", "毛", "禹", "狄", "米", "贝", "明", "臧", "计", "伏", "成", "戴", "谈", "宋", "茅",
        "庞", "熊", "纪", "舒", "屈", "项", "祝", "董", "梁", "杜", "阮", "蓝", "闽", "席", "季", "麻", "强", "贾", "路", "娄", "危",
        "江", "童", "颜", "郭", "梅", "盛", "林", "刁", "锺", "徐", "丘", "骆", "高", "夏", "蔡", "田", "樊", "胡", "凌", "霍", "虞",
        "万", "支", "柯", "昝", "管", "卢", "莫", "经", "房", "裘", "缪", "干", "解", "应", "宗", "丁", "宣", "贲", "邓", "郁", "单",
        "杭", "洪", "包", "诸", "左", "石", "崔", "吉", "钮", "龚", "程", "嵇", "邢", "滑", "裴", "陆", "荣", "翁", "荀", "羊", "於",
        "惠", "甄", "麹", "家", "封", "芮", "羿", "储", "靳", "汲", "邴", "糜", "松", "井", "段", "富", "巫", "乌", "焦", "巴", "弓",
        "牧", "隗", "山", "谷", "车", "侯", "宓", "蓬", "全", "郗", "班", "仰", "秋", "仲", "伊", "宫", "宁", "仇", "栾", "暴", "甘",
        "斜", "厉", "戎", "祖", "武", "符", "刘", "景", "詹", "束", "龙", "叶", "幸", "司", "韶", "郜", "黎", "蓟", "薄", "印", "宿",
        "白", "怀", "蒲", "邰", "从", "鄂", "索", "咸", "籍", "赖", "卓", "蔺", "屠", "蒙", "池", "乔", "阴", "郁", "胥", "能", "苍",
        "双", "闻", "莘", "党", "翟", "谭", "贡", "劳", "逄", "姬", "申", "扶", "堵", "冉", "宰", "郦", "雍", "郤", "璩", "桑", "桂",
        "濮", "牛", "寿", "通", "边", "扈", "燕", "冀", "郏", "浦", "尚", "农", "温", "别", "庄", "晏", "柴", "瞿", "阎", "充", "慕",
        "连", "茹", "习", "宦", "艾", "鱼", "容", "向", "古", "易", "慎", "戈", "廖", "庾", "终", "暨", "居", "衡", "步", "都", "耿",
        "满", "弘", "匡", "国", "文", "寇", "广", "禄", "阙", "东", "欧", "殳", "沃", "利", "蔚", "越", "夔", "隆", "师", "巩", "厍",
        "聂", "晁", "勾", "敖", "融", "冷", "訾", "辛", "阚", "那", "简", "饶", "空", "曾", "毋", "沙", "乜", "养", "鞠", "须", "丰",
        "巢", "关", "蒯", "相", "查", "后", "荆", "红", "游", "竺", "权", "逑", "盖", "益", "桓", "公", "万俟", "司马", "上官", "欧阳",
        "夏侯", "诸葛", "闻人", "东方", "赫连", "皇甫", "尉迟", "公羊", "澹台", "公冶", "宗政", "濮阳", "淳于", "单于", "太叔", "申屠", "公孙", "仲孙",
        "轩辕", "令狐", "锺离", "宇文", "长孙", "慕容", "鲜于", "闾丘", "司徒", "司空", "丌官", "司寇", "仉", "督", "子车", "颛孙", "端木", "巫马",
        "公西", "漆雕", "乐正", "壤驷", "公良", "拓拔", "夹谷", "宰父", "谷梁", "晋", "楚", "阎", "法", "汝", "鄢", "涂", "钦", "段干", "百里",
        "东郭", "南门", "呼延", "归", "海", "羊舌", "微生", "岳", "帅", "缑", "亢", "况", "后", "有", "琴", "梁丘", "左丘", "东门", "西门",
        "商", "牟", "佘", "佴", "伯", "赏", "南宫", "墨", "哈", "谯", "笪", "年", "爱", "阳", "佟" };

    static final String[] traditionalNames = { "Abbot", "Morton", "Zachary", "Williams", "趙", "錢", "孫", "李", "周", "吳", "鄭", "王", "馮", "陳", "楮", "衛", "蔣", "沈",
        "韓", "楊", "朱", "秦", "尤", "許", "何", "呂", "施", "張", "孔", "曹", "嚴", "華", "金", "魏", "陶", "姜", "戚", "謝", "鄒",
        "喻", "柏", "水", "竇", "章", "雲", "蘇", "潘", "葛", "奚", "範", "彭", "郎", "魯", "韋", "昌", "馬", "苗", "鳳", "花", "方",
        "俞", "任", "袁", "柳", "酆", "鮑", "史", "唐", "費", "廉", "岑", "薛", "雷", "賀", "倪", "湯", "滕", "殷", "羅", "畢", "郝",
        "鄔", "安", "常", "樂", "於", "時", "傅", "皮", "卞", "齊", "康", "伍", "餘", "元", "卜", "顧", "孟", "平", "黃", "和", "穆",
        "蕭", "尹", "姚", "邵", "湛", "汪", "祁", "毛", "禹", "狄", "米", "貝", "明", "臧", "計", "伏", "成", "戴", "談", "宋", "茅",
        "龐", "熊", "紀", "舒", "屈", "項", "祝", "董", "梁", "杜", "阮", "藍", "閩", "席", "季", "麻", "強", "賈", "路", "婁", "危",
        "江", "童", "顏", "郭", "梅", "盛", "林", "刁", "鍾", "徐", "丘", "駱", "高", "夏", "蔡", "田", "樊", "胡", "凌", "霍", "虞",
        "萬", "支", "柯", "昝", "管", "盧", "莫", "經", "房", "裘", "繆", "幹", "解", "應", "宗", "丁", "宣", "賁", "鄧", "鬱", "單",
        "杭", "洪", "包", "諸", "左", "石", "崔", "吉", "鈕", "龔", "程", "嵇", "邢", "滑", "裴", "陸", "榮", "翁", "荀", "羊", "於",
        "惠", "甄", "麴", "家", "封", "芮", "羿", "儲", "靳", "汲", "邴", "糜", "松", "井", "段", "富", "巫", "烏", "焦", "巴", "弓",
        "牧", "隗", "山", "谷", "車", "侯", "宓", "蓬", "全", "郗", "班", "仰", "秋", "仲", "伊", "宮", "寧", "仇", "欒", "暴", "甘",
        "斜", "厲", "戎", "祖", "武", "符", "劉", "景", "詹", "束", "龍", "葉", "幸", "司", "韶", "郜", "黎", "薊", "薄", "印", "宿",
        "白", "懷", "蒲", "邰", "從", "鄂", "索", "咸", "籍", "賴", "卓", "藺", "屠", "蒙", "池", "喬", "陰", "鬱", "胥", "能", "蒼",
        "雙", "聞", "莘", "黨", "翟", "譚", "貢", "勞", "逄", "姬", "申", "扶", "堵", "冉", "宰", "酈", "雍", "郤", "璩", "桑", "桂",
        "濮", "牛", "壽", "通", "邊", "扈", "燕", "冀", "郟", "浦", "尚", "農", "溫", "別", "莊", "晏", "柴", "瞿", "閻", "充", "慕",
        "連", "茹", "習", "宦", "艾", "魚", "容", "向", "古", "易", "慎", "戈", "廖", "庾", "終", "暨", "居", "衡", "步", "都", "耿",
        "滿", "弘", "匡", "國", "文", "寇", "廣", "祿", "闕", "東", "歐", "殳", "沃", "利", "蔚", "越", "夔", "隆", "師", "鞏", "厙",
        "聶", "晁", "勾", "敖", "融", "冷", "訾", "辛", "闞", "那", "簡", "饒", "空", "曾", "毋", "沙", "乜", "養", "鞠", "須", "豐",
        "巢", "關", "蒯", "相", "查", "後", "荊", "紅", "遊", "竺", "權", "逑", "蓋", "益", "桓", "公", "万俟", "司馬", "上官", "歐陽",
        "夏侯", "諸葛", "聞人", "東方", "赫連", "皇甫", "尉遲", "公羊", "澹台", "公冶", "宗政", "濮陽", "淳于", "單于", "太叔", "申屠", "公孫", "仲孫",
        "軒轅", "令狐", "鍾離", "宇文", "長孫", "慕容", "鮮于", "閭丘", "司徒", "司空", "丌官", "司寇", "仉", "督", "子車", "顓孫", "端木", "巫馬",
        "公西", "漆雕", "樂正", "壤駟", "公良", "拓拔", "夾谷", "宰父", "穀梁", "晉", "楚", "閻", "法", "汝", "鄢", "塗", "欽", "段干", "百里",
        "東郭", "南門", "呼延", "歸", "海", "羊舌", "微生", "岳", "帥", "緱", "亢", "況", "後", "有", "琴", "梁丘", "左丘", "東門", "西門",
        "商", "牟", "佘", "佴", "伯", "賞", "南宮", "墨", "哈", "譙", "笪", "年", "愛", "陽", "佟" };
}
