// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2002-2016 International Business Machines Corporation         *
 * and others. All Rights Reserved.                                            *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.docs;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

public class ICUDraftTaglet extends ICUTaglet {
    private static final String NAME = "draft";

    public ICUDraftTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        String text = getText(tag, element);
        if (text.length() == 0) {
            System.err.println("Warning: empty draft tag");
        }
        return STATUS + "<dd>Draft " + text + ".</dd>";
    }
}
