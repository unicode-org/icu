// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 * ****************************************************************************** Copyright (C)
 * 2002-2016 International Business Machines Corporation * and others. All Rights Reserved. *
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.docs;

import com.sun.source.doctree.DocTree;
import javax.lang.model.element.Element;

public class ICUStableTaglet extends ICUTaglet {
    private static final String NAME = "stable";

    public ICUStableTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        String text = getText(tag, element);
        if (text.length() > 0) {
            return STATUS + "<dd>Stable " + text + ".</dd>";
        } else {
            return STATUS + "<dd>Stable.</dd>";
        }
    }
}
