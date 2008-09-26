/**
*******************************************************************************
* Copyright (C) 1996-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

// copied from the Transliterator demo

package com.ibm.icu.dev.test.util;
import java.util.*;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

/**
 * Incrementally returns the set of all strings that case-fold to the same value.
 */
public class CaseIterator {
    
    // testing stuff
    private static Transliterator toName = Transliterator.getInstance("[:^ascii:] Any-Name");
    private static Transliterator toHex = Transliterator.getInstance("[:^ascii:] Any-Hex");
    private static Transliterator toHex2 = Transliterator.getInstance("[[^\u0021-\u007F]-[,]] Any-Hex");
    
    // global tables (could be precompiled)
    private static Map fromCaseFold = new HashMap();
    private static Map toCaseFold = new HashMap();
    private static int maxLength = 0;
    
    // This exception list is generated on the console by turning on the GENERATED flag, 
    // which MUST be false for normal operation.
    // Once the list is generated, it is pasted in here.
    // A bit of a cludge, but this bootstrapping is the easiest way 
    // to get around certain complications in the data.
    
    private static final boolean GENERATE = false;

    private static final boolean DUMP = false;
    
    private static String[][] exceptionList = {
        // a\N{MODIFIER LETTER RIGHT HALF RING}
        {"a\u02BE","A\u02BE","a\u02BE",},
        // ff
        {"ff","FF","Ff","fF","ff",},
        // ffi
        {"ffi","FFI","FFi","FfI","Ffi","F\uFB01","fFI","fFi","ffI","ffi","f\uFB01","\uFB00I","\uFB00i",},
        // ffl
        {"ffl","FFL","FFl","FfL","Ffl","F\uFB02","fFL","fFl","ffL","ffl","f\uFB02","\uFB00L","\uFB00l",},
        // fi
        {"fi","FI","Fi","fI","fi",},
        // fl
        {"fl","FL","Fl","fL","fl",},
        // h\N{COMBINING MACRON BELOW}
        {"h\u0331","H\u0331","h\u0331",},
        // i\N{COMBINING DOT ABOVE}
        {"i\u0307","I\u0307","i\u0307",},
        // j\N{COMBINING CARON}
        {"j\u030C","J\u030C","j\u030C",},
        // ss
        {"ss","SS","Ss","S\u017F","sS","ss","s\u017F","\u017FS","\u017Fs","\u017F\u017F",},
        // st
        {"st","ST","St","sT","st","\u017FT","\u017Ft",},
        // t\N{COMBINING DIAERESIS}
        {"t\u0308","T\u0308","t\u0308",},
        // w\N{COMBINING RING ABOVE}
        {"w\u030A","W\u030A","w\u030A",},
        // y\N{COMBINING RING ABOVE}
        {"y\u030A","Y\u030A","y\u030A",},
        // \N{MODIFIER LETTER APOSTROPHE}n
        {"\u02BCn","\u02BCN","\u02BCn",},
        // \N{GREEK SMALL LETTER ALPHA WITH TONOS}\N{GREEK SMALL LETTER IOTA}
        {"\u03AC\u03B9","\u0386\u0345","\u0386\u0399","\u0386\u03B9","\u0386\u1FBE","\u03AC\u0345","\u03AC\u0399","\u03AC\u03B9","\u03AC\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH TONOS}\N{GREEK SMALL LETTER IOTA}
        {"\u03AE\u03B9","\u0389\u0345","\u0389\u0399","\u0389\u03B9","\u0389\u1FBE","\u03AE\u0345","\u03AE\u0399","\u03AE\u03B9","\u03AE\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA}\N{COMBINING GREEK PERISPOMENI}
        {"\u03B1\u0342","\u0391\u0342","\u03B1\u0342",},
        // \N{GREEK SMALL LETTER ALPHA}\N{COMBINING GREEK PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u03B1\u0342\u03B9","\u0391\u0342\u0345","\u0391\u0342\u0399","\u0391\u0342\u03B9","\u0391\u0342\u1FBE",
            "\u03B1\u0342\u0345","\u03B1\u0342\u0399","\u03B1\u0342\u03B9","\u03B1\u0342\u1FBE","\u1FB6\u0345",
            "\u1FB6\u0399","\u1FB6\u03B9","\u1FB6\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA}\N{GREEK SMALL LETTER IOTA}
        {"\u03B1\u03B9","\u0391\u0345","\u0391\u0399","\u0391\u03B9","\u0391\u1FBE","\u03B1\u0345","\u03B1\u0399","\u03B1\u03B9","\u03B1\u1FBE",},
        // \N{GREEK SMALL LETTER ETA}\N{COMBINING GREEK PERISPOMENI}
        {"\u03B7\u0342","\u0397\u0342","\u03B7\u0342",},
        // \N{GREEK SMALL LETTER ETA}\N{COMBINING GREEK PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u03B7\u0342\u03B9","\u0397\u0342\u0345","\u0397\u0342\u0399","\u0397\u0342\u03B9","\u0397\u0342\u1FBE",
            "\u03B7\u0342\u0345","\u03B7\u0342\u0399","\u03B7\u0342\u03B9","\u03B7\u0342\u1FBE","\u1FC6\u0345","\u1FC6\u0399",
            "\u1FC6\u03B9","\u1FC6\u1FBE",},
        // \N{GREEK SMALL LETTER ETA}\N{GREEK SMALL LETTER IOTA}
        {"\u03B7\u03B9","\u0397\u0345","\u0397\u0399","\u0397\u03B9","\u0397\u1FBE","\u03B7\u0345","\u03B7\u0399","\u03B7\u03B9","\u03B7\u1FBE",},
        // \N{GREEK SMALL LETTER IOTA}\N{COMBINING DIAERESIS}\N{COMBINING GRAVE ACCENT}
        {"\u03B9\u0308\u0300","\u0345\u0308\u0300","\u0399\u0308\u0300","\u03B9\u0308\u0300","\u1FBE\u0308\u0300",},
        // \N{GREEK SMALL LETTER IOTA}\N{COMBINING DIAERESIS}\N{COMBINING ACUTE ACCENT}
        {"\u03B9\u0308\u0301","\u0345\u0308\u0301","\u0399\u0308\u0301","\u03B9\u0308\u0301","\u1FBE\u0308\u0301",},
        // \N{GREEK SMALL LETTER IOTA}\N{COMBINING DIAERESIS}\N{COMBINING GREEK PERISPOMENI}
        {"\u03B9\u0308\u0342","\u0345\u0308\u0342","\u0399\u0308\u0342","\u03B9\u0308\u0342","\u1FBE\u0308\u0342",},
        // \N{GREEK SMALL LETTER IOTA}\N{COMBINING GREEK PERISPOMENI}
        {"\u03B9\u0342","\u0345\u0342","\u0399\u0342","\u03B9\u0342","\u1FBE\u0342",},
        // \N{GREEK SMALL LETTER RHO}\N{COMBINING COMMA ABOVE}
        {"\u03C1\u0313","\u03A1\u0313","\u03C1\u0313","\u03F1\u0313",},
        // \N{GREEK SMALL LETTER UPSILON}\N{COMBINING DIAERESIS}\N{COMBINING GRAVE ACCENT}
        {"\u03C5\u0308\u0300","\u03A5\u0308\u0300","\u03C5\u0308\u0300",},
        // \N{GREEK SMALL LETTER UPSILON}\N{COMBINING DIAERESIS}\N{COMBINING ACUTE ACCENT}
        {"\u03C5\u0308\u0301","\u03A5\u0308\u0301","\u03C5\u0308\u0301",},
        // \N{GREEK SMALL LETTER UPSILON}\N{COMBINING DIAERESIS}\N{COMBINING GREEK PERISPOMENI}
        {"\u03C5\u0308\u0342","\u03A5\u0308\u0342","\u03C5\u0308\u0342",},
        // \N{GREEK SMALL LETTER UPSILON}\N{COMBINING COMMA ABOVE}
        {"\u03C5\u0313","\u03A5\u0313","\u03C5\u0313",},
        // \N{GREEK SMALL LETTER UPSILON}\N{COMBINING COMMA ABOVE}\N{COMBINING GRAVE ACCENT}
        {"\u03C5\u0313\u0300","\u03A5\u0313\u0300","\u03C5\u0313\u0300","\u1F50\u0300",},
        // \N{GREEK SMALL LETTER UPSILON}\N{COMBINING COMMA ABOVE}\N{COMBINING ACUTE ACCENT}
        {"\u03C5\u0313\u0301","\u03A5\u0313\u0301","\u03C5\u0313\u0301","\u1F50\u0301",},
        // \N{GREEK SMALL LETTER UPSILON}\N{COMBINING COMMA ABOVE}\N{COMBINING GREEK PERISPOMENI}
        {"\u03C5\u0313\u0342","\u03A5\u0313\u0342","\u03C5\u0313\u0342","\u1F50\u0342",},
        // \N{GREEK SMALL LETTER UPSILON}\N{COMBINING GREEK PERISPOMENI}
        {"\u03C5\u0342","\u03A5\u0342","\u03C5\u0342",},
        // \N{GREEK SMALL LETTER OMEGA}\N{COMBINING GREEK PERISPOMENI}
        {"\u03C9\u0342","\u03A9\u0342","\u03C9\u0342","\u2126\u0342",},
        // \N{GREEK SMALL LETTER OMEGA}\N{COMBINING GREEK PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u03C9\u0342\u03B9","\u03A9\u0342\u0345","\u03A9\u0342\u0399","\u03A9\u0342\u03B9","\u03A9\u0342\u1FBE","\u03C9\u0342\u0345","\u03C9\u0342\u0399","\u03C9\u0342\u03B9","\u03C9\u0342\u1FBE","\u1FF6\u0345",
            "\u1FF6\u0399","\u1FF6\u03B9","\u1FF6\u1FBE","\u2126\u0342\u0345","\u2126\u0342\u0399","\u2126\u0342\u03B9","\u2126\u0342\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA}\N{GREEK SMALL LETTER IOTA}
        {"\u03C9\u03B9","\u03A9\u0345","\u03A9\u0399","\u03A9\u03B9","\u03A9\u1FBE","\u03C9\u0345","\u03C9\u0399","\u03C9\u03B9","\u03C9\u1FBE","\u2126\u0345","\u2126\u0399","\u2126\u03B9","\u2126\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH TONOS}\N{GREEK SMALL LETTER IOTA}
        {"\u03CE\u03B9","\u038F\u0345","\u038F\u0399","\u038F\u03B9","\u038F\u1FBE","\u03CE\u0345","\u03CE\u0399","\u03CE\u03B9","\u03CE\u1FBE",},
        // \N{ARMENIAN SMALL LETTER ECH}\N{ARMENIAN SMALL LETTER YIWN}
        {"\u0565\u0582","\u0535\u0552","\u0535\u0582","\u0565\u0552","\u0565\u0582",},
        // \N{ARMENIAN SMALL LETTER MEN}\N{ARMENIAN SMALL LETTER ECH}
        {"\u0574\u0565","\u0544\u0535","\u0544\u0565","\u0574\u0535","\u0574\u0565",},
        // \N{ARMENIAN SMALL LETTER MEN}\N{ARMENIAN SMALL LETTER INI}
        {"\u0574\u056B","\u0544\u053B","\u0544\u056B","\u0574\u053B","\u0574\u056B",},
        // \N{ARMENIAN SMALL LETTER MEN}\N{ARMENIAN SMALL LETTER XEH}
        {"\u0574\u056D","\u0544\u053D","\u0544\u056D","\u0574\u053D","\u0574\u056D",},
        // \N{ARMENIAN SMALL LETTER MEN}\N{ARMENIAN SMALL LETTER NOW}
        {"\u0574\u0576","\u0544\u0546","\u0544\u0576","\u0574\u0546","\u0574\u0576",},
        // \N{ARMENIAN SMALL LETTER VEW}\N{ARMENIAN SMALL LETTER NOW}
        {"\u057E\u0576","\u054E\u0546","\u054E\u0576","\u057E\u0546","\u057E\u0576",},
        // \N{GREEK SMALL LETTER ALPHA WITH PSILI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F00\u03B9","\u1F00\u0345","\u1F00\u0399","\u1F00\u03B9","\u1F00\u1FBE","\u1F08\u0345","\u1F08\u0399","\u1F08\u03B9","\u1F08\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA WITH DASIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F01\u03B9","\u1F01\u0345","\u1F01\u0399","\u1F01\u03B9","\u1F01\u1FBE","\u1F09\u0345","\u1F09\u0399","\u1F09\u03B9","\u1F09\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA WITH PSILI AND VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F02\u03B9","\u1F02\u0345","\u1F02\u0399","\u1F02\u03B9","\u1F02\u1FBE","\u1F0A\u0345","\u1F0A\u0399","\u1F0A\u03B9","\u1F0A\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA WITH DASIA AND VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F03\u03B9","\u1F03\u0345","\u1F03\u0399","\u1F03\u03B9","\u1F03\u1FBE","\u1F0B\u0345","\u1F0B\u0399","\u1F0B\u03B9","\u1F0B\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA WITH PSILI AND OXIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F04\u03B9","\u1F04\u0345","\u1F04\u0399","\u1F04\u03B9","\u1F04\u1FBE","\u1F0C\u0345","\u1F0C\u0399","\u1F0C\u03B9","\u1F0C\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA WITH DASIA AND OXIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F05\u03B9","\u1F05\u0345","\u1F05\u0399","\u1F05\u03B9","\u1F05\u1FBE","\u1F0D\u0345","\u1F0D\u0399","\u1F0D\u03B9","\u1F0D\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA WITH PSILI AND PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F06\u03B9","\u1F06\u0345","\u1F06\u0399","\u1F06\u03B9","\u1F06\u1FBE","\u1F0E\u0345","\u1F0E\u0399","\u1F0E\u03B9","\u1F0E\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA WITH DASIA AND PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F07\u03B9","\u1F07\u0345","\u1F07\u0399","\u1F07\u03B9","\u1F07\u1FBE","\u1F0F\u0345","\u1F0F\u0399","\u1F0F\u03B9","\u1F0F\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH PSILI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F20\u03B9","\u1F20\u0345","\u1F20\u0399","\u1F20\u03B9","\u1F20\u1FBE","\u1F28\u0345","\u1F28\u0399","\u1F28\u03B9","\u1F28\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH DASIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F21\u03B9","\u1F21\u0345","\u1F21\u0399","\u1F21\u03B9","\u1F21\u1FBE","\u1F29\u0345","\u1F29\u0399","\u1F29\u03B9","\u1F29\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH PSILI AND VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F22\u03B9","\u1F22\u0345","\u1F22\u0399","\u1F22\u03B9","\u1F22\u1FBE","\u1F2A\u0345","\u1F2A\u0399","\u1F2A\u03B9","\u1F2A\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH DASIA AND VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F23\u03B9","\u1F23\u0345","\u1F23\u0399","\u1F23\u03B9","\u1F23\u1FBE","\u1F2B\u0345","\u1F2B\u0399","\u1F2B\u03B9","\u1F2B\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH PSILI AND OXIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F24\u03B9","\u1F24\u0345","\u1F24\u0399","\u1F24\u03B9","\u1F24\u1FBE","\u1F2C\u0345","\u1F2C\u0399","\u1F2C\u03B9","\u1F2C\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH DASIA AND OXIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F25\u03B9","\u1F25\u0345","\u1F25\u0399","\u1F25\u03B9","\u1F25\u1FBE","\u1F2D\u0345","\u1F2D\u0399","\u1F2D\u03B9","\u1F2D\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH PSILI AND PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F26\u03B9","\u1F26\u0345","\u1F26\u0399","\u1F26\u03B9","\u1F26\u1FBE","\u1F2E\u0345","\u1F2E\u0399","\u1F2E\u03B9","\u1F2E\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH DASIA AND PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F27\u03B9","\u1F27\u0345","\u1F27\u0399","\u1F27\u03B9","\u1F27\u1FBE","\u1F2F\u0345","\u1F2F\u0399","\u1F2F\u03B9","\u1F2F\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH PSILI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F60\u03B9","\u1F60\u0345","\u1F60\u0399","\u1F60\u03B9","\u1F60\u1FBE","\u1F68\u0345","\u1F68\u0399","\u1F68\u03B9","\u1F68\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH DASIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F61\u03B9","\u1F61\u0345","\u1F61\u0399","\u1F61\u03B9","\u1F61\u1FBE","\u1F69\u0345","\u1F69\u0399","\u1F69\u03B9","\u1F69\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH PSILI AND VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F62\u03B9","\u1F62\u0345","\u1F62\u0399","\u1F62\u03B9","\u1F62\u1FBE","\u1F6A\u0345","\u1F6A\u0399","\u1F6A\u03B9","\u1F6A\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH DASIA AND VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F63\u03B9","\u1F63\u0345","\u1F63\u0399","\u1F63\u03B9","\u1F63\u1FBE","\u1F6B\u0345","\u1F6B\u0399","\u1F6B\u03B9","\u1F6B\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH PSILI AND OXIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F64\u03B9","\u1F64\u0345","\u1F64\u0399","\u1F64\u03B9","\u1F64\u1FBE","\u1F6C\u0345","\u1F6C\u0399","\u1F6C\u03B9","\u1F6C\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH DASIA AND OXIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F65\u03B9","\u1F65\u0345","\u1F65\u0399","\u1F65\u03B9","\u1F65\u1FBE","\u1F6D\u0345","\u1F6D\u0399","\u1F6D\u03B9","\u1F6D\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH PSILI AND PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F66\u03B9","\u1F66\u0345","\u1F66\u0399","\u1F66\u03B9","\u1F66\u1FBE","\u1F6E\u0345","\u1F6E\u0399","\u1F6E\u03B9","\u1F6E\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH DASIA AND PERISPOMENI}\N{GREEK SMALL LETTER IOTA}
        {"\u1F67\u03B9","\u1F67\u0345","\u1F67\u0399","\u1F67\u03B9","\u1F67\u1FBE","\u1F6F\u0345","\u1F6F\u0399","\u1F6F\u03B9","\u1F6F\u1FBE",},
        // \N{GREEK SMALL LETTER ALPHA WITH VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F70\u03B9","\u1F70\u0345","\u1F70\u0399","\u1F70\u03B9","\u1F70\u1FBE","\u1FBA\u0345","\u1FBA\u0399","\u1FBA\u03B9","\u1FBA\u1FBE",},
        // \N{GREEK SMALL LETTER ETA WITH VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F74\u03B9","\u1F74\u0345","\u1F74\u0399","\u1F74\u03B9","\u1F74\u1FBE","\u1FCA\u0345","\u1FCA\u0399","\u1FCA\u03B9","\u1FCA\u1FBE",},
        // \N{GREEK SMALL LETTER OMEGA WITH VARIA}\N{GREEK SMALL LETTER IOTA}
        {"\u1F7C\u03B9","\u1F7C\u0345","\u1F7C\u0399","\u1F7C\u03B9","\u1F7C\u1FBE","\u1FFA\u0345","\u1FFA\u0399","\u1FFA\u03B9","\u1FFA\u1FBE",},
    };
    
    // this initializes the data used to generated the case-equivalents

    static {
        
        // Gather up the exceptions in a form we can use
        
        if (!GENERATE) {
            for (int i = 0; i < exceptionList.length; ++i) {
                String[] exception = exceptionList[i];
                Set s = new HashSet();
                // there has to be some method to do the following, but I can't find it in the collections
                for (int j = 0; j < exception.length; ++j) {
                    s.add(exception[j]);
                }
                fromCaseFold.put(exception[0], s);
            }
        }
        
        // walk through all the characters, and at every case fold result,
        // put a set of all the characters that map to that result

        boolean defaultmapping = true; // false for turkish
        for (int i = 0; i <= 0x10FFFF; ++i) {
            int cat = UCharacter.getType(i);
            if (cat == Character.UNASSIGNED || cat == Character.PRIVATE_USE) continue;
            
            String cp = UTF16.valueOf(i);
            String mapped = UCharacter.foldCase(cp, defaultmapping);
            if (mapped.equals(cp)) continue;
            
            if (maxLength < mapped.length()) maxLength = mapped.length();
            
            // at this point, have different case folding
            
            Set s = (Set) fromCaseFold.get(mapped);
            if (s == null) {
                s = new HashSet();
                s.add(mapped); // add the case fold result itself
                fromCaseFold.put(mapped, s);
            }
            s.add(cp);
            toCaseFold.put(cp, mapped);
            toCaseFold.put(mapped, mapped); // add mapping to self
        }
        
        // Emit the final data

        if (DUMP) {
            System.out.println("maxLength = " + maxLength);

            System.out.println("\nfromCaseFold:");
            Iterator it = fromCaseFold.keySet().iterator();
            while (it.hasNext()) {
                Object key = it.next();
                System.out.print(" " + toHex2.transliterate((String)key) + ": ");
                Set s = (Set) fromCaseFold.get(key);
                Iterator it2 = s.iterator();
                boolean first = true;
                while (it2.hasNext()) {
                    if (first) {
                        first = false;
                    } else {
                        System.out.print(", ");
                    }
                    System.out.print(toHex2.transliterate((String)it2.next()));
                }
                System.out.println("");
            }

            System.out.println("\ntoCaseFold:");
            it = toCaseFold.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                String value = (String) toCaseFold.get(key);
                System.out.println(" " + toHex2.transliterate(key) + ": " + toHex2.transliterate(value));
            }            
        }
        
        // Now convert all those sets into linear arrays
        // We can't do this in place in Java, so make a temporary target array
        
        // Note: This could be transformed into a single array, with offsets into it.
        // Might be best choice in C.
        
        
        Map fromCaseFold2 = new HashMap();
        Iterator it = fromCaseFold.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            Set s = (Set) fromCaseFold.get(key);
            String[] temp = new String[s.size()];
            s.toArray(temp);
            fromCaseFold2.put(key, temp);
        }
        fromCaseFold = fromCaseFold2;

        // We have processed everything, so the iterator will now work
        // The following is normally OFF. 
        // It is here to generate (under the GENERATE flag) the static exception list.
        // It must be at the very end of initialization, so that the iterator is functional.
        // (easiest to do it that way)
            
        if (GENERATE) {

            // first get small set of items that have multiple characters
            
            Set multichars = new TreeSet();
            it = fromCaseFold.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                if (UTF16.countCodePoint(key) < 2) continue;
                multichars.add(key);
            }            
            
            // now we will go through each of them.
            
            CaseIterator ci = new CaseIterator();
            it = multichars.iterator();
            
            while (it.hasNext()) {
                String key = (String) it.next();
                
                // here is a nasty complication. Take 'ffi' ligature. We
                // can't just close it, since we would miss the combination
                // that includes the 'fi' => "fi" ligature
                // so first do a pass through, and add substring combinations
                // we call this a 'partial closure'
                
                Set partialClosure = new TreeSet();
                partialClosure.add(key);
                
                if (UTF16.countCodePoint(key) > 2) {
                    Iterator multiIt2 = multichars.iterator();
                    while (multiIt2.hasNext()) {
                        String otherKey = (String) multiIt2.next();
                        if (otherKey.length() >= key.length()) continue;
                        int pos = -1;
                        while (true) {
                            // The following is not completely general
                            // but works for the actual cased stuff,
                            // and should work for future characters, since we won't have
                            // more ligatures & other oddities.
                            pos = key.indexOf(otherKey, pos+1);
                            if (pos < 0) break;
                            int endPos = pos + otherKey.length();
                            // we know we have a proper substring,
                            // so get the combinations
                            String[] choices = (String[]) fromCaseFold.get(otherKey);
                            for (int ii = 0; ii < choices.length; ++ii) {
                                String patchwork = key.substring(0, pos)
                                    + choices[ii]
                                    + key.substring(endPos);
                                partialClosure.add(patchwork);
                            }
                        }
                    }
                }
                
                // now, for each thing in the partial closure, get its
                // case closure and add it to the final result.
                
                Set closure = new TreeSet(); // this will be the real closure
                Iterator partialIt = partialClosure.iterator();
                while (partialIt.hasNext()) {
                    String key2 = (String) partialIt.next();
                    ci.reset(key2);
                    for (String temp = ci.next(); temp != null; temp = ci.next()) {
                        closure.add(temp);
                    }
                    // form closure
                    /*String[] choices = (String[]) fromCaseFold.get(key2);
                    for (int i = 0; i < choices.length; ++i) {
                        ci.reset(choices[i]);
                        String temp;
                        while (null != (temp = ci.next())) {
                            closure.add(temp);
                        }
                    }
                    */
                }
                
                // print it out, so that it can be cut and pasted back into this document.
                
                Iterator it2 = closure.iterator();
                System.out.println("\t// " + toName.transliterate(key));
                System.out.print("\t{\"" + toHex.transliterate(key) + "\",");
                while (it2.hasNext()) {
                    String item = (String)it2.next();
                    System.out.print("\"" + toHex.transliterate(item) + "\",");
                }
                System.out.println("},");
            }
        }
    }
    
    // ============ PRIVATE CLASS DATA ============ 
    
    // pieces that we will put together
    // is not changed during iteration
    private int count = 0;
    private String[][] variants;
    
    // state information, changes during iteration
    private boolean done = false;
    private int[] counts;
    
    // internal buffer for efficiency
    private StringBuffer nextBuffer = new StringBuffer();
    
    // ========================  

    /**
     * Reset to different source. Once reset, the iteration starts from the beginning.
     * @param source The string to get case variants for
     */
    public void reset(String source) {
        
        // allocate arrays to store pieces
        // using length might be slightly too long, but we don't care much
        
        counts = new int[source.length()];
        variants = new String[source.length()][];
        
        // walk through the source, and break up into pieces
        // each piece becomes an array of equivalent values
        // TODO: could optimized this later to coalesce all single string pieces
        
        String piece = null;
        count = 0;
        for (int i = 0; i < source.length(); i += piece.length()) {
            
            // find *longest* matching piece
            String caseFold = null;
            
            if (GENERATE) {
                // do exactly one CP
                piece = UTF16.valueOf(source, i);
                caseFold = (String) toCaseFold.get(piece);
            } else {               
                int max = i + maxLength;
                if (max > source.length()) max = source.length();
                for (int j = max; j > i; --j) {
                    piece = source.substring(i, j);
                    caseFold = (String) toCaseFold.get(piece);
                    if (caseFold != null) break;
                }
            }
            
            // if we fail, pick one code point
            if (caseFold == null) {
                piece = UTF16.valueOf(source, i);
                variants[count++] = new String[] {piece}; // single item string
            } else {
                variants[count++] = (String[])fromCaseFold.get(caseFold);
            }
        }
        reset();
    }
    
    /**
     * Restart the iteration from the beginning, but with same source
     */
    public void reset() {
        done = false;
        for (int i = 0; i < count; ++i) {
            counts[i] = 0;
        }
    }
    
    /**
     * Iterates through the case variants.
     * @return next case variant. Each variant will case-fold to the same value as the source will.
     * When the iteration is done, null is returned.
     */
    public String next() {
        
        if (done) return null;
        int i;
        
        // TODO Optimize so we keep the piece before and after the current position
        // so we don't have so much concatenation
        
        // get the result, a concatenation
        
        nextBuffer.setLength(0);
        for (i = 0; i < count; ++i) {
            nextBuffer.append(variants[i][counts[i]]);
        }
        
        // find the next right set of pieces to concatenate
        
        for (i = count-1; i >= 0; --i) {
            counts[i]++;
            if (counts[i] < variants[i].length) break;
            counts[i] = 0;
        }
        
        // if we go too far, bail
        
        if (i < 0) {
            done = true;
        }
        
        return nextBuffer.toString();            
    }
        
        
    /**
     * Temporary test, just to see how the stuff works.
     */
    static public void main(String[] args) {
        String[] testCases = {"fiss", "h\u03a3"};
        CaseIterator ci = new CaseIterator();
        
        for (int i = 0; i < testCases.length; ++i) {
            String item = testCases[i];
            System.out.println();
            System.out.println("Testing: " + toName.transliterate(item));
            System.out.println();
            ci.reset(item);
            int count = 0;
            for (String temp = ci.next(); temp != null; temp = ci.next()) {
                System.out.println(toName.transliterate(temp));
                count++;
            }
            System.out.println("Total: " + count);
        }

        // generate a list of all caseless characters -- characters whose
        // case closure is themselves.

        UnicodeSet caseless = new UnicodeSet();

        for (int i = 0; i <= 0x10FFFF; ++i) {
            String cp = UTF16.valueOf(i);
            ci.reset(cp);
            int count = 0;
            String fold = null;
            for (String temp = ci.next(); temp != null; temp = ci.next()) {
                fold = temp;
                if (++count > 1) break;
            }
            if (count==1 && fold.equals(cp)) {
                caseless.add(i);
            }
        }

        System.out.println("caseless = " + caseless.toPattern(true));

        UnicodeSet not_lc = new UnicodeSet("[:^lc:]");
        
        UnicodeSet a = new UnicodeSet();
        a.set(not_lc);
        a.removeAll(caseless);
        System.out.println("[:^lc:] - caseless = " + a.toPattern(true));

        a.set(caseless);
        a.removeAll(not_lc);
        System.out.println("caseless - [:^lc:] = " + a.toPattern(true));
    }
}
