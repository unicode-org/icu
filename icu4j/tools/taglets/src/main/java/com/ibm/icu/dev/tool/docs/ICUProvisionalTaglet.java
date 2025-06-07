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

public class ICUProvisionalTaglet extends ICUTaglet {
    private static final String NAME = "provisional";

    public ICUProvisionalTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        return null;
    }
}
