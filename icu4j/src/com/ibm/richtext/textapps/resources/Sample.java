/**
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.richtext.textapps.resources;

import java.util.ListResourceBundle;

public final class Sample extends ListResourceBundle {
    
    public Object[][] getContents() {

        /*Object sample =*/ this.getClass().getResource("hagan");

        return new Object[][] {
            { "default.sample", this.getClass().getResource("unicode.hebrew.red") },
            { "default.name", "What is Unicode - Hebrew" },
            { "arabic.sample", this.getClass().getResource("unicode.arabic.red") },
            { "arabic.name", "What is Unicode - Arabic" },
            { "japanese.sample", "\u6ce8: {1} \u306e\u30e1\u30bd\u30c3\u30c9 {0} \u306f\u63a8\u5968\u3055\u308c\u307e\u305b\u3093\u3002" },
            { "japanese.name", "Japanese Message" },
        };
    }
}
