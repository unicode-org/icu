/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/BreakIteratorRules_th.java,v $ 
 * $Date: 2000/03/10 04:07:26 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */
package com.ibm.text.resources;

import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.net.URL;

public class BreakIteratorRules_th extends ListResourceBundle {
    public Object[][] getContents() {

        URL url = getClass().getResource("thai_dict");

        // if dictionary wasn't found, then this resource bundle doesn't have
        // much to contribute...
        if (url == null) {
            return new Object[0][0];
        }

        return new Object[][] {
            // names of classes to instantiate for the different kinds of break
            // iterator.  Notice we're now using DictionaryBasedBreakIterator
            // for word and line breaking.
            { "BreakIteratorClasses",
                new String[] { "RuleBasedBreakIterator",           // character-break iterator class
                               "DictionaryBasedBreakIterator",     // word-break iterator class
                               "DictionaryBasedBreakIterator",     // line-break iterator class
                               "RuleBasedBreakIterator" }          // sentence-break iterator class
            },

            { "WordBreakRules",
                "$dictionary=[\u0e01-\u0e2e\u0e30-\u0e3a\u0e40-\u0e44\u0e47-\u0e4e];" // this rule breaks the iterator with mixed Thai and English
                + "$ignore=[[[:Mn:][:Me:][:Cf:]]-{$dictionary}];"
                + "paiyannoi=[\u0e2f];"
                + "maiyamok=[\u0e46];"
                + "danda=[\u0964\u0965];"
                + "kanji=[\u3005\u4e00-\u9fa5\uf900-\ufa2d];"
                + "kata=[\u30a1-\u30fa];"
                + "hira=[\u3041-\u3094];"
                + "cjk-diacrit=[\u3099-\u309c];"
                + "let=[[[:L:][:Mc:]]-[{kanji}{kata}{hira}{cjk-diacrit}{$dictionary}]];"
                + "dgt=[:N:];"
                + "mid-word=[[:Pd:]\u00ad\u2027\\\"\\\'\\.];"
                + "mid-num=[\\\"\\\'\\,\u066b\\.];"
                + "pre-num=[[[:Sc:]-[\u00a2]]\\#\\.];"
                + "post-num=[\\%\\&\u00a2\u066a\u2030\u2031];"
                + "ls=[\n\u000c\u2028\u2029];"
                + "ws=[[:Zs:]\t];"
                + "word=(({let}+({mid-word}{let}+)*){danda}?);"
                + "number=({dgt}+({mid-num}{dgt}+)*);"
                + "thai-etc={paiyannoi}\u0e25{paiyannoi};"
                + ".;"
                + "{word}?({number}{word})*({number}{post-num}?)?;"
                + "{pre-num}({number}{word})*({number}{post-num}?)?;"
                + "{$dictionary}+({paiyannoi}{maiyamok}?)?;"
                + "{$dictionary}+{paiyannoi}/([^\u0e25{$ignore}]"
                        + "|\u0e25[^{paiyannoi}{$ignore}]);"
                + "{thai-etc};"
                + "{ws}*\r?{ls}?;"
                + "[{kata}{cjk-diacrit}]*;"
                + "[{hira}{cjk-diacrit}]*;"
                + "{kanji}*;"
            },

            { "LineBreakRules",
                "$dictionary=[\u0e01-\u0e2e\u0e30-\u0e3a\u0e40-\u0e44\u0e47-\u0e4e];" // this rule breaks the iterator with mixed Thai and English
                + "$ignore=[[[:Mn:][:Me:][:Cf:]]-[{$dictionary}]];"
                + "danda=[\u0964\u0965];"
                + "break=[\u0003\t\n\f\u2028\u2029];"
                + "nbsp=[\u00a0\u2007\u2011\ufeff];"
                + "space=[[[:Zs:][:Cc:]]-[{nbsp}{break}\r]];"
                + "dash=[[[:Pd:]\u00ad]-{nbsp}];"
                + "paiyannoi=[\u0e2f];"
                + "maiyamok=[\u0e46];"
                + "thai-etc=({paiyannoi}\u0e25{paiyannoi});"
                + "pre-word=[[[:Sc:]-[\u00a2]][:Ps:]\\\"];"
                + "post-word=[[:Pe:]\\!\\%\\.\\,\\:\\;\\?\\\"\u00a2\u00b0\u066a\u2030-\u2034\u2103"
                        + "\u2105\u2109\u3001\u3002\u3005\u3041\u3043\u3045\u3047\u3049\u3063"
                        + "\u3083\u3085\u3087\u308e\u3099-\u309e\u30a1\u30a3\u30a5\u30a7\u30a9"
                        + "\u30c3\u30e3\u30e5\u30e7\u30ee\u30f5\u30f6\u30fc-\u30fe\uff01\uff0e"
                        + "\uff1f{maiyamok}];"
                + "kanji=[[\u4e00-\u9fa5\uf900-\ufa2d\u3041-\u3094\u30a1-\u30fa]-[{post-word}{$ignore}]];"
                + "digit=[[:Nd:][:No:]];"
                + "mid-num=[\\.\\,];"
                + "char=[^{break}{space}{dash}{kanji}{nbsp}{$ignore}{pre-word}{post-word}"
                        + "{mid-num}\r{danda}{$dictionary}{paiyannoi}{maiyamok}];"
                + "number=([{pre-word}{dash}]*{digit}+({mid-num}{digit}+)*);"
                + "word-core=({char}*|{kanji}|{number}|{$dictionary}+|{thai-etc});"
                + "word-suffix=(({dash}+|{post-word}*){space}*);"
                + "word=({pre-word}*{word-core}{word-suffix});"
                + "{word}({nbsp}+{word})*(\r?{break}?|{paiyannoi}\r{break}|{paiyannoi}{break})?;"
                + "{word}({nbsp}+{word})*{paiyannoi}/([^[\u0e25{$ignore}]]|"
                        + "\u0e25[^{paiyannoi}{$ignore}]);"
            },

            { "WordBreakDictionary", url },
            { "LineBreakDictionary", url }
        };
    }
}
