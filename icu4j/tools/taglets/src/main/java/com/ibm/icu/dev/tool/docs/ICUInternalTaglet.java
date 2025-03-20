// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2002-2016 International Business Machines Corporation         *
 * and others. All Rights Reserved.                                            *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.docs;

import java.util.Locale;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

public class ICUInternalTaglet extends ICUTaglet {
    private static final String NAME = "internal";

    public ICUInternalTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        String text = getText(tag, element);
        if (text.toLowerCase(Locale.US).indexOf("technology preview") >= 0) {
            return STATUS + "<dd><em>Technology Preview</em>. <font color='red'>" +
                    "This API is still in the early stages of development. Use at your own risk.</font></dd>";
        }
        return STATUS + "<dd><em>Internal</em>. <font color='red'>" +
                "This API is <em>ICU internal only</em>.</font></dd>";
    }
}
