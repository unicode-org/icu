/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/rbbi/BreakIteratorRules_en_US_TEST.java,v $ 
 * $Date: 2000/03/10 03:47:46 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.text.resources;

import java.util.ListResourceBundle;
import java.net.URL;

/**
 * This resource bundle is included for testing and demonstration purposes only.
 * It applies the dictionary-based algorithm to English text that has had all the
 * spaces removed.  Once we have good test cases for Thai, we will replace this
 * with good resource data (and a good dictionary file) for Thai
 */
public class BreakIteratorRules_en_US_TEST extends ListResourceBundle {
    public Object[][] getContents() {
        URL url = getClass().getResource("english.dict");

        // if dictionary wasn't found, then this resource bundle doesn't have
        // much to contribute...
        if (url == null) {
            return new Object[0][0];
        }
        return new Object[][]  {
        	// names of classes to instantiate for the different kinds of break
        	// iterator.  Notice we're now using DictionaryBasedBreakIterator
        	// for word and line breaking.
        	{ "BreakIteratorClasses",
            	new String[] { "RuleBasedBreakIterator",           // character-break iterator class
                        	"DictionaryBasedBreakIterator",     // word-break iterator class
                        	"DictionaryBasedBreakIterator",     // line-break iterator class
                        	"RuleBasedBreakIterator" }          // sentence-break iterator class
        	},
	        
        	// These are the same word-breaking rules as are specified in the default
        	// resource, except that the Latin letters, apostrophe, and hyphen are
        	// specified as dictionary characters
        	{ "WordBreakRules",
            	"$ignore=[[:Mn:][:Me:][:Cf:]];"
            	+ "dictionary=[a-zA-z\\'\\-];"
            	+ "kanji=[\u3005\u4e00-\u9fa5\uf900-\ufa2d];"
            	+ "kata=[\u30a1-\u30fa];"
            	+ "hira=[\u3041-\u3094];"
            	+ "cjk-diacrit=[\u3099-\u309c];"
            	+ "let=[[:L:]^[{kanji}{kata}{hira}{cjk-diacrit}{dictionary}]];"
            	+ "dgt=[[:N:]];"
            	+ "mid-word=[[:Pd:]\u00ad\u2027\\\"\\\'\\.];"
            	+ "mid-num=[\\\"\\\'\\,\u066b\\.];"
            	+ "pre-num=[[:Sc:]\\#\\.^\u00a2];"
            	+ "post-num=[\\%\\&\u00a2\u066a\u2030\u2031];"
            	+ "ls=[\n\u000c\u2028\u2029];"
            	+ "ws=[[:Zs:]\t];"
            	+ "word=({let}{let}*({mid-word}{let}{let}*)*|[a-zA-Z][a-z\\'\\-]*);"
            	+ "number=({dgt}{dgt}*({mid-num}{dgt}{dgt}*)*);"
            	+ ".;"
            	+ "{{word}}({number}{word})*{{number}{{post-num}}};"
            	+ "{pre-num}({number}{word})*{{number}{{post-num}}};"
            	+ "{ws}*{\r}{{ls}};"
            	+ "[{kata}{cjk-diacrit}]*;"
            	+ "[{hira}{cjk-diacrit}]*;"
            	+ "{kanji}*;" },
	        
        	// These are the same line-breaking rules as are specified in the default
        	// resource, except that the Latin letters, apostrophe, and hyphen are
        	// specified as dictionary characters
        	{ "LineBreakRules",
            	"<ignore>=[:Mn::Me::Cf:];"
            	+ "<dictionary>=[a-zA-z\\'\\-];"
            	+ "<break>=[\u0003\t\n\f\u2028\u2029];"
            	+ "<nbsp>=[\u00a0\u2007\u2011\ufeff];"
            	+ "<space>=[:Zs::Cc:^[<nbsp><break>\r]];"
            	+ "<dash>=[:Pd:\u00ad^<nbsp>];"
            	+ "<pre-word>=[:Sc::Ps:^\u00a2];"
            	+ "<post-word>=[:Pe:\\!\\%\\.\\,\\:\\;\\?\u00a2\u00b0\u066a\u2030-\u2034\u2103"
                    	+ "\u2105\u2109\u3001\u3002\u3005\u3041\u3043\u3045\u3047\u3049\u3063"
                    	+ "\u3083\u3085\u3087\u308e\u3099-\u309e\u30a1\u30a3\u30a5\u30a7\u30a9"
                    	+ "\u30c3\u30e3\u30e5\u30e7\u30ee\u30f5\u30f6\u30fc-\u30fe\uff01\uff0e"
                    	+ "\uff1f];"
            	+ "<kanji>=[\u4e00-\u9fa5\uf900-\ufa2d\u3041-\u3094\u30a1-\u30fa^[<post-word><ignore>]];"
            	+ "<digit>=[:Nd::No:];"
            	+ "<mid-num>=[\\.\\,];"
            	+ "<char>=[^[<break><space><dash><kanji><nbsp><ignore><dictionary>\r]];"
            	+ "<number>=([<pre-word><dash>]*<digit><digit>*(<mid-num><digit><digit>*)*);"
            	+ "<word-core>=(<char>*|<kanji>|<number>|[a-zA-Z][a-z\\'\\-]*);"
            	+ "<word-suffix>=((<dash><dash>*|<post-word>*)<space>*);"
            	+ "<word>=(<pre-word>*<word-core><word-suffix>);"
            	+ "<word>(<nbsp><nbsp>*<word>)*{\r}{<break>};" },
	            
        	// these two resources specify the pathnames of the dictionary files to
        	// use for word breaking and line breaking.  Both currently refer to 
        	// a file called english.dict placed in com\ibm\text\resources
        	// somewhere in the class path.  It's important to note that
        	// english.dict was created for testing purposes only, and doesn't
        	// come anywhere close to being an exhaustive dictionary of English
        	// words (basically, it contains all the words in the Declaration of
        	// Independence, and the Revised Standard Version of the book of Genesis,
        	// plus a few other words thrown in to show more interesting cases).
	//        { "WordBreakDictionary", "com\\ibm\\text\\resources\\english.dict" },
	//       { "LineBreakDictionary", "com\\ibm\\text\\resources\\english.dict" }
        	{ "WordBreakDictionary", url },
        	{ "LineBreakDictionary", url }
    	};
    }
}
