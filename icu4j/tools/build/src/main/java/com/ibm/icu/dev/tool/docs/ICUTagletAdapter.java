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
import com.sun.tools.doclets.formats.html.markup.RawHtml;
import com.sun.tools.doclets.internal.toolkit.Content;
import com.sun.tools.doclets.internal.toolkit.taglets.Taglet;
import com.sun.tools.doclets.internal.toolkit.taglets.TagletWriter;

/**
 * The ICUTagletAdapter class is the abstract base class that adapts the ICUTaglet class to different implementations of the JavaDoc API. 
 * The methods in this class are meant to minimize the dual maintenance nature of supporting multiple JavaDoc APIs.
 * 
 * This adapter supports the v8 JavaDoc API
 */
public abstract class ICUTagletAdapter implements Taglet {
    
    public abstract String toString(Tag tag);

    public abstract String toString(Tag[] tags);

    public Content getTagletOutput(Tag tag, TagletWriter writer)
        throws IllegalArgumentException {

        // addContext doesn't except nulls so filter them out
        String encodedText = toString(tag);
        if(encodedText == null) return null;
           
        Content out = writer.getOutputInstance();
        out.addContent(new RawHtml(encodedText));
         
        return out;
    }

    public Content getTagletOutput(Doc holder, TagletWriter writer)
        throws IllegalArgumentException {

        Content out = writer.getOutputInstance();
        Tag[] tags = holder.tags(getName());
        if (tags.length == 0) {
            return null;
        }

        // addContext doesn't except nulls so filter them out
        String encodedText = toString(tags[0]);
        if(encodedText == null) return null;

        out.addContent(new RawHtml(encodedText));
        return out;
    }

}
