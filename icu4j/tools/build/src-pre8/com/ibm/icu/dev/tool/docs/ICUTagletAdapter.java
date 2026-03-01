// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2015, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.docs;

import com.sun.javadoc.Doc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.internal.toolkit.taglets.Taglet;
import com.sun.tools.doclets.internal.toolkit.taglets.TagletOutput;
import com.sun.tools.doclets.internal.toolkit.taglets.TagletWriter;

/**
 * The ICUTagletAdapter class is the abstract base class that adapts the ICUTaglet class to different implementations of the JavaDoc API. 
 * The methods in this class are meant to minimize the dual maintenance nature of supporting multiple JavaDoc APIs.
 * 
 * This adapter supports the v7 and earlier JavaDoc API
 */
public abstract class ICUTagletAdapter implements Taglet {

    public abstract String toString(Tag tag);

    public abstract String toString(Tag[] tags);

    public TagletOutput getTagletOutput(Tag tag, TagletWriter writer)
            throws IllegalArgumentException {

            TagletOutput out = writer.getTagletOutputInstance();
            out.setOutput(toString(tag));
            return out;
        }

        public TagletOutput getTagletOutput(Doc holder, TagletWriter writer)
            throws IllegalArgumentException {

            TagletOutput out = writer.getTagletOutputInstance();
            Tag[] tags = holder.tags(getName());
            if (tags.length == 0) {
                return null;
            }
            out.setOutput(toString(tags[0]));
            return out;
        }

}
