// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 2002-2010, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
/**
 * This is a tool to check the tags on ICU4J files.  In particular, we're looking for:
 *
 * - methods that have no tags
 * - custom tags: @draft, @stable, @internal?
 * - standard tags: @since, @deprecated
 *
 * Syntax of tags:
 * '@draft ICU X.X.X'
 * '@stable ICU X.X.X'
 * '@internal'
 * '@since  (don't use)'
 * '@obsolete ICU X.X.X'
 * '@deprecated to be removed in ICU X.X. [Use ...]'
 *
 * flags names of classes and their members that have no tags or incorrect syntax.
 *
 * Use build.xml 'checktags' ant target, or
 * run from directory containing CheckTags.class as follows:
 * javadoc -classpath ${JAVA_HOME}/lib/tools.jar -doclet CheckTags -sourcepath ${ICU4J_src} [packagenames]
 */

package com.ibm.icu.dev.tool.docs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.InlineTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;

import jdk.javadoc.doclet.Doclet;

class JavadocHelper {

    // ===== Kind =====

    /** The standard {@code isClass()} test returns true for both {@code CLASS}
     * and {@code ENUM}.
     * If what you need is CLASS only, use {@link #isKindClassExact(Element element)}@
     */
    static boolean isKindClass(Element element) {
        return element.getKind().isClass();
    }

    static boolean isKindClassExact(Element element) {
        return element.getKind() == ElementKind.CLASS;
    }

    /** The standard {@code isInterface()} test returns true for both {@code INTERFACE}
     * and {@code ANNOTATION_TYPE}.
     * If what you need is INTERFACE only, use {@link #isKindInterfaceExact(Element element)}@
     */
    static boolean isKindInterface(Element element) {
        return element.getKind().isInterface();
    }

    static boolean isKindInterfaceExact(Element element) {
        return element.getKind() == ElementKind.INTERFACE;
    }

    static boolean isKindConstructor(Element element) {
        return element.getKind() == ElementKind.CONSTRUCTOR;
    }

    static boolean isKindMethod(Element element) {
        return element.getKind() == ElementKind.METHOD;
    }

    static boolean isKindEnum(Element element) {
        return element.getKind() == ElementKind.ENUM;
    }

    /** The standard {@code isField()} test returns true for both {@code FIELD}
     * and {@code ENUM_CONSTANT}.
     * If what you need is FIELD only, use {@link #isKindFieldExact(Element element)}@
     */
    static boolean isKindField(Element element) {
        return element.getKind().isField();
    }

    static boolean isKindFieldExact(Element element) {
        return element.getKind() == ElementKind.FIELD;
    }

    static boolean isKindEnumConstant(Element element) {
        return element.getKind() == ElementKind.ENUM_CONSTANT;
    }

    static boolean isKindPackage(Element element) {
        return element.getKind() == ElementKind.PACKAGE;
    }

    static boolean isKindClassOrInterface(Element element) {
        ElementKind kind = element.getKind();
        return kind.isClass() || kind.isInterface();
    }

    // ===== Visibility =====

    static boolean isPublic(Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC);
    }

    static boolean isProtected(Element element) {
        return element.getModifiers().contains(Modifier.PROTECTED);
    }

    static boolean isPrivate(Element element) {
        return element.getModifiers().contains(Modifier.PRIVATE);
    }

    static boolean isAbstract(Element element) {
        return element.getModifiers().contains(Modifier.ABSTRACT);
    }

    static boolean isStatic(Element element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    static boolean isFinal(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    static boolean isDefault(Element element) {
        return element.getModifiers().contains(Modifier.DEFAULT);
    }

    static boolean isSynchronized(Element element) {
        return element.getModifiers().contains(Modifier.SYNCHRONIZED);
    }

    static boolean isVisibilityPackage(Element element) {
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.PUBLIC)
                || modifiers.contains(Modifier.PROTECTED)
                || modifiers.contains(Modifier.PRIVATE)) {
            return false;
        }
        return true;
    }

    static boolean isSynthetic(Elements elements, Element element) {
        return elements.getOrigin(element) == Elements.Origin.SYNTHETIC;
    }

    static String flatSignature(Element element) {
        return element == null ? "null" : element.toString().replace("java.lang.", "");
    }

    /** Returns the location of the element, in the /absolute/path/to/File.java[1234] form. */
    static String position(Elements el, DocTrees docTrees, Element element) {
        TreePath path = docTrees.getPath(element);
        if (path == null) {
            // I've seen this happening for values() and valueOf(), which are created by the compiler
            // in enums. So they "exist", but there is no location in the file for them.
            // After ignoring values() and valueOf() (we can't tag them anyway), this error never happens.
            // But let's keep it here, for future safety. Who knows what else the compiler will start creating.
            return "<unknown_location>:<unknown_line>";
        }
        CompilationUnitTree cu = path.getCompilationUnit();
        long startPos = docTrees.getSourcePositions().getStartPosition(cu, docTrees.getTree(element));
        return cu.getSourceFile().getName() + ":" + cu.getLineMap().getLineNumber(startPos);
    }

    static String position(Elements el, DocTrees docTrees, Element element, DocTree tag) {
        // I didn't manage to get the location of a tag, but at least we can show the location
        // of the parent element.
        return position(el, docTrees, element);
    }

    // ===== Accessing tags =====

    static List<BlockTagTree> getBlockTags(DocTrees docTrees, Element element) {
        List<BlockTagTree> result = new ArrayList<>();
        DocCommentTree dct = docTrees.getDocCommentTree(element);
        if (dct == null) {
            return result;
        }
        List<? extends DocTree> blockTags = dct.getBlockTags();
        if (blockTags == null) {
            return result;
        }
        for (DocTree btags : blockTags) {
            result.add((BlockTagTree) btags);
        }
        return result;
    }

    static List<InlineTagTree> getInnerTags(DocTrees docTrees, Element element) {
        List<InlineTagTree> result = new ArrayList<>();
        DocCommentTree dct = docTrees.getDocCommentTree(element);
        if (dct == null) {
            return result;
        }
        List<? extends DocTree> fullBody = dct.getFullBody();
        if (fullBody == null) {
            return result;
        }
        for (DocTree tag : fullBody) {
            if (tag instanceof InlineTagTree) {
                result.add((InlineTagTree) tag);
            }
        }
        return result;
    }

    static String toText(DocTree dt) {
        String result = dt == null ? "<null>" : dt.toString();
        if (result.startsWith("{@") && result.endsWith("}")) {
            // Remove the `{` and `}` from an inline tag (`{@foo some text here}`)
            result = result.substring(1, result.length() - 1);
        }
        // Remove the `@foo` part of the tag, applies to the block tags, and the inline tag cleaned above
        if (result.startsWith("@")) {
            result = result.replaceFirst("^@[a-zA-Z0-9_]+\\s+", "");
        }
        return result;
    }

    // Sigh. Javadoc doesn't indicate when the compiler generates
    // the values and valueOf enum methods.  The position of the
    // method for these is not always the same as the position of
    // the class, though it often is, so we can't use that.
    static boolean isIgnoredEnumMethod(Element doc) {
        if (JavadocHelper.isKindMethod(doc) && JavadocHelper.isKindEnum(doc.getEnclosingElement())) {
            String name = doc.getSimpleName().toString();
            // assume we don't have enums that overload these method names.
            return "values".equals(name) || "valueOf".equals(name);
        }
        return false;
    }

    // Known taglets, standard or ICU extensions
    static enum TagKind {
        UNKNOWN("<unknown>"),
        INTERNAL("internal"),
        DRAFT("draft"),
        STABLE("stable"),
        SINCE("since"),
        DEPRECATED("deprecated"),
        AUTHOR("author"),
        SEE("see"),
        VERSION("version"),
        PARAM("param"),
        RETURN("return"),
        THROWS("throws"),
        OBSOLETE("obsolete"),
        EXCEPTION("exception"),
        SERIAL("serial"),
        PROVISIONAL("provisional"), // never used
        DISCOURAGED("discouraged"), // DecimalFormatSymbols(x13), IslamicCalendar(x2)
        CATEGORY("category"); // only used in DecimalFormat(x82)

        private final String value;

        private TagKind(String value) {
            this.value = value;
        }

        static TagKind ofTag(BlockTagTree tag) {
            for (TagKind tk : TagKind.values()) {
                if (tk.value.equals(tag.getTagName())) {
                    return tk;
                }
            }
            return TagKind.UNKNOWN;
        }
    }

    // Known ICU taglets
    static enum IcuTagKind {
        UNKNOWN("<unknown>"),
        ICU("icu"),
        ICUNOTE("icunote"),
        ICUENHANCED("icuenhanced");

        private final String value;

        private IcuTagKind(String value) {
            this.value = value;
        }

        static IcuTagKind ofTag(InlineTagTree tag) {
            for (IcuTagKind tk : IcuTagKind.values()) {
                if (tk.value.equals(tag.getTagName())) {
                    return tk;
                }
            }
            return IcuTagKind.UNKNOWN;
        }
    }

    // ===== Doclet options convenience class =====

    static class GatherApiDataOption implements Doclet.Option {
        final int length;
        final String name;
        final String paramName;
        final String description;
        String strValue = null;
        Boolean boolValue = null;

        GatherApiDataOption(int length, String name, String description) {
            this(length, name, null, description); // no parameter
        }

        GatherApiDataOption(int length, String name, String paramName, String description) {
            this.length = length;
            this.name = name;
            this.paramName = paramName;
            this.description = description;
        }

        @Override
        public int getArgumentCount() {
            return length;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Doclet.Option.Kind getKind() {
            return Doclet.Option.Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return List.of(name);
        }

        @Override
        public String getParameters() {
            return this.paramName;
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            if (!option.equals(name)) {
                return false;
            }
            if (length == 0) {
                boolValue = true;
                return true;
            }
            if (arguments == null || arguments.size() < 1) {
                return false;
            }
            strValue = arguments.get(0);
            return true;
        }

        public String getName() {
            return name;
        }

        public Boolean getBooleanValue(Boolean fallbackValue) {
            return boolValue != null ? boolValue : fallbackValue;
        }

        public String getStringValue(String fallbackValue) {
            return strValue != null ? strValue : fallbackValue;
        }
    }
}
