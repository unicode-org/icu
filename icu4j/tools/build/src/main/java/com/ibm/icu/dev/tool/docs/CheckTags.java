// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 * ****************************************************************************** Copyright (C)
 * 2002-2010, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 */
/**
 * This is a tool to check the tags on ICU4J files. In particular, we're looking for:
 *
 * <p>- methods that have no tags - custom tags: @draft, @stable, @internal? - standard
 * tags: @since, @deprecated
 *
 * <p>Syntax of tags: '@draft ICU X.X.X' '@stable ICU X.X.X' '@internal' '@since (don't use)'
 * '@obsolete ICU X.X.X' '@deprecated to be removed in ICU X.X. [Use ...]'
 *
 * <p>flags names of classes and their members that have no tags or incorrect syntax.
 *
 * <p>Requires JDK 1.4 or later
 *
 * <p>Use build.xml 'checktags' ant target, or run from directory containing CheckTags.class as
 * follows: javadoc -classpath ${JAVA_HOME}/lib/tools.jar -doclet CheckTags -sourcepath ${ICU4J_src}
 * [packagenames]
 */
package com.ibm.icu.dev.tool.docs;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.InlineTagTree;
import com.sun.source.util.DocTrees;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public class CheckTags implements Doclet {
    private DocTrees docTrees;
    private Elements elements;
    private boolean log;
    private boolean brief;
    private boolean isShort;
    private DocStack stack = new DocStack();

    class DocNode {
        private String header;
        private boolean printed;
        private boolean reportError;
        private int errorCount;

        public void reset(String header, boolean reportError) {
            this.header = header;
            this.printed = false;
            this.errorCount = 0;
            this.reportError = reportError;
        }

        public String toString() {
            return header
                    + " printed: "
                    + printed
                    + " reportError: "
                    + reportError
                    + " errorCount: "
                    + errorCount;
        }
    }

    class DocStack {
        private DocNode[] stack;
        private int index;
        private boolean newline;

        public void push(String header, boolean reportError) {
            if (stack == null) {
                stack = new DocNode[5];
            } else {
                if (index == stack.length) {
                    DocNode[] temp = new DocNode[stack.length * 2];
                    System.arraycopy(stack, 0, temp, 0, index);
                    stack = temp;
                }
            }
            if (stack[index] == null) {
                stack[index] = new DocNode();
            }
            //  System.out.println("reset [" + index + "] header: " + header + " report: " +
            // reportError);
            stack[index++].reset(header, reportError);
        }

        public void pop() {
            if (index == 0) {
                throw new IndexOutOfBoundsException();
            }
            --index;

            int ec = stack[index].errorCount; // index already decremented
            if (ec > 0 || index == 0) { // always report for outermost element
                if (stack[index].reportError) {
                    output("(" + ec + (ec == 1 ? " error" : " errors") + ")", false, true, index);
                }

                // propagate to parent
                if (index > 0) {
                    stack[index - 1].errorCount += ec;
                }
            }
            if (index == 0) {
                System.out.println(); // always since we always report number of errors
            }
        }

        public void output(String msg, boolean error, boolean newline) {
            output(msg, error, newline, index - 1);
        }

        void output(String msg, boolean error, boolean newline, int ix) {
            DocNode last = stack[ix];
            if (error) {
                last.errorCount += 1;
            }

            boolean show = !brief || last.reportError;
            // boolean nomsg = show && brief && error;
            //            System.out.println(">>> " + last + " error: " + error + " show: " + show +
            // " nomsg: " + nomsg);

            if (show) {
                if (isShort || (brief && error)) {
                    msg = null; // nuke error messages if we're brief, just report headers and
                    // totals
                }
                for (int i = 0; i <= ix; ) {
                    DocNode n = stack[i];
                    if (n.printed) {
                        if (msg != null || !last.printed) { // since index > 0 last is not null
                            if (this.newline && i == 0) {
                                System.out.println();
                                this.newline = false;
                            }
                            System.out.print("  ");
                        }
                        ++i;
                    } else {
                        System.out.print(n.header);
                        n.printed = true;
                        this.newline = true;
                        i = 0;
                    }
                }

                if (msg != null) {
                    if (index == 0 && this.newline) {
                        System.out.println();
                    }
                    if (error) {
                        System.out.print("*** ");
                    }
                    System.out.print(msg);
                }
            }

            this.newline = newline;
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // The documentation says "usually the latest version"
        // But even if at this time JDK 23 is already released, we
        // want to be able to compile / use this doclet with at least JDK 11.
        // So anything above RELEASE_11 is undefined
        return SourceVersion.RELEASE_11;
    }

    @Override
    public void init(Locale locale, Reporter reporter) {}

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<Option> getSupportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    private static final Set<Option> SUPPORTED_OPTIONS =
            Set.of(
                    new JavadocHelper.GatherApiDataOption(
                            0, "-log", "log", "the description of name"),
                    new JavadocHelper.GatherApiDataOption(
                            0, "-brief", "brief", "the description of output"),
                    new JavadocHelper.GatherApiDataOption(
                            0, "-short", "short", "the description of base"));

    private void initFromOptions() {
        for (Option opt : SUPPORTED_OPTIONS) {
            JavadocHelper.GatherApiDataOption option = (JavadocHelper.GatherApiDataOption) opt;
            switch (option.getName()) {
                case "-log":
                    this.log = option.getBooleanValue(false);
                    break;
                case "-brief":
                    this.brief = option.getBooleanValue(false);
                    break;
                case "-isShort":
                    this.isShort = option.getBooleanValue(false);
                    break;
            }
        }
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        docTrees = environment.getDocTrees();
        elements = environment.getElementUtils();
        initFromOptions();
        List<? extends Element> allClasses =
                environment.getIncludedElements().stream()
                        .filter(e -> e.getKind().isClass())
                        .collect(Collectors.toList());
        doElements(allClasses, "Package", true);
        return true;
    }

    boolean newline = false;

    void output(String msg, boolean error, boolean newline) {
        stack.output(msg, error, newline);
    }

    void log() {
        output(null, false, false);
    }

    void logln() {
        output(null, false, true);
    }

    void log(String msg) {
        output(msg, false, false);
    }

    void logln(String msg) {
        output(msg, false, true);
    }

    void err(String msg) {
        output(msg, true, false);
    }

    void errln(String msg) {
        output(msg, true, true);
    }

    void tagErr(String msg, Element element, DocTree tag) {
        if (msg.length() > 0) {
            msg += ": ";
        }
        errln(
                msg
                        + tag.toString()
                        + " ["
                        + JavadocHelper.position(elements, docTrees, element, tag)
                        + "]");
    }
    ;

    void tagErr(Element element, BlockTagTree tag) {
        tagErr("", element, tag);
    }

    void doElements(Collection<? extends Element> elements, String header, boolean reportError) {
        if (elements != null && !elements.isEmpty()) {
            stack.push(header, reportError);
            for (Element element : elements) {
                doElement(element);
            }
            stack.pop();
        }
    }

    void doElement(Element element) {
        if (element != null
                && (JavadocHelper.isPublic(element) || JavadocHelper.isProtected(element))
                && !(JavadocHelper.isKindConstructor(element)
                        && JavadocHelper.isSynthetic(elements, element))) {
            // unfortunately, in JDK 1.4.1 MemberDoc.isSynthetic is not properly implemented for
            // synthetic constructors.  So you'll have to live with spurious errors or 'implement'
            // the synthetic constructors...

            boolean isClass = JavadocHelper.isKindClassOrInterface(element);
            String header;
            if (!isShort || isClass) {
                header = "--- ";
            } else {
                header = "";
            }
            if (element instanceof ExecutableElement) {
                header += JavadocHelper.flatSignature(element);
            } else {
                header +=
                        (isClass
                                ? ((QualifiedNameable) element).getQualifiedName()
                                : element.getSimpleName());
            }
            if (!isShort || isClass) {
                header += " ---";
            }
            stack.push(header, isClass);
            if (log) {
                logln();
            }
            boolean recurse = doTags(element);
            if (recurse && isClass) {
                TypeElement typeElement = (TypeElement) element;
                List<? extends Element> fields =
                        typeElement.getEnclosedElements().stream()
                                .filter(JavadocHelper::isKindField)
                                .collect(Collectors.toList());
                doElements(fields, "Fields", !brief);
                List<? extends Element> constructors =
                        typeElement.getEnclosedElements().stream()
                                .filter(JavadocHelper::isKindConstructor)
                                .collect(Collectors.toList());
                doElements(constructors, "Constructors", !brief);
                List<? extends Element> methods =
                        typeElement.getEnclosedElements().stream()
                                .filter(JavadocHelper::isKindMethod)
                                .collect(Collectors.toList());
                doElements(methods, "Methods", !brief);
            }
            stack.pop();
        }
    }

    /** Return true if sub-elements of this element should be checked */
    boolean doTags(Element element) {
        boolean foundRequiredTag = false;
        boolean foundDraftTag = false;
        boolean foundProvisionalTag = false;
        boolean foundDeprecatedTag = false;
        boolean foundObsoleteTag = false;
        boolean foundInternalTag = false;
        boolean foundStableTag = false;
        boolean retainAll = false;

        if (JavadocHelper.isIgnoredEnumMethod(element)) {
            return false;
        }

        // first check inline tags
        for (InlineTagTree tag : JavadocHelper.getInnerTags(docTrees, element)) {
            JavadocHelper.IcuTagKind tagKind = JavadocHelper.IcuTagKind.ofTag(tag);
            String text = JavadocHelper.toText(tag).trim();
            switch (tagKind) {
                case ICU:
                    {
                        if (JavadocHelper.isKindClassOrInterface(element)) {
                            tagErr("tag should appear only in member elements", element, tag);
                        }
                    }
                    break;
                case ICUNOTE:
                    {
                        if (!text.isEmpty()) {
                            tagErr("tag should not contain text", element, tag);
                        }
                    }
                    break;
                case ICUENHANCED:
                    {
                        if (text.isEmpty()) {
                            tagErr("text should name related jdk class", element, tag);
                        }
                        if (!(JavadocHelper.isKindClassOrInterface(element))) {
                            tagErr(
                                    "tag should appear only in class/interface elements",
                                    element,
                                    tag);
                        }
                    }
                    break;
                case UNKNOWN:
                    // It might be a standard tag, so we don't complain about this
                    break;
                default:
                    tagErr("unrecognized tagKind for tag", element, tag);
                    break;
            }
        }

        // next check regular tags
        for (BlockTagTree tag : JavadocHelper.getBlockTags(docTrees, element)) {
            JavadocHelper.TagKind tagKind = JavadocHelper.TagKind.ofTag(tag);
            String tagText = JavadocHelper.toText(tag);
            switch (tagKind) {
                case UNKNOWN:
                    errln("unknown kind: " + tag.getTagName());
                    break;

                case INTERNAL:
                    foundRequiredTag = true;
                    foundInternalTag = true;
                    break;

                case DRAFT:
                    foundRequiredTag = true;
                    foundDraftTag = true;
                    if (tagText.indexOf("ICU 2.8") != -1
                            && tagText.indexOf("(retain")
                                    == -1) { // catch both retain and retainAll
                        tagErr(element, tag);
                        break;
                    }
                    if (tagText.indexOf("ICU") != 0) {
                        tagErr(element, tag);
                        break;
                    }
                    retainAll |= (tagText.indexOf("(retainAll)") != -1);
                    break;

                case PROVISIONAL:
                    foundProvisionalTag = true;
                    break;

                case DEPRECATED:
                    foundDeprecatedTag = true;
                    if (tagText.indexOf("ICU") == 0) {
                        foundRequiredTag = true;
                    }
                    break;

                case OBSOLETE:
                    if (tagText.indexOf("ICU") != 0) {
                        tagErr(element, tag);
                    }
                    foundObsoleteTag = true;
                    foundRequiredTag = true;
                    break;

                case STABLE:
                    {
                        if (tagText.length() != 0 && tagText.indexOf("ICU") != 0) {
                            tagErr(tagText, element, tag);
                        }
                        foundRequiredTag = true;
                        foundStableTag = true;
                    }
                    break;

                case SINCE:
                    tagErr(element, tag);
                    break;

                case EXCEPTION:
                    // TODO: Why would we report this?
                    // logln("You really ought to use @throws, you know... :-)");
                    break;

                case AUTHOR:
                case SEE:
                case PARAM:
                case RETURN:
                case THROWS:
                case SERIAL:
                case DISCOURAGED:
                case CATEGORY:
                    break;

                case VERSION:
                    tagErr(element, tag);
                    break;

                default:
                    errln("unknown tagKind: " + tagKind);
            } // end if switch
        } // end of iteration on tags
        if (!foundRequiredTag) {
            errln(
                    "missing required tag ["
                            + JavadocHelper.position(elements, docTrees, element)
                            + "]");
        }
        if (foundInternalTag && !foundDeprecatedTag) {
            errln("internal tag missing deprecated");
        }
        if (foundDraftTag && !(foundDeprecatedTag || foundProvisionalTag)) {
            errln("draft tag missing deprecated or provisional");
        }
        if (foundObsoleteTag && !foundDeprecatedTag) {
            errln("obsolete tag missing deprecated");
        }
        if (foundStableTag && foundDeprecatedTag) {
            logln("stable deprecated");
        }

        return !retainAll;
    }
}
