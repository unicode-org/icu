package com.ibm.icu.dev.tool.docs;

import java.util.Map;

import com.sun.javadoc.Tag;

public class ICUIgnoreTaglet extends ICUTaglet {
    private static ICUTaglet singleton;

    public static void register(Map taglets) {
        if (singleton == null) {
            singleton = new ICUIgnoreTaglet();
        }
        taglets.put("bug", singleton);
        taglets.put("test", singleton);
        taglets.put("summary", singleton);
    }

    private ICUIgnoreTaglet() {
        super(".ignore", MASK_DEFAULT);
    }

    public String toString(Tag tag) {
        return null;
    }
}