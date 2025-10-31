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
 * This taglet should be used in the first line of the class description of classes that are
 * enhancements of JDK classes that similar names and APIs. The text should provide the full package
 * and name of the JDK class. A period should follow the tag. This puts an 'icu enhancement' message
 * into the first line of the class docs, where it will also appear in the class summary.
 *
 * <p>Following this tag (and period), ideally in the first paragraph, the '@icu' tag should be used
 * with the text '_label_' to generate the standard boilerplate about how that tag is used in the
 * class docs. See {@link ICUNewTaglet}.
 *
 * <p>This cumbersome process is necessary because the javadoc code that handles taglets doesn't
 * look at punctuation in the substitution text to determine when to end the first line, it looks in
 * the original javadoc comment. So we need a tag to identify the related java class, then a period,
 * then another tag.
 */
public class ICUEnhancedTaglet extends ICUTaglet {
    private static final String NAME = "icuenhanced";

    public ICUEnhancedTaglet() {
        super(NAME, true);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        String text = getText(tag, element).trim();

        boolean isClassDoc = element.getKind().isClass() || element.getKind().isInterface();
        if (isClassDoc && text.length() > 0) {
            StringBuilder sb = new StringBuilder();
            return sb.append("<strong style=\"color:red\">[icu enhancement]</strong> ")
                    .append("ICU's replacement for <code>")
                    .append(text)
                    .append("</code>")
                    .toString();
        }
        return "";
    }
}
