package com.ibm.icu.dev.tool.docs;

import java.util.Map;

import com.sun.javadoc.Tag;

public class ICUDraftTaglet extends ICUTaglet {
    private static final String NAME = "draft";

    public static void register(Map taglets) {
        taglets.put(NAME, new ICUDraftTaglet());
    }

    private ICUDraftTaglet() {
        super(NAME, MASK_DEFAULT);
    }

    public String toString(Tag tag) {
        String text = tag.text();
        if (text.length() == 0) {
            System.err.println("Warning: empty draft tag");
        }
        return STATUS + "<dd>Draft " + tag.text() + ".</dd>";
    }
}