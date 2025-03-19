package com.ibm.icu.dev.tool.docs;

import java.util.Map;

import com.sun.javadoc.Tag;

public class ICUProvisionalTaglet extends ICUTaglet {
    private static final String NAME = "provisional";

    public static void register(Map taglets) {
        taglets.remove(NAME); // override standard deprecated taglet
        taglets.put(NAME, new ICUProvisionalTaglet());
    }

    private ICUProvisionalTaglet() {
        super(NAME, MASK_DEFAULT);
    }

    public String toString(Tag tag) {
        return null;
    }
}