/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.collator;

import java.util.Collection;
import java.util.List;

import com.ibm.icu.dev.test.TestFmwk;
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
        for (int i = 0; i < list.length; ++i) {
            ULocale locale = list[i];
            if (locale.getCountry().length() != 0) {
                continue;
            }
            IndexCharacters indexCharacters = new IndexCharacters(locale);
            final Collection mainChars = indexCharacters.getIndexCharacters();
            String mainCharString = mainChars.toString();
            if (mainCharString.length() > 500) {
                mainCharString = mainCharString.substring(0,500) + "...";
            }
            logln(mainChars.size() + "\t" + locale + "\t" + locale.getDisplayName(ULocale.ENGLISH) + "\t" 
                    + mainCharString);
            assertTrue("Index character set too large", mainChars.size() <= 100);
            showIfNotEmpty("AlreadyIn", indexCharacters.getAlreadyIn());
            showIfNotEmpty("Sorts Same as Components", indexCharacters.getNoDistinctSorting());
            showIfNotEmpty("Nonalphabetic", indexCharacters.getNotAlphabetic());
        }
    }
    private void showIfNotEmpty(String title, List alreadyIn) {
        if (alreadyIn.size() != 0) {
            logln("\t" + title + ":\t" + alreadyIn);
        }
    }
}
