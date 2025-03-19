package com.ibm.icu.dev.tool.docs;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.Map;

import com.sun.javadoc.Tag;

public class ICUObsoleteTaglet extends ICUTaglet {
    private static final String NAME = "obsolete";

    public static void register(Map taglets) {
        taglets.put(NAME, new ICUObsoleteTaglet());
    }

    private ICUObsoleteTaglet() {
        super(NAME, MASK_DEFAULT);
    }

    public String toString(Tag tag) {
        BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
        String text = tag.text();
        bi.setText(text);
        int first = bi.first();
        int next = bi.next();
        if (text.length() == 0) {
            first = next = 0;
        }
        return STATUS + "<dd><em>Obsolete.</em> <font color='red'>Will be removed in " +
            text.substring(first, next) + "</font>. " + text.substring(next) + "</dd>";

    }
}