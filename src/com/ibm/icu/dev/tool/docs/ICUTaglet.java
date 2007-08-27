//##header J2SE15
/**
*******************************************************************************
* Copyright (C) 2002-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

/**
 * Preprocess with com.ibm.icu.dev.tool.docs.CodeMangler to generate
 * either a JDK 1.4 or JDK 1.5 version.  For the 1.5 version, define
 * VERSION_1.5.
 */

package com.ibm.icu.dev.tool.docs;

import com.sun.javadoc.*;
//#if defined(J2SE13) || defined(J2SE14)
//##import com.sun.tools.doclets.*;
//#else
// jdk 1.5 contains both com.sun.tools.doclets.Taglet and
// com.sun.tools.doclets.internal.toolkit.taglets.Taglet.
// Their registration code casts to the second, not the first, and the
// second doesn't implement the first, so if you just implement the
// first, you die.
import com.sun.tools.doclets.internal.toolkit.taglets.*;
import com.sun.tools.doclets.internal.toolkit.taglets.Taglet;
//#endif

import java.text.BreakIterator;
import java.util.Locale;
import java.util.Map;

public abstract class ICUTaglet implements Taglet {
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
    protected static final int MASK_VALID = 0x007f; // includes inline

    public static void register(Map taglets) {
        ICUInternalTaglet.register(taglets);
        ICUDraftTaglet.register(taglets);
        ICUStableTaglet.register(taglets);
//#if defined(J2SE13) || defined(J2SE14)
//##        ICUDeprecatedTaglet.register(taglets);
//#endif
        ICUProvisionalTaglet.register(taglets);
        ICUObsoleteTaglet.register(taglets);
        ICUIgnoreTaglet.register(taglets);
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
        if (tags != null) {
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

//#if defined(J2SE13) || defined(J2SE14)
//#else
    public TagletOutput getTagletOutput(Tag tag, TagletWriter writer) throws IllegalArgumentException {
        TagletOutput out = writer.getTagletOutputInstance();
        out.setOutput(toString(tag));
        return out;
    }

    public TagletOutput getTagletOutput(Doc holder, TagletWriter writer) throws IllegalArgumentException {
        TagletOutput out = writer.getTagletOutputInstance();
        Tag[] tags = holder.tags(getName());
        if (tags.length == 0) {
            return null;
        }
        out.setOutput(toString(tags[0]));
        return out;
    }
//#endif

    protected static final String STATUS = "<dt><b>Status:</b></dt>";

    public static class ICUInternalTaglet extends ICUTaglet {
        private static final String NAME = "internal";

        public static void register(Map taglets) {
            taglets.put(NAME, new ICUInternalTaglet());
        }

        private ICUInternalTaglet() {
            super(NAME, MASK_DEFAULT);
        }

        public String toString(Tag tag) {
            return STATUS + "<dd><em>Internal</em>. <font color='red'>This API is <em>ICU internal only</em>.</font></dd>";
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
//#if defined(J2SE13) || defined(J2SE14)
//##    /*
//##     * sigh, in JDK 1.5 we can't override the standard deprecated taglet
//##     * so easily.  I'm not impressed with the javadoc code.
//##     */
//##    public static class ICUDeprecatedTaglet extends ICUTaglet {
//##        private static final String NAME = "deprecated";
//##
//##        public static void register(Map taglets) {
//##	    taglets.remove(NAME); // override standard deprecated taglet
//##            taglets.put(NAME, new ICUDeprecatedTaglet());
//##        }
//##
//##        private ICUDeprecatedTaglet() {
//##            super(NAME, MASK_DEFAULT);
//##        }
//##
//##        public String toString(Tag tag) {
//##            BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
//##            String text = tag.text();
//##            bi.setText(text);
//##            int first = bi.first();
//##            int next = bi.next();
//##            if (first == -1 || next == -1) {
//##                System.err.println("Warning: bad deprecated tag '" + text + "'");
//##                return "<dd><em>Note</em>. " + text + "</dd>";
//##            } else {
//##                if ("This API is ICU internal only.".equals(text)) {
//##                    return null;
//##                }
//##                return "<dd><em>Note, " + text.substring(first, next) + "</em>. " + text.substring(next) + "</dd>";
//##            }
//##        }
//##    }
//#endif

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
            return STATUS + "<dd><em>Obsolete.</em> <font color='red'>Will be removed in " + text.substring(first, next) + "</font>. " + text.substring(next) + "</dd>";

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
}
