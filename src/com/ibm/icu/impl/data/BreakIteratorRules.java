/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Default break-iterator rules.  
 * This resource tells which break iterator class type is instantiated
 *  for each of the standard (built-in) boundary types.
 *
 *  Locales (Thai) needing a dictionary based iterator override this.
 */

public class BreakIteratorRules extends ListResourceBundle {
    public Object[][] getContents() {
        return contents;
    }

    static final Object[][] contents = {
        // BreakIteratorClasses lists the class names to instantiate for each
        // built-in type of BreakIterator
        { "BreakIteratorClasses",
            new String[] { "RuleBasedBreakIterator",     // character-break iterator class
                           "RuleBasedBreakIterator",     // word-break iterator class
                           "RuleBasedBreakIterator",     // line-break iterator class
                           "RuleBasedBreakIterator",     // sentence-break iterator class
                           "RuleBasedBreakIterator"}     // Title-Case break iterator class
        }

    };
}
