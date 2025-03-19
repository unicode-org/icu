package com.ibm.icu.dev.tool.docs;

import java.util.Map;

import com.sun.javadoc.Tag;

public class ICUStableTaglet extends ICUTaglet {
    private static final String NAME = "stable";

    public static void register(Map taglets) {
        taglets.put(NAME, new ICUStableTaglet());
    }

    private ICUStableTaglet() {
        super(NAME, MASK_DEFAULT);
    }

    public String toString(Tag tag) {
        String text = tag.text();
        if (text.length() > 0) {
            return STATUS + "<dd>Stable " + text + ".</dd>";
        } else {
            return STATUS + "<dd>Stable.</dd>";
        }
    }
}