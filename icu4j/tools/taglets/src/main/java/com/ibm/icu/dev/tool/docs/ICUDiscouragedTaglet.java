package com.ibm.icu.dev.tool.docs;

import java.util.Map;

import com.sun.javadoc.Tag;

public class ICUDiscouragedTaglet extends ICUTaglet {
    private static final String NAME = "discouraged";

    public static void register(Map taglets) {
        taglets.put(NAME, new ICUDiscouragedTaglet());
    }

    private ICUDiscouragedTaglet() {
        super(NAME, MASK_DEFAULT);
    }

    public String toString(Tag tag) {
        String text = tag.text();
        if (text.length() == 0) {
            System.err.println("Error: empty discouraged tag ");
        } 
        return "<dt><b><font color=red>Discouraged:</font></b></dt><dd>" + text + "</dd>";
    }
}