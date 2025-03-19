package com.ibm.icu.dev.tool.docs;

import java.util.Locale;
import java.util.Map;

import com.sun.javadoc.Tag;

public class ICUInternalTaglet extends ICUTaglet {
    private static final String NAME = "internal";

    public static void register(Map taglets) {
        taglets.put(NAME, new ICUInternalTaglet());
    }

    private ICUInternalTaglet() {
        super(NAME, MASK_DEFAULT);
    }

    public String toString(Tag tag) {
        if (tag.text().toLowerCase(Locale.US).indexOf("technology preview") >= 0) {
            return STATUS + "<dd><em>Technology Preview</em>. <font color='red'>" +
                "This API is still in the early stages of development. Use at your own risk.</font></dd>";
        }
        return STATUS + "<dd><em>Internal</em>. <font color='red'>" +
            "This API is <em>ICU internal only</em>.</font></dd>";
    }
}