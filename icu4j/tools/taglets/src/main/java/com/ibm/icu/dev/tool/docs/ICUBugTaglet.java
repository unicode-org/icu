// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.tool.docs;

import com.sun.source.doctree.DocTree;
import javax.lang.model.element.Element;

public class ICUBugTaglet extends ICUTaglet {
    private static ICUTaglet singleton;
    private static final String NAME = "bug";

    public ICUBugTaglet() {
        super(NAME, false);
    }

    @Override
    public String toStringDocTree(DocTree tag, Element element) {
        return null;
    }
}
