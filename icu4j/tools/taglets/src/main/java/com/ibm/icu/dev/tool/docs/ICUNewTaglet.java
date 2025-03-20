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

/**
 * This taglet should be used in the first line of any icu-specific members in a class
 * that is an enhancement of a JDK class (see {@link ICUEnhancedTaglet}). It generates
 * the '[icu]' marker followed by the &lt;strong&gt; text, if any.  This does not
 * start or end a paragraph or provide additional leading or trailing punctuation such
 * as spaces or periods.
 *
 * <p>Note: if the text is '_usage_' (without quotes) this spits out a boilerplate
 * message describing the meaning of the '[icu]' tag.  This should be done in the
 * first paragraph of the class docs of any class containing '@icu' tags.
 */
public class ICUNewTaglet extends ICUTaglet {
    private static final String NAME = "icu";
    private static String ICU_LABEL = "<strong style=\"color:red\">[icu]</strong>";

    public ICUNewTaglet() {
        super(NAME, true);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        String text = getText(tag, element).trim();
        StringBuilder sb = new StringBuilder();
        if ("_usage_".equals(text)) {
            return sb.append(" Methods, fields, and other functionality specific to ICU ")
                    .append("are labeled '" + ICU_LABEL + "'.")
                    .toString();
        }

        sb.append("<strong style=\"color:red\">[icu]");
        if (text.length() > 0) {
            sb.append(" ").append(text);
        }
        sb.append("</strong>");
        return sb.toString();
    }
}
