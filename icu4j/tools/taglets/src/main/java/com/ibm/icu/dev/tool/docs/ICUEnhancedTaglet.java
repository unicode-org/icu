package com.ibm.icu.dev.tool.docs;

import java.util.Map;

import com.sun.javadoc.Tag;

/**
 * This taglet should be used in the first line of the class description of classes
 * that are enhancements of JDK classes that similar names and APIs.  The text should
 * provide the full package and name of the JDK class.  A period should follow the
 * tag.  This puts an 'icu enhancement' message into the first line of the class docs,
 * where it will also appear in the class summary.
 *
 * <p>Following this tag (and period), ideally in the first paragraph, the '@icu' tag
 * should be used with the text '_label_' to generate the standard boilerplate about
 * how that tag is used in the class docs.  See {@link ICUNewTaglet}.
 *
 * <p>This cumbersome process is necessary because the javadoc code that handles
 * taglets doesn't look at punctuation in the substitution text to determine when to
 * end the first line, it looks in the original javadoc comment.  So we need a tag to
 * identify the related java class, then a period, then another tag.
 */
 public class ICUEnhancedTaglet extends ICUTaglet {
    private static final String NAME = "icuenhanced";

    public static void register(Map taglets) {
        taglets.put(NAME, new ICUEnhancedTaglet());
    }

    private ICUEnhancedTaglet() {
        super(NAME, MASK_DEFAULT_INLINE);
    }

    public String toString(Tag tag) {
        String text = tag.text().trim();

        boolean isClassDoc = tag.holder().isClass() || tag.holder().isInterface();
        if (isClassDoc && text.length() > 0) {
            StringBuilder sb = new StringBuilder();
            return sb.append("<strong><font color=red>[icu enhancement]</font></strong> ")
                .append("ICU's replacement for <code>")
                .append(text)
                .append("</code>")
                .toString();
        }
        return "";
    }
}