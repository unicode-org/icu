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

/**
 * This taglet should be used in class or member documentation, after the first line, where the
 * behavior of the ICU method or class has notable differences from its JDK counterpart. It starts a
 * new paragraph and generates an '[icu] Note:' header.
 */
public class ICUNoteTaglet extends ICUTaglet {
    private static final String NAME = "icunote";

    public ICUNoteTaglet() {
        super(NAME, true);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        return "<p><strong style=\"color:red\">[icu] Note:</strong> ";
    }
}
