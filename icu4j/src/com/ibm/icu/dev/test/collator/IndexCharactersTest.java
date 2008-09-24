//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.collator;

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
}
//#endif
