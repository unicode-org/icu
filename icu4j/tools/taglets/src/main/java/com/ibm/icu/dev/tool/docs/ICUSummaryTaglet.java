// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.tool.docs;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

public class ICUSummaryTaglet extends ICUTaglet {
    private static ICUTaglet singleton;
    private static final String NAME = "summary";

    public ICUSummaryTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        return null;
    }
}
