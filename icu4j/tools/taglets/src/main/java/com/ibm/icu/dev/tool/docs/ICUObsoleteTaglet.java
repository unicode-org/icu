// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 * ****************************************************************************** Copyright (C)
 * 2002-2016 International Business Machines Corporation * and others. All Rights Reserved. *
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.docs;

import com.sun.source.doctree.DocTree;
import java.text.BreakIterator;
import java.util.Locale;
import javax.lang.model.element.Element;

public class ICUObsoleteTaglet extends ICUTaglet {
    private static final String NAME = "obsolete";

    public ICUObsoleteTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        String text = getText(tag, element);
        BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
        bi.setText(text);
        int first = bi.first();
        int next = bi.next();
        if (text.length() == 0) {
            first = next = 0;
        }
        return STATUS
                + "<dd><em>Obsolete.</em> <font color='red'>Will be removed in "
                + text.substring(first, next)
                + "</font>. "
                + text.substring(next)
                + "</dd>";
    }
}
