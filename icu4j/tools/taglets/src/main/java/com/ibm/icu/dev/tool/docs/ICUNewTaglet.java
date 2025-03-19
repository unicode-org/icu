package com.ibm.icu.dev.tool.docs;

import java.util.Map;

import com.sun.javadoc.Tag;

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

    public static void register(Map taglets) {
        taglets.put(NAME, new ICUNewTaglet());
    }

    private ICUNewTaglet() {
        super(NAME, MASK_DEFAULT_INLINE);
    }

    public String toString(Tag tag) {
        String text = tag.text().trim();
        StringBuilder sb = new StringBuilder();
        if ("_usage_".equals(text)) {
            return sb.append(" Methods, fields, and other functionality specific to ICU ")
                .append("are labeled '" + ICU_LABEL + "'.</p>")
                .toString();
        }

        sb.append("<strong><font color=red>[icu]</font>");
        if (text.length() > 0) {
            sb.append(" ").append(text);
        }
        sb.append("</strong>");
        return sb.toString();
    }
}