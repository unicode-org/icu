// © 2025 and later: Unicode, Inc. and others.

package com.ibm.icu.dev.tool.errorprone;

import java.io.PrintStream;
import java.util.Map;

class HtmlUtils {
    private static final int NOTHING          = 0b0000_0000;
    private static final int STANDALONE       = 0b0000_0001;
    private static final int INDENT_KIDS      = 0b0000_0010;
    private static final int NL_BEFORE_START  = 0b0000_0100;
    private static final int NL_AFTER_START   = 0b0000_1000;
    private static final int NL_BEFORE_END    = 0b0001_0000;
    private static final int NL_AFTER_END     = 0b0010_0000;
    private static final int NLNL_AFTER_END   = 0b0100_0000;
    private static final int NLNL_AFTER_START = 0b1000_0000;
    private static final int INLINE = 0b1_0000_0000;

    private static final Map<String, Integer> HTML_TAGS = Map.ofEntries(
            Map.entry("html", NL_AFTER_START | NL_AFTER_END),
            Map.entry("head", NL_AFTER_START | NL_AFTER_END),
            Map.entry("meta", NL_AFTER_START | NL_AFTER_END),
            Map.entry("script", NL_AFTER_END),
            Map.entry("style", NL_AFTER_START | NL_AFTER_END),
            Map.entry("link", NL_AFTER_START | NL_AFTER_END),
            Map.entry("body", NLNL_AFTER_START | NL_BEFORE_END | NL_AFTER_END),
            Map.entry("h1", NLNL_AFTER_END),
            Map.entry("h2", NLNL_AFTER_END),
            Map.entry("h3", NLNL_AFTER_END),
            Map.entry("h4", NLNL_AFTER_END),
            Map.entry("h5", NLNL_AFTER_END),
            Map.entry("h6", NLNL_AFTER_END),
            Map.entry("div", NL_AFTER_START | NLNL_AFTER_END),
            Map.entry("hr", NL_BEFORE_START | NL_AFTER_START | STANDALONE),
            Map.entry("br", NL_AFTER_START | STANDALONE | INLINE),
            Map.entry("p", NL_AFTER_END),
            Map.entry("table", NL_AFTER_START | NL_AFTER_END),
            Map.entry("thead", NL_AFTER_START | NL_AFTER_END),
            Map.entry("tr", NL_AFTER_START | NL_AFTER_END | INDENT_KIDS),
            Map.entry("td", NL_AFTER_END),
            Map.entry("th", NL_AFTER_END),
            Map.entry("code", INLINE),
            Map.entry("a", INLINE),
            Map.entry("span", INLINE)
    );

    private static final String INDENT_SPACES = "  ";

    private static PrintStream wrt;

    int indent = 0;

    HtmlUtils(PrintStream wrt) {
        this.wrt = wrt;
    }

    HtmlUtils indent() {
        for (int i = 0; i < indent; i++) {
            wrt.print(INDENT_SPACES);
        }
        return this;
    }

    static int getFlags(String tag) {
        Integer flags = HTML_TAGS.get(tag);
        if (flags == null) {
            System.out.println("Unknown tag '" + tag + "'");
        }
        return flags == null ? NOTHING : flags;
    }

    static boolean isSet(int flag, int bit) {
        return (flag & bit) == bit;
    }

    HtmlUtils openTag(String tag, Map<String, String> attributes) {
        int flags = getFlags(tag);

        if (isSet(flags, NL_BEFORE_START)) {
            wrt.print("\n");
        }

        if (!isSet(flags, INLINE)) {
            indent();
        }
        wrt.print("<" + tag);

        if (attributes != null) {
            for (Map.Entry<String, String> attr : attributes.entrySet()) {
                wrt.print(" " + attr.getKey() + "=\"" + escAttr(attr.getValue()) + "\"");
            }
        }

        if (isSet(flags, STANDALONE)) {
            wrt.print("/>");
        } else {
            wrt.print(">");
        }

        if (isSet(flags, NL_AFTER_START)) {
            wrt.print("\n");
        }
        if (isSet(flags, NLNL_AFTER_START)) {
            wrt.print("\n\n");
        }

        if (isSet(flags, INDENT_KIDS)) {
            indent++;
        }
        return this;
    }

    HtmlUtils openTag(String tag) {
        return openTag(tag, null);
    }

    HtmlUtils closeTag(String tag) {
        int flags = getFlags(tag);
        if (isSet(flags, INDENT_KIDS)) {
            indent--;
        }
        if (isSet(flags, NL_BEFORE_END)) {
            wrt.print("\n");
            indent();
        }
        wrt.print("</" + tag + ">");
        if (isSet(flags, NL_AFTER_END)) {
            wrt.print("\n");
        }
        if (isSet(flags, NLNL_AFTER_END)) {
            wrt.print("\n\n");
        }

        return this;
    }

    HtmlUtils text(String text, boolean escape) {
        if (escape) {
            wrt.print(escText(text));
        } else {
            wrt.print(text);
        }
        return this;
    }

    HtmlUtils text(String text) {
        return text(text, true);
    }

    HtmlUtils nl() {
        wrt.println();
        return this;
    }

    private static String escAttr(String text) {
        return text.replace("\"", "&quot;");
    }

    private static String escText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
