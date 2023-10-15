// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 1996-2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.dev.test.lang;

import com.ibm.icu.dev.test.CoreTestFmwk;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacterCategory;

/**
* Testing UCharacterCategory
* @author Syn Wee Quek
* @since April 02 2002
*/
@RunWith(JUnit4.class)
public class UCharacterCategoryTest extends CoreTestFmwk
{
    // constructor -----------------------------------------------------------

    /**
    * Private constructor to prevent initialization
    */
    public UCharacterCategoryTest()
    {
    }

      // public methods --------------------------------------------------------

    /**
    * Gets the name of the argument category
    * @returns category name
    */
    @Test
    public void TestToString()
    {
          String name[] = {"Unassigned",
                           "Letter, Uppercase",
                           "Letter, Lowercase",
                           "Letter, Titlecase",
                           "Letter, Modifier",
                           "Letter, Other",
                           "Mark, Non-Spacing",
                           "Mark, Enclosing",
                           "Mark, Spacing Combining",
                           "Number, Decimal Digit",
                           "Number, Letter",
                           "Number, Other",
                           "Separator, Space",
                           "Separator, Line",
                           "Separator, Paragraph",
                           "Other, Control",
                           "Other, Format",
                           "Other, Private Use",
                           "Other, Surrogate",
                           "Punctuation, Dash",
                           "Punctuation, Open",
                           "Punctuation, Close",
                           "Punctuation, Connector",
                           "Punctuation, Other",
                           "Symbol, Math",
                           "Symbol, Currency",
                           "Symbol, Modifier",
                           "Symbol, Other",
                           "Punctuation, Initial quote",
                           "Punctuation, Final quote"};
        for (int i = UCharacterCategory.UNASSIGNED;
                 i < UCharacterCategory.CHAR_CATEGORY_COUNT; i ++) {
             if (!UCharacterCategory.toString(i).equals(name[i])) {
                 errln("Error toString for category " + i + " expected " +
                       name[i]);
             }
        }
    }
}
