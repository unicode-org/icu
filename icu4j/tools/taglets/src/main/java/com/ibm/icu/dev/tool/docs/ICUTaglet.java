// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 2002-2016 International Business Machines Corporation   *
* and others. All Rights Reserved.                                            *
*******************************************************************************
*/

package com.ibm.icu.dev.tool.docs;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.internal.toolkit.taglets.Taglet;

public abstract class ICUTaglet extends ICUTagletAdapter implements Taglet {
    protected final String name;
    protected final int mask;

    protected static final int MASK_FIELD = 1;
    protected static final int MASK_CONSTRUCTOR = 2;
    protected static final int MASK_METHOD = 4;
    protected static final int MASK_OVERVIEW = 8;
    protected static final int MASK_PACKAGE = 16;
    protected static final int MASK_TYPE = 32;
    protected static final int MASK_INLINE = 64;

    protected static final int MASK_DEFAULT = 0x003f; // no inline
    protected static final int MASK_DEFAULT_INLINE = 0x007f; // includes inline
    protected static final int MASK_VALID = 0x007f;

    public static void register(Map taglets) {
        ICUInternalTaglet.register(taglets);
        ICUDraftTaglet.register(taglets);
        ICUStableTaglet.register(taglets);
        ICUProvisionalTaglet.register(taglets);
        ICUObsoleteTaglet.register(taglets);
        ICUIgnoreTaglet.register(taglets);
        ICUNewTaglet.register(taglets);
        ICUNoteTaglet.register(taglets);
        ICUEnhancedTaglet.register(taglets);
        ICUDiscouragedTaglet.register(taglets);
    }

    protected ICUTaglet(String name, int mask) {
        this.name = name;
        this.mask = mask & MASK_VALID;
    }

    public boolean inField() {
        return (mask & MASK_FIELD) != 0;
    }

    public boolean inConstructor() {
        return (mask & MASK_CONSTRUCTOR) != 0;
    }

    public boolean inMethod() {
        return (mask & MASK_METHOD) != 0;
    }

    public boolean inOverview() {
        return (mask & MASK_OVERVIEW) != 0;
    }

    public boolean inPackage() {
        return (mask & MASK_PACKAGE) != 0;
    }

    public boolean inType() {
        return (mask & MASK_TYPE) != 0;
    }

    public boolean isInlineTag() {
        return (mask & MASK_INLINE) != 0;
    }

    public String getName() {
        return name;
    }

    public String toString(Tag tag) {
        return tag.text();
    }

    public String toString(Tag[] tags) {
      
      if (!isInlineTag() && tags != null) {
            if (tags.length > 1) {
                String msg = "Should not have more than one ICU tag per element:\n";
                for (int i = 0; i < tags.length; ++i) {
                    msg += "  [" + i + "] " + tags[i] + "\n";
                }
                throw new IllegalStateException(msg);
            } else if (tags.length > 0) {
                return toString(tags[0]);
            }
        }
        return null;
    }

    protected static final String STATUS = "<dt><b>Status:</b></dt>";

    public static class ICUDiscouragedTaglet extends ICUTaglet {
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

    public static class ICUInternalTaglet extends ICUTaglet {
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

    public static class ICUDraftTaglet extends ICUTaglet {
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

    public static class ICUStableTaglet extends ICUTaglet {
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

    public static class ICUProvisionalTaglet extends ICUTaglet {
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

    public static class ICUObsoleteTaglet extends ICUTaglet {
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

    public static class ICUIgnoreTaglet extends ICUTaglet {
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

    private static String ICU_LABEL = "<strong><font color=red>[icu]</font></strong>";

    /**
     * This taglet should be used in the first line of the class description of classes
     * that are enhancements of JDK classes that similar names and APIs.  The text should
     * provide the full package and name of the JDK class.  A period should follow the
     * tag.  This puts an 'icu enhancement' message into the first line of the class docs,
     * where it will also appear in the class summary.
     *
     * <p>Following this tag (and period), ideally in the first paragraph, the '@icu' tag
     * should be used with the text '_label_' to generate the standard boilerplate about
     * how that tag is used in the class docs.  See {@link ICUNewTaglet}.
     *
     * <p>This cumbersome process is necessary because the javadoc code that handles
     * taglets doesn't look at punctuation in the substitution text to determine when to
     * end the first line, it looks in the original javadoc comment.  So we need a tag to
     * identify the related java class, then a period, then another tag.
     */
     public static class ICUEnhancedTaglet extends ICUTaglet {
        private static final String NAME = "icuenhanced";

        public static void register(Map taglets) {
            taglets.put(NAME, new ICUEnhancedTaglet());
        }

        private ICUEnhancedTaglet() {
            super(NAME, MASK_DEFAULT_INLINE);
        }

        public String toString(Tag tag) {
            String text = tag.text().trim();

            boolean isClassDoc = tag.holder().isClass() || tag.holder().isInterface();
            if (isClassDoc && text.length() > 0) {
                StringBuilder sb = new StringBuilder();
                return sb.append("<strong><font color=red>[icu enhancement]</font></strong> ")
                    .append("ICU's replacement for <code>")
                    .append(text)
                    .append("</code>")
                    .toString();
            }
            return "";
        }
    }

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
    public static class ICUNewTaglet extends ICUTaglet {
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

    /**
     * This taglet should be used in class or member documentation, after the first line,
     * where the behavior of the ICU method or class has notable differences from its JDK
     * counterpart. It starts a new paragraph and generates an '[icu] Note:' header.
     */
    public static class ICUNoteTaglet extends ICUTaglet {
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
}
