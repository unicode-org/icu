// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2001-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.data;
import java.util.ListResourceBundle;

public class TestDataElements_testtypes extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public final Object[][] getContents() {
          return  contents;
    }

    private static Object[][] contents = {
                {
                    "binarytest",
                    new byte[] {
                        0,   1,   2,   3,   4,   5,   6,   7,   8,   9,   10,  11,  12,  13,  14,
                    },
                },
                {
                    "emptyarray",
                    new String[] { 
                    },
                },
                {
                    "emptybin",
                    new byte[] {},
                },
                {
                    "emptyexplicitstring",
                    "",
                },
                {
                    "emptyint",
                    0,
                },
                {
                    "emptyintv",
                    new Integer[] {
                    },
                },
                {
                    "emptystring",
                    "",
                },
                {
                    "emptytable",
                    new Object[][]{
                    },
                },
                {
                    "importtest",
                    new byte[] {
                        0,   1,   2,   3,   4,   5,   6,   7,   8,   9,   10,  11,  12,  13,  14,
                    },
                },
                {
                    "integerarray",
                    new Integer[] {
                        1,
                        2,
                        3,
                        -3,
                        4,
                        5,
                        6,
                        7,
                    },
                },
                {
                    "menu",
                    new Object[][]{
                        {
                            "file",
                            new Object[][]{
                                {
                                    "exit",
                                    "Exit",
                                },
                                {
                                    "open",
                                    "Open",
                                },
                                {
                                    "save",
                                    "Save",
                                },
                            },
                        },
                    },
                },
                {
                    "minusone",
                    -1,
                },
                {
                    "one",
                    1,
                },
                {
                    "onehundredtwentythree",
                    123,
                },
                {
                    "plusone",
                    1,
                },
                {
                    "string",
                    new String[] { 
                    },
                },
                {
                    "stringTable",
                    new Object[]{
                        new String[] { 
                        },

                    },
                },
                {
                    "test_underscores",
                    "test message ....",
                },
                {
                    "testescape",
                    "tab:\u0009 cr:\f ff:\u000C newline:\n backslash:\\" +
                    " quote=\\\' doubleQuote=\\\" singlequoutes=''",
                },
                {
                    "zerotest",
                    "abc\u0000def",
                },
    };
}
