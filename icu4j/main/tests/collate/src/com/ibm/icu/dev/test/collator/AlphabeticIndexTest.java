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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.AlphabeticIndex;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.AlphabeticIndex.Bucket;
import com.ibm.icu.text.AlphabeticIndex.Record;
import com.ibm.icu.text.AlphabeticIndex.Bucket.LabelType;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public class AlphabeticIndexTest extends TestFmwk {
    /**
     * 
     */
    private static final String ARROW = "\u2192";
    private static final boolean DEBUG = false;
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
        ULocale additionalLocale = ULocale.ENGLISH;            

        for (String[] pair : localeAndIndexCharactersLists) {
            checkBuckets(pair[0], SimpleTests, additionalLocale, "E", "edgar", "Effron", "Effron");
        }
    }

    public void TestInflow() {
        Object[][] tests = {
                {0, ULocale.ENGLISH},
                {0, ULocale.ENGLISH, new ULocale("el")},
                {1, ULocale.ENGLISH, new ULocale("ru")},
                {0, ULocale.ENGLISH, new ULocale("el"), new UnicodeSet("[\u2C80]"), new ULocale("ru")},
                {0, ULocale.ENGLISH},
                {2, ULocale.ENGLISH, new ULocale("ru"), ULocale.JAPANESE},
        };
        for (Object[] test : tests) {
            int expected = (Integer) test[0];
            AlphabeticIndex<Double> indexCharacters = new AlphabeticIndex((ULocale)test[1]);
            for (int i = 2; i < test.length; ++i) {
                if (test[i] instanceof ULocale) {
                    indexCharacters.addLabels((ULocale)test[i]);
                } else {
                    indexCharacters.addLabels((UnicodeSet)test[i]);
                }
            }
            Counter<AlphabeticIndex.Bucket.LabelType> counter = new Counter();
            for (Bucket<Double> bucket : indexCharacters) {
                LabelType labelType = bucket.getLabelType();
                counter.add(labelType, 1);
            }
            String printList = Arrays.asList(test).toString();
            assertEquals(LabelType.UNDERFLOW + "\t" + printList, 1, counter.get(LabelType.UNDERFLOW));
            assertEquals(LabelType.INFLOW + "\t" + printList, expected, counter.get(LabelType.INFLOW));
            if (expected != counter.get(LabelType.INFLOW)) {
                // for debugging
                AlphabeticIndex<Double> indexCharacters2 = new AlphabeticIndex((ULocale)test[1]);
                for (int i = 2; i < test.length; ++i) {
                    if (test[i] instanceof ULocale) {
                        indexCharacters2.addLabels((ULocale)test[i]);
                    } else {
                        indexCharacters2.addLabels((UnicodeSet)test[i]);
                    }
                }
                List<Bucket<Double>> buckets = CollectionUtilities.addAll(indexCharacters.iterator(), new ArrayList<Bucket<Double>>());
                logln(buckets.toString());
            }
            assertEquals(LabelType.OVERFLOW + "\t" + printList, 1, counter.get(LabelType.OVERFLOW));
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
                    showIndexedItem(UI, item.getName(), item.getData());
                }
                logln(UI.toString());
            }
        }
    }

    public <T> void showIndex(AlphabeticIndex<T> index, boolean showEmpty) {
        StringBuilder UI = new StringBuilder();
        for (AlphabeticIndex.Bucket<T> bucket : index) {
            if (showEmpty || bucket.size() != 0) {
                showLabelInList(UI, bucket.getLabel());
                for (AlphabeticIndex.Record<T> item : bucket) {
                    showIndexedItem(UI, item.getName(), item.getData());
                }
                logln(UI.toString());
            }
        }
    }

    /**
     * @param myBucketLabels
     * @param myBucketContents
     * @param b
     */
    private void showIndex(List<String> myBucketLabels, ArrayList<Set<R4<RawCollationKey, String, Integer, Double>>> myBucketContents, boolean showEmpty) {
        StringBuilder UI = new StringBuilder();

        for (int i = 0; i < myBucketLabels.size(); ++i) {
            Set<R4<RawCollationKey, String, Integer, Double>> bucket = myBucketContents.get(i);
            if (!showEmpty && bucket.size() == 0) {
                continue;
            }
            UI.setLength(0);
            UI.append("*").append(myBucketLabels.get(i));
            for (R4<RawCollationKey, String, Integer, Double> item : bucket) {
                UI.append("\t ").append(item.get1().toString()).append(ARROW).append(item.get3().toString());
            }
            logln(UI.toString());
        }
    }

    private void showLabelAtTop(StringBuilder buffer, String label) {
        buffer.append(label + " ");
    }

    private <T> void showIndexedItem(StringBuilder buffer, CharSequence key, T value) {
        buffer.append("\t " + key + ARROW + value);
    }

    private void showLabelInList(StringBuilder buffer, String label) {
        buffer.setLength(0);
        buffer.append(label);
    }

    private Counter<String> getKeys(AlphabeticIndex.Bucket<Integer> entry) {
        Counter<String> keys = new Counter<String>();
        for (AlphabeticIndex.Record x : entry) {
            String key = x.getName().toString();
            keys.add(key, 1);
        }
        return keys;
    }

    public void TestIndexCharactersList() {
        for (String[] localeAndIndexCharacters : localeAndIndexCharactersLists) {
            ULocale locale = new ULocale(localeAndIndexCharacters[0]);
            String expectedIndexCharacters = "\u2026:" + localeAndIndexCharacters[1] + ":\u2026";
            Collection<String> indexCharacters = new AlphabeticIndex(locale).getBucketLabels();

            // Join the elements of the list to a string with delimiter ":"
            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = indexCharacters.iterator();
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
                final Collection mainChars = indexCharacters.getBucketLabels();
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

    public void TestClientSupport() {
        for (String localeString : KEY_LOCALES) { // KEY_LOCALES, new String[] {"zh"}
            ULocale ulocale = new ULocale(localeString);
            AlphabeticIndex<Double> indexCharacters = new AlphabeticIndex<Double>(ulocale).addLabels(ULocale.ENGLISH);
            RuleBasedCollator collator = indexCharacters.getCollator();
            String [][] tests;

            if (!localeString.equals("zh") ) {
                tests = new String[][] {SimpleTests};
            } else {
                tests = new String[][] {SimpleTests, hackPinyin, simplifiedNames};
            }

            for (String [] shortTest : tests) {
                double testValue = 100;
                indexCharacters.clearRecords();
                for (String name : shortTest) {
                    indexCharacters.addRecord(name, testValue++);
                }

                if (DEBUG) showIndex(indexCharacters, false);

                // make my own copy
                testValue = 100;
                List<String> myBucketLabels = indexCharacters.getBucketLabels();
                ArrayList<Set<R4<RawCollationKey, String, Integer, Double>>> myBucketContents = new ArrayList<Set<R4<RawCollationKey, String, Integer, Double>>>(myBucketLabels.size());
                for (int i = 0; i < myBucketLabels.size(); ++i) {
                    myBucketContents.add(new TreeSet<R4<RawCollationKey, String, Integer, Double>>());
                }
                for (String name : shortTest) {
                    int bucketIndex = indexCharacters.getBucketIndex(name);
                    Set<R4<RawCollationKey, String, Integer, Double>> myBucket = myBucketContents.get(bucketIndex);
                    RawCollationKey rawCollationKey = collator.getRawCollationKey(name, null);
                    R4<RawCollationKey, String, Integer, Double> row = Row.of(rawCollationKey, name, name.length(), testValue++);
                    myBucket.add(row);
                }
                if (DEBUG) showIndex(myBucketLabels, myBucketContents, false);

                // now compare
                int index = 0;
                boolean gotError = false;
                for (AlphabeticIndex.Bucket<Double> bucket : indexCharacters) {
                    String bucketLabel = bucket.getLabel();
                    String myLabel = myBucketLabels.get(index);
                    if (!bucketLabel.equals(myLabel)) {
                        gotError |= !assertEquals(ulocale + "\tBucket Labels (" + index + ")", bucketLabel, myLabel);
                    }
                    Set<R4<RawCollationKey, String, Integer, Double>> myBucket = myBucketContents.get(index);
                    Iterator<R4<RawCollationKey, String, Integer, Double>> myBucketIterator = myBucket.iterator();
                    int recordIndex = 0;
                    for (Record<Double> record : bucket) {
                        String myName = null;
                        if (myBucketIterator.hasNext()) {
                            R4<RawCollationKey, String, Integer, Double> myRecord = myBucketIterator.next();
                            myName = (String) myRecord.get1();
                        }
                        if (!record.getName().equals(myName)) {
                            gotError |= !assertEquals(ulocale + "\t" + bucketLabel + "\t" + "Record Names (" + index + "." + recordIndex++ + ")", record.getName(), myName);
                        }
                    }
                    while (myBucketIterator.hasNext()) {
                        R4<RawCollationKey, String, Integer, Double> myRecord = myBucketIterator.next();
                        String myName = (String) myRecord.get1();
                        gotError |= !assertEquals(ulocale + "\t" + bucketLabel + "\t" + "Record Names (" + index + "." + recordIndex++ + ")", null, myName);
                    }
                    index++;
                }
                if (gotError) {
                    showIndex(myBucketLabels, myBucketContents, false);
                    showIndex(indexCharacters, false);
                }
            }
        }
    }

    public void TestZZZ() {
        //            int x = 3;
        //            AlphabeticIndex index = new AlphabeticIndex(ULocale.ENGLISH);
        //            UnicodeSet additions = new UnicodeSet();
        //            additions.add(0x410).add(0x415);  // Cyrillic
        //            // additions.add(0x391).add(0x393);     // Greek
        //            index.addLabels(additions);
        //            int lc = index.getLabels().size();
        //            List  labels = index.getLabels();
        //            System.out.println("Label Count = " + lc + "\t" + labels);
        //            System.out.println("Bucket Count =" + index.getBucketCount());
    }

    public void TestSimplified() {
        checkBuckets("zh", simplifiedNames, ULocale.ENGLISH, "W", "\u897f");
    }
    public void TestTraditional() {
        checkBuckets("zh_Hant", traditionalNames, ULocale.ENGLISH, "\u4e9f", "\u5357\u9580");
    }

    static final String[] SimpleTests = { "$", "\u00a3", "12", "2", 
        "Davis", "Davis", "Abbot", "\u1D05avis", "Zach", "\u1D05avis", "\u01b5", "\u0130stanbul", "Istanbul", "istanbul", "\u0131stanbul",
        "\u00deor", "\u00c5berg", "\u00d6stlund",
        "\u1f2d\u03c1\u03b1", "\u1f08\u03b8\u03b7\u03bd\u1fb6", "\u0396\u03b5\u03cd\u03c2", "\u03a0\u03bf\u03c3\u03b5\u03b9\u03b4\u1f63\u03bd", "\u1f0d\u03b9\u03b4\u03b7\u03c2", "\u0394\u03b7\u03bc\u03ae\u03c4\u03b7\u03c1", "\u1f19\u03c3\u03c4\u03b9\u03ac", 
        //"\u1f08\u03c0\u03cc\u03bb\u03bb\u03c9\u03bd", "\u1f0c\u03c1\u03c4\u03b5\u03bc\u03b9\u03c2", "\u1f19\u03c1\u03bc\u1f23\u03c2", "\u1f0c\u03c1\u03b7\u03c2", "\u1f08\u03c6\u03c1\u03bf\u03b4\u03af\u03c4\u03b7", "\u1f2d\u03c6\u03b1\u03b9\u03c3\u03c4\u03bf\u03c2", "\u0394\u03b9\u03cc\u03bd\u03c5\u03c3\u03bf\u03c2",
        "\u6589\u85e4", "\u4f50\u85e4", "\u9234\u6728", "\u9ad8\u6a4b", "\u7530\u4e2d", "\u6e21\u8fba", "\u4f0a\u85e4", "\u5c71\u672c", "\u4e2d\u6751", "\u5c0f\u6797", "\u658e\u85e4", "\u52a0\u85e4",
        //"\u5409\u7530", "\u5c71\u7530", "\u4f50\u3005\u6728", "\u5c71\u53e3", "\u677e\u672c", "\u4e95\u4e0a", "\u6728\u6751", "\u6797", "\u6e05\u6c34"
    };

    static final String[] hackPinyin = { 
        "\u0101", "\u5416", "\u58ba", //
        "b", "\u516b", "\u62d4", "\u8500", //
        "c", "\u5693", "\u7938", "\u9e7e", //
        "d", "\u5491", "\u8fcf", "\u964a", //
        "\u0113","\u59b8", "\u92e8", "\u834b", //
        "f", "\u53d1", "\u9197", "\u99a5", //
        "g", "\u7324", "\u91d3", "\u8142", //
        "h", "\u598e", "\u927f", "\u593b", //
        "j", "\u4e0c", "\u6785", "\u9d58", //
        "k", "\u5494", "\u958b", "\u7a52", //
        "l", "\u5783", "\u62c9", "\u9ba5", //
        "m", "\u5638", "\u9ebb", "\u65c0", //
        "n", "\u62ff", "\u80ad", "\u685b", //
        "\u014D", "\u5662", "\u6bee", "\u8bb4", //
        "p", "\u5991", "\u8019", "\u8c31", //
        "q", "\u4e03", "\u6053", "\u7f56", //
        "r", "\u5465", "\u72aa", "\u6e03", //
        "s", "\u4ee8", "\u9491", "\u93c1", //
        "t", "\u4ed6", "\u9248", "\u67dd", //
        "w", "\u5c72", "\u5558", "\u5a7a", //
        "x", "\u5915", "\u5438", "\u6bbe", //
        "y", "\u4e2b", "\u82bd", "\u8574", //
        "z", "\u5e00", "\u707d", "\u5c0a"
    };

    static final String[] simplifiedNames = { 
        "Abbot", "Morton", "Zachary", "Williams", "\u8d75", "\u94b1", "\u5b59", "\u674e", "\u5468", "\u5434", "\u90d1", "\u738b", "\u51af", "\u9648", "\u696e", "\u536b", "\u848b", "\u6c88",
        "\u97e9", "\u6768", "\u6731", "\u79e6", "\u5c24", "\u8bb8", "\u4f55", "\u5415", "\u65bd", "\u5f20", "\u5b54", "\u66f9", "\u4e25", "\u534e", "\u91d1", "\u9b4f", "\u9676", "\u59dc", "\u621a", "\u8c22", "\u90b9",
        "\u55bb", "\u67cf", "\u6c34", "\u7aa6", "\u7ae0", "\u4e91", "\u82cf", "\u6f58", "\u845b", "\u595a", "\u8303", "\u5f6d", "\u90ce", "\u9c81", "\u97e6", "\u660c", "\u9a6c", "\u82d7", "\u51e4", "\u82b1", "\u65b9",
        "\u4fde", "\u4efb", "\u8881", "\u67f3", "\u9146", "\u9c8d", "\u53f2", "\u5510", "\u8d39", "\u5ec9", "\u5c91", "\u859b", "\u96f7", "\u8d3a", "\u502a", "\u6c64", "\u6ed5", "\u6bb7", "\u7f57", "\u6bd5", "\u90dd",
        "\u90ac", "\u5b89", "\u5e38", "\u4e50", "\u4e8e", "\u65f6", "\u5085", "\u76ae", "\u535e", "\u9f50", "\u5eb7", "\u4f0d", "\u4f59", "\u5143", "\u535c", "\u987e", "\u5b5f", "\u5e73", "\u9ec4", "\u548c", "\u7a46",
        "\u8427", "\u5c39", "\u59da", "\u90b5", "\u6e5b", "\u6c6a", "\u7941", "\u6bdb", "\u79b9", "\u72c4", "\u7c73", "\u8d1d", "\u660e", "\u81e7", "\u8ba1", "\u4f0f", "\u6210", "\u6234", "\u8c08", "\u5b8b", "\u8305",
        "\u5e9e", "\u718a", "\u7eaa", "\u8212", "\u5c48", "\u9879", "\u795d", "\u8463", "\u6881", "\u675c", "\u962e", "\u84dd", "\u95fd", "\u5e2d", "\u5b63", "\u9ebb", "\u5f3a", "\u8d3e", "\u8def", "\u5a04", "\u5371",
        "\u6c5f", "\u7ae5", "\u989c", "\u90ed", "\u6885", "\u76db", "\u6797", "\u5201", "\u953a", "\u5f90", "\u4e18", "\u9a86", "\u9ad8", "\u590f", "\u8521", "\u7530", "\u6a0a", "\u80e1", "\u51cc", "\u970d", "\u865e",
        "\u4e07", "\u652f", "\u67ef", "\u661d", "\u7ba1", "\u5362", "\u83ab", "\u7ecf", "\u623f", "\u88d8", "\u7f2a", "\u5e72", "\u89e3", "\u5e94", "\u5b97", "\u4e01", "\u5ba3", "\u8d32", "\u9093", "\u90c1", "\u5355",
        "\u676d", "\u6d2a", "\u5305", "\u8bf8", "\u5de6", "\u77f3", "\u5d14", "\u5409", "\u94ae", "\u9f9a", "\u7a0b", "\u5d47", "\u90a2", "\u6ed1", "\u88f4", "\u9646", "\u8363", "\u7fc1", "\u8340", "\u7f8a", "\u65bc",
        "\u60e0", "\u7504", "\u9eb9", "\u5bb6", "\u5c01", "\u82ae", "\u7fbf", "\u50a8", "\u9773", "\u6c72", "\u90b4", "\u7cdc", "\u677e", "\u4e95", "\u6bb5", "\u5bcc", "\u5deb", "\u4e4c", "\u7126", "\u5df4", "\u5f13",
        "\u7267", "\u9697", "\u5c71", "\u8c37", "\u8f66", "\u4faf", "\u5b93", "\u84ec", "\u5168", "\u90d7", "\u73ed", "\u4ef0", "\u79cb", "\u4ef2", "\u4f0a", "\u5bab", "\u5b81", "\u4ec7", "\u683e", "\u66b4", "\u7518",
        "\u659c", "\u5389", "\u620e", "\u7956", "\u6b66", "\u7b26", "\u5218", "\u666f", "\u8a79", "\u675f", "\u9f99", "\u53f6", "\u5e78", "\u53f8", "\u97f6", "\u90dc", "\u9ece", "\u84df", "\u8584", "\u5370", "\u5bbf",
        "\u767d", "\u6000", "\u84b2", "\u90b0", "\u4ece", "\u9102", "\u7d22", "\u54b8", "\u7c4d", "\u8d56", "\u5353", "\u853a", "\u5c60", "\u8499", "\u6c60", "\u4e54", "\u9634", "\u90c1", "\u80e5", "\u80fd", "\u82cd",
        "\u53cc", "\u95fb", "\u8398", "\u515a", "\u7fdf", "\u8c2d", "\u8d21", "\u52b3", "\u9004", "\u59ec", "\u7533", "\u6276", "\u5835", "\u5189", "\u5bb0", "\u90e6", "\u96cd", "\u90e4", "\u74a9", "\u6851", "\u6842",
        "\u6fee", "\u725b", "\u5bff", "\u901a", "\u8fb9", "\u6248", "\u71d5", "\u5180", "\u90cf", "\u6d66", "\u5c1a", "\u519c", "\u6e29", "\u522b", "\u5e84", "\u664f", "\u67f4", "\u77bf", "\u960e", "\u5145", "\u6155",
        "\u8fde", "\u8339", "\u4e60", "\u5ba6", "\u827e", "\u9c7c", "\u5bb9", "\u5411", "\u53e4", "\u6613", "\u614e", "\u6208", "\u5ed6", "\u5ebe", "\u7ec8", "\u66a8", "\u5c45", "\u8861", "\u6b65", "\u90fd", "\u803f",
        "\u6ee1", "\u5f18", "\u5321", "\u56fd", "\u6587", "\u5bc7", "\u5e7f", "\u7984", "\u9619", "\u4e1c", "\u6b27", "\u6bb3", "\u6c83", "\u5229", "\u851a", "\u8d8a", "\u5914", "\u9686", "\u5e08", "\u5de9", "\u538d",
        "\u8042", "\u6641", "\u52fe", "\u6556", "\u878d", "\u51b7", "\u8a3e", "\u8f9b", "\u961a", "\u90a3", "\u7b80", "\u9976", "\u7a7a", "\u66fe", "\u6bcb", "\u6c99", "\u4e5c", "\u517b", "\u97a0", "\u987b", "\u4e30",
        "\u5de2", "\u5173", "\u84af", "\u76f8", "\u67e5", "\u540e", "\u8346", "\u7ea2", "\u6e38", "\u7afa", "\u6743", "\u9011", "\u76d6", "\u76ca", "\u6853", "\u516c", "\u4e07\u4fdf", "\u53f8\u9a6c", "\u4e0a\u5b98", "\u6b27\u9633",
        "\u590f\u4faf", "\u8bf8\u845b", "\u95fb\u4eba", "\u4e1c\u65b9", "\u8d6b\u8fde", "\u7687\u752b", "\u5c09\u8fdf", "\u516c\u7f8a", "\u6fb9\u53f0", "\u516c\u51b6", "\u5b97\u653f", "\u6fee\u9633", "\u6df3\u4e8e", "\u5355\u4e8e", "\u592a\u53d4", "\u7533\u5c60", "\u516c\u5b59", "\u4ef2\u5b59",
        "\u8f69\u8f95", "\u4ee4\u72d0", "\u953a\u79bb", "\u5b87\u6587", "\u957f\u5b59", "\u6155\u5bb9", "\u9c9c\u4e8e", "\u95fe\u4e18", "\u53f8\u5f92", "\u53f8\u7a7a", "\u4e0c\u5b98", "\u53f8\u5bc7", "\u4ec9", "\u7763", "\u5b50\u8f66", "\u989b\u5b59", "\u7aef\u6728", "\u5deb\u9a6c",
        "\u516c\u897f", "\u6f06\u96d5", "\u4e50\u6b63", "\u58e4\u9a77", "\u516c\u826f", "\u62d3\u62d4", "\u5939\u8c37", "\u5bb0\u7236", "\u8c37\u6881", "\u664b", "\u695a", "\u960e", "\u6cd5", "\u6c5d", "\u9122", "\u6d82", "\u94a6", "\u6bb5\u5e72", "\u767e\u91cc",
        "\u4e1c\u90ed", "\u5357\u95e8", "\u547c\u5ef6", "\u5f52", "\u6d77", "\u7f8a\u820c", "\u5fae\u751f", "\u5cb3", "\u5e05", "\u7f11", "\u4ea2", "\u51b5", "\u540e", "\u6709", "\u7434", "\u6881\u4e18", "\u5de6\u4e18", "\u4e1c\u95e8", "\u897f\u95e8",
        "\u5546", "\u725f", "\u4f58", "\u4f74", "\u4f2f", "\u8d4f", "\u5357\u5bab", "\u58a8", "\u54c8", "\u8c2f", "\u7b2a", "\u5e74", "\u7231", "\u9633", "\u4f5f"
    };

    static final String[] traditionalNames = { 
        "Abbot", "Morton", "Zachary", "Williams", "\u8d99", "\u9322", "\u5b6b", "\u674e", "\u5468", "\u5433", "\u912d", "\u738b", "\u99ae", "\u9673", "\u696e", "\u885b", "\u8523", "\u6c88",
        "\u97d3", "\u694a", "\u6731", "\u79e6", "\u5c24", "\u8a31", "\u4f55", "\u5442", "\u65bd", "\u5f35", "\u5b54", "\u66f9", "\u56b4", "\u83ef", "\u91d1", "\u9b4f", "\u9676", "\u59dc", "\u621a", "\u8b1d", "\u9112",
        "\u55bb", "\u67cf", "\u6c34", "\u7ac7", "\u7ae0", "\u96f2", "\u8607", "\u6f58", "\u845b", "\u595a", "\u7bc4", "\u5f6d", "\u90ce", "\u9b6f", "\u97cb", "\u660c", "\u99ac", "\u82d7", "\u9cf3", "\u82b1", "\u65b9",
        "\u4fde", "\u4efb", "\u8881", "\u67f3", "\u9146", "\u9b91", "\u53f2", "\u5510", "\u8cbb", "\u5ec9", "\u5c91", "\u859b", "\u96f7", "\u8cc0", "\u502a", "\u6e6f", "\u6ed5", "\u6bb7", "\u7f85", "\u7562", "\u90dd",
        "\u9114", "\u5b89", "\u5e38", "\u6a02", "\u65bc", "\u6642", "\u5085", "\u76ae", "\u535e", "\u9f4a", "\u5eb7", "\u4f0d", "\u9918", "\u5143", "\u535c", "\u9867", "\u5b5f", "\u5e73", "\u9ec3", "\u548c", "\u7a46",
        "\u856d", "\u5c39", "\u59da", "\u90b5", "\u6e5b", "\u6c6a", "\u7941", "\u6bdb", "\u79b9", "\u72c4", "\u7c73", "\u8c9d", "\u660e", "\u81e7", "\u8a08", "\u4f0f", "\u6210", "\u6234", "\u8ac7", "\u5b8b", "\u8305",
        "\u9f90", "\u718a", "\u7d00", "\u8212", "\u5c48", "\u9805", "\u795d", "\u8463", "\u6881", "\u675c", "\u962e", "\u85cd", "\u95a9", "\u5e2d", "\u5b63", "\u9ebb", "\u5f37", "\u8cc8", "\u8def", "\u5a41", "\u5371",
        "\u6c5f", "\u7ae5", "\u984f", "\u90ed", "\u6885", "\u76db", "\u6797", "\u5201", "\u937e", "\u5f90", "\u4e18", "\u99f1", "\u9ad8", "\u590f", "\u8521", "\u7530", "\u6a0a", "\u80e1", "\u51cc", "\u970d", "\u865e",
        "\u842c", "\u652f", "\u67ef", "\u661d", "\u7ba1", "\u76e7", "\u83ab", "\u7d93", "\u623f", "\u88d8", "\u7e46", "\u5e79", "\u89e3", "\u61c9", "\u5b97", "\u4e01", "\u5ba3", "\u8cc1", "\u9127", "\u9b31", "\u55ae",
        "\u676d", "\u6d2a", "\u5305", "\u8af8", "\u5de6", "\u77f3", "\u5d14", "\u5409", "\u9215", "\u9f94", "\u7a0b", "\u5d47", "\u90a2", "\u6ed1", "\u88f4", "\u9678", "\u69ae", "\u7fc1", "\u8340", "\u7f8a", "\u65bc",
        "\u60e0", "\u7504", "\u9eb4", "\u5bb6", "\u5c01", "\u82ae", "\u7fbf", "\u5132", "\u9773", "\u6c72", "\u90b4", "\u7cdc", "\u677e", "\u4e95", "\u6bb5", "\u5bcc", "\u5deb", "\u70cf", "\u7126", "\u5df4", "\u5f13",
        "\u7267", "\u9697", "\u5c71", "\u8c37", "\u8eca", "\u4faf", "\u5b93", "\u84ec", "\u5168", "\u90d7", "\u73ed", "\u4ef0", "\u79cb", "\u4ef2", "\u4f0a", "\u5bae", "\u5be7", "\u4ec7", "\u6b12", "\u66b4", "\u7518",
        "\u659c", "\u53b2", "\u620e", "\u7956", "\u6b66", "\u7b26", "\u5289", "\u666f", "\u8a79", "\u675f", "\u9f8d", "\u8449", "\u5e78", "\u53f8", "\u97f6", "\u90dc", "\u9ece", "\u858a", "\u8584", "\u5370", "\u5bbf",
        "\u767d", "\u61f7", "\u84b2", "\u90b0", "\u5f9e", "\u9102", "\u7d22", "\u54b8", "\u7c4d", "\u8cf4", "\u5353", "\u85fa", "\u5c60", "\u8499", "\u6c60", "\u55ac", "\u9670", "\u9b31", "\u80e5", "\u80fd", "\u84bc",
        "\u96d9", "\u805e", "\u8398", "\u9ee8", "\u7fdf", "\u8b5a", "\u8ca2", "\u52de", "\u9004", "\u59ec", "\u7533", "\u6276", "\u5835", "\u5189", "\u5bb0", "\u9148", "\u96cd", "\u90e4", "\u74a9", "\u6851", "\u6842",
        "\u6fee", "\u725b", "\u58fd", "\u901a", "\u908a", "\u6248", "\u71d5", "\u5180", "\u90df", "\u6d66", "\u5c1a", "\u8fb2", "\u6eab", "\u5225", "\u838a", "\u664f", "\u67f4", "\u77bf", "\u95bb", "\u5145", "\u6155",
        "\u9023", "\u8339", "\u7fd2", "\u5ba6", "\u827e", "\u9b5a", "\u5bb9", "\u5411", "\u53e4", "\u6613", "\u614e", "\u6208", "\u5ed6", "\u5ebe", "\u7d42", "\u66a8", "\u5c45", "\u8861", "\u6b65", "\u90fd", "\u803f",
        "\u6eff", "\u5f18", "\u5321", "\u570b", "\u6587", "\u5bc7", "\u5ee3", "\u797f", "\u95d5", "\u6771", "\u6b50", "\u6bb3", "\u6c83", "\u5229", "\u851a", "\u8d8a", "\u5914", "\u9686", "\u5e2b", "\u978f", "\u5399",
        "\u8076", "\u6641", "\u52fe", "\u6556", "\u878d", "\u51b7", "\u8a3e", "\u8f9b", "\u95de", "\u90a3", "\u7c21", "\u9952", "\u7a7a", "\u66fe", "\u6bcb", "\u6c99", "\u4e5c", "\u990a", "\u97a0", "\u9808", "\u8c50",
        "\u5de2", "\u95dc", "\u84af", "\u76f8", "\u67e5", "\u5f8c", "\u834a", "\u7d05", "\u904a", "\u7afa", "\u6b0a", "\u9011", "\u84cb", "\u76ca", "\u6853", "\u516c", "\u4e07\u4fdf", "\u53f8\u99ac", "\u4e0a\u5b98", "\u6b50\u967d",
        "\u590f\u4faf", "\u8af8\u845b", "\u805e\u4eba", "\u6771\u65b9", "\u8d6b\u9023", "\u7687\u752b", "\u5c09\u9072", "\u516c\u7f8a", "\u6fb9\u53f0", "\u516c\u51b6", "\u5b97\u653f", "\u6fee\u967d", "\u6df3\u4e8e", "\u55ae\u4e8e", "\u592a\u53d4", "\u7533\u5c60", "\u516c\u5b6b", "\u4ef2\u5b6b",
        "\u8ed2\u8f45", "\u4ee4\u72d0", "\u937e\u96e2", "\u5b87\u6587", "\u9577\u5b6b", "\u6155\u5bb9", "\u9bae\u4e8e", "\u95ad\u4e18", "\u53f8\u5f92", "\u53f8\u7a7a", "\u4e0c\u5b98", "\u53f8\u5bc7", "\u4ec9", "\u7763", "\u5b50\u8eca", "\u9853\u5b6b", "\u7aef\u6728", "\u5deb\u99ac",
        "\u516c\u897f", "\u6f06\u96d5", "\u6a02\u6b63", "\u58e4\u99df", "\u516c\u826f", "\u62d3\u62d4", "\u593e\u8c37", "\u5bb0\u7236", "\u7a40\u6881", "\u6649", "\u695a", "\u95bb", "\u6cd5", "\u6c5d", "\u9122", "\u5857", "\u6b3d", "\u6bb5\u5e72", "\u767e\u91cc",
        "\u6771\u90ed", "\u5357\u9580", "\u547c\u5ef6", "\u6b78", "\u6d77", "\u7f8a\u820c", "\u5fae\u751f", "\u5cb3", "\u5e25", "\u7df1", "\u4ea2", "\u6cc1", "\u5f8c", "\u6709", "\u7434", "\u6881\u4e18", "\u5de6\u4e18", "\u6771\u9580", "\u897f\u9580",
        "\u5546", "\u725f", "\u4f58", "\u4f74", "\u4f2f", "\u8cde", "\u5357\u5bae", "\u58a8", "\u54c8", "\u8b59", "\u7b2a", "\u5e74", "\u611b", "\u967d", "\u4f5f" 
    };
}
