package com.ibm.icu.dev.tool.docs;

import java.util.Map;

import com.sun.javadoc.Tag;

/**
 * This taglet should be used in class or member documentation, after the first line,
 * where the behavior of the ICU method or class has notable differences from its JDK
 * counterpart. It starts a new paragraph and generates an '[icu] Note:' header.
 */
public class ICUNoteTaglet extends ICUTaglet {
    private static final String NAME = "icunote";

    public static void register(Map taglets) {
        taglets.put(NAME, new ICUNoteTaglet());
    }

    private ICUNoteTaglet() {
        super(NAME, MASK_DEFAULT_INLINE);
    }

    public String toString(Tag tag) {
        return "<p><strong><font color=red>[icu]</font> Note:</strong> ";
    }
}