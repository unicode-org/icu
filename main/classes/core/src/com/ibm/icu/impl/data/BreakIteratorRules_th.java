/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

import com.ibm.icu.impl.ICUData;

public class BreakIteratorRules_th extends ListResourceBundle {
    private static final String DATA_NAME = "data/th.brk";

    public Object[][] getContents() {
        final boolean exists = ICUData.exists(DATA_NAME);

        // if dictionary wasn't found, then this resource bundle doesn't have
        // much to contribute...
        if (!exists) {
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


            { "WordBreakDictionary", DATA_NAME }, // now a path to ICU4J-specific resource
            { "LineBreakDictionary", DATA_NAME }
        };
    }
}
