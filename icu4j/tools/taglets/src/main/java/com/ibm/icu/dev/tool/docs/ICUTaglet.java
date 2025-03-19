// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 2002-2016 International Business Machines Corporation   *
* and others. All Rights Reserved.                                            *
*******************************************************************************
*/

package com.ibm.icu.dev.tool.docs;

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

    static String ICU_LABEL = "<strong><font color=red>[icu]</font></strong>";
}
