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

public class ICUDiscouragedTaglet extends ICUTaglet {
    private static final String NAME = "discouraged";

    public ICUDiscouragedTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        String text = getText(tag, element);
        if (text.length() == 0) {
            System.err.println("Error: empty discouraged tag ");
        }
        return "<dt><span class=\"simpleTagLabel\" style=\"color:red\">Discouraged:</span></dt><dd>" + text + "</dd>";
    }
}
