// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2004-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/**
 * Generate a list of ICU's public APIs, sorted by qualified name and signature
 * public APIs are all non-internal, non-package apis in com.ibm.icu.[lang|math|text|util].
 * For each API, list
 * - public, package, protected, or private (PB PK PT PR)
 * - static or non-static (STK NST)
 * - final or non-final (FN NF)
 * - synchronized or non-synchronized (SYN NSY)
 * - stable, draft, deprecated, obsolete (ST DR DP OB)
 * - abstract or non-abstract (AB NA)
 * - constructor, member, field (C M F)
 *
 * Requires JDK 1.5 or later
 *
 * Sample compilation:
 * c:/doug/java/jdk1.5/build/windows-i586/bin/javac *.java
 *
 * Sample execution
 * c:/j2sdk1.5/bin/javadoc
 *   -classpath c:/jd2sk1.5/lib/tools.jar
 *   -doclet com.ibm.icu.dev.tool.docs.GatherAPIData
 *   -docletpath c:/doug/icu4j/tools/build/out/lib/icu4j-build-tools.jar
 *   -sourcepath c:/doug/icu4j/main/classes/core/src
 *   -name "ICU4J 4.2"
 *   -output icu4j42.api2
 *   -gzip
 *   -source 1.5
 *   com.ibm.icu.lang com.ibm.icu.math com.ibm.icu.text com.ibm.icu.util
 *
 * todo: provide command-line control of filters of which subclasses/packages to process
 * todo: record full inheritance hierarchy, not just immediate inheritance
 * todo: allow for aliasing comparisons (force (pkg.)*class to be treated as though it
 *       were in a different pkg/class hierarchy (facilitates comparison of icu4j and java)
 */

package com.ibm.icu.dev.tool.docs;

// standard release sdk won't work, need internal build to get access to javadoc
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_9)
public class GatherAPIData implements Doclet {
    private Elements elementUtils;
    private DocTrees docTrees;
    private TreeSet<APIInfo> results = new TreeSet<>(APIInfo.defaultComparator());
    private String srcName = ""; // default source name
    private String output; // name of output file to write
    private String base; // strip this prefix
    private Pattern pat;
    private boolean zip;
    private boolean gzip;
    private boolean internal;
    private boolean version;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // The documentation says "usually the latest version"
        // But even if at this time JDK 23 is already released, we
        // want to be able to compile / use this doclet with at least JDK 11.
        // So anything above RELEASE_11 is undefined
        return SourceVersion.RELEASE_11;
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<Doclet.Option> getSupportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        elementUtils = environment.getElementUtils();
        docTrees = environment.getDocTrees();

        initFromOptions();
        doElements(environment.getIncludedElements());

        try (OutputStream os = getOutputFileAsStream(output);
                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            BufferedWriter bw = new BufferedWriter(osw);
            // writing data file
            bw.write(String.valueOf(APIInfo.VERSION) + APIInfo.SEP); // header version
            bw.write(srcName + APIInfo.SEP); // source name
            bw.write((base == null ? "" : base) + APIInfo.SEP); // base
            bw.newLine();
            writeResults(results, bw);
            bw.close(); // should flush, close all, etc
        } catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.initCause(e);
            throw re;
        }

        return true;
    }

    private OutputStream getOutputFileAsStream(String output) throws IOException {
        if (output == null) {
            return System.out;
        }
        if (zip) {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output + ".zip"));
            zos.putNextEntry(new ZipEntry(output));
            return zos;
        }
        if (gzip) {
            return new GZIPOutputStream(new FileOutputStream(output + ".gz"));
        }
        return new FileOutputStream(output);
    }

    private void doElements(Collection<? extends Element> elements) {
        if (elements != null) {
            for (Element element : elements) {
                doElement(element);
            }
        }
    }

    private void doElement(Element element) {
        if (ignore(element)) return;

        // isClass() ==> CLASS || ENUM;
        // isInterface() ==> INTERFACE || ANNOTATION_TYPE
        if (JavadocHelper.isKindClassOrInterface(element)) {
            doElements(element.getEnclosedElements());
        }

        APIInfo info = createInfo(element);
        if (info != null) {
            results.add(info);
        }
    }

    // isSynthesized also doesn't seem to work.  Let's do this, documenting
    // synthesized constructors for abstract classes is kind of weird.
    // We can't actually tell if the constructor was synthesized or is
    // actually in the docs, but this shouldn't matter.  We don't really
    // care if we didn't properly document the draft status of
    // default constructors for abstract classes.

    // Update: We mandate a no-arg synthetic constructor with explicit
    // javadoc comments by the policy. So, we no longer ignore abstract
    // class's no-arg constructor blindly. -Yoshito 2014-05-21

    private boolean isAbstractClassDefaultConstructor(Element element) {
        return JavadocHelper.isKindConstructor(element)
            && JavadocHelper.isAbstract(element.getEnclosingElement())
            && ((ExecutableElement) element).getParameters().isEmpty();
    }

    private static final boolean IGNORE_NO_ARG_ABSTRACT_CTOR = false;

    private boolean ignore(Element element) {
        if (element == null) {
            return true;
        }

        if (JavadocHelper.isPrivate(element) || JavadocHelper.isDefault(element)) {
            return true;
        }

        if (JavadocHelper.isVisibilityPackage(element)) {
            return true;
        }

        if (JavadocHelper.isKindPackage(element)) {
            return true;
        }

        if (element.toString().contains(".misc")) {
            System.out.println("misc: " + element.toString()); {
                return true;
            }
        }

        if (JavadocHelper.isIgnoredEnumMethod(element)) {
            return true;
        }

        if (IGNORE_NO_ARG_ABSTRACT_CTOR && isAbstractClassDefaultConstructor(element)) {
            return true;
        }

        if (!internal) { // debug
            for (BlockTagTree tag : JavadocHelper.getBlockTags(docTrees, element)) {
                if (JavadocHelper.TagKind.ofTag(tag) == JavadocHelper.TagKind.INTERNAL) {
                    return true;
                }
            }
        }

        if (pat != null && JavadocHelper.isKindClassOrInterface(element)) {
            if (!pat.matcher(element.getSimpleName().toString()).matches()) {
                return true;
            }
        }

        return false;
    }

    private static void writeResults(Collection<APIInfo> c, BufferedWriter w) {
        for (APIInfo info : c) {
            info.writeln(w);
        }
    }

    private String trimBase(String arg) {
        String orgArg = arg;
        if (base != null) {
            for (int n = arg.indexOf(base); n != -1; n = arg.indexOf(base, n)) {
                arg = arg.substring(0, n) + arg.substring(n + base.length());
            }
        }
        return arg;
    }

    private APIInfo createInfo(Element element) {
        if (ignore(element)) return null;

        APIInfo info = new APIInfo();
        if (version) {
            info.includeStatusVersion(true);
        }

        // status
        String[] version = new String[1];
        info.setType(APIInfo.STA, tagStatus(element, version));
        info.setStatusVersion(version[0]);

        // visibility
        if (JavadocHelper.isPublic(element)) {
            info.setPublic();
        } else if (JavadocHelper.isProtected(element)) {
            info.setProtected();
        } else if (JavadocHelper.isPrivate(element)) {
            info.setPrivate();
        } else {
            // default is package
        }

        // static
        if (JavadocHelper.isStatic(element)) {
            info.setStatic();
        } else {
            // default is non-static
        }

        // Final. Enums are final by default.
        if (JavadocHelper.isFinal(element) && !JavadocHelper.isKindEnum(element)) {
            info.setFinal();
        } else {
            // default is non-final
        }

        // type
        if (JavadocHelper.isKindFieldExact(element)) {
            info.setField();
        } else if (JavadocHelper.isKindMethod(element)) {
            info.setMethod();
        } else if (JavadocHelper.isKindConstructor(element)) {
            info.setConstructor();
        } else if (JavadocHelper.isKindClassOrInterface(element)) {
            if (JavadocHelper.isKindEnum(element)) {
                info.setEnum();
            } else {
                info.setClass();
            }
        } else if (JavadocHelper.isKindEnumConstant(element)) {
            info.setEnumConstant();
        }

        PackageElement packageElement = elementUtils.getPackageOf(element);
        info.setPackage(trimBase(packageElement.getQualifiedName().toString()));

        String className = (JavadocHelper.isKindClassOrInterface(element) || element.getEnclosingElement() == null)
                ? ""
                : withoutPackage(element.getEnclosingElement());
        info.setClassName(className);

        String name = element.getSimpleName().toString();
        if (JavadocHelper.isKindConstructor(element)) {
            // The constructor name is always `<init>` with the javax.lang APIs.
            // For backward compatibility with older generated files we use the class name instead.
            name = className;
        } else if (JavadocHelper.isKindClassOrInterface(element)) {
            name = withoutPackage(element);
        }
        info.setName(name);

        if (JavadocHelper.isKindField(element)) {
            VariableElement varElement = (VariableElement) element;
            hackSetSignature(info, trimBase(varElement.asType().toString()));
        } else if (JavadocHelper.isKindClassOrInterface(element)) {
            TypeElement typeElementc = (TypeElement) element;

            if (!JavadocHelper.isKindInterface(element) && JavadocHelper.isAbstract(typeElementc)) {
                // interfaces are abstract by default, don't mark them as abstract
                info.setAbstract();
            }

            StringBuilder buf = new StringBuilder();
            if (JavadocHelper.isKindClass(typeElementc)) {
                buf.append("extends ");
                buf.append(typeElementc.getSuperclass().toString());
            }
            List<? extends TypeMirror> imp = typeElementc.getInterfaces();
            if (!imp.isEmpty()) {
                if (buf.length() > 0) {
                    buf.append(" ");
                }
                buf.append("implements");
                for (int i = 0; i < imp.size(); ++i) {
                    if (i != 0) {
                        buf.append(",");
                    }
                    buf.append(" ");
                    buf.append(imp.get(i).toString()
                            .replaceAll("<[^<>]+>", "") // interfaces with parameters.
                            .replaceAll("<[^<>]+>", "") // 3 nesting levels should be enough
                            .replaceAll("<[^<>]+>", "") // max I've seen was 2
                    );
                }
            }
            hackSetSignature(info, trimBase(buf.toString()));
        } else if (JavadocHelper.isKindMethod(element) || JavadocHelper.isKindConstructor(element)) {
            ExecutableElement execElement = (ExecutableElement) element;
            if (JavadocHelper.isSynchronized(execElement)) {
                info.setSynchronized();
            }

            if (JavadocHelper.isKindMethod(element)) {
                if (JavadocHelper.isAbstract(execElement)) {
                    // Workaround for Javadoc incompatibility between 7 and 8.
                    // isAbstract() returns false for a method in an interface
                    // on Javadoc 7, while Javadoc 8 returns true. Because existing
                    // API signature data files were generated before, we do not
                    // set abstract if a method is in an interface.
                    if (!JavadocHelper.isKindInterface(execElement.getEnclosingElement())) {
                        info.setAbstract();
                    }
                }

                String retSig = stringFromTypeMirror(execElement.getReturnType());

                // Signature, as returned by default, can be something like this: "boolean<T>containsAll(java.util.Iterator<T>)"
                // The old API returned "boolean(java.util.Iterator<T>)"
                // Consider using the signature "as is" (including the method name)
                hackSetSignature(info, trimBase(retSig + toTheBracket(execElement.toString())));
            } else {
                // constructor
                hackSetSignature(info, toTheBracket(execElement.toString()));
            }
        } else {
            throw new RuntimeException("Unknown element kind: " + element.getKind());
        }

        return info;
    }

    private static String stringFromTypeMirror(TypeMirror rrt) {
        StringBuilder retSig = new StringBuilder();
        rrt.accept(new ToStringTypeVisitor(), retSig);
        return retSig.toString();
    }

    private void hackSetSignature(APIInfo info, String value) {
        value = value.replace(",", ", ").replace(",  ", ", ");
        info.setSignature(value);
    }

    private String withoutPackage(Element enclosingElement) {
        if (enclosingElement == null) {
            return "";
        }

        String result = enclosingElement.toString();

        PackageElement pack = this.elementUtils.getPackageOf(enclosingElement);
        if (pack == null) {
            return result;
        }

        // Takes something like "com.ibm.icu.charset.CharsetCallback.Decoder"
        // and removes the package, resulting in "CharsetCallback.Decoder"
        // This can't really be done just by looking at the string form.
        String packName = pack.getQualifiedName().toString() + ".";
        return result.startsWith(packName) ? result.substring(packName.length()) : result;
    }

    private String toTheBracket(String str) {
        if (str == null) return null;
        int openBr = str.indexOf('(');
        return openBr > 1 ? str.substring(openBr) : str;
    }

    private int tagStatus(final Element element, String[] version) {
        class Result {
            boolean deprecatedFlag = false;
            int res = -1;
            void set(int val) {
                if (res != -1) {
                    boolean isValid = true;
                    if (val == APIInfo.STA_DEPRECATED) {
                        // @internal and @obsolete should be always used along with @deprecated.
                        // no change for status
                        isValid = (res == APIInfo.STA_INTERNAL || res == APIInfo.STA_OBSOLETE);
                        deprecatedFlag = true;
                    } else if (val == APIInfo.STA_INTERNAL) {
                        // @deprecated should be always used along with @internal.
                        // update status
                        if (res == APIInfo.STA_DEPRECATED) {
                            res = val;  // APIInfo.STA_INTERNAL
                        } else {
                            isValid = false;
                        }
                    } else if (val == APIInfo.STA_OBSOLETE) {
                        // @deprecated should be always used along with @obsolete.
                        // update status
                        if (res == APIInfo.STA_DEPRECATED) {
                            res = val;  // APIInfo.STA_OBSOLETE
                        } else {
                            isValid = false;
                        }
                    } else {
                        // two different status tags must not co-exist, except for
                        // following two cases:
                        // 1. @internal and @deprecated
                        // 2. @obsolete and @deprecated
                        isValid = false;
                    }
                    if (!isValid) {
                        System.err.println("bad element: " + element + " both: "
                                           + APIInfo.getTypeValName(APIInfo.STA, res) + " and: "
                                           + APIInfo.getTypeValName(APIInfo.STA, val));
                        return;
                    }
                } else {
                    // ok to replace with new tag
                    res = val;
                    if (val == APIInfo.STA_DEPRECATED) {
                        deprecatedFlag = true;
                    }
                }
            }
            int get() {
                if (res == -1) {
                    System.err.println("warning: no tag for " + element);
                    return 0;
                } else if (res == APIInfo.STA_INTERNAL && !deprecatedFlag) {
                    System.err.println("warning: no @deprecated tag for @internal API: " + element);
                }
                return res;
            }
        }

        List<BlockTagTree> tags = JavadocHelper.getBlockTags(docTrees, element);
        Result result = new Result();
        String statusVer = "";
        for (BlockTagTree tag : tags) {
            JavadocHelper.TagKind tagKind = JavadocHelper.TagKind.ofTag(tag);
            switch (tagKind) {
                case INTERNAL:
                    result.set(internal ? APIInfo.STA_INTERNAL : -2); // -2 for legacy compatibility
                    statusVer = getStatusVersion(tag);
                    break;

                case DRAFT:
                    result.set(APIInfo.STA_DRAFT);
                    statusVer = getStatusVersion(tag);
                    break;

                case STABLE:
                    result.set(APIInfo.STA_STABLE);
                    statusVer = getStatusVersion(tag);
                    break;

                case DEPRECATED:
                    result.set(APIInfo.STA_DEPRECATED);
                    statusVer = getStatusVersion(tag);
                    break;

                case OBSOLETE:
                    result.set(APIInfo.STA_OBSOLETE);
                    statusVer = getStatusVersion(tag);
                    break;

                case SINCE:
                case EXCEPTION:
                case VERSION:
                case UNKNOWN:
                case AUTHOR:
                case SEE:
                case PARAM:
                case RETURN:
                case THROWS:
                case SERIAL:
                case DISCOURAGED:
                case CATEGORY:
                    break;

                default:
                    throw new RuntimeException("unknown tagKind " + tagKind + " for tag: " + tag);
            }
        }

        if (version != null) {
            version[0] = statusVer;
        }
        return result.get();
    }

    private String getStatusVersion(BlockTagTree tag) {
        String text = tag.toString();
        if (text != null && text.length() > 0) {
            // Extract version string
            int start = -1;
            int i = 0;
            for (; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '.' || (ch >= '0' && ch <= '9')) {
                    if (start == -1) {
                        start = i;
                    }
                } else if (start != -1) {
                    break;
                }
            }
            if (start != -1) {
                return text.substring(start, i);
            }
        }
        return "";
    }

    private final static Set<Doclet.Option> SUPPORTED_OPTIONS = Set.of(
        new JavadocHelper.GatherApiDataOption(1, "-name", "the_name", "the description of name"),
        new JavadocHelper.GatherApiDataOption(1, "-output", "the_output", "the description of output"),
        new JavadocHelper.GatherApiDataOption(1, "-base", "the_base", "the description of base"),
        new JavadocHelper.GatherApiDataOption(1, "--filter", "the_filter", "the description of filter"),
        new JavadocHelper.GatherApiDataOption(0, "-zip", "the description of zip"),
        new JavadocHelper.GatherApiDataOption(0, "-gzip", "the description of gzip"),
        new JavadocHelper.GatherApiDataOption(0, "-internal", "the description of internal"),
        new JavadocHelper.GatherApiDataOption(0, "-version", "the description of version")
    );

    private void initFromOptions() {
        for (Doclet.Option opt : SUPPORTED_OPTIONS) {
            JavadocHelper.GatherApiDataOption option = (JavadocHelper.GatherApiDataOption) opt;
            switch (option.getName()) {
                case "-name":
                    this.srcName = option.getStringValue("");
                    break;
                case "-output":
                    this.output = option.getStringValue(null);
                    break;
                case "-base":
                    this.base = option.getStringValue(null); // should not include '.'
                    break;
                case "-filter":
                    String filt = option.getStringValue(null);
                    if (filt != null) {
                        this.pat = Pattern.compile(filt, Pattern.CASE_INSENSITIVE);
                    }
                    break;
                case "-zip":
                    this.zip = option.getBooleanValue(false);
                    break;
                case "-gzip":
                    this.gzip = option.getBooleanValue(false);
                    break;
                case "-internal":
                    this.internal = option.getBooleanValue(false);
                    break;
                case "-version":
                    this.version = option.getBooleanValue(false);
                    break;
            }
        }
    }

    static class ToStringTypeVisitor implements TypeVisitor<StringBuilder, StringBuilder> {

        @Override
        public StringBuilder visitPrimitive(PrimitiveType t, StringBuilder p) {
            return p.append(t);
        }

        @Override
        public StringBuilder visitArray(ArrayType t, StringBuilder p) {
            return p.append(t);
        }

        @Override
        public StringBuilder visitDeclared(DeclaredType t, StringBuilder p) {
            return p.append(t);
        }

        @Override
        public StringBuilder visitTypeVariable(TypeVariable t, StringBuilder p) {
            String upperBound = t.getUpperBound().toString();
            p.append(t.asElement().getSimpleName());
            if (!"java.lang.Object".equals(upperBound)) {
                return p.append(" extends ").append(upperBound);
            }
            return p;
        }

        @Override
        public StringBuilder visitNoType(NoType t, StringBuilder p) {
            return p.append(t);
        }

        // ==================================================================
        // Below this line there are unexpected types.
        // But we must implement them because the interface requires it.
        // And also because we want to throw (so that we can fix it).

        @Override
        public StringBuilder visitNull(NullType t, StringBuilder p) {
            throw new RuntimeException("Unexpected visitor NullType:" + t);
        }

        @Override
        public StringBuilder visitUnknown(TypeMirror t, StringBuilder p) {
            throw new RuntimeException("Unexpected visitor TypeMirror:" + t);
        }

        @Override
        public StringBuilder visitUnion(UnionType t, StringBuilder p) {
            throw new RuntimeException("Unexpected visitor UnionType:" + t);
        }

        @Override
        public StringBuilder visit(TypeMirror t, StringBuilder p) {
            throw new RuntimeException("Unexpected visitor TypeMirror:" + t);
        }

        @Override
        public StringBuilder visitIntersection(IntersectionType t, StringBuilder p) {
            throw new RuntimeException("Unexpected visitor IntersectionType:" + t);
        }

        @Override
        public StringBuilder visitWildcard(WildcardType t, StringBuilder p) {
            throw new RuntimeException("Unexpected visitor WildcardType:" + t);
        }

        @Override
        public StringBuilder visitExecutable(ExecutableType t, StringBuilder p) {
            throw new RuntimeException("Unexpected visitor ExecutableType:" + t);
        }

        @Override
        public StringBuilder visitError(ErrorType t, StringBuilder p) {
            throw new RuntimeException("Unexpected visitor ErrorType:" + t);
        }
    }

}
