// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 * ****************************************************************************** Copyright (C)
 * 2002-2016 International Business Machines Corporation * and others. All Rights Reserved. *
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.docs;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.util.SimpleDocTreeVisitor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import jdk.javadoc.doclet.Taglet;

public abstract class ICUTaglet implements Taglet {
    protected final String name;
    protected final boolean isInline;

    protected static final String STATUS = "<dt><span class=\"simpleTagLabel\">Status:</span></dt>";

    public abstract String toStringDocTree(DocTree doc, Element element);

    protected ICUTaglet(String name, boolean isInline) {
        this.name = name;
        this.isInline = isInline;
    }

    @Override
    public Set<Location> getAllowedLocations() {
        Set<Location> result = new HashSet<>();
        result.add(Location.CONSTRUCTOR); // In the documentation for a constructor.
        result.add(Location.FIELD); // In the documentation for a field.
        result.add(Location.METHOD); // In the documentation for a method.
        result.add(Location.MODULE); // In the documentation for a module.
        result.add(Location.OVERVIEW); // In an Overview document.
        result.add(Location.PACKAGE); // In the documentation for a package.
        result.add(Location.TYPE); // In the documentation for a class, interface or enum.
        return result;
    }

    public boolean isInlineTag() {
        return isInline;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        if (tags != null) {
            if (tags.size() > 1) {
                String msg = "Should not have more than one ICU tag per element:\n";
                for (int i = 0; i < tags.size(); ++i) {
                    msg += "  [" + i + "] " + tags.get(i) + "\n";
                }
                throw new IllegalStateException(msg);
            } else if (tags.size() > 0) {
                return toStringDocTree(tags.get(0), element);
            }
        }
        return null;
    }

    static String getText(DocTree dt, Element element) {
        return dt.accept(
                new SimpleDocTreeVisitor<String, Void>() {
                    @Override
                    public String visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
                        for (DocTree dt : node.getContent()) {
                            return dt.accept(this, null);
                        }
                        return "";
                    }

                    @Override
                    public String visitUnknownInlineTag(UnknownInlineTagTree node, Void p) {
                        for (DocTree dt : node.getContent()) {
                            return dt.accept(this, null);
                        }
                        return "";
                    }

                    @Override
                    public String visitText(TextTree node, Void p) {
                        return node.getBody();
                    }
                },
                null);
    }
}
